package com.jingxu.catmaintain.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOrderRequest(
        @NotBlank @Pattern(regexp = "\\d{8}", message = "核销码必须是 8 位数字") String verificationCode
) {
}
