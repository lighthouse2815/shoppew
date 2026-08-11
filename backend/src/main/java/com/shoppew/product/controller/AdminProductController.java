package com.shoppew.product.controller;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.product.dto.AttributeDefinitionRequest;
import com.shoppew.product.dto.AttributeDefinitionResponse;
import com.shoppew.product.dto.ProductDetailResponse;
import com.shoppew.product.dto.ProductModerationRequest;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.service.ProductAttributeService;
import com.shoppew.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
public class AdminProductController {

    private final ProductService productService;
    private final ProductAttributeService attributeService;
    private final AdminAuditService audit;
    private final Clock clock;

    public AdminProductController(
            ProductService productService,
            ProductAttributeService attributeService,
            AdminAuditService audit,
            Clock clock) {
        this.productService = productService;
        this.attributeService = attributeService;
        this.audit = audit;
        this.clock = clock;
    }

    @GetMapping("/pending")
    ApiResponse<PageResponse<ProductSummaryResponse>> pending(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return ApiResponse.success(productService.pendingProducts(page, size), clock);
    }

    @PostMapping("/{productId}/approve")
    ApiResponse<ProductDetailResponse> approve(@PathVariable UUID productId) {
        ProductDetailResponse response = productService.approve(productId);
        audit.record(null, "PRODUCT_APPROVED", "PRODUCT", productId, null, response);
        return ApiResponse.success(response, clock);
    }

    @PostMapping("/{productId}/reject")
    ApiResponse<ProductDetailResponse> reject(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductModerationRequest request) {
        ProductDetailResponse response = productService.reject(productId, request.reason());
        audit.record(null, "PRODUCT_REJECTED", "PRODUCT", productId, null, response);
        return ApiResponse.success(response, clock);
    }

    @PostMapping("/{productId}/hide")
    ApiResponse<ProductDetailResponse> hide(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductModerationRequest request) {
        ProductDetailResponse response = productService.hide(productId, request.reason());
        audit.record(null, "PRODUCT_HIDDEN", "PRODUCT", productId, null, response);
        return ApiResponse.success(response, clock);
    }

    @GetMapping("/attributes")
    ApiResponse<List<AttributeDefinitionResponse>> attributes(@RequestParam(required = false) UUID categoryId) {
        return ApiResponse.success(attributeService.definitions(categoryId), clock);
    }

    @PostMapping("/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AttributeDefinitionResponse> createAttribute(
            @Valid @RequestBody AttributeDefinitionRequest request) {
        AttributeDefinitionResponse response = attributeService.createDefinition(request);
        audit.record(null, "ATTRIBUTE_CREATED", "ATTRIBUTE_DEFINITION", response.id(), null, response);
        return ApiResponse.success(response, clock);
    }

    @PutMapping("/attributes/{attributeId}")
    ApiResponse<AttributeDefinitionResponse> updateAttribute(
            @PathVariable UUID attributeId,
            @Valid @RequestBody AttributeDefinitionRequest request) {
        AttributeDefinitionResponse response = attributeService.updateDefinition(attributeId, request);
        audit.record(null, "ATTRIBUTE_UPDATED", "ATTRIBUTE_DEFINITION", attributeId, null, response);
        return ApiResponse.success(response, clock);
    }
}
