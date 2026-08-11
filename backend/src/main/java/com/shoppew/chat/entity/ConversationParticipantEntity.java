package com.shoppew.chat.entity;

import com.shoppew.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "conversation_participants")
public class ConversationParticipantEntity {

    @EmbeddedId
    private ConversationParticipantId id;

    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 24)
    private ParticipantType participantType;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    protected ConversationParticipantEntity() {}

    public static ConversationParticipantEntity join(
            ConversationEntity conversation,
            UserEntity user,
            ParticipantType participantType,
            Instant now) {
        ConversationParticipantEntity participant = new ConversationParticipantEntity();
        participant.id = new ConversationParticipantId(conversation.getId(), user.getId());
        participant.conversation = conversation;
        participant.user = user;
        participant.participantType = participantType;
        participant.joinedAt = now;
        return participant;
    }

    public Instant getLastReadAt() { return lastReadAt; }

    public void markRead(Instant now) {
        if (lastReadAt == null || lastReadAt.isBefore(now)) {
            lastReadAt = now;
        }
    }
}
