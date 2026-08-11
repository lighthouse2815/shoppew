package com.shoppew.promotion.service;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.exception.ApiException;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.product.repository.ProductVariantRepository;
import com.shoppew.promotion.dto.PromotionRequest;
import com.shoppew.promotion.dto.PromotionResponse;
import com.shoppew.promotion.dto.PromotionTargetRequest;
import com.shoppew.promotion.entity.PromotionEntity;
import com.shoppew.promotion.entity.PromotionOwnerType;
import com.shoppew.promotion.entity.PromotionProductEntity;
import com.shoppew.promotion.entity.PromotionStatus;
import com.shoppew.promotion.entity.PromotionType;
import com.shoppew.promotion.repository.PromotionProductRepository;
import com.shoppew.promotion.repository.PromotionRepository;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.repository.ShopRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.voucher.entity.DiscountType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionManagementService {
    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository targetRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ShopRepository shopRepository;
    private final ShopAccessService shopAccessService;
    private final AdminAuditService audit;
    private final Clock clock;

    public PromotionManagementService(PromotionRepository promotionRepository,
            PromotionProductRepository targetRepository, ProductRepository productRepository,
            ProductVariantRepository variantRepository, ShopRepository shopRepository,
            ShopAccessService shopAccessService, AdminAuditService audit, Clock clock) {
        this.promotionRepository = promotionRepository; this.targetRepository = targetRepository;
        this.productRepository = productRepository; this.variantRepository = variantRepository;
        this.shopRepository = shopRepository; this.shopAccessService = shopAccessService;
        this.audit = audit; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> sellerList(UUID userId, UUID shopId) {
        shopAccessService.requireActiveMember(userId, shopId);
        return promotionRepository.findAllByShop_IdOrderByCreatedAtDesc(shopId).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true)
    public List<PromotionResponse> adminList() {
        return promotionRepository.findAllByShopIsNullOrderByCreatedAtDesc().stream().map(this::response).toList();
    }
    @Transactional
    public PromotionResponse sellerCreate(UUID userId, UUID shopId, PromotionRequest request) {
        shopAccessService.requireActiveMember(userId, shopId);
        ShopEntity shop = shopRepository.findById(shopId).orElseThrow(this::notFound);
        validate(request, PromotionOwnerType.SHOP, shopId);
        return create(PromotionOwnerType.SHOP, shop, request);
    }
    @Transactional
    public PromotionResponse adminCreate(PromotionRequest request) {
        validate(request, PromotionOwnerType.PLATFORM, null);
        PromotionResponse created = create(PromotionOwnerType.PLATFORM, null, request);
        audit.record(null, "PLATFORM_PROMOTION_CREATED", "PROMOTION", created.id(), null, created);
        return created;
    }
    @Transactional
    public PromotionResponse sellerUpdate(UUID userId, UUID shopId, UUID promotionId, PromotionRequest request) {
        shopAccessService.requireActiveMember(userId, shopId);
        PromotionEntity promotion = promotionRepository.findById(promotionId)
                .filter(value -> shopId.equals(value.getShopId())).orElseThrow(this::notFound);
        return update(promotion, request, PromotionOwnerType.SHOP, shopId);
    }
    @Transactional
    public PromotionResponse adminUpdate(UUID promotionId, PromotionRequest request) {
        PromotionEntity promotion = promotionRepository.findById(promotionId)
                .filter(value -> value.getShopId() == null).orElseThrow(this::notFound);
        PromotionResponse before = response(promotion);
        PromotionResponse updated = update(promotion, request, PromotionOwnerType.PLATFORM, null);
        audit.record(null, "PLATFORM_PROMOTION_UPDATED", "PROMOTION", promotionId, before, updated);
        return updated;
    }
    @Transactional
    public PromotionResponse sellerStatus(UUID userId, UUID shopId, UUID promotionId, String action) {
        shopAccessService.requireActiveMember(userId, shopId);
        PromotionEntity promotion = promotionRepository.findById(promotionId)
                .filter(value -> shopId.equals(value.getShopId())).orElseThrow(this::notFound);
        changeStatus(promotion, action); return response(promotion);
    }
    @Transactional
    public PromotionResponse adminStatus(UUID promotionId, String action) {
        PromotionEntity promotion = promotionRepository.findById(promotionId)
                .filter(value -> value.getShopId() == null).orElseThrow(this::notFound);
        PromotionResponse before = response(promotion);
        changeStatus(promotion, action);
        PromotionResponse updated = response(promotion);
        audit.record(null, "PLATFORM_PROMOTION_" + auditStatusAction(action),
                "PROMOTION", promotionId, before, updated);
        return updated;
    }

    private PromotionResponse create(PromotionOwnerType owner, ShopEntity shop, PromotionRequest request) {
        Instant now = Instant.now(clock);
        PromotionEntity promotion = promotionRepository.save(PromotionEntity.create(owner, shop,
                request.name().strip(), request.promotionType(), request.discountType(), request.discountValue(),
                request.maxDiscount(), request.startsAt(), request.endsAt(), now));
        saveTargets(promotion, request.targets());
        return response(promotion);
    }

    private PromotionResponse update(PromotionEntity promotion, PromotionRequest request,
            PromotionOwnerType owner, UUID shopId) {
        if (promotion.getStatus() == PromotionStatus.ACTIVE || promotion.getStatus() == PromotionStatus.SCHEDULED
                || promotion.getStatus() == PromotionStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMOTION_NOT_EDITABLE", "Hãy tạm dừng promotion trước khi sửa");
        }
        List<PromotionProductEntity> existing = targetRepository.findAllByPromotion_IdOrderById(promotion.getId());
        if (existing.stream().anyMatch(target -> target.getSoldQuantity() > 0)) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMOTION_ALREADY_USED", "Promotion đã có lượt sử dụng và không thể đổi phạm vi");
        }
        validate(request, owner, shopId);
        promotion.update(request.name().strip(), request.promotionType(), request.discountType(),
                request.discountValue(), request.maxDiscount(), request.startsAt(), request.endsAt(), Instant.now(clock));
        targetRepository.deleteAllByPromotion_Id(promotion.getId());
        targetRepository.flush();
        saveTargets(promotion, request.targets());
        return response(promotion);
    }

    private void validate(PromotionRequest request, PromotionOwnerType owner, UUID shopId) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION_WINDOW", "Thời gian kết thúc phải sau bắt đầu");
        }
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION_PERCENTAGE", "Phần trăm giảm không vượt quá 100");
        }
        if (owner == PromotionOwnerType.SHOP && request.promotionType() == PromotionType.PLATFORM_CAMPAIGN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION_OWNER", "Seller không thể tạo campaign nền tảng");
        }
        if (owner == PromotionOwnerType.PLATFORM && request.promotionType() == PromotionType.SHOP_DISCOUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION_OWNER", "Promotion shop phải do seller sở hữu");
        }
        Set<String> scopes = new HashSet<>();
        for (PromotionTargetRequest target : request.targets()) {
            ProductEntity product = productRepository.findById(target.productId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "PROMOTION_PRODUCT_NOT_FOUND", "Sản phẩm promotion không tồn tại"));
            if (shopId != null && !shopId.equals(product.getShopId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PROMOTION_PRODUCT_OUTSIDE_SHOP", "Sản phẩm promotion phải thuộc đúng shop");
            }
            ProductVariantEntity variant = target.variantId() == null ? null
                    : variantRepository.findById(target.variantId()).orElseThrow(() -> new ApiException(
                            HttpStatus.BAD_REQUEST, "PROMOTION_VARIANT_NOT_FOUND", "Biến thể promotion không tồn tại"));
            if (variant != null && !variant.getProductId().equals(product.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PROMOTION_VARIANT_MISMATCH", "Biến thể không thuộc sản phẩm promotion");
            }
            if (target.promotionalPrice() != null && variant != null
                    && target.promotionalPrice().compareTo(variant.getPrice()) >= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PROMOTIONAL_PRICE_NOT_LOWER", "Giá promotion phải thấp hơn giá hiện tại");
            }
            String scope = target.productId() + "|" + target.variantId();
            if (!scopes.add(scope)) throw new ApiException(HttpStatus.CONFLICT, "PROMOTION_SCOPE_DUPLICATE", "Phạm vi promotion bị trùng");
        }
    }

    private void saveTargets(PromotionEntity promotion, List<PromotionTargetRequest> requests) {
        for (PromotionTargetRequest request : requests) {
            ProductEntity product = productRepository.findById(request.productId()).orElseThrow(this::notFound);
            ProductVariantEntity variant = request.variantId() == null ? null
                    : variantRepository.findById(request.variantId()).orElseThrow(this::notFound);
            targetRepository.save(PromotionProductEntity.create(promotion, product, variant,
                    request.promotionalPrice(), request.quantityLimit()));
        }
    }

    private void changeStatus(PromotionEntity promotion, String action) {
        Instant now = Instant.now(clock);
        switch (action) {
            case "activate" -> {
                if (!promotion.getEndsAt().isAfter(now)) throw new ApiException(HttpStatus.CONFLICT,
                        "PROMOTION_EXPIRED", "Promotion đã hết hạn");
                promotion.activate(now);
            }
            case "pause" -> promotion.pause(now);
            case "archive" -> promotion.archive(now);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROMOTION_ACTION", "Thao tác promotion không hợp lệ");
        }
    }

    private PromotionResponse response(PromotionEntity promotion) {
        List<PromotionResponse.Target> targets = targetRepository.findAllByPromotion_IdOrderById(promotion.getId())
                .stream().map(target -> new PromotionResponse.Target(target.getId(), target.getProductId(),
                        target.getVariantId(), target.getPromotionalPrice(), target.getQuantityLimit(),
                        target.getSoldQuantity())).toList();
        return new PromotionResponse(promotion.getId(), promotion.getOwnerType(), promotion.getShopId(),
                promotion.getName(), promotion.getPromotionType(), promotion.getDiscountType(),
                promotion.getDiscountValue(), promotion.getMaxDiscount(), promotion.getStartsAt(),
                promotion.getEndsAt(), promotion.getStatus(), targets, promotion.getCreatedAt(), promotion.getUpdatedAt());
    }
    private String auditStatusAction(String action) {
        return switch (action) {
            case "activate" -> "ACTIVATED";
            case "pause" -> "PAUSED";
            case "archive" -> "ARCHIVED";
            default -> throw new IllegalArgumentException("Unsupported promotion action");
        };
    }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "PROMOTION_NOT_FOUND", "Không tìm thấy promotion"); }
}
