package com.jingxu.catmaintain.domain.review;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Review {

    private Long id;
    private Long accountId;
    private Long storeId;
    private Long orderId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
