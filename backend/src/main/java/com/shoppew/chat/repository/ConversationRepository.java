package com.shoppew.chat.repository;

import com.shoppew.chat.entity.ConversationEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @EntityGraph(attributePaths = {"shop", "customer"})
    Optional<ConversationEntity> findByShop_IdAndCustomer_Id(UUID shopId, UUID customerId);

    @EntityGraph(attributePaths = {"shop", "customer"})
    Page<ConversationEntity> findAllByCustomer_Id(UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "customer"})
    Page<ConversationEntity> findAllByShop_Id(UUID shopId, Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "customer"})
    @Query("select conversation from ConversationEntity conversation where conversation.id = :id")
    Optional<ConversationEntity> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"shop", "customer"})
    @Query("select conversation from ConversationEntity conversation where conversation.id = :id")
    Optional<ConversationEntity> findLockedById(@Param("id") UUID id);
}
