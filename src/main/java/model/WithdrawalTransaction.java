 package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class WithdrawalTransaction extends Transaction {

    private final String userMobileNumber;

    public WithdrawalTransaction(
            String userMobileNumber,
            BigDecimal amount,
            String details
    ) {
        this(null, userMobileNumber, amount, details, LocalDateTime.now());
    }

    public WithdrawalTransaction(
            Long id,
            String userMobileNumber,
            BigDecimal amount,
            String details,
            LocalDateTime dateTime
    ) {
        super(id, TransactionType.WITHDRAWAL, amount, details, dateTime);
        this.userMobileNumber = requireMobileNumber(userMobileNumber);
    }

    public String getUserMobileNumber() {
        return userMobileNumber;
    }
}
