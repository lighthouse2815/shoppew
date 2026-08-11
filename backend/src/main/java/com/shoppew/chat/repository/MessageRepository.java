package com.shoppew.chat.repository;

import com.shoppew.chat.entity.MessageEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    @EntityGraph(attributePaths = {"sender", "product", "order"})
    Page<MessageEntity> findAllByConversation_Id(UUID conversationId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "product", "order"})
    Optional<MessageEntity> findFirstByConversation_IdOrderBySentAtDesc(UUID conversationId);
}
