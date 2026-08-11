package com.shoppew.common.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class ProductionSecurityConfigurationValidator implements InitializingBean {

    private static final String DEVELOPMENT_JWT_SECRET =
            "shoppew-development-signing-secret-change-before-production-2026";
    private static final String DEVELOPMENT_DATABASE_PASSWORD = "shoppew_dev_password";
    private static final String DEVELOPMENT_STORAGE_ACCESS_KEY = "shoppew_minio";
    private static final String DEVELOPMENT_STORAGE_SECRET_KEY = "shoppew_minio_dev_password";

    private final AppProperties properties;
    private final Environment environment;

    public ProductionSecurityConfigurationValidator(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> unsafe = new ArrayList<>();
        if (!properties.secureCookies()) unsafe.add("APP_SECURE_COOKIES");
        if (!properties.emailVerificationRequired()) unsafe.add("APP_EMAIL_VERIFICATION_REQUIRED");
        if (properties.payment() == null || properties.payment().mockEnabled()) {
            unsafe.add("APP_MOCK_PAYMENT_ENABLED");
        }
        AppProperties.RateLimit rateLimit = properties.rateLimit();
        if (rateLimit == null || !rateLimit.enabled()) {
            unsafe.add("APP_RATE_LIMIT_ENABLED");
        } else if (rateLimit.loginPerMinute() < 1
                || rateLimit.registrationPerMinute() < 1
                || rateLimit.accountRecoveryPerMinute() < 1
                || rateLimit.searchPerMinute() < 1
                || rateLimit.sensitiveMutationPerMinute() < 1) {
            unsafe.add("app.rate-limit");
        }

        requireSecret(
                unsafe,
                "APP_JWT_SECRET",
                properties.jwtSecret(),
                64,
                DEVELOPMENT_JWT_SECRET);
        requireSecret(
                unsafe,
                "DB_PASSWORD",
                environment.getProperty("spring.datasource.password"),
                16,
                DEVELOPMENT_DATABASE_PASSWORD);

        AppProperties.Storage storage = properties.storage();
        if (storage == null) {
            unsafe.add("app.storage");
        } else {
            requireSecret(unsafe, "S3_ACCESS_KEY", storage.accessKey(), 8, DEVELOPMENT_STORAGE_ACCESS_KEY);
            requireSecret(unsafe, "S3_SECRET_KEY", storage.secretKey(), 16, DEVELOPMENT_STORAGE_SECRET_KEY);
            requireHttpsUrl(unsafe, "S3_PUBLIC_ENDPOINT", storage.publicEndpoint(), false);
        }

        AppProperties.Email email = properties.email();
        requireHttpsUrl(unsafe, "APP_WEB_BASE_URL", email == null ? null : email.webBaseUrl(), false);

        List<String> allowedOrigins = properties.corsAllowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            unsafe.add("APP_CORS_ALLOWED_ORIGINS");
        } else {
            allowedOrigins.forEach(origin -> requireHttpsUrl(
                    unsafe, "APP_CORS_ALLOWED_ORIGINS", origin, true));
        }

        if (!unsafe.isEmpty()) {
            throw new IllegalStateException(
                    "Unsafe production security configuration; replace or enable: "
                            + String.join(", ", unsafe.stream().distinct().toList()));
        }
    }

    private void requireSecret(
            List<String> unsafe,
            String setting,
            String value,
            int minimumBytes,
            String developmentValue) {
        if (!StringUtils.hasText(value)
                || value.getBytes(StandardCharsets.UTF_8).length < minimumBytes
                || developmentValue.equals(value)) {
            unsafe.add(setting);
        }
    }

    private void requireHttpsUrl(
            List<String> unsafe,
            String setting,
            String value,
            boolean originOnly) {
        if (!StringUtils.hasText(value)) {
            unsafe.add(setting);
            return;
        }
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);
            boolean localHost = normalizedHost.equals("localhost")
                    || normalizedHost.endsWith(".localhost")
                    || normalizedHost.equals("127.0.0.1")
                    || normalizedHost.equals("0.0.0.0")
                    || normalizedHost.equals("::1");
            boolean invalidOriginPath = originOnly && StringUtils.hasText(uri.getPath());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || localHost
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || invalidOriginPath) {
                unsafe.add(setting);
            }
        } catch (IllegalArgumentException exception) {
            unsafe.add(setting);
        }
    }
}
