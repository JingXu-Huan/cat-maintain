package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.dto.admin.ReviewDecisionRequest;
import com.jingxu.catmaintain.dto.admin.StoreApplicationResponse;
import com.jingxu.catmaintain.service.StoreReviewService;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class AdminStoreController {

    private final StoreReviewService storeReviewService;

    @GetMapping
    public List<StoreApplicationResponse> list(
            @RequestParam(defaultValue = "PENDING") AccountStatus status,
            HttpSession session
    ) {
        return storeReviewService.list(status, session);
    }

    @PutMapping("/{storeId}/approve")
    public StoreApplicationResponse approve(@PathVariable Long storeId, HttpSession session) {
        return storeReviewService.approve(storeId, session);
    }

    @PutMapping("/{storeId}/reject")
    public StoreApplicationResponse reject(
            @PathVariable Long storeId,
            @Valid @RequestBody(required = false) ReviewDecisionRequest request,
            HttpSession session
    ) {
        return storeReviewService.reject(storeId, request == null ? null : request.reason(), session);
    }
}
