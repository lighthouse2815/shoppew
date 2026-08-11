package com.shoppew.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String apiVersion,
        String locale,
        String currency,
        String timeZone,
        List<String> corsAllowedOrigins,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration inventoryReservationTtl,
        String jwtSecret,
        boolean secureCookies,
        boolean emailVerificationRequired,
        Duration emailVerificationTtl,
        Duration passwordResetTtl,
        Email email,
        Storage storage,
        Payment payment,
        RateLimit rateLimit) {

    public record Email(
            boolean deliveryEnabled,
            String from,
            String webBaseUrl) {}

    public record Storage(
            String endpoint,
            String publicEndpoint,
            String accessKey,
            String secretKey,
            String bucket) {}

    public record Payment(
            boolean mockEnabled,
            String mockWebhookSecret) {}

    public record RateLimit(
            boolean enabled,
            int loginPerMinute,
            int registrationPerMinute,
            int accountRecoveryPerMinute,
            int searchPerMinute,
            int sensitiveMutationPerMinute) {}
}
