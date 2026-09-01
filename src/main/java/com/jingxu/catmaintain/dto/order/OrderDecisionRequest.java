package com.jingxu.catmaintain.dto.order;

import jakarta.validation.constraints.Size;

public record OrderDecisionRequest(@Size(max = 255) String reason) {
}
