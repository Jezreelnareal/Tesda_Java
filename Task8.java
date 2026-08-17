public class Task8 {

    public static int sumOfSums(int... numbers) {
        int cumulativeSum = 0;

        for (int number : numbers) {
            int triangularSum = 0;

            for (int i = 1; i <= number; i++) {
                triangularSum += i;
            }

            cumulativeSum += triangularSum;

            System.out.println("Parameter " + number + ": sum = " + triangularSum + ", cumulative sum = " + cumulativeSum
            );
        }
        return cumulativeSum;
    }

    public static void main(String[] args) {
        int total = sumOfSums(4, 5, 10);
        System.out.println("Total sum: " + total);
    }
}
