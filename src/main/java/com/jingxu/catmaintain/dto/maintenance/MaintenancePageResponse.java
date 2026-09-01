package com.jingxu.catmaintain.dto.maintenance;

import java.util.List;

public record MaintenancePageResponse(List<MaintenanceResponse> content, long total, int page, int size) {
}
