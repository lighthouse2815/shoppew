package com.shoppew.audit.controller;

import com.shoppew.audit.dto.AuditLogResponse;
import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated @RestController @RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminAuditController {
    private final AdminAuditService service; private final Clock clock;
    public AdminAuditController(AdminAuditService service, Clock clock) { this.service = service; this.clock = clock; }
    @GetMapping ApiResponse<PageResponse<AuditLogResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(page, size), clock);
    }
}
