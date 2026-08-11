package com.shoppew.catalog.controller;

import com.shoppew.catalog.dto.BrandResponse;
import com.shoppew.catalog.dto.CategoryTreeResponse;
import com.shoppew.catalog.service.BrandService;
import com.shoppew.catalog.service.CategoryService;
import com.shoppew.common.api.ApiResponse;
import java.time.Clock;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicCatalogController {

    private final CategoryService categoryService;
    private final BrandService brandService;
    private final Clock clock;

    public PublicCatalogController(CategoryService categoryService, BrandService brandService, Clock clock) {
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.clock = clock;
    }

    @GetMapping("/categories")
    ApiResponse<List<CategoryTreeResponse>> categories() {
        return ApiResponse.success(categoryService.publicTree(), clock);
    }

    @GetMapping("/brands")
    ApiResponse<List<BrandResponse>> brands() {
        return ApiResponse.success(brandService.publicList(), clock);
    }
}
