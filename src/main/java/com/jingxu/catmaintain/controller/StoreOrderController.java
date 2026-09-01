package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.order.OrderPageResponse;
import com.jingxu.catmaintain.dto.order.OrderResponse;
import com.jingxu.catmaintain.dto.order.VerifyOrderRequest;
import com.jingxu.catmaintain.service.OrderService;
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
@RequestMapping("/api/store/orders")
@RequiredArgsConstructor
public class StoreOrderController {

    private final OrderService orderService;

    @GetMapping
    public OrderPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        return orderService.listForStore(page, size, session);
    }

    @GetMapping("/lookup")
    public OrderResponse lookup(@RequestParam String code, HttpSession session) {
        return orderService.lookupByCode(code, session);
    }

    @PutMapping("/{id}/verify")
    public OrderResponse verify(
            @PathVariable Long id,
            @Valid @RequestBody VerifyOrderRequest request,
            HttpSession session
    ) {
        return orderService.verify(id, request, session);
    }
}
