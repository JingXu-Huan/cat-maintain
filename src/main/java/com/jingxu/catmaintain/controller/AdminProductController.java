package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.domain.product.ProductStatus;
import com.jingxu.catmaintain.dto.product.ProductPageResponse;
import com.jingxu.catmaintain.dto.product.ProductRequest;
import com.jingxu.catmaintain.dto.product.ProductResponse;
import com.jingxu.catmaintain.dto.product.ProductStatusRequest;
import com.jingxu.catmaintain.dto.product.StockAdjustmentRequest;
import com.jingxu.catmaintain.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ProductPageResponse list(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session
    ) {
        productService.requireAdminForController(session);
        return productService.listForAdmin(status, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, HttpSession session) {
        return productService.create(request, session);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request, HttpSession session) {
        return productService.update(id, request, session);
    }

    @PutMapping("/{id}/status")
    public ProductResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusRequest request,
            HttpSession session
    ) {
        return productService.updateStatus(id, request.status(), session);
    }

    @PostMapping("/{id}/stock")
    public ProductResponse adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request,
            HttpSession session
    ) {
        return productService.adjustStock(id, request.delta(), session);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpSession session) {
        productService.delete(id, session);
    }
}
