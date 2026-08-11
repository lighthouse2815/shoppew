package com.shoppew.shipping.entity;

import com.shoppew.order.entity.OrderEntity;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shipments")
public class ShipmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_method_id", nullable = false)
    private ShippingMethodEntity method;
    @Column(name = "provider_reference", length = 160)
    private String providerReference;
    @Column(name = "tracking_number", length = 160)
    private String trackingNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShipmentStatus status;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;
    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;
    @Column(name = "estimated_delivery_from")
    private LocalDate estimatedDeliveryFrom;
    @Column(name = "estimated_delivery_to")
    private LocalDate estimatedDeliveryTo;
    @Column(name = "shipped_at")
    private Instant shippedAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShipmentEntity() {}

    public static ShipmentEntity create(
            OrderEntity order, ShippingMethodEntity method, BigDecimal fee, String currency,
            LocalDate from, LocalDate to, Instant now) {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.order = order;
        shipment.method = method;
        shipment.status = ShipmentStatus.PENDING;
        shipment.fee = fee;
        shipment.currency = currency;
        shipment.estimatedDeliveryFrom = from;
        shipment.estimatedDeliveryTo = to;
        shipment.createdAt = now;
        shipment.updatedAt = now;
        return shipment;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return order.getId(); }
    public ShippingMethodEntity getMethod() { return method; }
    public String getProviderReference() { return providerReference; }
    public String getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getStatus() { return status; }
    public BigDecimal getFee() { return fee; }
    public String getCurrency() { return currency; }
    public LocalDate getEstimatedDeliveryFrom() { return estimatedDeliveryFrom; }
    public LocalDate getEstimatedDeliveryTo() { return estimatedDeliveryTo; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }

    public void transition(ShipmentStatus next, String trackingNumber, Instant now) {
        status = next;
        if (trackingNumber != null && !trackingNumber.isBlank()) this.trackingNumber = trackingNumber.strip();
        if (next == ShipmentStatus.IN_TRANSIT && shippedAt == null) shippedAt = now;
        if (next == ShipmentStatus.DELIVERED) deliveredAt = now;
        updatedAt = now;
    }
}
