package com.hfing.tonadmin.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record StockTransferItemRequest(

        @NotBlank(message = "Vui lòng chọn sản phẩm")
        String productId,

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "1", message = "Số lượng phải từ 1 trở lên")
        BigDecimal quantity
) {
}