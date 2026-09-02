package model;

import java.math.BigDecimal;

public record TransferReceipt(
        User sender,
        User receiver,
        BigDecimal previousSenderBalance,
        TransferTransaction transaction
) {

    public TransferReceipt {
        if (sender == null || receiver == null || previousSenderBalance == null
                || transaction == null) {
            throw new IllegalArgumentException("Invalid transfer receipt");
        }
    }
}
