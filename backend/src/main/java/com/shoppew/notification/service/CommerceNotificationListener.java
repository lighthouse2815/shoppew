package com.shoppew.notification.service;

import com.shoppew.auth.event.UserRegisteredEvent;
import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.checkout.event.CheckoutPlacedEvent;
import com.shoppew.checkout.repository.CheckoutGroupRepository;
import com.shoppew.notification.entity.NotificationDeliveryEntity.Channel;
import com.shoppew.notification.entity.NotificationType;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommerceNotificationListener {

    private final NotificationService notifications;
    private final UserRepository users;
    private final CheckoutGroupRepository checkouts;
    private final OrderRepository orders;

    public CommerceNotificationListener(
            NotificationService notifications,
            UserRepository users,
            CheckoutGroupRepository checkouts,
            OrderRepository orders) {
        this.notifications = notifications;
        this.users = users;
        this.checkouts = checkouts;
        this.orders = orders;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void welcome(UserRegisteredEvent event) {
        UserEntity user = users.findById(event.userId()).orElse(null);
        if (user == null) return;
        notifications.create(
                user,
                NotificationType.SYSTEM,
                "Chào mừng bạn đến với shoppew",
                "Tài khoản shoppew của bạn đã được tạo. Hãy hoàn thiện hồ sơ và khám phá các gian hàng.",
                Map.of("kind", "WELCOME"),
                Channel.EMAIL);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void orderConfirmation(CheckoutPlacedEvent event) {
        CheckoutGroupEntity checkout = checkouts.findById(event.checkoutId()).orElse(null);
        if (checkout == null) return;
        UserEntity user = users.findById(checkout.getUserId()).orElse(null);
        if (user == null) return;
        List<OrderEntity> placedOrders = orders.findAllByCheckoutGroup_IdOrderByCreatedAtAsc(checkout.getId());
        String numbers = placedOrders.stream().map(OrderEntity::getOrderNumber).collect(Collectors.joining(", "));
        String body = "Đơn hàng " + numbers + " đã được tiếp nhận với tổng thanh toán "
                + formatMoney(checkout.getGrandTotal()) + " " + checkout.getCurrency() + ".";
        notifications.create(
                user,
                NotificationType.ORDER,
                "Xác nhận đơn hàng " + checkout.getCheckoutNumber(),
                body,
                Map.of(
                        "kind", "ORDER_CONFIRMATION",
                        "checkoutId", checkout.getId().toString(),
                        "checkoutNumber", checkout.getCheckoutNumber(),
                        "orderIds", placedOrders.stream().map(order -> order.getId().toString()).toList()),
                Channel.EMAIL,
                Channel.PUSH);
    }

    private String formatMoney(java.math.BigDecimal value) {
        NumberFormat formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return formatter.format(value);
    }
}
