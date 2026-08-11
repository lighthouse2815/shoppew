package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductOptionValueEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValueEntity, UUID> {

    List<ProductOptionValueEntity> findAllByOption_IdOrderBySortOrderAscValueAsc(UUID optionId);

    @EntityGraph(attributePaths = "option")
    List<ProductOptionValueEntity> findAllByIdIn(Collection<UUID> ids);

    boolean existsByOption_IdAndValueIgnoreCase(UUID optionId, String value);
}
