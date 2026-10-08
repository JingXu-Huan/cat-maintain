package com.jingxu.catmaintain.controller;

import com.jingxu.catmaintain.domain.account.AccountStatus;
import com.jingxu.catmaintain.domain.store.MerchantStoreApplication;
import com.jingxu.catmaintain.mapper.MerchantStoreMapper;
import com.jingxu.catmaintain.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantStoreMapper merchantStoreMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void orderSnapshotApprovalDeliveryAndIdempotentVerificationWork() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("order_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        mockMvc.perform(put("/api/admin/stores/{id}/approve", storeId).session(adminSession))
                .andExpect(status().isOk());
        MockHttpSession storeSession = login(storeUsername, "Password123");

        String userUsername = registerUser("order_user_" + suffix);
        MockHttpSession userSession = login(userUsername, "Password123");
        long productId = createProduct(adminSession, "ORDER-SKU-" + suffix, 2);

        MvcResult createOrder = mockMvc.perform(post("/api/orders")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":" + storeId + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.productAmount").value(99.90))
                .andExpect(jsonPath("$.laborFeeAmount").value(30.00))
                .andExpect(jsonPath("$.items[0].productName").value("测试机油滤芯"))
                .andReturn();
        long orderId = extractLong(createOrder, "id");

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(2));

        mockMvc.perform(get("/api/orders").session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId))
                .andExpect(jsonPath("$.content[0].items.length()").value(1));

        mockMvc.perform(put("/api/admin/orders/{id}/approve", orderId).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        MvcResult delivered = mockMvc.perform(put("/api/admin/orders/{id}/deliver", orderId).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.verificationCode").isString())
                .andReturn();
        String code = extractString(delivered, "verificationCode");
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(1));
        mockMvc.perform(put("/api/admin/orders/{id}/deliver", orderId).session(adminSession))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(1));
        if (!code.matches("\\d{8}")) {
            throw new IllegalStateException("核销码不是 8 位数字");
        }

        mockMvc.perform(get("/api/store/orders/lookup").param("code", code).session(storeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));
        String verifyBody = "{\"verificationCode\":\"" + code + "\"}";
        mockMvc.perform(put("/api/store/orders/{id}/verify", orderId)
                        .session(storeSession).contentType(MediaType.APPLICATION_JSON).content(verifyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
        mockMvc.perform(put("/api/store/orders/{id}/verify", orderId)
                        .session(storeSession).contentType(MediaType.APPLICATION_JSON).content(verifyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    @Transactional
    void insufficientStockRollsBackOrderCreation() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("rollback_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        mockMvc.perform(put("/api/admin/stores/{id}/approve", storeId).session(adminSession))
                .andExpect(status().isOk());
        MockHttpSession userSession = login(registerUser("rollback_user_" + suffix), "Password123");
        long productId = createProduct(adminSession, "ROLLBACK-SKU-" + suffix, 1);

        mockMvc.perform(post("/api/orders")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":" + storeId + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":2}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    @Transactional
    void userCannotReadAnotherUsersOrder() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("access_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        mockMvc.perform(put("/api/admin/stores/{id}/approve", storeId).session(adminSession))
                .andExpect(status().isOk());
        MockHttpSession ownerSession = login(registerUser("owner_" + suffix), "Password123");
        MockHttpSession otherSession = login(registerUser("other_" + suffix), "Password123");
        long productId = createProduct(adminSession, "ACCESS-SKU-" + suffix, 1);
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .session(ownerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":" + storeId + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        long orderId = extractLong(result, "id");

        mockMvc.perform(get("/api/orders/{id}", orderId).session(otherSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORDER_ACCESS_DENIED"));
    }

    @Test
    void failedDeliveryRollsBackAllStockChangesAndKeepsApprovedOrder() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = "stock_store_" + suffix;
        String userUsername = "stock_user_" + suffix;
        // 让请求真正提交/回滚自己的事务，避免外层测试事务掩盖配送回滚结果。
        try {
            registerStore(storeUsername);
            long storeId = findPendingStore(storeUsername).getId();
            MockHttpSession adminSession = login("admin", "Admin123!");
            mockMvc.perform(put("/api/admin/stores/{id}/approve", storeId).session(adminSession)).andExpect(status().isOk());
            MockHttpSession userSession = login(registerUser(userUsername), "Password123");
            long firstProductId = createProduct(adminSession, "STOCK-FIRST-" + suffix, 2);
            long secondProductId = createProduct(adminSession, "STOCK-SECOND-" + suffix, 2);
            MvcResult created = mockMvc.perform(post("/api/orders").session(userSession).contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"storeId":%d,"items":[{"productId":%d,"quantity":1},{"productId":%d,"quantity":1}]}
                                    """.formatted(storeId, firstProductId, secondProductId)))
                    .andExpect(status().isCreated()).andReturn();
            long orderId = extractLong(created, "id");
            assertThat(productMapper.findById(firstProductId).getStock()).isEqualTo(2);
            assertThat(productMapper.findById(secondProductId).getStock()).isEqualTo(2);
            mockMvc.perform(put("/api/admin/orders/{id}/approve", orderId).session(adminSession)).andExpect(status().isOk());
            mockMvc.perform(post("/api/admin/products/{id}/stock", secondProductId).session(adminSession)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"delta\":-2}"))
                    .andExpect(status().isOk());
            mockMvc.perform(put("/api/admin/orders/{id}/deliver", orderId).session(adminSession))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
            assertThat(productMapper.findById(firstProductId).getStock()).isEqualTo(2);
            assertThat(productMapper.findById(secondProductId).getStock()).isZero();
            mockMvc.perform(get("/api/orders/{id}", orderId).session(userSession))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.verificationCode").doesNotExist());
        } finally {
            jdbcTemplate.update("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE account_id IN (SELECT id FROM accounts WHERE username = ?))", userUsername);
            jdbcTemplate.update("DELETE FROM orders WHERE account_id IN (SELECT id FROM accounts WHERE username = ?)", userUsername);
            jdbcTemplate.update("DELETE FROM products WHERE sku IN (?, ?)", "STOCK-FIRST-" + suffix, "STOCK-SECOND-" + suffix);
            jdbcTemplate.update("DELETE FROM merchant_stores WHERE account_id IN (SELECT id FROM accounts WHERE username = ?)", storeUsername);
            jdbcTemplate.update("DELETE FROM accounts WHERE username IN (?, ?)", userUsername, storeUsername);
        }
    }

    @Test
    @Transactional
    void rejectionKeepsNewStockAndRestoresLegacyReservationOnce() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("reject_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        mockMvc.perform(put("/api/admin/stores/{id}/approve", storeId).session(adminSession)).andExpect(status().isOk());
        MockHttpSession userSession = login(registerUser("reject_user_" + suffix), "Password123");
        long productId = createProduct(adminSession, "REJECT-" + suffix, 2);
        String body = "{\"storeId\":" + storeId + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}";
        MvcResult created = mockMvc.perform(post("/api/orders").session(userSession).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        mockMvc.perform(put("/api/admin/orders/{id}/reject", extractLong(created, "id")).session(adminSession))
                .andExpect(status().isOk());
        assertThat(productMapper.findById(productId).getStock()).isEqualTo(2);

        MvcResult legacy = mockMvc.perform(post("/api/orders").session(userSession).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        long legacyId = extractLong(legacy, "id");
        productMapper.adjustStock(productId, -1);
        jdbcTemplate.update("UPDATE orders SET stock_deducted = TRUE WHERE id = ?", legacyId);
        mockMvc.perform(put("/api/admin/orders/{id}/reject", legacyId).session(adminSession)).andExpect(status().isOk());
        assertThat(productMapper.findById(productId).getStock()).isEqualTo(2);
        mockMvc.perform(put("/api/admin/orders/{id}/reject", legacyId).session(adminSession)).andExpect(status().isConflict());
        assertThat(productMapper.findById(productId).getStock()).isEqualTo(2);
    }

    private long createProduct(MockHttpSession adminSession, String sku, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/products")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","productName":"测试机油滤芯","brand":"测试品牌",
                                 "description":"商品测试数据","price":99.90,"laborFee":30.00,"stock":%d}
                                """.formatted(sku, stock)))
                .andExpect(status().isCreated()).andReturn();
        return extractLong(result, "id");
    }

    private String registerStore(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Password123","storeName":"订单测试门店",
                                 "contactName":"测试联系人","phone":"13500000000","address":"测试地址"}
                                """.formatted(username)))
                .andExpect(status().isCreated());
        return username;
    }

    private String registerUser(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Password123\",\"phone\":\"13600000000\"}"))
                .andExpect(status().isCreated());
        return username;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private MerchantStoreApplication findPendingStore(String username) {
        return merchantStoreMapper.findApplicationsByStatus(AccountStatus.PENDING).stream()
                .filter(store -> store.getUsername().equals(username)).findFirst().orElseThrow();
    }

    private long extractLong(MvcResult result, String field) throws Exception {
        return Long.parseLong(extract(result, field, "\\d+"));
    }

    private String extractString(MvcResult result, String field) throws Exception {
        return extract(result, field, "[^\"]+");
    }

    private String extract(MvcResult result, String field, String pattern) throws Exception {
        Matcher matcher = Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"?(" + pattern + ")\\\"?").matcher(result.getResponse().getContentAsString());
        if (!matcher.find()) throw new IllegalStateException("响应中没有字段 " + field);
        return matcher.group(1);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
