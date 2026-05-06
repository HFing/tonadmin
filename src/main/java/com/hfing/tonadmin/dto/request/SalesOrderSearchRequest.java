package com.hfing.tonadmin.dto.request;

import com.hfing.tonadmin.common.PaymentStatus;
import com.hfing.tonadmin.common.SalesOrderStatus;

import java.time.LocalDate;

public record SalesOrderSearchRequest(
        String keyword,
        String branchId,
        SalesOrderStatus status,
        PaymentStatus paymentStatus,
        Boolean debtOnly,
        LocalDate fromDate,
        LocalDate toDate
) {
}
