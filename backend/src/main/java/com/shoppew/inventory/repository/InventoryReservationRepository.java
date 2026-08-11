package com.shoppew.inventory.repository;

import com.shoppew.inventory.entity.InventoryReservationEntity;
import com.shoppew.inventory.entity.InventoryReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from InventoryReservationEntity reservation where reservation.id = :id")
    Optional<InventoryReservationEntity> findLocked(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<InventoryReservationEntity> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            InventoryReservationStatus status,
            Instant expiresAt);

    long countByVariant_IdAndStatus(UUID variantId, InventoryReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation from InventoryReservationEntity reservation
            where reservation.orderId = :orderId
            order by reservation.createdAt asc
            """)
    List<InventoryReservationEntity> findAllLockedByOrderId(@Param("orderId") UUID orderId);
}
