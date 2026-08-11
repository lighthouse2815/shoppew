package com.shoppew.payment.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.payment.dto.MockPaymentWebhookRequest;
import com.shoppew.payment.dto.PaymentResponse;
import com.shoppew.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public PaymentController(PaymentService paymentService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.paymentService = paymentService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping("/{paymentId}")
    ApiResponse<PaymentResponse> detail(@PathVariable UUID paymentId) {
        return ApiResponse.success(paymentService.customerPayment(authenticatedUser.id(), paymentId), clock);
    }

    @PostMapping("/mock/webhook")
    ApiResponse<PaymentResponse> mockWebhook(
            @RequestHeader(value = "X-Shoppew-Mock-Signature", required = false) String signature,
            @Valid @RequestBody MockPaymentWebhookRequest request) {
        return ApiResponse.success(paymentService.processMockWebhook(signature, request), clock);
    }
}
