package com.jingxu.catmaintain.dto.appointment;

import jakarta.validation.constraints.Size;

public record AppointmentDecisionRequest(@Size(max = 500) String reason) {
}
