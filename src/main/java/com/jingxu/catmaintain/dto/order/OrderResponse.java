package com.jingxu.catmaintain.dto.order;

import com.jingxu.catmaintain.domain.order.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        Long accountId,
        Long storeId,
        String status,
        BigDecimal productAmount,
        BigDecimal laborFeeAmount,
        BigDecimal totalAmount,
        String verificationCode,
        String rejectedReason,
        LocalDateTime approvedAt,
        LocalDateTime deliveredAt,
        LocalDateTime verifiedAt,
        Long verifiedByStoreId,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(Order order, List<OrderItemResponse> items) {
        return new OrderResponse(
                order.getId(), order.getOrderNo(), order.getAccountId(), order.getStoreId(),
                order.getStatus().name(), order.getProductAmount(), order.getLaborFeeAmount(), order.getTotalAmount(),
                order.getVerificationCode(), order.getRejectedReason(), order.getApprovedAt(), order.getDeliveredAt(),
                order.getVerifiedAt(), order.getVerifiedByStoreId(), order.getCompletedAt(), order.getCreatedAt(), items
        );
    }
}
