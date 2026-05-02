package com.hfing.tonadmin.dto.request;

import com.hfing.tonadmin.common.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentUpdateRequest(

        @NotNull(message = "Vui lòng chọn phương thức thanh toán")
        PaymentMethod paymentMethod,

        @NotNull(message = "Vui lòng nhập số tiền thanh toán")
        @DecimalMin(value = "1", message = "Số tiền thanh toán phải lớn hơn 0")
        BigDecimal amount
) {
}