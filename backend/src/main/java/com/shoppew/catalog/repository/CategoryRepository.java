package com.shoppew.catalog.repository;

import com.shoppew.catalog.entity.CatalogStatus;
import com.shoppew.catalog.entity.CategoryEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<CategoryEntity> findBySlug(String slug);

    List<CategoryEntity> findAllByOrderBySortOrderAscNameAsc();

    List<CategoryEntity> findAllByStatusOrderBySortOrderAscNameAsc(CatalogStatus status);
}
