package com.shoppew.review.repository;

import com.shoppew.review.entity.ReviewImageEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImageEntity, UUID> {
    List<ReviewImageEntity> findAllByReview_IdOrderBySortOrderAsc(UUID reviewId);
    Optional<ReviewImageEntity> findByIdAndReview_Id(UUID id, UUID reviewId);
    long countByReview_Id(UUID reviewId);
}
