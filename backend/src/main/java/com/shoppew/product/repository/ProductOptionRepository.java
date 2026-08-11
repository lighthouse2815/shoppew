package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductOptionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOptionEntity, UUID> {

    List<ProductOptionEntity> findAllByProduct_IdOrderBySortOrderAscNameAsc(UUID productId);

    Optional<ProductOptionEntity> findByIdAndProduct_Id(UUID id, UUID productId);

    long countByProduct_Id(UUID productId);

    boolean existsByProduct_IdAndNameIgnoreCase(UUID productId, String name);
}
