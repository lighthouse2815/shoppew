package com.shoppew.common.security;

import com.shoppew.common.api.ApiError;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AbuseRateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RedisAbuseRateLimiter limiter;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AbuseRateLimitFilter(
            RedisAbuseRateLimiter limiter,
            AppProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.limiter = limiter;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.rateLimit() == null
                || !properties.rateLimit().enabled()
                || rule(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Rule rule = rule(request);
        if (rule == null || limiter.allow(rule.namespace(), request.getRemoteAddr(), rule.limit(), WINDOW)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(WINDOW.toSeconds()));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.failure(
                        new ApiError(
                                "RATE_LIMITED",
                                "Bạn thao tác quá nhanh. Vui lòng thử lại sau.",
                                List.of()),
                        clock));
    }

    private Rule rule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = normalizedPath(request);
        AppProperties.RateLimit limits = properties.rateLimit();
        if (limits == null) return null;

        if ("POST".equals(method) && "/api/v1/auth/login".equals(path)) {
            return new Rule("login", limits.loginPerMinute());
        }
        if ("POST".equals(method) && "/api/v1/auth/register".equals(path)) {
            return new Rule("registration", limits.registrationPerMinute());
        }
        if ("POST".equals(method) && ("/api/v1/auth/forgot-password".equals(path)
                || "/api/v1/auth/verify-email/request".equals(path))) {
            return new Rule("account-recovery", limits.accountRecoveryPerMinute());
        }
        if ("GET".equals(method) && ("/api/v1/public/products".equals(path)
                || path.startsWith("/api/v1/public/search/"))) {
            return new Rule("public-search", limits.searchPerMinute());
        }
        if ("POST".equals(method) && (path.equals("/api/v1/checkout")
                || path.equals("/api/v1/checkout/preview")
                || path.equals("/api/v1/payments/mock/webhook")
                || path.endsWith("/images"))) {
            return new Rule("sensitive-mutation", limits.sensitiveMutationPerMinute());
        }
        return null;
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.replaceAll(";[^/]*", "");
    }

    private record Rule(String namespace, int limit) {}
}
