import java.sql.SQLException;
import java.util.Scanner;
import util.DatabaseConnection;
import util.InputValidator;

public class Main {

    private static final int LOGIN_OPTION = 1;
    private static final int EXIT_OPTION = 0;

    public static void main(String[] args) {
        if (!verifyDatabaseConnection()) {
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            runMenu(scanner);
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

    private static void runMenu(Scanner scanner) {
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
                case LOGIN_OPTION -> System.out.println(
                        "Login is not available yet."
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
}
