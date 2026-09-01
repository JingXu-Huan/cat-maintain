package com.jingxu.catmaintain.dto.appointment;

import java.util.List;

public record AppointmentPageResponse(List<AppointmentResponse> content, long total, int page, int size) {
}
