package com.shoppew.dispute.entity;

import com.shoppew.order.entity.OrderEntity;
import com.shoppew.refund.entity.RefundRequestEntity;
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
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "disputes")
public class DisputeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "dispute_number", nullable = false, length = 40) private String disputeNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "refund_request_id") private RefundRequestEntity refundRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "opened_by") private UserEntity openedBy;
    @Column(nullable = false, length = 80) private String reason;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private DisputeStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to") private UserEntity assignedTo;
    @Column(length = 1000) private String resolution;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected DisputeEntity() {}
    public static DisputeEntity create(String number, OrderEntity order, RefundRequestEntity refundRequest,
            UserEntity openedBy, String reason, String description, Instant now) {
        DisputeEntity dispute = new DisputeEntity(); dispute.disputeNumber = number; dispute.order = order;
        dispute.refundRequest = refundRequest; dispute.openedBy = openedBy; dispute.reason = reason;
        dispute.description = description; dispute.status = DisputeStatus.OPEN; dispute.createdAt = now; dispute.updatedAt = now;
        return dispute;
    }
    public UUID getId() { return id; }
    public String getDisputeNumber() { return disputeNumber; }
    public OrderEntity getOrder() { return order; }
    public UUID getOrderId() { return order.getId(); }
    public UUID getShopId() { return order.getShopId(); }
    public UUID getCustomerId() { return order.getUserId(); }
    public UUID getRefundRequestId() { return refundRequest == null ? null : refundRequest.getId(); }
    public UUID getOpenedById() { return openedBy.getId(); }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public DisputeStatus getStatus() { return status; }
    public UUID getAssignedToId() { return assignedTo == null ? null : assignedTo.getId(); }
    public String getResolution() { return resolution; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(DisputeStatus status, UserEntity assignee, String resolution, Instant now) {
        this.status = status; this.assignedTo = assignee; this.resolution = resolution; this.updatedAt = now;
        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.CLOSED) resolvedAt = now;
    }
}
