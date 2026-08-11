package com.shoppew.shop.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.text.SlugService;
import com.shoppew.shop.dto.CreateShopRequest;
import com.shoppew.shop.dto.ShopResponse;
import com.shoppew.shop.dto.UpdateShopRequest;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.entity.ShopMemberEntity;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.shop.repository.ShopMemberRepository;
import com.shoppew.shop.repository.ShopRepository;
import com.shoppew.shop.repository.ShopSettingsRepository;
import com.shoppew.shop.entity.ShopSettingsEntity;
import com.shoppew.common.config.AppProperties;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserRole;
import com.shoppew.user.entity.UserStatus;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ShopSettingsRepository settingsRepository;
    private final ShopAccessService accessService;
    private final SlugService slugService;
    private final AppProperties properties;
    private final Clock clock;

    public ShopService(
            ShopRepository shopRepository,
            ShopMemberRepository memberRepository,
            UserRepository userRepository,
            ShopSettingsRepository settingsRepository,
            ShopAccessService accessService,
            SlugService slugService,
            AppProperties properties,
            Clock clock) {
        this.shopRepository = shopRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.accessService = accessService;
        this.slugService = slugService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ShopResponse create(UUID userId, CreateShopRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản hiện không hoạt động");
        }
        String slug = requestedSlug(request.slug(), request.name());
        if (shopRepository.existsBySlug(slug)) {
            throw new ApiException(HttpStatus.CONFLICT, "SHOP_SLUG_ALREADY_USED", "Đường dẫn cửa hàng đã được sử dụng");
        }
        Instant now = Instant.now(clock);
        ShopEntity shop = shopRepository.save(ShopEntity.create(
                user, request.name().strip(), slug, trimToNull(request.description()), now));
        memberRepository.save(ShopMemberEntity.owner(shop, user, now));
        settingsRepository.save(ShopSettingsEntity.defaults(shop, properties.currency(), properties.timeZone(), now));
        user.addRole(UserRole.SELLER, now);
        return toResponse(shop);
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> owned(UUID userId) {
        return shopRepository.findAllByOwner_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> ownedBy(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return shopRepository.findAllByOwner_IdInOrderByCreatedAtDesc(userIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ShopResponse update(UUID userId, UUID shopId, UpdateShopRequest request) {
        ShopMemberEntity member = accessService.requireActiveMember(userId, shopId);
        ShopEntity shop = member.getShop();
        String slug = slugService.normalize(request.slug());
        if (slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SHOP_SLUG", "Đường dẫn cửa hàng không hợp lệ");
        }
        if (shopRepository.existsBySlugAndIdNot(slug, shopId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SHOP_SLUG_ALREADY_USED", "Đường dẫn cửa hàng đã được sử dụng");
        }
        shop.update(
                request.name().strip(),
                slug,
                trimToNull(request.description()),
                trimToNull(request.logoUrl()),
                trimToNull(request.bannerUrl()),
                Instant.now(clock));
        return toResponse(shop);
    }

    @Transactional(readOnly = true)
    public ShopResponse publicShop(String slug) {
        ShopEntity shop = shopRepository
                .findBySlugAndStatus(slugService.normalize(slug), ShopStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Không tìm thấy cửa hàng"));
        return toResponse(shop);
    }

    @Transactional(readOnly = true)
    public PageResponse<ShopResponse> adminList(
            String query, ShopStatus status, int page, int size) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        Specification<ShopEntity> specification = (root, criteria, builder) -> {
            var predicate = builder.conjunction();
            if (!normalized.isEmpty()) {
                String pattern = "%" + escapeLike(normalized) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), pattern, '\\'),
                        builder.like(builder.lower(root.get("slug")), pattern, '\\')));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            return predicate;
        };
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(shopRepository.findAll(specification, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public ShopResponse adminDetail(UUID shopId) {
        return toResponse(shopRepository.findById(shopId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND",
                        "KhĂ´ng tĂ¬m tháº¥y cá»­a hĂ ng")));
    }

    @Transactional
    public ShopResponse moderate(UUID shopId, ShopStatus status) {
        ShopEntity shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Không tìm thấy cửa hàng"));
        shop.changeStatus(status, Instant.now(clock));
        return toResponse(shop);
    }

    private String requestedSlug(String requested, String name) {
        String value = requested == null || requested.isBlank() ? name : requested;
        String slug = slugService.normalize(value);
        if (slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SHOP_SLUG", "Đường dẫn cửa hàng không hợp lệ");
        }
        return slug.length() <= 180 ? slug : slug.substring(0, 180).replaceAll("-+$", "");
    }

    private ShopResponse toResponse(ShopEntity shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getOwnerId(),
                shop.getName(),
                shop.getSlug(),
                shop.getDescription(),
                shop.getLogoUrl(),
                shop.getBannerUrl(),
                shop.getRatingAverage(),
                shop.getReviewCount(),
                shop.getStatus().name(),
                shop.getCreatedAt(),
                shop.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
