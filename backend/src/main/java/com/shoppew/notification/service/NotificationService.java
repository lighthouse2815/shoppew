package com.shoppew.notification.service;

import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.notification.dto.NotificationResponse;
import com.shoppew.notification.entity.NotificationDeliveryEntity;
import com.shoppew.notification.entity.NotificationEntity;
import com.shoppew.notification.entity.NotificationType;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import com.shoppew.notification.repository.NotificationDeliveryRepository;
import com.shoppew.notification.repository.NotificationRepository;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.event.OrderStatusChangedEvent;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository repository; private final NotificationDeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository; private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; private final Clock clock;
    public NotificationService(NotificationRepository repository, NotificationDeliveryRepository deliveryRepository,
            OrderRepository orderRepository, UserRepository userRepository,
            ApplicationEventPublisher eventPublisher, Clock clock) {
        this.repository = repository; this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository; this.userRepository = userRepository;
        this.eventPublisher = eventPublisher; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, int page, int size) {
        return PageResponse.from(repository.findAllByUser_Id(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::response);
    }
    @Transactional(readOnly = true) public long unreadCount(UUID userId) { return repository.countByUser_IdAndReadAtIsNull(userId); }
    @Transactional
    public NotificationResponse read(UUID userId, UUID notificationId) {
        NotificationEntity value = repository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo"));
        value.read(Instant.now(clock)); return response(value);
    }
    @Transactional public int readAll(UUID userId) { return repository.markAllRead(userId, Instant.now(clock)); }

    @EventListener
    @Transactional
    public void orderStatusChanged(OrderStatusChangedEvent event) {
        OrderEntity order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) return;
        UserEntity user = userRepository.getReferenceById(order.getUserId());
        create(user, NotificationType.ORDER, "Order " + order.getOrderNumber(),
                "Order status changed to " + event.to().name(), Map.of(
                        "orderId", order.getId().toString(), "orderNumber", order.getOrderNumber(),
                        "fromStatus", event.from() == null ? "" : event.from().name(),
                        "toStatus", event.to().name()), Channel.PUSH);
    }

    @Transactional
    public NotificationEntity create(UserEntity user, NotificationType type, String title,
            String body, Map<String, Object> data) {
        return create(user, type, title, body, data, new Channel[0]);
    }

    @Transactional
    public NotificationEntity create(UserEntity user, NotificationType type, String title,
            String body, Map<String, Object> data, Channel... additionalChannels) {
        Instant now = Instant.now(clock);
        NotificationEntity notification = repository.save(NotificationEntity.create(user, type, title, body, data, now));
        deliveryRepository.save(NotificationDeliveryEntity.inApp(notification, now));
        java.util.Arrays.stream(additionalChannels)
                .filter(channel -> channel != Channel.IN_APP)
                .distinct()
                .forEach(channel -> {
                    deliveryRepository.save(NotificationDeliveryEntity.pending(notification, channel, now));
                    eventPublisher.publishEvent(new NotificationDispatchRequestedEvent(notification.getId(), channel));
                });
        return notification;
    }
    private NotificationResponse response(NotificationEntity value) {
        return new NotificationResponse(value.getId(), value.getNotificationType(), value.getTitle(), value.getBody(),
                value.getData(), value.getReadAt() != null, value.getReadAt(), value.getCreatedAt());
    }
}
