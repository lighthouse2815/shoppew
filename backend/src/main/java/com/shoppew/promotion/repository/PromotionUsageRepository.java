package com.shoppew.promotion.repository;

import com.shoppew.promotion.entity.PromotionUsageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsageEntity, UUID> {
    @Query("select usage from PromotionUsageEntity usage join fetch usage.promotionProduct where usage.checkoutGroup.id = :checkoutId")
    List<PromotionUsageEntity> findAllByCheckout(@Param("checkoutId") UUID checkoutId);
    @Query("select usage from PromotionUsageEntity usage join fetch usage.promotionProduct where usage.order.id = :orderId")
    List<PromotionUsageEntity> findAllByOrder(@Param("orderId") UUID orderId);
}
