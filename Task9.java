import static java.lang.Math.*;

public class Task9 {
    public static int add(int a, int b) {
        return addExact(a, b);
    }

    public static int subtract(int a, int b) {
        return subtractExact(a, b);
    }

    public static int multiply(int a, int b) {
        return multiplyExact(a, b);
    }

    public static float divide(int a, int b) {
        return floorDiv(a, b);
    }

    public static void main(String[] args) {
        System.out.println("Sum: " + add(10, 5));
        System.out.println("Difference: " + subtract(10, 5));
        System.out.println("Product: " + multiply(10, 5));
        System.out.println("Quotient: " + divide(10, 5));
    }
}