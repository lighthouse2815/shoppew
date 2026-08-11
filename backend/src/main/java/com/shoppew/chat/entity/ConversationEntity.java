package com.shoppew.chat.entity;

import com.shoppew.shop.entity.ShopEntity;
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
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ConversationStatus status;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {}

    public static ConversationEntity create(ShopEntity shop, UserEntity customer, Instant now) {
        ConversationEntity conversation = new ConversationEntity();
        conversation.shop = shop;
        conversation.customer = customer;
        conversation.status = ConversationStatus.ACTIVE;
        conversation.createdAt = now;
        conversation.updatedAt = now;
        return conversation;
    }

    public UUID getId() { return id; }
    public ShopEntity getShop() { return shop; }
    public UUID getShopId() { return shop.getId(); }
    public UserEntity getCustomer() { return customer; }
    public UUID getCustomerId() { return customer.getId(); }
    public ConversationStatus getStatus() { return status; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void recordMessage(Instant now) {
        lastMessageAt = now;
        updatedAt = now;
    }
}
