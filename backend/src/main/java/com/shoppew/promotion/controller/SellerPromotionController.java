package com.shoppew.promotion.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.promotion.dto.PromotionRequest;
import com.shoppew.promotion.dto.PromotionResponse;
import com.shoppew.promotion.service.PromotionManagementService;
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
@RequestMapping("/api/v1/seller/shops/{shopId}/promotions")
public class SellerPromotionController {
    private final PromotionManagementService service; private final AuthenticatedUser user; private final Clock clock;
    public SellerPromotionController(PromotionManagementService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<List<PromotionResponse>> list(@PathVariable UUID shopId) { return ApiResponse.success(service.sellerList(user.id(), shopId), clock); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) ApiResponse<PromotionResponse> create(@PathVariable UUID shopId, @Valid @RequestBody PromotionRequest request) { return ApiResponse.success(service.sellerCreate(user.id(), shopId, request), clock); }
    @PutMapping("/{promotionId}") ApiResponse<PromotionResponse> update(@PathVariable UUID shopId, @PathVariable UUID promotionId, @Valid @RequestBody PromotionRequest request) { return ApiResponse.success(service.sellerUpdate(user.id(), shopId, promotionId, request), clock); }
    @PostMapping("/{promotionId}/{action:activate|pause|archive}") ApiResponse<PromotionResponse> status(@PathVariable UUID shopId, @PathVariable UUID promotionId, @PathVariable String action) { return ApiResponse.success(service.sellerStatus(user.id(), shopId, promotionId, action), clock); }
}
