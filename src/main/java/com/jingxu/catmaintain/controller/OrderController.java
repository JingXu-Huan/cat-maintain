package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.order.OrderCreateRequest;
import com.jingxu.catmaintain.dto.order.OrderPageResponse;
import com.jingxu.catmaintain.dto.order.OrderResponse;
import com.jingxu.catmaintain.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request, HttpSession session) {
        return orderService.create(request, session);
    }

    @GetMapping
    public OrderPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        return orderService.listForUser(page, size, session);
    }

    @GetMapping("/{id}")
    public OrderResponse detail(@PathVariable Long id, HttpSession session) {
        return orderService.detailForUser(id, session);
    }
}
