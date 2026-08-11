package com.shoppew.inventory.service;

import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.dto.InventoryAdjustmentMode;
import com.shoppew.inventory.dto.InventoryAdjustmentRequest;
import com.shoppew.inventory.dto.InventoryResponse;
import com.shoppew.inventory.dto.InventoryTransactionResponse;
import com.shoppew.inventory.entity.InventoryEntity;
import com.shoppew.inventory.entity.InventoryTransactionEntity;
import com.shoppew.inventory.entity.InventoryTransactionType;
import com.shoppew.inventory.repository.InventoryRepository;
import com.shoppew.inventory.repository.InventoryTransactionRepository;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.repository.ProductVariantRepository;
import com.shoppew.shop.service.ShopAccessService;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductVariantRepository variantRepository;
    private final ShopAccessService shopAccessService;
    private final Clock clock;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository transactionRepository,
            ProductVariantRepository variantRepository,
            ShopAccessService shopAccessService,
            Clock clock) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.variantRepository = variantRepository;
        this.shopAccessService = shopAccessService;
        this.clock = clock;
    }

    @Transactional
    public void initialize(ProductVariantEntity variant) {
        inventoryRepository.provision(variant.getId());
    }

    @Transactional
    public PageResponse<InventoryResponse> list(
            UUID userId,
            UUID shopId,
            String query,
            boolean lowStock,
            int page,
            int size) {
        shopAccessService.requireActiveMember(userId, shopId);
        String normalizedQuery = query == null || query.isBlank() ? null : query.strip();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductVariantEntity> variants = normalizedQuery == null
                ? variantRepository.listForInventory(shopId, lowStock, pageable)
                : variantRepository.searchForInventory(shopId, normalizedQuery, lowStock, pageable);
        List<UUID> variantIds = variants.getContent().stream().map(ProductVariantEntity::getId).toList();
        variantIds.forEach(inventoryRepository::provision);
        Map<UUID, InventoryEntity> inventories = new HashMap<>();
        if (!variantIds.isEmpty()) {
            inventoryRepository.findAllDetailedByVariantIdIn(variantIds)
                    .forEach(inventory -> inventories.put(inventory.getVariantId(), inventory));
        }
        List<InventoryResponse> content = variants.getContent().stream()
                .map(variant -> response(requireInventory(inventories, variant.getId())))
                .toList();
        return new PageResponse<>(content, variants.getNumber(), variants.getSize(),
                variants.getTotalElements(), variants.getTotalPages());
    }

    @Transactional
    public InventoryResponse adjust(
            UUID userId,
            UUID shopId,
            UUID variantId,
            InventoryAdjustmentRequest request) {
        shopAccessService.requireActiveMember(userId, shopId);
        ProductVariantEntity variant = requireVariantForShop(variantId, shopId);
        inventoryRepository.provision(variantId);
        InventoryEntity inventory = inventoryRepository.findLocked(variantId).orElseThrow(this::inventoryMissing);

        long before = inventory.getAvailableQuantity();
        long after = adjustedQuantity(before, request);
        InventoryTransactionType type = switch (request.mode()) {
            case INCREASE -> InventoryTransactionType.STOCK_IN;
            case DECREASE -> InventoryTransactionType.STOCK_OUT;
            case SET -> InventoryTransactionType.ADJUSTMENT;
        };
        long transactionQuantity = request.mode() == InventoryAdjustmentMode.SET
                ? Math.abs(after - before)
                : request.quantity();
        if (transactionQuantity == 0 && request.lowStockThreshold() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_INVENTORY_ADJUSTMENT",
                    "Điều chỉnh tồn kho không tạo ra thay đổi");
        }

        inventory.setAvailableQuantity(after, request.lowStockThreshold(), Instant.now(clock));
        if (transactionQuantity > 0) {
            transactionRepository.save(InventoryTransactionEntity.create(
                    variant,
                    type,
                    transactionQuantity,
                    before,
                    after,
                    inventory.getReservedQuantity(),
                    inventory.getReservedQuantity(),
                    "SELLER_ADJUSTMENT",
                    null,
                    normalizeNote(request.note()),
                    userId,
                    Instant.now(clock)));
        }
        return response(inventory);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryTransactionResponse> transactions(
            UUID userId,
            UUID shopId,
            UUID variantId,
            int page,
            int size) {
        shopAccessService.requireActiveMember(userId, shopId);
        requireVariantForShop(variantId, shopId);
        return PageResponse.from(
                transactionRepository.findAllByVariant_Id(
                        variantId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
                this::transactionResponse);
    }

    private ProductVariantEntity requireVariantForShop(UUID variantId, UUID shopId) {
        ProductVariantEntity variant = variantRepository.findForInventory(variantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                        "Không tìm thấy phân loại sản phẩm"));
        if (!variant.getShopId().equals(shopId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                    "Không tìm thấy phân loại sản phẩm");
        }
        return variant;
    }

    private long adjustedQuantity(long before, InventoryAdjustmentRequest request) {
        try {
            return switch (request.mode()) {
                case INCREASE -> {
                    requirePositive(request.quantity());
                    yield Math.addExact(before, request.quantity());
                }
                case DECREASE -> {
                    requirePositive(request.quantity());
                    if (request.quantity() > before) {
                        throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                                "Số lượng tồn khả dụng không đủ cho điều chỉnh này");
                    }
                    yield before - request.quantity();
                }
                case SET -> request.quantity();
            };
        } catch (ArithmeticException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVENTORY_QUANTITY_OVERFLOW",
                    "Số lượng tồn kho vượt quá giới hạn hỗ trợ");
        }
    }

    private void requirePositive(long quantity) {
        if (quantity <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INVENTORY_QUANTITY",
                    "Số lượng tăng hoặc giảm phải lớn hơn 0");
        }
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }

    private InventoryEntity requireInventory(Map<UUID, InventoryEntity> inventories, UUID variantId) {
        InventoryEntity inventory = inventories.get(variantId);
        if (inventory == null) throw inventoryMissing();
        return inventory;
    }

    private ApiException inventoryMissing() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INVENTORY_NOT_INITIALIZED",
                "Tồn kho của phân loại chưa được khởi tạo");
    }

    private InventoryResponse response(InventoryEntity inventory) {
        ProductVariantEntity variant = inventory.getVariant();
        return new InventoryResponse(
                variant.getId(),
                variant.getProductId(),
                variant.getProduct().getName(),
                variant.getProduct().getSlug(),
                variant.getSku(),
                variant.getName(),
                variant.getStatus().name(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getSoldQuantity(),
                inventory.getLowStockThreshold(),
                inventory.getAvailableQuantity() <= inventory.getLowStockThreshold(),
                inventory.getUpdatedAt());
    }

    private InventoryTransactionResponse transactionResponse(InventoryTransactionEntity transaction) {
        return new InventoryTransactionResponse(
                transaction.getId(), transaction.getVariantId(), transaction.getType().name(), transaction.getQuantity(),
                transaction.getAvailableBefore(), transaction.getAvailableAfter(), transaction.getReservedBefore(),
                transaction.getReservedAfter(), transaction.getReferenceType(), transaction.getReferenceId(),
                transaction.getNote(), transaction.getActorId(), transaction.getCreatedAt());
    }
}
