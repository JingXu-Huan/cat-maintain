package com.jingxu.catmaintain.dto.review;

import com.jingxu.catmaintain.domain.review.ProductReview;

import java.time.LocalDateTime;

public record ProductReviewResponse(Long id, Long orderId, Long productId, String productName,
                                    String reviewerName, Integer rating, String content, LocalDateTime createdAt) {
    public static ProductReviewResponse from(ProductReview review) {
        return new ProductReviewResponse(review.getId(), review.getOrderId(), review.getProductId(),
                review.getProductName(), review.getReviewerName(), review.getRating(), review.getContent(), review.getCreatedAt());
    }
}
