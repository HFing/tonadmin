package com.hfing.tonadmin.dto.response;

import com.hfing.tonadmin.common.StockTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record StockTransactionSummary(
        String batchCode,
        StockTransactionType transactionType,
        String branchName,
        Integer itemCount,
        BigDecimal totalQuantity,
        String note,
        Instant createdAt,
        String createdBy
) {
}