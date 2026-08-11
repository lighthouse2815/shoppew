package com.shoppew.shop.repository;

import com.shoppew.shop.entity.ShopMemberEntity;
import com.shoppew.shop.entity.ShopMemberStatus;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopMemberRepository extends JpaRepository<ShopMemberEntity, UUID> {

    @Query("""
            select member from ShopMemberEntity member
            join fetch member.shop
            where member.shop.id = :shopId
              and member.user.id = :userId
              and member.status = :status
            """)
    Optional<ShopMemberEntity> findByShopIdAndUserIdAndStatus(
            @Param("shopId") UUID shopId,
            @Param("userId") UUID userId,
            @Param("status") ShopMemberStatus status);

    List<ShopMemberEntity> findAllByShop_IdAndStatus(UUID shopId, ShopMemberStatus status);
}
