package com.hfing.tonadmin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchRequest(

        @NotBlank(message = "Mã chi nhánh không được để trống")
        @Size(max = 50, message = "Mã chi nhánh tối đa 50 ký tự")
        String code,

        @NotBlank(message = "Tên chi nhánh không được để trống")
        @Size(max = 255, message = "Tên chi nhánh tối đa 255 ký tự")
        String name,

        String address,

        String phone
) {
}
