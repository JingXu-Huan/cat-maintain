package com.jingxu.catmaintain.service;

import com.jingxu.catmaintain.domain.account.Account;
import com.jingxu.catmaintain.domain.account.AccountRole;
import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.dto.auth.LoginRequest;
import com.jingxu.catmaintain.dto.auth.StoreRegisterRequest;
import com.jingxu.catmaintain.dto.auth.UserRegisterRequest;
import com.jingxu.catmaintain.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceMySqlIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Test
    @Transactional
    void userRegistrationPersistsPhoneAndAllowsLogin() {
        String username = uniqueUsername("user");
        String password = "Password123";

        Account registered = accountService.registerUser(
                new UserRegisterRequest(username, password, "13800000000")
        );
        Account persisted = accountService.findById(registered.getId());
        Account authenticated = accountService.authenticate(new LoginRequest(username, password));

        assertThat(persisted.getRole()).isEqualTo(AccountRole.USER);
        assertThat(persisted.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(persisted.getPhone()).isEqualTo("13800000000");
        assertThat(authenticated.getPasswordHash()).isNotEqualTo(password);
    }

    @Test
    @Transactional
    void storeRegistrationStartsPendingAndCannotLoginBeforeApproval() {
        String username = uniqueUsername("store");

        Account registered = accountService.registerStore(
                new StoreRegisterRequest(
                        username,
                        "Password123",
                        "明珠汽车保养店",
                        "张师傅",
                        "13900000000",
                        "华龙区示范路 1 号"
                )
        );

        assertThat(registered.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThatThrownBy(() -> accountService.authenticate(new LoginRequest(username, "Password123")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("ACCOUNT_NOT_ACTIVE");
                });
    }

    @Test
    @Transactional
    void wrongPasswordIsRejected() {
        String username = uniqueUsername("wrong");
        accountService.registerUser(new UserRegisterRequest(username, "Password123", "13700000000"));

        assertThatThrownBy(() -> accountService.authenticate(new LoginRequest(username, "WrongPassword")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_CREDENTIALS");
                });
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
