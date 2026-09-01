package com.jingxu.catmaintain.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 100) String productName,
        @Size(max = 50) String brand,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        @NotNull @DecimalMin(value = "0.00") BigDecimal laborFee,
        @NotNull @Min(0) Integer stock
) {
}
