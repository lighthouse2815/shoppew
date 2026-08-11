package com.shoppew.product.service;

import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.service.InventoryService;
import com.shoppew.product.dto.OptionMetadataRequest;
import com.shoppew.product.dto.OptionValueRequest;
import com.shoppew.product.dto.OptionValueResponse;
import com.shoppew.product.dto.ProductOptionRequest;
import com.shoppew.product.dto.ProductOptionResponse;
import com.shoppew.product.dto.VariantRequest;
import com.shoppew.product.dto.VariantResponse;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductOptionEntity;
import com.shoppew.product.entity.ProductOptionValueEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.entity.VariantStatus;
import com.shoppew.product.repository.ProductOptionRepository;
import com.shoppew.product.repository.ProductOptionValueRepository;
import com.shoppew.product.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductConfigurationService {

    private static final int MAX_OPTIONS = 3;
    private static final int MAX_VARIANTS = 200;
    private final ProductService productService;
    private final ProductOptionRepository optionRepository;
    private final ProductOptionValueRepository valueRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductResponseAssembler assembler;
    private final InventoryService inventoryService;
    private final AppProperties properties;
    private final Clock clock;

    public ProductConfigurationService(
            ProductService productService,
            ProductOptionRepository optionRepository,
            ProductOptionValueRepository valueRepository,
            ProductVariantRepository variantRepository,
            ProductResponseAssembler assembler,
            InventoryService inventoryService,
            AppProperties properties,
            Clock clock) {
        this.productService = productService;
        this.optionRepository = optionRepository;
        this.valueRepository = valueRepository;
        this.variantRepository = variantRepository;
        this.assembler = assembler;
        this.inventoryService = inventoryService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ProductOptionResponse createOption(
            UUID userId,
            UUID shopId,
            UUID productId,
            ProductOptionRequest request) {
        ProductEntity product = editableProduct(userId, shopId, productId);
        if (optionRepository.countByProduct_Id(productId) >= MAX_OPTIONS) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_OPTION_LIMIT", "Mỗi sản phẩm hỗ trợ tối đa 3 tùy chọn");
        }
        String name = request.name().strip();
        if (optionRepository.existsByProduct_IdAndNameIgnoreCase(productId, name)) {
            throw optionNameConflict();
        }
        ensureUniqueValues(request.values());
        ProductOptionEntity option = optionRepository.save(ProductOptionEntity.create(product, name, request.sortOrder()));
        List<ProductOptionValueEntity> values = request.values().stream()
                .map(value -> ProductOptionValueEntity.create(option, value.value().strip(), value.sortOrder()))
                .map(valueRepository::save)
                .toList();
        return optionResponse(option, values);
    }

    @Transactional
    public ProductOptionResponse updateOption(
            UUID userId,
            UUID shopId,
            UUID productId,
            UUID optionId,
            OptionMetadataRequest request) {
        editableProduct(userId, shopId, productId);
        ProductOptionEntity option = requireOption(productId, optionId);
        String name = request.name().strip();
        boolean duplicate = optionRepository.findAllByProduct_IdOrderBySortOrderAscNameAsc(productId).stream()
                .anyMatch(other -> !other.getId().equals(optionId) && other.getName().equalsIgnoreCase(name));
        if (duplicate) throw optionNameConflict();
        option.update(name, request.sortOrder());
        return optionResponse(option, valueRepository.findAllByOption_IdOrderBySortOrderAscValueAsc(optionId));
    }

    @Transactional
    public OptionValueResponse addOptionValue(
            UUID userId,
            UUID shopId,
            UUID productId,
            UUID optionId,
            OptionValueRequest request) {
        editableProduct(userId, shopId, productId);
        ProductOptionEntity option = requireOption(productId, optionId);
        String value = request.value().strip();
        if (valueRepository.existsByOption_IdAndValueIgnoreCase(optionId, value)) {
            throw optionValueConflict();
        }
        ProductOptionValueEntity saved = valueRepository.save(
                ProductOptionValueEntity.create(option, value, request.sortOrder()));
        return new OptionValueResponse(saved.getId(), saved.getValue(), saved.getSortOrder());
    }

    @Transactional
    public OptionValueResponse updateOptionValue(
            UUID userId,
            UUID shopId,
            UUID productId,
            UUID optionId,
            UUID valueId,
            OptionValueRequest request) {
        editableProduct(userId, shopId, productId);
        ProductOptionValueEntity value = requireValue(productId, optionId, valueId);
        String normalized = request.value().strip();
        boolean duplicate = valueRepository.findAllByOption_IdOrderBySortOrderAscValueAsc(optionId).stream()
                .anyMatch(other -> !other.getId().equals(valueId) && other.getValue().equalsIgnoreCase(normalized));
        if (duplicate) throw optionValueConflict();
        value.update(normalized, request.sortOrder());
        return new OptionValueResponse(value.getId(), value.getValue(), value.getSortOrder());
    }

    @Transactional
    public void deleteOptionValue(
            UUID userId,
            UUID shopId,
            UUID productId,
            UUID optionId,
            UUID valueId) {
        editableProduct(userId, shopId, productId);
        ProductOptionValueEntity value = requireValue(productId, optionId, valueId);
        if (variantRepository.existsByOptionValues_Id(valueId)) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTION_VALUE_IN_USE", "Giá trị tùy chọn đang được một phân loại sử dụng");
        }
        valueRepository.delete(value);
    }

    @Transactional
    public void deleteOption(UUID userId, UUID shopId, UUID productId, UUID optionId) {
        editableProduct(userId, shopId, productId);
        ProductOptionEntity option = requireOption(productId, optionId);
        if (variantRepository.existsByOptionValues_Option_Id(optionId)) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_OPTION_IN_USE", "Tùy chọn đang được một phân loại sử dụng");
        }
        optionRepository.delete(option);
    }

    @Transactional
    public VariantResponse createVariant(
            UUID userId,
            UUID shopId,
            UUID productId,
            VariantRequest request) {
        ProductEntity product = editableProduct(userId, shopId, productId);
        if (variantRepository.countByProduct_Id(productId) >= MAX_VARIANTS) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_VARIANT_LIMIT", "Mỗi sản phẩm hỗ trợ tối đa 200 phân loại");
        }
        String sku = request.sku().strip();
        if (variantRepository.existsByShop_IdAndSkuIgnoreCase(shopId, sku)) {
            throw skuConflict();
        }
        Set<ProductOptionValueEntity> selections = validateSelections(productId, request.optionValueIds(), null);
        Money money = validateMoney(request.price(), request.compareAtPrice(), request.currency());
        VariantStatus status = request.status() == null ? VariantStatus.ACTIVE : request.status();
        if (status == VariantStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_VARIANT_STATUS", "Không thể tạo phân loại ở trạng thái lưu trữ");
        }
        Instant now = Instant.now(clock);
        ProductVariantEntity variant = ProductVariantEntity.create(
                product, sku, request.name().strip(), money.price(), money.compareAtPrice(), money.currency(),
                request.weightGrams(), request.lengthMm(), request.widthMm(), request.heightMm(),
                validateOptionalUrl(request.imageUrl()), selections, now);
        if (status != VariantStatus.ACTIVE) {
            variant.update(
                    sku, request.name().strip(), money.price(), money.compareAtPrice(), request.weightGrams(),
                    request.lengthMm(), request.widthMm(), request.heightMm(), validateOptionalUrl(request.imageUrl()),
                    selections, status, now);
        }
        ProductVariantEntity saved = variantRepository.save(variant);
        inventoryService.initialize(saved);
        return assembler.variant(saved);
    }

    @Transactional
    public VariantResponse updateVariant(
            UUID userId,
            UUID shopId,
            UUID productId,
            UUID variantId,
            VariantRequest request) {
        editableProduct(userId, shopId, productId);
        ProductVariantEntity variant = requireVariant(productId, variantId);
        String sku = request.sku().strip();
        if (variantRepository.existsByShop_IdAndSkuIgnoreCaseAndIdNot(shopId, sku, variantId)) {
            throw skuConflict();
        }
        Set<ProductOptionValueEntity> selections = validateSelections(productId, request.optionValueIds(), variantId);
        Money money = validateMoney(request.price(), request.compareAtPrice(), request.currency());
        VariantStatus status = request.status() == null ? variant.getStatus() : request.status();
        variant.update(
                sku, request.name().strip(), money.price(), money.compareAtPrice(), request.weightGrams(),
                request.lengthMm(), request.widthMm(), request.heightMm(), validateOptionalUrl(request.imageUrl()),
                selections, status, Instant.now(clock));
        return assembler.variant(variant);
    }

    @Transactional
    public void archiveVariant(UUID userId, UUID shopId, UUID productId, UUID variantId) {
        editableProduct(userId, shopId, productId);
        requireVariant(productId, variantId).archive(Instant.now(clock));
    }

    private ProductEntity editableProduct(UUID userId, UUID shopId, UUID productId) {
        ProductEntity product = productService.requireOwned(userId, shopId, productId);
        productService.requireSellerEditable(product);
        return product;
    }

    private Set<ProductOptionValueEntity> validateSelections(UUID productId, Set<UUID> ids, UUID excludedVariantId) {
        Set<UUID> selectionIds = ids == null ? Set.of() : Set.copyOf(ids);
        List<ProductOptionEntity> options = optionRepository.findAllByProduct_IdOrderBySortOrderAscNameAsc(productId);
        List<ProductOptionValueEntity> values = valueRepository.findAllByIdIn(selectionIds);
        boolean wrongProduct = values.stream().anyMatch(value -> !value.getProductId().equals(productId));
        Set<UUID> selectedOptions = values.stream().map(ProductOptionValueEntity::getOptionId).collect(Collectors.toSet());
        if (values.size() != selectionIds.size()
                || wrongProduct
                || selectedOptions.size() != values.size()
                || selectedOptions.size() != options.size()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VARIANT_SELECTION",
                    "Phân loại phải chọn đúng một giá trị cho mỗi tùy chọn sản phẩm");
        }
        for (ProductVariantEntity existing : variantRepository.findAllDetailedByProductId(productId)) {
            if (existing.getId().equals(excludedVariantId)) continue;
            Set<UUID> existingIds = existing.getOptionValues().stream()
                    .map(ProductOptionValueEntity::getId)
                    .collect(Collectors.toSet());
            if (existingIds.equals(selectionIds)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "VARIANT_COMBINATION_ALREADY_EXISTS",
                        "Tổ hợp tùy chọn của phân loại đã tồn tại");
            }
        }
        return new LinkedHashSet<>(values);
    }

    private Money validateMoney(BigDecimal price, BigDecimal compareAtPrice, String currency) {
        String normalizedCurrency = currency.strip().toUpperCase(Locale.ROOT);
        if (!properties.currency().equals(normalizedCurrency)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_CURRENCY", "Đơn vị tiền tệ chưa được hỗ trợ");
        }
        BigDecimal normalizedPrice = price.setScale(2);
        BigDecimal normalizedCompare = compareAtPrice == null ? null : compareAtPrice.setScale(2);
        if (normalizedCompare != null && normalizedCompare.compareTo(normalizedPrice) < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_COMPARE_AT_PRICE",
                    "Giá gốc phải lớn hơn hoặc bằng giá bán");
        }
        return new Money(normalizedPrice, normalizedCompare, normalizedCurrency);
    }

    private ProductOptionEntity requireOption(UUID productId, UUID optionId) {
        return optionRepository.findByIdAndProduct_Id(optionId, productId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "PRODUCT_OPTION_NOT_FOUND", "Không tìm thấy tùy chọn sản phẩm"));
    }

    private ProductOptionValueEntity requireValue(UUID productId, UUID optionId, UUID valueId) {
        ProductOptionValueEntity value = valueRepository.findById(valueId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "OPTION_VALUE_NOT_FOUND", "Không tìm thấy giá trị tùy chọn"));
        if (!value.getProductId().equals(productId) || !value.getOptionId().equals(optionId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "OPTION_VALUE_NOT_FOUND", "Không tìm thấy giá trị tùy chọn");
        }
        return value;
    }

    private ProductVariantEntity requireVariant(UUID productId, UUID variantId) {
        return variantRepository.findOwned(variantId, productId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "PRODUCT_VARIANT_NOT_FOUND", "Không tìm thấy phân loại sản phẩm"));
    }

    private ProductOptionResponse optionResponse(ProductOptionEntity option, List<ProductOptionValueEntity> values) {
        return new ProductOptionResponse(
                option.getId(), option.getName(), option.getSortOrder(),
                values.stream()
                        .map(value -> new OptionValueResponse(value.getId(), value.getValue(), value.getSortOrder()))
                        .toList());
    }

    private void ensureUniqueValues(List<OptionValueRequest> values) {
        Set<String> normalized = new HashSet<>();
        if (values.stream().map(value -> value.value().strip().toLowerCase(Locale.ROOT)).anyMatch(value -> !normalized.add(value))) {
            throw optionValueConflict();
        }
    }

    private String validateOptionalUrl(String value) {
        String normalized = value == null || value.isBlank() ? null : value.strip();
        if (normalized == null) return null;
        try {
            URI uri = URI.create(normalized);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_URL", "URL ảnh không hợp lệ");
        }
    }

    private ApiException optionNameConflict() {
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_OPTION_NAME_ALREADY_USED", "Tên tùy chọn đã được sử dụng");
    }

    private ApiException optionValueConflict() {
        return new ApiException(HttpStatus.CONFLICT, "OPTION_VALUE_ALREADY_USED", "Giá trị tùy chọn đã được sử dụng");
    }

    private ApiException skuConflict() {
        return new ApiException(HttpStatus.CONFLICT, "SHOP_SKU_ALREADY_USED", "SKU đã được sử dụng trong cửa hàng");
    }

    private record Money(BigDecimal price, BigDecimal compareAtPrice, String currency) {}
}
