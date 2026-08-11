package com.shoppew.dispute.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.dispute.dto.DisputeMessageRequest;
import com.shoppew.dispute.dto.DisputeResponse;
import com.shoppew.dispute.dto.DisputeUpdateRequest;
import com.shoppew.dispute.entity.DisputeStatus;
import com.shoppew.dispute.service.DisputeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/admin/disputes")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminDisputeController {
    private final DisputeService service; private final AuthenticatedUser user; private final Clock clock;
    public AdminDisputeController(DisputeService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<DisputeResponse>> list(@RequestParam(required = false) DisputeStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.adminList(status, page, size), clock);
    }
    @GetMapping("/{disputeId}") ApiResponse<DisputeResponse> detail(@PathVariable UUID disputeId) { return ApiResponse.success(service.adminDetail(disputeId), clock); }
    @PutMapping("/{disputeId}") ApiResponse<DisputeResponse> update(@PathVariable UUID disputeId,
            @Valid @RequestBody DisputeUpdateRequest request) { return ApiResponse.success(service.adminUpdate(user.id(), disputeId, request), clock); }
    @PostMapping("/{disputeId}/messages") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<DisputeResponse> message(@PathVariable UUID disputeId, @Valid @RequestBody DisputeMessageRequest request) {
        return ApiResponse.success(service.adminMessage(user.id(), disputeId, request), clock);
    }
}
