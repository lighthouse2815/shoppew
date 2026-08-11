package com.shoppew.finance.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.finance.dto.SellerBalanceResponse;
import com.shoppew.finance.dto.SellerTransactionResponse;
import com.shoppew.finance.service.SellerFinanceService;
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

@Validated @RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/finance")
public class SellerFinanceController {
    private final SellerFinanceService service; private final AuthenticatedUser user; private final Clock clock;
    public SellerFinanceController(SellerFinanceService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping("/balance") ApiResponse<SellerBalanceResponse> balance(@PathVariable UUID shopId) {
        return ApiResponse.success(service.balance(user.id(), shopId), clock);
    }
    @GetMapping("/transactions") ApiResponse<PageResponse<SellerTransactionResponse>> transactions(
            @PathVariable UUID shopId, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.transactions(user.id(), shopId, page, size), clock);
    }
}
