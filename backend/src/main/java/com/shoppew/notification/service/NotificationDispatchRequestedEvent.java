package com.shoppew.notification.service;

import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import java.util.UUID;

public record NotificationDispatchRequestedEvent(UUID notificationId, Channel channel) {}
