package com.jingxu.catmaintain.dto.product;

import java.util.List;

public record ProductPageResponse(List<ProductResponse> content, long total, int page, int size) {
}
