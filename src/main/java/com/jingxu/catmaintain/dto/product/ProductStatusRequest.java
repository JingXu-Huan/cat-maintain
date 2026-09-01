package com.jingxu.catmaintain.dto.product;

import com.jingxu.catmaintain.domain.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequest(@NotNull ProductStatus status) {
}
