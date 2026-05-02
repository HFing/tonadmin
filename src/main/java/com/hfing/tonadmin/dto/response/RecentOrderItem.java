package com.hfing.tonadmin.dto.response;

import java.math.BigDecimal;

public record RecentOrderItem(
        String id,
        String orderCode,
        String customerName,
        String branchName,
        BigDecimal finalAmount,
        BigDecimal remainingAmount,
        String paymentStatus,
        String createdAt
) {
}