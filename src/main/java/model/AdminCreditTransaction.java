package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AdminCreditTransaction extends Transaction {

    private final String adminUsername;
    private final String userMobileNumber;

    public AdminCreditTransaction(
            String adminUsername,
            String userMobileNumber,
            BigDecimal amount,
            String details
    ) {
        this(
                null,
                adminUsername,
                userMobileNumber,
                amount,
                details,
                LocalDateTime.now()
        );
    }

    public AdminCreditTransaction(
            Long id,
            String adminUsername,
            String userMobileNumber,
            BigDecimal amount,
            String details,
            LocalDateTime dateTime
    ) {
        super(id, TransactionType.ADMIN_CREDIT, amount, details, dateTime);
        this.adminUsername = requireAdminUsername(adminUsername);
        this.userMobileNumber = requireMobileNumber(userMobileNumber);
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getUserMobileNumber() {
        return userMobileNumber;
    }

    private static String requireAdminUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin username is required");
        }
        return username.trim();
    }
}
