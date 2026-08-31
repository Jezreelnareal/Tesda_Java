package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Base type for every JCash transaction.
 */
public abstract class Transaction {

    private final Long id;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String details;
    private final LocalDateTime dateTime;

    protected Transaction(
            Long id,
            TransactionType type,
            BigDecimal amount,
            String details,
            LocalDateTime dateTime
    ) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Transaction ID must be positive");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Details cannot be null or empty");
        }
        if (details.trim().length() > 255) {
            throw new IllegalArgumentException(
                    "Details cannot exceed 255 characters"
            );
        }
        if (dateTime == null) {
            throw new IllegalArgumentException("Date/time cannot be null");
        }

        this.id = id;
        this.type = type;
        this.amount = amount;
        this.details = details.trim();
        this.dateTime = dateTime.withNano(0);
    }

    public Long getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDetails() {
        return details;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    protected static String requireMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                    "Mobile number must contain 11 digits and start with 09"
            );
        }
        return mobileNumber;
    }
}
