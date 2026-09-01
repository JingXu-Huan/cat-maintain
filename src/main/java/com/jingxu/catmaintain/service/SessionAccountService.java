package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.exception.BusinessException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionAccountService {

    public static final String ACCOUNT_ID_ATTRIBUTE = "accountId";

    private final AccountService accountService;

    public Account require(HttpSession session) {
        Object value = session.getAttribute(ACCOUNT_ID_ATTRIBUTE);
        if (!(value instanceof Number accountId)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "NOT_LOGGED_IN", "请先登录");
        }
        return accountService.findById(accountId.longValue());
    }

    public Account requireRole(HttpSession session, AccountRole expectedRole) {
        Account account = require(session);
        if (account.getRole() != expectedRole) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号无权执行此操作");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "当前账号未处于可用状态");
        }
        return account;
    }
}
