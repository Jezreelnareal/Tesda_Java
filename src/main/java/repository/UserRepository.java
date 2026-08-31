package repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.User;
import util.DatabaseConnection;

public class UserRepository {

    public void save(User user) throws SQLException {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        String sql = """
                INSERT INTO users
                    (mobile_number, pin, full_name, balance)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getMobileNumber());
            statement.setString(2, user.getPin());
            statement.setString(3, user.getFullName());
            statement.setBigDecimal(4, user.getBalance());
            statement.executeUpdate();
        }
    }

    public User findByMobileNumber(String mobileNumber) throws SQLException {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Mobile number cannot be null or empty"
            );
        }

        String sql = """
                SELECT mobile_number, pin, full_name, balance
                FROM users
                WHERE mobile_number = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, mobileNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                User user = new User(
                        resultSet.getString("full_name"),
                        resultSet.getString("mobile_number"),
                        resultSet.getString("pin")
                );

                BigDecimal savedBalance = resultSet.getBigDecimal("balance");
                if (savedBalance != null
                        && savedBalance.compareTo(BigDecimal.ZERO) > 0) {
                    user.deposit(savedBalance);
                }

                return user;
            }
        }
    }
}
