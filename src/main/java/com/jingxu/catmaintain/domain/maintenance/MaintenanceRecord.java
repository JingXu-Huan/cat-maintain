package com.jingxu.catmaintain.domain.maintenance;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRecord {

    private Long id;
    private Long appointmentId;
    private Long orderId;
    private Long accountId;
    private Long storeId;
    private LocalDateTime serviceStartedAt;
    private LocalDateTime serviceCompletedAt;
    private Integer mileage;
    private String content;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
