package admin.api;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import admin.model.Admin;
import admin.model.BalanceChangeReceipt;
import admin.model.SystemReport;
import admin.repository.AdminRepository;
import admin.service.AdminAccountService;
import shared.api.Views;

public final class AdminApi {
    private final AdminAccountService service;

    public AdminApi() { this(new AdminAccountService()); }

    public AdminApi(AdminAccountService service) {
        this.service = java.util.Objects.requireNonNull(service);
    }

    public boolean deleteTransactionLog(long transactionId, String confirmation) throws SQLException {
        return service.deleteTransactionLog(transactionId, confirmation);
    }

    public Object create(String name, String mobile, String pin) throws SQLException {
        if (name.length() > 100) throw new IllegalArgumentException("Full name must be at most 100 characters.");
        return Views.user(service.createAccount(name.trim(), mobile.trim(), pin));
    }

    public Object dashboard() throws SQLException {
        SystemReport report = service.generateSystemReport();
        Map<String, Object> totals = new LinkedHashMap<>();
        report.totalsByType().forEach((type, total) -> totals.put(type.name(),
                Map.of("count", total.count(), "amount", total.amount().toPlainString())));
        return Map.of("users", service.listAccounts().stream().map(Views::user).toList(),
                "userCount", report.userCount(), "combinedBalance", report.combinedBalance().toPlainString(),
                "totals", totals, "transactions", report.recentTransactions().stream()
                        .map(t -> Views.transaction(t, null)).toList());
    }

    public Object adjust(String username, String mobile, BigDecimal amount, boolean credit) throws SQLException {
        Admin admin = new AdminRepository().findByUsername(username);
        if (admin == null) throw new IllegalArgumentException("Admin account no longer exists.");
        BalanceChangeReceipt receipt = credit ? service.creditAccount(admin, mobile, amount)
                : service.debitAccount(admin, mobile, amount);
        return Map.of("user", Views.user(receipt.user()), "previousBalance", receipt.previousBalance().toPlainString(),
                "amount", amount.toPlainString(), "transaction", Views.transaction(receipt.transaction(), mobile),
                "message", "Account adjustment completed.");
    }
}
