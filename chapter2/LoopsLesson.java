package chapter2;

import java.util.Scanner;

public class LoopsLesson {
    // A helper method that checks if a number is still valid
    static boolean checkLimit(int i) {
        System.out.println("(Checking condition method...)");
        return i < 10;
    }

    public static void main(String[] args) {
        int a = 1;
        while (a <= 10) {
            System.out.println("a = " + a++);

        }

        // at this point a = 11

        do
            System.out.println("do while : a = " + a--);
        while (a < 10);
        System.out.println("End of program");
        System.out.println("Final value of a = " + a);

        for (int i = 0; i < 10; i++) { // Initialize, Condition, UpdateStatement
            System.out.println("for loop : i = " + i); // Body
        }

        /**
         * step 1: initialize the loop variable
         * step 2: check the condition
         * step 3: execute the loop body
         * step 4: increment/decrement the loop variable
         */

        // trick for loop
        int b = 0;
        for (; b < 10;) {
            System.out.println("trick for loop : b = " + b++);
        }
        System.out.println("End of program");

        int c = 0;

        // Initialization runs once, update runs after every iteration
        for (System.out.println("--- Initialization ---"); c < 10; System.out.println("--- Update Statement ---")) {
            System.out.println("c is: " + c++);
        }

        // We use our method directly inside the middle condition slot
        for (int i = 0; checkLimit(i); i++) {
            System.out.println("i is: " + i);
        }

        for (int i = 0; i < 10; i++) {
            System.out.println("i is: " + i++);
        }

        for (int i = 1; i < 10; i++) {
            // Inner loop
            for (int j = 1; j <= i; j++) {
                System.out.print(i);
            }
            System.out.println();

        }

Scanner q = new Scanner(System.in);
        
        System.out.print("Enter limit: ");
        int limit = q.nextInt(); // Takes your input (e.g., 10)
        
        // Outer loop uses the limit variable
        for (int x = 1; x < limit; x++) {
            
            // Inner loop prints the repeated numbers
            for (int y = 1; y <= x; y++) {
                System.out.print(x);
            }
            
            System.out.println(); // Moves to the next line after each row
        }
        
        q.close();

        // An array of numbers
        int[] numbers = {10, 20, 30, 40, 50};

        // For-each loop: reads as "For every int 'num' in 'numbers'"
        for (int num : numbers) {
            System.out.println("num = " + num);
        }

        String[] names = {"Alice", "Bob", "Charlie"};
        for (String name : names) 
            System.out.println("name = " + name);
        ROW: for (int p = 1; p <= 10; p++){
        COL: for (int l = 1; l <= 10; l++){
                if (l==5)
                    continue COL;
            System.out.print(l*p + "\t");
            }
            System.out.println();
           
    


    }
}
}