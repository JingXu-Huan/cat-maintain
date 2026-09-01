package com.jingxu.catmaintain.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @NotNull Long storeId,
        @NotEmpty @Size(max = 50) List<@Valid CartItemRequest> items
) {
}
