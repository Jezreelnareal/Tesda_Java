package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.AdminCreditTransaction;
import model.AdminDebitTransaction;
import model.CashInTransaction;
import model.Transaction;
import model.TransactionType;
import model.TransferTransaction;
import model.WithdrawalTransaction;
import util.DatabaseConnection;

public class TransactionRepository {

    public long save(Transaction transaction) throws SQLException {
        requireTransaction(transaction);

        return DatabaseConnection.withReusableConnection(
                connection -> save(connection, transaction)
        );
    }

    public long save(Connection connection, Transaction transaction)
            throws SQLException {
        requireConnection(connection);
        requireTransaction(transaction);

        String sql = """
                INSERT INTO transactions
                    (transaction_type, amount, details,
                     transaction_date_time, sender_mobile_number,
                     receiver_mobile_number, admin_username)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, transaction.getType().name());
            statement.setBigDecimal(2, transaction.getAmount());
            statement.setString(3, transaction.getDetails());
            statement.setTimestamp(
                    4,
                    Timestamp.valueOf(transaction.getDateTime())
            );
            setParticipants(statement, transaction);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException(
                            "Transaction was saved without a generated ID"
                    );
                }
                return generatedKeys.getLong(1);
            }
        }
    }

    public List<Transaction> findByUserMobileNumber(String mobileNumber)
            throws SQLException {
        requireMobileNumber(mobileNumber);

        String sql = """
                SELECT id, transaction_type, amount, details,
                       transaction_date_time, sender_mobile_number,
                       receiver_mobile_number, admin_username
                FROM transactions
                WHERE sender_mobile_number = ? OR receiver_mobile_number = ?
                ORDER BY transaction_date_time DESC, id DESC
                """;

        List<Transaction> transactions = new ArrayList<>();

        DatabaseConnection.withReusableConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, mobileNumber);
                statement.setString(2, mobileNumber);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(mapTransaction(resultSet));
                    }
                }
                return null;
            }
        });

        return List.copyOf(transactions);
    }

    public List<Transaction> findAll() throws SQLException {
        String sql = """
                SELECT id, transaction_type, amount, details,
                       transaction_date_time, sender_mobile_number,
                       receiver_mobile_number, admin_username
                FROM transactions
                ORDER BY transaction_date_time DESC, id DESC
                """;

        List<Transaction> transactions = new ArrayList<>();

        DatabaseConnection.withReusableConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapTransaction(resultSet));
                }
                return null;
            }
        });

        return List.copyOf(transactions);
    }

    public boolean deleteById(long transactionId) throws SQLException {
        requireTransactionId(transactionId);

        String sql = "DELETE FROM transactions WHERE id = ?";
        return DatabaseConnection.withReusableConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, transactionId);
                return statement.executeUpdate() == 1;
            }
        });
    }

    private static void setParticipants(
            PreparedStatement statement,
            Transaction transaction
    ) throws SQLException {
        if (transaction instanceof CashInTransaction cashIn) {
            statement.setNull(5, Types.VARCHAR);
            statement.setString(6, cashIn.getUserMobileNumber());
            statement.setNull(7, Types.VARCHAR);
            return;
        }

        if (transaction instanceof WithdrawalTransaction withdrawal) {
            statement.setString(5, withdrawal.getUserMobileNumber());
            statement.setNull(6, Types.VARCHAR);
            statement.setNull(7, Types.VARCHAR);
            return;
        }

        if (transaction instanceof TransferTransaction transfer) {
            statement.setString(5, transfer.getSenderMobileNumber());
            statement.setString(6, transfer.getReceiverMobileNumber());
            statement.setNull(7, Types.VARCHAR);
            return;
        }

        if (transaction instanceof AdminCreditTransaction credit) {
            statement.setNull(5, Types.VARCHAR);
            statement.setString(6, credit.getUserMobileNumber());
            statement.setString(7, credit.getAdminUsername());
            return;
        }

        if (transaction instanceof AdminDebitTransaction debit) {
            statement.setString(5, debit.getUserMobileNumber());
            statement.setNull(6, Types.VARCHAR);
            statement.setString(7, debit.getAdminUsername());
            return;
        }

        throw new IllegalArgumentException(
                "Unsupported transaction class: "
                + transaction.getClass().getName()
        );
    }

    private static Transaction mapTransaction(ResultSet resultSet)
            throws SQLException {
        long id = resultSet.getLong("id");
        TransactionType type;

        try {
            type = TransactionType.valueOf(
                    resultSet.getString("transaction_type")
            );
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException(
                    "Unsupported transaction type for record " + id,
                    exception
            );
        }

        try {
            return switch (type) {
                case CASH_IN -> new CashInTransaction(
                        id,
                        resultSet.getString("receiver_mobile_number"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("details"),
                        resultSet.getTimestamp("transaction_date_time")
                                .toLocalDateTime()
                );
                case WITHDRAWAL -> new WithdrawalTransaction(
                        id,
                        resultSet.getString("sender_mobile_number"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("details"),
                        resultSet.getTimestamp("transaction_date_time")
                                .toLocalDateTime()
                );
                case TRANSFER -> new TransferTransaction(
                        id,
                        resultSet.getString("sender_mobile_number"),
                        resultSet.getString("receiver_mobile_number"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("details"),
                        resultSet.getTimestamp("transaction_date_time")
                                .toLocalDateTime()
                );
                case ADMIN_CREDIT -> new AdminCreditTransaction(
                        id,
                        resultSet.getString("admin_username"),
                        resultSet.getString("receiver_mobile_number"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("details"),
                        resultSet.getTimestamp("transaction_date_time")
                                .toLocalDateTime()
                );
                case ADMIN_DEBIT -> new AdminDebitTransaction(
                        id,
                        resultSet.getString("admin_username"),
                        resultSet.getString("sender_mobile_number"),
                        resultSet.getBigDecimal("amount"),
                        resultSet.getString("details"),
                        resultSet.getTimestamp("transaction_date_time")
                                .toLocalDateTime()
                );
            };
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new SQLException(
                    "Invalid transaction data for record " + id,
                    exception
            );
        }
    }

    private static void requireConnection(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
    }

    private static void requireTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }
    }

    private static void requireMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                    "Mobile number must contain 11 digits and start with 09"
            );
        }
    }

    private static void requireTransactionId(long transactionId) {
        if (transactionId <= 0) {
            throw new IllegalArgumentException(
                    "Transaction ID must be positive"
            );
        }
    }
}
