package com.jingxu.catmaintain.dto.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaintenanceStartRequest(@NotBlank @Size(max = 2000) String content) {
}
