package com.shoppew.recommendation;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.product.dto.ProductSummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/recommendations/recently-viewed")
public class UserRecommendationController {

    private final RecommendationService service;
    private final AuthenticatedUser user;
    private final Clock clock;

    public UserRecommendationController(RecommendationService service, AuthenticatedUser user, Clock clock) {
        this.service = service;
        this.user = user;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<List<ProductSummaryResponse>> list(
            @RequestParam(defaultValue = "12") @Min(1) @Max(40) int size) {
        return ApiResponse.success(service.recentlyViewed(user.id(), size), clock);
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Map<String, UUID>> record(@PathVariable UUID productId) {
        service.recordView(user.id(), productId);
        return ApiResponse.success(Map.of("productId", productId), clock);
    }
}
