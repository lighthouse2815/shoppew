package com.shoppew.notification.entity;

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
@Table(name = "notification_deliveries")
public class NotificationDeliveryEntity {
    public enum Channel { IN_APP, EMAIL, PUSH }
    public enum Status { PENDING, SENT, DELIVERED, FAILED, SKIPPED }
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "notification_id")
    private NotificationEntity notification;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Channel channel;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    @Column(name = "provider_reference", length = 180) private String providerReference;
    @Column(name = "attempted_at") private Instant attemptedAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "failure_message", length = 500) private String failureMessage;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected NotificationDeliveryEntity() {}
    public static NotificationDeliveryEntity inApp(NotificationEntity notification, Instant now) {
        NotificationDeliveryEntity value = new NotificationDeliveryEntity(); value.notification = notification;
        value.channel = Channel.IN_APP; value.status = Status.DELIVERED; value.attemptedAt = now;
        value.deliveredAt = now; value.createdAt = now; return value;
    }
    public static NotificationDeliveryEntity pending(
            NotificationEntity notification, Channel channel, Instant now) {
        if (channel == Channel.IN_APP) return inApp(notification, now);
        NotificationDeliveryEntity value = new NotificationDeliveryEntity();
        value.notification = notification; value.channel = channel; value.status = Status.PENDING;
        value.createdAt = now; return value;
    }
    public UUID getId() { return id; }
    public NotificationEntity getNotification() { return notification; }
    public Channel getChannel() { return channel; }
    public Status getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void beginAttempt(Instant now) {
        this.status = Status.SENT;
        this.attemptCount += 1;
        this.attemptedAt = now;
        this.nextAttemptAt = null;
    }
    public void delivered(String providerReference, Instant now) {
        this.status = Status.DELIVERED; this.providerReference = providerReference;
        this.attemptedAt = now; this.deliveredAt = now; this.failureMessage = null; this.nextAttemptAt = null;
    }
    public void skipped(String reason, Instant now) {
        this.status = Status.SKIPPED; this.attemptedAt = now; this.failureMessage = truncate(reason);
    }
    public void failed(String reason, Instant now, Instant nextAttemptAt) {
        this.status = Status.FAILED; this.attemptedAt = now;
        this.failureMessage = truncate(reason); this.nextAttemptAt = nextAttemptAt;
    }
    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
