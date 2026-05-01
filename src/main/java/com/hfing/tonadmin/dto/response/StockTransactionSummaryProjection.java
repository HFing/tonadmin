package com.hfing.tonadmin.dto.response;

import com.hfing.tonadmin.common.StockTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public interface StockTransactionSummaryProjection {

    String getBatchCode();

    StockTransactionType getTransactionType();

    String getBranchName();

    Long getItemCount();

    BigDecimal getTotalQuantity();

    String getNote();

    Instant getCreatedAt();

    String getCreatedBy();
}