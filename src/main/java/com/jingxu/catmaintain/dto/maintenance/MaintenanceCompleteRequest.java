package com.jingxu.catmaintain.dto.maintenance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MaintenanceCompleteRequest(
        @Min(0) Integer mileage,
        @NotBlank @Size(max = 2000) String content,
        @Size(max = 500) String remark
) {
}
