package com.jingxu.catmaintain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreRegisterRequest(
        @NotBlank(message = "登录用户名不能为空")
        @Size(min = 3, max = 50, message = "登录用户名长度应为 3-50 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度应为 8-72 个字符")
        String password,

        @NotBlank(message = "店面名称不能为空")
        @Size(max = 100, message = "店面名称长度不能超过 100 个字符")
        String storeName,

        @NotBlank(message = "联系人不能为空")
        @Size(max = 50, message = "联系人长度不能超过 50 个字符")
        String contactName,

        @NotBlank(message = "联系电话不能为空")
        @Size(max = 30, message = "联系电话长度不能超过 30 个字符")
        String phone,

        @NotBlank(message = "店面地址不能为空")
        @Size(max = 255, message = "店面地址长度不能超过 255 个字符")
        String address
) {
}
