package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.order.Order;
import com.jingxu.catmaintain.domain.order.OrderItem;
import com.jingxu.catmaintain.domain.order.OrderStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Select("""
            SELECT id, order_no, account_id, store_id, status, product_amount, labor_fee_amount, total_amount,
                   verification_code, rejected_reason, approved_at, delivered_at, verified_at,
                   verified_by_store_id, completed_at, created_at, updated_at
            FROM orders WHERE id = #{id}
            """)
    Order findById(@Param("id") Long id);

    @Select("""
            SELECT id, order_no, account_id, store_id, status, product_amount, labor_fee_amount, total_amount,
                   verification_code, rejected_reason, approved_at, delivered_at, verified_at,
                   verified_by_store_id, completed_at, created_at, updated_at
            FROM orders WHERE verification_code = #{code}
            """)
    Order findByVerificationCode(@Param("code") String code);

    @Select("""
            <script>
            SELECT id, order_no, account_id, store_id, status, product_amount, labor_fee_amount, total_amount,
                   verification_code, rejected_reason, approved_at, delivered_at, verified_at,
                   verified_by_store_id, completed_at, created_at, updated_at
            FROM orders
            <where>
                <if test="accountId != null">account_id = #{accountId}</if>
                <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Order> findPage(@Param("accountId") Long accountId,
                         @Param("storeId") Long storeId,
                         @Param("offset") int offset,
                         @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM orders
            <where>
                <if test="accountId != null">account_id = #{accountId}</if>
                <if test="storeId != null">store_id = #{storeId}</if>
            </where>
            </script>
            """)
    long count(@Param("accountId") Long accountId, @Param("storeId") Long storeId);

    @Select("""
            <script>
            SELECT id, order_id, product_id, product_name, quantity, unit_price, unit_labor_fee, line_total
            FROM order_items
            WHERE order_id IN
            <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                #{orderId}
            </foreach>
            ORDER BY order_id, id
            </script>
            """)
    List<OrderItem> findItemsByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Insert("""
            INSERT INTO orders (order_no, account_id, store_id, status, product_amount, labor_fee_amount, total_amount)
            VALUES (#{orderNo}, #{accountId}, #{storeId}, #{status}, #{productAmount}, #{laborFeeAmount}, #{totalAmount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Order order);

    @Insert("""
            <script>
            INSERT INTO order_items
                (order_id, product_id, product_name, quantity, unit_price, unit_labor_fee, line_total)
            VALUES
            <foreach collection="items" item="item" separator=",">
                (#{item.orderId}, #{item.productId}, #{item.productName}, #{item.quantity},
                 #{item.unitPrice}, #{item.unitLaborFee}, #{item.lineTotal})
            </foreach>
            </script>
            """)
    int insertItems(@Param("items") List<OrderItem> items);

    @Update("""
            UPDATE orders SET status = 'APPROVED', approved_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'PENDING_APPROVAL'
            """)
    int approve(@Param("id") Long id);

    @Update("""
            UPDATE orders SET status = 'REJECTED', rejected_reason = #{reason}
            WHERE id = #{id} AND status = 'PENDING_APPROVAL'
            """)
    int reject(@Param("id") Long id, @Param("reason") String reason);

    @Update("""
            UPDATE orders SET status = 'DELIVERED', verification_code = #{code}, delivered_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'APPROVED'
            """)
    int deliver(@Param("id") Long id, @Param("code") String code);

    @Update("""
            UPDATE orders
            SET status = 'VERIFIED', verified_at = CURRENT_TIMESTAMP, verified_by_store_id = #{storeId}
            WHERE id = #{id} AND status = 'DELIVERED' AND verification_code = #{code}
            """)
    int verify(@Param("id") Long id, @Param("storeId") Long storeId, @Param("code") String code);

    @Update("""
            UPDATE orders SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND status = 'VERIFIED'
            """)
    int complete(@Param("id") Long id);
}
