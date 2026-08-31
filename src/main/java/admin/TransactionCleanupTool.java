package admin;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import model.CashInTransaction;
import model.Transaction;
import model.TransferTransaction;
import repository.TransactionRepository;
import service.Balance;
import util.DatabaseConnection;
import util.InputValidator;

public final class TransactionCleanupTool {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TransactionCleanupTool() {
        // Utility entry point
    }

    public static void main(String[] args) {
        try {
            if (!verifyDatabaseConnection()) {
                return;
            }

            try (Scanner scanner = new Scanner(System.in)) {
                runCleanup(
                        scanner,
                        new TransactionRepository(),
                        new Balance()
                );
            }
        } finally {
            DatabaseConnection.shutdown();
        }
    }

    private static boolean verifyDatabaseConnection() {
        try {
            DatabaseConnection.verifyConnection();
            return true;
        } catch (SQLException exception) {
            System.out.println("Unable to connect to the JCash database.");
            System.out.println(
                    "Start MySQL and verify the database configuration."
            );
            return false;
        }
    }

    private static void runCleanup(
            Scanner scanner,
            TransactionRepository repository,
            Balance balance
    ) {
        List<Transaction> transactions;
        try {
            transactions = repository.findAll();
        } catch (SQLException exception) {
            System.out.println("Unable to load transaction records.");
            return;
        }

        System.out.println("JCash transaction cleanup utility");
        if (transactions.isEmpty()) {
            System.out.println("No transaction records found.");
            return;
        }

        for (Transaction transaction : transactions) {
            System.out.println(formatTransaction(transaction, balance));
        }

        Long transactionId = InputValidator.readPositiveLong(
                scanner,
                "Transaction ID to delete: "
        );
        if (transactionId == null) {
            System.out.println("Deletion cancelled.");
            return;
        }

        System.out.println(
                "Warning: this permanently deletes the log only. "
                + "Account balances will not change."
        );
        String confirmation = InputValidator.readTrimmedLine(
                scanner,
                "Type DELETE to confirm: "
        );
        if (!"DELETE".equals(confirmation)) {
            System.out.println("Deletion cancelled.");
            return;
        }

        try {
            if (repository.deleteById(transactionId)) {
                System.out.println(
                        "Transaction " + transactionId + " deleted successfully."
                );
            } else {
                System.out.println(
                        "Transaction " + transactionId + " was not found."
                );
            }
        } catch (SQLException exception) {
            System.out.println("Unable to delete the transaction record.");
        }
    }

    private static String formatTransaction(
            Transaction transaction,
            Balance balance
    ) {
        String sender = "-";
        String receiver;

        if (transaction instanceof CashInTransaction cashIn) {
            receiver = cashIn.getUserMobileNumber();
        } else if (transaction instanceof TransferTransaction transfer) {
            sender = transfer.getSenderMobileNumber();
            receiver = transfer.getReceiverMobileNumber();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported transaction class: "
                    + transaction.getClass().getName()
            );
        }

        return "ID: " + transaction.getId()
                + " | " + transaction.getDateTime().format(DATE_TIME_FORMAT)
                + " | " + transaction.getType()
                + " | " + balance.formatAmount(transaction.getAmount())
                + " | " + transaction.getDetails()
                + " | Sender: " + sender
                + " | Receiver: " + receiver;
    }
}
