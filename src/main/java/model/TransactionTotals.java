package model;

import java.math.BigDecimal;

public record TransactionTotals(int count, BigDecimal amount) {

    public TransactionTotals {
        if (count < 0 || amount == null) {
            throw new IllegalArgumentException("Invalid transaction totals");
        }
    }
}
