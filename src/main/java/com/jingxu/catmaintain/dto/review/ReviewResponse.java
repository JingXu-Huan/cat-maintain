package com.jingxu.catmaintain.dto.review;

import com.jingxu.catmaintain.domain.review.Review;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long accountId, Long storeId, Long orderId, Integer rating, String content, LocalDateTime createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getAccountId(), review.getStoreId(), review.getOrderId(), review.getRating(), review.getContent(), review.getCreatedAt());
    }
}
