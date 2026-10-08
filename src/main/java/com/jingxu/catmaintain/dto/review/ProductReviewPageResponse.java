package com.jingxu.catmaintain.dto.review;

import java.util.List;

public record ProductReviewPageResponse(List<ProductReviewResponse> content, long total, int page, int size) {
}
