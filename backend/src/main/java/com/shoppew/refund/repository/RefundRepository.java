package com.shoppew.refund.repository;

import com.shoppew.refund.entity.RefundEntity;
import com.shoppew.refund.entity.RefundStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<RefundEntity, UUID> {
    Optional<RefundEntity> findByRefundRequest_Id(UUID requestId);
    Optional<RefundEntity> findByIdempotencyKey(String idempotencyKey);
    @Query("select coalesce(sum(refund.amount), 0) from RefundEntity refund where refund.refundRequest.order.id = :orderId and refund.status = :status")
    BigDecimal sumForOrder(@Param("orderId") UUID orderId, @Param("status") RefundStatus status);
}
