package admin.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import admin.model.Admin;
import shared.util.DatabaseConnection;

public class AdminRepository {

    public Admin findByUsername(String username) throws SQLException {
        requireUsername(username);
        String sql = "SELECT username, pin FROM admins WHERE username = ?";

        return DatabaseConnection.withReusableConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username.trim());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next()
                            ? new Admin(
                                    resultSet.getString("username"),
                                    resultSet.getString("pin")
                            )
                            : null;
                }
            }
        });
    }

    private static void requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username is required");
        }
    }
}
