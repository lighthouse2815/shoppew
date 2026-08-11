package com.shoppew.voucher.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.voucher.dto.VoucherRequest;
import com.shoppew.voucher.dto.VoucherResponse;
import com.shoppew.voucher.service.VoucherManagementService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminVoucherController {
    private final VoucherManagementService service; private final Clock clock;
    public AdminVoucherController(VoucherManagementService service, Clock clock) { this.service = service; this.clock = clock; }
    @GetMapping ApiResponse<List<VoucherResponse>> list() { return ApiResponse.success(service.adminList(), clock); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<VoucherResponse> create(@Valid @RequestBody VoucherRequest request) { return ApiResponse.success(service.adminCreate(request), clock); }
    @PutMapping("/{voucherId}") ApiResponse<VoucherResponse> update(@PathVariable UUID voucherId, @Valid @RequestBody VoucherRequest request) { return ApiResponse.success(service.adminUpdate(voucherId, request), clock); }
    @PostMapping("/{voucherId}/{action:activate|pause|archive}") ApiResponse<VoucherResponse> status(@PathVariable UUID voucherId, @PathVariable String action) { return ApiResponse.success(service.adminStatus(voucherId, action), clock); }
}
