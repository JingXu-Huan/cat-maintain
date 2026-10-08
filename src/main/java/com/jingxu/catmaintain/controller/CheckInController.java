package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.appointment.AppointmentResponse;
import com.jingxu.catmaintain.dto.appointment.CheckInRequest;
import com.jingxu.catmaintain.dto.appointment.CheckInResponse;
import com.jingxu.catmaintain.dto.store.StoreResponse;
import com.jingxu.catmaintain.service.CheckInService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @GetMapping("/api/store/profile")
    public StoreResponse profile(HttpSession session) {
        return checkInService.storeProfile(session);
    }

    @GetMapping("/api/store/orders/lookup-appointments")
    public List<AppointmentResponse> lookupAppointments(@RequestParam String code, HttpSession session) {
        return checkInService.lookupAppointments(code, session);
    }

    @PostMapping("/api/stores/{storeId}/check-ins")
    public CheckInResponse checkIn(@PathVariable Long storeId, @Valid @RequestBody CheckInRequest request, HttpSession session) {
        return checkInService.checkIn(storeId, request, session);
    }
}
