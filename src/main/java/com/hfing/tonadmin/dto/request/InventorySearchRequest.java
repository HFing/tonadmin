package com.hfing.tonadmin.dto.request;

import com.hfing.tonadmin.common.StockStatus;

public record InventorySearchRequest(
        String keyword,
        String branchId,
        String productId,
        StockStatus stockStatus
) {
}
