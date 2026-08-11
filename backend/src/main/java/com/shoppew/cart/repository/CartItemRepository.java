package com.shoppew.cart.repository;

import com.shoppew.cart.entity.CartItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    Optional<CartItemEntity> findByCart_IdAndVariant_Id(UUID cartId, UUID variantId);

    Optional<CartItemEntity> findByIdAndCart_Id(UUID id, UUID cartId);

    @Query("""
            select distinct item from CartItemEntity item
            join fetch item.shop
            join fetch item.product
            join fetch item.variant variant
            left join fetch variant.optionValues value
            left join fetch value.option
            where item.cart.id = :cartId
            order by item.createdAt asc
            """)
    List<CartItemEntity> findAllDetailedByCartId(@Param("cartId") UUID cartId);

    @Modifying
    void deleteAllByCart_Id(UUID cartId);
}
