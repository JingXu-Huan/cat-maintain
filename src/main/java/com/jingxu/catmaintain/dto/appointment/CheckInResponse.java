package com.jingxu.catmaintain.dto.appointment;

import com.jingxu.catmaintain.dto.order.OrderResponse;

public record CheckInResponse(AppointmentResponse appointment, OrderResponse order) {
}
