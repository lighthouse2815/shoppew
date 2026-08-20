package com.shoppew.notification.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.notification.dto.PushDeviceRequest;
import com.shoppew.notification.dto.PushDeviceResponse;
import com.shoppew.notification.entity.PushDeviceEntity;
import com.shoppew.notification.repository.PushDeviceRepository;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushDeviceService {
    private static final int MAX_ACTIVE_DEVICES_PER_USER = 10;
    private final PushDeviceRepository devices;
    private final UserRepository users;
    private final PushTargetCodec codec;
    private final Clock clock;

    public PushDeviceService(PushDeviceRepository devices, UserRepository users, PushTargetCodec codec, Clock clock) {
        this.devices = devices;
        this.users = users;
        this.codec = codec;
        this.clock = clock;
    }

    @Transactional
    public PushDeviceResponse register(UUID userId, PushDeviceRequest request) {
        String target = request.target().trim();
        String hash = codec.hash(target);
        Instant now = Instant.now(clock);
        var user = users.getReferenceById(userId);
        PushDeviceEntity device = devices.findByTargetHash(hash).orElseGet(() ->
                PushDeviceEntity.create(
                        user,
                        request.platform(),
                        request.targetType(),
                        hash,
                        codec.encrypt(target, hash),
                        now));
        if (device.getId() != null) {
            device.refresh(
                    user,
                    request.platform(),
                    request.targetType(),
                    codec.encrypt(target, hash),
                    now);
        }
        PushDeviceEntity saved = devices.save(device);
        devices.findAllByUser_IdAndActiveTrueOrderByLastSeenAtDesc(userId).stream()
                .skip(MAX_ACTIVE_DEVICES_PER_USER)
                .forEach(value -> value.deactivate(now));
        return response(saved);
    }

    @Transactional
    public void unregister(UUID userId, String target) {
        String hash = codec.hash(target.trim());
        PushDeviceEntity device = devices.findByTargetHash(hash)
                .filter(value -> value.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "PUSH_DEVICE_NOT_FOUND",
                        "Không tìm thấy thiết bị thông báo"));
        device.deactivate(Instant.now(clock));
    }

    private PushDeviceResponse response(PushDeviceEntity value) {
        return new PushDeviceResponse(
                value.getId(), value.getPlatform(), value.getTargetType(), value.isActive(), value.getLastSeenAt());
    }
}
