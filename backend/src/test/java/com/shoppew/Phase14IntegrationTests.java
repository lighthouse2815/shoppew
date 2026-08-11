package com.shoppew;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shoppew.auth.email.AuthenticationEmailGateway;
import com.shoppew.chat.realtime.ChatRealtimePublisher;
import com.shoppew.media.StorageService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class Phase14IntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @MockitoBean private AuthenticationEmailGateway authenticationEmailGateway;
    @MockitoBean private StorageService storageService;
    @MockitoBean private ChatRealtimePublisher realtimePublisher;

    private final UUID buyerId = UUID.randomUUID();
    private final UUID otherBuyerId = UUID.randomUUID();
    private final UUID sellerId = UUID.randomUUID();
    private final UUID outsiderSellerId = UUID.randomUUID();
    private final UUID shopId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID brandId = UUID.randomUUID();
    private final UUID productAlphaId = UUID.randomUUID();
    private final UUID productBetaId = UUID.randomUUID();

    @BeforeEach
    void seed() {
        assertThat(messagingTemplate).isNotNull();
        user(buyerId, "phase14-buyer", "CUSTOMER");
        user(otherBuyerId, "phase14-other", "CUSTOMER");
        user(sellerId, "phase14-seller", "SELLER");
        user(outsiderSellerId, "phase14-outsider", "SELLER");
        jdbc.update("""
                insert into shops (id, owner_id, name, slug, description, status)
                values (?, ?, ?, ?, ?, 'ACTIVE')
                """, shopId, sellerId, "Phase 14 Market", "phase14-market-" + shopId, "Search and chat shop");
        jdbc.update("""
                insert into shop_members (id, shop_id, user_id, member_role, status)
                values (?, ?, ?, 'OWNER', 'ACTIVE')
                """, UUID.randomUUID(), shopId, sellerId);
        jdbc.update("""
                insert into shop_settings (shop_id, currency_code, time_zone, chat_enabled)
                values (?, 'VND', 'Asia/Ho_Chi_Minh', true)
                """, shopId);
        jdbc.update("""
                insert into categories (id, name, slug, status)
                values (?, 'Phase 14 Category', ?, 'ACTIVE')
                """, categoryId, "phase14-category-" + categoryId);
        jdbc.update("""
                insert into brands (id, name, slug, status)
                values (?, ?, ?, 'ACTIVE')
                """, brandId, "Phase 14 Brand " + brandId, "phase14-brand-" + brandId);
        product(productAlphaId, "Phase14 Alpha Linen", "phase14-alpha-" + productAlphaId, 4.80, 9, 120_000);
        product(productBetaId, "Phase14 Beta Cotton", "phase14-beta-" + productBetaId, 3.50, 30, 310_000);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("delete from conversations where shop_id = ?", shopId);
        jdbc.update("delete from products where shop_id = ?", shopId);
        jdbc.update("delete from shops where id = ?", shopId);
        jdbc.update("delete from categories where id = ?", categoryId);
        jdbc.update("delete from brands where id = ?", brandId);
        jdbc.update(
                "delete from app_users where id in (?, ?, ?, ?)",
                buyerId, otherBuyerId, sellerId, outsiderSellerId);
    }

    @Test
    void postgresSearchAndRecommendationContractsWork() throws Exception {
        mockMvc.perform(get("/api/v1/public/products")
                        .param("q", "Alpha Linen")
                        .param("categoryId", categoryId.toString())
                        .param("brandId", brandId.toString())
                        .param("minPrice", "100000")
                        .param("maxPrice", "150000")
                        .param("minRating", "4")
                        .param("sort", "PRICE_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(productAlphaId.toString()))
                .andExpect(jsonPath("$.data.content[0].minimumPrice").value(120000));

        mockMvc.perform(get("/api/v1/public/products")
                        .param("minPrice", "200000")
                        .param("maxPrice", "100000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PRICE_RANGE"));

        mockMvc.perform(get("/api/v1/public/search/suggestions").param("q", "Phase14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/public/recommendations/popular").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(productBetaId.toString()));

        mockMvc.perform(get("/api/v1/public/recommendations/trending").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(productBetaId.toString()));

        mockMvc.perform(get("/api/v1/public/recommendations/products/{id}/related", productAlphaId)
                        .param("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(productBetaId.toString()));

        mockMvc.perform(post("/api/v1/recommendations/recently-viewed/{id}", productAlphaId)
                        .with(auth(buyerId, "CUSTOMER")))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/recommendations/recently-viewed")
                        .with(auth(buyerId, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(productAlphaId.toString()));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/public/search/suggestions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/public/recommendations/popular']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recommendations/recently-viewed/{productId}']").exists());
    }

    @Test
    void persistedChatEnforcesCustomerAndSellerParticipation() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/v1/chat/conversations")
                        .with(auth(buyerId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shopId":"%s","productId":"%s"}
                                """.formatted(shopId, productAlphaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shopId").value(shopId.toString()))
                .andReturn();
        JsonNode document = objectMapper.readTree(started.getResponse().getContentAsByteArray());
        UUID conversationId = UUID.fromString(document.path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/chat/conversations/{id}/messages", conversationId)
                        .with(auth(buyerId, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"TEXT","textContent":"Shop cho mình hỏi thời gian giao hàng?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mine").value(true));

        mockMvc.perform(get("/api/v1/chat/conversations/{id}/messages", conversationId)
                        .with(auth(otherBuyerId, "CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONVERSATION_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/chat/conversations", shopId)
                        .with(auth(outsiderSellerId, "SELLER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SHOP_ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/chat/conversations", shopId)
                        .with(auth(sellerId, "SELLER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(conversationId.toString()));

        mockMvc.perform(get("/api/v1/seller/shops/{shopId}/chat/conversations/{id}/messages", shopId, conversationId)
                        .with(auth(sellerId, "SELLER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(post("/api/v1/seller/shops/{shopId}/chat/conversations/{id}/messages", shopId, conversationId)
                        .with(auth(sellerId, "SELLER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"PRODUCT","productId":"%s"}
                                """.formatted(productBetaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value(productBetaId.toString()));

        assertThat(jdbc.queryForObject(
                "select count(*) from messages where conversation_id = ?", Long.class, conversationId)).isEqualTo(3L);
        assertThat(jdbc.queryForObject(
                "select count(*) from conversation_participants where conversation_id = ?", Long.class, conversationId)).isEqualTo(2L);
        assertThat(jdbc.queryForObject(
                "select count(*) from notifications where notification_type = 'CHAT' and data ->> 'conversationId' = ?",
                Long.class, conversationId.toString())).isGreaterThanOrEqualTo(2L);

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/chat/conversations']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/seller/shops/{shopId}/chat/conversations']").exists());
    }

    private void user(UUID id, String prefix, String role) {
        jdbc.update("""
                insert into app_users (id, email, password_hash, status, email_verified)
                values (?, ?, 'not-used', 'ACTIVE', true)
                """, id, prefix + "-" + id + "@example.test");
        jdbc.update("insert into user_roles (user_id, role) values (?, ?)", id, role);
        jdbc.update("""
                insert into user_profiles (user_id, display_name, locale)
                values (?, ?, 'vi-VN')
                """, id, prefix);
    }

    private void product(UUID id, String name, String slug, double rating, long sold, long price) {
        jdbc.update("""
                insert into products (
                    id, shop_id, category_id, brand_id, name, slug, short_description, description,
                    status, rating_average, review_count, sold_count, published_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, 4, ?, now())
                """, id, shopId, categoryId, brandId, name, slug, name + " short", name + " description", rating, sold);
        jdbc.update("""
                insert into product_variants (id, product_id, shop_id, sku, name, price, currency, status)
                values (?, ?, ?, ?, 'Mặc định', ?, 'VND', 'ACTIVE')
                """, UUID.randomUUID(), id, shopId, "P14-" + id, price);
    }

    private JwtRequestPostProcessor auth(UUID userId, String role) {
        return jwt().jwt(token -> token
                        .subject(userId.toString())
                        .claim("sid", UUID.randomUUID().toString())
                        .claim("roles", List.of(role)))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
