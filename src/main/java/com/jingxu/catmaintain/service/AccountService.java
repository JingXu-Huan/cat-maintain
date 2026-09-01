package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.domain.store.MerchantStore;
import com.jingxu.catmaintain.dto.auth.LoginRequest;
import com.jingxu.catmaintain.dto.auth.StoreRegisterRequest;
import com.jingxu.catmaintain.dto.auth.UserRegisterRequest;
import com.jingxu.catmaintain.exception.BusinessException;
import com.jingxu.catmaintain.mapper.AccountMapper;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final MerchantStoreMapper merchantStoreMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Account registerUser(UserRegisterRequest request) {
        ensureUsernameAvailable(request.username());

        Account account = new Account(
                normalizeUsername(request.username()),
                passwordEncoder.encode(request.password()),
                request.phone().trim(),
                AccountRole.USER,
                AccountStatus.ACTIVE
        );
        accountMapper.insert(account);
        return account;
    }

    @Transactional
    public Account registerStore(StoreRegisterRequest request) {
        ensureUsernameAvailable(request.username());

        Account account = new Account(
                normalizeUsername(request.username()),
                passwordEncoder.encode(request.password()),
                request.phone().trim(),
                AccountRole.STORE,
                AccountStatus.PENDING
        );
        accountMapper.insert(account);
        merchantStoreMapper.insert(new MerchantStore(
                account.getId(),
                request.storeName(),
                request.contactName(),
                request.phone(),
                request.address()
        ));
        return account;
    }

    public Account authenticate(LoginRequest request) {
        Account account = accountMapper.findByUsername(request.username());
        if (account == null || !passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        if (account.getStatus() == AccountStatus.PENDING) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "加盟店账号正在等待平台审核");
        }
        if (account.getStatus() == AccountStatus.REJECTED) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_REJECTED", "加盟店注册申请未通过");
        }
        return account;
    }

    public Account findById(Long id) {
        Account account = accountMapper.findById(id);
        if (account == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "SESSION_ACCOUNT_NOT_FOUND", "登录会话已失效");
        }
        return account;
    }

    private void ensureUsernameAvailable(String username) {
        if (accountMapper.findByUsername(normalizeUsername(username)) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "用户名已存在");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }
}
