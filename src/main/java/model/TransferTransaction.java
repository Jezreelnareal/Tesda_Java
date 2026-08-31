package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TransferTransaction extends Transaction {

    private final String senderMobileNumber;
    private final String receiverMobileNumber;

    public TransferTransaction(
            String senderMobileNumber,
            String receiverMobileNumber,
            BigDecimal amount,
            String details
    ) {
        this(
                null,
                senderMobileNumber,
                receiverMobileNumber,
                amount,
                details,
                LocalDateTime.now()
        );
    }

    public TransferTransaction(
            Long id,
            String senderMobileNumber,
            String receiverMobileNumber,
            BigDecimal amount,
            String details,
            LocalDateTime dateTime
    ) {
        super(id, TransactionType.TRANSFER, amount, details, dateTime);
        this.senderMobileNumber = requireMobileNumber(senderMobileNumber);
        this.receiverMobileNumber = requireMobileNumber(receiverMobileNumber);
    }

    public String getSenderMobileNumber() {
        return senderMobileNumber;
    }

    public String getReceiverMobileNumber() {
        return receiverMobileNumber;
    }
}
