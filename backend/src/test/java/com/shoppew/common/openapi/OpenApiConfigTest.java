package com.shoppew.common.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void documentsBearerAuthenticationAndClearsItOnlyForPublicOperations() {
        Operation login = new Operation();
        Operation publicCatalog = new Operation();
        Operation currentUser = new Operation();
        OpenAPI openApi = config.shoppewOpenApi().paths(new Paths()
                .addPathItem("/api/v1/auth/login", new PathItem().post(login))
                .addPathItem("/api/v1/public/products", new PathItem().get(publicCatalog))
                .addPathItem("/api/v1/auth/me", new PathItem().get(currentUser)));

        config.publicOperationSecurityCustomizer().customise(openApi);

        assertThat(openApi.getSecurity()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
        assertThat(login.getSecurity()).isEmpty();
        assertThat(publicCatalog.getSecurity()).isEmpty();
        assertThat(currentUser.getSecurity()).isNull();
    }
}
