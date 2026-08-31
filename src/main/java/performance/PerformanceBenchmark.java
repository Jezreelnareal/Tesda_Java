package performance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import model.User;
import repository.UserRepository;
import util.DatabaseConnection;

/**
 * Read-only benchmark comparing a new physical JDBC connection per lookup
 * with JCash's reusable single-threaded connection.
 */
public final class PerformanceBenchmark {

    private static final int DEFAULT_ITERATIONS = 200;
    private static final int WARM_UP_ITERATIONS = 20;
    private static final String DEFAULT_MOBILE_NUMBER = "09171234567";

    private PerformanceBenchmark() {
        // Utility entry point
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            BenchmarkMode mode = readMode(args);
            int iterations = readIterations(args);
            String mobileNumber = getBenchmarkMobileNumber();

            DatabaseConnection.verifyConnection();
            DatabaseConnection.shutdown();

            UserRepository repository = new UserRepository();
            for (int index = 0; index < WARM_UP_ITERATIONS; index++) {
                runLookup(mode, repository, mobileNumber, null);
            }

            List<Long> elapsedNanoseconds = new ArrayList<>(iterations);
            Set<Long> connectionIds = new LinkedHashSet<>();
            int foundUsers = 0;

            for (int index = 0; index < iterations; index++) {
                LookupResult result = runLookup(
                        mode,
                        repository,
                        mobileNumber,
                        connectionIds
                );
                elapsedNanoseconds.add(result.elapsedNanoseconds());
                if (result.userFound()) {
                    foundUsers++;
                }
            }

            printResults(
                    mode,
                    iterations,
                    mobileNumber,
                    foundUsers,
                    elapsedNanoseconds,
                    connectionIds
            );
        } catch (IllegalArgumentException exception) {
            System.err.println("Benchmark configuration error: "
                    + exception.getMessage());
            exitCode = 2;
        } catch (SQLException exception) {
            System.err.println("Benchmark database error: "
                    + exception.getMessage());
            exitCode = 1;
        } finally {
            DatabaseConnection.shutdown();
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static LookupResult runLookup(
            BenchmarkMode mode,
            UserRepository repository,
            String mobileNumber,
            Set<Long> connectionIds
    ) throws SQLException {
        if (mode == BenchmarkMode.UNPOOLED) {
            long startedAt = System.nanoTime();
            try (Connection connection = DatabaseConnection.getConnection()) {
                User user = repository.findByMobileNumber(
                        connection,
                        mobileNumber
                );
                long elapsed = System.nanoTime() - startedAt;
                recordConnectionId(connection, connectionIds);
                return new LookupResult(user != null, elapsed);
            }
        }

        long startedAt = System.nanoTime();
        User user = DatabaseConnection.withReusableConnection(
                connection -> repository.findByMobileNumber(
                        connection,
                        mobileNumber
                )
        );
        long elapsed = System.nanoTime() - startedAt;

        if (connectionIds != null) {
            DatabaseConnection.withReusableConnection(connection -> {
                recordConnectionId(connection, connectionIds);
                return null;
            });
        }
        return new LookupResult(user != null, elapsed);
    }

    private static void recordConnectionId(
            Connection connection,
            Set<Long> connectionIds
    ) throws SQLException {
        if (connectionIds == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CONNECTION_ID()"
        ); ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                connectionIds.add(resultSet.getLong(1));
            }
        }
    }

    private static void printResults(
            BenchmarkMode mode,
            int iterations,
            String mobileNumber,
            int foundUsers,
            List<Long> elapsedNanoseconds,
            Set<Long> connectionIds
    ) {
        List<Long> sorted = new ArrayList<>(elapsedNanoseconds);
        Collections.sort(sorted);

        long totalNanoseconds = elapsedNanoseconds.stream()
                .mapToLong(Long::longValue)
                .sum();
        double totalMilliseconds = nanosToMilliseconds(totalNanoseconds);
        double averageMilliseconds = totalMilliseconds / iterations;
        double medianMilliseconds = nanosToMilliseconds(
                percentile(sorted, 0.50)
        );
        double p95Milliseconds = nanosToMilliseconds(
                percentile(sorted, 0.95)
        );
        double throughput = iterations / (totalNanoseconds / 1_000_000_000.0);

        System.out.println("JCash JDBC performance benchmark");
        System.out.println("Mode: " + mode.commandName);
        System.out.println("Mobile number: " + mobileNumber);
        System.out.println("Warm-up iterations: " + WARM_UP_ITERATIONS);
        System.out.println("Measured iterations: " + iterations);
        System.out.println("Successful lookups: " + foundUsers);
        System.out.println("Unique JDBC connection IDs: "
                + connectionIds.size());
        System.out.printf(Locale.ROOT, "Total: %.3f ms%n", totalMilliseconds);
        System.out.printf(
                Locale.ROOT,
                "Average: %.3f ms/lookup%n",
                averageMilliseconds
        );
        System.out.printf(
                Locale.ROOT,
                "Median: %.3f ms/lookup%n",
                medianMilliseconds
        );
        System.out.printf(
                Locale.ROOT,
                "P95: %.3f ms/lookup%n",
                p95Milliseconds
        );
        System.out.printf(
                Locale.ROOT,
                "Throughput: %.2f lookups/second%n",
                throughput
        );
    }

    private static long percentile(List<Long> sortedValues, double percentile) {
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(index, 0));
    }

    private static double nanosToMilliseconds(long nanoseconds) {
        return nanoseconds / 1_000_000.0;
    }

    private static BenchmarkMode readMode(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "Mode is required: unpooled or reused"
            );
        }

        String mode = args[0].trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "unpooled" -> BenchmarkMode.UNPOOLED;
            case "reused" -> BenchmarkMode.REUSED;
            default -> throw new IllegalArgumentException(
                    "Unsupported mode '" + args[0]
                            + "'. Use unpooled or reused"
            );
        };
    }

    private static int readIterations(String[] args) {
        if (args.length < 2) {
            return DEFAULT_ITERATIONS;
        }

        try {
            int iterations = Integer.parseInt(args[1]);
            if (iterations <= 0) {
                throw new NumberFormatException();
            }
            return iterations;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Iteration count must be a positive whole number"
            );
        }
    }

    private static String getBenchmarkMobileNumber() {
        String configured = System.getenv("JCASH_BENCHMARK_MOBILE");
        String mobileNumber = configured == null
                ? DEFAULT_MOBILE_NUMBER : configured.trim();
        if (!mobileNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                    "JCASH_BENCHMARK_MOBILE must contain 11 digits "
                            + "and start with 09"
            );
        }
        return mobileNumber;
    }

    private enum BenchmarkMode {
        UNPOOLED("unpooled"),
        REUSED("reused");

        private final String commandName;

        BenchmarkMode(String commandName) {
            this.commandName = commandName;
        }
    }

    private record LookupResult(boolean userFound, long elapsedNanoseconds) {
    }
}
