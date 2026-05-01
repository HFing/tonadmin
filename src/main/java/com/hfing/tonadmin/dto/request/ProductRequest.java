package com.hfing.tonadmin.dto.request;

import com.hfing.tonadmin.common.UnitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "Mã sản phẩm không được để trống")
        @Size(max = 100, message = "Mã sản phẩm tối đa 100 ký tự")
        String code,

        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
        String name,

        String description,

        @NotBlank(message = "Vui lòng chọn danh mục")
        String categoryId,

        @NotNull(message = "Vui lòng chọn đơn vị tính")
        UnitType unit,

        BigDecimal thickness,

        BigDecimal width,

        BigDecimal length,

        String color,

        String material,

        @DecimalMin(value = "0", message = "Giá nhập không được âm")
        BigDecimal importPrice,

        @DecimalMin(value = "0", message = "Giá bán không được âm")
        BigDecimal sellingPrice,

        @DecimalMin(value = "0", message = "Tồn tối thiểu không được âm")
        BigDecimal minStock
) {
}
