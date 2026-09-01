package com.jingxu.catmaintain.dto.store;

import com.jingxu.catmaintain.domain.store.MerchantStore;

public record StoreResponse(Long id, String storeName, String contactName, String phone, String address) {

    public static StoreResponse from(MerchantStore store) {
        return new StoreResponse(store.getId(), store.getStoreName(), store.getContactName(), store.getPhone(), store.getAddress());
    }
}
