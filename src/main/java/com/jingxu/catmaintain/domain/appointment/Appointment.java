package com.jingxu.catmaintain.domain.appointment;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    private Long id;
    private Long accountId;
    private Long storeId;
    private Long orderId;
    private LocalDateTime appointmentTime;
    private AppointmentStatus status;
    private String vehiclePlate;
    private String vehicleModel;
    private String remark;
    private LocalDateTime checkedInAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
