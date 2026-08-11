package com.shoppew.user.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.user.dto.AdminSellerDetailResponse;
import com.shoppew.user.dto.AdminSellerSummaryResponse;
import com.shoppew.user.dto.AdminUserDetailResponse;
import com.shoppew.user.dto.AdminUserStatusRequest;
import com.shoppew.user.dto.AdminUserSummaryResponse;
import com.shoppew.user.entity.UserRole;
import com.shoppew.user.entity.UserStatus;
import com.shoppew.user.service.AdminUserService;
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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService service;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public AdminUserController(
            AdminUserService service, AuthenticatedUser authenticatedUser, Clock clock) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping("/users")
    ApiResponse<PageResponse<AdminUserSummaryResponse>> users(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.users(query, status, role, page, size), clock);
    }

    @GetMapping("/users/{userId}")
    ApiResponse<AdminUserDetailResponse> user(@PathVariable UUID userId) {
        return ApiResponse.success(service.user(userId), clock);
    }

    @PatchMapping("/users/{userId}/status")
    ApiResponse<AdminUserDetailResponse> changeUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.success(service.changeStatus(
                authenticatedUser.id(), userId, request.status(), request.reason().strip()), clock);
    }

    @GetMapping("/sellers")
    ApiResponse<PageResponse<AdminSellerSummaryResponse>> sellers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) ShopStatus shopStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.sellers(query, status, shopStatus, page, size), clock);
    }

    @GetMapping("/sellers/{userId}")
    ApiResponse<AdminSellerDetailResponse> seller(@PathVariable UUID userId) {
        return ApiResponse.success(service.seller(userId), clock);
    }
}
