package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.product.Product;
import com.jingxu.catmaintain.domain.product.ProductStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("""
            SELECT id, sku, product_name, brand, description, price, labor_fee,
                   stock, status, created_at, updated_at
            FROM products
            WHERE id = #{id}
            """)
    Product findById(@Param("id") Long id);

    @Select("""
            SELECT id, sku, product_name, brand, description, price, labor_fee,
                   stock, status, created_at, updated_at
            FROM products
            WHERE id = #{id} AND status = 'ACTIVE' AND stock > 0
            """)
    Product findSaleableById(@Param("id") Long id);

    @Select("""
            SELECT id, sku, product_name, brand, description, price, labor_fee,
                   stock, status, created_at, updated_at
            FROM products
            WHERE sku = #{sku}
            """)
    Product findBySku(@Param("sku") String sku);

    @Select("""
            <script>
            SELECT id, sku, product_name, brand, description, price, labor_fee,
                   stock, status, created_at, updated_at
            FROM products
            <where>
                <if test="status != null">status = #{status}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{size} OFFSET #{offset}
            </script>
            """)
    List<Product> findPage(@Param("status") ProductStatus status,
                           @Param("offset") int offset,
                           @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM products
            <where>
                <if test="status != null">status = #{status}</if>
            </where>
            </script>
            """)
    long count(@Param("status") ProductStatus status);

    @Select("""
            <script>
            SELECT id, sku, product_name, brand, description, price, labor_fee,
                   stock, status, created_at, updated_at
            FROM products
            WHERE status = 'ACTIVE' AND stock > 0 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            ORDER BY id
            </script>
            """)
    List<Product> findSaleableByIds(@Param("ids") List<Long> ids);

    @Insert("""
            INSERT INTO products (sku, product_name, brand, description, price, labor_fee, stock, status)
            VALUES (#{sku}, #{productName}, #{brand}, #{description}, #{price}, #{laborFee}, #{stock}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Product product);

    @Update("""
            UPDATE products
            SET sku = #{sku}, product_name = #{productName}, brand = #{brand}, description = #{description},
                price = #{price}, labor_fee = #{laborFee}, stock = #{stock}
            WHERE id = #{id}
            """)
    int update(Product product);

    @Update("""
            UPDATE products
            SET status = #{status}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, @Param("status") ProductStatus status);

    @Update("""
            UPDATE products
            SET stock = stock + #{delta}
            WHERE id = #{id} AND stock >= -#{delta}
            """)
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    @Delete("""
            DELETE FROM products
            WHERE id = #{id}
            """)
    int deleteById(@Param("id") Long id);
}
