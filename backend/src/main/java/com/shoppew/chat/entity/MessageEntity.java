package com.shoppew.chat.entity;

import com.shoppew.order.entity.OrderEntity;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 24)
    private MessageType messageType;

    @Column(name = "text_content", columnDefinition = "text")
    private String textContent;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected MessageEntity() {}

    public static MessageEntity create(
            ConversationEntity conversation,
            UserEntity sender,
            MessageType type,
            String textContent,
            String mediaUrl,
            ProductEntity product,
            OrderEntity order,
            Instant now) {
        MessageEntity message = new MessageEntity();
        message.conversation = conversation;
        message.sender = sender;
        message.messageType = type;
        message.textContent = textContent;
        message.mediaUrl = mediaUrl;
        message.product = product;
        message.order = order;
        message.sentAt = now;
        return message;
    }

    public UUID getId() { return id; }
    public ConversationEntity getConversation() { return conversation; }
    public UserEntity getSender() { return sender; }
    public UUID getSenderId() { return sender.getId(); }
    public MessageType getMessageType() { return messageType; }
    public String getTextContent() { return textContent; }
    public String getMediaUrl() { return mediaUrl; }
    public ProductEntity getProduct() { return product; }
    public OrderEntity getOrder() { return order; }
    public Instant getSentAt() { return sentAt; }
}
