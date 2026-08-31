import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import model.User;
import service.Auth;
import service.Balance;
import service.CashIn;
import service.Logs;
import service.Transfer;
import util.DatabaseConnection;
import util.InputValidator;

public class Main {

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int LOGIN_OPTION = 1;
    private static final int VIEW_BALANCE_OPTION = 1;
    private static final int CASH_IN_OPTION = 2;
    private static final int TRANSFER_OPTION = 3;
    private static final int LOGS_OPTION = 4;
    private static final int EXIT_OPTION = 0;
    private static final int LOGOUT_OPTION = 0;

    public static void main(String[] args) {
        try {
            if (!verifyDatabaseConnection()) {
                return;
            }

            try (Scanner scanner = new Scanner(System.in)) {
                runMenu(
                        scanner,
                        new Auth(),
                        new Balance(),
                        new CashIn(),
                        new Transfer(),
                        new Logs()
                );
            }
        } finally {
            DatabaseConnection.shutdown();
        }
    }

    private static boolean verifyDatabaseConnection() {
        try {
            DatabaseConnection.verifyConnection();
            System.out.println("Database connection successful.");
            return true;
        } catch (SQLException exception) {
            System.out.println("Unable to connect to the JCash database.");
            System.out.println(
                    "Start MySQL and verify the database configuration."
            );
            return false;
        }
    }

    private static void runMenu(
            Scanner scanner,
            Auth auth,
            Balance balance,
            CashIn cashIn,
            Transfer transfer,
            Logs logs
    ) {
        System.out.println("Hello JCash");

        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("1. Login");
            System.out.println("0. Exit");

            int choice = InputValidator.readMenuChoice(
                    scanner,
                    EXIT_OPTION,
                    LOGIN_OPTION
            );

            switch (choice) {
                case LOGIN_OPTION -> running = handleLogin(
                        scanner,
                        auth,
                        balance,
                        cashIn,
                        transfer,
                        logs
                );
                case EXIT_OPTION -> {
                    System.out.println("Thank you for using JCash.");
                    running = false;
                }
                default -> throw new IllegalStateException(
                        "Unexpected menu choice: " + choice
                );
            }
        }
    }

    private static boolean handleLogin(
            Scanner scanner,
            Auth auth,
            Balance balance,
            CashIn cashIn,
            Transfer transfer,
            Logs logs
    ) {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            System.out.printf(
                    "%nLogin attempt %d of %d%n",
                    attempt,
                    MAX_LOGIN_ATTEMPTS
            );

            String mobileNumber = InputValidator.readTrimmedLine(
                    scanner,
                    "Mobile number: "
            );
            String pin = InputValidator.readTrimmedLine(scanner, "PIN: ");

            User authenticatedUser;
            try {
                authenticatedUser = auth.authenticate(mobileNumber, pin);
            } catch (SQLException exception) {
                System.out.println("Authentication is currently unavailable.");
                System.out.println("Please restart JCash and try again.");
                return false;
            }

            if (authenticatedUser != null) {
                runAuthenticatedMenu(
                        scanner,
                        authenticatedUser,
                        balance,
                        cashIn,
                        transfer,
                        logs
                );
                return true;
            }

            int remainingAttempts = MAX_LOGIN_ATTEMPTS - attempt;
            if (remainingAttempts > 0) {
                System.out.printf(
                        "Invalid mobile number or PIN. Attempts remaining: %d.%n",
                        remainingAttempts
                );
            }
        }

        System.out.println(
                "Too many failed attempts. JCash is locked for this session."
        );
        return false;
    }

    private static void runAuthenticatedMenu(
            Scanner scanner,
            User user,
            Balance balance,
            CashIn cashIn,
            Transfer transfer,
            Logs logs
    ) {
        System.out.printf(
                "%nLogin successful. Welcome, %s!%n",
                user.getFullName()
        );

        User currentUser = user;
        displayBalance(currentUser, balance);

        boolean signedIn = true;
        while (signedIn) {
            System.out.println();
            System.out.println("Signed in as: " + currentUser.getFullName());
            System.out.println("1. View balance");
            System.out.println("2. Cash in");
            System.out.println("3. Transfer");
            System.out.println("4. Transaction logs");
            System.out.println("0. Logout");

            int choice = InputValidator.readMenuChoice(
                    scanner,
                    LOGOUT_OPTION,
                    LOGS_OPTION
            );

            switch (choice) {
                case VIEW_BALANCE_OPTION -> displayBalance(currentUser, balance);
                case CASH_IN_OPTION -> currentUser = handleCashIn(
                        scanner,
                        currentUser,
                        cashIn,
                        balance
                );
                case TRANSFER_OPTION -> currentUser = handleTransfer(
                        scanner,
                        currentUser,
                        transfer,
                        balance
                );
                case LOGS_OPTION -> displayLogs(currentUser, logs);
                case LOGOUT_OPTION -> {
                    System.out.println("Logged out successfully.");
                    signedIn = false;
                }
                default -> throw new IllegalStateException(
                        "Unexpected authenticated menu choice: " + choice
                );
            }
        }
    }

    private static User handleCashIn(
            Scanner scanner,
            User user,
            CashIn cashIn,
            Balance balance
    ) {
        BigDecimal amount = InputValidator.readPositiveAmount(
                scanner,
                "Cash-in amount: "
        );
        if (amount == null) {
            System.out.println("Cash-in cancelled.");
            return user;
        }

        try {
            User updatedUser = cashIn.cashIn(user, amount);
            System.out.println("Cash-in successful.");
            displayBalance(updatedUser, balance);
            return updatedUser;
        } catch (IllegalArgumentException exception) {
            System.out.println("Cash-in rejected: " + exception.getMessage());
        } catch (SQLException exception) {
            System.out.println("Cash-in is currently unavailable.");
        }
        return user;
    }

    private static User handleTransfer(
            Scanner scanner,
            User user,
            Transfer transfer,
            Balance balance
    ) {
        String receiverMobileNumber = InputValidator.readTrimmedLine(
                scanner,
                "Receiver mobile number: "
        );
        BigDecimal amount = InputValidator.readPositiveAmount(
                scanner,
                "Transfer amount: "
        );
        if (amount == null) {
            System.out.println("Transfer cancelled.");
            return user;
        }

        try {
            User updatedUser = transfer.transfer(
                    user,
                    receiverMobileNumber,
                    amount
            );
            System.out.println("Transfer successful.");
            displayBalance(updatedUser, balance);
            return updatedUser;
        } catch (IllegalArgumentException exception) {
            System.out.println("Transfer rejected: " + exception.getMessage());
        } catch (SQLException exception) {
            System.out.println("Transfer is currently unavailable.");
        }
        return user;
    }

    private static void displayLogs(User user, Logs logs) {
        try {
            List<String> transactionLogs = logs.getFormattedLogs(user);
            System.out.println();
            System.out.println("Transaction logs");

            if (transactionLogs.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }

            for (String transactionLog : transactionLogs) {
                System.out.println(transactionLog);
            }
        } catch (SQLException exception) {
            System.out.println("Transaction logs are currently unavailable.");
        }
    }

    private static void displayBalance(User user, Balance balance) {
        System.out.println("Current balance: " + balance.formatBalance(user));
    }
}
