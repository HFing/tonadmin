package com.hfing.tonadmin.dto.request;

import com.hfing.tonadmin.common.RoleType;
import com.hfing.tonadmin.common.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
        String password,

        @NotBlank(message = "Tên không được để trống")
        String firstName,

        @NotBlank(message = "Họ không được để trống")
        String lastName,

        String phone,

        @NotNull(message = "Vui lòng chọn trạng thái")
        UserStatus userStatus,

        @NotNull(message = "Vui lòng chọn quyền")
        RoleType roleType,

        String branchId
) {
}