package com.hfing.tonadmin.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
public record StockTransferRequest(

        @NotBlank(message = "Vui lòng chọn chi nhánh xuất")
        String sourceBranchId,

        @NotBlank(message = "Vui lòng chọn chi nhánh nhận")
        String targetBranchId,

        String note,

        @Valid
        @NotEmpty(message = "Vui lòng thêm ít nhất một sản phẩm")
        List<StockTransferItemRequest> items
) {
    public StockTransferRequest {
        if (items == null) {
            items = new ArrayList<>();
        }
    }
}