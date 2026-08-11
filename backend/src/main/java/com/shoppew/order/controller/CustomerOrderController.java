package com.shoppew.order.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.order.dto.OrderActionRequest;
import com.shoppew.order.dto.OrderDetailResponse;
import com.shoppew.order.dto.OrderSummaryResponse;
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
@RequestMapping("/api/v1/orders")
public class CustomerOrderController {
    private final OrderService orderService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public CustomerOrderController(OrderService orderService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.orderService = orderService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<OrderSummaryResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(orderService.customerOrders(authenticatedUser.id(), page, size), clock);
    }

    @GetMapping("/{orderId}")
    ApiResponse<OrderDetailResponse> detail(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.customerDetail(authenticatedUser.id(), orderId), clock);
    }

    @PostMapping("/{orderId}/cancel")
    ApiResponse<OrderDetailResponse> cancel(
            @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) OrderActionRequest request) {
        return ApiResponse.success(orderService.customerCancel(authenticatedUser.id(), orderId, request), clock);
    }

    @PostMapping("/{orderId}/complete")
    ApiResponse<OrderDetailResponse> complete(@PathVariable UUID orderId) {
        return ApiResponse.success(orderService.customerComplete(authenticatedUser.id(), orderId), clock);
    }
}
