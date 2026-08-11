package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductAttributeEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductAttributeRepository extends JpaRepository<ProductAttributeEntity, UUID> {

    @Query("""
            select attribute from ProductAttributeEntity attribute
            where attribute.category is null or attribute.category.id = :categoryId
            order by attribute.sortOrder asc, attribute.name asc
            """)
    List<ProductAttributeEntity> findApplicable(@Param("categoryId") UUID categoryId);

    List<ProductAttributeEntity> findAllByIdIn(Collection<UUID> ids);

    @Query("""
            select (count(attribute) > 0) from ProductAttributeEntity attribute
            where lower(attribute.name) = lower(:name)
              and ((:categoryId is null and attribute.category is null) or attribute.category.id = :categoryId)
              and (:excludedId is null or attribute.id <> :excludedId)
            """)
    boolean existsDuplicate(
            @Param("categoryId") UUID categoryId,
            @Param("name") String name,
            @Param("excludedId") UUID excludedId);
}
