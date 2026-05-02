package com.hfing.tonadmin.dto.request;

import com.hfing.tonadmin.common.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
public record SalesOrderRequest(

        @NotBlank(message = "Vui lòng chọn chi nhánh")
        String branchId,

        String customerName,

        String customerPhone,

        BigDecimal discountAmount,

        String note,

        @Valid
        @NotEmpty(message = "Vui lòng thêm ít nhất một sản phẩm")
        List<SalesOrderItemRequest> items
) {
    public SalesOrderRequest {
        if (items == null) {
            items = new ArrayList<>();
        }
    }
}