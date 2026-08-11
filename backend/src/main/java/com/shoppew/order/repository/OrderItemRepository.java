package com.shoppew.order.repository;

import com.shoppew.order.entity.OrderItemEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {
    List<OrderItemEntity> findAllByOrder_IdOrderByCreatedAtAsc(UUID orderId);
    List<OrderItemEntity> findAllByOrder_IdIn(Collection<UUID> orderIds);
    long countByOrder_Id(UUID orderId);

    @Query("""
            select item from OrderItemEntity item
            join fetch item.order orders
            left join fetch item.product
            left join fetch item.variant
            where item.id = :itemId and orders.user.id = :userId
            """)
    java.util.Optional<OrderItemEntity> findReviewable(
            @Param("itemId") UUID itemId, @Param("userId") UUID userId);
}
