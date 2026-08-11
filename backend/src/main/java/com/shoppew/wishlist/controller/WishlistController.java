package com.shoppew.wishlist.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.wishlist.dto.WishlistResponse;
import com.shoppew.wishlist.service.WishlistService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {
    private final WishlistService service; private final AuthenticatedUser user; private final Clock clock;
    public WishlistController(WishlistService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<List<WishlistResponse>> list() { return ApiResponse.success(service.list(user.id()), clock); }
    @PostMapping("/products/{productId}") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<WishlistResponse> add(@PathVariable UUID productId) { return ApiResponse.success(service.add(user.id(), productId), clock); }
    @DeleteMapping("/products/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID productId) { service.remove(user.id(), productId); }
}
