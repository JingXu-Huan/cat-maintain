package com.jingxu.catmaintain.dto.auth;

import com.jingxu.catmaintain.domain.account.Account;

public record AccountResponse(Long id, String username, String phone, String role, String status) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getPhone(),
                account.getRole().name(),
                account.getStatus().name()
        );
    }
}
