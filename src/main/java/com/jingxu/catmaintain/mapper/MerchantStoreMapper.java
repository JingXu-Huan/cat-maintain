package com.jingxu.catmaintain.mapper;

import com.jingxu.catmaintain.domain.store.MerchantStore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface MerchantStoreMapper {

    @Insert("""
            INSERT INTO merchant_stores (account_id, store_name, contact_name, phone, address)
            VALUES (#{accountId}, #{storeName}, #{contactName}, #{phone}, #{address})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(MerchantStore store);
}
