package com.jingxu.catmaintain.domain.review;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProductReview {
    private Long id;
    private Long accountId;
    private Long orderId;
    private Long productId;
    private String productName;
    private String reviewerName;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
