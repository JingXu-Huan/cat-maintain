package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.product.Product;
import com.jingxu.catmaintain.domain.product.ProductStatus;
import com.jingxu.catmaintain.dto.product.ProductPageResponse;
import com.jingxu.catmaintain.dto.product.ProductRequest;
import com.jingxu.catmaintain.dto.product.ProductResponse;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.ProductMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final SessionAccountService sessionAccountService;

    public ProductPageResponse listSaleable(int page, int size) {
        return page(ProductStatus.ACTIVE, page, size);
    }

    public ProductPageResponse listForAdmin(ProductStatus status, int page, int size) {
        return page(status, page, size);
    }

    public void requireAdminForController(HttpSession session) {
        sessionAccountService.requireRole(session, AccountRole.ADMIN);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, HttpSession session) {
        requireAdminForController(session);
        ensureSkuAvailable(request.sku().trim(), null);
        Product product = new Product();
        apply(product, request);
        product.setStatus(ProductStatus.ACTIVE);
        try {
            productMapper.insert(product);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "SKU_EXISTS", "SKU 已存在");
        }
        return ProductResponse.from(requireProduct(product.getId()));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, HttpSession session) {
        requireAdminForController(session);
        Product product = requireProduct(id);
        ensureSkuAvailable(request.sku().trim(), id);
        apply(product, request);
        productMapper.update(product);
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, ProductStatus status, HttpSession session) {
        requireAdminForController(session);
        requireProduct(id);
        productMapper.updateStatus(id, status);
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional
    public ProductResponse adjustStock(Long id, int delta, HttpSession session) {
        requireAdminForController(session);
        if (delta == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_STOCK_DELTA", "库存调整量不能为 0");
        }
        requireProduct(id);
        if (productMapper.adjustStock(id, delta) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "库存不足，不能减到负数");
        }
        return ProductResponse.from(requireProduct(id));
    }

    public Product requireSaleable(Long id) {
        Product product = productMapper.findSaleableById(id);
        if (product == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_AVAILABLE", "商品不存在、已下架或库存不足");
        }
        return product;
    }

    public List<Product> findSaleableByIds(List<Long> ids) {
        return productMapper.findSaleableByIds(ids);
    }

    private ProductPageResponse page(ProductStatus status, int page, int size) {
        validatePage(page, size);
        List<ProductResponse> content = productMapper.findPage(status, page * size, size)
                .stream().map(ProductResponse::from).toList();
        return new ProductPageResponse(content, productMapper.count(status), page, size);
    }

    private Product requireProduct(Long id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "商品不存在");
        }
        return product;
    }

    private void ensureSkuAvailable(String sku, Long currentId) {
        Product existing = productMapper.findBySku(sku);
        if (existing != null && !existing.getId().equals(currentId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "SKU_EXISTS", "SKU 已存在");
        }
    }

    private void apply(Product product, ProductRequest request) {
        product.setSku(request.sku().trim());
        product.setProductName(request.productName().trim());
        product.setBrand(normalize(request.brand()));
        product.setDescription(normalize(request.description()));
        product.setPrice(request.price());
        product.setLaborFee(request.laborFee());
        product.setStock(request.stock());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "分页参数不合法，size 必须在 1 到 100 之间");
        }
    }
}
