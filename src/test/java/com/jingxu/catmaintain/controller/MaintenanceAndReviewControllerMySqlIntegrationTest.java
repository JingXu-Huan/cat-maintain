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

import java.time.LocalDateTime;
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
class MaintenanceAndReviewControllerMySqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantStoreMapper merchantStoreMapper;

    @Test
    @Transactional
    void appointmentMaintenanceCompletionAndReviewFormOneOwnedWorkflow() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("service_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        approveStore(adminSession, storeId);
        MockHttpSession storeSession = login(storeUsername, "Password123");
        String userUsername = registerUser("service_user_" + suffix);
        MockHttpSession userSession = login(userUsername, "Password123");
        long productId = createProduct(adminSession, "SERVICE-SKU-" + suffix);
        long orderId = createOrder(userSession, storeId, productId);
        approveAndVerify(adminSession, storeSession, orderId);

        String appointmentTime = LocalDateTime.now().plusDays(1).withNano(0).toString();
        MvcResult appointmentResult = mockMvc.perform(post("/api/appointments")
                        .session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId":%d,"orderId":%d,"appointmentTime":"%s",
                                 "vehiclePlate":"粤A12345","vehicleModel":"测试车型","remark":"请提前联系"}
                                """.formatted(storeId, orderId, appointmentTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long appointmentId = extractLong(appointmentResult, "id");

        mockMvc.perform(put("/api/store/appointments/{id}/confirm", appointmentId).session(storeSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        MvcResult startResult = mockMvc.perform(post("/api/store/maintenance-records/appointments/{id}/start", appointmentId)
                        .session(storeSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"已开始更换机油滤芯\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(appointmentId))
                .andExpect(jsonPath("$.serviceCompletedAt").doesNotExist())
                .andReturn();
        long recordId = extractLong(startResult, "id");

        mockMvc.perform(put("/api/store/maintenance-records/{id}/complete", recordId)
                        .session(storeSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mileage\":52000,\"content\":\"更换完成并完成检查\",\"remark\":\"车辆状态正常\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileage").value(52000))
                .andExpect(jsonPath("$.serviceCompletedAt").isNotEmpty());
        mockMvc.perform(get("/api/orders/{id}", orderId).session(userSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/reviews").session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"rating\":5,\"content\":\"服务很好\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rating").value(5));
        mockMvc.perform(post("/api/reviews").session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"rating\":4,\"content\":\"重复评价\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
        mockMvc.perform(get("/api/stores/{storeId}/reviews", storeId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].orderId").value(orderId));
        mockMvc.perform(get("/api/maintenance-records").session(userSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(recordId));

        String productReview = "{\"orderId\":" + orderId + ",\"productId\":" + productId + ",\"rating\":5,\"content\":\"配件质量很好\"}";
        mockMvc.perform(post("/api/product-reviews").session(userSession).contentType(MediaType.APPLICATION_JSON).content(productReview))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.productId").value(productId));
        mockMvc.perform(post("/api/product-reviews").session(userSession).contentType(MediaType.APPLICATION_JSON).content(productReview))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PRODUCT_REVIEW_ALREADY_EXISTS"));
        mockMvc.perform(get("/api/products/{id}/reviews", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].rating").value(5));
        mockMvc.perform(get("/api/product-reviews").session(userSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(productId));
        long unpurchasedId = createProduct(adminSession, "UNPURCHASED-" + suffix);
        mockMvc.perform(post("/api/product-reviews").session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"productId\":" + unpurchasedId + ",\"rating\":5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PRODUCT_NOT_PURCHASED"));
        MockHttpSession otherSession = login(registerUser("review_other_" + suffix), "Password123");
        mockMvc.perform(post("/api/product-reviews").session(otherSession).contentType(MediaType.APPLICATION_JSON).content(productReview))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("REVIEW_ACCESS_DENIED"));
    }

    @Test
    @Transactional
    void userCheckInValidatesOwnershipAndCodeAndIsIdempotent() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("checkin_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        approveStore(adminSession, storeId);
        MockHttpSession storeSession = login(storeUsername, "Password123");
        MockHttpSession userSession = login(registerUser("checkin_user_" + suffix), "Password123");
        MockHttpSession otherSession = login(registerUser("checkin_other_" + suffix), "Password123");
        long productId = createProduct(adminSession, "CHECKIN-" + suffix);
        long orderId = createOrder(userSession, storeId, productId);
        mockMvc.perform(put("/api/admin/orders/{id}/approve", orderId).session(adminSession)).andExpect(status().isOk());
        MvcResult delivered = mockMvc.perform(put("/api/admin/orders/{id}/deliver", orderId).session(adminSession))
                .andExpect(status().isOk()).andReturn();
        String code = extractString(delivered, "verificationCode");
        String appointmentBody = """
                {"storeId":%d,"orderId":%d,"appointmentTime":"%s","vehiclePlate":"粤B12345"}
                """.formatted(storeId, orderId, LocalDateTime.now().plusDays(1).withNano(0));
        MvcResult created = mockMvc.perform(post("/api/appointments").session(userSession)
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody))
                .andExpect(status().isCreated()).andReturn();
        long appointmentId = extractLong(created, "id");
        String checkInBody = "{\"appointmentId\":" + appointmentId + ",\"verificationCode\":\"" + code + "\"}";

        mockMvc.perform(post("/api/stores/{id}/check-ins", storeId).session(userSession)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("APPOINTMENT_NOT_CONFIRMED"));
        mockMvc.perform(put("/api/store/appointments/{id}/confirm", appointmentId).session(storeSession))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/appointments").session(userSession).contentType(MediaType.APPLICATION_JSON).content(appointmentBody))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ORDER_ALREADY_APPOINTED"));
        mockMvc.perform(post("/api/stores/{id}/check-ins", storeId).session(otherSession)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CHECK_IN_ACCESS_DENIED"));
        String wrongCode = code.equals("00000000") ? "11111111" : "00000000";
        mockMvc.perform(post("/api/stores/{id}/check-ins", storeId).session(userSession)
                        .contentType(MediaType.APPLICATION_JSON).content(checkInBody.replace(code, wrongCode)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));
        mockMvc.perform(get("/api/orders/{id}", orderId).session(userSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));
        mockMvc.perform(get("/api/appointments").session(userSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].checkedInAt").doesNotExist());
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/stores/{id}/check-ins", storeId).session(userSession)
                            .contentType(MediaType.APPLICATION_JSON).content(checkInBody))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.appointment.checkedInAt").isNotEmpty())
                    .andExpect(jsonPath("$.order.status").value("VERIFIED"));
        }
        mockMvc.perform(get("/api/store/orders/lookup-appointments").param("code", code).session(storeSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(appointmentId))
                .andExpect(jsonPath("$[0].checkedInAt").isNotEmpty());
        mockMvc.perform(put("/api/appointments/{id}/cancel", appointmentId).session(userSession))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/store/profile").session(userSession)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/store/profile").session(storeSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(storeId));
    }

    @Test
    @Transactional
    void appointmentMustBeFutureAndReviewNeedsCompletedOrder() throws Exception {
        String suffix = uniqueSuffix();
        String storeUsername = registerStore("validation_store_" + suffix);
        long storeId = findPendingStore(storeUsername).getId();
        MockHttpSession adminSession = login("admin", "Admin123!");
        approveStore(adminSession, storeId);
        MockHttpSession userSession = login(registerUser("validation_user_" + suffix), "Password123");

        mockMvc.perform(post("/api/appointments").session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId":%d,"appointmentTime":"2020-01-01T10:00:00","vehiclePlate":"粤A00001"}
                                """.formatted(storeId)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(post("/api/reviews").session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":999999999,\"rating\":6}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private void approveStore(MockHttpSession adminSession, long storeId) throws Exception {
        mockMvc.perform(put("/api/admin/stores/{id}/approve", storeId).session(adminSession))
                .andExpect(status().isOk());
    }

    private void approveAndVerify(MockHttpSession adminSession, MockHttpSession storeSession, long orderId) throws Exception {
        mockMvc.perform(put("/api/admin/orders/{id}/approve", orderId).session(adminSession)).andExpect(status().isOk());
        MvcResult delivered = mockMvc.perform(put("/api/admin/orders/{id}/deliver", orderId).session(adminSession))
                .andExpect(status().isOk()).andReturn();
        String code = extractString(delivered, "verificationCode");
        mockMvc.perform(put("/api/store/orders/{id}/verify", orderId).session(storeSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verificationCode\":\"" + code + "\"}"))
                .andExpect(status().isOk());
    }

    private long createOrder(MockHttpSession userSession, long storeId, long productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders").session(userSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":" + storeId + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        return extractLong(result, "id");
    }

    private long createProduct(MockHttpSession adminSession, String sku) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/products").session(adminSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"" + sku + "\",\"productName\":\"测试机油滤芯\",\"price\":99.90,\"laborFee\":30.00,\"stock\":2}"))
                .andExpect(status().isCreated()).andReturn();
        return extractLong(result, "id");
    }

    private String registerStore(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register/store").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"%s","password":"Password123","storeName":"保养测试门店",
                         "contactName":"测试联系人","phone":"13500000000","address":"测试地址"}
                        """.formatted(username))).andExpect(status().isCreated());
        return username;
    }

    private String registerUser(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register/user").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Password123\",\"phone\":\"13600000000\"}"))
                .andExpect(status().isCreated());
        return username;
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
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
