package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.review.Review;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Select("""
            SELECT id, account_id, store_id, order_id, rating, content, created_at
            FROM reviews WHERE account_id = #{accountId} AND order_id = #{orderId}
            """)
    Review findByAccountAndOrder(@Param("accountId") Long accountId, @Param("orderId") Long orderId);

    @Select("""
            <script>
            SELECT id, account_id, store_id, order_id, rating, content, created_at
            FROM reviews WHERE store_id = #{storeId}
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Review> findPageByStore(@Param("storeId") Long storeId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM reviews WHERE store_id = #{storeId}")
    long countByStore(@Param("storeId") Long storeId);

    @Select("""
            SELECT id, account_id, store_id, order_id, rating, content, created_at
            FROM reviews WHERE account_id = #{accountId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Review> findByAccount(@Param("accountId") Long accountId);

    @Insert("""
            INSERT INTO reviews (account_id, store_id, order_id, rating, content)
            VALUES (#{accountId}, #{storeId}, #{orderId}, #{rating}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Review review);
}
