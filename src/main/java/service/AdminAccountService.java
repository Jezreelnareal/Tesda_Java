package service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import model.Admin;
import model.AdminCreditTransaction;
import model.AdminDebitTransaction;
import model.BalanceChangeReceipt;
import model.SystemReport;
import model.Transaction;
import model.TransactionTotals;
import model.TransactionType;
import model.User;
import repository.TransactionRepository;
import repository.UserRepository;
import util.DatabaseConnection;

public final class AdminAccountService {

    private static final BigDecimal MAXIMUM_BALANCE =
            new BigDecimal("9999999999999.99");
    private static final int REPORT_TRANSACTION_LIMIT = 100;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public AdminAccountService() {
        this(new UserRepository(), new TransactionRepository());
    }

    public AdminAccountService(
            UserRepository userRepository,
            TransactionRepository transactionRepository
    ) {
        if (userRepository == null || transactionRepository == null) {
            throw new IllegalArgumentException("Repositories cannot be null");
        }
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<User> listAccounts() throws SQLException {
        return userRepository.findAll();
    }

    public User findAccount(String mobileNumber) throws SQLException {
        requireMobileNumber(mobileNumber);
        return userRepository.findByMobileNumber(mobileNumber);
    }

    public User createAccount(
            String fullName,
            String mobileNumber,
            String pin
    ) throws SQLException {
        User user = new User(fullName, mobileNumber, pin);
        if (userRepository.findByMobileNumber(mobileNumber) != null) {
            throw new IllegalArgumentException(
                    "That mobile number is already registered"
            );
        }
        userRepository.save(user);
        return user;
    }

    public BalanceChangeReceipt creditAccount(
            Admin admin,
            String mobileNumber,
            BigDecimal amount
    ) throws SQLException {
        return adjustBalance(admin, mobileNumber, amount, true);
    }

    public BalanceChangeReceipt debitAccount(
            Admin admin,
            String mobileNumber,
            BigDecimal amount
    ) throws SQLException {
        return adjustBalance(admin, mobileNumber, amount, false);
    }

    public SystemReport generateSystemReport() throws SQLException {
        List<User> users = userRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();
        BigDecimal combinedBalance = users.stream()
                .map(User::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<TransactionType, TransactionTotals> totals =
                new EnumMap<>(TransactionType.class);

        for (Transaction transaction : transactions) {
            TransactionTotals previous = totals.getOrDefault(
                    transaction.getType(),
                    new TransactionTotals(0, BigDecimal.ZERO)
            );
            totals.put(
                    transaction.getType(),
                    new TransactionTotals(
                            previous.count() + 1,
                            previous.amount().add(transaction.getAmount())
                    )
            );
        }

        List<Transaction> recent = new ArrayList<>(transactions.subList(
                0,
                Math.min(REPORT_TRANSACTION_LIMIT, transactions.size())
        ));
        return new SystemReport(
                users.size(),
                combinedBalance,
                totals,
                recent
        );
    }

    private BalanceChangeReceipt adjustBalance(
            Admin admin,
            String mobileNumber,
            BigDecimal amount,
            boolean credit
    ) throws SQLException {
        if (admin == null) {
            throw new IllegalArgumentException("Admin session is required");
        }
        requireMobileNumber(mobileNumber);
        requireAmount(amount);

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                User user = userRepository.findByMobileNumberForUpdate(
                        connection,
                        mobileNumber
                );
                if (user == null) {
                    throw new IllegalArgumentException("Account was not found");
                }

                BigDecimal previousBalance = user.getBalance();
                Transaction pending;
                if (credit) {
                    if (previousBalance.add(amount)
                            .compareTo(MAXIMUM_BALANCE) > 0) {
                        throw new IllegalArgumentException(
                                "Adjustment would exceed the balance limit"
                        );
                    }
                    user.deposit(amount);
                    pending = new AdminCreditTransaction(
                            admin.username(),
                            mobileNumber,
                            amount,
                            "Admin credit by " + admin.username()
                    );
                } else {
                    user.withdraw(amount);
                    pending = new AdminDebitTransaction(
                            admin.username(),
                            mobileNumber,
                            amount,
                            "Admin debit by " + admin.username()
                    );
                }

                userRepository.updateBalance(connection, user);
                long id = transactionRepository.save(connection, pending);
                Transaction saved = credit
                        ? new AdminCreditTransaction(
                                id,
                                admin.username(),
                                mobileNumber,
                                amount,
                                pending.getDetails(),
                                pending.getDateTime()
                        )
                        : new AdminDebitTransaction(
                                id,
                                admin.username(),
                                mobileNumber,
                                amount,
                                pending.getDetails(),
                                pending.getDateTime()
                        );
                connection.commit();
                user.addTransaction(saved);
                return new BalanceChangeReceipt(
                        user,
                        previousBalance,
                        saved
                );
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private static void requireMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                    "Mobile number must contain 11 digits and start with 09"
            );
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
        if (amount.compareTo(MAXIMUM_BALANCE) > 0) {
            throw new IllegalArgumentException("Amount exceeds the limit");
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
