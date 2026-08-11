package com.shoppew.dispute.repository;

import com.shoppew.dispute.entity.DisputeEntity;
import com.shoppew.dispute.entity.DisputeStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisputeRepository extends JpaRepository<DisputeEntity, UUID> {
    Page<DisputeEntity> findAllByOrder_User_Id(UUID userId, Pageable pageable);
    Page<DisputeEntity> findAllByOrder_Shop_Id(UUID shopId, Pageable pageable);
    Page<DisputeEntity> findAllByStatus(DisputeStatus status, Pageable pageable);
    Optional<DisputeEntity> findByIdAndOrder_User_Id(UUID id, UUID userId);
    Optional<DisputeEntity> findByIdAndOrder_Shop_Id(UUID id, UUID shopId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select dispute from DisputeEntity dispute where dispute.id = :id")
    Optional<DisputeEntity> findLocked(@Param("id") UUID id);
}
