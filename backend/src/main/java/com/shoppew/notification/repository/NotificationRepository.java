package com.shoppew.notification.repository;

import com.shoppew.notification.entity.NotificationEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    Page<NotificationEntity> findAllByUser_Id(UUID userId, Pageable pageable);
    Optional<NotificationEntity> findByIdAndUser_Id(UUID id, UUID userId);
    long countByUser_IdAndReadAtIsNull(UUID userId);
    @Modifying
    @Query("update NotificationEntity notification set notification.readAt = :now where notification.user.id = :userId and notification.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
