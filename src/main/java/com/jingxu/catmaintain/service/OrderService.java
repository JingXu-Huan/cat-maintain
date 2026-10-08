package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.order.Order;
import com.jingxu.catmaintain.domain.order.OrderItem;
import com.jingxu.catmaintain.domain.order.OrderStatus;
import com.jingxu.catmaintain.domain.product.Product;
import com.jingxu.catmaintain.domain.store.MerchantStore;
import com.jingxu.catmaintain.dto.order.CartItemRequest;
import com.jingxu.catmaintain.dto.order.OrderCreateRequest;
import com.jingxu.catmaintain.dto.order.OrderDecisionRequest;
import com.jingxu.catmaintain.dto.order.OrderItemResponse;
import com.jingxu.catmaintain.dto.order.OrderPageResponse;
import com.jingxu.catmaintain.dto.order.OrderResponse;
import com.jingxu.catmaintain.dto.order.VerifyOrderRequest;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import com.jingxu.catmaintain.mapper.OrderMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final ProductService productService;
    private final MerchantStoreMapper merchantStoreMapper;
    private final SessionAccountService sessionAccountService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OrderResponse create(OrderCreateRequest request, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        MerchantStore store = merchantStoreMapper.findActiveById(request.storeId());
        if (store == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "STORE_NOT_FOUND", "目标加盟店不存在或未启用");
        }

        LinkedHashMap<Long, Integer> quantities = mergeItems(request.items());
        List<Product> products = productService.findSaleableByIds(new ArrayList<>(quantities.keySet()));
        if (products.size() != quantities.size()) {
            throw new BusinessException(HttpStatus.CONFLICT, "PRODUCT_NOT_AVAILABLE", "订单中包含不存在、下架或库存不足的商品");
        }
        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal productAmount = BigDecimal.ZERO;
        BigDecimal laborFeeAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productMap.get(entry.getKey());
            int quantity = entry.getValue();
            if (product.getStock() < quantity) {
                throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "商品库存不足，订单未创建");
            }
            BigDecimal productLine = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            BigDecimal laborLine = product.getLaborFee().multiply(BigDecimal.valueOf(quantity));
            productAmount = productAmount.add(productLine);
            laborFeeAmount = laborFeeAmount.add(laborLine);

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getProductName());
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setUnitLaborFee(product.getLaborFee());
            item.setLineTotal(productLine.add(laborLine));
            items.add(item);
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setAccountId(account.getId());
        order.setStoreId(store.getId());
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setProductAmount(productAmount);
        order.setLaborFeeAmount(laborFeeAmount);
        order.setTotalAmount(productAmount.add(laborFeeAmount));
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
        }
        orderMapper.insertItems(items);
        return response(order, items);
    }

    public OrderPageResponse listForUser(int page, int size, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        return page(account.getId(), null, page, size);
    }

    public OrderPageResponse listForStore(int page, int size, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.STORE);
        MerchantStore store = requireStore(account.getId());
        return page(null, store.getId(), page, size);
    }

    public OrderPageResponse listForAdmin(int page, int size, HttpSession session) {
        sessionAccountService.requireRole(session, AccountRole.ADMIN);
        return page(null, null, page, size);
    }

    public OrderResponse detailForUser(Long id, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.USER);
        Order order = requireOrder(id);
        if (!order.getAccountId().equals(account.getId())) {
            throw forbiddenOrder();
        }
        return response(order);
    }

    @Transactional
    public OrderResponse approve(Long id, HttpSession session) {
        sessionAccountService.requireRole(session, AccountRole.ADMIN);
        Order order = requireOrder(id);
        ensureStatus(order, OrderStatus.PENDING_APPROVAL);
        if (orderMapper.approve(id) == 0) {
            throw invalidState();
        }
        return response(requireOrder(id));
    }

    @Transactional
    public OrderResponse reject(Long id, OrderDecisionRequest request, HttpSession session) {
        sessionAccountService.requireRole(session, AccountRole.ADMIN);
        Order order = requireOrderForUpdate(id);
        ensureStatus(order, OrderStatus.PENDING_APPROVAL);
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "订单未通过平台审核" : request.reason().trim();
        if (orderMapper.reject(id, reason) == 0) {
            throw invalidState();
        }
        if (order.isStockDeducted()) {
            orderMapper.findItemsByOrderIds(List.of(id)).stream()
                    .sorted(java.util.Comparator.comparing(OrderItem::getProductId))
                    .forEach(item -> productService.restoreStockForOrder(item.getProductId(), item.getQuantity()));
        }
        return response(requireOrder(id));
    }

    @Transactional
    public OrderResponse deliver(Long id, HttpSession session) {
        sessionAccountService.requireRole(session, AccountRole.ADMIN);
        Order order = requireOrderForUpdate(id);
        ensureStatus(order, OrderStatus.APPROVED);
        // 按商品 ID 加锁，降低多商品订单并发配送时的死锁风险。
        List<OrderItem> items = orderMapper.findItemsByOrderIds(List.of(id));
        if (!order.isStockDeducted()) {
            items.stream().sorted(java.util.Comparator.comparing(OrderItem::getProductId))
                    .forEach(item -> productService.decreaseStockForOrder(item.getProductId(), item.getQuantity()));
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = generateVerificationCode();
            try {
                if (orderMapper.deliver(id, code) == 1) {
                    return response(requireOrder(id));
                }
            } catch (DuplicateKeyException ignored) {
                // 极低概率的随机码冲突，重新生成后重试。
            }
        }
        throw new BusinessException(HttpStatus.CONFLICT, "VERIFICATION_CODE_UNAVAILABLE", "暂时无法生成唯一核销码");
    }

    public OrderResponse lookupByCode(String code, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.STORE);
        MerchantStore store = requireStore(account.getId());
        Order order = orderMapper.findByVerificationCode(code);
        if (order == null || !order.getStoreId().equals(store.getId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "没有找到分配给当前门店的订单");
        }
        return response(order);
    }

    @Transactional
    public OrderResponse verify(Long id, VerifyOrderRequest request, HttpSession session) {
        Account account = sessionAccountService.requireRole(session, AccountRole.STORE);
        MerchantStore store = requireStore(account.getId());
        Order order = requireOrderForUpdate(id);
        if (!order.getStoreId().equals(store.getId())) {
            throw forbiddenOrder();
        }
        return verifyOrder(order, store.getId(), request.verificationCode());
    }

    @Transactional
    public OrderResponse verifyForCheckIn(Long id, Long accountId, Long storeId, String code) {
        Order order = requireOrderForUpdate(id);
        if (!order.getAccountId().equals(accountId) || !order.getStoreId().equals(storeId)) {
            throw forbiddenOrder();
        }
        return verifyOrder(order, storeId, code);
    }

    private OrderResponse verifyOrder(Order order, Long storeId, String code) {
        if (!code.equals(order.getVerificationCode())) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_VERIFICATION_CODE", "核销码不正确");
        }
        if (order.getStatus() == OrderStatus.VERIFIED || order.getStatus() == OrderStatus.COMPLETED) {
            return response(order);
        }
        ensureStatus(order, OrderStatus.DELIVERED);
        if (orderMapper.verify(order.getId(), storeId, code) == 0) {
            Order latest = requireOrder(order.getId());
            if (latest.getStatus() == OrderStatus.VERIFIED || latest.getStatus() == OrderStatus.COMPLETED) {
                return response(latest);
            }
            throw invalidState();
        }
        return response(requireOrder(order.getId()));
    }

    public Order requireOrderForUpdate(Long id) {
        Order order = orderMapper.findByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        return order;
    }

    @Transactional
    public void completeForMaintenance(Long orderId) {
        Order order = requireOrder(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return;
        }
        ensureStatus(order, OrderStatus.VERIFIED);
        if (orderMapper.complete(orderId) == 0) {
            throw invalidState();
        }
    }

    public Order requireOrder(Long id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        return order;
    }

    private OrderPageResponse page(Long accountId, Long storeId, int page, int size) {
        validatePage(page, size);
        List<Order> orders = orderMapper.findPage(accountId, storeId, page * size, size);
        Map<Long, List<OrderItem>> itemsByOrder = orders.isEmpty()
                ? Map.of()
                : orderMapper.findItemsByOrderIds(orders.stream().map(Order::getId).toList()).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        List<OrderResponse> content = orders.stream()
                .map(order -> response(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
        return new OrderPageResponse(content, orderMapper.count(accountId, storeId), page, size);
    }

    private OrderResponse response(Order order) {
        List<OrderItem> items = orderMapper.findItemsByOrderIds(List.of(order.getId()));
        return response(order, items);
    }

    private OrderResponse response(Order order, List<OrderItem> items) {
        return OrderResponse.from(order, items.stream().map(OrderItemResponse::from).toList());
    }

    private LinkedHashMap<Long, Integer> mergeItems(List<CartItemRequest> requests) {
        LinkedHashMap<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItemRequest request : requests) {
            int total = quantities.getOrDefault(request.productId(), 0) + request.quantity();
            if (total > 99) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "QUANTITY_TOO_LARGE", "同一商品数量不能超过 99");
            }
            quantities.put(request.productId(), total);
        }
        return quantities;
    }

    private MerchantStore requireStore(Long accountId) {
        MerchantStore store = merchantStoreMapper.findByAccountId(accountId);
        if (store == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "STORE_PROFILE_NOT_FOUND", "当前加盟店资料不存在");
        }
        return store;
    }

    private void ensureStatus(Order order, OrderStatus expected) {
        if (order.getStatus() != expected) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_ORDER_STATE", "订单当前状态不能执行此操作");
        }
    }

    private BusinessException invalidState() {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_ORDER_STATE", "订单状态已发生变化，请刷新后重试");
    }

    private BusinessException forbiddenOrder() {
        return new BusinessException(HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED", "无权访问该订单");
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(ORDER_TIME_FORMAT)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 15);
    }

    private String generateVerificationCode() {
        return String.format("%08d", secureRandom.nextInt(100_000_000));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "分页参数不合法，size 必须在 1 到 100 之间");
        }
    }
}
