package service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import model.User;
import model.WithdrawalTransaction;
import repository.TransactionRepository;
import repository.UserRepository;
import util.DatabaseConnection;

public final class Withdrawal {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public Withdrawal() {
        this(new UserRepository(), new TransactionRepository());
    }

    public Withdrawal(
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        if (userRepository == null || transactionRepository == null) {
            throw new IllegalArgumentException("Repositories cannot be null");
        }
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public User withdraw(User user, BigDecimal amount) throws SQLException {
        requireUser(user);
        requireAmount(amount);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                User lockedUser = userRepository.findByMobileNumberForUpdate(
                        connection,
                        user.getMobileNumber()
                );
                if (lockedUser == null) {
                    throw new SQLException("Withdrawal user no longer exists");
                }

                lockedUser.withdraw(amount);
                userRepository.updateBalance(connection, lockedUser);
                WithdrawalTransaction pending = new WithdrawalTransaction(
                        lockedUser.getMobileNumber(),
                        amount,
                        "Cash withdrawal"
                );
                long id = transactionRepository.save(connection, pending);
                WithdrawalTransaction saved = new WithdrawalTransaction(
                        id,
                        lockedUser.getMobileNumber(),
                        amount,
                        pending.getDetails(),
                        pending.getDateTime()
                );
                connection.commit();
                lockedUser.addTransaction(saved);
                return lockedUser;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }

    private static void requireAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places"
            );
        }
    }

    private static void rollback(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            cause.addSuppressed(rollbackException);
        }
    }
}
