package service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import model.User;

public final class Balance {

    public BigDecimal getCurrentBalance(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return user.getBalance();
    }

    public String formatBalance(User user) {
        BigDecimal amount = getCurrentBalance(user).setScale(
                2,
                RoundingMode.HALF_EVEN
        );
        return "PHP " + amount.toPlainString();
    }
}
