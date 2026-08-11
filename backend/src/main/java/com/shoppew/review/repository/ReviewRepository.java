package com.shoppew.review.repository;

import com.shoppew.review.entity.ReviewEntity;
import com.shoppew.review.entity.ReviewStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    boolean existsByOrderItem_Id(UUID orderItemId);
    @EntityGraph(attributePaths = {"user", "product", "shop", "orderItem"})
    Page<ReviewEntity> findAllByProduct_IdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"user", "product", "shop", "orderItem"})
    Page<ReviewEntity> findAllByUser_Id(UUID userId, Pageable pageable);
    @EntityGraph(attributePaths = {"user", "product", "shop", "orderItem"})
    Page<ReviewEntity> findAllByShop_Id(UUID shopId, Pageable pageable);
    @EntityGraph(attributePaths = {"user", "product", "shop", "orderItem"})
    Optional<ReviewEntity> findByIdAndUser_Id(UUID id, UUID userId);
    @EntityGraph(attributePaths = {"user", "product", "shop", "orderItem"})
    Optional<ReviewEntity> findByIdAndShop_Id(UUID id, UUID shopId);

    @Modifying
    @Query(value = """
            update products set
              rating_average = coalesce((select round(avg(rating)::numeric, 2) from reviews where product_id = :productId and status = 'PUBLISHED'), 0),
              review_count = (select count(*) from reviews where product_id = :productId and status = 'PUBLISHED'),
              version = version + 1
            where id = :productId
            """, nativeQuery = true)
    int recomputeProduct(@Param("productId") UUID productId);

    @Modifying
    @Query(value = """
            update shops set
              rating_average = coalesce((select round(avg(rating)::numeric, 2) from reviews where shop_id = :shopId and status = 'PUBLISHED'), 0),
              review_count = (select count(*) from reviews where shop_id = :shopId and status = 'PUBLISHED'),
              version = version + 1
            where id = :shopId
            """, nativeQuery = true)
    int recomputeShop(@Param("shopId") UUID shopId);
}
