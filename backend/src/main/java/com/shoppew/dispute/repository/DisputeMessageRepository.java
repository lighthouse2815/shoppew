package com.shoppew.dispute.repository;

import com.shoppew.dispute.entity.DisputeMessageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeMessageRepository extends JpaRepository<DisputeMessageEntity, UUID> {
    List<DisputeMessageEntity> findAllByDispute_IdOrderByCreatedAtAsc(UUID disputeId);
}
