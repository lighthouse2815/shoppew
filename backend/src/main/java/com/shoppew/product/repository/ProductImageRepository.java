package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductImageEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {

    List<ProductImageEntity> findAllByProduct_IdOrderBySortOrderAscCreatedAtAsc(UUID productId);

    List<ProductImageEntity> findAllByProduct_IdInOrderBySortOrderAsc(Collection<UUID> productIds);

    Optional<ProductImageEntity> findByIdAndProduct_Id(UUID id, UUID productId);

    long countByProduct_Id(UUID productId);

    boolean existsByProduct_Id(UUID productId);

    @Modifying
    @Query("update ProductImageEntity image set image.primary = false where image.product.id = :productId and image.primary = true")
    int clearPrimary(@Param("productId") UUID productId);
}
