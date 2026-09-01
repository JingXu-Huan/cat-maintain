package com.jingxu.catmaintain.domain.account;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Account {

    private Long id;
    private String username;
    private String passwordHash;
    private String phone;
    private AccountRole role;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account(String username, String passwordHash, String phone, AccountRole role, AccountStatus status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }
}
