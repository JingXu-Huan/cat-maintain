package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.order.Order;
import com.jingxu.catmaintain.domain.order.OrderStatus;
import com.jingxu.catmaintain.domain.review.Review;
import com.jingxu.catmaintain.dto.review.ReviewCreateRequest;
import com.jingxu.catmaintain.dto.review.ReviewPageResponse;
import com.jingxu.catmaintain.dto.review.ReviewResponse;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.ReviewMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderService orderService;
    private final SessionAccountService sessionAccountService;

    @Transactional
    public ReviewResponse create(ReviewCreateRequest request, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        Order order = orderService.requireOrder(request.orderId());
        if (!order.getAccountId().equals(account.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REVIEW_ACCESS_DENIED", "只能评价自己的订单");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.CONFLICT, "ORDER_NOT_COMPLETED", "只有已完成订单才能评价");
        }
        if (reviewMapper.findByAccountAndOrder(account.getId(), order.getId()) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "同一订单只能评价一次");
        }
        Review review = new Review();
        review.setAccountId(account.getId());
        review.setStoreId(order.getStoreId());
        review.setOrderId(order.getId());
        review.setRating(request.rating());
        review.setContent(normalize(request.content()));
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "同一订单只能评价一次");
        }
        return ReviewResponse.from(reviewMapper.findByAccountAndOrder(account.getId(), order.getId()));
    }

    public List<ReviewResponse> listForUser(HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        return reviewMapper.findByAccount(account.getId()).stream().map(ReviewResponse::from).toList();
    }

    public ReviewPageResponse listForStore(Long storeId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "分页参数不合法，size 必须在 1 到 100 之间");
        }
        List<ReviewResponse> content = reviewMapper.findPageByStore(storeId, page * size, size)
                .stream().map(ReviewResponse::from).toList();
        return new ReviewPageResponse(content, reviewMapper.countByStore(storeId), page, size);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
