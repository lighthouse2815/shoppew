package com.shoppew.notification.service;

import com.shoppew.notification.entity.NotificationDeliveryEntity;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import com.shoppew.notification.entity.NotificationEntity;
import com.shoppew.notification.repository.NotificationDeliveryRepository;
import com.shoppew.notification.sender.EmailNotificationSender;
import com.shoppew.notification.sender.NotificationMessage;
import com.shoppew.notification.sender.NotificationSendResult;
import com.shoppew.notification.sender.PushNotificationSender;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationChannelDispatcher {

    private final NotificationDeliveryRepository deliveries;
    private final EmailNotificationSender emailSender;
    private final PushNotificationSender pushSender;
    private final Clock clock;

    public NotificationChannelDispatcher(
            NotificationDeliveryRepository deliveries,
            EmailNotificationSender emailSender,
            PushNotificationSender pushSender,
            Clock clock) {
        this.deliveries = deliveries;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationDispatchRequestedEvent event) {
        NotificationDeliveryEntity delivery = deliveries
                .findByNotification_IdAndChannel(event.notificationId(), event.channel())
                .orElse(null);
        if (delivery == null || delivery.getStatus() != NotificationDeliveryEntity.Status.PENDING) {
            return;
        }
        NotificationEntity notification = delivery.getNotification();
        NotificationMessage message = new NotificationMessage(
                notification.getUserId(),
                notification.getUserEmail(),
                notification.getTitle(),
                notification.getBody(),
                notification.getData());
        Instant now = Instant.now(clock);
        try {
            NotificationSendResult result = event.channel() == Channel.EMAIL
                    ? emailSender.send(message)
                    : pushSender.send(message);
            if (result.status() == NotificationSendResult.Status.DELIVERED) {
                delivery.delivered(result.providerReference(), now);
            } else {
                delivery.skipped(result.message(), now);
            }
        } catch (RuntimeException exception) {
            delivery.failed(exception.getClass().getSimpleName() + ": " + exception.getMessage(), now);
        }
    }
}
