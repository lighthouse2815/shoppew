package com.shoppew.wishlist.repository;

import com.shoppew.wishlist.entity.WishlistEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<WishlistEntity, UUID> {
    @EntityGraph(attributePaths = {"product", "product.shop", "product.category", "product.brand"})
    List<WishlistEntity> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
    Optional<WishlistEntity> findByUser_IdAndProduct_Id(UUID userId, UUID productId);
    void deleteByUser_IdAndProduct_Id(UUID userId, UUID productId);
}
