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

    public static void verifyConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new SQLException("Database connection validation failed");
            }
        }
    }

    private static String getSetting(String environmentName, String fallback) {
        String value = System.getenv(environmentName);
        return value == null ? fallback : value;
    }
}
