package com.shoppew.inventory.repository;

import com.shoppew.inventory.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into inventories (variant_id, available_quantity, reserved_quantity, sold_quantity,
                                     low_stock_threshold, updated_at, version)
            values (:variantId, 0, 0, 0, 5, now(), 0)
            on conflict (variant_id) do nothing
            """, nativeQuery = true)
    int provision(@Param("variantId") UUID variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select inventory from InventoryEntity inventory
            join fetch inventory.variant variant
            join fetch variant.product
            join fetch variant.shop
            where inventory.variantId = :variantId
            """)
    Optional<InventoryEntity> findLocked(@Param("variantId") UUID variantId);

    @Query("""
            select inventory from InventoryEntity inventory
            join fetch inventory.variant variant
            join fetch variant.product
            join fetch variant.shop
            where inventory.variantId in :variantIds
            """)
    List<InventoryEntity> findAllDetailedByVariantIdIn(@Param("variantIds") Collection<UUID> variantIds);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update inventories
            set available_quantity = available_quantity - :quantity,
                reserved_quantity = reserved_quantity + :quantity,
                updated_at = now(),
                version = version + 1
            where variant_id = :variantId
              and available_quantity >= :quantity
            """, nativeQuery = true)
    int reserveAtomically(@Param("variantId") UUID variantId, @Param("quantity") long quantity);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update inventories
            set available_quantity = available_quantity + :quantity,
                reserved_quantity = reserved_quantity - :quantity,
                updated_at = now(),
                version = version + 1
            where variant_id = :variantId
              and reserved_quantity >= :quantity
            """, nativeQuery = true)
    int releaseAtomically(@Param("variantId") UUID variantId, @Param("quantity") long quantity);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update inventories
            set reserved_quantity = reserved_quantity - :quantity,
                sold_quantity = sold_quantity + :quantity,
                updated_at = now(),
                version = version + 1
            where variant_id = :variantId
              and reserved_quantity >= :quantity
            """, nativeQuery = true)
    int consumeAtomically(@Param("variantId") UUID variantId, @Param("quantity") long quantity);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update inventories
            set available_quantity = available_quantity + :quantity,
                sold_quantity = sold_quantity - :quantity,
                updated_at = now(),
                version = version + 1
            where variant_id = :variantId
              and sold_quantity >= :quantity
            """, nativeQuery = true)
    int returnSoldAtomically(@Param("variantId") UUID variantId, @Param("quantity") long quantity);
}
