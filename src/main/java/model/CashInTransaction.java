package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class CashInTransaction extends Transaction {

    private final String userMobileNumber;

    public CashInTransaction(
            String userMobileNumber,
            BigDecimal amount,
            String details
    ) {
        this(null, userMobileNumber, amount, details, LocalDateTime.now());
    }

    public CashInTransaction(
            Long id,
            String userMobileNumber,
            BigDecimal amount,
            String details,
            LocalDateTime dateTime
    ) {
        super(id, TransactionType.CASH_IN, amount, details, dateTime);
        this.userMobileNumber = requireMobileNumber(userMobileNumber);
    }

    public String getUserMobileNumber() {
        return userMobileNumber;
    }
}
