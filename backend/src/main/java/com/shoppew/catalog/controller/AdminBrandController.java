package com.shoppew.catalog.controller;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.catalog.dto.BrandRequest;
import com.shoppew.catalog.dto.BrandResponse;
import com.shoppew.catalog.dto.CatalogStatusRequest;
import com.shoppew.catalog.service.BrandService;
import com.shoppew.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/admin/brands")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminBrandController {

    private final BrandService brandService;
    private final AdminAuditService audit;
    private final Clock clock;

    public AdminBrandController(BrandService brandService, AdminAuditService audit, Clock clock) {
        this.brandService = brandService;
        this.audit = audit;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<List<BrandResponse>> list() {
        return ApiResponse.success(brandService.adminList(), clock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<BrandResponse> create(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.create(request);
        audit.record(null, "BRAND_CREATED", "BRAND", response.id(), null, response);
        return ApiResponse.success(response, clock);
    }

    @PutMapping("/{brandId}")
    ApiResponse<BrandResponse> update(@PathVariable UUID brandId, @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.update(brandId, request);
        audit.record(null, "BRAND_UPDATED", "BRAND", brandId, null, response);
        return ApiResponse.success(response, clock);
    }

    @PatchMapping("/{brandId}/status")
    ApiResponse<BrandResponse> changeStatus(
            @PathVariable UUID brandId,
            @Valid @RequestBody CatalogStatusRequest request) {
        BrandResponse response = brandService.changeStatus(brandId, request.status());
        audit.record(null, "BRAND_STATUS_CHANGED", "BRAND", brandId, null, response);
        return ApiResponse.success(response, clock);
    }
}
