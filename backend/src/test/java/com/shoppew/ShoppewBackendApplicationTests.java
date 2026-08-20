package com.shoppew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoppew.auth.email.AuthenticationEmailGateway;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.service.InventoryReservationService;
import com.shoppew.media.StorageService;
import com.shoppew.notification.service.PushTargetCodec;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserProfileEntity;
import com.shoppew.user.entity.UserRole;
import com.shoppew.user.entity.UserStatus;
import com.shoppew.user.repository.UserProfileRepository;
import com.shoppew.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ShoppewBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InventoryReservationService inventoryReservationService;

    @Autowired
    private PushTargetCodec pushTargetCodec;

    @MockitoBean
    private AuthenticationEmailGateway authenticationEmailGateway;

    @MockitoBean
    private StorageService storageService;

    @Test
    void contextLoads() {}

    @Test
    void publicSystemEndpointUsesStableEnvelopeAndCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/public/system").header("X-Request-ID", "integration-test-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "integration-test-123"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiVersion").value("v1"))
                .andExpect(jsonPath("$.data.locale").value("vi-VN"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        mockMvc.perform(get("/api/v1/public/commerce-capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availablePaymentProviders").isArray())
                .andExpect(jsonPath("$.data.availablePaymentProviders[0]").value("COD"))
                .andExpect(jsonPath("$.data.availablePaymentProviders[1]").value("MOCK_ONLINE"))
                .andExpect(jsonPath("$.data.availableShippingMethods[0]").value("MOCK_STANDARD"));
    }

    @Test
    void protectedActuatorEndpointUsesJsonAuthenticationError() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    void pushDeviceRegistrationEncryptsTargetTransfersOwnershipAndRevokes() throws Exception {
        Instant now = Instant.now(clock);
        UserEntity first = userRepository.saveAndFlush(UserEntity.register(
                "push-owner-one@example.test",
                null,
                passwordEncoder.encode("PushDevice2026!"),
                UserStatus.ACTIVE,
                now));
        profileRepository.saveAndFlush(UserProfileEntity.create(first, "Push Owner One", now));
        UserEntity second = userRepository.saveAndFlush(UserEntity.register(
                "push-owner-two@example.test",
                null,
                passwordEncoder.encode("PushDevice2026!"),
                UserStatus.ACTIVE,
                now));
        profileRepository.saveAndFlush(UserProfileEntity.create(second, "Push Owner Two", now));
        String fid = "firebase-installation-id-for-shoppew-integration-test-2026";
        String request = """
                {"platform":"ANDROID","targetType":"FID","target":"%s"}
                """.formatted(fid);

        mockMvc.perform(put("/api/v1/notifications/devices/current")
                        .with(jwt().jwt(token -> token.subject(first.getId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform").value("ANDROID"))
                .andExpect(jsonPath("$.data.targetType").value("FID"))
                .andExpect(jsonPath("$.data.target").doesNotExist());

        String targetHash = jdbcTemplate.queryForObject(
                "select target_hash from push_devices where user_id = ?", String.class, first.getId());
        String encryptedTarget = jdbcTemplate.queryForObject(
                "select encrypted_target from push_devices where user_id = ?", String.class, first.getId());
        assertThat(targetHash).isNotEqualTo(fid);
        assertThat(encryptedTarget).doesNotContain(fid);
        assertThat(pushTargetCodec.decrypt(encryptedTarget, targetHash)).isEqualTo(fid);

        mockMvc.perform(put("/api/v1/notifications/devices/current")
                        .with(jwt().jwt(token -> token.subject(second.getId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from push_devices where target_hash = ?", Long.class, targetHash)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select user_id from push_devices where target_hash = ?", UUID.class, targetHash)).isEqualTo(second.getId());

        mockMvc.perform(delete("/api/v1/notifications/devices/current")
                        .with(jwt().jwt(token -> token.subject(first.getId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"" + fid + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PUSH_DEVICE_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/notifications/devices/current")
                        .with(jwt().jwt(token -> token.subject(second.getId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"" + fid + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revoked").value(true));
        assertThat(jdbcTemplate.queryForObject(
                "select active from push_devices where target_hash = ?", Boolean.class, targetHash)).isFalse();
    }

    @Test
    void customerRegistrationSessionRotationAddressAndShopFlowWorksEndToEnd() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.USER_AGENT, "shoppew-integration-test")
                        .content("""
                                {
                                  "email": "khach.hang@example.vn",
                                  "password": "MuaSam2026!",
                                  "displayName": "Khách Hàng Shoppew",
                                  "phone": "+84901234567",
                                  "deviceName": "Trình duyệt kiểm thử"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("khach.hang@example.vn"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("CUSTOMER"))
                .andReturn();

        JsonNode registrationBody = objectMapper.readTree(registration.getResponse().getContentAsString());
        String accessToken = registrationBody.path("data").path("accessToken").asString();
        String firstRefreshToken = cookieValue(registration, "shoppew_refresh");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("Khách Hàng Shoppew"));

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "Nhà",
                                  "recipientName": "Khách Hàng Shoppew",
                                  "phone": "+84901234567",
                                  "countryCode": "VN",
                                  "province": "TP. Hồ Chí Minh",
                                  "district": "Quận 1",
                                  "ward": "Phường Bến Nghé",
                                  "addressLine": "12 Nguyễn Huệ",
                                  "postalCode": "700000",
                                  "defaultAddress": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.defaultAddress").value(true));

        MvcResult shopCreation = mockMvc.perform(post("/api/v1/seller/shops")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Góc Nhà Việt",
                                  "description": "Đồ dùng gia đình chọn lọc tại Việt Nam."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("goc-nha-viet"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        String shopId = objectMapper.readTree(shopCreation.getResponse().getContentAsString())
                .path("data").path("id").asString();

        mockMvc.perform(get("/api/v1/seller/shops")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(shopId))
                .andExpect(jsonPath("$.data[0].name").value("Góc Nhà Việt"));

        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/addresses", shopId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "PICKUP",
                                  "contactName": "Kho Goc Nha Viet",
                                  "phone": "+84901234567",
                                  "countryCode": "VN",
                                  "province": "TP. Ho Chi Minh",
                                  "district": "Quan 1",
                                  "ward": "Ben Nghe",
                                  "addressLine": "25 Le Loi",
                                  "postalCode": "700000",
                                  "defaultAddress": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("PICKUP"))
                .andExpect(jsonPath("$.data.defaultAddress").value(true));

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/settings", shopId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("VND"))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"));

        mockMvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].current").value(true));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("shoppew_refresh", firstRefreshToken))
                        .header(HttpHeaders.ORIGIN, "https://attacker.invalid")
                        .header("Sec-Fetch-Site", "cross-site"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("UNTRUSTED_REQUEST_ORIGIN"));

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("shoppew_refresh", firstRefreshToken))
                        .header(HttpHeaders.USER_AGENT, "shoppew-integration-test-rotated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        String rotatedRefreshToken = cookieValue(refresh, "shoppew_refresh");
        String rotatedAccessToken = objectMapper.readTree(refresh.getResponse().getContentAsString())
                .path("data").path("accessToken").asString();
        org.assertj.core.api.Assertions.assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("shoppew_refresh", firstRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_REUSED"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rotatedAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailVerificationTokenIsSingleUseAndActivatesPendingAccount() throws Exception {
        String email = "xac-minh@example.vn";
        String password = "XacMinh2026!";
        Instant now = Instant.now(clock);
        UserEntity user = userRepository.saveAndFlush(UserEntity.register(
                email,
                null,
                passwordEncoder.encode(password),
                UserStatus.PENDING_VERIFICATION,
                now));
        profileRepository.saveAndFlush(UserProfileEntity.create(user, "Nguoi Can Xac Minh", now));

        mockMvc.perform(post("/api/v1/auth/verify-email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(authenticationEmailGateway)
                .sendEmailVerification(org.mockito.ArgumentMatchers.eq(email), tokenCaptor.capture());
        String verificationToken = tokenCaptor.getValue();

        mockMvc.perform(post("/api/v1/auth/verify-email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + verificationToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "xac-minh@example.vn",
                                  "password": "XacMinh2026!",
                                  "deviceName": "Verification test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.emailVerified").value(true));

        mockMvc.perform(post("/api/v1/auth/verify-email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + verificationToken + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_ACTION_TOKEN"));
    }

    @Test
    void passwordResetRevokesSessionsAndOldPassword() throws Exception {
        String email = "reset-password@example.vn";
        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-password@example.vn",
                                  "password": "MatKhauCu2026!",
                                  "displayName": "Nguoi Dat Lai Mat Khau",
                                  "deviceName": "Reset test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String oldAccessToken = objectMapper.readTree(registration.getResponse().getContentAsString())
                .path("data").path("accessToken").asString();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(authenticationEmailGateway)
                .sendPasswordReset(org.mockito.ArgumentMatchers.eq(email), tokenCaptor.capture());
        String resetToken = tokenCaptor.getValue();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"MatKhauMoi2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordReset").value(true));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-password@example.vn",
                                  "password": "MatKhauCu2026!",
                                  "deviceName": "Old password"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset-password@example.vn",
                                  "password": "MatKhauMoi2026!",
                                  "deviceName": "New password"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"KhongDuocDung2026!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_OR_EXPIRED_ACTION_TOKEN"));
    }

    @Test
    void catalogProductMediaAndModerationFlowWorksEndToEnd() throws Exception {
        org.mockito.Mockito.when(storageService.upload(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String objectKey = invocation.getArgument(0);
                    return new StorageService.StoredObject(objectKey, "http://media.test/shoppew-media/" + objectKey);
                });

        MvcResult rootCategoryResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Thoi trang",
                                  "slug": "thoi-trang-test",
                                  "description": "Danh muc goc cho kiem thu",
                                  "sortOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        String rootCategoryId = objectMapper.readTree(rootCategoryResult.getResponse().getContentAsString())
                .path("data").path("id").asString();

        MvcResult childCategoryResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ao thun",
                                  "slug": "ao-thun-test",
                                  "parentId": "%s",
                                  "description": "Danh muc con",
                                  "sortOrder": 1
                                }
                                """.formatted(rootCategoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        String categoryId = objectMapper.readTree(childCategoryResult.getResponse().getContentAsString())
                .path("data").path("id").asString();

        mockMvc.perform(put("/api/v1/admin/categories/{categoryId}", rootCategoryId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Thoi trang",
                                  "slug": "thoi-trang-test",
                                  "parentId": "%s",
                                  "description": "Khong duoc tao vong",
                                  "sortOrder": 1
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_HIERARCHY_CYCLE"));

        MvcResult brandResult = mockMvc.perform(post("/api/v1/admin/brands")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "May Moc Studio",
                                  "slug": "may-moc-studio-test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String brandId = objectMapper.readTree(brandResult.getResponse().getContentAsString())
                .path("data").path("id").asString();

        MvcResult attributeResult = mockMvc.perform(post("/api/v1/admin/products/attributes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "name": "Chat lieu",
                                  "valueType": "TEXT",
                                  "required": true,
                                  "sortOrder": 1
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        String attributeId = objectMapper.readTree(attributeResult.getResponse().getContentAsString())
                .path("data").path("id").asString();

        MvcResult sellerRegistration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "catalog.seller@example.vn",
                                  "password": "Catalog2026!",
                                  "displayName": "Catalog Seller",
                                  "deviceName": "Catalog integration"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String sellerAccessToken = objectMapper.readTree(sellerRegistration.getResponse().getContentAsString())
                .path("data").path("accessToken").asString();
        String sellerAuthorization = "Bearer " + sellerAccessToken;

        MvcResult shopResult = mockMvc.perform(post("/api/v1/seller/shops")
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "May Moc Market",
                                  "slug": "may-moc-market-test",
                                  "description": "Cua hang cho catalog integration"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String shopId = objectMapper.readTree(shopResult.getResponse().getContentAsString())
                .path("data").path("id").asString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/admin/shops/{shopId}/status", shopId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get(
                                "/api/v1/seller/shops/{shopId}/products/attribute-definitions", shopId)
                        .param("categoryId", categoryId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(attributeId))
                .andExpect(jsonPath("$.data[0].categoryId").value(categoryId))
                .andExpect(jsonPath("$.data[0].name").value("Chat lieu"))
                .andExpect(jsonPath("$.data[0].valueType").value("TEXT"))
                .andExpect(jsonPath("$.data[0].required").value(true))
                .andExpect(jsonPath("$.data[0].sortOrder").value(1));

        mockMvc.perform(get(
                                "/api/v1/seller/shops/{shopId}/products/attribute-definitions", shopId)
                        .param("categoryId", categoryId)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SHOP_ACCESS_DENIED"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                                "$.paths['/api/v1/seller/shops/{shopId}/products/attribute-definitions'].get")
                        .exists());

        MvcResult productResult = mockMvc.perform(post("/api/v1/seller/shops/{shopId}/products", shopId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": "%s",
                                  "brandId": "%s",
                                  "name": "Ao thun shoppew Everyday",
                                  "slug": "ao-thun-shoppew-everyday-test",
                                  "shortDescription": "Cotton mem, form de mac",
                                  "description": "Ao thun everyday duoc thiet ke cho nhip song thanh thi."
                                }
                                """.formatted(categoryId, brandId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        String productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data").path("id").asString();

        MvcResult colorOption = mockMvc.perform(post(
                                "/api/v1/seller/shops/{shopId}/products/{productId}/options", shopId, productId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mau",
                                  "sortOrder": 1,
                                  "values": [
                                    {"value": "Den", "sortOrder": 1},
                                    {"value": "Trang", "sortOrder": 2}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode colorBody = objectMapper.readTree(colorOption.getResponse().getContentAsString());
        String blackValueId = colorBody.path("data").path("values").path(0).path("id").asString();

        MvcResult sizeOption = mockMvc.perform(post(
                                "/api/v1/seller/shops/{shopId}/products/{productId}/options", shopId, productId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kich co",
                                  "sortOrder": 2,
                                  "values": [
                                    {"value": "S", "sortOrder": 1},
                                    {"value": "M", "sortOrder": 2}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String smallValueId = objectMapper.readTree(sizeOption.getResponse().getContentAsString())
                .path("data").path("values").path(0).path("id").asString();

        String variantJson = """
                {
                  "sku": "EVERYDAY-BLACK-S",
                  "name": "Den / S",
                  "price": 199000,
                  "compareAtPrice": 249000,
                  "currency": "VND",
                  "weightGrams": 250,
                  "lengthMm": 300,
                  "widthMm": 220,
                  "heightMm": 30,
                  "optionValueIds": ["%s", "%s"]
                }
                """.formatted(blackValueId, smallValueId);
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/products/{productId}/variants", shopId, productId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.price").value(199000.00))
                .andExpect(jsonPath("$.data.selections.length()").value(2));

        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/products/{productId}/variants", shopId, productId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson.replace("EVERYDAY-BLACK-S", "EVERYDAY-DUPLICATE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VARIANT_COMBINATION_ALREADY_EXISTS"));

        mockMvc.perform(put("/api/v1/seller/shops/{shopId}/products/{productId}/attributes", shopId, productId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"values":[{"attributeId":"%s","value":"100%% cotton"}]}
                                """.formatted(attributeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].value").value("100% cotton"));

        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        MockMultipartFile image = new MockMultipartFile("file", "everyday.png", "image/png", png);
        mockMvc.perform(multipart("/api/v1/seller/shops/{shopId}/products/{productId}/images", shopId, productId)
                        .file(image)
                        .param("altText", "Ao thun shoppew Everyday mau den")
                        .param("primary", "true")
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primary").value(true))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.containsString("shoppew-media/products/")));

        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/products/{productId}/submit", shopId, productId)
                        .header(HttpHeaders.AUTHORIZATION, sellerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        mockMvc.perform(get("/api/v1/public/products/ao-thun-shoppew-everyday-test"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/admin/products/{productId}/approve", productId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/public/products")
                        .param("q", "Everyday")
                        .param("shopId", shopId.toString())
                        .param("categoryId", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].minimumPrice").value(199000.00))
                .andExpect(jsonPath("$.data.content[0].primaryImageUrl").isNotEmpty());

        mockMvc.perform(get("/api/v1/public/products")
                        .param("q", "Everyday")
                        .param("shopId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/public/products")
                        .param("shopId", shopId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/v1/public/products/ao-thun-shoppew-everyday-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options.length()").value(2))
                .andExpect(jsonPath("$.data.variants.length()").value(1))
                .andExpect(jsonPath("$.data.attributes[0].value").value("100% cotton"));

        mockMvc.perform(get("/api/v1/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].children[0].slug").value("ao-thun-test"));
    }

    @Test
    void atomicInventoryReservationAllowsOnlyOneWinnerForStockOfOne() throws Exception {
        CommerceFixture fixture = createCommerceFixture(1, 1);
        UUID variantId = fixture.variantIds().getFirst();
        int attempts = 100;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(32);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int attempt = 0; attempt < attempts; attempt++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        inventoryReservationService.reserve(fixture.userId(), variantId, 1, null);
                        return true;
                    } catch (ApiException exception) {
                        if ("INSUFFICIENT_STOCK".equals(exception.code())) return false;
                        throw exception;
                    }
                }));
            }
            start.countDown();
            long winners = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) winners++;
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        var inventory = jdbcTemplate.queryForMap(
                "select available_quantity, reserved_quantity, sold_quantity from inventories where variant_id = ?",
                variantId);
        assertThat(((Number) inventory.get("available_quantity")).longValue()).isZero();
        assertThat(((Number) inventory.get("reserved_quantity")).longValue()).isEqualTo(1);
        assertThat(((Number) inventory.get("sold_quantity")).longValue()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from inventory_reservations where variant_id = ? and status = 'ACTIVE'",
                Long.class,
                variantId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from inventory_transactions where variant_id = ? and transaction_type = 'RESERVE'",
                Long.class,
                variantId)).isEqualTo(1);

        jdbcTemplate.update("""
                update inventory_reservations
                set created_at = now() - interval '2 seconds', expires_at = now() - interval '1 second'
                where variant_id = ? and status = 'ACTIVE'
                """, variantId);
        inventoryReservationService.releaseExpired();
        var releasedInventory = jdbcTemplate.queryForMap(
                "select available_quantity, reserved_quantity from inventories where variant_id = ?", variantId);
        assertThat(((Number) releasedInventory.get("available_quantity")).longValue()).isEqualTo(1);
        assertThat(((Number) releasedInventory.get("reserved_quantity")).longValue()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from inventory_reservations where variant_id = ? and status = 'EXPIRED'",
                Long.class,
                variantId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from inventory_transactions where variant_id = ? and transaction_type = 'RELEASE'",
                Long.class,
                variantId)).isEqualTo(1);
    }

    @Test
    void multiSellerCartRevalidatesDatabasePriceStockAndEligibility() throws Exception {
        CommerceFixture fixture = createCommerceFixture(2, 10);
        UUID firstVariantId = fixture.variantIds().get(0);
        UUID secondVariantId = fixture.variantIds().get(1);

        var customerJwt = jwt().jwt(token -> token
                .subject(fixture.userId().toString())
                .claim("sid", UUID.randomUUID().toString()));
        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/inventory", fixture.shopIds().getFirst())
                        .with(customerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].variantId").value(firstVariantId.toString()));

        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/inventory/{variantId}/adjustments",
                                fixture.shopIds().getFirst(), firstVariantId)
                        .with(customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "INCREASE",
                                  "quantity": 5,
                                  "lowStockThreshold": 3,
                                  "note": "Integration restock"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity").value(15));

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + firstVariantId + "\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shops.length()").value(1));

        MvcResult twoShopCart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + secondVariantId + "\",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shops.length()").value(2))
                .andExpect(jsonPath("$.data.itemCount").value(3))
                .andExpect(jsonPath("$.data.selectedSubtotal").value(400000.00))
                .andReturn();
        assertThat(findCartItem(
                objectMapper.readTree(twoShopCart.getResponse().getContentAsString()), firstVariantId)
                .path("unitPrice").decimalValue()).isEqualByComparingTo("100000.00");

        jdbcTemplate.update("update product_variants set price = 125000.00 where id = ?", firstVariantId);
        jdbcTemplate.update("update inventories set available_quantity = 1, version = version + 1 where variant_id = ?",
                firstVariantId);

        MvcResult revalidated = mockMvc.perform(get("/api/v1/cart")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shops.length()").value(2))
                .andExpect(jsonPath("$.data.selectedSubtotal").value(200000.00))
                .andReturn();
        JsonNode staleItem = findCartItem(
                objectMapper.readTree(revalidated.getResponse().getContentAsString()), firstVariantId);
        assertThat(staleItem.path("unitPrice").decimalValue()).isEqualByComparingTo("125000.00");
        assertThat(staleItem.path("eligible").asBoolean()).isFalse();
        assertThat(staleItem.path("issues").toString()).contains("INSUFFICIENT_STOCK");

        mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + firstVariantId + "\",\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void codCheckoutSplitsShopsPreservesSnapshotsAndUsesExplicitOrderTransitions() throws Exception {
        CommerceFixture fixture = createCommerceFixture(2, 10);
        MvcResult firstCart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().get(0) + "\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstItemId = UUID.fromString(findCartItem(
                objectMapper.readTree(firstCart.getResponse().getContentAsString()), fixture.variantIds().get(0))
                .path("id").asString());
        MvcResult secondCart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().get(1) + "\",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID secondItemId = UUID.fromString(findCartItem(
                objectMapper.readTree(secondCart.getResponse().getContentAsString()), fixture.variantIds().get(1))
                .path("id").asString());
        String checkoutRequest = """
                {
                  "cartItemIds": ["%s", "%s"],
                  "addressId": "%s",
                  "paymentProvider": "COD",
                  "shippingMethodCode": "MOCK_STANDARD",
                  "customerNote": "Please call before delivery"
                }
                """.formatted(firstItemId, secondItemId, fixture.addressId());

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shops.length()").value(2))
                .andExpect(jsonPath("$.data.itemsSubtotal").value(400000.00))
                .andExpect(jsonPath("$.data.shippingTotal").value(44000.00))
                .andExpect(jsonPath("$.data.grandTotal").value(444000.00));

        String idempotencyKey = "checkout-cod-" + UUID.randomUUID();
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.orders.length()").value(2))
                .andExpect(jsonPath("$.data.orders[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.payment.provider").value("COD"))
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"))
                .andReturn();
        JsonNode checkoutBody = objectMapper.readTree(checkout.getResponse().getContentAsString());
        String checkoutId = checkoutBody.path("data").path("id").asString();
        UUID firstOrderId = UUID.fromString(checkoutBody.path("data").path("orders").path(0).path("id").asString());

        mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(checkoutId));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from checkout_groups where idempotency_key = ?", Long.class, idempotencyKey))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from orders where checkout_group_id = ?::uuid", Long.class, checkoutId))
                .isEqualTo(2);

        mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest.replace("COD", "MOCK_ONLINE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED"));

        assertInventory(fixture.variantIds().get(0), 8, 0, 2);
        assertInventory(fixture.variantIds().get(1), 9, 0, 1);
        mockMvc.perform(get("/api/v1/cart")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(0));

        jdbcTemplate.update("update products set name = 'MUTATED PRODUCT' where id = ?", fixture.productIds().get(0));
        jdbcTemplate.update("""
                update product_variants
                set name = 'MUTATED VARIANT', sku = ?, price = 999999.00
                where id = ?
                """, "MUTATED-" + UUID.randomUUID(), fixture.variantIds().get(0));
        mockMvc.perform(get("/api/v1/orders/{orderId}", firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productName").value("Fixture Product 0"))
                .andExpect(jsonPath("$.data.items[0].variantName").value("Fixture Variant 0"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(100000.00))
                .andExpect(jsonPath("$.data.address.addressLine").value("12 Nguyen Hue"));

        mockMvc.perform(get("/api/v1/orders/{orderId}", firstOrderId)
                        .with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));

        UUID firstShopId = UUID.fromString(checkoutBody.path("data").path("orders").path(0).path("shopId").asString());
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/process", firstShopId, firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PROCESSING"));
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/ready-to-ship", firstShopId, firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("READY_TO_SHIP"));
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/ship", firstShopId, firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"MOCK-TRACK-1\",\"location\":\"Ho Chi Minh City\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"))
                .andExpect(jsonPath("$.data.shipment.trackingNumber").value("MOCK-TRACK-1"));
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/deliver", firstShopId, firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.shipment.status").value("DELIVERED"));
        mockMvc.perform(post("/api/v1/orders/{orderId}/complete", firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.history.length()").value(6));
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/process", firstShopId, firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ORDER_INVALID_STATE"));
    }

    @Test
    void mockPaymentWebhookIsSignedIdempotentAndConsumesInventoryOnce() throws Exception {
        CommerceFixture fixture = createCommerceFixture(1, 5);
        MvcResult cart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().getFirst() + "\",\"quantity\":2}"))
                .andExpect(status().isCreated()).andReturn();
        UUID itemId = UUID.fromString(findCartItem(
                objectMapper.readTree(cart.getResponse().getContentAsString()), fixture.variantIds().getFirst())
                .path("id").asString());
        String checkoutRequest = """
                {
                  "cartItemIds": ["%s"],
                  "addressId": "%s",
                  "paymentProvider": "MOCK_ONLINE",
                  "shippingMethodCode": "MOCK_STANDARD"
                }
                """.formatted(itemId, fixture.addressId());
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", "mock-payment-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PAYMENT_PENDING"))
                .andExpect(jsonPath("$.data.orders[0].status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"))
                .andReturn();
        JsonNode body = objectMapper.readTree(checkout.getResponse().getContentAsString());
        String providerReference = body.path("data").path("payment").path("providerReference").asString();
        UUID orderId = UUID.fromString(body.path("data").path("orders").path(0).path("id").asString());
        UUID shopId = UUID.fromString(body.path("data").path("orders").path(0).path("shopId").asString());
        assertInventory(fixture.variantIds().getFirst(), 3, 2, 0);

        String webhook = """
                {"providerEventId":"mock-event-1","providerReference":"%s","succeeded":true}
                """.formatted(providerReference);
        mockMvc.perform(post("/api/v1/payments/mock/webhook")
                        .header("X-Shoppew-Mock-Signature", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_SIGNATURE"));
        mockMvc.perform(post("/api/v1/payments/mock/webhook")
                        .header("X-Shoppew-Mock-Signature", "shoppew-mock-webhook-development-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        assertInventory(fixture.variantIds().getFirst(), 3, 0, 2);
        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(post("/api/v1/payments/mock/webhook")
                        .header("X-Shoppew-Mock-Signature", "shoppew-mock-webhook-development-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(webhook))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payment_events where provider_event_id = 'mock-event-1'", Long.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from inventory_transactions
                where variant_id = ? and transaction_type = 'SALE'
                """, Long.class, fixture.variantIds().getFirst())).isEqualTo(1);
        assertInventory(fixture.variantIds().getFirst(), 3, 0, 2);

        mockMvc.perform(post("/api/v1/payments/mock/webhook")
                        .header("X-Shoppew-Mock-Signature", "shoppew-mock-webhook-development-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhook.replace("true", "false")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_EVENT_PAYLOAD_MISMATCH"));
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/confirm", shopId, orderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void promotionAndScopedVouchersAreServerPricedAndConsumedOnce() throws Exception {
        CommerceFixture fixture = createCommerceFixture(2, 10);
        var customerJwt = jwt().jwt(token -> token.subject(fixture.userId().toString())
                .claim("sid", UUID.randomUUID().toString()));
        Instant startsAt = Instant.now(clock).minusSeconds(60);
        Instant endsAt = Instant.now(clock).plusSeconds(3600);

        MvcResult promotion = mockMvc.perform(post("/api/v1/seller/shops/{shopId}/promotions", fixture.shopIds().get(0))
                        .with(customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fixture flash price",
                                  "promotionType": "FLASH_SALE",
                                  "discountType": "PERCENTAGE",
                                  "discountValue": 20,
                                  "maxDiscount": 30000,
                                  "startsAt": "%s",
                                  "endsAt": "%s",
                                  "targets": [{
                                    "productId": "%s",
                                    "variantId": "%s",
                                    "promotionalPrice": 80000,
                                    "quantityLimit": 2
                                  }]
                                }
                                """.formatted(startsAt, endsAt, fixture.productIds().get(0), fixture.variantIds().get(0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        String promotionId = objectMapper.readTree(promotion.getResponse().getContentAsString()).path("data").path("id").asString();
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/promotions/{promotionId}/activate",
                                fixture.shopIds().get(0), promotionId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        String promotedSlug = jdbcTemplate.queryForObject(
                "select slug from products where id = ?", String.class, fixture.productIds().get(0));
        mockMvc.perform(get("/api/v1/public/products/{slug}", promotedSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].price").value(80000.00))
                .andExpect(jsonPath("$.data.variants[0].originalPrice").value(100000.00))
                .andExpect(jsonPath("$.data.variants[0].promotionId").value(promotionId));

        String shopCode = "SHOP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        MvcResult shopVoucher = mockMvc.perform(post("/api/v1/seller/shops/{shopId}/vouchers", fixture.shopIds().get(0))
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Fixture product voucher",
                                  "voucherType": "PRODUCT",
                                  "discountType": "FIXED",
                                  "discountValue": 10000,
                                  "minimumSpend": 100000,
                                  "currency": "VND",
                                  "startsAt": "%s",
                                  "endsAt": "%s",
                                  "totalQuantity": 10,
                                  "perUserLimit": 1,
                                  "productIds": ["%s"],
                                  "paymentProviders": ["COD"]
                                }
                                """.formatted(shopCode, startsAt, endsAt, fixture.productIds().get(0))))
                .andExpect(status().isCreated()).andReturn();
        String shopVoucherId = objectMapper.readTree(shopVoucher.getResponse().getContentAsString()).path("data").path("id").asString();
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/vouchers/{voucherId}/activate",
                                fixture.shopIds().get(0), shopVoucherId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        String platformCode = "SHIP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        MvcResult platformVoucher = mockMvc.perform(post("/api/v1/admin/vouchers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Fixture platform shipping",
                                  "voucherType": "SHIPPING",
                                  "discountType": "FIXED",
                                  "discountValue": 5000,
                                  "minimumSpend": 0,
                                  "currency": "VND",
                                  "startsAt": "%s",
                                  "endsAt": "%s",
                                  "totalQuantity": 10,
                                  "perUserLimit": 2
                                }
                                """.formatted(platformCode, startsAt, endsAt)))
                .andExpect(status().isCreated()).andReturn();
        String platformVoucherId = objectMapper.readTree(platformVoucher.getResponse().getContentAsString()).path("data").path("id").asString();
        mockMvc.perform(post("/api/v1/admin/vouchers/{voucherId}/activate", platformVoucherId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        MvcResult firstCart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().get(0) + "\",\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shops[0].items[0].unitPrice").value(80000.00))
                .andExpect(jsonPath("$.data.shops[0].items[0].originalUnitPrice").value(100000.00))
                .andExpect(jsonPath("$.data.shops[0].items[0].promotionId").value(promotionId))
                .andReturn();
        UUID firstItemId = UUID.fromString(findCartItem(objectMapper.readTree(firstCart.getResponse().getContentAsString()),
                fixture.variantIds().get(0)).path("id").asString());
        MvcResult secondCart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().get(1) + "\",\"quantity\":1}"))
                .andExpect(status().isCreated()).andReturn();
        UUID secondItemId = UUID.fromString(findCartItem(objectMapper.readTree(secondCart.getResponse().getContentAsString()),
                fixture.variantIds().get(1)).path("id").asString());

        String requestTemplate = """
                {
                  "cartItemIds": ["%s", "%s"],
                  "addressId": "%s",
                  "paymentProvider": "%s",
                  "shippingMethodCode": "MOCK_STANDARD",
                  "voucherCodes": ["%s", "%s"]
                }
                """;
        mockMvc.perform(post("/api/v1/checkout/preview")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestTemplate.formatted(firstItemId, secondItemId, fixture.addressId(),
                                "MOCK_ONLINE", shopCode, platformCode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOUCHER_PAYMENT_RESTRICTED"));

        String checkoutRequest = requestTemplate.formatted(firstItemId, secondItemId, fixture.addressId(),
                "COD", shopCode, platformCode);
        mockMvc.perform(post("/api/v1/checkout/preview")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemsSubtotal").value(360000.00))
                .andExpect(jsonPath("$.data.shippingTotal").value(44000.00))
                .andExpect(jsonPath("$.data.discountTotal").value(15000.00))
                .andExpect(jsonPath("$.data.grandTotal").value(389000.00))
                .andExpect(jsonPath("$.data.appliedVouchers.length()").value(2));

        String key = "promotion-voucher-" + UUID.randomUUID();
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.grandTotal").value(389000.00))
                .andExpect(jsonPath("$.data.appliedVouchers.length()").value(2))
                .andReturn();
        JsonNode checkoutBody = objectMapper.readTree(checkout.getResponse().getContentAsString());
        UUID checkoutId = UUID.fromString(checkoutBody.path("data").path("id").asString());
        UUID firstOrderId = UUID.fromString(checkoutBody.path("data").path("orders").path(0).path("id").asString());

        mockMvc.perform(get("/api/v1/orders/{orderId}", firstOrderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(80000.00))
                .andExpect(jsonPath("$.data.shopDiscountTotal").value(10000.00))
                .andExpect(jsonPath("$.data.platformDiscountTotal").value(2500.00));
        assertThat(jdbcTemplate.queryForObject("select sold_quantity from promotion_products where promotion_id = ?::uuid",
                Long.class, promotionId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from promotion_usages where checkout_group_id = ? and status = 'CONSUMED'",
                Long.class, checkoutId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from voucher_usages where checkout_group_id = ? and status = 'CONSUMED'",
                Long.class, checkoutId)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select sum(used_quantity) from vouchers where id in (?::uuid, ?::uuid)",
                Long.class, shopVoucherId, platformVoucherId)).isEqualTo(2);

        mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(checkoutId.toString()));
        assertThat(jdbcTemplate.queryForObject("select sold_quantity from promotion_products where promotion_id = ?::uuid",
                Long.class, promotionId)).isEqualTo(2);
    }

    @Test
    void concurrentCheckoutsEnforceVoucherPerUserLimit() throws Exception {
        CommerceFixture fixture = createCommerceFixture(2, 5);
        Instant startsAt = Instant.now(clock).minusSeconds(60);
        Instant endsAt = Instant.now(clock).plusSeconds(3600);
        String code = "RACE" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        UUID voucherId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into vouchers (id, owner_type, code, name, voucher_type, discount_type,
                    discount_value, minimum_spend, currency, starts_at, ends_at, total_quantity,
                    used_quantity, per_user_limit, status)
                values (?, 'PLATFORM', ?, 'Concurrent fixture voucher', 'PLATFORM', 'FIXED',
                    1000, 0, 'VND', ?, ?, 10, 0, 1, 'ACTIVE')
                """, voucherId, code, java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt));

        List<UUID> itemIds = new ArrayList<>();
        for (UUID variantId : fixture.variantIds()) {
            MvcResult cart = mockMvc.perform(post("/api/v1/cart/items")
                            .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"variantId\":\"" + variantId + "\",\"quantity\":1}"))
                    .andExpect(status().isCreated()).andReturn();
            itemIds.add(UUID.fromString(findCartItem(objectMapper.readTree(cart.getResponse().getContentAsString()),
                    variantId).path("id").asString()));
        }

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Integer>> futures = new ArrayList<>();
        try {
            for (UUID itemId : itemIds) {
                futures.add(executor.submit(() -> {
                    start.await();
                    String request = """
                            {"cartItemIds":["%s"],"addressId":"%s","paymentProvider":"COD",
                             "shippingMethodCode":"MOCK_STANDARD","voucherCodes":["%s"]}
                            """.formatted(itemId, fixture.addressId(), code);
                    return mockMvc.perform(post("/api/v1/checkout")
                                    .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                            .claim("sid", UUID.randomUUID().toString())))
                                    .header("Idempotency-Key", "voucher-race-" + itemId)
                                    .contentType(MediaType.APPLICATION_JSON).content(request))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) statuses.add(future.get(30, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jdbcTemplate.queryForObject("select used_quantity from vouchers where id = ?", Long.class, voucherId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(distinct checkout_group_id) from voucher_usages where voucher_id = ? and status = 'CONSUMED'",
                Long.class, voucherId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select sum(sold_quantity) from inventories where variant_id in (?, ?)",
                Long.class, fixture.variantIds().get(0), fixture.variantIds().get(1))).isEqualTo(1);
    }

    @Test
    void failedOnlinePaymentReleasesPromotionVoucherAndInventoryReservations() throws Exception {
        CommerceFixture fixture = createCommerceFixture(1, 5);
        Instant startsAt = Instant.now(clock).minusSeconds(60);
        Instant endsAt = Instant.now(clock).plusSeconds(3600);
        UUID promotionId = UUID.randomUUID();
        UUID promotionProductId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into promotions (id, owner_type, name, promotion_type, discount_type, discount_value,
                    starts_at, ends_at, status)
                values (?, 'PLATFORM', 'Release fixture promotion', 'FLASH_SALE', 'PERCENTAGE', 20,
                    ?, ?, 'ACTIVE')
                """, promotionId, java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt));
        jdbcTemplate.update("""
                insert into promotion_products (id, promotion_id, product_id, variant_id,
                    promotional_price, quantity_limit, sold_quantity)
                values (?, ?, ?, ?, 80000, 2, 0)
                """, promotionProductId, promotionId, fixture.productIds().getFirst(), fixture.variantIds().getFirst());

        UUID voucherId = UUID.randomUUID();
        String code = "FAIL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        jdbcTemplate.update("""
                insert into vouchers (id, owner_type, code, name, voucher_type, discount_type,
                    discount_value, minimum_spend, currency, starts_at, ends_at, total_quantity,
                    used_quantity, per_user_limit, status)
                values (?, 'PLATFORM', ?, 'Release fixture voucher', 'PLATFORM', 'FIXED',
                    5000, 0, 'VND', ?, ?, 1, 0, 1, 'ACTIVE')
                """, voucherId, code, java.sql.Timestamp.from(startsAt), java.sql.Timestamp.from(endsAt));

        MvcResult cart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().getFirst() + "\",\"quantity\":2}"))
                .andExpect(status().isCreated()).andReturn();
        UUID itemId = UUID.fromString(findCartItem(objectMapper.readTree(cart.getResponse().getContentAsString()),
                fixture.variantIds().getFirst()).path("id").asString());
        String request = """
                {"cartItemIds":["%s"],"addressId":"%s","paymentProvider":"MOCK_ONLINE",
                 "shippingMethodCode":"MOCK_STANDARD","voucherCodes":["%s"]}
                """.formatted(itemId, fixture.addressId(), code);
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", "failure-release-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itemsSubtotal").value(160000.00))
                .andExpect(jsonPath("$.data.discountTotal").value(5000.00))
                .andReturn();
        JsonNode body = objectMapper.readTree(checkout.getResponse().getContentAsString());
        UUID checkoutId = UUID.fromString(body.path("data").path("id").asString());
        String providerReference = body.path("data").path("payment").path("providerReference").asString();
        assertInventory(fixture.variantIds().getFirst(), 3, 2, 0);
        assertThat(jdbcTemplate.queryForObject("select sold_quantity from promotion_products where id = ?",
                Long.class, promotionProductId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select used_quantity from vouchers where id = ?",
                Long.class, voucherId)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/payments/mock/webhook")
                        .header("X-Shoppew-Mock-Signature", "shoppew-mock-webhook-development-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"providerEventId":"release-failure-%s","providerReference":"%s","succeeded":false}
                                """.formatted(UUID.randomUUID(), providerReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
        assertInventory(fixture.variantIds().getFirst(), 5, 0, 0);
        assertThat(jdbcTemplate.queryForObject("select sold_quantity from promotion_products where id = ?",
                Long.class, promotionProductId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select used_quantity from vouchers where id = ?",
                Long.class, voucherId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from promotion_usages where checkout_group_id = ? and status = 'RELEASED'",
                Long.class, checkoutId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from voucher_usages where checkout_group_id = ? and status = 'RELEASED'",
                Long.class, checkoutId)).isEqualTo(1);

        MvcResult replacementCart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().getFirst() + "\",\"quantity\":1}"))
                .andExpect(status().isCreated()).andReturn();
        UUID replacementItemId = UUID.fromString(findCartItem(
                objectMapper.readTree(replacementCart.getResponse().getContentAsString()), fixture.variantIds().getFirst())
                .path("id").asString());
        String retryRequest = request.replace(itemId.toString(), replacementItemId.toString()).replace("MOCK_ONLINE", "COD");
        mockMvc.perform(post("/api/v1/checkout/preview")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content(retryRequest))
                .andExpect(status().isOk());

        UUID expiredVoucherId = UUID.randomUUID();
        String expiredCode = "OLD" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        jdbcTemplate.update("""
                insert into vouchers (id, owner_type, code, name, voucher_type, discount_type,
                    discount_value, minimum_spend, currency, starts_at, ends_at, total_quantity,
                    used_quantity, per_user_limit, status)
                values (?, 'PLATFORM', ?, 'Expired fixture voucher', 'PLATFORM', 'FIXED',
                    1000, 0, 'VND', now() - interval '2 hours', now() - interval '1 hour', 10, 0, 1, 'ACTIVE')
                """, expiredVoucherId, expiredCode);
        mockMvc.perform(post("/api/v1/checkout/preview")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(retryRequest.replace(code, expiredCode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOUCHER_UNAVAILABLE"));
    }

    @Test
    void wishlistReviewAndOrderNotificationsEnforceOwnershipAndPurchaseEligibility() throws Exception {
        CommerceFixture fixture = createCommerceFixture(1, 5);
        UUID productId = fixture.productIds().getFirst();
        UUID shopId = fixture.shopIds().getFirst();
        var customerJwt = jwt().jwt(token -> token.subject(fixture.userId().toString())
                .claim("sid", UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/wishlist/products/{productId}", productId).with(customerJwt))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.product.id").value(productId.toString()));
        mockMvc.perform(post("/api/v1/wishlist/products/{productId}", productId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isCreated());
        assertThat(jdbcTemplate.queryForObject("select count(*) from wishlists where user_id = ? and product_id = ?",
                Long.class, fixture.userId(), productId)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/wishlist")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(delete("/api/v1/wishlist/products/{productId}", productId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isNoContent());

        MvcResult cart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().getFirst() + "\",\"quantity\":1}"))
                .andExpect(status().isCreated()).andReturn();
        UUID itemId = UUID.fromString(findCartItem(objectMapper.readTree(cart.getResponse().getContentAsString()),
                fixture.variantIds().getFirst()).path("id").asString());
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", "review-flow-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemIds":["%s"],"addressId":"%s","paymentProvider":"COD",
                                 "shippingMethodCode":"MOCK_STANDARD"}
                                """.formatted(itemId, fixture.addressId())))
                .andExpect(status().isCreated()).andReturn();
        JsonNode checkoutBody = objectMapper.readTree(checkout.getResponse().getContentAsString());
        UUID orderId = UUID.fromString(checkoutBody.path("data").path("orders").path(0).path("id").asString());
        UUID orderItemId = jdbcTemplate.queryForObject(
                "select id from order_items where order_id = ?", UUID.class, orderId);

        mockMvc.perform(post("/api/v1/reviews").with(customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":\"" + orderItemId + "\",\"rating\":4,\"content\":\"Too early\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REVIEW_PURCHASE_NOT_ELIGIBLE"));

        String[] actions = {"process", "ready-to-ship", "ship", "deliver"};
        for (String action : actions) {
            mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/{action}", shopId, orderId, action)
                            .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/orders/{orderId}/complete", orderId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk());

        MvcResult notifications = mockMvc.perform(get("/api/v1/notifications")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(6))
                .andReturn();
        UUID notificationId = UUID.fromString(objectMapper.readTree(notifications.getResponse().getContentAsString())
                .path("data").path("content").path(0).path("id").asString());
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.count").value(6));
        mockMvc.perform(post("/api/v1/notifications/{notificationId}/read", notificationId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.read").value(true));
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.count").value(0));
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from notification_deliveries d
                join notifications n on n.id = d.notification_id
                where d.status = 'DELIVERED' and n.user_id = ?
                """, Long.class, fixture.userId())).isEqualTo(6);

        MvcResult reviewResult = mockMvc.perform(post("/api/v1/reviews")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":\"" + orderItemId + "\",\"rating\":4,\"content\":\"Verified purchase\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andReturn();
        UUID reviewId = UUID.fromString(objectMapper.readTree(reviewResult.getResponse().getContentAsString())
                .path("data").path("id").asString());
        mockMvc.perform(post("/api/v1/reviews")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderItemId\":\"" + orderItemId + "\",\"rating\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REVIEW_ALREADY_EXISTS"));
        assertThat(jdbcTemplate.queryForObject("select rating_average from products where id = ?", BigDecimal.class, productId))
                .isEqualByComparingTo("4.00");
        assertThat(jdbcTemplate.queryForObject("select rating_average from shops where id = ?", BigDecimal.class, shopId))
                .isEqualByComparingTo("4.00");

        mockMvc.perform(put("/api/v1/reviews/{reviewId}", reviewId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"content\":\"Updated verified purchase\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rating").value(5));
        mockMvc.perform(put("/api/v1/seller/shops/{shopId}/reviews/{reviewId}/reply", shopId, reviewId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reply\":\"Thank you\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sellerReply").value("Thank you"));
        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/reviews", shopId)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(reviewId.toString()))
                .andExpect(jsonPath("$.data.content[0].sellerReply").value("Thank you"));
        mockMvc.perform(get("/api/v1/public/products/{productId}/reviews", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));

        org.mockito.Mockito.when(storageService.upload(
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq("image/png")))
                .thenAnswer(invocation -> new StorageService.StoredObject(
                        invocation.getArgument(0), "https://cdn.example.test/" + invocation.getArgument(0)));
        MockMultipartFile reviewImage = new MockMultipartFile("file", "review.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
        mockMvc.perform(multipart("/api/v1/reviews/{reviewId}/images", reviewId)
                        .file(reviewImage)
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.images.length()").value(1));

        mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/hide", reviewId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("HIDDEN"));
        mockMvc.perform(get("/api/v1/public/products/{productId}/reviews", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        assertThat(jdbcTemplate.queryForObject("select review_count from products where id = ?", Long.class, productId)).isZero();
        mockMvc.perform(post("/api/v1/admin/reviews/{reviewId}/publish", reviewId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("select review_count from products where id = ?", Long.class, productId)).isEqualTo(1);
    }

    @Test
    void refundDisputeFinanceAuditAndAnalyticsUsePersistedAuthorizedData() throws Exception {
        CommerceFixture fixture = createCommerceFixture(1, 5);
        UUID sellerId = fixture.userId();
        UUID buyerId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID buyerAddressId = UUID.randomUUID();
        for (UUID userId : List.of(buyerId, outsiderId)) {
            jdbcTemplate.update("""
                    insert into app_users (id, email, password_hash, status, email_verified, password_changed_at)
                    values (?, ?, 'not-used-by-integration-fixture', 'ACTIVE', true, now())
                    """, userId, "phase9-" + userId + "@example.test");
            jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'CUSTOMER')", userId);
        }
        jdbcTemplate.update("""
                insert into user_addresses (id, user_id, label, recipient_name, phone, country_code,
                    province, district, ward, address_line, postal_code, is_default)
                values (?, ?, 'Home', 'Phase 9 Buyer', '+84901111111', 'VN', 'TP. Ho Chi Minh',
                    'Quan 1', 'Ben Nghe', '9 Marketplace Street', '700000', true)
                """, buyerAddressId, buyerId);

        var buyerJwt = jwt().jwt(token -> token.subject(buyerId.toString()).claim("sid", UUID.randomUUID().toString()));
        MvcResult cart = mockMvc.perform(post("/api/v1/cart/items").with(buyerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().getFirst() + "\",\"quantity\":2}"))
                .andExpect(status().isCreated()).andReturn();
        UUID cartItemId = UUID.fromString(findCartItem(objectMapper.readTree(cart.getResponse().getContentAsString()),
                fixture.variantIds().getFirst()).path("id").asString());
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(buyerId.toString()).claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", "phase9-checkout-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cartItemIds":["%s"],"addressId":"%s","paymentProvider":"COD",
                                 "shippingMethodCode":"MOCK_STANDARD"}
                                """.formatted(cartItemId, buyerAddressId)))
                .andExpect(status().isCreated()).andReturn();
        UUID orderId = UUID.fromString(objectMapper.readTree(checkout.getResponse().getContentAsString())
                .path("data").path("orders").path(0).path("id").asString());
        UUID orderItemId = jdbcTemplate.queryForObject("select id from order_items where order_id = ?", UUID.class, orderId);

        for (String action : List.of("process", "ready-to-ship", "ship", "deliver")) {
            mockMvc.perform(post("/api/v1/seller/shops/{shopId}/orders/{orderId}/{action}",
                            fixture.shopIds().getFirst(), orderId, action)
                            .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString())))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/orders/{orderId}/complete", orderId)
                        .with(jwt().jwt(token -> token.subject(buyerId.toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/finance/balance", fixture.shopIds().getFirst())
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingAmount").value(0))
                .andExpect(jsonPath("$.data.availableAmount").value(190000));
        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/finance/transactions", fixture.shopIds().getFirst())
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(3));

        MvcResult refundResult = mockMvc.perform(post("/api/v1/refunds")
                        .with(jwt().jwt(token -> token.subject(buyerId.toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","reason":"NOT_AS_DESCRIBED","description":"Persisted claim",
                                 "items":[{"orderItemId":"%s","quantity":1}]}
                                """.formatted(orderId, orderItemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.requestedAmount").value(100000))
                .andReturn();
        UUID refundRequestId = UUID.fromString(objectMapper.readTree(refundResult.getResponse().getContentAsString())
                .path("data").path("id").asString());
        mockMvc.perform(get("/api/v1/refunds/{requestId}", refundRequestId)
                        .with(jwt().jwt(token -> token.subject(outsiderId.toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/refunds/{requestId}/review",
                        fixture.shopIds().getFirst(), refundRequestId)
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Seller evidence attached\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));

        MvcResult disputeResult = mockMvc.perform(post("/api/v1/disputes")
                        .with(jwt().jwt(token -> token.subject(buyerId.toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","refundRequestId":"%s","reason":"SELLER_RESPONSE",
                                 "description":"Admin review requested"}
                                """.formatted(orderId, refundRequestId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("OPEN")).andReturn();
        UUID disputeId = UUID.fromString(objectMapper.readTree(disputeResult.getResponse().getContentAsString())
                .path("data").path("id").asString());
        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/disputes/{disputeId}/messages",
                        fixture.shopIds().getFirst(), disputeId)
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Seller persisted response\",\"attachments\":[]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.messages.length()").value(1));

        var adminJwt = jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockMvc.perform(post("/api/v1/admin/refunds/{requestId}/approve", refundRequestId)
                        .with(adminJwt).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvedAmount\":100000,\"note\":\"Evidence accepted\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        String refundKey = "phase9-refund-" + UUID.randomUUID();
        mockMvc.perform(post("/api/v1/admin/refunds/{requestId}/process", refundRequestId)
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("Idempotency-Key", refundKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.status").value("SUCCEEDED"));
        mockMvc.perform(post("/api/v1/admin/refunds/{requestId}/process", refundRequestId)
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .header("Idempotency-Key", refundKey))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refund.status").value("SUCCEEDED"));

        mockMvc.perform(put("/api/v1/admin/disputes/{disputeId}", disputeId)
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"resolution\":\"Refund completed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/finance/balance", fixture.shopIds().getFirst())
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availableAmount").value(90000));
        assertThat(jdbcTemplate.queryForObject("select count(*) from refunds where refund_request_id = ?", Long.class, refundRequestId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from seller_transactions where order_id = ?", Long.class, orderId)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId)).isEqualTo("PARTIALLY_REFUNDED");
        assertThat(jdbcTemplate.queryForObject("select status from payments where checkout_group_id = (select checkout_group_id from orders where id = ?)", String.class, orderId)).isEqualTo("PARTIALLY_REFUNDED");

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/analytics", fixture.shopIds().getFirst())
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").value(90000))
                .andExpect(jsonPath("$.data.completedOrders").value(1))
                .andExpect(jsonPath("$.data.topProducts[0].quantity").value(2));
        mockMvc.perform(get("/api/v1/admin/analytics")
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedOrders").isNumber())
                .andExpect(jsonPath("$.data.refundVolume").value(100000));
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .with(jwt().jwt(token -> token.subject(sellerId.toString()).claim("sid", UUID.randomUUID().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").isNumber());
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_logs where actor_id = ?", Long.class, sellerId)).isEqualTo(3);
    }

    @Test
    void adminOperationsProtectUsersAndInspectMarketplaceDataEndToEnd() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Instant now = Instant.now(clock);

        UserEntity admin = UserEntity.register(
                "admin-" + suffix + "@example.test", null,
                passwordEncoder.encode("AdminApi2026!"), UserStatus.ACTIVE, now);
        admin.verifyEmail(now);
        admin.addRole(UserRole.ADMIN, now);
        admin = userRepository.save(admin);
        profileRepository.save(UserProfileEntity.create(admin, "Admin API", now));
        UUID adminId = admin.getId();

        UserEntity protectedAdmin = UserEntity.register(
                "protected-admin-" + suffix + "@example.test", null,
                passwordEncoder.encode("AdminApi2026!"), UserStatus.ACTIVE, now);
        protectedAdmin.verifyEmail(now);
        protectedAdmin.addRole(UserRole.ADMIN, now);
        protectedAdmin = userRepository.save(protectedAdmin);
        profileRepository.save(UserProfileEntity.create(protectedAdmin, "Protected Admin", now));
        UUID protectedAdminId = protectedAdmin.getId();

        MvcResult registration = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.USER_AGENT, "shoppew-admin-api-test")
                        .content("""
                                {
                                  "email":"suspend-%s@example.test",
                                  "password":"Suspend2026!",
                                  "displayName":"Suspend Target",
                                  "deviceName":"Admin API target"
                                }
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode registrationBody = objectMapper.readTree(registration.getResponse().getContentAsString());
        UUID targetUserId = UUID.fromString(registrationBody.path("data").path("user").path("id").asString());
        String targetAccessToken = registrationBody.path("data").path("accessToken").asString();

        CommerceFixture fixture = createCommerceFixture(1, 5);
        UserEntity seller = userRepository.findById(fixture.userId()).orElseThrow();
        seller.addRole(UserRole.SELLER, now);
        userRepository.save(seller);
        profileRepository.save(UserProfileEntity.create(seller, "Admin API Seller", now));

        MvcResult cart = mockMvc.perform(post("/api/v1/cart/items")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":\"" + fixture.variantIds().getFirst() + "\",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID cartItemId = UUID.fromString(findCartItem(
                objectMapper.readTree(cart.getResponse().getContentAsString()), fixture.variantIds().getFirst())
                .path("id").asString());
        String checkoutRequest = """
                {
                  "cartItemIds":["%s"],
                  "addressId":"%s",
                  "paymentProvider":"COD",
                  "shippingMethodCode":"MOCK_STANDARD"
                }
                """.formatted(cartItemId, fixture.addressId());
        MvcResult checkout = mockMvc.perform(post("/api/v1/checkout")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString())
                                .claim("sid", UUID.randomUUID().toString())))
                        .header("Idempotency-Key", "admin-inspection-" + suffix)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode checkoutBody = objectMapper.readTree(checkout.getResponse().getContentAsString());
        UUID orderId = UUID.fromString(checkoutBody.path("data").path("orders").path(0).path("id").asString());
        UUID paymentId = UUID.fromString(checkoutBody.path("data").path("payment").path("id").asString());
        String orderNumber = checkoutBody.path("data").path("orders").path(0).path("orderNumber").asString();

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/users")
                        .with(jwt().jwt(token -> token.subject(fixture.userId().toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_SELLER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("query", "Suspend Target")
                        .param("status", "ACTIVE")
                        .param("role", "CUSTOMER")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(targetUserId.toString()))
                .andExpect(jsonPath("$.data.content[0].displayName").value("Suspend Target"));
        mockMvc.perform(get("/api/v1/admin/users/{userId}", targetUserId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSessionCount").value(1))
                .andExpect(jsonPath("$.data.email").value("suspend-" + suffix + "@example.test"));

        mockMvc.perform(get("/api/v1/admin/sellers")
                        .param("query", "Admin API Seller")
                        .param("status", "ACTIVE")
                        .param("shopStatus", "ACTIVE")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].userId").value(fixture.userId().toString()))
                .andExpect(jsonPath("$.data.content[0].shopCount").value(1));
        mockMvc.perform(get("/api/v1/admin/sellers/{userId}", fixture.userId())
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seller.id").value(fixture.userId().toString()))
                .andExpect(jsonPath("$.data.shops.length()").value(1));

        mockMvc.perform(get("/api/v1/admin/shops")
                        .param("query", "Fixture Shop 0")
                        .param("status", "ACTIVE")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(fixture.shopIds().getFirst().toString()));
        mockMvc.perform(get("/api/v1/admin/shops/{shopId}", fixture.shopIds().getFirst())
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(fixture.userId().toString()));

        mockMvc.perform(get("/api/v1/admin/orders")
                        .param("query", orderNumber)
                        .param("status", "CONFIRMED")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.content[0].customerEmail").isNotEmpty());
        mockMvc.perform(get("/api/v1/admin/orders/{orderId}", orderId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(fixture.userId().toString()))
                .andExpect(jsonPath("$.data.order.items[0].productName").value("Fixture Product 0"))
                .andExpect(jsonPath("$.data.payment.id").value(paymentId.toString()));

        mockMvc.perform(get("/api/v1/admin/payments")
                        .param("query", checkoutBody.path("data").path("checkoutNumber").asString())
                        .param("status", "PENDING")
                        .param("provider", "COD")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(paymentId.toString()));
        mockMvc.perform(get("/api/v1/admin/payments/{paymentId}", paymentId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("COD"))
                .andExpect(jsonPath("$.data.userId").value(fixture.userId().toString()));
        mockMvc.perform(get("/api/v1/admin/settings")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.availablePaymentProviders").isArray())
                .andExpect(jsonPath("$.data.availableShippingProviders[0]").value("MOCK_STANDARD"));

        Instant campaignStart = Instant.now(clock).minusSeconds(60);
        Instant campaignEnd = Instant.now(clock).plusSeconds(86_400);
        MvcResult platformVoucher = mockMvc.perform(post("/api/v1/admin/vouchers")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"ADM%s",
                                  "name":"Admin API voucher",
                                  "voucherType":"PLATFORM",
                                  "discountType":"FIXED",
                                  "discountValue":10000,
                                  "minimumSpend":100000,
                                  "currency":"VND",
                                  "startsAt":"%s",
                                  "endsAt":"%s",
                                  "totalQuantity":10,
                                  "perUserLimit":1
                                }
                                """.formatted(suffix.replace("-", ""), campaignStart, campaignEnd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownerType").value("PLATFORM"))
                .andReturn();
        UUID voucherId = UUID.fromString(objectMapper.readTree(platformVoucher.getResponse().getContentAsString())
                .path("data").path("id").asString());
        MvcResult platformPromotion = mockMvc.perform(post("/api/v1/admin/promotions")
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Admin API campaign",
                                  "promotionType":"PLATFORM_CAMPAIGN",
                                  "discountType":"PERCENTAGE",
                                  "discountValue":10,
                                  "maxDiscount":50000,
                                  "startsAt":"%s",
                                  "endsAt":"%s",
                                  "targets":[{
                                    "productId":"%s",
                                    "variantId":"%s",
                                    "promotionalPrice":90000,
                                    "quantityLimit":10
                                  }]
                                }
                                """.formatted(campaignStart, campaignEnd,
                                fixture.productIds().getFirst(), fixture.variantIds().getFirst())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownerType").value("PLATFORM"))
                .andReturn();
        UUID promotionId = UUID.fromString(objectMapper.readTree(platformPromotion.getResponse().getContentAsString())
                .path("data").path("id").asString());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where actor_id = ? and action = 'PLATFORM_VOUCHER_CREATED' and resource_id = ?",
                Long.class, adminId, voucherId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where actor_id = ? and action = 'PLATFORM_PROMOTION_CREATED' and resource_id = ?",
                Long.class, adminId, promotionId)).isEqualTo(1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/admin/users/{userId}/status", protectedAdminId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\",\"reason\":\"Unauthorized peer action\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_HIERARCHY_VIOLATION"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/admin/users/{userId}/status", targetUserId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\",\"reason\":\"Risk review required\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.activeSessionCount").value(0));
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + targetAccessToken))
                .andExpect(status().isUnauthorized());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where actor_id = ? and action = 'USER_SUSPENDED' and resource_id = ?",
                Long.class, adminId, targetUserId)).isEqualTo(1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                                "/api/v1/admin/users/{userId}/status", targetUserId)
                        .with(jwt().jwt(token -> token.subject(adminId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"reason\":\"Risk review completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where actor_id = ? and action = 'USER_RESTORED' and resource_id = ?",
                Long.class, adminId, targetUserId)).isEqualTo(1);

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{userId}/status'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/sellers'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/shops'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/orders'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/payments'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/settings'].get").exists());
    }

    private void assertInventory(UUID variantId, long available, long reserved, long sold) {
        var inventory = jdbcTemplate.queryForMap("""
                select available_quantity, reserved_quantity, sold_quantity
                from inventories where variant_id = ?
                """, variantId);
        assertThat(((Number) inventory.get("available_quantity")).longValue()).isEqualTo(available);
        assertThat(((Number) inventory.get("reserved_quantity")).longValue()).isEqualTo(reserved);
        assertThat(((Number) inventory.get("sold_quantity")).longValue()).isEqualTo(sold);
    }

    private CommerceFixture createCommerceFixture(int shopCount, long stockPerVariant) {
        UUID userId = UUID.randomUUID();
        String suffix = userId.toString();
        jdbcTemplate.update("""
                insert into app_users (id, email, password_hash, status, email_verified, password_changed_at)
                values (?, ?, 'not-used-by-integration-fixture', 'ACTIVE', true, now())
                """, userId, "commerce-" + suffix + "@example.test");
        jdbcTemplate.update("insert into user_roles (user_id, role) values (?, 'CUSTOMER')", userId);
        UUID addressId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into user_addresses (
                    id, user_id, label, recipient_name, phone, country_code, province, district,
                    ward, address_line, postal_code, is_default)
                values (?, ?, 'Home', 'Commerce Customer', '+84901234567', 'VN',
                        'TP. Ho Chi Minh', 'Quan 1', 'Ben Nghe', '12 Nguyen Hue', '700000', true)
                """, addressId, userId);
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into categories (id, name, slug, status)
                values (?, ?, ?, 'ACTIVE')
                """, categoryId, "Commerce fixture " + suffix, "commerce-fixture-" + suffix);

        List<UUID> shopIds = new ArrayList<>();
        List<UUID> productIds = new ArrayList<>();
        List<UUID> variantIds = new ArrayList<>();
        for (int index = 0; index < shopCount; index++) {
            UUID shopId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID variantId = UUID.randomUUID();
            shopIds.add(shopId);
            productIds.add(productId);
            variantIds.add(variantId);
            jdbcTemplate.update("""
                    insert into shops (id, owner_id, name, slug, status)
                    values (?, ?, ?, ?, 'ACTIVE')
                    """, shopId, userId, "Fixture Shop " + index, "fixture-shop-" + index + "-" + suffix);
            jdbcTemplate.update("""
                    insert into shop_members (shop_id, user_id, member_role, status)
                    values (?, ?, 'OWNER', 'ACTIVE')
                    """, shopId, userId);
            jdbcTemplate.update("""
                    insert into products (id, shop_id, category_id, name, slug, description, status, published_at)
                    values (?, ?, ?, ?, ?, 'Commerce integration fixture', 'ACTIVE', now())
                    """, productId, shopId, categoryId, "Fixture Product " + index,
                    "fixture-product-" + index + "-" + suffix);
            BigDecimal price = index == 0 ? new BigDecimal("100000.00") : new BigDecimal("200000.00");
            jdbcTemplate.update("""
                    insert into product_variants (id, product_id, shop_id, sku, name, price, currency, status)
                    values (?, ?, ?, ?, ?, ?, 'VND', 'ACTIVE')
                    """, variantId, productId, shopId, "FIXTURE-" + index + "-" + suffix,
                    "Fixture Variant " + index, price);
            jdbcTemplate.update("""
                    insert into inventories (variant_id, available_quantity, reserved_quantity, sold_quantity,
                                             low_stock_threshold, version)
                    values (?, ?, 0, 0, 2, 0)
                    """, variantId, stockPerVariant);
        }
        return new CommerceFixture(
                userId, addressId, List.copyOf(shopIds), List.copyOf(productIds), List.copyOf(variantIds));
    }

    private JsonNode findCartItem(JsonNode response, UUID variantId) {
        for (JsonNode shop : response.path("data").path("shops")) {
            for (JsonNode item : shop.path("items")) {
                if (variantId.toString().equals(item.path("variantId").asString())) return item;
            }
        }
        throw new AssertionError("Cart item not found for variant " + variantId);
    }

    private record CommerceFixture(
            UUID userId,
            UUID addressId,
            List<UUID> shopIds,
            List<UUID> productIds,
            List<UUID> variantIds) {}

    private String cookieValue(MvcResult result, String name) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(header).isNotBlank();
        String prefix = name + "=";
        org.assertj.core.api.Assertions.assertThat(header).startsWith(prefix);
        return header.substring(prefix.length(), header.indexOf(';'));
    }
}
