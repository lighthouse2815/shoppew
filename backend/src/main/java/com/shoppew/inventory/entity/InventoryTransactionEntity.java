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
@Table(name = "inventory_transactions")
public class InventoryTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 24)
    private InventoryTransactionType type;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "available_before", nullable = false)
    private long availableBefore;

    @Column(name = "available_after", nullable = false)
    private long availableAfter;

    @Column(name = "reserved_before", nullable = false)
    private long reservedBefore;

    @Column(name = "reserved_after", nullable = false)
    private long reservedAfter;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(length = 500)
    private String note;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryTransactionEntity() {}

    public static InventoryTransactionEntity create(
            ProductVariantEntity variant,
            InventoryTransactionType type,
            long quantity,
            long availableBefore,
            long availableAfter,
            long reservedBefore,
            long reservedAfter,
            String referenceType,
            UUID referenceId,
            String note,
            UUID actorId,
            Instant now) {
        InventoryTransactionEntity transaction = new InventoryTransactionEntity();
        transaction.variant = variant;
        transaction.type = type;
        transaction.quantity = quantity;
        transaction.availableBefore = availableBefore;
        transaction.availableAfter = availableAfter;
        transaction.reservedBefore = reservedBefore;
        transaction.reservedAfter = reservedAfter;
        transaction.referenceType = referenceType;
        transaction.referenceId = referenceId;
        transaction.note = note;
        transaction.actorId = actorId;
        transaction.createdAt = now;
        return transaction;
    }

    public UUID getId() { return id; }
    public UUID getVariantId() { return variant.getId(); }
    public InventoryTransactionType getType() { return type; }
    public long getQuantity() { return quantity; }
    public long getAvailableBefore() { return availableBefore; }
    public long getAvailableAfter() { return availableAfter; }
    public long getReservedBefore() { return reservedBefore; }
    public long getReservedAfter() { return reservedAfter; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public String getNote() { return note; }
    public UUID getActorId() { return actorId; }
    public Instant getCreatedAt() { return createdAt; }
}
