package com.shoppew.voucher.service;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.catalog.entity.CategoryEntity;
import com.shoppew.catalog.repository.CategoryRepository;
import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.repository.ShopRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.voucher.dto.VoucherRequest;
import com.shoppew.voucher.dto.VoucherResponse;
import com.shoppew.voucher.entity.DiscountType;
import com.shoppew.voucher.entity.VoucherEntity;
import com.shoppew.voucher.entity.VoucherOwnerType;
import com.shoppew.voucher.entity.VoucherStatus;
import com.shoppew.voucher.entity.VoucherType;
import com.shoppew.voucher.repository.VoucherRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherManagementService {
    private final VoucherRepository voucherRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ShopAccessService shopAccessService;
    private final AdminAuditService audit;
    private final AppProperties properties;
    private final Clock clock;

    public VoucherManagementService(VoucherRepository voucherRepository, ProductRepository productRepository,
            CategoryRepository categoryRepository, ShopRepository shopRepository,
            ShopAccessService shopAccessService, AdminAuditService audit,
            AppProperties properties, Clock clock) {
        this.voucherRepository = voucherRepository; this.productRepository = productRepository;
        this.categoryRepository = categoryRepository; this.shopRepository = shopRepository;
        this.shopAccessService = shopAccessService; this.audit = audit;
        this.properties = properties; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> sellerList(UUID userId, UUID shopId) {
        shopAccessService.requireActiveMember(userId, shopId);
        return voucherRepository.findAllByShop_IdOrderByCreatedAtDesc(shopId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> adminList() {
        return voucherRepository.findAllByShopIsNullOrderByCreatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional
    public VoucherResponse sellerCreate(UUID userId, UUID shopId, VoucherRequest request) {
        shopAccessService.requireActiveMember(userId, shopId);
        ShopEntity shop = shopRepository.findById(shopId).orElseThrow(this::notFound);
        validateRequest(request, VoucherOwnerType.SHOP, shopId, null);
        return response(voucherRepository.save(create(VoucherOwnerType.SHOP, shop, request)));
    }

    @Transactional
    public VoucherResponse adminCreate(VoucherRequest request) {
        validateRequest(request, VoucherOwnerType.PLATFORM, null, null);
        VoucherResponse created = response(voucherRepository.save(create(VoucherOwnerType.PLATFORM, null, request)));
        audit.record(null, "PLATFORM_VOUCHER_CREATED", "VOUCHER", created.id(), null, created);
        return created;
    }

    @Transactional
    public VoucherResponse sellerUpdate(UUID userId, UUID shopId, UUID voucherId, VoucherRequest request) {
        shopAccessService.requireActiveMember(userId, shopId);
        VoucherEntity voucher = voucherRepository.findById(voucherId).filter(v -> shopId.equals(v.getShopId()))
                .orElseThrow(this::notFound);
        return update(voucher, request, VoucherOwnerType.SHOP, shopId);
    }

    @Transactional
    public VoucherResponse adminUpdate(UUID voucherId, VoucherRequest request) {
        VoucherEntity voucher = voucherRepository.findById(voucherId).filter(v -> v.getShopId() == null)
                .orElseThrow(this::notFound);
        VoucherResponse before = response(voucher);
        VoucherResponse updated = update(voucher, request, VoucherOwnerType.PLATFORM, null);
        audit.record(null, "PLATFORM_VOUCHER_UPDATED", "VOUCHER", voucherId, before, updated);
        return updated;
    }

    @Transactional
    public VoucherResponse sellerStatus(UUID userId, UUID shopId, UUID voucherId, String action) {
        shopAccessService.requireActiveMember(userId, shopId);
        VoucherEntity voucher = voucherRepository.findById(voucherId).filter(v -> shopId.equals(v.getShopId()))
                .orElseThrow(this::notFound);
        changeStatus(voucher, action); return response(voucher);
    }

    @Transactional
    public VoucherResponse adminStatus(UUID voucherId, String action) {
        VoucherEntity voucher = voucherRepository.findById(voucherId).filter(v -> v.getShopId() == null)
                .orElseThrow(this::notFound);
        VoucherResponse before = response(voucher);
        changeStatus(voucher, action);
        VoucherResponse updated = response(voucher);
        audit.record(null, "PLATFORM_VOUCHER_" + auditStatusAction(action),
                "VOUCHER", voucherId, before, updated);
        return updated;
    }

    private VoucherEntity create(VoucherOwnerType owner, ShopEntity shop, VoucherRequest request) {
        return VoucherEntity.create(owner, shop, normalizeCode(request.code()), request.name().strip(),
                request.voucherType(), request.discountType(), request.discountValue(), request.maxDiscount(),
                request.minimumSpend(), request.currency(), request.startsAt(), request.endsAt(),
                request.totalQuantity(), request.perUserLimit(), products(request.productIds()),
                categories(request.categoryIds()), safeSet(request.paymentProviders()), Instant.now(clock));
    }

    private VoucherResponse update(VoucherEntity voucher, VoucherRequest request,
            VoucherOwnerType owner, UUID shopId) {
        if (voucher.getStatus() == VoucherStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_ARCHIVED", "Voucher đã lưu trữ không thể sửa");
        }
        validateRequest(request, owner, shopId, voucher.getId());
        if (request.totalQuantity() < voucher.getUsedQuantity()) {
            throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_QUANTITY_BELOW_USAGE",
                    "Số lượng voucher không thể nhỏ hơn lượng đã dùng");
        }
        voucher.update(normalizeCode(request.code()), request.name().strip(), request.voucherType(),
                request.discountType(), request.discountValue(), request.maxDiscount(), request.minimumSpend(),
                request.currency(), request.startsAt(), request.endsAt(), request.totalQuantity(),
                request.perUserLimit(), products(request.productIds()), categories(request.categoryIds()),
                safeSet(request.paymentProviders()), Instant.now(clock));
        return response(voucher);
    }

    private void validateRequest(VoucherRequest request, VoucherOwnerType owner, UUID shopId, UUID currentId) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VOUCHER_WINDOW", "Thời gian kết thúc phải sau bắt đầu");
        }
        if (!properties.currency().equals(request.currency())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VOUCHER_CURRENCY_UNSUPPORTED", "Tiền tệ voucher chưa được hỗ trợ");
        }
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VOUCHER_PERCENTAGE", "Phần trăm giảm không vượt quá 100");
        }
        String code = normalizeCode(request.code());
        boolean duplicate = currentId == null ? voucherRepository.existsByCodeIgnoreCase(code)
                : voucherRepository.existsByCodeIgnoreCaseAndIdNot(code, currentId);
        if (duplicate) throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_CODE_EXISTS", "Mã voucher đã tồn tại");
        if (owner == VoucherOwnerType.SHOP && request.voucherType() == VoucherType.PLATFORM) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VOUCHER_OWNER", "Seller không thể tạo voucher nền tảng");
        }
        Set<ProductEntity> products = products(request.productIds());
        if (shopId != null && products.stream().anyMatch(product -> !shopId.equals(product.getShopId()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VOUCHER_PRODUCT_OUTSIDE_SHOP", "Sản phẩm voucher phải thuộc đúng shop");
        }
        if (request.voucherType() == VoucherType.PRODUCT && products.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VOUCHER_PRODUCTS_REQUIRED", "Voucher sản phẩm cần ít nhất một sản phẩm");
        }
        if (request.voucherType() == VoucherType.CATEGORY && categories(request.categoryIds()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VOUCHER_CATEGORIES_REQUIRED", "Voucher danh mục cần ít nhất một danh mục");
        }
    }

    private void changeStatus(VoucherEntity voucher, String action) {
        Instant now = Instant.now(clock);
        switch (action) {
            case "activate" -> {
                if (!voucher.getEndsAt().isAfter(now) || voucher.getTotalQuantity() <= voucher.getUsedQuantity())
                    throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_NOT_ACTIVATABLE", "Voucher đã hết hạn hoặc hết lượt");
                voucher.activate(now);
            }
            case "pause" -> voucher.pause(now);
            case "archive" -> voucher.archive(now);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VOUCHER_ACTION", "Thao tác voucher không hợp lệ");
        }
    }

    private Set<ProductEntity> products(Set<UUID> ids) {
        Set<UUID> safe = safeSet(ids);
        List<ProductEntity> values = productRepository.findAllById(safe);
        if (values.size() != safe.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "VOUCHER_PRODUCT_NOT_FOUND", "Có sản phẩm voucher không tồn tại");
        return new LinkedHashSet<>(values);
    }
    private Set<CategoryEntity> categories(Set<UUID> ids) {
        Set<UUID> safe = safeSet(ids);
        List<CategoryEntity> values = categoryRepository.findAllById(safe);
        if (values.size() != safe.size()) throw new ApiException(HttpStatus.BAD_REQUEST, "VOUCHER_CATEGORY_NOT_FOUND", "Có danh mục voucher không tồn tại");
        return new LinkedHashSet<>(values);
    }
    private <T> Set<T> safeSet(Set<T> values) { return values == null ? Set.of() : new LinkedHashSet<>(values); }
    private String normalizeCode(String code) { return code.strip().toUpperCase(java.util.Locale.ROOT); }
    private String auditStatusAction(String action) {
        return switch (action) {
            case "activate" -> "ACTIVATED";
            case "pause" -> "PAUSED";
            case "archive" -> "ARCHIVED";
            default -> throw new IllegalArgumentException("Unsupported voucher action");
        };
    }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "VOUCHER_NOT_FOUND", "Không tìm thấy voucher"); }
    private VoucherResponse response(VoucherEntity voucher) {
        return new VoucherResponse(voucher.getId(), voucher.getOwnerType(), voucher.getShopId(), voucher.getCode(),
                voucher.getName(), voucher.getVoucherType(), voucher.getDiscountType(), voucher.getDiscountValue(),
                voucher.getMaxDiscount(), voucher.getMinimumSpend(), voucher.getCurrency(), voucher.getStartsAt(),
                voucher.getEndsAt(), voucher.getTotalQuantity(), voucher.getUsedQuantity(), voucher.getPerUserLimit(),
                voucher.getStatus(), voucher.getProducts().stream().map(ProductEntity::getId).collect(Collectors.toSet()),
                voucher.getCategories().stream().map(CategoryEntity::getId).collect(Collectors.toSet()),
                voucher.getPaymentProviders(), voucher.getCreatedAt(), voucher.getUpdatedAt());
    }
}
