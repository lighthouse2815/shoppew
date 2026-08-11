package com.shoppew.promotion.controller;

import com.shoppew.common.api.ApiResponse;
import com.shoppew.promotion.dto.PromotionRequest;
import com.shoppew.promotion.dto.PromotionResponse;
import com.shoppew.promotion.service.PromotionManagementService;
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
@RequestMapping("/api/v1/admin/promotions")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminPromotionController {
    private final PromotionManagementService service; private final Clock clock;
    public AdminPromotionController(PromotionManagementService service, Clock clock) { this.service = service; this.clock = clock; }
    @GetMapping ApiResponse<List<PromotionResponse>> list() { return ApiResponse.success(service.adminList(), clock); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) ApiResponse<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) { return ApiResponse.success(service.adminCreate(request), clock); }
    @PutMapping("/{promotionId}") ApiResponse<PromotionResponse> update(@PathVariable UUID promotionId, @Valid @RequestBody PromotionRequest request) { return ApiResponse.success(service.adminUpdate(promotionId, request), clock); }
    @PostMapping("/{promotionId}/{action:activate|pause|archive}") ApiResponse<PromotionResponse> status(@PathVariable UUID promotionId, @PathVariable String action) { return ApiResponse.success(service.adminStatus(promotionId, action), clock); }
}
