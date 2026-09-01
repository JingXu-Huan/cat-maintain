package com.jingxu.catmaintain.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Order {

    private Long id;
    private String orderNo;
    private Long accountId;
    private Long storeId;
    private OrderStatus status;
    private BigDecimal productAmount;
    private BigDecimal laborFeeAmount;
    private BigDecimal totalAmount;
    private String verificationCode;
    private String rejectedReason;
    private LocalDateTime approvedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime verifiedAt;
    private Long verifiedByStoreId;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
