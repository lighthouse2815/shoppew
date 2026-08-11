package com.shoppew.chat.service;

import com.shoppew.chat.dto.ConversationResponse;
import com.shoppew.chat.dto.MessageResponse;
import com.shoppew.chat.dto.SendMessageRequest;
import com.shoppew.chat.dto.StartConversationRequest;
import com.shoppew.chat.entity.ConversationEntity;
import com.shoppew.chat.entity.ConversationParticipantEntity;
import com.shoppew.chat.entity.ConversationParticipantId;
import com.shoppew.chat.entity.ConversationStatus;
import com.shoppew.chat.entity.MessageEntity;
import com.shoppew.chat.entity.MessageType;
import com.shoppew.chat.entity.ParticipantType;
import com.shoppew.chat.realtime.ChatRealtimeEvent;
import com.shoppew.chat.realtime.ChatRealtimePublisher;
import com.shoppew.chat.repository.ConversationParticipantRepository;
import com.shoppew.chat.repository.ConversationRepository;
import com.shoppew.chat.repository.MessageRepository;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.notification.entity.NotificationType;
import com.shoppew.notification.service.NotificationService;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.entity.ShopMemberStatus;
import com.shoppew.shop.entity.ShopSettingsEntity;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.shop.repository.ShopMemberRepository;
import com.shoppew.shop.repository.ShopRepository;
import com.shoppew.shop.repository.ShopSettingsRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ConversationRepository conversations;
    private final ConversationParticipantRepository participants;
    private final MessageRepository messages;
    private final ShopRepository shops;
    private final ShopSettingsRepository shopSettings;
    private final ShopMemberRepository shopMembers;
    private final ShopAccessService shopAccess;
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final NotificationService notifications;
    private final ChatRealtimePublisher realtimePublisher;
    private final Clock clock;

    public ChatService(
            ConversationRepository conversations,
            ConversationParticipantRepository participants,
            MessageRepository messages,
            ShopRepository shops,
            ShopSettingsRepository shopSettings,
            ShopMemberRepository shopMembers,
            ShopAccessService shopAccess,
            UserRepository users,
            ProductRepository products,
            OrderRepository orders,
            NotificationService notifications,
            ChatRealtimePublisher realtimePublisher,
            Clock clock) {
        this.conversations = conversations;
        this.participants = participants;
        this.messages = messages;
        this.shops = shops;
        this.shopSettings = shopSettings;
        this.shopMembers = shopMembers;
        this.shopAccess = shopAccess;
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.notifications = notifications;
        this.realtimePublisher = realtimePublisher;
        this.clock = clock;
    }

    @Transactional
    public ConversationResponse start(UUID customerId, StartConversationRequest request) {
        ShopEntity shop = requireChatShop(request.shopId());
        if (shop.getOwnerId().equals(customerId)) {
            throw new ApiException(HttpStatus.CONFLICT, "OWN_SHOP_CHAT_NOT_ALLOWED", "Không thể tự nhắn tin cho gian hàng của bạn");
        }
        UserEntity customer = requireUser(customerId);
        ConversationEntity conversation = conversations
                .findByShop_IdAndCustomer_Id(shop.getId(), customerId)
                .orElse(null);
        boolean created = conversation == null;
        if (created) {
            conversation = conversations.saveAndFlush(ConversationEntity.create(shop, customer, Instant.now(clock)));
            participants.save(ConversationParticipantEntity.join(
                    conversation, customer, ParticipantType.CUSTOMER, Instant.now(clock)));
        }
        if (created && request.productId() != null) {
            append(customerId, conversation.getId(), new SendMessageRequest(
                    MessageType.PRODUCT, null, null, request.productId(), null));
        }
        return response(conversation);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> customerConversations(UUID customerId, int page, int size) {
        return page(conversations.findAllByCustomer_Id(customerId, pageRequest(page, size)), this::response);
    }

    @Transactional
    public PageResponse<MessageResponse> customerMessages(
            UUID customerId, UUID conversationId, int page, int size) {
        ConversationEntity conversation = requireCustomer(customerId, conversationId);
        markRead(conversation, requireUser(customerId), ParticipantType.CUSTOMER);
        return messagePage(customerId, conversationId, page, size);
    }

    @Transactional
    public MessageResponse customerSend(UUID customerId, UUID conversationId, SendMessageRequest request) {
        requireCustomer(customerId, conversationId);
        return append(customerId, conversationId, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> sellerConversations(
            UUID sellerId, UUID shopId, int page, int size) {
        shopAccess.requireActiveMember(sellerId, shopId);
        return page(conversations.findAllByShop_Id(shopId, pageRequest(page, size)), this::response);
    }

    @Transactional
    public PageResponse<MessageResponse> sellerMessages(
            UUID sellerId, UUID shopId, UUID conversationId, int page, int size) {
        ConversationEntity conversation = requireSeller(sellerId, shopId, conversationId);
        markRead(conversation, requireUser(sellerId), ParticipantType.SHOP_MEMBER);
        return messagePage(sellerId, conversationId, page, size);
    }

    @Transactional
    public MessageResponse sellerSend(
            UUID sellerId, UUID shopId, UUID conversationId, SendMessageRequest request) {
        requireSeller(sellerId, shopId, conversationId);
        return append(sellerId, conversationId, request);
    }

    private MessageResponse append(UUID senderId, UUID conversationId, SendMessageRequest request) {
        ConversationEntity conversation = conversations.findLockedById(conversationId).orElseThrow(this::notFound);
        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "CONVERSATION_NOT_ACTIVE", "Cuộc trò chuyện hiện không hoạt động");
        }
        requireChatEnabled(conversation.getShopId());
        UserEntity sender = requireUser(senderId);
        MessageContent content = validateContent(conversation, request);
        Instant now = Instant.now(clock);
        MessageEntity message = messages.save(MessageEntity.create(
                conversation,
                sender,
                request.type(),
                content.text(),
                content.mediaUrl(),
                content.product(),
                content.order(),
                now));
        conversation.recordMessage(now);
        MessageResponse response = message(message, senderId);
        Set<UUID> recipients = recipients(conversation, senderId);
        createNotifications(conversation, senderId, recipients, response);
        realtimePublisher.publish(recipients, new ChatRealtimeEvent("MESSAGE_CREATED", conversationId, response));
        return response;
    }

    private MessageContent validateContent(ConversationEntity conversation, SendMessageRequest request) {
        return switch (request.type()) {
            case TEXT -> {
                String text = trimToNull(request.textContent());
                if (text == null) {
                    throw invalidMessage("Tin nhắn văn bản không được để trống");
                }
                yield new MessageContent(text, null, null, null);
            }
            case IMAGE -> {
                String url = validMediaUrl(request.mediaUrl());
                yield new MessageContent(null, url, null, null);
            }
            case PRODUCT -> {
                if (request.productId() == null) {
                    throw invalidMessage("Tin nhắn sản phẩm cần productId");
                }
                ProductEntity product = products.findDetailedById(request.productId()).orElseThrow(this::productNotFound);
                if (product.getStatus() != ProductStatus.ACTIVE
                        || !product.getShopId().equals(conversation.getShopId())) {
                    throw productNotFound();
                }
                yield new MessageContent(null, null, product, null);
            }
            case ORDER -> {
                if (request.orderId() == null) {
                    throw invalidMessage("Tin nhắn đơn hàng cần orderId");
                }
                OrderEntity order = orders.findById(request.orderId()).orElseThrow(this::orderNotFound);
                if (!order.getShopId().equals(conversation.getShopId())
                        || !order.getUserId().equals(conversation.getCustomerId())) {
                    throw orderNotFound();
                }
                yield new MessageContent(null, null, null, order);
            }
        };
    }

    private PageResponse<MessageResponse> messagePage(
            UUID currentUserId, UUID conversationId, int page, int size) {
        Page<MessageEntity> result = messages.findAllByConversation_Id(
                conversationId,
                PageRequest.of(Math.max(page, 0), boundedSize(size), Sort.by(Sort.Direction.DESC, "sentAt")));
        return page(result, message -> message(message, currentUserId));
    }

    private ConversationEntity requireCustomer(UUID customerId, UUID conversationId) {
        ConversationEntity conversation = conversations.findDetailedById(conversationId).orElseThrow(this::notFound);
        if (!conversation.getCustomerId().equals(customerId)) {
            throw notFound();
        }
        return conversation;
    }

    private ConversationEntity requireSeller(UUID sellerId, UUID shopId, UUID conversationId) {
        shopAccess.requireActiveMember(sellerId, shopId);
        ConversationEntity conversation = conversations.findDetailedById(conversationId).orElseThrow(this::notFound);
        if (!conversation.getShopId().equals(shopId)) {
            throw notFound();
        }
        return conversation;
    }

    private void markRead(
            ConversationEntity conversation,
            UserEntity user,
            ParticipantType type) {
        ConversationParticipantId id = new ConversationParticipantId(conversation.getId(), user.getId());
        ConversationParticipantEntity participant = participants.findById(id)
                .orElseGet(() -> ConversationParticipantEntity.join(conversation, user, type, Instant.now(clock)));
        participant.markRead(Instant.now(clock));
        participants.save(participant);
    }

    private Set<UUID> recipients(ConversationEntity conversation, UUID senderId) {
        LinkedHashSet<UUID> recipientIds = new LinkedHashSet<>();
        if (!conversation.getCustomerId().equals(senderId)) {
            recipientIds.add(conversation.getCustomerId());
        }
        shopMembers.findAllByShop_IdAndStatus(conversation.getShopId(), ShopMemberStatus.ACTIVE).stream()
                .map(member -> member.getUserId())
                .filter(userId -> !userId.equals(senderId))
                .forEach(recipientIds::add);
        return recipientIds;
    }

    private void createNotifications(
            ConversationEntity conversation,
            UUID senderId,
            Set<UUID> recipientIds,
            MessageResponse message) {
        String senderLabel = conversation.getCustomerId().equals(senderId)
                ? conversation.getCustomer().getEmail()
                : conversation.getShop().getName();
        String preview = preview(message);
        for (UUID recipientId : recipientIds) {
            notifications.create(
                    users.getReferenceById(recipientId),
                    NotificationType.CHAT,
                    "Tin nhắn mới từ " + senderLabel,
                    preview,
                    java.util.Map.of(
                            "conversationId", conversation.getId().toString(),
                            "shopId", conversation.getShopId().toString(),
                            "messageId", message.id().toString()));
        }
    }

    private ConversationResponse response(ConversationEntity conversation) {
        MessageEntity latest = messages.findFirstByConversation_IdOrderBySentAtDesc(conversation.getId()).orElse(null);
        return new ConversationResponse(
                conversation.getId(),
                conversation.getShopId(),
                conversation.getShop().getName(),
                conversation.getCustomerId(),
                conversation.getCustomer().getEmail(),
                conversation.getStatus().name(),
                latest == null ? null : preview(message(latest, null)),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }

    private MessageResponse message(MessageEntity message, UUID currentUserId) {
        ProductEntity product = message.getProduct();
        OrderEntity order = message.getOrder();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderId(),
                message.getSender().getEmail(),
                currentUserId != null && message.getSenderId().equals(currentUserId),
                message.getMessageType().name(),
                message.getTextContent(),
                message.getMediaUrl(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                order == null ? null : order.getId(),
                order == null ? null : order.getOrderNumber(),
                message.getSentAt());
    }

    private String preview(MessageResponse message) {
        return switch (MessageType.valueOf(message.type())) {
            case TEXT -> message.textContent();
            case IMAGE -> "Đã gửi một hình ảnh";
            case PRODUCT -> "Sản phẩm: " + message.productName();
            case ORDER -> "Đơn hàng: " + message.orderNumber();
        };
    }

    private ShopEntity requireChatShop(UUID shopId) {
        ShopEntity shop = shops.findById(shopId).orElseThrow(this::shopNotFound);
        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw shopNotFound();
        }
        requireChatEnabled(shopId);
        return shop;
    }

    private void requireChatEnabled(UUID shopId) {
        ShopSettingsEntity settings = shopSettings.findById(shopId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT, "SHOP_SETTINGS_NOT_FOUND", "Gian hàng chưa có thiết lập chat"));
        if (!settings.isChatEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, "SHOP_CHAT_DISABLED", "Gian hàng hiện tắt tính năng chat");
        }
    }

    private UserEntity requireUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
    }

    private String validMediaUrl(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw invalidMessage("Tin nhắn hình ảnh cần mediaUrl");
        }
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw invalidMessage("mediaUrl phải là địa chỉ HTTP hoặc HTTPS hợp lệ");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw invalidMessage("mediaUrl không hợp lệ");
        }
    }

    private <S, T> PageResponse<T> page(Page<S> source, java.util.function.Function<S, T> mapper) {
        return PageResponse.from(source, mapper);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                boundedSize(size),
                Sort.by(Sort.Order.desc("lastMessageAt"), Sort.Order.desc("updatedAt")));
    }

    private int boundedSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private ApiException invalidMessage(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CHAT_MESSAGE", message);
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Không tìm thấy cuộc trò chuyện");
    }

    private ApiException shopNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Không tìm thấy gian hàng");
    }

    private ApiException productNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm trong gian hàng này");
    }

    private ApiException orderNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng trong cuộc trò chuyện này");
    }

    private record MessageContent(
            String text,
            String mediaUrl,
            ProductEntity product,
            OrderEntity order) {}
}
