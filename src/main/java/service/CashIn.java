package service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import model.CashInTransaction;
import model.User;
import repository.TransactionRepository;
import repository.UserRepository;
import util.DatabaseConnection;

public final class CashIn {

    private static final BigDecimal MAXIMUM_BALANCE =
            new BigDecimal("9999999999999.99");

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public CashIn() {
        this(new UserRepository(), new TransactionRepository());
    }

    public CashIn(
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        if (userRepository == null || transactionRepository == null) {
            throw new IllegalArgumentException(
                    "Repositories cannot be null"
            );
        }
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public User cashIn(User user, BigDecimal amount) throws SQLException {
        requireUser(user);
        requireValidAmount(amount);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                User lockedUser = userRepository.findByMobileNumberForUpdate(
                        connection,
                        user.getMobileNumber()
                );
                if (lockedUser == null) {
                    throw new SQLException("Cash-in user no longer exists");
                }
                if (lockedUser.getBalance().add(amount)
                        .compareTo(MAXIMUM_BALANCE) > 0) {
                    throw new IllegalArgumentException(
                            "Cash-in would exceed the account balance limit"
                    );
                }

                lockedUser.deposit(amount);
                userRepository.updateBalance(connection, lockedUser);

                CashInTransaction pendingTransaction =
                        new CashInTransaction(
                                lockedUser.getMobileNumber(),
                                amount,
                                "Cash-in"
                        );
                long transactionId = transactionRepository.save(
                        connection,
                        pendingTransaction
                );
                CashInTransaction savedTransaction =
                        new CashInTransaction(
                                transactionId,
                                lockedUser.getMobileNumber(),
                                amount,
                                pendingTransaction.getDetails(),
                                pendingTransaction.getDateTime()
                        );

                connection.commit();
                lockedUser.addTransaction(savedTransaction);
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

    private static void requireValidAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places"
            );
        }
        if (amount.compareTo(MAXIMUM_BALANCE) > 0) {
            throw new IllegalArgumentException("Amount exceeds the allowed limit");
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
