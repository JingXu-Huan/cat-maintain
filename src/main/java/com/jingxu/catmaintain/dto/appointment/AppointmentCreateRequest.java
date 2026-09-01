package com.jingxu.catmaintain.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull Long storeId,
        Long orderId,
        @NotNull @Future LocalDateTime appointmentTime,
        @NotBlank @Size(max = 20) String vehiclePlate,
        @Size(max = 100) String vehicleModel,
        @Size(max = 500) String remark
) {
}
