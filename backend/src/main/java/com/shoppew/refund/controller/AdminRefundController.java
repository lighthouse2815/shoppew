package com.shoppew.refund.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.refund.dto.RefundDecisionRequest;
import com.shoppew.refund.dto.RefundResponse;
import com.shoppew.refund.dto.RefundReviewRequest;
import com.shoppew.refund.entity.RefundRequestStatus;
import com.shoppew.refund.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/admin/refunds")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminRefundController {
    private final RefundService service; private final AuthenticatedUser user; private final Clock clock;
    public AdminRefundController(RefundService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<RefundResponse>> list(@RequestParam(required = false) RefundRequestStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.adminList(status, page, size), clock);
    }
    @PostMapping("/{requestId}/approve") ApiResponse<RefundResponse> approve(@PathVariable UUID requestId,
            @Valid @RequestBody RefundDecisionRequest request) { return ApiResponse.success(service.approve(user.id(), requestId, request), clock); }
    @PostMapping("/{requestId}/reject") ApiResponse<RefundResponse> reject(@PathVariable UUID requestId,
            @Valid @RequestBody RefundReviewRequest request) { return ApiResponse.success(service.reject(user.id(), requestId, request.note()), clock); }
    @PostMapping("/{requestId}/process") ApiResponse<RefundResponse> process(@PathVariable UUID requestId,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return ApiResponse.success(service.process(user.id(), requestId, idempotencyKey), clock);
    }
}
