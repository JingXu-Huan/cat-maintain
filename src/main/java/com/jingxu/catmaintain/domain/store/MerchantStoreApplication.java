package com.jingxu.catmaintain.domain.store;

import com.jingxu.catmaintain.domain.account.AccountStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MerchantStoreApplication {

    private Long id;
    private Long accountId;
    private String username;
    private String storeName;
    private String contactName;
    private String phone;
    private String address;
    private AccountStatus status;
    private String reviewRemark;
    private LocalDateTime reviewedAt;
}
