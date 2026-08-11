package com.shoppew.analytics.controller;

import com.shoppew.analytics.dto.AdminAnalyticsResponse;
import com.shoppew.analytics.service.MarketplaceAnalyticsService;
import com.shoppew.common.api.ApiResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminAnalyticsController {
    private final MarketplaceAnalyticsService service; private final Clock clock;
    public AdminAnalyticsController(MarketplaceAnalyticsService service, Clock clock) { this.service = service; this.clock = clock; }
    @GetMapping ApiResponse<AdminAnalyticsResponse> analytics(@RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) { return ApiResponse.success(service.admin(from, to), clock); }
}
