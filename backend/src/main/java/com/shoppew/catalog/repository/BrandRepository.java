package com.shoppew.catalog.repository;

import com.shoppew.catalog.entity.BrandEntity;
import com.shoppew.catalog.entity.CatalogStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<BrandEntity, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<BrandEntity> findBySlug(String slug);

    List<BrandEntity> findAllByOrderByNameAsc();

    List<BrandEntity> findAllByStatusOrderByNameAsc(CatalogStatus status);
}
