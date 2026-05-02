package com.hfing.tonadmin.dto.response;

import java.math.BigDecimal;

public record RecentStockActivityItem(
        String branchName,
        String productCode,
        String productName,
        String transactionType,
        BigDecimal quantity,
        String createdAt
) {
}