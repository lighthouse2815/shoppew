package com.shoppew.inventory.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.inventory.dto.InventoryAdjustmentRequest;
import com.shoppew.inventory.dto.InventoryResponse;
import com.shoppew.inventory.dto.InventoryTransactionResponse;
import com.shoppew.inventory.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/inventory")
public class SellerInventoryController {

    private final InventoryService inventoryService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public SellerInventoryController(
            InventoryService inventoryService,
            AuthenticatedUser authenticatedUser,
            Clock clock) {
        this.inventoryService = inventoryService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<InventoryResponse>> list(
            @PathVariable UUID shopId,
            @RequestParam(required = false) @Size(max = 160) String q,
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                inventoryService.list(authenticatedUser.id(), shopId, q, lowStock, page, size), clock);
    }

    @PostMapping("/{variantId}/adjustments")
    ApiResponse<InventoryResponse> adjust(
            @PathVariable UUID shopId,
            @PathVariable UUID variantId,
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        return ApiResponse.success(
                inventoryService.adjust(authenticatedUser.id(), shopId, variantId, request), clock);
    }

    @GetMapping("/{variantId}/transactions")
    ApiResponse<PageResponse<InventoryTransactionResponse>> transactions(
            @PathVariable UUID shopId,
            @PathVariable UUID variantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                inventoryService.transactions(authenticatedUser.id(), shopId, variantId, page, size), clock);
    }
}
