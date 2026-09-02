package model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SystemReport(
        int userCount,
        BigDecimal combinedBalance,
        Map<TransactionType, TransactionTotals> totalsByType,
        List<Transaction> recentTransactions
) {

    public SystemReport {
        if (userCount < 0 || combinedBalance == null
                || totalsByType == null || recentTransactions == null) {
            throw new IllegalArgumentException("Invalid system report");
        }
        totalsByType = Map.copyOf(totalsByType);
        recentTransactions = List.copyOf(recentTransactions);
    }
}
