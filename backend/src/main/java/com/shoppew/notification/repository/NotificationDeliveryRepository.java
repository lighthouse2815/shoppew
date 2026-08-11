package com.shoppew.notification.repository;

import com.shoppew.notification.entity.NotificationDeliveryEntity;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, UUID> {
    @EntityGraph(attributePaths = {"notification", "notification.user"})
    Optional<NotificationDeliveryEntity> findByNotification_IdAndChannel(UUID notificationId, Channel channel);
}
