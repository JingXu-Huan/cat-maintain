package com.jingxu.catmaintain.dto.common;

import java.util.Map;

public record ApiErrorResponse(String code, String message, Map<String, String> details) {
}
