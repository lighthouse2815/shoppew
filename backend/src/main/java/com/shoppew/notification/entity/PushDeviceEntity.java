package com.shoppew.notification.entity;

import com.shoppew.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_devices")
public class PushDeviceEntity {
    public enum Platform { ANDROID, IOS, WEB }
    public enum TargetType { FID, TOKEN }

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private UserEntity user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Platform platform;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 24)
    private TargetType targetType;
    @Column(name = "target_hash", nullable = false, length = 128) private String targetHash;
    @Column(name = "encrypted_target", nullable = false) private String encryptedTarget;
    @Column(nullable = false) private boolean active;
    @Column(name = "last_seen_at", nullable = false) private Instant lastSeenAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PushDeviceEntity() {}

    public static PushDeviceEntity create(
            UserEntity user,
            Platform platform,
            TargetType targetType,
            String targetHash,
            String encryptedTarget,
            Instant now) {
        PushDeviceEntity value = new PushDeviceEntity();
        value.user = user;
        value.refresh(user, platform, targetType, encryptedTarget, now);
        value.targetHash = targetHash;
        value.createdAt = now;
        return value;
    }

    public void refresh(
            UserEntity user,
            Platform platform,
            TargetType targetType,
            String encryptedTarget,
            Instant now) {
        this.user = user;
        this.platform = platform;
        this.targetType = targetType;
        this.encryptedTarget = encryptedTarget;
        this.active = true;
        this.lastSeenAt = now;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return user.getId(); }
    public Platform getPlatform() { return platform; }
    public TargetType getTargetType() { return targetType; }
    public String getTargetHash() { return targetHash; }
    public String getEncryptedTarget() { return encryptedTarget; }
    public boolean isActive() { return active; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}
