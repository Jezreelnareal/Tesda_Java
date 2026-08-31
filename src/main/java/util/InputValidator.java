package util;

import java.math.BigDecimal;
import java.util.Scanner;

public final class InputValidator {

    private static final BigDecimal MAXIMUM_AMOUNT =
            new BigDecimal("9999999999999.99");

    private InputValidator() {
        // Utility class
    }

    public static int readMenuChoice(Scanner scanner, int minimum, int maximum) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "Minimum choice cannot be greater than maximum choice"
            );
        }

        while (true) {
            System.out.print("Choose an option: ");

            if (!scanner.hasNextLine()) {
                return minimum;
            }

            String input = scanner.nextLine().trim();

            try {
                int choice = Integer.parseInt(input);
                if (choice >= minimum && choice <= maximum) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {
                // The common validation message below covers non-numeric input.
            }

            System.out.printf(
                    "Invalid choice. Enter a number from %d to %d.%n",
                    minimum,
                    maximum
            );
        }
    }

    public static String readTrimmedLine(Scanner scanner, String prompt) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }

        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    public static BigDecimal readPositiveAmount(
            Scanner scanner,
            String prompt
    ) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner cannot be null");
        }
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }

        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return null;
            }

            String input = scanner.nextLine().trim();
            if (input.matches("\\d+(\\.\\d{1,2})?")) {
                BigDecimal amount = new BigDecimal(input);
                if (amount.compareTo(BigDecimal.ZERO) > 0
                        && amount.compareTo(MAXIMUM_AMOUNT) <= 0) {
                    return amount;
                }
            }

            System.out.println(
                    "Invalid amount. Enter a positive number with up to "
                    + "two decimal places."
            );
        }
    }
}
