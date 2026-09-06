package admin.model;

import shared.model.Transaction;
import user.model.User;

import java.math.BigDecimal;

public record BalanceChangeReceipt(
        User user,
        BigDecimal previousBalance,
        Transaction transaction
) {

    public BalanceChangeReceipt {
        if (user == null || previousBalance == null || transaction == null) {
            throw new IllegalArgumentException("Invalid balance change receipt");
        }
    }
}
