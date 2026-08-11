package com.shoppew.review.controller;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.review.dto.ReviewResponse;
import com.shoppew.review.service.ReviewService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN','SUPER_ADMIN')")
public class AdminReviewController {
    private final ReviewService service; private final AdminAuditService audit; private final Clock clock;
    public AdminReviewController(ReviewService service, AdminAuditService audit, Clock clock) { this.service = service; this.audit = audit; this.clock = clock; }
    @PostMapping("/{reviewId}/{action:publish|hide|remove}")
    ApiResponse<ReviewResponse> moderate(@PathVariable UUID reviewId, @PathVariable String action) {
        ReviewResponse response = service.moderate(reviewId, action);
        audit.record(null, "REVIEW_" + action.toUpperCase(java.util.Locale.ROOT), "REVIEW", reviewId, null, response);
        return ApiResponse.success(response, clock);
    }
}
