package com.shoppew.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";
    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/verify-email/request",
            "/api/v1/auth/verify-email/confirm",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/payments/mock/webhook");

    @Bean
    OpenAPI shoppewOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("shoppew API")
                        .version("v1")
                        .description("API contract for the shoppew storefront, Seller Center, Admin, and Android clients.")
                        .contact(new Contact().name("shoppew engineering")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    OpenApiCustomizer publicOperationSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
                boolean publicCatalog = path.equals("/api/v1/public") || path.startsWith("/api/v1/public/");
                boolean publicPost = method == io.swagger.v3.oas.models.PathItem.HttpMethod.POST
                        && PUBLIC_POST_PATHS.contains(path);
                if (publicCatalog || publicPost) {
                    // An explicit empty requirement overrides the global bearer rule.
                    operation.setSecurity(List.of());
                }
            }));
        };
    }
}
