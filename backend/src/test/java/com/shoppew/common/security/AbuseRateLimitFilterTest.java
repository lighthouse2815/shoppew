package com.shoppew.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoppew.common.config.AppProperties;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class AbuseRateLimitFilterTest {

    @Test
    void rejectsLimitedLoginWithStableJsonAndRetryHeader() throws Exception {
        RedisAbuseRateLimiter limiter = mock(RedisAbuseRateLimiter.class);
        when(limiter.allow(eq("login"), eq("203.0.113.10"), eq(20), any())).thenReturn(false);
        AppProperties properties = mock(AppProperties.class);
        when(properties.rateLimit()).thenReturn(new AppProperties.RateLimit(true, 20, 10, 5, 180, 60));
        AbuseRateLimitFilter filter = new AbuseRateLimitFilter(
                limiter, properties, new ObjectMapper(), Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentAsString()).contains("RATE_LIMITED");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void leavesUnclassifiedRequestsOutsideTheLimiter() throws Exception {
        RedisAbuseRateLimiter limiter = mock(RedisAbuseRateLimiter.class);
        AppProperties properties = mock(AppProperties.class);
        when(properties.rateLimit()).thenReturn(new AppProperties.RateLimit(true, 20, 10, 5, 180, 60));
        AbuseRateLimitFilter filter = new AbuseRateLimitFilter(
                limiter, properties, new ObjectMapper(), Clock.systemUTC());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/system");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
        verify(limiter, never()).allow(any(), any(), any(Integer.class), any());
    }
}
