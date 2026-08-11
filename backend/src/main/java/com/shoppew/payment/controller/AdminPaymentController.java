package com.shoppew.payment.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.payment.dto.AdminPaymentResponse;
import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.payment.entity.PaymentStatus;
import com.shoppew.payment.service.PaymentService;
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
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminPaymentController {

    private final PaymentService service;
    private final Clock clock;

    public AdminPaymentController(PaymentService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<AdminPaymentResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentProviderType provider,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.adminPayments(query, status, provider, page, size), clock);
    }

    @GetMapping("/{paymentId}")
    ApiResponse<AdminPaymentResponse> detail(@PathVariable UUID paymentId) {
        return ApiResponse.success(service.adminPayment(paymentId), clock);
    }
}
