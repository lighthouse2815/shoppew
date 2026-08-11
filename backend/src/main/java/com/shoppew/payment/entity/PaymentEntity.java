package com.shoppew.payment.entity;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkout_group_id", nullable = false, unique = true)
    private CheckoutGroupEntity checkoutGroup;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentProviderType provider;
    @Column(name = "provider_reference", length = 160)
    private String providerReference;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;
    @Column(name = "failure_code", length = 80)
    private String failureCode;
    @Column(name = "failure_message", length = 500)
    private String failureMessage;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    protected PaymentEntity() {}

    public static PaymentEntity create(
            CheckoutGroupEntity checkout, PaymentProviderType provider, String providerReference,
            BigDecimal amount, String currency, String idempotencyKey, Instant now) {
        PaymentEntity payment = new PaymentEntity();
        payment.checkoutGroup = checkout;
        payment.provider = provider;
        payment.providerReference = providerReference;
        payment.status = PaymentStatus.PENDING;
        payment.amount = amount;
        payment.currency = currency;
        payment.idempotencyKey = idempotencyKey;
        payment.createdAt = now;
        payment.updatedAt = now;
        return payment;
    }

    public UUID getId() { return id; }
    public CheckoutGroupEntity getCheckoutGroup() { return checkoutGroup; }
    public UUID getCheckoutGroupId() { return checkoutGroup.getId(); }
    public PaymentProviderType getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void succeed(Instant now) {
        if (status == PaymentStatus.SUCCEEDED) return;
        status = PaymentStatus.SUCCEEDED;
        paidAt = now;
        failureCode = null;
        failureMessage = null;
        updatedAt = now;
    }

    public void fail(String code, String message, Instant now) {
        if (status == PaymentStatus.SUCCEEDED) return;
        status = PaymentStatus.FAILED;
        failureCode = code;
        failureMessage = message;
        updatedAt = now;
    }

    public void cancel(Instant now) {
        if (status == PaymentStatus.PENDING) {
            status = PaymentStatus.CANCELLED;
            updatedAt = now;
        }
    }

    public void recordRefund(BigDecimal refundedTotal, Instant now) {
        status = refundedTotal.compareTo(amount) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = now;
    }
}
