package com.hfing.tonadmin.dto.request;

import lombok.Builder;

@Builder
public record CancelSalesOrderRequest(
        String reason
) {
}