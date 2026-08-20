package com.shoppew.notification.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.notification.dto.NotificationResponse;
import com.shoppew.notification.dto.UnreadCountResponse;
import com.shoppew.notification.dto.PushDeviceRequest;
import com.shoppew.notification.dto.PushDeviceRevocationRequest;
import com.shoppew.notification.dto.PushDeviceResponse;
import com.shoppew.notification.service.PushDeviceService;
import com.shoppew.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service; private final PushDeviceService pushDevices;
    private final AuthenticatedUser user; private final Clock clock;
    public NotificationController(NotificationService service, PushDeviceService pushDevices, AuthenticatedUser user, Clock clock) {
        this.service = service; this.pushDevices = pushDevices; this.user = user; this.clock = clock;
    }
    @GetMapping ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.list(user.id(), page, size), clock);
    }
    @GetMapping("/unread-count") ApiResponse<UnreadCountResponse> unread() { return ApiResponse.success(new UnreadCountResponse(service.unreadCount(user.id())), clock); }
    @PostMapping("/{notificationId}/read") ApiResponse<NotificationResponse> read(@PathVariable UUID notificationId) { return ApiResponse.success(service.read(user.id(), notificationId), clock); }
    @PostMapping("/read-all") ApiResponse<UnreadCountResponse> readAll() { service.readAll(user.id()); return ApiResponse.success(new UnreadCountResponse(0), clock); }
    @PutMapping("/devices/current") ApiResponse<PushDeviceResponse> registerDevice(
            @Valid @RequestBody PushDeviceRequest request) {
        return ApiResponse.success(pushDevices.register(user.id(), request), clock);
    }
    @DeleteMapping("/devices/current") ApiResponse<java.util.Map<String, Boolean>> unregisterDevice(
            @Valid @RequestBody PushDeviceRevocationRequest request) {
        pushDevices.unregister(user.id(), request.target());
        return ApiResponse.success(java.util.Map.of("revoked", true), clock);
    }
}
