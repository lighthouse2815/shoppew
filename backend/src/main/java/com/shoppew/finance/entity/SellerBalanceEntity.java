package com.shoppew.finance.entity;

import com.shoppew.shop.entity.ShopEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "seller_balances")
public class SellerBalanceEntity implements Persistable<UUID> {
    @Id @Column(name = "shop_id") private UUID shopId;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private ShopEntity shop;
    @Column(nullable = false, columnDefinition = "char(3)") @JdbcTypeCode(SqlTypes.CHAR) private String currency;
    @Column(name = "pending_amount", nullable = false, precision = 19, scale = 2) private BigDecimal pendingAmount;
    @Column(name = "available_amount", nullable = false, precision = 19, scale = 2) private BigDecimal availableAmount;
    @Column(name = "held_amount", nullable = false, precision = 19, scale = 2) private BigDecimal heldAmount;
    @Column(name = "paid_out_amount", nullable = false, precision = 19, scale = 2) private BigDecimal paidOutAmount;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;
    @Transient private boolean isNew = true;

    protected SellerBalanceEntity() {}

    public static SellerBalanceEntity create(ShopEntity shop, String currency, Instant now) {
        SellerBalanceEntity balance = new SellerBalanceEntity();
        balance.shop = shop; balance.currency = currency;
        balance.pendingAmount = BigDecimal.ZERO.setScale(2);
        balance.availableAmount = BigDecimal.ZERO.setScale(2);
        balance.heldAmount = BigDecimal.ZERO.setScale(2);
        balance.paidOutAmount = BigDecimal.ZERO.setScale(2);
        balance.updatedAt = now;
        return balance;
    }

    @Override public UUID getId() { return shopId; }
    @Override public boolean isNew() { return isNew; }
    @PostLoad @PostPersist void markNotNew() { isNew = false; }
    public UUID getShopId() { return shopId; }
    public String getCurrency() { return currency; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public BigDecimal getAvailableAmount() { return availableAmount; }
    public BigDecimal getHeldAmount() { return heldAmount; }
    public BigDecimal getPaidOutAmount() { return paidOutAmount; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void addPending(BigDecimal amount, Instant now) {
        pendingAmount = pendingAmount.add(amount); updatedAt = now;
    }
    public void makeAvailable(BigDecimal amount, Instant now) {
        if (pendingAmount.compareTo(amount) < 0) throw new IllegalStateException("Pending seller balance is insufficient");
        pendingAmount = pendingAmount.subtract(amount); availableAmount = availableAmount.add(amount); updatedAt = now;
    }
    public void deductRefund(BigDecimal amount, Instant now) {
        BigDecimal availableDeduction = availableAmount.min(amount);
        availableAmount = availableAmount.subtract(availableDeduction);
        BigDecimal remaining = amount.subtract(availableDeduction);
        if (remaining.signum() > 0) {
            BigDecimal pendingDeduction = pendingAmount.min(remaining);
            pendingAmount = pendingAmount.subtract(pendingDeduction);
            remaining = remaining.subtract(pendingDeduction);
        }
        if (remaining.signum() > 0) heldAmount = heldAmount.add(remaining);
        updatedAt = now;
    }
    public void payOut(BigDecimal amount, Instant now) {
        if (availableAmount.compareTo(amount) < 0) throw new IllegalStateException("Available seller balance is insufficient");
        availableAmount = availableAmount.subtract(amount); paidOutAmount = paidOutAmount.add(amount); updatedAt = now;
    }
}
