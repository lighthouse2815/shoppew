package com.shoppew.product.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.product.dto.ProductDetailResponse;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.service.ProductService;
import com.shoppew.search.ProductSearchCriteria;
import com.shoppew.search.SearchService;
import com.shoppew.search.SearchSort;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/products")
public class PublicProductController {

    private final ProductService productService;
    private final SearchService searchService;
    private final Clock clock;

    public PublicProductController(ProductService productService, SearchService searchService, Clock clock) {
        this.productService = productService;
        this.searchService = searchService;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<ProductSummaryResponse>> search(
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxPrice,
            @RequestParam(required = false) @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal minRating,
            @RequestParam(defaultValue = "RELEVANCE") SearchSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return ApiResponse.success(searchService.search(new ProductSearchCriteria(
                q, shopId, categoryId, brandId, minPrice, maxPrice, minRating, sort, page, size)), clock);
    }

    @GetMapping("/{slug}")
    ApiResponse<ProductDetailResponse> detail(@PathVariable String slug) {
        return ApiResponse.success(productService.publicDetail(slug), clock);
    }
}
