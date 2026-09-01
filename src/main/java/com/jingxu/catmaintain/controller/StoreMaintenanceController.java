package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.maintenance.MaintenanceCompleteRequest;
import com.jingxu.catmaintain.dto.maintenance.MaintenancePageResponse;
import com.jingxu.catmaintain.dto.maintenance.MaintenanceResponse;
import com.jingxu.catmaintain.dto.maintenance.MaintenanceStartRequest;
import com.jingxu.catmaintain.service.MaintenanceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store/maintenance-records")
@RequiredArgsConstructor
public class StoreMaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping
    public MaintenancePageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        return maintenanceService.listForStore(page, size, session);
    }

    @PostMapping("/appointments/{appointmentId}/start")
    public MaintenanceResponse start(
            @PathVariable Long appointmentId,
            @Valid @RequestBody MaintenanceStartRequest request,
            HttpSession session
    ) {
        return maintenanceService.start(appointmentId, request, session);
    }

    @PutMapping("/{id}/complete")
    public MaintenanceResponse complete(
            @PathVariable Long id,
            @Valid @RequestBody MaintenanceCompleteRequest request,
            HttpSession session
    ) {
        return maintenanceService.complete(id, request, session);
    }
}
