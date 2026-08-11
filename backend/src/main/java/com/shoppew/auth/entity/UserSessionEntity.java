package com.shoppew.auth.entity;

import com.shoppew.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "user_sessions")
public class UserSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "device_name", length = 160)
    private String deviceName;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", length = 120)
    private String revokeReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotated_from_session_id")
    private UserSessionEntity rotatedFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserSessionEntity() {}

    public static UserSessionEntity create(
            UserEntity user,
            UUID familyId,
            String refreshTokenHash,
            String deviceName,
            String userAgent,
            Instant expiresAt,
            Instant now,
            UserSessionEntity rotatedFrom) {
        UserSessionEntity session = new UserSessionEntity();
        session.user = user;
        session.tokenFamilyId = familyId;
        session.refreshTokenHash = refreshTokenHash;
        session.deviceName = deviceName;
        session.userAgent = userAgent;
        session.expiresAt = expiresAt;
        session.lastUsedAt = now;
        session.createdAt = now;
        session.rotatedFrom = rotatedFrom;
        return session;
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isUsableAt(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public boolean wasRotated() {
        return "ROTATED".equals(revokeReason);
    }

    public void revoke(Instant now, String reason) {
        if (revokedAt == null) {
            revokedAt = now;
            revokeReason = reason;
        }
    }

    public void touch(Instant now) {
        lastUsedAt = now;
    }
}
