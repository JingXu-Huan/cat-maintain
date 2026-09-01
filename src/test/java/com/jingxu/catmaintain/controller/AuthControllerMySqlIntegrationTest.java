package com.jingxu.catmaintain.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void userCanRegisterLoginReadSessionAndLogout() throws Exception {
        String username = uniqueUsername("http_user");
        String password = "Password123";
        String registerBody = """
                {
                  "username": "%s",
                  "password": "%s",
                  "phone": "13600000000"
                }
                """.formatted(username, password);

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.phone").value("13600000000"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.username").value(username))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NOT_LOGGED_IN"));
    }

    @Test
    @Transactional
    void storeRegistrationReturnsPendingAndBlocksLogin() throws Exception {
        String username = uniqueUsername("http_store");
        String storeBody = """
                {
                  "username": "%s",
                  "password": "Password123",
                  "storeName": "星河汽车保养店",
                  "contactName": "李师傅",
                  "phone": "13500000000",
                  "address": "经三路 2 号"
                }
                """.formatted(username);

        mockMvc.perform(post("/api/auth/register/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(storeBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(username)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
