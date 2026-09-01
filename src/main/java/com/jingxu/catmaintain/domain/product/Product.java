package com.jingxu.catmaintain.domain.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Product {

    private Long id;
    private String sku;
    private String productName;
    private String brand;
    private String description;
    private BigDecimal price;
    private BigDecimal laborFee;
    private Integer stock;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
