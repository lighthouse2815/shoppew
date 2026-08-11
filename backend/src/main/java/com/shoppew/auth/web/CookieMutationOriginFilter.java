package com.shoppew.auth.web;

import com.shoppew.common.api.ApiError;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CookieMutationOriginFilter extends OncePerRequestFilter {

    private static final Set<String> COOKIE_MUTATION_PATHS = Set.of(
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout");

    private final Set<String> allowedOrigins;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CookieMutationOriginFilter(AppProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.allowedOrigins = Set.copyOf(properties.corsAllowedOrigins());
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !COOKIE_MUTATION_PATHS.contains(normalizedPath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String fetchSite = request.getHeader("Sec-Fetch-Site");
        boolean crossSite = "cross-site".equalsIgnoreCase(fetchSite);
        boolean untrustedOrigin = origin != null && !allowedOrigins.contains(origin);
        if (crossSite || untrustedOrigin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(
                    response.getOutputStream(),
                    ApiResponse.failure(
                            new ApiError(
                                    "UNTRUSTED_REQUEST_ORIGIN",
                                    "Yêu cầu dùng cookie đến từ nguồn không được tin cậy",
                                    List.of()),
                            clock));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.replaceAll(";[^/]*", "");
    }
}
