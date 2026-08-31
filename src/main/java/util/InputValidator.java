package util;

import java.util.Scanner;

public final class InputValidator {

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
}
