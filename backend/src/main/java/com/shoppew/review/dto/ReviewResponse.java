package com.shoppew.review.dto;

import com.shoppew.review.entity.ReviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
        UUID id, UUID userId, String reviewerName, String reviewerAvatarUrl,
        UUID shopId, UUID productId, UUID orderItemId, int rating, String content,
        ReviewStatus status, String sellerReply, Instant sellerRepliedAt,
        List<Image> images, Instant createdAt, Instant updatedAt) {
    public record Image(UUID id, String url, int sortOrder) {}
}
