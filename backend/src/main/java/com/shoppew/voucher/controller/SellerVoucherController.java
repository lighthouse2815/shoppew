package com.shoppew.voucher.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.voucher.dto.VoucherRequest;
import com.shoppew.voucher.dto.VoucherResponse;
import com.shoppew.voucher.service.VoucherManagementService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/vouchers")
public class SellerVoucherController {
    private final VoucherManagementService service; private final AuthenticatedUser user; private final Clock clock;
    public SellerVoucherController(VoucherManagementService service, AuthenticatedUser user, Clock clock) {
        this.service = service; this.user = user; this.clock = clock;
    }
    @GetMapping ApiResponse<List<VoucherResponse>> list(@PathVariable UUID shopId) { return ApiResponse.success(service.sellerList(user.id(), shopId), clock); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<VoucherResponse> create(@PathVariable UUID shopId, @Valid @RequestBody VoucherRequest request) { return ApiResponse.success(service.sellerCreate(user.id(), shopId, request), clock); }
    @PutMapping("/{voucherId}") ApiResponse<VoucherResponse> update(@PathVariable UUID shopId, @PathVariable UUID voucherId, @Valid @RequestBody VoucherRequest request) { return ApiResponse.success(service.sellerUpdate(user.id(), shopId, voucherId, request), clock); }
    @PostMapping("/{voucherId}/{action:activate|pause|archive}") ApiResponse<VoucherResponse> status(@PathVariable UUID shopId, @PathVariable UUID voucherId, @PathVariable String action) { return ApiResponse.success(service.sellerStatus(user.id(), shopId, voucherId, action), clock); }
}
