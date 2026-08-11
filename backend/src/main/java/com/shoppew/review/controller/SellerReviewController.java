package com.shoppew.review.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.review.dto.ReviewResponse;
import com.shoppew.review.dto.SellerReplyRequest;
import com.shoppew.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/reviews")
public class SellerReviewController {
    private final ReviewService service; private final AuthenticatedUser user; private final Clock clock;
    public SellerReviewController(ReviewService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<ReviewResponse>> list(@PathVariable UUID shopId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.sellerList(user.id(), shopId, page, size), clock);
    }
    @PutMapping("/{reviewId}/reply") ApiResponse<ReviewResponse> reply(@PathVariable UUID shopId,
            @PathVariable UUID reviewId, @Valid @RequestBody SellerReplyRequest request) {
        return ApiResponse.success(service.sellerReply(user.id(), shopId, reviewId, request.reply()), clock);
    }
}
