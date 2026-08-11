package com.shoppew.product.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.media.ImageUploadValidator;
import com.shoppew.media.StorageService;
import com.shoppew.product.dto.ProductImageResponse;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductImageEntity;
import com.shoppew.product.repository.ProductImageRepository;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductMediaService {

    private static final int MAX_PRODUCT_IMAGES = 10;
    private final ProductService productService;
    private final ProductImageRepository imageRepository;
    private final ImageUploadValidator imageValidator;
    private final StorageService storageService;
    private final ProductResponseAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ProductMediaService(
            ProductService productService,
            ProductImageRepository imageRepository,
            ImageUploadValidator imageValidator,
            StorageService storageService,
            ProductResponseAssembler assembler,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.productService = productService;
        this.imageRepository = imageRepository;
        this.imageValidator = imageValidator;
        this.storageService = storageService;
        this.assembler = assembler;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ProductImageResponse upload(
            UUID userId,
            UUID shopId,
            UUID productId,
            MultipartFile file,
            String altText,
            int sortOrder,
            boolean primary) {
        ProductEntity product = productService.requireOwned(userId, shopId, productId);
        productService.requireSellerEditable(product);
        long imageCount = imageRepository.countByProduct_Id(productId);
        if (imageCount >= MAX_PRODUCT_IMAGES) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_IMAGE_LIMIT", "Mỗi sản phẩm hỗ trợ tối đa 10 ảnh");
        }
        ImageUploadValidator.ValidatedImage image = imageValidator.validate(file);
        String objectKey = "products/" + shopId + "/" + productId + "/" + UUID.randomUUID() + "." + image.extension();
        StorageService.StoredObject stored = storageService.upload(
                objectKey,
                new ByteArrayInputStream(image.bytes()),
                image.bytes().length,
                image.contentType());
        boolean makePrimary = primary || imageCount == 0;
        try {
            if (makePrimary) {
                imageRepository.clearPrimary(productId);
            }
            ProductImageEntity entity = ProductImageEntity.create(
                    product,
                    stored.objectKey(),
                    stored.publicUrl(),
                    normalizeAltText(altText, product.getName()),
                    Math.max(sortOrder, 0),
                    makePrimary,
                    Instant.now(clock));
            return assembler.image(imageRepository.saveAndFlush(entity));
        } catch (RuntimeException exception) {
            storageService.delete(stored.objectKey());
            throw exception;
        }
    }

    @Transactional
    public void delete(UUID userId, UUID shopId, UUID productId, UUID imageId) {
        ProductEntity product = productService.requireOwned(userId, shopId, productId);
        productService.requireSellerEditable(product);
        ProductImageEntity image = imageRepository.findByIdAndProduct_Id(imageId, productId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PRODUCT_IMAGE_NOT_FOUND", "Không tìm thấy ảnh sản phẩm"));
        boolean wasPrimary = image.isPrimary();
        String objectKey = image.getObjectKey();
        imageRepository.delete(image);
        imageRepository.flush();
        if (wasPrimary) {
            List<ProductImageEntity> remaining = imageRepository
                    .findAllByProduct_IdOrderBySortOrderAscCreatedAtAsc(productId);
            if (!remaining.isEmpty()) {
                remaining.getFirst().makePrimary();
            }
        }
        eventPublisher.publishEvent(new ProductImageDeletedEvent(objectKey));
    }

    private String normalizeAltText(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.strip();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }
}
