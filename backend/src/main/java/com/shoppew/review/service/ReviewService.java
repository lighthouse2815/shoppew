package com.shoppew.review.service;

import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.media.ImageUploadValidator;
import com.shoppew.media.StorageService;
import com.shoppew.order.entity.OrderItemEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.repository.OrderItemRepository;
import com.shoppew.review.dto.ReviewRequest;
import com.shoppew.review.dto.ReviewResponse;
import com.shoppew.review.dto.ReviewUpdateRequest;
import com.shoppew.review.entity.ReviewEntity;
import com.shoppew.review.entity.ReviewImageEntity;
import com.shoppew.review.entity.ReviewStatus;
import com.shoppew.review.repository.ReviewImageRepository;
import com.shoppew.review.repository.ReviewRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserProfileEntity;
import com.shoppew.user.repository.UserProfileRepository;
import com.shoppew.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReviewService {
    private static final int MAX_IMAGES = 5;
    private final ReviewRepository repository; private final ReviewImageRepository imageRepository;
    private final OrderItemRepository orderItemRepository; private final UserRepository userRepository;
    private final UserProfileRepository profileRepository; private final ShopAccessService shopAccessService;
    private final ImageUploadValidator imageValidator; private final StorageService storageService; private final Clock clock;
    public ReviewService(ReviewRepository repository, ReviewImageRepository imageRepository,
            OrderItemRepository orderItemRepository, UserRepository userRepository,
            UserProfileRepository profileRepository, ShopAccessService shopAccessService,
            ImageUploadValidator imageValidator, StorageService storageService, Clock clock) {
        this.repository = repository; this.imageRepository = imageRepository; this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository; this.profileRepository = profileRepository; this.shopAccessService = shopAccessService;
        this.imageValidator = imageValidator; this.storageService = storageService; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> publicProduct(UUID productId, int page, int size) {
        return PageResponse.from(repository.findAllByProduct_IdAndStatus(productId, ReviewStatus.PUBLISHED,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::response);
    }
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> mine(UUID userId, int page, int size) {
        return PageResponse.from(repository.findAllByUser_Id(userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::response);
    }
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> sellerList(UUID userId, UUID shopId, int page, int size) {
        shopAccessService.requireActiveMember(userId, shopId);
        return PageResponse.from(repository.findAllByShop_Id(shopId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::response);
    }

    @Transactional
    public ReviewResponse create(UUID userId, ReviewRequest request) {
        OrderItemEntity item = orderItemRepository.findReviewable(request.orderItemId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_ITEM_NOT_FOUND", "Không tìm thấy sản phẩm đã mua"));
        if (item.getOrder().getStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_PURCHASE_NOT_ELIGIBLE", "Đơn hàng phải hoàn tất trước khi đánh giá");
        }
        if (item.getProduct() == null) throw new ApiException(HttpStatus.CONFLICT, "REVIEW_PRODUCT_REMOVED", "Sản phẩm không còn để đánh giá");
        if (repository.existsByOrderItem_Id(item.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "Sản phẩm trong đơn đã được đánh giá");
        }
        UserEntity user = userRepository.getReferenceById(userId);
        ReviewEntity review = repository.save(ReviewEntity.create(
                user, item, request.rating(), normalize(request.content()), Instant.now(clock)));
        recompute(review); return response(review);
    }

    @Transactional
    public ReviewResponse update(UUID userId, UUID reviewId, ReviewUpdateRequest request) {
        ReviewEntity review = owned(userId, reviewId);
        if (review.getStatus() == ReviewStatus.REMOVED) throw new ApiException(
                HttpStatus.CONFLICT, "REVIEW_REMOVED", "Đánh giá đã bị gỡ và không thể sửa");
        review.update(request.rating(), normalize(request.content()), Instant.now(clock));
        recompute(review); return response(review);
    }

    @Transactional
    public ReviewResponse sellerReply(UUID userId, UUID shopId, UUID reviewId, String reply) {
        shopAccessService.requireActiveMember(userId, shopId);
        ReviewEntity review = repository.findByIdAndShop_Id(reviewId, shopId).orElseThrow(this::notFound);
        review.reply(reply.strip(), Instant.now(clock)); return response(review);
    }

    @Transactional
    public ReviewResponse moderate(UUID reviewId, String action) {
        ReviewEntity review = repository.findById(reviewId).orElseThrow(this::notFound);
        ReviewStatus next = switch (action) {
            case "publish" -> ReviewStatus.PUBLISHED;
            case "hide" -> ReviewStatus.HIDDEN;
            case "remove" -> ReviewStatus.REMOVED;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REVIEW_ACTION", "Thao tác đánh giá không hợp lệ");
        };
        review.changeStatus(next, Instant.now(clock)); recompute(review); return response(review);
    }

    @Transactional
    public ReviewResponse upload(UUID userId, UUID reviewId, MultipartFile file, int sortOrder) {
        ReviewEntity review = owned(userId, reviewId);
        if (imageRepository.countByReview_Id(reviewId) >= MAX_IMAGES) throw new ApiException(
                HttpStatus.CONFLICT, "REVIEW_IMAGE_LIMIT", "Mỗi đánh giá hỗ trợ tối đa 5 ảnh");
        ImageUploadValidator.ValidatedImage image = imageValidator.validate(file);
        String key = "reviews/" + userId + "/" + reviewId + "/" + UUID.randomUUID() + "." + image.extension();
        StorageService.StoredObject stored = storageService.upload(key, new ByteArrayInputStream(image.bytes()),
                image.bytes().length, image.contentType());
        try {
            imageRepository.saveAndFlush(ReviewImageEntity.create(
                    review, stored.objectKey(), stored.publicUrl(), Math.max(sortOrder, 0), Instant.now(clock)));
            return response(review);
        } catch (RuntimeException exception) {
            storageService.delete(stored.objectKey()); throw exception;
        }
    }

    @Transactional
    public void deleteImage(UUID userId, UUID reviewId, UUID imageId) {
        owned(userId, reviewId);
        ReviewImageEntity image = imageRepository.findByIdAndReview_Id(imageId, reviewId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REVIEW_IMAGE_NOT_FOUND", "Không tìm thấy ảnh đánh giá"));
        imageRepository.delete(image); imageRepository.flush(); storageService.delete(image.getObjectKey());
    }

    private ReviewEntity owned(UUID userId, UUID reviewId) {
        return repository.findByIdAndUser_Id(reviewId, userId).orElseThrow(this::notFound);
    }
    private void recompute(ReviewEntity review) {
        repository.flush(); repository.recomputeProduct(review.getProductId()); repository.recomputeShop(review.getShopId());
    }
    private ReviewResponse response(ReviewEntity review) {
        UserProfileEntity profile = profileRepository.findById(review.getUserId()).orElse(null);
        List<ReviewResponse.Image> images = imageRepository.findAllByReview_IdOrderBySortOrderAsc(review.getId()).stream()
                .map(image -> new ReviewResponse.Image(image.getId(), image.getUrl(), image.getSortOrder())).toList();
        return new ReviewResponse(review.getId(), review.getUserId(), profile == null ? "Shoppew customer" : profile.getDisplayName(),
                profile == null ? null : profile.getAvatarUrl(), review.getShopId(), review.getProductId(),
                review.getOrderItemId(), review.getRating(), review.getContent(), review.getStatus(),
                review.getSellerReply(), review.getSellerRepliedAt(), images, review.getCreatedAt(), review.getUpdatedAt());
    }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Không tìm thấy đánh giá"); }
}
