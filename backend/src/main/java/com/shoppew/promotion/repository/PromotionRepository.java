package com.shoppew.promotion.repository;

import com.shoppew.promotion.entity.PromotionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {
    List<PromotionEntity> findAllByShop_IdOrderByCreatedAtDesc(UUID shopId);
    List<PromotionEntity> findAllByShopIsNullOrderByCreatedAtDesc();
}
