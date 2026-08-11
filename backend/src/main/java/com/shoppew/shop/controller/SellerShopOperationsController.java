package com.shoppew.shop.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.shop.dto.ShopAddressRequest;
import com.shoppew.shop.dto.ShopAddressResponse;
import com.shoppew.shop.dto.ShopSettingsRequest;
import com.shoppew.shop.dto.ShopSettingsResponse;
import com.shoppew.shop.service.ShopOperationsService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}")
public class SellerShopOperationsController {

    private final ShopOperationsService operationsService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public SellerShopOperationsController(
            ShopOperationsService operationsService,
            AuthenticatedUser authenticatedUser,
            Clock clock) {
        this.operationsService = operationsService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping("/addresses")
    ApiResponse<List<ShopAddressResponse>> addresses(@PathVariable UUID shopId) {
        return ApiResponse.success(operationsService.addresses(authenticatedUser.id(), shopId), clock);
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ShopAddressResponse> createAddress(
            @PathVariable UUID shopId,
            @Valid @RequestBody ShopAddressRequest request) {
        return ApiResponse.success(
                operationsService.createAddress(authenticatedUser.id(), shopId, request),
                clock);
    }

    @PutMapping("/addresses/{addressId}")
    ApiResponse<ShopAddressResponse> updateAddress(
            @PathVariable UUID shopId,
            @PathVariable UUID addressId,
            @Valid @RequestBody ShopAddressRequest request) {
        return ApiResponse.success(
                operationsService.updateAddress(authenticatedUser.id(), shopId, addressId, request),
                clock);
    }

    @PatchMapping("/addresses/{addressId}/default")
    ApiResponse<ShopAddressResponse> setDefaultAddress(
            @PathVariable UUID shopId,
            @PathVariable UUID addressId) {
        return ApiResponse.success(
                operationsService.setDefaultAddress(authenticatedUser.id(), shopId, addressId),
                clock);
    }

    @DeleteMapping("/addresses/{addressId}")
    ApiResponse<Map<String, Boolean>> deleteAddress(
            @PathVariable UUID shopId,
            @PathVariable UUID addressId) {
        operationsService.deleteAddress(authenticatedUser.id(), shopId, addressId);
        return ApiResponse.success(Map.of("deleted", true), clock);
    }

    @GetMapping("/settings")
    ApiResponse<ShopSettingsResponse> settings(@PathVariable UUID shopId) {
        return ApiResponse.success(operationsService.settings(authenticatedUser.id(), shopId), clock);
    }

    @PutMapping("/settings")
    ApiResponse<ShopSettingsResponse> updateSettings(
            @PathVariable UUID shopId,
            @Valid @RequestBody ShopSettingsRequest request) {
        return ApiResponse.success(
                operationsService.updateSettings(authenticatedUser.id(), shopId, request),
                clock);
    }
}
