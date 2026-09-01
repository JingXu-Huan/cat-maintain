package com.jingxu.catmaintain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度应为 3-50 个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度应为 8-72 个字符")
        String password,

        @NotBlank(message = "手机号不能为空")
        @Size(max = 30, message = "手机号长度不能超过 30 个字符")
        String phone
) {
}
