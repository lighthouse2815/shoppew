package com.shoppew.common.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityConfigurationValidatorTest {

    @Test
    void rejectsDevelopmentDefaultsInProductionWithoutEchoingTheirValues() {
        AppProperties properties = properties(
                false,
                false,
                false,
                true,
                true,
                "shoppew-development-signing-secret-change-before-production-2026",
                "http://localhost:3000",
                "http://localhost:9000",
                List.of("http://localhost:3000"),
                "shoppew_minio",
                "shoppew_minio_dev_password");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.password", "shoppew_dev_password");

        assertThatThrownBy(() -> new ProductionSecurityConfigurationValidator(properties, environment)
                        .afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET")
                .hasMessageContaining("APP_SECURE_COOKIES")
                .hasMessageContaining("APP_EMAIL_DELIVERY_ENABLED")
                .hasMessageContaining("APP_CORS_ALLOWED_ORIGINS")
                .hasMessageContaining("APP_MOCK_PAYMENT_ENABLED")
                .hasMessageContaining("APP_MOCK_SHIPPING_ENABLED")
                .hasMessageNotContaining("shoppew-development-signing-secret");
    }

    @Test
    void acceptsExplicitHttpsProductionConfiguration() {
        AppProperties properties = properties(
                true,
                true,
                true,
                false,
                false,
                "a-production-signing-key-with-more-than-sixty-four-utf8-bytes-0123456789",
                "https://shoppew.example",
                "https://media.shoppew.example",
                List.of(
                        "https://shoppew.example",
                        "https://seller.shoppew.example",
                        "https://admin.shoppew.example"),
                "production-media-access",
                "production-media-secret-value");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.password", "production-database-password");

        assertThatCode(() -> new ProductionSecurityConfigurationValidator(properties, environment)
                        .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsCorsEntriesThatAreUrlsInsteadOfCanonicalOrigins() {
        AppProperties properties = properties(
                true,
                true,
                true,
                false,
                false,
                "a-production-signing-key-with-more-than-sixty-four-utf8-bytes-0123456789",
                "https://shoppew.example",
                "https://media.shoppew.example",
                List.of("https://shoppew.example/"),
                "production-media-access",
                "production-media-secret-value");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.password", "production-database-password");

        assertThatThrownBy(() -> new ProductionSecurityConfigurationValidator(properties, environment)
                        .afterPropertiesSet())
                .hasMessageContaining("APP_CORS_ALLOWED_ORIGINS");
    }

    private AppProperties properties(
            boolean secureCookies,
            boolean verificationRequired,
            boolean emailDeliveryEnabled,
            boolean mockPaymentEnabled,
            boolean mockShippingEnabled,
            String jwtSecret,
            String webBaseUrl,
            String publicStorageEndpoint,
            List<String> origins,
            String storageAccessKey,
            String storageSecretKey) {
        return new AppProperties(
                "v1",
                "vi-VN",
                "VND",
                "Asia/Ho_Chi_Minh",
                origins,
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                Duration.ofMinutes(15),
                jwtSecret,
                secureCookies,
                verificationRequired,
                Duration.ofHours(24),
                Duration.ofMinutes(30),
                new AppProperties.Email(emailDeliveryEnabled, "no-reply@shoppew.example", webBaseUrl),
                new AppProperties.Storage(
                        "https://storage.internal.example",
                        publicStorageEndpoint,
                        storageAccessKey,
                        storageSecretKey,
                        "shoppew-media"),
                new AppProperties.Payment(mockPaymentEnabled, "unused-when-mock-is-disabled"),
                new AppProperties.Shipping(mockShippingEnabled),
                new AppProperties.RateLimit(true, 20, 10, 5, 180, 60));
    }
}
