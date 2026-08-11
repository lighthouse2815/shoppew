package com.shoppew.review.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.review.dto.ReviewResponse;
import com.shoppew.review.service.ReviewService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public/products/{productId}/reviews")
public class PublicReviewController {
    private final ReviewService service; private final Clock clock;
    public PublicReviewController(ReviewService service, Clock clock) { this.service = service; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<ReviewResponse>> list(@PathVariable UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.publicProduct(productId, page, size), clock);
    }
}
