package com.shoppew.order.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.order.dto.OrderActionRequest;
import com.shoppew.order.dto.OrderDetailResponse;
import com.shoppew.order.dto.OrderSummaryResponse;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/orders")
public class SellerOrderController {
    private final OrderService orderService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public SellerOrderController(OrderService orderService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.orderService = orderService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<OrderSummaryResponse>> list(
            @PathVariable UUID shopId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                orderService.sellerOrders(authenticatedUser.id(), shopId, status, page, size), clock);
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderDetailResponse> detail(@PathVariable UUID shopId, @PathVariable UUID orderId) {
        return ApiResponse.success(orderService.sellerDetail(authenticatedUser.id(), shopId, orderId), clock);
    }

    @PostMapping("/{orderId}/confirm")
    ApiResponse<OrderDetailResponse> confirm(
            @PathVariable UUID shopId, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.sellerConfirm(authenticatedUser.id(), shopId, orderId, request), clock);
    }

    @PostMapping("/{orderId}/process")
    ApiResponse<OrderDetailResponse> process(
            @PathVariable UUID shopId, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.sellerProcess(authenticatedUser.id(), shopId, orderId, request), clock);
    }

    @PostMapping("/{orderId}/ready-to-ship")
    ApiResponse<OrderDetailResponse> ready(
            @PathVariable UUID shopId, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.sellerReady(authenticatedUser.id(), shopId, orderId, request), clock);
    }

    @PostMapping("/{orderId}/ship")
    ApiResponse<OrderDetailResponse> ship(
            @PathVariable UUID shopId, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.sellerShip(authenticatedUser.id(), shopId, orderId, request), clock);
    }

    @PostMapping("/{orderId}/deliver")
    ApiResponse<OrderDetailResponse> deliver(
            @PathVariable UUID shopId, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.sellerDeliver(authenticatedUser.id(), shopId, orderId, request), clock);
    }

    @PostMapping("/{orderId}/cancel")
    ApiResponse<OrderDetailResponse> cancel(
            @PathVariable UUID shopId, @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.sellerCancel(authenticatedUser.id(), shopId, orderId, request), clock);
    }
}
