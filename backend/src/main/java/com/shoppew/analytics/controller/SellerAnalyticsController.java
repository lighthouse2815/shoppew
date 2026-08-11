package com.shoppew.analytics.controller;

import com.shoppew.analytics.dto.SellerAnalyticsResponse;
import com.shoppew.analytics.service.MarketplaceAnalyticsService;
import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/seller/shops/{shopId}/analytics")
public class SellerAnalyticsController {
    private final MarketplaceAnalyticsService service; private final AuthenticatedUser user; private final Clock clock;
    public SellerAnalyticsController(MarketplaceAnalyticsService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<SellerAnalyticsResponse> analytics(@PathVariable UUID shopId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return ApiResponse.success(service.seller(user.id(), shopId, from, to), clock);
    }
}
