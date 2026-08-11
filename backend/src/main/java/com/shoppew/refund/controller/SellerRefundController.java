package com.shoppew.refund.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.refund.dto.RefundResponse;
import com.shoppew.refund.dto.RefundReviewRequest;
import com.shoppew.refund.entity.RefundRequestStatus;
import com.shoppew.refund.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/seller/shops/{shopId}/refunds")
public class SellerRefundController {
    private final RefundService service; private final AuthenticatedUser user; private final Clock clock;
    public SellerRefundController(RefundService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<RefundResponse>> list(@PathVariable UUID shopId,
            @RequestParam(required = false) RefundRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.sellerList(user.id(), shopId, status, page, size), clock);
    }
    @PostMapping("/{requestId}/review") ApiResponse<RefundResponse> review(@PathVariable UUID shopId,
            @PathVariable UUID requestId, @Valid @RequestBody RefundReviewRequest request) {
        return ApiResponse.success(service.sellerReview(user.id(), shopId, requestId, request.note()), clock);
    }
}
