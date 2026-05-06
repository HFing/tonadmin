package com.hfing.tonadmin.dto.request;

import java.time.LocalDate;

public record StockTransferSearchRequest(
        String keyword,
        String sourceBranchId,
        String targetBranchId,
        LocalDate fromDate,
        LocalDate toDate
) {
}
