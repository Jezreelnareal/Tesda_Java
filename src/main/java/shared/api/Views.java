package shared.api;

import java.util.LinkedHashMap;
import java.util.Map;
import admin.model.AdminCreditTransaction;
import admin.model.AdminDebitTransaction;
import shared.model.Transaction;
import user.model.CashInTransaction;
import user.model.TransferTransaction;
import user.model.User;
import user.model.WithdrawalTransaction;

/** Explicit response fields keep PIN hashes and internal models off the wire. */
public final class Views {
    private Views() { }

    public static Map<String, Object> user(User user) {
        return Map.of("fullName", user.getFullName(), "mobileNumber", user.getMobileNumber(),
                "balance", user.getBalance().toPlainString());
    }

    public static Map<String, Object> transaction(Transaction transaction, String viewer) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", transaction.getId());
        result.put("type", transaction.getType().name());
        result.put("amount", transaction.getAmount().toPlainString());
        result.put("details", transaction.getDetails());
        result.put("dateTime", transaction.getDateTime().toString());
        String sender = null;
        String receiver = null;
        if (transaction instanceof TransferTransaction transfer) {
            sender = transfer.getSenderMobileNumber(); receiver = transfer.getReceiverMobileNumber();
        } else if (transaction instanceof CashInTransaction cashIn) {
            receiver = cashIn.getUserMobileNumber();
        } else if (transaction instanceof WithdrawalTransaction withdrawal) {
            sender = withdrawal.getUserMobileNumber();
        } else if (transaction instanceof AdminCreditTransaction credit) {
            receiver = credit.getUserMobileNumber(); result.put("admin", credit.getAdminUsername());
        } else if (transaction instanceof AdminDebitTransaction debit) {
            sender = debit.getUserMobileNumber(); result.put("admin", debit.getAdminUsername());
        }
        result.put("sender", sender);
        result.put("receiver", receiver);
        result.put("direction", viewer == null ? "neutral" : viewer.equals(receiver) ? "in" : "out");
        return result;
    }
}
