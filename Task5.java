import java.util.Scanner;

public class Task5 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] numbers = new int[3];

        for (int i = 0; i < numbers.length; i++) {
            System.out.print("Enter number " + (i + 1) + ": ");
            numbers[i] = scanner.nextInt();
        }

        int largest = numbers[0]; //we assumed the first input number is the largest

        /*
        *this is for the condition of getting the largest value inside the numbers[] 
        *starting from index 1 
        *since we assumed index 0 as largest 
        *for comparison.
        */
        for (int i = 1; i < numbers.length; i++) { 
            if (numbers[i] > largest) {
                largest = numbers[i];
            }
        }

        if (numbers[0] == numbers[1] && numbers[1] == numbers[2]) {
            System.out.println("All numbers are equal.");
        } else {
            System.out.println("The largest number is " + largest);
        }

        scanner.close();
    }
}