package com.shoppew.inventory.service;

import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.dto.InventoryReservationResponse;
import com.shoppew.inventory.entity.InventoryEntity;
import com.shoppew.inventory.entity.InventoryReservationEntity;
import com.shoppew.inventory.entity.InventoryReservationStatus;
import com.shoppew.inventory.entity.InventoryTransactionEntity;
import com.shoppew.inventory.entity.InventoryTransactionType;
import com.shoppew.inventory.repository.InventoryRepository;
import com.shoppew.inventory.repository.InventoryReservationRepository;
import com.shoppew.inventory.repository.InventoryTransactionRepository;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.entity.VariantStatus;
import com.shoppew.product.repository.ProductVariantRepository;
import com.shoppew.shop.entity.ShopStatus;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductVariantRepository variantRepository;
    private final EntityManager entityManager;
    private final AppProperties properties;
    private final Clock clock;

    public InventoryReservationService(
            InventoryRepository inventoryRepository,
            InventoryReservationRepository reservationRepository,
            InventoryTransactionRepository transactionRepository,
            ProductVariantRepository variantRepository,
            EntityManager entityManager,
            AppProperties properties,
            Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.transactionRepository = transactionRepository;
        this.variantRepository = variantRepository;
        this.entityManager = entityManager;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public InventoryReservationResponse reserve(UUID userId, UUID variantId, long quantity, UUID orderId) {
        if (quantity <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVENTORY_QUANTITY",
                    "Số lượng giữ tồn phải lớn hơn 0");
        }
        ProductVariantEntity variant = requireEligibleVariant(variantId);
        inventoryRepository.provision(variantId);
        if (inventoryRepository.reserveAtomically(variantId, quantity) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Sản phẩm không còn đủ tồn kho khả dụng");
        }
        InventoryEntity after = inventoryRepository.findById(variantId).orElseThrow(() ->
                new IllegalStateException("Inventory disappeared after atomic reservation"));
        entityManager.refresh(after);
        Instant now = Instant.now(clock);
        InventoryReservationEntity reservation = reservationRepository.save(InventoryReservationEntity.create(
                variant, userId, orderId, quantity, now.plus(properties.inventoryReservationTtl()), now));
        transactionRepository.save(InventoryTransactionEntity.create(
                variant,
                InventoryTransactionType.RESERVE,
                quantity,
                after.getAvailableQuantity() + quantity,
                after.getAvailableQuantity(),
                after.getReservedQuantity() - quantity,
                after.getReservedQuantity(),
                "RESERVATION",
                reservation.getId(),
                null,
                userId,
                now));
        return response(reservation);
    }

    @Transactional
    public InventoryReservationResponse release(UUID reservationId) {
        InventoryReservationEntity reservation = reservationRepository.findLocked(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVENTORY_RESERVATION_NOT_FOUND",
                        "Không tìm thấy lượt giữ tồn kho"));
        release(reservation, InventoryReservationStatus.RELEASED, Instant.now(clock));
        return response(reservation);
    }

    @Transactional
    public InventoryReservationResponse consume(UUID reservationId) {
        InventoryReservationEntity reservation = reservationRepository.findLocked(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVENTORY_RESERVATION_NOT_FOUND",
                        "Không tìm thấy lượt giữ tồn kho"));
        consume(reservation, Instant.now(clock));
        return response(reservation);
    }

    @Transactional
    public void consumeOrder(UUID orderId) {
        Instant now = Instant.now(clock);
        reservationRepository.findAllLockedByOrderId(orderId)
                .forEach(reservation -> consume(reservation, now));
    }

    @Transactional
    public void releaseOrder(UUID orderId) {
        Instant now = Instant.now(clock);
        reservationRepository.findAllLockedByOrderId(orderId)
                .forEach(reservation -> release(reservation, InventoryReservationStatus.RELEASED, now));
    }

    @Transactional
    public void returnOrderSold(UUID orderId, UUID actorId) {
        Instant now = Instant.now(clock);
        for (InventoryReservationEntity reservation : reservationRepository.findAllLockedByOrderId(orderId)) {
            if (reservation.getStatus() != InventoryReservationStatus.CONSUMED) continue;
            InventoryEntity before = inventoryRepository.findById(reservation.getVariantId())
                    .orElseThrow(() -> new IllegalStateException("Inventory missing while returning sold stock"));
            if (inventoryRepository.returnSoldAtomically(reservation.getVariantId(), reservation.getQuantity()) != 1) {
                throw new IllegalStateException("Sold stock invariant failed while returning an order");
            }
            transactionRepository.save(InventoryTransactionEntity.create(
                    reservation.getVariant(), InventoryTransactionType.RETURN, reservation.getQuantity(),
                    before.getAvailableQuantity(), before.getAvailableQuantity() + reservation.getQuantity(),
                    before.getReservedQuantity(), before.getReservedQuantity(), "ORDER", orderId,
                    "ORDER_CANCELLED", actorId, now));
        }
    }

    private void consume(InventoryReservationEntity reservation, Instant now) {
        if (reservation.getStatus() != InventoryReservationStatus.ACTIVE) return;
        InventoryEntity before = inventoryRepository.findById(reservation.getVariantId())
                .orElseThrow(() -> new IllegalStateException("Inventory missing while consuming reservation"));
        if (inventoryRepository.consumeAtomically(reservation.getVariantId(), reservation.getQuantity()) != 1) {
            throw new IllegalStateException("Reserved stock invariant failed while consuming reservation");
        }
        reservation.consume();
        transactionRepository.save(InventoryTransactionEntity.create(
                reservation.getVariant(), InventoryTransactionType.SALE, reservation.getQuantity(),
                before.getAvailableQuantity(), before.getAvailableQuantity(), before.getReservedQuantity(),
                before.getReservedQuantity() - reservation.getQuantity(), "RESERVATION", reservation.getId(),
                null, reservation.getUserId(), now));
    }

    @Scheduled(fixedDelayString = "${app.inventory-expiry-scan-delay:PT30S}")
    @Transactional
    public int releaseExpired() {
        Instant now = Instant.now(clock);
        var expired = reservationRepository
                .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                        InventoryReservationStatus.ACTIVE, now);
        expired.forEach(reservation -> release(reservation, InventoryReservationStatus.EXPIRED, now));
        return expired.size();
    }

    private void release(
            InventoryReservationEntity reservation,
            InventoryReservationStatus terminalStatus,
            Instant now) {
        if (reservation.getStatus() != InventoryReservationStatus.ACTIVE) return;
        InventoryEntity before = inventoryRepository.findById(reservation.getVariantId())
                .orElseThrow(() -> new IllegalStateException("Inventory missing while releasing reservation"));
        if (inventoryRepository.releaseAtomically(reservation.getVariantId(), reservation.getQuantity()) != 1) {
            throw new IllegalStateException("Reserved stock invariant failed while releasing reservation");
        }
        reservation.release(terminalStatus, now);
        transactionRepository.save(InventoryTransactionEntity.create(
                reservation.getVariant(), InventoryTransactionType.RELEASE, reservation.getQuantity(),
                before.getAvailableQuantity(), before.getAvailableQuantity() + reservation.getQuantity(),
                before.getReservedQuantity(), before.getReservedQuantity() - reservation.getQuantity(),
                "RESERVATION", reservation.getId(), terminalStatus.name(), reservation.getUserId(), now));
    }

    private ProductVariantEntity requireEligibleVariant(UUID variantId) {
        ProductVariantEntity variant = variantRepository.findForInventory(variantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                        "Không tìm thấy phân loại sản phẩm"));
        if (variant.getStatus() != VariantStatus.ACTIVE
                || variant.getProduct().getStatus() != ProductStatus.ACTIVE
                || variant.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "VARIANT_NOT_ELIGIBLE",
                    "Phân loại sản phẩm hiện không thể đặt mua");
        }
        return variant;
    }

    private InventoryReservationResponse response(InventoryReservationEntity reservation) {
        return new InventoryReservationResponse(
                reservation.getId(), reservation.getVariantId(), reservation.getUserId(), reservation.getOrderId(),
                reservation.getQuantity(), reservation.getStatus().name(), reservation.getExpiresAt(),
                reservation.getCreatedAt());
    }
}
