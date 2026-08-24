package repository;

import model.User;
import util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserRepository {

    public void save(User user) throws SQLException {
        String sql = """
            INSERT INTO users
                (mobile_number, pin, full_name, balance)
            VALUES (?, ?, ?, ?)
            """;

        try (
                Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getMobileNumber());
            statement.setString(2, user.getPin());
            statement.setString(3, user.getFullName());
            statement.setBigDecimal(4, user.getBalance());

            statement.executeUpdate();
        }
    }
}
