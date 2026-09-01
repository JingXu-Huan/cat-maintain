package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.domain.store.MerchantStore;
import com.jingxu.catmaintain.domain.store.MerchantStoreApplication;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MerchantStoreMapper {

    @Insert("""
            INSERT INTO merchant_stores (account_id, store_name, contact_name, phone, address)
            VALUES (#{accountId}, #{storeName}, #{contactName}, #{phone}, #{address})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MerchantStore store);

    @Select("""
            SELECT s.id, s.account_id, a.username, s.store_name, s.contact_name,
                   s.phone, s.address, a.status, s.review_remark, s.reviewed_at
            FROM merchant_stores s
            JOIN accounts a ON a.id = s.account_id
            WHERE a.status = #{status}
            ORDER BY s.created_at DESC
            """)
    List<MerchantStoreApplication> findApplicationsByStatus(@Param("status") AccountStatus status);

    @Select("""
            SELECT s.id, s.account_id, a.username, s.store_name, s.contact_name,
                   s.phone, s.address, a.status, s.review_remark, s.reviewed_at
            FROM merchant_stores s
            JOIN accounts a ON a.id = s.account_id
            WHERE s.id = #{storeId}
            """)
    MerchantStoreApplication findApplicationById(@Param("storeId") Long storeId);

    @Update("""
            UPDATE merchant_stores
            SET review_remark = #{remark}, reviewed_at = CURRENT_TIMESTAMP
            WHERE id = #{storeId}
            """)
    int markReviewed(@Param("storeId") Long storeId, @Param("remark") String remark);

    @Select("""
            SELECT id, account_id, store_name, contact_name, phone, address
            FROM merchant_stores
            WHERE account_id = #{accountId}
            """)
    MerchantStore findByAccountId(@Param("accountId") Long accountId);

    @Select("""
            SELECT s.id, s.account_id, s.store_name, s.contact_name, s.phone, s.address
            FROM merchant_stores s
            JOIN accounts a ON a.id = s.account_id
            WHERE s.id = #{storeId} AND a.role = 'STORE' AND a.status = 'ACTIVE'
            """)
    MerchantStore findActiveById(@Param("storeId") Long storeId);

    @Select("""
            SELECT s.id, s.account_id, s.store_name, s.contact_name, s.phone, s.address
            FROM merchant_stores s
            JOIN accounts a ON a.id = s.account_id
            WHERE a.role = 'STORE' AND a.status = 'ACTIVE'
            ORDER BY s.store_name, s.id
            """)
    List<MerchantStore> findActiveStores();
}
