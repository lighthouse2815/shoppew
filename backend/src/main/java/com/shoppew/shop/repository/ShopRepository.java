package com.shoppew.shop.repository;

import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.entity.ShopStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShopRepository extends JpaRepository<ShopEntity, UUID>, JpaSpecificationExecutor<ShopEntity> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    List<ShopEntity> findAllByOwner_IdOrderByCreatedAtDesc(UUID ownerId);

    List<ShopEntity> findAllByOwner_IdInOrderByCreatedAtDesc(Collection<UUID> ownerIds);

    Optional<ShopEntity> findBySlugAndStatus(String slug, ShopStatus status);
}
