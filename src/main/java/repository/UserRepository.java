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
        requireUser(user);

        String sql = """
                INSERT INTO users
                    (mobile_number, pin, full_name, balance)
                VALUES (?, ?, ?, ?)
                """;

        DatabaseConnection.withReusableConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, user.getMobileNumber());
                statement.setString(2, user.getPin());
                statement.setString(3, user.getFullName());
                statement.setBigDecimal(4, user.getBalance());
                statement.executeUpdate();
                return null;
            }
        });
    }

    public User findByMobileNumber(String mobileNumber) throws SQLException {
        requireMobileNumber(mobileNumber);

        return DatabaseConnection.withReusableConnection(
                connection -> findByMobileNumber(
                        connection,
                        mobileNumber,
                        false
                )
        );
    }

    public User findByMobileNumber(
            Connection connection,
            String mobileNumber
    ) throws SQLException {
        requireConnection(connection);
        requireMobileNumber(mobileNumber);
        return findByMobileNumber(connection, mobileNumber, false);
    }

    public User findByMobileNumberForUpdate(
            Connection connection,
            String mobileNumber
    ) throws SQLException {
        requireConnection(connection);
        requireMobileNumber(mobileNumber);
        return findByMobileNumber(connection, mobileNumber, true);
    }

    public void updateBalance(Connection connection, User user)
            throws SQLException {
        requireConnection(connection);
        requireUser(user);

        String sql = """
                UPDATE users
                SET balance = ?
                WHERE mobile_number = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, user.getBalance());
            statement.setString(2, user.getMobileNumber());

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Expected to update exactly one user balance"
                );
            }
        }
    }

    private static User findByMobileNumber(
            Connection connection,
            String mobileNumber,
            boolean lockForUpdate
    ) throws SQLException {
        String sql = """
                SELECT mobile_number, pin, full_name, balance
                FROM users
                WHERE mobile_number = ?
                """ + (lockForUpdate ? " FOR UPDATE" : "");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, mobileNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    private static User mapUser(ResultSet resultSet) throws SQLException {
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

    private static void requireConnection(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }

    private static void requireMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Mobile number cannot be null or empty"
            );
        }
    }
}
