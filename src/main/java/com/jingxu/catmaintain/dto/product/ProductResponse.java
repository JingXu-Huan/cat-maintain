package com.jingxu.catmaintain.dto.product;

import com.jingxu.catmaintain.domain.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String productName,
        String brand,
        String description,
        BigDecimal price,
        BigDecimal laborFee,
        Integer stock,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getProductName(),
                product.getBrand(),
                product.getDescription(),
                product.getPrice(),
                product.getLaborFee(),
                product.getStock(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
