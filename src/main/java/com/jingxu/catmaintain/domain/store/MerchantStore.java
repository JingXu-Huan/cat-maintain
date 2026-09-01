package com.jingxu.catmaintain.domain.store;

public class MerchantStore {

    private Long id;
    private Long accountId;
    private String storeName;
    private String contactName;
    private String phone;
    private String address;

    public MerchantStore() {
    }

    public MerchantStore(Long accountId, String storeName, String contactName, String phone, String address) {
        this.accountId = accountId;
        this.storeName = storeName;
        this.contactName = contactName;
        this.phone = phone;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
