package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.product.ProductPageResponse;
import com.jingxu.catmaintain.dto.product.ProductResponse;
import com.jingxu.catmaintain.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ProductPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.listSaleable(page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse detail(@PathVariable Long id) {
        return ProductResponse.from(productService.requireSaleable(id));
    }
}
