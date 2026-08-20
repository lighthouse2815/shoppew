package com.shoppew.notification.repository;

import com.shoppew.notification.entity.NotificationDeliveryEntity;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, UUID> {
    @EntityGraph(attributePaths = {"notification", "notification.user"})
    Optional<NotificationDeliveryEntity> findByNotification_IdAndChannel(UUID notificationId, Channel channel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"notification", "notification.user"})
    @Query("select delivery from NotificationDeliveryEntity delivery where delivery.id = :id")
    Optional<NotificationDeliveryEntity> findByIdForAttempt(UUID id);

    @Query("""
            select delivery.id from NotificationDeliveryEntity delivery
            where delivery.status = com.shoppew.notification.entity.NotificationDeliveryEntity.Status.PENDING
               or (delivery.status = com.shoppew.notification.entity.NotificationDeliveryEntity.Status.FAILED
                   and delivery.nextAttemptAt is not null and delivery.nextAttemptAt <= :now)
            order by delivery.createdAt
            """)
    List<UUID> findRetryableIds(Instant now, Pageable pageable);
}
