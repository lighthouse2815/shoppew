package com.shoppew.user.service;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.auth.repository.UserSessionRepository;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.shop.dto.ShopResponse;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.shop.service.ShopService;
import com.shoppew.user.dto.AdminSellerDetailResponse;
import com.shoppew.user.dto.AdminSellerSummaryResponse;
import com.shoppew.user.dto.AdminUserDetailResponse;
import com.shoppew.user.dto.AdminUserSummaryResponse;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserProfileEntity;
import com.shoppew.user.entity.UserRole;
import com.shoppew.user.entity.UserStatus;
import com.shoppew.user.repository.UserProfileRepository;
import com.shoppew.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserSessionRepository sessionRepository;
    private final ShopService shopService;
    private final AdminAuditService audit;
    private final Clock clock;

    public AdminUserService(
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            UserSessionRepository sessionRepository,
            ShopService shopService,
            AdminAuditService audit,
            Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.shopService = shopService;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserSummaryResponse> users(
            String query, UserStatus status, UserRole role, int page, int size) {
        Page<UserEntity> users = userRepository.findAll(
                filters(query, status, role, null), pageable(page, size));
        Map<UUID, UserProfileEntity> profiles = profiles(users.getContent());
        return PageResponse.from(users, user -> summary(user, profiles.get(user.getId())));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse user(UUID userId) {
        return detail(requireUser(userId));
    }

    @Transactional
    public AdminUserDetailResponse changeStatus(
            UUID actorId, UUID userId, UserStatus status, String reason) {
        if (status == UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_USER_STATUS",
                    "Quáº£n trá»‹ viĂªn khĂ´ng thá»ƒ chuyá»ƒn tĂ i khoáº£n vá» chá» xĂ¡c minh");
        }
        UserEntity actor = requirePrivilegedActor(actorId);
        UserEntity target = requireUser(userId);
        authorizeStatusChange(actor, target);

        AdminUserDetailResponse before = detail(target);
        target.changeStatus(status, Instant.now(clock));
        if (status != UserStatus.ACTIVE) {
            sessionRepository.revokeAllForUser(target.getId(), Instant.now(clock), "ADMIN_" + status.name());
        }
        AdminUserDetailResponse after = detail(target);
        audit.record(actorId, action(status), "USER", target.getId(), before, after);
        return after;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSellerSummaryResponse> sellers(
            String query, UserStatus status, ShopStatus shopStatus, int page, int size) {
        Page<UserEntity> sellers = userRepository.findAll(
                filters(query, status, UserRole.SELLER, shopStatus), pageable(page, size));
        Map<UUID, UserProfileEntity> profiles = profiles(sellers.getContent());
        Map<UUID, List<ShopResponse>> shops = shopsByOwner(ids(sellers.getContent()));
        return PageResponse.from(sellers, seller -> sellerSummary(
                seller, profiles.get(seller.getId()), shops.getOrDefault(seller.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public AdminSellerDetailResponse seller(UUID userId) {
        UserEntity seller = requireUser(userId);
        if (!seller.getRoles().contains(UserRole.SELLER)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SELLER_NOT_FOUND", "KhĂ´ng tĂ¬m tháº¥y ngÆ°á»i bĂ¡n");
        }
        AdminUserDetailResponse detail = detail(seller);
        return new AdminSellerDetailResponse(detail, detail.shops());
    }

    private Specification<UserEntity> filters(
            String query, UserStatus status, UserRole role, ShopStatus shopStatus) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalized = normalize(query);
            if (!normalized.isEmpty()) {
                String pattern = "%" + escapeLike(normalized) + "%";
                Subquery<UUID> profileIds = criteriaQuery.subquery(UUID.class);
                Root<UserProfileEntity> profile = profileIds.from(UserProfileEntity.class);
                profileIds.select(profile.get("userId"));
                profileIds.where(builder.like(
                        builder.lower(profile.get("displayName")), pattern, '\\'));
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("email")), pattern, '\\'),
                        builder.like(builder.lower(root.get("phone")), pattern, '\\'),
                        root.get("id").in(profileIds)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (role != null) {
                criteriaQuery.distinct(true);
                predicates.add(builder.equal(root.join("roles"), role));
            }
            if (shopStatus != null) {
                Subquery<UUID> sellerIds = criteriaQuery.subquery(UUID.class);
                Root<ShopEntity> shop = sellerIds.from(ShopEntity.class);
                sellerIds.select(shop.get("owner").get("id"));
                sellerIds.where(builder.equal(shop.get("status"), shopStatus));
                predicates.add(root.get("id").in(sellerIds));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AdminUserDetailResponse detail(UserEntity user) {
        UserProfileEntity profile = profileRepository.findById(user.getId()).orElse(null);
        AdminUserSummaryResponse summary = summary(user, profile);
        List<ShopResponse> shops = shopService.owned(user.getId());
        return new AdminUserDetailResponse(
                summary.id(), summary.email(), summary.phone(), summary.displayName(), summary.avatarUrl(),
                summary.status(), summary.emailVerified(), summary.roles(),
                profile == null ? null : profile.getDateOfBirth(),
                profile == null ? null : profile.getGender(),
                profile == null ? "vi-VN" : profile.getLocale(),
                sessionRepository.countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(
                        user.getId(), Instant.now(clock)),
                shops, summary.createdAt(), summary.updatedAt());
    }

    private AdminUserSummaryResponse summary(UserEntity user, UserProfileEntity profile) {
        String displayName = profile == null || profile.getDisplayName() == null
                ? user.getEmail() : profile.getDisplayName();
        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .sorted()
                .toList();
        return new AdminUserSummaryResponse(
                user.getId(), user.getEmail(), user.getPhone(), displayName,
                profile == null ? null : profile.getAvatarUrl(), user.getStatus().name(),
                user.isEmailVerified(), roles, user.getCreatedAt(), user.getUpdatedAt());
    }

    private AdminSellerSummaryResponse sellerSummary(
            UserEntity seller, UserProfileEntity profile, List<ShopResponse> shops) {
        AdminUserSummaryResponse user = summary(seller, profile);
        long activeShops = shops.stream().filter(shop -> ShopStatus.ACTIVE.name().equals(shop.status())).count();
        return new AdminSellerSummaryResponse(
                user.id(), user.email(), user.phone(), user.displayName(), user.status(),
                user.emailVerified(), shops.size(), activeShops, user.createdAt(), user.updatedAt());
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "KhĂ´ng tĂ¬m tháº¥y ngÆ°á»i dĂ¹ng"));
    }

    private UserEntity requirePrivilegedActor(UUID actorId) {
        UserEntity actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "ADMIN_ACCESS_DENIED",
                        "KhĂ´ng cĂ³ quyá»n thá»±c hiá»‡n thao tĂ¡c quáº£n trá»‹"));
        boolean privileged = actor.getRoles().contains(UserRole.ADMIN)
                || actor.getRoles().contains(UserRole.SUPER_ADMIN);
        if (!privileged || actor.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_ACCESS_DENIED",
                    "KhĂ´ng cĂ³ quyá»n thá»±c hiá»‡n thao tĂ¡c quáº£n trá»‹");
        }
        return actor;
    }

    private void authorizeStatusChange(UserEntity actor, UserEntity target) {
        if (actor.getId().equals(target.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "ADMIN_SELF_STATUS_CHANGE_FORBIDDEN",
                    "KhĂ´ng thá»ƒ tá»± thay Ä‘á»•i tráº¡ng thĂ¡i tĂ i khoáº£n quáº£n trá»‹");
        }
        boolean superAdmin = actor.getRoles().contains(UserRole.SUPER_ADMIN);
        boolean targetIsAdmin = target.getRoles().contains(UserRole.ADMIN)
                || target.getRoles().contains(UserRole.SUPER_ADMIN);
        if (!superAdmin && targetIsAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_HIERARCHY_VIOLATION",
                    "Chá»‰ siĂªu quáº£n trá»‹ viĂªn cĂ³ thá»ƒ thay Ä‘á»•i tĂ i khoáº£n quáº£n trá»‹");
        }
    }

    private String action(UserStatus status) {
        return switch (status) {
            case ACTIVE -> "USER_RESTORED";
            case SUSPENDED -> "USER_SUSPENDED";
            case BANNED -> "USER_BANNED";
            case PENDING_VERIFICATION -> throw new IllegalArgumentException("Unsupported admin status");
        };
    }

    private Map<UUID, UserProfileEntity> profiles(Collection<UserEntity> users) {
        return profileRepository.findAllById(ids(users)).stream()
                .collect(Collectors.toMap(UserProfileEntity::getUserId, Function.identity()));
    }

    private Map<UUID, List<ShopResponse>> shopsByOwner(Collection<UUID> userIds) {
        return shopService.ownedBy(userIds).stream().collect(Collectors.groupingBy(
                ShopResponse::ownerId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<UUID> ids(Collection<UserEntity> users) {
        return users.stream().map(UserEntity::getId).toList();
    }

    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
