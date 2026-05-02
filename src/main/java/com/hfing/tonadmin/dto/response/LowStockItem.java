package com.hfing.tonadmin.dto.response;

import java.math.BigDecimal;

public record LowStockItem(
        String branchName,
        String productCode,
        String productName,
        BigDecimal quantity,
        BigDecimal minStock
) {
}