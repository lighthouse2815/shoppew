package com.shoppew.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_images")
public class ReviewImageEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "review_id") private ReviewEntity review;
    @Column(name = "object_key", nullable = false, length = 700) private String objectKey;
    @Column(nullable = false, length = 1000) private String url;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected ReviewImageEntity() {}
    public static ReviewImageEntity create(ReviewEntity review, String objectKey, String url, int sortOrder, Instant now) {
        ReviewImageEntity image = new ReviewImageEntity(); image.review = review; image.objectKey = objectKey;
        image.url = url; image.sortOrder = sortOrder; image.createdAt = now; return image;
    }
    public UUID getId() { return id; }
    public String getObjectKey() { return objectKey; }
    public String getUrl() { return url; }
    public int getSortOrder() { return sortOrder; }
}
