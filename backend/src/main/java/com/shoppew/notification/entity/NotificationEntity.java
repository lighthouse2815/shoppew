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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @Enumerated(EnumType.STRING) @Column(name = "notification_type", nullable = false, length = 24)
    private NotificationType notificationType;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 1000) private String body;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data = new LinkedHashMap<>();
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected NotificationEntity() {}
    public static NotificationEntity create(UserEntity user, NotificationType type, String title,
            String body, Map<String, Object> data, Instant now) {
        NotificationEntity value = new NotificationEntity(); value.user = user; value.notificationType = type;
        value.title = title; value.body = body; value.data.putAll(data); value.createdAt = now; return value;
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return user.getId(); }
    public String getUserEmail() { return user.getEmail(); }
    public NotificationType getNotificationType() { return notificationType; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Map<String, Object> getData() { return Map.copyOf(data); }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void read(Instant now) { if (readAt == null) readAt = now; }
}
