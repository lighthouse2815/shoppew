package com.shoppew.notification.sender;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.shoppew.notification.entity.PushDeviceEntity;
import com.shoppew.notification.repository.PushDeviceRepository;
import com.shoppew.notification.service.PushTargetCodec;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.push", name = "delivery-enabled", havingValue = "true")
public class FcmPushNotificationSender implements PushNotificationSender {
    private final FirebaseMessaging messaging;
    private final PushDeviceRepository devices;
    private final PushTargetCodec codec;
    private final Clock clock;

    public FcmPushNotificationSender(
            FirebaseMessaging messaging,
            PushDeviceRepository devices,
            PushTargetCodec codec,
            Clock clock) {
        this.messaging = messaging;
        this.devices = devices;
        this.codec = codec;
        this.clock = clock;
    }

    @Override
    public NotificationSendResult send(NotificationMessage notification) {
        List<PushDeviceEntity> targets = devices
                .findAllByUser_IdAndActiveTrueOrderByLastSeenAtDesc(notification.userId());
        if (targets.isEmpty()) return NotificationSendResult.skipped("No active push device registered");

        Map<String, String> data = new LinkedHashMap<>();
        notification.data().forEach((key, value) -> data.put(key, String.valueOf(value)));
        data.put("title", notification.title());
        data.put("body", notification.body());

        List<Message> messages = targets.stream().map(device -> targetMessage(device)
                        .putAllData(data)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .build())
                .toList();
        try {
            var response = messaging.sendEach(messages);
            List<String> failures = new ArrayList<>();
            Instant now = Instant.now(clock);
            for (int index = 0; index < response.getResponses().size(); index++) {
                var item = response.getResponses().get(index);
                if (item.isSuccessful()) continue;
                FirebaseMessagingException exception = item.getException();
                if (exception != null && exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    targets.get(index).deactivate(now);
                } else {
                    failures.add(exception == null ? "UNKNOWN" : String.valueOf(exception.getMessagingErrorCode()));
                }
            }
            if (response.getSuccessCount() == 0) {
                throw new NotificationSendException(
                        "FCM rejected every active target: " + String.join(",", failures), null);
            }
            return NotificationSendResult.delivered(
                    "fcm:" + response.getSuccessCount() + "/" + targets.size());
        } catch (FirebaseMessagingException exception) {
            throw new NotificationSendException("FCM request failed: " + exception.getMessagingErrorCode(), exception);
        }
    }

    private Message.Builder targetMessage(PushDeviceEntity device) {
        String target = codec.decrypt(device.getEncryptedTarget(), device.getTargetHash());
        Message.Builder builder = Message.builder();
        return device.getTargetType() == PushDeviceEntity.TargetType.FID
                ? builder.setFid(target)
                : builder.setToken(target);
    }
}
