package com.shoppew.shop.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.shop.dto.CreateShopRequest;
import com.shoppew.shop.dto.ShopResponse;
import com.shoppew.shop.dto.UpdateShopRequest;
import com.shoppew.shop.service.ShopService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/shops")
public class SellerShopController {

    private final ShopService shopService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public SellerShopController(ShopService shopService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.shopService = shopService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<List<ShopResponse>> owned() {
        return ApiResponse.success(shopService.owned(authenticatedUser.id()), clock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ShopResponse> create(@Valid @RequestBody CreateShopRequest request) {
        return ApiResponse.success(shopService.create(authenticatedUser.id(), request), clock);
    }

    @PutMapping("/{shopId}")
    ApiResponse<ShopResponse> update(@PathVariable UUID shopId, @Valid @RequestBody UpdateShopRequest request) {
        return ApiResponse.success(shopService.update(authenticatedUser.id(), shopId, request), clock);
    }
}
