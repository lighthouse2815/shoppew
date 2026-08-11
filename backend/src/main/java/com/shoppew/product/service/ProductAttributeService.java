package com.shoppew.product.service;

import com.shoppew.catalog.entity.CategoryEntity;
import com.shoppew.catalog.repository.CategoryRepository;
import com.shoppew.common.exception.ApiException;
import com.shoppew.product.dto.AttributeDefinitionRequest;
import com.shoppew.product.dto.AttributeDefinitionResponse;
import com.shoppew.product.dto.AttributeValueInput;
import com.shoppew.product.dto.ProductAttributeResponse;
import com.shoppew.product.dto.ProductAttributesRequest;
import com.shoppew.product.entity.AttributeValueType;
import com.shoppew.product.entity.ProductAttributeEntity;
import com.shoppew.product.entity.ProductAttributeValueEntity;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.repository.ProductAttributeRepository;
import com.shoppew.product.repository.ProductAttributeValueRepository;
import com.shoppew.shop.service.ShopAccessService;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductAttributeService {

    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository valueRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;
    private final ShopAccessService shopAccessService;

    public ProductAttributeService(
            ProductAttributeRepository attributeRepository,
            ProductAttributeValueRepository valueRepository,
            CategoryRepository categoryRepository,
            ProductService productService,
            ShopAccessService shopAccessService) {
        this.attributeRepository = attributeRepository;
        this.valueRepository = valueRepository;
        this.categoryRepository = categoryRepository;
        this.productService = productService;
        this.shopAccessService = shopAccessService;
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> sellerDefinitions(UUID userId, UUID shopId, UUID categoryId) {
        shopAccessService.requireActiveMember(userId, shopId);
        return definitions(categoryId);
    }

    @Transactional(readOnly = true)
    public List<AttributeDefinitionResponse> definitions(UUID categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục");
        }
        List<ProductAttributeEntity> attributes = categoryId == null
                ? attributeRepository.findAll()
                : attributeRepository.findApplicable(categoryId);
        return attributes.stream().map(this::definitionResponse).toList();
    }

    @Transactional
    public AttributeDefinitionResponse createDefinition(AttributeDefinitionRequest request) {
        CategoryEntity category = request.categoryId() == null
                ? null
                : categoryRepository.findById(request.categoryId()).orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục"));
        String name = request.name().strip();
        if (attributeRepository.existsDuplicate(request.categoryId(), name, null)) {
            throw duplicate();
        }
        ProductAttributeEntity attribute = attributeRepository.save(ProductAttributeEntity.create(
                category, name, request.valueType(), request.required(), request.sortOrder()));
        return definitionResponse(attribute);
    }

    @Transactional
    public AttributeDefinitionResponse updateDefinition(UUID attributeId, AttributeDefinitionRequest request) {
        ProductAttributeEntity attribute = requireDefinition(attributeId);
        if (!java.util.Objects.equals(attribute.getCategoryId(), request.categoryId())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ATTRIBUTE_CATEGORY_IMMUTABLE",
                    "Không thể chuyển thuộc tính sang danh mục khác sau khi tạo");
        }
        String name = request.name().strip();
        if (attributeRepository.existsDuplicate(request.categoryId(), name, attributeId)) {
            throw duplicate();
        }
        attribute.update(name, request.valueType(), request.required(), request.sortOrder());
        return definitionResponse(attribute);
    }

    @Transactional
    public List<ProductAttributeResponse> setProductValues(
            UUID userId,
            UUID shopId,
            UUID productId,
            ProductAttributesRequest request) {
        ProductEntity product = productService.requireOwned(userId, shopId, productId);
        productService.requireSellerEditable(product);
        Set<UUID> requestedIds = new HashSet<>();
        if (request.values().stream().map(AttributeValueInput::attributeId).anyMatch(id -> !requestedIds.add(id))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATE_ATTRIBUTE_VALUE", "Thuộc tính bị lặp trong yêu cầu");
        }

        Map<UUID, ProductAttributeEntity> applicable = attributeRepository.findApplicable(product.getCategory().getId())
                .stream().collect(Collectors.toMap(ProductAttributeEntity::getId, Function.identity()));
        if (!applicable.keySet().containsAll(requestedIds)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ATTRIBUTE_NOT_APPLICABLE",
                    "Có thuộc tính không áp dụng cho danh mục sản phẩm");
        }

        valueRepository.deleteAllByProduct_Id(productId);
        valueRepository.flush();
        for (AttributeValueInput input : request.values()) {
            ProductAttributeEntity attribute = applicable.get(input.attributeId());
            String value = validateValue(attribute.getValueType(), input.value());
            valueRepository.save(ProductAttributeValueEntity.create(product, attribute, value));
        }
        Map<UUID, String> values = request.values().stream()
                .collect(Collectors.toMap(AttributeValueInput::attributeId, input -> input.value().strip()));
        return applicable.values().stream()
                .sorted(java.util.Comparator.comparingInt(ProductAttributeEntity::getSortOrder))
                .map(attribute -> new ProductAttributeResponse(
                        attribute.getId(), attribute.getName(), attribute.getValueType().name(),
                        attribute.isRequired(), values.get(attribute.getId())))
                .toList();
    }

    private String validateValue(AttributeValueType type, String candidate) {
        String value = candidate.strip();
        try {
            switch (type) {
                case NUMBER -> new BigDecimal(value);
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException();
                    }
                    value = value.toLowerCase(java.util.Locale.ROOT);
                }
                case TEXT, SELECT -> { }
            }
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ATTRIBUTE_VALUE",
                    "Giá trị thuộc tính không đúng kiểu dữ liệu");
        }
        return value;
    }

    private ProductAttributeEntity requireDefinition(UUID id) {
        return attributeRepository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "PRODUCT_ATTRIBUTE_NOT_FOUND", "Không tìm thấy thuộc tính sản phẩm"));
    }

    private AttributeDefinitionResponse definitionResponse(ProductAttributeEntity attribute) {
        return new AttributeDefinitionResponse(
                attribute.getId(), attribute.getCategoryId(), attribute.getName(), attribute.getValueType().name(),
                attribute.isRequired(), attribute.getSortOrder());
    }

    private ApiException duplicate() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "PRODUCT_ATTRIBUTE_ALREADY_EXISTS",
                "Tên thuộc tính đã tồn tại trong phạm vi danh mục");
    }
}
