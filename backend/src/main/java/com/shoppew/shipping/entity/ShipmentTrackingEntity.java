package com.shoppew.shipping.entity;

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
@Table(name = "shipment_tracking")
public class ShipmentTrackingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private ShipmentEntity shipment;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShipmentStatus status;
    @Column(length = 500)
    private String description;
    @Column(length = 255)
    private String location;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ShipmentTrackingEntity() {}

    public static ShipmentTrackingEntity create(
            ShipmentEntity shipment, ShipmentStatus status, String description,
            String location, Instant now) {
        ShipmentTrackingEntity tracking = new ShipmentTrackingEntity();
        tracking.shipment = shipment;
        tracking.status = status;
        tracking.description = description;
        tracking.location = location;
        tracking.occurredAt = now;
        tracking.createdAt = now;
        return tracking;
    }

    public ShipmentStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Instant getOccurredAt() { return occurredAt; }
}
