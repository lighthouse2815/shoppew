package com.shoppew.recommendation;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.product.dto.ProductSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/recommendations")
public class PublicRecommendationController {

    private final RecommendationService service;
    private final Clock clock;

    public PublicRecommendationController(RecommendationService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @GetMapping("/popular")
    ApiResponse<List<ProductSummaryResponse>> popular(
            @RequestParam(defaultValue = "12") @Min(1) @Max(40) int size) {
        return ApiResponse.success(service.popular(size), clock);
    }

    @GetMapping("/trending")
    ApiResponse<List<ProductSummaryResponse>> trending(
            @RequestParam(defaultValue = "12") @Min(1) @Max(40) int size) {
        return ApiResponse.success(service.trending(size), clock);
    }

    @GetMapping("/products/{productId}/related")
    ApiResponse<List<ProductSummaryResponse>> related(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "12") @Min(1) @Max(40) int size) {
        return ApiResponse.success(service.related(productId, size), clock);
    }

    @GetMapping("/shops/{shopId}")
    ApiResponse<List<ProductSummaryResponse>> sameShop(
            @PathVariable UUID shopId,
            @RequestParam(required = false) UUID excludeProductId,
            @RequestParam(defaultValue = "12") @Min(1) @Max(40) int size) {
        return ApiResponse.success(service.sameShop(shopId, excludeProductId, size), clock);
    }
}
