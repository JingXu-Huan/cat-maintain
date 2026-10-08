package com.jingxu.catmaintain.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CheckInRequest(
        @NotNull @Positive Long appointmentId,
        @NotBlank @Pattern(regexp = "[0-9]{8}", message = "核销码必须是 8 位数字") String verificationCode
) {
}
