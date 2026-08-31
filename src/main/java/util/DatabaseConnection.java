package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/jcash_db";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final int VALIDATION_TIMEOUT_SECONDS = 2;
    private static Connection reusableConnection;

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
     * Executes one short database operation with the application's reusable
     * connection. JCash is a single-threaded console application, so one
     * synchronized connection avoids repeated connection handshakes without
     * introducing an external connection-pool dependency.
     */
    public static synchronized <T> T withReusableConnection(
            ConnectionOperation<T> operation
    ) throws SQLException {
        if (operation == null) {
            throw new IllegalArgumentException(
                    "Connection operation cannot be null"
            );
        }

        Connection connection = getOrOpenReusableConnection();
        try {
            return operation.execute(connection);
        } catch (SQLException exception) {
            discardIfInvalid(connection);
            throw exception;
        }
    }

    public static void verifyConnection() throws SQLException {
        withReusableConnection(connection -> {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new SQLException("Database connection validation failed");
            }
            return null;
        });
    }

    public static synchronized void shutdown() {
        closeQuietly(reusableConnection);
        reusableConnection = null;
    }

    private static Connection getOrOpenReusableConnection()
            throws SQLException {
        if (reusableConnection == null || reusableConnection.isClosed()) {
            reusableConnection = getConnection();
        }
        return reusableConnection;
    }

    private static void discardIfInvalid(Connection connection) {
        try {
            if (connection.isClosed()
                    || !connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                closeQuietly(connection);
                if (connection == reusableConnection) {
                    reusableConnection = null;
                }
            }
        } catch (SQLException ignored) {
            closeQuietly(connection);
            if (connection == reusableConnection) {
                reusableConnection = null;
            }
        }
    }

    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Shutdown must remain safe even if MySQL is already unavailable.
        }
    }

    private static String getSetting(String environmentName, String fallback) {
        String value = System.getenv(environmentName);
        return value == null ? fallback : value;
    }

    @FunctionalInterface
    public interface ConnectionOperation<T> {

        T execute(Connection connection) throws SQLException;
    }
}
