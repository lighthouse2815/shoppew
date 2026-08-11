package com.shoppew.shop.controller;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.shop.dto.ShopResponse;
import com.shoppew.shop.dto.ShopStatusRequest;
import com.shoppew.shop.service.ShopService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/shops")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminShopController {

    private final ShopService shopService;
    private final AdminAuditService audit;
    private final Clock clock;

    public AdminShopController(ShopService shopService, AdminAuditService audit, Clock clock) {
        this.shopService = shopService;
        this.audit = audit;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<ShopResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) com.shoppew.shop.entity.ShopStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(shopService.adminList(query, status, page, size), clock);
    }

    @GetMapping("/{shopId}")
    ApiResponse<ShopResponse> detail(@PathVariable UUID shopId) {
        return ApiResponse.success(shopService.adminDetail(shopId), clock);
    }

    @PatchMapping("/{shopId}/status")
    ApiResponse<ShopResponse> changeStatus(
            @PathVariable UUID shopId,
            @Valid @RequestBody ShopStatusRequest request) {
        ShopResponse before = shopService.adminDetail(shopId);
        ShopResponse response = shopService.moderate(shopId, request.status());
        audit.record(null, "SHOP_STATUS_CHANGED", "SHOP", shopId, before, response);
        return ApiResponse.success(response, clock);
    }
}
