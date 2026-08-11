package com.shoppew.finance.entity;

import com.shoppew.order.entity.OrderEntity;
import com.shoppew.refund.entity.RefundEntity;
import com.shoppew.shop.entity.ShopEntity;
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
@Table(name = "seller_transactions")
public class SellerTransactionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private ShopEntity shop;
    @Enumerated(EnumType.STRING) @Column(name = "transaction_type", nullable = false, length = 32) private SellerTransactionType transactionType;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, columnDefinition = "char(3)") @JdbcTypeCode(SqlTypes.CHAR) private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "balance_bucket", nullable = false, length = 24) private BalanceBucket balanceBucket;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "refund_id") private RefundEntity refund;
    @Column(name = "reference_key", nullable = false, length = 180) private String referenceKey;
    @Column(length = 500) private String description;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected SellerTransactionEntity() {}
    public static SellerTransactionEntity create(ShopEntity shop, SellerTransactionType type, BigDecimal amount,
            String currency, BalanceBucket bucket, OrderEntity order, RefundEntity refund,
            String referenceKey, String description, Instant now) {
        SellerTransactionEntity transaction = new SellerTransactionEntity();
        transaction.shop = shop; transaction.transactionType = type; transaction.amount = amount;
        transaction.currency = currency; transaction.balanceBucket = bucket; transaction.order = order;
        transaction.refund = refund; transaction.referenceKey = referenceKey; transaction.description = description;
        transaction.createdAt = now; return transaction;
    }
    public UUID getId() { return id; }
    public UUID getShopId() { return shop.getId(); }
    public SellerTransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public BalanceBucket getBalanceBucket() { return balanceBucket; }
    public UUID getOrderId() { return order == null ? null : order.getId(); }
    public UUID getRefundId() { return refund == null ? null : refund.getId(); }
    public String getReferenceKey() { return referenceKey; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
