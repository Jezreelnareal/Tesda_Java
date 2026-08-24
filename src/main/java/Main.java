
import java.math.BigDecimal;
import model.Transaction;
import model.User;

public class Main {

    public static void main(String[] args) {
        User user = new User("Juan Dela Cruz", "09171234567", "1234");
        System.out.println("Starting balance: " + user.getBalance());
        user.deposit(new BigDecimal("1000.00"));
        user.withdraw(new BigDecimal("250.00"));
        System.out.println("Final balance: " + user.getBalance());

        try {
            user.withdraw(new BigDecimal("1000.00"));
        } catch (IllegalArgumentException exception) {
            System.out.println("Expected error: " + exception.getMessage());

            System.out.println("Balance after failed withdrawal: " + user.getBalance());
        }

        user.addTransaction(new Transaction());

        System.out.println("Transaction count: " + user.getTransactions().size());

        user.getTransactions().clear();

        System.out.println(
                "Transaction count after clearing returned list: "
                + user.getTransactions().size()
        );
    }
}
