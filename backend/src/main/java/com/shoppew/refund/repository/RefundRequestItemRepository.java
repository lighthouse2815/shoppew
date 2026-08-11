package com.shoppew.refund.repository;

import com.shoppew.refund.entity.RefundRequestItemEntity;
import com.shoppew.refund.entity.RefundRequestItemId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRequestItemRepository extends JpaRepository<RefundRequestItemEntity, RefundRequestItemId> {
    List<RefundRequestItemEntity> findAllByRefundRequest_IdOrderByOrderItem_Id(UUID requestId);
    @Query("""
            select coalesce(sum(item.quantity), 0) from RefundRequestItemEntity item
            where item.orderItem.id = :orderItemId
              and item.refundRequest.status not in (com.shoppew.refund.entity.RefundRequestStatus.REJECTED,
                  com.shoppew.refund.entity.RefundRequestStatus.CANCELLED)
            """)
    long allocatedQuantity(@Param("orderItemId") UUID orderItemId);
    @Query("select coalesce(sum(item.sellerChargeAmount), 0) from RefundRequestItemEntity item where item.refundRequest.id = :requestId")
    java.math.BigDecimal sellerChargeTotal(@Param("requestId") UUID requestId);
}
