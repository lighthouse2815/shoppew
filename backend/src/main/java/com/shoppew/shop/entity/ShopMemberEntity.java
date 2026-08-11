package com.shoppew.shop.entity;

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
@Table(name = "shop_members")
public class ShopMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 32)
    private ShopMemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ShopMemberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopMemberEntity() {}

    public static ShopMemberEntity owner(ShopEntity shop, UserEntity user, Instant now) {
        ShopMemberEntity member = new ShopMemberEntity();
        member.shop = shop;
        member.user = user;
        member.memberRole = ShopMemberRole.OWNER;
        member.status = ShopMemberStatus.ACTIVE;
        member.createdAt = now;
        member.updatedAt = now;
        return member;
    }

    public UUID getId() {
        return id;
    }

    public ShopEntity getShop() {
        return shop;
    }

    public UUID getUserId() {
        return user.getId();
    }

    public ShopMemberRole getMemberRole() {
        return memberRole;
    }

    public ShopMemberStatus getStatus() {
        return status;
    }
}
