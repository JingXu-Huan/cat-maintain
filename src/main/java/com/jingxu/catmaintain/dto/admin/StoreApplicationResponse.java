package com.jingxu.catmaintain.dto.admin;

import com.jingxu.catmaintain.domain.store.MerchantStoreApplication;

import java.time.LocalDateTime;

public record StoreApplicationResponse(
        Long id,
        Long accountId,
        String username,
        String storeName,
        String contactName,
        String phone,
        String address,
        String status,
        String reviewRemark,
        LocalDateTime reviewedAt
) {

    public static StoreApplicationResponse from(MerchantStoreApplication application) {
        return new StoreApplicationResponse(
                application.getId(),
                application.getAccountId(),
                application.getUsername(),
                application.getStoreName(),
                application.getContactName(),
                application.getPhone(),
                application.getAddress(),
                application.getStatus().name(),
                application.getReviewRemark(),
                application.getReviewedAt()
        );
    }
}
