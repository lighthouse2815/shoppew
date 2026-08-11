package com.shoppew.promotion.repository;

import com.shoppew.promotion.entity.PromotionProductEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionProductRepository extends JpaRepository<PromotionProductEntity, UUID> {
    @Query("""
            select target from PromotionProductEntity target
            join fetch target.promotion promotion
            left join fetch promotion.shop
            join fetch target.product
            left join fetch target.variant
            where target.product.id in :productIds
              and promotion.status in ('ACTIVE', 'SCHEDULED')
              and promotion.startsAt <= :now and promotion.endsAt > :now
            """)
    List<PromotionProductEntity> findApplicable(
            @Param("productIds") Collection<UUID> productIds, @Param("now") Instant now);

    List<PromotionProductEntity> findAllByPromotion_IdOrderById(UUID promotionId);

    void deleteAllByPromotion_Id(UUID promotionId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PromotionProductEntity target
            set target.soldQuantity = target.soldQuantity + :quantity
            where target.id = :id
              and (target.quantityLimit is null or target.soldQuantity + :quantity <= target.quantityLimit)
            """)
    int reserveCapacity(@Param("id") UUID id, @Param("quantity") long quantity);

    @Modifying(flushAutomatically = true)
    @Query("""
            update PromotionProductEntity target
            set target.soldQuantity = target.soldQuantity - :quantity
            where target.id = :id and target.soldQuantity >= :quantity
            """)
    int releaseCapacity(@Param("id") UUID id, @Param("quantity") long quantity);
}
