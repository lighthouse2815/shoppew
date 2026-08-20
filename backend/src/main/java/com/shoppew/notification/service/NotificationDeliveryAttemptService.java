package com.shoppew.notification.service;

import com.shoppew.notification.entity.NotificationDeliveryEntity;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Status;
import com.shoppew.notification.repository.NotificationDeliveryRepository;
import com.shoppew.notification.sender.EmailNotificationSender;
import com.shoppew.notification.sender.NotificationMessage;
import com.shoppew.notification.sender.NotificationSendResult;
import com.shoppew.notification.sender.PushNotificationSender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(30);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(15);

    private final NotificationDeliveryRepository deliveries;
    private final EmailNotificationSender emailSender;
    private final PushNotificationSender pushSender;
    private final Clock clock;

    public NotificationDeliveryAttemptService(
            NotificationDeliveryRepository deliveries,
            EmailNotificationSender emailSender,
            PushNotificationSender pushSender,
            Clock clock) {
        this.deliveries = deliveries;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attempt(UUID deliveryId) {
        NotificationDeliveryEntity delivery = deliveries.findByIdForAttempt(deliveryId).orElse(null);
        Instant now = Instant.now(clock);
        if (delivery == null || !retryable(delivery, now)) return;

        delivery.beginAttempt(now);
        var notification = delivery.getNotification();
        NotificationMessage message = new NotificationMessage(
                notification.getUserId(),
                notification.getUserEmail(),
                notification.getTitle(),
                notification.getBody(),
                notification.getData());
        try {
            NotificationSendResult result = delivery.getChannel() == Channel.EMAIL
                    ? emailSender.send(message)
                    : pushSender.send(message);
            if (result.status() == NotificationSendResult.Status.DELIVERED) {
                delivery.delivered(result.providerReference(), now);
            } else {
                delivery.skipped(result.message(), now);
            }
        } catch (RuntimeException exception) {
            Instant nextAttempt = delivery.getAttemptCount() >= MAX_ATTEMPTS
                    ? null
                    : now.plus(retryDelay(delivery.getAttemptCount()));
            String detail = exception.getClass().getSimpleName()
                    + (exception.getMessage() == null ? "" : ": " + exception.getMessage());
            delivery.failed(detail, now, nextAttempt);
        }
    }

    private boolean retryable(NotificationDeliveryEntity delivery, Instant now) {
        if (delivery.getChannel() == Channel.IN_APP || delivery.getAttemptCount() >= MAX_ATTEMPTS) return false;
        if (delivery.getStatus() == Status.PENDING) return true;
        return delivery.getStatus() == Status.FAILED
                && delivery.getNextAttemptAt() != null
                && !delivery.getNextAttemptAt().isAfter(now);
    }

    private Duration retryDelay(int attemptCount) {
        long multiplier = 1L << Math.max(0, attemptCount - 1);
        Duration delay = INITIAL_RETRY_DELAY.multipliedBy(multiplier);
        return delay.compareTo(MAX_RETRY_DELAY) > 0 ? MAX_RETRY_DELAY : delay;
    }
}
