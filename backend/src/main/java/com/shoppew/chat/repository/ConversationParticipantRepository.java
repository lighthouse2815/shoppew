package com.shoppew.chat.repository;

import com.shoppew.chat.entity.ConversationParticipantEntity;
import com.shoppew.chat.entity.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipantEntity, ConversationParticipantId> {}
