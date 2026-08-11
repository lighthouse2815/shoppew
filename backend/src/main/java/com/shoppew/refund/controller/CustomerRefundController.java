package com.shoppew.refund.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.refund.dto.RefundCreateRequest;
import com.shoppew.refund.dto.RefundResponse;
import com.shoppew.refund.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/refunds")
public class CustomerRefundController {
    private final RefundService service; private final AuthenticatedUser user; private final Clock clock;
    public CustomerRefundController(RefundService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<RefundResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return ApiResponse.success(service.customerList(user.id(), page, size), clock); }
    @GetMapping("/{requestId}") ApiResponse<RefundResponse> detail(@PathVariable UUID requestId) { return ApiResponse.success(service.customerDetail(user.id(), requestId), clock); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<RefundResponse> create(@Valid @RequestBody RefundCreateRequest request) { return ApiResponse.success(service.create(user.id(), request), clock); }
    @PostMapping("/{requestId}/cancel") ApiResponse<RefundResponse> cancel(@PathVariable UUID requestId) { return ApiResponse.success(service.cancel(user.id(), requestId), clock); }
}
