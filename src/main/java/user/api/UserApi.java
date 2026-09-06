package user.api;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import shared.api.Views;
import user.model.User;
import user.repository.UserRepository;
import user.service.CashIn;
import user.service.Logs;
import user.service.Transfer;
import user.service.Withdrawal;

public final class UserApi {
    private final UserRepository users = new UserRepository();

    public User find(String mobile) throws SQLException {
        User user = users.findByMobileNumber(mobile);
        if (user == null) throw new IllegalArgumentException("Account no longer exists.");
        return user;
    }

    public Object dashboard(String mobile) throws SQLException {
        User user = find(mobile);
        return Map.of("user", Views.user(user), "transactions", new Logs().getTransactions(user)
                .stream().map(t -> Views.transaction(t, mobile)).toList());
    }

    public Object operate(String mobile, String operation, BigDecimal amount, String receiver) throws SQLException {
        User user = find(mobile);
        User updated = switch (operation) {
            case "cash-in" -> new CashIn().cashIn(user, amount);
            case "withdraw" -> new Withdrawal().withdraw(user, amount);
            case "transfer" -> new Transfer().transfer(user, receiver, amount);
            default -> throw new IllegalArgumentException("Unknown operation.");
        };
        BigDecimal previous = operation.equals("cash-in") ? updated.getBalance().subtract(amount)
                : updated.getBalance().add(amount);
        return Map.of("user", Views.user(updated), "amount", amount.toPlainString(),
                "previousBalance", previous.toPlainString(), "message", "Transaction completed.");
    }
}
