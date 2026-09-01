package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.dto.auth.AccountResponse;
import com.jingxu.catmaintain.dto.auth.LoginRequest;
import com.jingxu.catmaintain.dto.auth.LoginResponse;
import com.jingxu.catmaintain.dto.auth.StoreRegisterRequest;
import com.jingxu.catmaintain.dto.auth.StoreRegisterResponse;
import com.jingxu.catmaintain.dto.auth.UserRegisterRequest;
import com.jingxu.catmaintain.service.AccountService;
import com.jingxu.catmaintain.service.SessionAccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AccountService accountService;
    private final SessionAccountService sessionAccountService;

    @PostMapping("/register/user")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse registerUser(@Valid @RequestBody UserRegisterRequest request) {
        return AccountResponse.from(accountService.registerUser(request));
    }

    @PostMapping("/register/store")
    @ResponseStatus(HttpStatus.CREATED)
    public StoreRegisterResponse registerStore(@Valid @RequestBody StoreRegisterRequest request) {
        Account account = accountService.registerStore(request);
        return new StoreRegisterResponse(
                account.getId(),
                account.getUsername(),
                account.getStatus().name(),
                "加盟店注册成功，请等待平台审核"
        );
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpSession session
    ) {
        Account account = accountService.authenticate(request);
        servletRequest.changeSessionId();
        session.setAttribute(SessionAccountService.ACCOUNT_ID_ATTRIBUTE, account.getId());
        return new LoginResponse(AccountResponse.from(account), "登录成功");
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @GetMapping("/me")
    public AccountResponse currentAccount(HttpSession session) {
        return AccountResponse.from(sessionAccountService.require(session));
    }
}
