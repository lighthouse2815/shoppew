package com.shoppew.notification.service;

import com.shoppew.notification.repository.NotificationDeliveryRepository;
import java.time.Instant;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationChannelDispatcher {

    private final NotificationDeliveryRepository deliveries;
    private final NotificationDeliveryAttemptService attempts;
    private final Clock clock;

    public NotificationChannelDispatcher(
            NotificationDeliveryRepository deliveries,
            NotificationDeliveryAttemptService attempts,
            Clock clock) {
        this.deliveries = deliveries;
        this.attempts = attempts;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(NotificationDispatchRequestedEvent event) {
        deliveries.findByNotification_IdAndChannel(event.notificationId(), event.channel())
                .map(com.shoppew.notification.entity.NotificationDeliveryEntity::getId)
                .ifPresent(attempts::attempt);
    }

    @Scheduled(fixedDelayString = "${app.notification-delivery-retry-scan-delay:PT30S}")
    public void retryFailedDeliveries() {
        deliveries.findRetryableIds(Instant.now(clock), PageRequest.of(0, 100))
                .forEach(attempts::attempt);
    }
}
