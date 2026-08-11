package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    @Query("select product from ProductEntity product where product.id = :id and product.shop.id = :shopId")
    Optional<ProductEntity> findOwned(@Param("id") UUID id, @Param("shopId") UUID shopId);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    @Query("select product from ProductEntity product where product.slug = :slug and product.status = 'ACTIVE' and product.shop.status = 'ACTIVE'")
    Optional<ProductEntity> findPublicBySlug(@Param("slug") String slug);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    @Query("""
            select product from ProductEntity product
            where product.shop.id = :shopId
              and (:status is null or product.status = :status)
            """)
    Page<ProductEntity> findSellerProducts(
            @Param("shopId") UUID shopId,
            @Param("status") ProductStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    @Query("""
            select product from ProductEntity product
            where product.status = 'ACTIVE'
              and product.shop.status = 'ACTIVE'
              and (:shopId is null or product.shop.id = :shopId)
              and (:categoryId is null or product.category.id = :categoryId)
              and (:brandId is null or product.brand.id = :brandId)
              and (:query = '' or lower(product.name) like lower(concat('%', :query, '%')))
            """)
    Page<ProductEntity> searchPublic(
            @Param("query") String query,
            @Param("shopId") UUID shopId,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") UUID brandId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    Page<ProductEntity> findAllByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    @Query("select product from ProductEntity product where product.id = :id")
    Optional<ProductEntity> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"shop", "category", "brand"})
    @Query("select product from ProductEntity product where product.id in :ids")
    List<ProductEntity> findAllDetailedByIdIn(@Param("ids") Collection<UUID> ids);
}
