package shared.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/jcash_db";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;
    private static volatile HikariDataSource reusablePool;
    private static volatile java.util.function.Function<String, String> settings = System::getenv;

    /** Spring supplies environment variables and optional local .env properties. */
    public static void configureSettings(java.util.function.Function<String, String> resolver) {
        settings = java.util.Objects.requireNonNull(resolver);
    }

    private DatabaseConnection() {
        // Utility class
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                getSetting("JCASH_DB_URL", DEFAULT_URL),
                getSetting("JCASH_DB_USER", DEFAULT_USERNAME),
                getSetting("JCASH_DB_PASSWORD", DEFAULT_PASSWORD)
        );
    }

    /**
     * Borrows a connection for one short repository operation. The callback
     * runs outside the initialization monitor, so independent requests can
     * execute concurrently up to the configured pool limit. Closing the lease
     * returns it to the pool, including after callback failure. Money services
     * retain dedicated connections and their existing JDBC transactions.
     */
    public static <T> T withReusableConnection(
            ConnectionOperation<T> operation
    ) throws SQLException {
        if (operation == null) {
            throw new IllegalArgumentException(
                    "Connection operation cannot be null"
            );
        }

        try (Connection connection = getOrOpenPool().getConnection()) {
            return operation.execute(connection);
        }
    }

    public static synchronized void verifyConnection() throws SQLException {
        withReusableConnection(connection -> {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new SQLException("Database connection validation failed");
            }
            CredentialMigration.migrate(connection);
            return null;
        });
    }

    public static synchronized void shutdown() {
        HikariDataSource pool = reusablePool;
        reusablePool = null;
        if (pool != null) pool.close();
    }

    private static HikariDataSource getOrOpenPool() throws SQLException {
        HikariDataSource pool = reusablePool;
        if (pool == null) {
            synchronized (DatabaseConnection.class) {
                pool = reusablePool;
                if (pool == null) {
                    HikariConfig config = new HikariConfig();
                    config.setPoolName("JCashRepositoryPool");
                    config.setJdbcUrl(getSetting("JCASH_DB_URL", DEFAULT_URL));
                    config.setUsername(getSetting("JCASH_DB_USER", DEFAULT_USERNAME));
                    config.setPassword(getSetting("JCASH_DB_PASSWORD", DEFAULT_PASSWORD));
                    config.setMaximumPoolSize(integerSetting("JCASH_DB_POOL_SIZE", 5, 1, 20));
                    config.setConnectionTimeout(integerSetting("JCASH_DB_POOL_TIMEOUT_MS", 2000, 250, 60000));
                    config.setValidationTimeout(1000);
                    try {
                        pool = new HikariDataSource(config);
                    } catch (RuntimeException exception) {
                        throw new SQLException("Could not initialize the repository connection pool", exception);
                    }
                    reusablePool = pool;
                }
            }
        }
        return pool;
    }

    private static int integerSetting(String key, int fallback, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(getSetting(key, Integer.toString(fallback)));
            if (value >= minimum && value <= maximum) return value;
        } catch (NumberFormatException ignored) {
            // Do not expose configuration values in errors.
        }
        throw new IllegalArgumentException(key + " must be between " + minimum + " and " + maximum);
    }

    private static String getSetting(String environmentName, String fallback) {
        String value = settings.apply(environmentName);
        return value == null ? fallback : value;
    }

    @FunctionalInterface
    public interface ConnectionOperation<T> {

        T execute(Connection connection) throws SQLException;
    }
}
