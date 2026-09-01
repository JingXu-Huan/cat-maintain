package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.review.ReviewPageResponse;
import com.jingxu.catmaintain.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores/{storeId}/reviews")
@RequiredArgsConstructor
public class StoreReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ReviewPageResponse list(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.listForStore(storeId, page, size);
    }
}
