package com.jingxu.catmaintain.dto.maintenance;

import com.jingxu.catmaintain.domain.maintenance.MaintenanceRecord;

import java.time.LocalDateTime;

public record MaintenanceResponse(
        Long id,
        Long appointmentId,
        Long orderId,
        Long accountId,
        Long storeId,
        LocalDateTime serviceStartedAt,
        LocalDateTime serviceCompletedAt,
        Integer mileage,
        String content,
        String remark,
        LocalDateTime createdAt
) {

    public static MaintenanceResponse from(MaintenanceRecord record) {
        return new MaintenanceResponse(
                record.getId(), record.getAppointmentId(), record.getOrderId(), record.getAccountId(), record.getStoreId(),
                record.getServiceStartedAt(), record.getServiceCompletedAt(), record.getMileage(), record.getContent(),
                record.getRemark(), record.getCreatedAt()
        );
    }
}
