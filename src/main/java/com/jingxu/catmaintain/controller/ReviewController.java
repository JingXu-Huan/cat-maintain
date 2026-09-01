package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.dto.review.ReviewCreateRequest;
import com.jingxu.catmaintain.dto.review.ReviewResponse;
import com.jingxu.catmaintain.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@Valid @RequestBody ReviewCreateRequest request, HttpSession session) {
        return reviewService.create(request, session);
    }

    @GetMapping
    public List<ReviewResponse> list(HttpSession session) {
        return reviewService.listForUser(session);
    }
}
