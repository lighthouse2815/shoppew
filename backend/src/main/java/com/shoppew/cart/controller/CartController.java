package com.shoppew.cart.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.cart.dto.CartBulkSelectionRequest;
import com.shoppew.cart.dto.CartItemRequest;
import com.shoppew.cart.dto.CartQuantityRequest;
import com.shoppew.cart.dto.CartResponse;
import com.shoppew.cart.dto.CartSelectionRequest;
import com.shoppew.cart.service.CartService;
import com.shoppew.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.Clock;
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
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public CartController(CartService cartService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.cartService = cartService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<CartResponse> get() {
        return ApiResponse.success(cartService.get(authenticatedUser.id()), clock);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CartResponse> add(@Valid @RequestBody CartItemRequest request) {
        return ApiResponse.success(cartService.add(authenticatedUser.id(), request), clock);
    }

    @PutMapping("/items/{itemId}")
    ApiResponse<CartResponse> updateQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody CartQuantityRequest request) {
        return ApiResponse.success(cartService.updateQuantity(authenticatedUser.id(), itemId, request), clock);
    }

    @PatchMapping("/items/{itemId}/selection")
    ApiResponse<CartResponse> select(
            @PathVariable UUID itemId,
            @RequestBody CartSelectionRequest request) {
        return ApiResponse.success(cartService.select(authenticatedUser.id(), itemId, request), clock);
    }

    @PutMapping("/selection")
    ApiResponse<CartResponse> selectMany(@Valid @RequestBody CartBulkSelectionRequest request) {
        return ApiResponse.success(cartService.selectMany(authenticatedUser.id(), request), clock);
    }

    @DeleteMapping("/items/{itemId}")
    ApiResponse<CartResponse> remove(@PathVariable UUID itemId) {
        return ApiResponse.success(cartService.remove(authenticatedUser.id(), itemId), clock);
    }

    @DeleteMapping("/items")
    ApiResponse<CartResponse> clear() {
        return ApiResponse.success(cartService.clear(authenticatedUser.id()), clock);
    }
}
