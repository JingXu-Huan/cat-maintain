package com.jingxu.catmaintain.dto.order;

import java.util.List;

public record OrderPageResponse(List<OrderResponse> content, long total, int page, int size) {
}
