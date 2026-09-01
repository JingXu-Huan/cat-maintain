package com.jingxu.catmaintain.domain.store;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MerchantStore {

    private Long id;
    private Long accountId;
    private String storeName;
    private String contactName;
    private String phone;
    private String address;

    public MerchantStore(Long accountId, String storeName, String contactName, String phone, String address) {
        this.accountId = accountId;
        this.storeName = storeName;
        this.contactName = contactName;
        this.phone = phone;
        this.address = address;
    }
}
