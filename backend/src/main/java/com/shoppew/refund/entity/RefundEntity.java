package com.shoppew.refund.entity;

import com.shoppew.payment.entity.PaymentEntity;
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
@Table(name = "refunds")
public class RefundEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "refund_request_id") private RefundRequestEntity refundRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id") private PaymentEntity payment;
    @Column(name = "provider_reference", length = 180) private String providerReference;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "seller_charge_amount", nullable = false, precision = 19, scale = 2) private BigDecimal sellerChargeAmount;
    @Column(nullable = false, columnDefinition = "char(3)") @JdbcTypeCode(SqlTypes.CHAR) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private RefundStatus status;
    @Column(name = "idempotency_key", nullable = false, length = 128) private String idempotencyKey;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected RefundEntity() {}
    public static RefundEntity processing(RefundRequestEntity request, PaymentEntity payment, BigDecimal amount,
            BigDecimal sellerCharge, String key, Instant now) {
        RefundEntity refund = new RefundEntity(); refund.refundRequest = request; refund.payment = payment;
        refund.amount = amount; refund.sellerChargeAmount = sellerCharge; refund.currency = request.getCurrency();
        refund.status = RefundStatus.PROCESSING; refund.idempotencyKey = key; refund.createdAt = now; refund.updatedAt = now;
        return refund;
    }
    public UUID getId() { return id; }
    public UUID getRefundRequestId() { return refundRequest.getId(); }
    public UUID getPaymentId() { return payment.getId(); }
    public String getProviderReference() { return providerReference; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getSellerChargeAmount() { return sellerChargeAmount; }
    public String getCurrency() { return currency; }
    public RefundStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void succeed(String providerReference, Instant now) { this.providerReference = providerReference; status = RefundStatus.SUCCEEDED; completedAt = now; updatedAt = now; }
}
