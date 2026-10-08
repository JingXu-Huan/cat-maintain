package com.jingxu.catmaintain.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductReviewCreateRequest(
        @NotNull @Positive Long orderId,
        @NotNull @Positive Long productId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Size(max = 500) String content
) {
}
