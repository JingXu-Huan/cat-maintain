package com.jingxu.catmaintain.dto.order;

import com.jingxu.catmaintain.domain.order.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal unitLaborFee,
        BigDecimal lineTotal
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(), item.getProductName(), item.getQuantity(),
                item.getUnitPrice(), item.getUnitLaborFee(), item.getLineTotal()
        );
    }
}
