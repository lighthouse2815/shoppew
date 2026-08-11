package com.shoppew.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shoppew.common.config.AppProperties;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class CookieMutationOriginFilterTest {

    @Test
    void matrixParametersCannotBypassCookieMutationOriginCheck() throws Exception {
        AppProperties properties = mock(AppProperties.class);
        when(properties.corsAllowedOrigins()).thenReturn(List.of("https://shoppew.example"));
        CookieMutationOriginFilter filter = new CookieMutationOriginFilter(
                properties, new ObjectMapper(), Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/v1/auth/refresh;route=alternate");
        request.addHeader("Origin", "https://attacker.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("UNTRUSTED_REQUEST_ORIGIN");
        assertThat(chain.getRequest()).isNull();
    }
}
