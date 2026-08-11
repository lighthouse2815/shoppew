package com.shoppew.inventory.entity;

import com.shoppew.product.entity.ProductVariantEntity;
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
@Table(name = "inventory_reservations")
public class InventoryReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(nullable = false)
    private long quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private InventoryReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryReservationEntity() {}

    public static InventoryReservationEntity create(
            ProductVariantEntity variant,
            UUID userId,
            UUID orderId,
            long quantity,
            Instant expiresAt,
            Instant now) {
        InventoryReservationEntity reservation = new InventoryReservationEntity();
        reservation.variant = variant;
        reservation.userId = userId;
        reservation.orderId = orderId;
        reservation.quantity = quantity;
        reservation.status = InventoryReservationStatus.ACTIVE;
        reservation.expiresAt = expiresAt;
        reservation.createdAt = now;
        return reservation;
    }

    public UUID getId() { return id; }
    public ProductVariantEntity getVariant() { return variant; }
    public UUID getVariantId() { return variant.getId(); }
    public UUID getUserId() { return userId; }
    public UUID getOrderId() { return orderId; }
    public long getQuantity() { return quantity; }
    public InventoryReservationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getReleasedAt() { return releasedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void release(InventoryReservationStatus terminalStatus, Instant now) {
        if (status != InventoryReservationStatus.ACTIVE) return;
        if (terminalStatus != InventoryReservationStatus.RELEASED
                && terminalStatus != InventoryReservationStatus.EXPIRED) {
            throw new IllegalArgumentException("Reservation release requires RELEASED or EXPIRED status");
        }
        status = terminalStatus;
        releasedAt = now;
    }

    public void consume() {
        if (status == InventoryReservationStatus.ACTIVE) status = InventoryReservationStatus.CONSUMED;
    }
}
