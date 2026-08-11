package com.shoppew.product.repository;

import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.entity.VariantStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, UUID> {

    @Query("""
            select variant from ProductVariantEntity variant
            join fetch variant.product product
            join fetch variant.shop
            where variant.id = :variantId
            """)
    Optional<ProductVariantEntity> findForInventory(@Param("variantId") UUID variantId);

    @Query(
            value = """
                    select variant from ProductVariantEntity variant
                    join fetch variant.product product
                    join fetch variant.shop
                    where variant.shop.id = :shopId
                      and (:lowStock = false or exists (
                            select inventory.variantId from InventoryEntity inventory
                            where inventory.variantId = variant.id
                              and inventory.availableQuantity <= inventory.lowStockThreshold))
                      and (lower(variant.sku) like lower(concat('%', :query, '%'))
                           or lower(variant.name) like lower(concat('%', :query, '%'))
                           or lower(product.name) like lower(concat('%', :query, '%')))
                    """,
            countQuery = """
                    select count(variant) from ProductVariantEntity variant
                    join variant.product product
                    where variant.shop.id = :shopId
                      and (:lowStock = false or exists (
                            select inventory.variantId from InventoryEntity inventory
                            where inventory.variantId = variant.id
                              and inventory.availableQuantity <= inventory.lowStockThreshold))
                      and (lower(variant.sku) like lower(concat('%', :query, '%'))
                           or lower(variant.name) like lower(concat('%', :query, '%'))
                           or lower(product.name) like lower(concat('%', :query, '%')))
                    """)
    Page<ProductVariantEntity> searchForInventory(
            @Param("shopId") UUID shopId,
            @Param("query") String query,
            @Param("lowStock") boolean lowStock,
            Pageable pageable);

    @Query(
            value = """
                    select variant from ProductVariantEntity variant
                    join fetch variant.product
                    join fetch variant.shop
                    where variant.shop.id = :shopId
                      and (:lowStock = false or exists (
                            select inventory.variantId from InventoryEntity inventory
                            where inventory.variantId = variant.id
                              and inventory.availableQuantity <= inventory.lowStockThreshold))
                    """,
            countQuery = """
                    select count(variant) from ProductVariantEntity variant
                    where variant.shop.id = :shopId
                      and (:lowStock = false or exists (
                            select inventory.variantId from InventoryEntity inventory
                            where inventory.variantId = variant.id
                              and inventory.availableQuantity <= inventory.lowStockThreshold))
                    """)
    Page<ProductVariantEntity> listForInventory(
            @Param("shopId") UUID shopId,
            @Param("lowStock") boolean lowStock,
            Pageable pageable);

    @Query("""
            select distinct variant from ProductVariantEntity variant
            left join fetch variant.optionValues value
            left join fetch value.option
            where variant.product.id = :productId
            order by variant.createdAt asc
            """)
    List<ProductVariantEntity> findAllDetailedByProductId(@Param("productId") UUID productId);

    @Query("""
            select distinct variant from ProductVariantEntity variant
            left join fetch variant.optionValues value
            left join fetch value.option
            where variant.product.id in :productIds
            """)
    List<ProductVariantEntity> findAllDetailedByProductIdIn(@Param("productIds") Collection<UUID> productIds);

    @Query("""
            select distinct variant from ProductVariantEntity variant
            left join fetch variant.optionValues value
            left join fetch value.option
            where variant.id = :id and variant.product.id = :productId
            """)
    Optional<ProductVariantEntity> findOwned(@Param("id") UUID id, @Param("productId") UUID productId);

    boolean existsByShop_IdAndSkuIgnoreCase(UUID shopId, String sku);

    boolean existsByShop_IdAndSkuIgnoreCaseAndIdNot(UUID shopId, String sku, UUID id);

    boolean existsByProduct_IdAndStatus(UUID productId, VariantStatus status);

    long countByProduct_Id(UUID productId);

    boolean existsByOptionValues_Id(UUID valueId);

    boolean existsByOptionValues_Option_Id(UUID optionId);
}
