package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.appointment.AppointmentDecisionRequest;
import com.jingxu.catmaintain.dto.appointment.AppointmentPageResponse;
import com.jingxu.catmaintain.dto.appointment.AppointmentResponse;
import com.jingxu.catmaintain.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store/appointments")
@RequiredArgsConstructor
public class StoreAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public AppointmentPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        return appointmentService.listForStore(page, size, session);
    }

    @PutMapping("/{id}/confirm")
    public AppointmentResponse confirm(@PathVariable Long id, HttpSession session) {
        return appointmentService.confirm(id, session);
    }

    @PutMapping("/{id}/reject")
    public AppointmentResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AppointmentDecisionRequest request,
            HttpSession session
    ) {
        return appointmentService.reject(id, request, session);
    }
}
