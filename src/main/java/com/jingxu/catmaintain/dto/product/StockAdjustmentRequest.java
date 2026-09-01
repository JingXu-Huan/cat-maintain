package com.jingxu.catmaintain.dto.product;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(@NotNull Integer delta) {
}
