package com.jingxu.catmaintain.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull Long orderId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 500) String content
) {
}
