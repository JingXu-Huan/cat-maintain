package com.jingxu.catmaintain.dto.admin;

import jakarta.validation.constraints.Size;

public record ReviewDecisionRequest(
        @Size(max = 255, message = "审核备注不能超过 255 个字符")
        String reason
) {
}
