package com.shoppew.refund.entity;

import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.shop.entity.ShopEntity;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "refund_requests")
public class RefundRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "request_number", nullable = false, length = 40) private String requestNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private ShopEntity shop;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RefundReason reason;
    @Column(columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RefundRequestStatus status;
    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2) private BigDecimal requestedAmount;
    @Column(name = "approved_amount", precision = 19, scale = 2) private BigDecimal approvedAmount;
    @Column(nullable = false, columnDefinition = "char(3)") @JdbcTypeCode(SqlTypes.CHAR) private String currency;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private UserEntity reviewedBy;
    @Column(name = "review_note", length = 1000) private String reviewNote;
    @Enumerated(EnumType.STRING) @Column(name = "previous_order_status", nullable = false, length = 32) private OrderStatus previousOrderStatus;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected RefundRequestEntity() {}
    public static RefundRequestEntity create(String number, OrderEntity order, UserEntity user, RefundReason reason,
            String description, BigDecimal requestedAmount, Instant now) {
        RefundRequestEntity request = new RefundRequestEntity();
        request.requestNumber = number; request.order = order; request.user = user; request.shop = order.getShop();
        request.reason = reason; request.description = description; request.status = RefundRequestStatus.REQUESTED;
        request.requestedAmount = requestedAmount; request.currency = order.getCurrency();
        request.previousOrderStatus = order.getStatus(); request.createdAt = now; request.updatedAt = now;
        return request;
    }
    public UUID getId() { return id; }
    public String getRequestNumber() { return requestNumber; }
    public OrderEntity getOrder() { return order; }
    public UUID getOrderId() { return order.getId(); }
    public UUID getUserId() { return user.getId(); }
    public UUID getShopId() { return shop.getId(); }
    public RefundReason getReason() { return reason; }
    public String getDescription() { return description; }
    public RefundRequestStatus getStatus() { return status; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public String getCurrency() { return currency; }
    public UUID getReviewedById() { return reviewedBy == null ? null : reviewedBy.getId(); }
    public String getReviewNote() { return reviewNote; }
    public OrderStatus getPreviousOrderStatus() { return previousOrderStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void review(UserEntity actor, String note, Instant now) { status = RefundRequestStatus.UNDER_REVIEW; reviewedBy = actor; reviewNote = note; updatedAt = now; }
    public void approve(UserEntity actor, BigDecimal amount, String note, Instant now) { status = RefundRequestStatus.APPROVED; reviewedBy = actor; approvedAmount = amount; reviewNote = note; updatedAt = now; }
    public void reject(UserEntity actor, String note, Instant now) { status = RefundRequestStatus.REJECTED; reviewedBy = actor; reviewNote = note; updatedAt = now; }
    public void cancel(Instant now) { status = RefundRequestStatus.CANCELLED; updatedAt = now; }
    public void refunding(Instant now) { status = RefundRequestStatus.REFUNDING; updatedAt = now; }
    public void refunded(Instant now) { status = RefundRequestStatus.REFUNDED; updatedAt = now; }
}
