package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Upgrades databases created before PIN hashing was introduced.
 */
public final class CredentialMigration {

    private static final int PIN_COLUMN_LENGTH = 255;

    private CredentialMigration() {
        // Utility class
    }

    public static void migrate(Connection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }

        widenPinColumnIfRequired(connection, "users");
        widenPinColumnIfRequired(connection, "admins");

        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            migrateTable(connection, "users", "mobile_number");
            migrateTable(connection, "admins", "username");
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            if (exception instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Could not secure stored PINs", exception);
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private static void widenPinColumnIfRequired(
            Connection connection,
            String tableName
    ) throws SQLException {
        String lookupSql = """
                SELECT CHARACTER_MAXIMUM_LENGTH
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = 'pin'
                """;
        long currentLength;
        try (PreparedStatement statement = connection.prepareStatement(
                lookupSql
        )) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException(
                            "Missing PIN column in table " + tableName
                    );
                }
                currentLength = resultSet.getLong(1);
            }
        }

        if (currentLength >= PIN_COLUMN_LENGTH) {
            return;
        }

        String alterSql = "ALTER TABLE " + tableName
                + " MODIFY COLUMN pin VARCHAR(" + PIN_COLUMN_LENGTH
                + ") NOT NULL";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(alterSql);
        }
    }

    private static void migrateTable(
            Connection connection,
            String tableName,
            String keyColumn
    ) throws SQLException {
        String selectSql = "SELECT " + keyColumn + ", pin FROM " + tableName;
        List<LegacyCredential> legacyCredentials = new ArrayList<>();

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(selectSql)) {
            while (resultSet.next()) {
                String key = resultSet.getString(keyColumn);
                String storedPin = resultSet.getString("pin");
                if (PinHasher.isEncodedHash(storedPin)) {
                    continue;
                }
                if (!PinHasher.isLegacyPlaintextPin(storedPin)) {
                    throw new SQLException(
                            "Unsupported PIN value for " + tableName
                                    + " record " + key
                    );
                }
                legacyCredentials.add(new LegacyCredential(key, storedPin));
            }
        }

        if (legacyCredentials.isEmpty()) {
            return;
        }

        String updateSql = "UPDATE " + tableName + " SET pin = ? WHERE "
                + keyColumn + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(
                updateSql
        )) {
            for (LegacyCredential credential : legacyCredentials) {
                statement.setString(1, PinHasher.hash(credential.pin()));
                statement.setString(2, credential.key());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private record LegacyCredential(String key, String pin) {
    }
}
