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
        return formatAmount(getCurrentBalance(user));
    }

    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        BigDecimal formattedAmount = amount.setScale(2, RoundingMode.HALF_EVEN);
        return "PHP " + formattedAmount.toPlainString();
    }
}
