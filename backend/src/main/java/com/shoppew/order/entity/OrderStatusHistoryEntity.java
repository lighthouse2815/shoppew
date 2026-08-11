package com.shoppew.order.entity;

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
@Table(name = "order_status_history")
public class OrderStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private OrderStatus toStatus;
    @Column(name = "actor_id")
    private UUID actorId;
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 24)
    private OrderActorType actorType;
    @Column(length = 500)
    private String reason;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderStatusHistoryEntity() {}

    public static OrderStatusHistoryEntity create(
            OrderEntity order, OrderStatus from, OrderStatus to, UUID actorId,
            OrderActorType actorType, String reason, Instant now) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.order = order;
        history.fromStatus = from;
        history.toStatus = to;
        history.actorId = actorId;
        history.actorType = actorType;
        history.reason = reason;
        history.createdAt = now;
        return history;
    }

    public UUID getId() { return id; }
    public OrderStatus getFromStatus() { return fromStatus; }
    public OrderStatus getToStatus() { return toStatus; }
    public UUID getActorId() { return actorId; }
    public OrderActorType getActorType() { return actorType; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
