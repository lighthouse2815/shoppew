package com.shoppew.order.service;

import com.shoppew.order.dto.OrderAddressResponse;
import com.shoppew.order.dto.AdminOrderSummaryResponse;
import com.shoppew.order.dto.OrderDetailResponse;
import com.shoppew.order.dto.OrderHistoryResponse;
import com.shoppew.order.dto.OrderItemResponse;
import com.shoppew.order.dto.OrderSummaryResponse;
import com.shoppew.order.entity.OrderAddressEntity;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderItemEntity;
import com.shoppew.order.repository.OrderAddressRepository;
import com.shoppew.order.repository.OrderItemRepository;
import com.shoppew.order.repository.OrderStatusHistoryRepository;
import com.shoppew.shipping.dto.ShipmentResponse;
import com.shoppew.shipping.repository.ShipmentRepository;
import com.shoppew.shipping.repository.ShipmentTrackingRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderResponseAssembler {

    private final OrderItemRepository itemRepository;
    private final OrderAddressRepository addressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;

    public OrderResponseAssembler(
            OrderItemRepository itemRepository,
            OrderAddressRepository addressRepository,
            OrderStatusHistoryRepository historyRepository,
            ShipmentRepository shipmentRepository,
            ShipmentTrackingRepository trackingRepository) {
        this.itemRepository = itemRepository;
        this.addressRepository = addressRepository;
        this.historyRepository = historyRepository;
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
    }

    public OrderSummaryResponse summary(OrderEntity order) {
        return new OrderSummaryResponse(
                order.getId(), order.getOrderNumber(), order.getShopId(), order.getShop().getName(),
                order.getStatus().name(), itemRepository.countByOrder_Id(order.getId()), order.getGrandTotal(),
                order.getCurrency(), order.getPlacedAt(), order.getUpdatedAt());
    }

    public AdminOrderSummaryResponse adminSummary(OrderEntity order) {
        return new AdminOrderSummaryResponse(
                order.getId(), order.getOrderNumber(), order.getCheckoutGroupId(),
                order.getUserId(), order.getUserEmail(), order.getShopId(), order.getShop().getName(),
                order.getStatus().name(), itemRepository.countByOrder_Id(order.getId()),
                order.getGrandTotal(), order.getCurrency(), order.getPlacedAt(), order.getUpdatedAt());
    }

    public OrderDetailResponse detail(OrderEntity order) {
        List<OrderItemResponse> items = itemRepository.findAllByOrder_IdOrderByCreatedAtAsc(order.getId()).stream()
                .map(this::item).toList();
        OrderAddressResponse address = addressRepository.findById(order.getId()).map(this::address).orElse(null);
        List<OrderHistoryResponse> history = historyRepository
                .findAllByOrder_IdOrderByCreatedAtAsc(order.getId()).stream()
                .map(entry -> new OrderHistoryResponse(
                        entry.getFromStatus() == null ? null : entry.getFromStatus().name(),
                        entry.getToStatus().name(), entry.getActorId(), entry.getActorType().name(),
                        entry.getReason(), entry.getCreatedAt()))
                .toList();
        ShipmentResponse shipment = shipmentRepository.findByOrder_Id(order.getId()).map(entity ->
                new ShipmentResponse(
                        entity.getId(), entity.getMethod().getProvider(), entity.getMethod().getCode(),
                        entity.getMethod().getName(), entity.getTrackingNumber(), entity.getStatus().name(),
                        entity.getFee(), entity.getCurrency(), entity.getEstimatedDeliveryFrom(),
                        entity.getEstimatedDeliveryTo(), entity.getShippedAt(), entity.getDeliveredAt(),
                        trackingRepository.findAllByShipment_IdOrderByOccurredAtAsc(entity.getId()).stream()
                                .map(track -> new ShipmentResponse.TrackingResponse(
                                        track.getStatus().name(), track.getDescription(), track.getLocation(),
                                        track.getOccurredAt()))
                                .toList()))
                .orElse(null);
        return new OrderDetailResponse(
                order.getId(), order.getOrderNumber(), order.getCheckoutGroupId(), order.getShopId(),
                order.getShop().getName(), order.getShop().getSlug(), order.getStatus().name(),
                order.getItemsSubtotal(), order.getShippingTotal(), order.getShopDiscountTotal(),
                order.getPlatformDiscountTotal(), order.getGrandTotal(), order.getCurrency(),
                order.getCustomerNote(), address, items, history, shipment, order.getPlacedAt(),
                order.getPaidAt(), order.getCompletedAt(), order.getCancelledAt(), order.getUpdatedAt());
    }

    private OrderItemResponse item(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getId(), item.getProductId(), item.getVariantId(), item.getProductName(),
                item.getVariantName(), item.getSku(), item.getImageUrl(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal(), item.getCurrency());
    }

    private OrderAddressResponse address(OrderAddressEntity address) {
        return new OrderAddressResponse(
                address.getRecipientName(), address.getPhone(), address.getCountryCode(), address.getProvince(),
                address.getDistrict(), address.getWard(), address.getAddressLine(), address.getPostalCode());
    }
}
