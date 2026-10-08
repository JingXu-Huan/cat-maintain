package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.order.Order;
import com.jingxu.catmaintain.domain.order.OrderStatus;
import com.jingxu.catmaintain.domain.review.ProductReview;
import com.jingxu.catmaintain.dto.review.ProductReviewCreateRequest;
import com.jingxu.catmaintain.dto.review.ProductReviewPageResponse;
import com.jingxu.catmaintain.dto.review.ProductReviewResponse;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.ProductMapper;
import com.jingxu.catmaintain.mapper.ProductReviewMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private final ProductReviewMapper productReviewMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;
    private final SessionAccountService sessionAccountService;

    @Transactional
    public ProductReviewResponse create(ProductReviewCreateRequest request, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        Order order = orderService.requireOrder(request.orderId());
        if (!order.getAccountId().equals(account.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "REVIEW_ACCESS_DENIED", "只能评价自己的订单商品");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.CONFLICT, "ORDER_NOT_COMPLETED", "只有已完成订单才能评价");
        }
        if (productReviewMapper.countPurchasedProduct(order.getId(), request.productId()) == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_PURCHASED", "该商品不在指定订单中");
        }
        ProductReview review = new ProductReview();
        review.setAccountId(account.getId());
        review.setOrderId(order.getId());
        review.setProductId(request.productId());
        review.setRating(request.rating());
        review.setContent(request.content() == null || request.content().isBlank() ? null : request.content().trim());
        try {
            productReviewMapper.insert(review);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "PRODUCT_REVIEW_ALREADY_EXISTS", "同一订单的同一商品只能评价一次");
        }
        return ProductReviewResponse.from(productReviewMapper.findById(review.getId()));
    }

    public List<ProductReviewResponse> listForUser(HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        return productReviewMapper.findByAccount(account.getId()).stream().map(ProductReviewResponse::from).toList();
    }

    public ProductReviewPageResponse listForProduct(Long productId, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "分页参数不合法");
        }
        if (productMapper.findById(productId) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        List<ProductReviewResponse> content = productReviewMapper.findPageByProduct(productId, page * size, size)
                .stream().map(ProductReviewResponse::from).toList();
        return new ProductReviewPageResponse(content, productReviewMapper.countByProduct(productId), page, size);
    }
}
