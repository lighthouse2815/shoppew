package com.shoppew.refund.repository;

import com.shoppew.refund.entity.RefundRequestEntity;
import com.shoppew.refund.entity.RefundRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRequestRepository extends JpaRepository<RefundRequestEntity, UUID> {
    Page<RefundRequestEntity> findAllByUser_Id(UUID userId, Pageable pageable);
    Page<RefundRequestEntity> findAllByShop_Id(UUID shopId, Pageable pageable);
    Page<RefundRequestEntity> findAllByShop_IdAndStatus(UUID shopId, RefundRequestStatus status, Pageable pageable);
    Page<RefundRequestEntity> findAllByStatus(RefundRequestStatus status, Pageable pageable);
    Optional<RefundRequestEntity> findByIdAndUser_Id(UUID id, UUID userId);
    Optional<RefundRequestEntity> findByIdAndShop_Id(UUID id, UUID shopId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select request from RefundRequestEntity request where request.id = :id")
    Optional<RefundRequestEntity> findLocked(@Param("id") UUID id);
}
