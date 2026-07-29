import java.util.Scanner;

public class Task4 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a string: ");
        String input = scanner.nextLine();// Read the input string from the user

        String reversed = new StringBuilder(input).reverse().toString();// Reverse the input string

        if (input.equals(reversed)) {
            System.out.println("The input string is a palindrome.");
        } 
        else {
            System.out.println("The input string is not a palindrome.");
        }

        scanner.close();
    }
}