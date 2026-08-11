package com.shoppew.product.service;

import com.shoppew.product.dto.OptionValueResponse;
import com.shoppew.product.dto.ProductAttributeResponse;
import com.shoppew.product.dto.ProductDetailResponse;
import com.shoppew.product.dto.ProductImageResponse;
import com.shoppew.product.dto.ProductOptionResponse;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.dto.VariantResponse;
import com.shoppew.product.dto.VariantSelectionResponse;
import com.shoppew.catalog.entity.BrandEntity;
import com.shoppew.product.entity.ProductAttributeEntity;
import com.shoppew.product.entity.ProductAttributeValueEntity;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductImageEntity;
import com.shoppew.product.entity.ProductOptionEntity;
import com.shoppew.product.entity.ProductOptionValueEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.entity.VariantStatus;
import com.shoppew.product.repository.ProductAttributeRepository;
import com.shoppew.product.repository.ProductAttributeValueRepository;
import com.shoppew.product.repository.ProductImageRepository;
import com.shoppew.product.repository.ProductOptionRepository;
import com.shoppew.product.repository.ProductOptionValueRepository;
import com.shoppew.product.repository.ProductVariantRepository;
import com.shoppew.promotion.service.PromotionPricingService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductResponseAssembler {

    private final ProductImageRepository imageRepository;
    private final ProductOptionRepository optionRepository;
    private final ProductOptionValueRepository optionValueRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final PromotionPricingService promotionPricing;

    public ProductResponseAssembler(
            ProductImageRepository imageRepository,
            ProductOptionRepository optionRepository,
            ProductOptionValueRepository optionValueRepository,
            ProductVariantRepository variantRepository,
            ProductAttributeRepository attributeRepository,
            ProductAttributeValueRepository attributeValueRepository,
            PromotionPricingService promotionPricing) {
        this.imageRepository = imageRepository;
        this.optionRepository = optionRepository;
        this.optionValueRepository = optionValueRepository;
        this.variantRepository = variantRepository;
        this.attributeRepository = attributeRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.promotionPricing = promotionPricing;
    }

    public ProductDetailResponse detail(ProductEntity product) {
        List<ProductImageResponse> images = imageRepository
                .findAllByProduct_IdOrderBySortOrderAscCreatedAtAsc(product.getId())
                .stream().map(this::image).toList();
        List<ProductOptionResponse> options = optionResponses(product.getId());
        List<ProductVariantEntity> variantEntities = variantRepository.findAllDetailedByProductId(product.getId());
        Map<UUID, PromotionPricingService.PriceDecision> prices = promotionPricing.variantPrices(variantEntities);
        List<VariantResponse> variants = variantEntities.stream()
                .map(variant -> variant(variant, prices.get(variant.getId()))).toList();
        List<ProductAttributeResponse> attributes = attributeResponses(product);
        BrandEntity brand = product.getBrand();
        return new ProductDetailResponse(
                product.getId(),
                product.getShopId(),
                product.getShop().getName(),
                product.getShop().getSlug(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                brand == null ? null : brand.getId(),
                brand == null ? null : brand.getName(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                product.getStatus().name(),
                product.getModerationNote(),
                product.getRatingAverage(),
                product.getReviewCount(),
                product.getSoldCount(),
                product.getPublishedAt(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                images,
                options,
                variants,
                attributes);
    }

    public List<ProductSummaryResponse> summaries(List<ProductEntity> products) {
        if (products.isEmpty()) return List.of();
        List<UUID> ids = products.stream().map(ProductEntity::getId).toList();
        Map<UUID, List<ProductImageEntity>> images = imageRepository
                .findAllByProduct_IdInOrderBySortOrderAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(ProductImageEntity::getProductId));
        Map<UUID, List<ProductVariantEntity>> variants = variantRepository
                .findAllDetailedByProductIdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(ProductVariantEntity::getProductId));
        Map<UUID, PromotionPricingService.PriceDecision> prices = promotionPricing.variantPrices(
                variants.values().stream().flatMap(List::stream).toList());
        return products.stream()
                .map(product -> summary(product, images.getOrDefault(product.getId(), List.of()),
                        variants.getOrDefault(product.getId(), List.of()), prices))
                .toList();
    }

    public ProductImageResponse image(ProductImageEntity image) {
        return new ProductImageResponse(
                image.getId(), image.getUrl(), image.getAltText(), image.getSortOrder(),
                image.isPrimary(), image.getCreatedAt());
    }

    public VariantResponse variant(ProductVariantEntity variant) {
        return variant(variant, promotionPricing.variantPrices(List.of(variant)).get(variant.getId()));
    }

    private VariantResponse variant(ProductVariantEntity variant, PromotionPricingService.PriceDecision price) {
        List<VariantSelectionResponse> selections = variant.getOptionValues().stream()
                .sorted(Comparator.comparingInt(value -> value.getOption().getSortOrder()))
                .map(value -> new VariantSelectionResponse(
                        value.getOptionId(), value.getOption().getName(), value.getId(), value.getValue()))
                .toList();
        return new VariantResponse(
                variant.getId(), variant.getSku(), variant.getName(), price.unitPrice(), variant.getPrice(),
                price.promotionId(), price.promotionName(),
                variant.getCompareAtPrice(), variant.getCurrency(), variant.getWeightGrams(),
                variant.getLengthMm(), variant.getWidthMm(), variant.getHeightMm(), variant.getImageUrl(),
                variant.getStatus().name(), selections, variant.getCreatedAt(), variant.getUpdatedAt());
    }

    private List<ProductOptionResponse> optionResponses(UUID productId) {
        return optionRepository.findAllByProduct_IdOrderBySortOrderAscNameAsc(productId).stream()
                .map(option -> new ProductOptionResponse(
                        option.getId(), option.getName(), option.getSortOrder(),
                        optionValueRepository.findAllByOption_IdOrderBySortOrderAscValueAsc(option.getId())
                                .stream()
                                .map(value -> new OptionValueResponse(value.getId(), value.getValue(), value.getSortOrder()))
                                .toList()))
                .toList();
    }

    private List<ProductAttributeResponse> attributeResponses(ProductEntity product) {
        Map<UUID, String> values = attributeValueRepository.findAllByProduct_Id(product.getId()).stream()
                .collect(Collectors.toMap(value -> value.getAttribute().getId(), ProductAttributeValueEntity::getValueText));
        return attributeRepository.findApplicable(product.getCategory().getId()).stream()
                .map(attribute -> new ProductAttributeResponse(
                        attribute.getId(), attribute.getName(), attribute.getValueType().name(),
                        attribute.isRequired(), values.get(attribute.getId())))
                .toList();
    }

    private ProductSummaryResponse summary(
            ProductEntity product,
            List<ProductImageEntity> images,
            List<ProductVariantEntity> variants,
            Map<UUID, PromotionPricingService.PriceDecision> prices) {
        ProductImageEntity primary = images.stream().filter(ProductImageEntity::isPrimary).findFirst()
                .orElse(images.isEmpty() ? null : images.getFirst());
        ProductVariantEntity cheapest = variants.stream()
                .filter(variant -> variant.getStatus() == VariantStatus.ACTIVE)
                .min(Comparator.comparing(variant -> prices.get(variant.getId()).unitPrice()))
                .orElse(null);
        ProductVariantEntity originalCheapest = variants.stream()
                .filter(variant -> variant.getStatus() == VariantStatus.ACTIVE)
                .min(Comparator.comparing(ProductVariantEntity::getPrice)).orElse(null);
        BrandEntity brand = product.getBrand();
        return new ProductSummaryResponse(
                product.getId(), product.getShopId(), product.getShop().getName(),
                product.getCategory().getId(), product.getCategory().getName(),
                brand == null ? null : brand.getId(), brand == null ? null : brand.getName(),
                product.getName(), product.getSlug(), product.getShortDescription(), product.getStatus().name(),
                primary == null ? null : primary.getUrl(),
                cheapest == null ? null : prices.get(cheapest.getId()).unitPrice(),
                originalCheapest == null ? null : originalCheapest.getPrice(),
                cheapest == null ? null : cheapest.getCurrency(),
                product.getRatingAverage(), product.getReviewCount(), product.getSoldCount(),
                product.getPublishedAt(), product.getCreatedAt());
    }

}
