package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.review.ProductReview;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductReviewMapper {
    String SELECT_REVIEW = """
            SELECT r.id, r.account_id, r.order_id, r.product_id, r.rating, r.content, r.created_at,
                   p.product_name, a.username AS reviewer_name
            FROM product_reviews r
            JOIN products p ON p.id = r.product_id
            JOIN accounts a ON a.id = r.account_id
            """;

    @Select(SELECT_REVIEW + " WHERE r.id = #{id}")
    ProductReview findById(@Param("id") Long id);

    @Select(SELECT_REVIEW + " WHERE r.account_id = #{accountId} ORDER BY r.created_at DESC, r.id DESC")
    List<ProductReview> findByAccount(@Param("accountId") Long accountId);

    @Select(SELECT_REVIEW + " WHERE r.product_id = #{productId} ORDER BY r.created_at DESC, r.id DESC LIMIT #{size} OFFSET #{offset}")
    List<ProductReview> findPageByProduct(@Param("productId") Long productId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM product_reviews WHERE product_id = #{productId}")
    long countByProduct(@Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM order_items WHERE order_id = #{orderId} AND product_id = #{productId}")
    int countPurchasedProduct(@Param("orderId") Long orderId, @Param("productId") Long productId);

    @Insert("""
            INSERT INTO product_reviews (account_id, order_id, product_id, rating, content)
            VALUES (#{accountId}, #{orderId}, #{productId}, #{rating}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ProductReview review);
}
