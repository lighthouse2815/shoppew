package com.shoppew.order.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.order.dto.AdminOrderDetailResponse;
import com.shoppew.order.dto.AdminOrderSummaryResponse;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.service.OrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminOrderController {

    private final OrderService service;
    private final Clock clock;

    public AdminOrderController(OrderService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<AdminOrderSummaryResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.adminOrders(query, status, shopId, userId, page, size), clock);
    }

    @GetMapping("/{orderId}")
    ApiResponse<AdminOrderDetailResponse> detail(@PathVariable UUID orderId) {
        return ApiResponse.success(service.adminDetail(orderId), clock);
    }
}
