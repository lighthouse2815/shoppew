package com.shoppew.voucher.entity;

import com.shoppew.catalog.entity.CategoryEntity;
import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.shop.entity.ShopEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vouchers")
public class VoucherEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 24)
    private VoucherOwnerType ownerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private ShopEntity shop;

    @Column(nullable = false, columnDefinition = "citext")
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", nullable = false, length = 24)
    private VoucherType voucherType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 24)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "minimum_spend", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumSpend;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "total_quantity", nullable = false)
    private long totalQuantity;

    @Column(name = "used_quantity", nullable = false)
    private long usedQuantity;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private VoucherStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "voucher_products", joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<ProductEntity> products = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "voucher_categories", joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<CategoryEntity> categories = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "voucher_payment_methods", joinColumns = @JoinColumn(name = "voucher_id"))
    @Column(name = "payment_provider", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<PaymentProviderType> paymentProviders = new LinkedHashSet<>();

    protected VoucherEntity() {}

    public static VoucherEntity create(
            VoucherOwnerType ownerType, ShopEntity shop, String code, String name, VoucherType voucherType,
            DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscount,
            BigDecimal minimumSpend, String currency, Instant startsAt, Instant endsAt,
            long totalQuantity, int perUserLimit, Set<ProductEntity> products,
            Set<CategoryEntity> categories, Set<PaymentProviderType> paymentProviders, Instant now) {
        VoucherEntity voucher = new VoucherEntity();
        voucher.ownerType = ownerType;
        voucher.shop = shop;
        voucher.apply(code, name, voucherType, discountType, discountValue, maxDiscount, minimumSpend,
                currency, startsAt, endsAt, totalQuantity, perUserLimit, products, categories, paymentProviders, now);
        voucher.usedQuantity = 0;
        voucher.status = VoucherStatus.DRAFT;
        voucher.createdAt = now;
        return voucher;
    }

    public void update(
            String code, String name, VoucherType voucherType, DiscountType discountType,
            BigDecimal discountValue, BigDecimal maxDiscount, BigDecimal minimumSpend, String currency,
            Instant startsAt, Instant endsAt, long totalQuantity, int perUserLimit,
            Set<ProductEntity> products, Set<CategoryEntity> categories,
            Set<PaymentProviderType> paymentProviders, Instant now) {
        apply(code, name, voucherType, discountType, discountValue, maxDiscount, minimumSpend,
                currency, startsAt, endsAt, totalQuantity, perUserLimit, products, categories, paymentProviders, now);
    }

    private void apply(
            String code, String name, VoucherType voucherType, DiscountType discountType,
            BigDecimal discountValue, BigDecimal maxDiscount, BigDecimal minimumSpend, String currency,
            Instant startsAt, Instant endsAt, long totalQuantity, int perUserLimit,
            Set<ProductEntity> products, Set<CategoryEntity> categories,
            Set<PaymentProviderType> paymentProviders, Instant now) {
        this.code = code;
        this.name = name;
        this.voucherType = voucherType;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscount = maxDiscount;
        this.minimumSpend = minimumSpend;
        this.currency = currency;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.totalQuantity = totalQuantity;
        this.perUserLimit = perUserLimit;
        this.products.clear(); this.products.addAll(products);
        this.categories.clear(); this.categories.addAll(categories);
        this.paymentProviders.clear(); this.paymentProviders.addAll(paymentProviders);
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public VoucherOwnerType getOwnerType() { return ownerType; }
    public ShopEntity getShop() { return shop; }
    public UUID getShopId() { return shop == null ? null : shop.getId(); }
    public String getCode() { return code; }
    public String getName() { return name; }
    public VoucherType getVoucherType() { return voucherType; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public BigDecimal getMinimumSpend() { return minimumSpend; }
    public String getCurrency() { return currency; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public long getTotalQuantity() { return totalQuantity; }
    public long getUsedQuantity() { return usedQuantity; }
    public int getPerUserLimit() { return perUserLimit; }
    public VoucherStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<ProductEntity> getProducts() { return Set.copyOf(products); }
    public Set<CategoryEntity> getCategories() { return Set.copyOf(categories); }
    public Set<PaymentProviderType> getPaymentProviders() { return Set.copyOf(paymentProviders); }

    public boolean availableAt(Instant now) {
        return status == VoucherStatus.ACTIVE && !now.isBefore(startsAt) && now.isBefore(endsAt)
                && usedQuantity < totalQuantity;
    }
    public void activate(Instant now) { status = VoucherStatus.ACTIVE; updatedAt = now; }
    public void pause(Instant now) { status = VoucherStatus.PAUSED; updatedAt = now; }
    public void archive(Instant now) { status = VoucherStatus.ARCHIVED; updatedAt = now; }
    public void reserveOne(Instant now) { usedQuantity++; }
    public void releaseOne(Instant now) { if (usedQuantity > 0) usedQuantity--; }
}
