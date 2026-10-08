package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.review.ProductReviewCreateRequest;
import com.jingxu.catmaintain.dto.review.ProductReviewPageResponse;
import com.jingxu.catmaintain.dto.review.ProductReviewResponse;
import com.jingxu.catmaintain.service.ProductReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewService productReviewService;

    @PostMapping("/api/product-reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductReviewResponse create(@Valid @RequestBody ProductReviewCreateRequest request, HttpSession session) {
        return productReviewService.create(request, session);
    }

    @GetMapping("/api/product-reviews")
    public List<ProductReviewResponse> mine(HttpSession session) {
        return productReviewService.listForUser(session);
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ProductReviewPageResponse list(@PathVariable Long productId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return productReviewService.listForProduct(productId, page, size);
    }
}
