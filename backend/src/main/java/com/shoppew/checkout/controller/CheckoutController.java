package com.shoppew.checkout.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.checkout.dto.CheckoutPreviewResponse;
import com.shoppew.checkout.dto.CheckoutRequest;
import com.shoppew.checkout.dto.CheckoutResponse;
import com.shoppew.checkout.service.CheckoutService;
import com.shoppew.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public CheckoutController(CheckoutService checkoutService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.checkoutService = checkoutService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @PostMapping("/preview")
    ApiResponse<CheckoutPreviewResponse> preview(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success(checkoutService.preview(authenticatedUser.id(), request), clock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CheckoutResponse> place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success(
                checkoutService.place(authenticatedUser.id(), idempotencyKey, request), clock);
    }
}
