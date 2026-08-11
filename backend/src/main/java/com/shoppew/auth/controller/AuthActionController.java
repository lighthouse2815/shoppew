package com.shoppew.auth.controller;

import com.shoppew.auth.dto.EmailActionRequest;
import com.shoppew.auth.dto.ResetPasswordRequest;
import com.shoppew.auth.dto.TokenActionRequest;
import com.shoppew.auth.service.AuthActionService;
import com.shoppew.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthActionController {

    private final AuthActionService authActionService;
    private final Clock clock;

    public AuthActionController(AuthActionService authActionService, Clock clock) {
        this.authActionService = authActionService;
        this.clock = clock;
    }

    @PostMapping("/verify-email/request")
    ApiResponse<Map<String, String>> requestEmailVerification(@Valid @RequestBody EmailActionRequest request) {
        authActionService.requestEmailVerification(request.email());
        return ApiResponse.success(Map.of("message", "Nếu tài khoản cần xác minh, email hướng dẫn đã được gửi."), clock);
    }

    @PostMapping("/verify-email/confirm")
    ApiResponse<Map<String, Boolean>> verifyEmail(@Valid @RequestBody TokenActionRequest request) {
        authActionService.verifyEmail(request.token());
        return ApiResponse.success(Map.of("verified", true), clock);
    }

    @PostMapping("/forgot-password")
    ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody EmailActionRequest request) {
        authActionService.requestPasswordReset(request.email());
        return ApiResponse.success(Map.of("message", "Nếu tài khoản tồn tại, email đặt lại mật khẩu đã được gửi."), clock);
    }

    @PostMapping("/reset-password")
    ApiResponse<Map<String, Boolean>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authActionService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.success(Map.of("passwordReset", true), clock);
    }
}
