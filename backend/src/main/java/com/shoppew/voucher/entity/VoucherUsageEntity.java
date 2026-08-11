package com.shoppew.voucher.entity;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.order.entity.OrderEntity;
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
@Table(name = "voucher_usages")
public class VoucherUsageEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "voucher_id")
    private VoucherEntity voucher;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "checkout_group_id")
    private CheckoutGroupEntity checkoutGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id")
    private OrderEntity order;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(nullable = false, columnDefinition = "char(3)") @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private VoucherUsageStatus status;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "released_at") private Instant releasedAt;

    protected VoucherUsageEntity() {}

    public static VoucherUsageEntity reserve(
            VoucherEntity voucher, UserEntity user, CheckoutGroupEntity checkout, OrderEntity order,
            BigDecimal discountAmount, String currency, Instant now) {
        VoucherUsageEntity usage = new VoucherUsageEntity();
        usage.voucher = voucher; usage.user = user; usage.checkoutGroup = checkout; usage.order = order;
        usage.discountAmount = discountAmount; usage.currency = currency;
        usage.status = VoucherUsageStatus.RESERVED; usage.createdAt = now;
        return usage;
    }

    public UUID getId() { return id; }
    public VoucherEntity getVoucher() { return voucher; }
    public UUID getVoucherId() { return voucher.getId(); }
    public UUID getCheckoutGroupId() { return checkoutGroup.getId(); }
    public UUID getOrderId() { return order.getId(); }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public VoucherUsageStatus getStatus() { return status; }
    public void consume(Instant now) { if (status == VoucherUsageStatus.RESERVED) { status = VoucherUsageStatus.CONSUMED; consumedAt = now; } }
    public void release(Instant now) { if (status != VoucherUsageStatus.RELEASED) { status = VoucherUsageStatus.RELEASED; releasedAt = now; } }
}
