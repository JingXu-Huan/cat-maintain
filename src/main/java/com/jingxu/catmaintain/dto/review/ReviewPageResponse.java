package com.jingxu.catmaintain.dto.review;

import java.util.List;

public record ReviewPageResponse(List<ReviewResponse> content, long total, int page, int size) {
}
