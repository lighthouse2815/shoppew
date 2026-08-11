package com.shoppew.inventory.entity;

import com.shoppew.product.entity.ProductVariantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventories")
public class InventoryEntity {

    @Id
    @Column(name = "variant_id")
    private UUID variantId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    @Column(name = "available_quantity", nullable = false)
    private long availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private long reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private long soldQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private long lowStockThreshold;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected InventoryEntity() {}

    public static InventoryEntity create(ProductVariantEntity variant, Instant now) {
        InventoryEntity inventory = new InventoryEntity();
        inventory.variant = variant;
        inventory.variantId = variant.getId();
        inventory.availableQuantity = 0;
        inventory.reservedQuantity = 0;
        inventory.soldQuantity = 0;
        inventory.lowStockThreshold = 5;
        inventory.updatedAt = now;
        return inventory;
    }

    public UUID getVariantId() { return variantId; }
    public ProductVariantEntity getVariant() { return variant; }
    public long getAvailableQuantity() { return availableQuantity; }
    public long getReservedQuantity() { return reservedQuantity; }
    public long getSoldQuantity() { return soldQuantity; }
    public long getLowStockThreshold() { return lowStockThreshold; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAvailableQuantity(long availableQuantity, Long lowStockThreshold, Instant now) {
        if (availableQuantity < 0) throw new IllegalArgumentException("availableQuantity must not be negative");
        if (lowStockThreshold != null) {
            if (lowStockThreshold < 0) throw new IllegalArgumentException("lowStockThreshold must not be negative");
            this.lowStockThreshold = lowStockThreshold;
        }
        this.availableQuantity = availableQuantity;
        this.updatedAt = now;
    }
}
