package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductAttributeValueEntity;
import com.shoppew.product.entity.ProductAttributeValueId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAttributeValueRepository
        extends JpaRepository<ProductAttributeValueEntity, ProductAttributeValueId> {

    @EntityGraph(attributePaths = "attribute")
    List<ProductAttributeValueEntity> findAllByProduct_Id(UUID productId);

    Optional<ProductAttributeValueEntity> findByProduct_IdAndAttribute_Id(UUID productId, UUID attributeId);

    void deleteAllByProduct_Id(UUID productId);
}
