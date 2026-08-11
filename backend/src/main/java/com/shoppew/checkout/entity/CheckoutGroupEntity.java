package com.shoppew.checkout.entity;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "checkout_groups")
public class CheckoutGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "checkout_number", nullable = false, length = 40)
    private String checkoutNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "items_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal itemsSubtotal;

    @Column(name = "shipping_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingTotal;

    @Column(name = "discount_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CheckoutStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheckoutGroupEntity() {}

    public static CheckoutGroupEntity create(
            String checkoutNumber,
            UserEntity user,
            String currency,
            BigDecimal itemsSubtotal,
            BigDecimal shippingTotal,
            BigDecimal discountTotal,
            String idempotencyKey,
            String requestHash,
            CheckoutStatus status,
            Instant now) {
        CheckoutGroupEntity checkout = new CheckoutGroupEntity();
        checkout.checkoutNumber = checkoutNumber;
        checkout.user = user;
        checkout.currency = currency;
        checkout.itemsSubtotal = itemsSubtotal;
        checkout.shippingTotal = shippingTotal;
        checkout.discountTotal = discountTotal;
        checkout.grandTotal = itemsSubtotal.add(shippingTotal).subtract(discountTotal);
        checkout.idempotencyKey = idempotencyKey;
        checkout.requestHash = requestHash;
        checkout.status = status;
        checkout.createdAt = now;
        checkout.updatedAt = now;
        return checkout;
    }

    public UUID getId() { return id; }
    public String getCheckoutNumber() { return checkoutNumber; }
    public UUID getUserId() { return user.getId(); }
    public String getUserEmail() { return user.getEmail(); }
    public String getCurrency() { return currency; }
    public BigDecimal getItemsSubtotal() { return itemsSubtotal; }
    public BigDecimal getShippingTotal() { return shippingTotal; }
    public BigDecimal getDiscountTotal() { return discountTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public CheckoutStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void changeStatus(CheckoutStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
