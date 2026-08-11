package com.shoppew.catalog.controller;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.catalog.dto.CatalogStatusRequest;
import com.shoppew.catalog.dto.CategoryRequest;
import com.shoppew.catalog.dto.CategoryResponse;
import com.shoppew.catalog.service.CategoryService;
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
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AdminAuditService audit;
    private final Clock clock;

    public AdminCategoryController(CategoryService categoryService, AdminAuditService audit, Clock clock) {
        this.categoryService = categoryService;
        this.audit = audit;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.success(categoryService.adminList(), clock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        audit.record(null, "CATEGORY_CREATED", "CATEGORY", response.id(), null, response);
        return ApiResponse.success(response, clock);
    }

    @PutMapping("/{categoryId}")
    ApiResponse<CategoryResponse> update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.update(categoryId, request);
        audit.record(null, "CATEGORY_UPDATED", "CATEGORY", categoryId, null, response);
        return ApiResponse.success(response, clock);
    }

    @PatchMapping("/{categoryId}/status")
    ApiResponse<CategoryResponse> changeStatus(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CatalogStatusRequest request) {
        CategoryResponse response = categoryService.changeStatus(categoryId, request.status());
        audit.record(null, "CATEGORY_STATUS_CHANGED", "CATEGORY", categoryId, null, response);
        return ApiResponse.success(response, clock);
    }
}
