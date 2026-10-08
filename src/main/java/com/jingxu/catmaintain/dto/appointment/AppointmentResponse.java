package com.jingxu.catmaintain.dto.appointment;

import com.jingxu.catmaintain.domain.appointment.Appointment;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long accountId,
        Long storeId,
        Long orderId,
        LocalDateTime appointmentTime,
        String status,
        String vehiclePlate,
        String vehicleModel,
        String remark,
        LocalDateTime checkedInAt,
        LocalDateTime createdAt
) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(), appointment.getAccountId(), appointment.getStoreId(), appointment.getOrderId(),
                appointment.getAppointmentTime(), appointment.getStatus().name(), appointment.getVehiclePlate(),
                appointment.getVehicleModel(), appointment.getRemark(), appointment.getCheckedInAt(), appointment.getCreatedAt()
        );
    }
}
