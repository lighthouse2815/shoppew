package com.shoppew.order.repository;

import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {
    Page<OrderEntity> findAllByUser_Id(UUID userId, Pageable pageable);
    Page<OrderEntity> findAllByShop_Id(UUID shopId, Pageable pageable);
    Page<OrderEntity> findAllByShop_IdAndStatus(UUID shopId, OrderStatus status, Pageable pageable);
    Optional<OrderEntity> findByIdAndUser_Id(UUID id, UUID userId);
    Optional<OrderEntity> findByIdAndShop_Id(UUID id, UUID shopId);
    List<OrderEntity> findAllByCheckoutGroup_IdOrderByCreatedAtAsc(UUID checkoutId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select order from OrderEntity order where order.id = :id")
    Optional<OrderEntity> findLocked(@Param("id") UUID id);
}
