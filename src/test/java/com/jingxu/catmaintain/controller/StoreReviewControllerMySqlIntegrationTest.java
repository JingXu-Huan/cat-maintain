package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.domain.store.MerchantStoreApplication;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StoreReviewControllerMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantStoreMapper merchantStoreMapper;

    @Test
    @Transactional
    void adminCanApprovePendingStoreAndRepeatedReviewFails() throws Exception {
        String username = registerStore("approve_store");
        Long storeId = findStoreId(username);
        MockHttpSession adminSession = login("admin", "Admin123!");

        mockMvc.perform(get("/api/admin/stores")
                        .param("status", "PENDING")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == '%s')].status".formatted(username)).value("PENDING"));

        mockMvc.perform(put("/api/admin/stores/{storeId}/approve", storeId)
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/admin/stores/{storeId}/approve", storeId)
                        .session(adminSession))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STORE_REVIEW_ALREADY_PROCESSED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "Password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.status").value("ACTIVE"));
    }

    @Test
    @Transactional
    void normalUserCannotReviewStore() throws Exception {
        registerStore("forbidden_store");
        String username = registerUser("normal_user");
        Long storeId = findStoreIdByPrefix("forbidden_store");
        MockHttpSession userSession = login(username, "Password123");

        mockMvc.perform(put("/api/admin/stores/{storeId}/approve", storeId)
                        .session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @Transactional
    void rejectedStoreCannotLogin() throws Exception {
        String username = registerStore("reject_store");
        Long storeId = findStoreId(username);
        MockHttpSession adminSession = login("admin", "Admin123!");

        mockMvc.perform(put("/api/admin/stores/{storeId}/reject", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"资料不完整\"}")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewRemark").value("资料不完整"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "Password123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_REJECTED"));
    }

    private String registerStore(String prefix) throws Exception {
        String username = uniqueUsername(prefix);
        mockMvc.perform(post("/api/auth/register/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Password123",
                                  "storeName": "测试门店",
                                  "contactName": "测试联系人",
                                  "phone": "13500000000",
                                  "address": "测试地址"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());
        return username;
    }

    private String registerUser(String prefix) throws Exception {
        String username = uniqueUsername(prefix);
        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Password123",
                                  "phone": "13600000000"
                                }
                                """.formatted(username)))
                .andExpect(status().isCreated());
        return username;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Long findStoreId(String username) {
        return merchantStoreMapper.findApplicationsByStatus(AccountStatus.PENDING).stream()
                .filter(application -> application.getUsername().equals(username))
                .map(MerchantStoreApplication::getId)
                .findFirst()
                .orElseThrow();
    }

    private Long findStoreIdByPrefix(String prefix) {
        List<MerchantStoreApplication> applications = merchantStoreMapper
                .findApplicationsByStatus(AccountStatus.PENDING);
        return applications.stream()
                .filter(application -> application.getUsername().startsWith(prefix + "_"))
                .map(MerchantStoreApplication::getId)
                .findFirst()
                .orElseThrow();
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
