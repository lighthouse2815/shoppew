package com.shoppew.order.event;

import com.shoppew.order.entity.OrderActorType;
import com.shoppew.order.entity.OrderStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        OrderStatus from,
        OrderStatus to,
        UUID actorId,
        OrderActorType actorType,
        Instant occurredAt) {}
