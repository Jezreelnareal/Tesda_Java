package service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import model.TransferTransaction;
import model.User;
import repository.TransactionRepository;
import repository.UserRepository;
import util.DatabaseConnection;

public final class Transfer {

    private static final BigDecimal MAXIMUM_BALANCE =
            new BigDecimal("9999999999999.99");

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public Transfer() {
        this(new UserRepository(), new TransactionRepository());
    }

    public Transfer(
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

    public User transfer(
            User sender,
            String receiverMobileNumber,
            BigDecimal amount
    ) throws SQLException {
        requireUser(sender);
        requireMobileNumber(receiverMobileNumber);
        requireValidAmount(amount);

        String senderMobileNumber = sender.getMobileNumber();
        if (senderMobileNumber.equals(receiverMobileNumber)) {
            throw new IllegalArgumentException(
                    "You cannot transfer money to your own account"
            );
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                String firstMobileNumber = senderMobileNumber.compareTo(
                        receiverMobileNumber
                ) <= 0 ? senderMobileNumber : receiverMobileNumber;
                String secondMobileNumber = firstMobileNumber.equals(
                        senderMobileNumber
                ) ? receiverMobileNumber : senderMobileNumber;

                User firstUser = userRepository.findByMobileNumberForUpdate(
                        connection,
                        firstMobileNumber
                );
                User secondUser = userRepository.findByMobileNumberForUpdate(
                        connection,
                        secondMobileNumber
                );

                User lockedSender = senderMobileNumber.equals(firstMobileNumber)
                        ? firstUser : secondUser;
                User lockedReceiver = receiverMobileNumber.equals(
                        firstMobileNumber
                ) ? firstUser : secondUser;

                if (lockedSender == null) {
                    throw new SQLException("Transfer sender no longer exists");
                }
                if (lockedReceiver == null) {
                    throw new IllegalArgumentException("Receiver does not exist");
                }
                if (lockedReceiver.getBalance().add(amount)
                        .compareTo(MAXIMUM_BALANCE) > 0) {
                    throw new IllegalArgumentException(
                            "Transfer would exceed the receiver balance limit"
                    );
                }

                lockedSender.withdraw(amount);
                lockedReceiver.deposit(amount);
                userRepository.updateBalance(connection, lockedSender);
                userRepository.updateBalance(connection, lockedReceiver);

                String details = "Transfer from " + senderMobileNumber
                        + " to " + receiverMobileNumber;
                TransferTransaction pendingTransaction =
                        new TransferTransaction(
                                senderMobileNumber,
                                receiverMobileNumber,
                                amount,
                                details
                        );
                long transactionId = transactionRepository.save(
                        connection,
                        pendingTransaction
                );
                TransferTransaction savedTransaction =
                        new TransferTransaction(
                                transactionId,
                                senderMobileNumber,
                                receiverMobileNumber,
                                amount,
                                details,
                                pendingTransaction.getDateTime()
                        );

                connection.commit();
                lockedSender.addTransaction(savedTransaction);
                return lockedSender;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Sender cannot be null");
        }
    }

    private static void requireMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                    "Receiver mobile number must contain 11 digits and start with 09"
            );
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
