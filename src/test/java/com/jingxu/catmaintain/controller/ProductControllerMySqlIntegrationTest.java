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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Transactional
    void adminCanCreateManageAndAdjustProductWithoutNegativeStock() throws Exception {
        MockHttpSession adminSession = login("admin", "Admin123!");
        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("SKU-" + uniqueSuffix(), 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.stock").value(3))
                .andReturn();
        long productId = productId(createResult);

        mockMvc.perform(post("/api/admin/products/{id}/stock", productId)
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(1));

        mockMvc.perform(post("/api/admin/products/{id}/stock", productId)
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":-2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(put("/api/admin/products/{id}/status", productId)
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
    }

    @Test
    @Transactional
    void publicListReturnsOnlyActiveProductsAndAdminEndpointRequiresAdmin() throws Exception {
        MockHttpSession adminSession = login("admin", "Admin123!");
        mockMvc.perform(post("/api/admin/products")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody("SKU-" + uniqueSuffix(), 1)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        String username = "product_user_" + uniqueSuffix();
        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Password123\",\"phone\":\"13600000000\"}"))
                .andExpect(status().isCreated());
        MockHttpSession userSession = login(username, "Password123");

        mockMvc.perform(get("/api/admin/products").session(userSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long productId(MvcResult result) throws Exception {
        Matcher matcher = Pattern.compile("\\\"id\\\"\\s*:\\s*(\\d+)").matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) {
            throw new IllegalStateException("创建商品响应中没有 id");
        }
        return Long.parseLong(matcher.group(1));
    }

    private String productBody(String sku, int stock) {
        return """
                {
                  "sku": "%s",
                  "productName": "测试机油滤芯",
                  "brand": "测试品牌",
                  "description": "商品测试数据",
                  "price": 99.90,
                  "laborFee": 30.00,
                  "stock": %d
                }
                """.formatted(sku, stock);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
