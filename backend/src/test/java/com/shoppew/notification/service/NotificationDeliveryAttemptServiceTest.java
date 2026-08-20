package com.shoppew.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.shoppew.notification.entity.NotificationDeliveryEntity;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Status;
import com.shoppew.notification.entity.NotificationEntity;
import com.shoppew.notification.entity.NotificationType;
import com.shoppew.notification.repository.NotificationDeliveryRepository;
import com.shoppew.notification.sender.EmailNotificationSender;
import com.shoppew.notification.sender.NotificationSendException;
import com.shoppew.notification.sender.NotificationSendResult;
import com.shoppew.notification.sender.PushNotificationSender;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDeliveryAttemptServiceTest {
    @Test
    void failedExternalDeliveryIsPersistedForRetryAndThenCompletes() {
        Instant firstAttempt = Instant.parse("2026-08-20T04:00:00Z");
        UserEntity user = UserEntity.register(
                "retry@example.test", null, "unused-hash", UserStatus.ACTIVE, firstAttempt);
        NotificationEntity notification = NotificationEntity.create(
                user, NotificationType.ORDER, "Đơn hàng", "Đã cập nhật", Map.of(), firstAttempt);
        NotificationDeliveryEntity delivery = NotificationDeliveryEntity.pending(notification, Channel.PUSH, firstAttempt);
        UUID deliveryId = UUID.randomUUID();
        NotificationDeliveryRepository repository = mock(NotificationDeliveryRepository.class);
        EmailNotificationSender email = mock(EmailNotificationSender.class);
        PushNotificationSender push = mock(PushNotificationSender.class);
        when(repository.findByIdForAttempt(deliveryId)).thenReturn(Optional.of(delivery));
        when(push.send(any())).thenThrow(new NotificationSendException("temporary outage", null));

        new NotificationDeliveryAttemptService(
                repository, email, push, Clock.fixed(firstAttempt, ZoneOffset.UTC)).attempt(deliveryId);

        assertThat(delivery.getStatus()).isEqualTo(Status.FAILED);
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
        assertThat(delivery.getNextAttemptAt()).isEqualTo(firstAttempt.plus(Duration.ofSeconds(30)));

        doReturn(NotificationSendResult.delivered("fcm:1/1")).when(push).send(any());
        Instant retryTime = firstAttempt.plusSeconds(31);
        new NotificationDeliveryAttemptService(
                repository, email, push, Clock.fixed(retryTime, ZoneOffset.UTC)).attempt(deliveryId);

        assertThat(delivery.getStatus()).isEqualTo(Status.DELIVERED);
        assertThat(delivery.getAttemptCount()).isEqualTo(2);
        assertThat(delivery.getNextAttemptAt()).isNull();
    }
}
