package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.order.OrderDecisionRequest;
import com.jingxu.catmaintain.dto.order.OrderPageResponse;
import com.jingxu.catmaintain.dto.order.OrderResponse;
import com.jingxu.catmaintain.service.OrderService;
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
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public OrderPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        return orderService.listForAdmin(page, size, session);
    }

    @PutMapping("/{id}/approve")
    public OrderResponse approve(@PathVariable Long id, HttpSession session) {
        return orderService.approve(id, session);
    }

    @PutMapping("/{id}/reject")
    public OrderResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) OrderDecisionRequest request,
            HttpSession session
    ) {
        return orderService.reject(id, request, session);
    }

    @PutMapping("/{id}/deliver")
    public OrderResponse deliver(@PathVariable Long id, HttpSession session) {
        return orderService.deliver(id, session);
    }
}
