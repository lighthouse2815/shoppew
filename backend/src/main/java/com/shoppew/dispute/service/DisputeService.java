package com.shoppew.dispute.service;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.dispute.dto.DisputeCreateRequest;
import com.shoppew.dispute.dto.DisputeMessageRequest;
import com.shoppew.dispute.dto.DisputeResponse;
import com.shoppew.dispute.dto.DisputeUpdateRequest;
import com.shoppew.dispute.entity.DisputeEntity;
import com.shoppew.dispute.entity.DisputeMessageEntity;
import com.shoppew.dispute.entity.DisputeStatus;
import com.shoppew.dispute.repository.DisputeMessageRepository;
import com.shoppew.dispute.repository.DisputeRepository;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.refund.entity.RefundRequestEntity;
import com.shoppew.refund.repository.RefundRequestRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisputeService {
    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private final DisputeRepository disputes; private final DisputeMessageRepository messages;
    private final OrderRepository orders; private final RefundRequestRepository refunds; private final UserRepository users;
    private final ShopAccessService access; private final AdminAuditService audit; private final Clock clock;
    public DisputeService(DisputeRepository disputes, DisputeMessageRepository messages, OrderRepository orders,
            RefundRequestRepository refunds, UserRepository users, ShopAccessService access,
            AdminAuditService audit, Clock clock) {
        this.disputes = disputes; this.messages = messages; this.orders = orders; this.refunds = refunds;
        this.users = users; this.access = access; this.audit = audit; this.clock = clock;
    }

    @Transactional
    public DisputeResponse create(UUID userId, DisputeCreateRequest input) {
        OrderEntity order = orders.findByIdAndUser_Id(input.orderId(), userId).orElseThrow(this::notFound);
        if (!Set.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED, OrderStatus.REFUND_REQUESTED,
                OrderStatus.PARTIALLY_REFUNDED, OrderStatus.REFUNDED).contains(order.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "DISPUTE_ORDER_NOT_ELIGIBLE", "The order is not eligible for a dispute");
        }
        RefundRequestEntity refund = input.refundRequestId() == null ? null : refunds.findById(input.refundRequestId()).orElseThrow(this::notFound);
        if (refund != null && (!refund.getOrderId().equals(order.getId()) || !refund.getUserId().equals(userId))) throw notFound();
        Instant now = Instant.now(clock);
        return response(disputes.save(DisputeEntity.create(number(), order, refund,
                users.findById(userId).orElseThrow(this::notFound), input.reason().strip(), input.description().strip(), now)));
    }

    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> customerList(UUID userId, int page, int size) {
        return PageResponse.from(disputes.findAllByOrder_User_Id(userId, page(page, size)), this::response);
    }
    @Transactional(readOnly = true)
    public DisputeResponse customerDetail(UUID userId, UUID disputeId) {
        return response(disputes.findByIdAndOrder_User_Id(disputeId, userId).orElseThrow(this::notFound));
    }
    @Transactional
    public DisputeResponse customerMessage(UUID userId, UUID disputeId, DisputeMessageRequest input) {
        DisputeEntity dispute = disputes.findByIdAndOrder_User_Id(disputeId, userId).orElseThrow(this::notFound);
        addMessage(dispute, userId, input); return response(dispute);
    }

    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> sellerList(UUID userId, UUID shopId, int page, int size) {
        access.requireActiveMember(userId, shopId);
        return PageResponse.from(disputes.findAllByOrder_Shop_Id(shopId, page(page, size)), this::response);
    }
    @Transactional(readOnly = true)
    public DisputeResponse sellerDetail(UUID userId, UUID shopId, UUID disputeId) {
        access.requireActiveMember(userId, shopId);
        return response(disputes.findByIdAndOrder_Shop_Id(disputeId, shopId).orElseThrow(this::notFound));
    }
    @Transactional
    public DisputeResponse sellerMessage(UUID userId, UUID shopId, UUID disputeId, DisputeMessageRequest input) {
        access.requireActiveMember(userId, shopId);
        DisputeEntity dispute = disputes.findByIdAndOrder_Shop_Id(disputeId, shopId).orElseThrow(this::notFound);
        addMessage(dispute, userId, input); return response(dispute);
    }

    @Transactional(readOnly = true)
    public PageResponse<DisputeResponse> adminList(DisputeStatus status, int page, int size) {
        return PageResponse.from(status == null ? disputes.findAll(page(page, size))
                : disputes.findAllByStatus(status, page(page, size)), this::response);
    }
    @Transactional(readOnly = true)
    public DisputeResponse adminDetail(UUID disputeId) { return response(disputes.findById(disputeId).orElseThrow(this::notFound)); }
    @Transactional
    public DisputeResponse adminMessage(UUID actorId, UUID disputeId, DisputeMessageRequest input) {
        DisputeEntity dispute = disputes.findById(disputeId).orElseThrow(this::notFound);
        addMessage(dispute, actorId, input); return response(dispute);
    }
    @Transactional
    public DisputeResponse adminUpdate(UUID actorId, UUID disputeId, DisputeUpdateRequest input) {
        DisputeEntity dispute = disputes.findLocked(disputeId).orElseThrow(this::notFound);
        if (dispute.getStatus() == DisputeStatus.CLOSED) throw invalidState(dispute);
        if ((input.status() == DisputeStatus.RESOLVED || input.status() == DisputeStatus.CLOSED)
                && (input.resolution() == null || input.resolution().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DISPUTE_RESOLUTION_REQUIRED", "A resolution is required to resolve or close a dispute");
        }
        DisputeStatus before = dispute.getStatus();
        UserEntity actor = users.findById(actorId).orElseThrow(this::notFound);
        dispute.update(input.status(), actor, normalize(input.resolution()), Instant.now(clock));
        audit.record(actorId, "DISPUTE_STATUS_CHANGED", "DISPUTE", disputeId,
                Map.of("status", before), Map.of("status", dispute.getStatus(), "resolution", dispute.getResolution() == null ? "" : dispute.getResolution()));
        return response(dispute);
    }

    private void addMessage(DisputeEntity dispute, UUID authorId, DisputeMessageRequest input) {
        if (Set.of(DisputeStatus.RESOLVED, DisputeStatus.CLOSED).contains(dispute.getStatus())) throw invalidState(dispute);
        List<String> attachments = input.attachments() == null ? List.of() : input.attachments().stream().map(String::strip).toList();
        messages.save(DisputeMessageEntity.create(dispute, users.findById(authorId).orElseThrow(this::notFound),
                input.content().strip(), attachments, Instant.now(clock)));
    }
    private DisputeResponse response(DisputeEntity dispute) {
        List<DisputeResponse.Message> timeline = messages.findAllByDispute_IdOrderByCreatedAtAsc(dispute.getId()).stream()
                .map(message -> new DisputeResponse.Message(message.getId(), message.getAuthorId(), message.getContent(),
                        message.getAttachments(), message.getCreatedAt())).toList();
        return new DisputeResponse(dispute.getId(), dispute.getDisputeNumber(), dispute.getOrderId(), dispute.getOrder().getOrderNumber(),
                dispute.getShopId(), dispute.getCustomerId(), dispute.getRefundRequestId(), dispute.getOpenedById(), dispute.getReason(),
                dispute.getDescription(), dispute.getStatus(), dispute.getAssignedToId(), dispute.getResolution(), dispute.getResolvedAt(),
                timeline, dispute.getCreatedAt(), dispute.getUpdatedAt());
    }
    private PageRequest page(int page, int size) { return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")); }
    private String number() { return "DSP-" + NUMBER_TIME.format(Instant.now(clock)) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "DISPUTE_NOT_FOUND", "Dispute was not found"); }
    private ApiException invalidState(DisputeEntity dispute) { return new ApiException(HttpStatus.CONFLICT, "DISPUTE_INVALID_STATE", "Dispute cannot change from " + dispute.getStatus()); }
}
