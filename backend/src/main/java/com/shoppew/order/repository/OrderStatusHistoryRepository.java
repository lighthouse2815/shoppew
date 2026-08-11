package com.shoppew.order.repository;

import com.shoppew.order.entity.OrderStatusHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, UUID> {
    List<OrderStatusHistoryEntity> findAllByOrder_IdOrderByCreatedAtAsc(UUID orderId);
}
