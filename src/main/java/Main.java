import java.sql.SQLException;
import java.util.Scanner;
import model.User;
import service.Auth;
import util.DatabaseConnection;
import util.InputValidator;

public class Main {

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final int LOGIN_OPTION = 1;
    private static final int EXIT_OPTION = 0;

    public static void main(String[] args) {
        if (!verifyDatabaseConnection()) {
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(scanner, new Auth());
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

    private static void runMenu(Scanner scanner, Auth auth) {
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
                case LOGIN_OPTION -> running = handleLogin(scanner, auth);
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

    private static boolean handleLogin(Scanner scanner, Auth auth) {
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
                runAuthenticatedMenu(scanner, authenticatedUser);
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

    private static void runAuthenticatedMenu(Scanner scanner, User user) {
        System.out.printf(
                "%nLogin successful. Welcome, %s!%n",
                user.getFullName()
        );

        boolean signedIn = true;
        while (signedIn) {
            System.out.println();
            System.out.println("Signed in as: " + user.getFullName());
            System.out.println("0. Logout");

            int choice = InputValidator.readMenuChoice(scanner, 0, 0);
            if (choice == 0) {
                System.out.println("Logged out successfully.");
                signedIn = false;
            }
        }
    }
}
