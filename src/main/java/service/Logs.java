package service;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import model.CashInTransaction;
import model.AdminCreditTransaction;
import model.AdminDebitTransaction;
import model.Transaction;
import model.TransferTransaction;
import model.WithdrawalTransaction;
import model.User;
import repository.TransactionRepository;

public final class Logs {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransactionRepository transactionRepository;
    private final Balance balance;

    public Logs() {
        this(new TransactionRepository(), new Balance());
    }

    public Logs(
            TransactionRepository transactionRepository,
            Balance balance
    ) {
        if (transactionRepository == null || balance == null) {
            throw new IllegalArgumentException(
                    "Transaction repository and balance service cannot be null"
            );
        }
        this.transactionRepository = transactionRepository;
        this.balance = balance;
    }

    public List<Transaction> getTransactions(User user) throws SQLException {
        requireUser(user);
        return transactionRepository.findByUserMobileNumber(
                user.getMobileNumber()
        );
    }

    public List<String> getFormattedLogs(User user) throws SQLException {
        List<String> formattedLogs = new ArrayList<>();
        for (Transaction transaction : getTransactions(user)) {
            formattedLogs.add(formatTransaction(user, transaction));
        }
        return List.copyOf(formattedLogs);
    }

    private String formatTransaction(User user, Transaction transaction) {
        String type;
        String amountPrefix;
        String participantDetails = "";

        if (transaction instanceof CashInTransaction) {
            type = "CASH_IN";
            amountPrefix = "+";
        } else if (transaction instanceof WithdrawalTransaction) {
            type = "WITHDRAWAL";
            amountPrefix = "-";
        } else if (transaction instanceof AdminCreditTransaction credit) {
            type = "ADMIN CREDIT";
            amountPrefix = "+";
            participantDetails = " | Admin: "
                    + credit.getAdminUsername();
        } else if (transaction instanceof AdminDebitTransaction debit) {
            type = "ADMIN DEBIT";
            amountPrefix = "-";
            participantDetails = " | Admin: "
                    + debit.getAdminUsername();
        } else if (transaction instanceof TransferTransaction transfer) {
            if (user.getMobileNumber().equals(
                    transfer.getSenderMobileNumber()
            )) {
                type = "TRANSFER SENT";
                amountPrefix = "-";
                participantDetails = " | To: "
                        + transfer.getReceiverMobileNumber();
            } else {
                type = "TRANSFER RECEIVED";
                amountPrefix = "+";
                participantDetails = " | From: "
                        + transfer.getSenderMobileNumber();
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported transaction class: "
                    + transaction.getClass().getName()
            );
        }

        return transaction.getDateTime().format(DATE_TIME_FORMAT)
                + " | " + type
                + " | " + amountPrefix
                + balance.formatAmount(transaction.getAmount())
                + " | " + transaction.getDetails()
                + participantDetails;
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
    }
}
