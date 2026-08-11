package com.shoppew.shop.service;

import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.shop.dto.ShopAddressRequest;
import com.shoppew.shop.dto.ShopAddressResponse;
import com.shoppew.shop.dto.ShopSettingsRequest;
import com.shoppew.shop.dto.ShopSettingsResponse;
import com.shoppew.shop.entity.ShopAddressEntity;
import com.shoppew.shop.entity.ShopAddressType;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.entity.ShopSettingsEntity;
import com.shoppew.shop.repository.ShopAddressRepository;
import com.shoppew.shop.repository.ShopSettingsRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopOperationsService {

    private final ShopAddressRepository addressRepository;
    private final ShopSettingsRepository settingsRepository;
    private final ShopAccessService accessService;
    private final AppProperties properties;
    private final Clock clock;

    public ShopOperationsService(
            ShopAddressRepository addressRepository,
            ShopSettingsRepository settingsRepository,
            ShopAccessService accessService,
            AppProperties properties,
            Clock clock) {
        this.addressRepository = addressRepository;
        this.settingsRepository = settingsRepository;
        this.accessService = accessService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ShopAddressResponse> addresses(UUID userId, UUID shopId) {
        accessService.requireActiveMember(userId, shopId);
        return addressRepository.findAllByShopIdOrderByAddressTypeAscDefaultAddressDescCreatedAtDesc(shopId)
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public ShopAddressResponse createAddress(UUID userId, UUID shopId, ShopAddressRequest request) {
        ShopEntity shop = accessService.requireActiveMember(userId, shopId).getShop();
        boolean makeDefault = request.defaultAddress()
                || !addressRepository.existsByShopIdAndAddressType(shopId, request.type());
        if (makeDefault) {
            addressRepository.clearDefault(shopId, request.type());
        }
        Instant now = Instant.now(clock);
        ShopAddressEntity address = ShopAddressEntity.create(
                shop,
                request.type(),
                addressValues(request),
                makeDefault,
                now);
        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public ShopAddressResponse updateAddress(
            UUID userId,
            UUID shopId,
            UUID addressId,
            ShopAddressRequest request) {
        accessService.requireActiveMember(userId, shopId);
        ShopAddressEntity address = requireAddress(shopId, addressId);
        ShopAddressType previousType = address.getAddressType();
        boolean movedType = previousType != request.type();
        boolean wasDefault = address.isDefaultAddress();
        Instant now = Instant.now(clock);

        if (movedType) {
            boolean makeDefault = request.defaultAddress()
                    || !addressRepository.existsByShopIdAndAddressType(shopId, request.type());
            if (makeDefault) {
                addressRepository.clearDefault(shopId, request.type());
                address.makeDefault(now);
            } else {
                address.clearDefault(now);
            }
        } else if (request.defaultAddress() && !address.isDefaultAddress()) {
            addressRepository.clearDefault(shopId, request.type());
            address.makeDefault(now);
        }

        address.update(request.type(), addressValues(request), now);
        addressRepository.flush();
        if (movedType && wasDefault) {
            promoteFirstRemaining(shopId, previousType, now);
        }
        return toAddressResponse(address);
    }

    @Transactional
    public ShopAddressResponse setDefaultAddress(UUID userId, UUID shopId, UUID addressId) {
        accessService.requireActiveMember(userId, shopId);
        ShopAddressEntity address = requireAddress(shopId, addressId);
        if (!address.isDefaultAddress()) {
            Instant now = Instant.now(clock);
            addressRepository.clearDefault(shopId, address.getAddressType());
            address.makeDefault(now);
        }
        return toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID shopId, UUID addressId) {
        accessService.requireActiveMember(userId, shopId);
        ShopAddressEntity address = requireAddress(shopId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        ShopAddressType type = address.getAddressType();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            promoteFirstRemaining(shopId, type, Instant.now(clock));
        }
    }

    @Transactional
    public ShopSettingsResponse settings(UUID userId, UUID shopId) {
        ShopEntity shop = accessService.requireActiveMember(userId, shopId).getShop();
        return toSettingsResponse(requireSettings(shop));
    }

    @Transactional
    public ShopSettingsResponse updateSettings(
            UUID userId,
            UUID shopId,
            ShopSettingsRequest request) {
        ShopEntity shop = accessService.requireActiveMember(userId, shopId).getShop();
        String zone = validateZone(request.timeZone());
        ShopSettingsEntity settings = requireSettings(shop);
        settings.update(
                request.currencyCode().toUpperCase(Locale.ROOT),
                zone,
                request.orderAutoCancelMinutes(),
                request.returnWindowDays(),
                request.chatEnabled(),
                request.vacationMode(),
                Instant.now(clock));
        return toSettingsResponse(settings);
    }

    private ShopAddressEntity requireAddress(UUID shopId, UUID addressId) {
        return addressRepository.findByIdAndShopId(addressId, shopId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SHOP_ADDRESS_NOT_FOUND",
                        "Không tìm thấy địa chỉ của cửa hàng"));
    }

    private ShopSettingsEntity requireSettings(ShopEntity shop) {
        return settingsRepository.findById(shop.getId())
                .orElseGet(() -> settingsRepository.save(ShopSettingsEntity.defaults(
                        shop,
                        properties.currency(),
                        properties.timeZone(),
                        Instant.now(clock))));
    }

    private void promoteFirstRemaining(UUID shopId, ShopAddressType type, Instant now) {
        List<ShopAddressEntity> remaining =
                addressRepository.findAllByShopIdAndAddressTypeOrderByDefaultAddressDescCreatedAtDesc(shopId, type);
        if (!remaining.isEmpty() && remaining.stream().noneMatch(ShopAddressEntity::isDefaultAddress)) {
            remaining.getFirst().makeDefault(now);
        }
    }

    private String validateZone(String candidate) {
        try {
            return ZoneId.of(candidate.strip()).getId();
        } catch (DateTimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_ZONE", "Múi giờ không hợp lệ");
        }
    }

    private ShopAddressEntity.AddressValues addressValues(ShopAddressRequest request) {
        return new ShopAddressEntity.AddressValues(
                request.contactName().strip(),
                request.phone().strip(),
                request.countryCode(),
                request.province().strip(),
                request.district().strip(),
                trimToNull(request.ward()),
                request.addressLine().strip(),
                trimToNull(request.postalCode()));
    }

    private ShopAddressResponse toAddressResponse(ShopAddressEntity address) {
        return new ShopAddressResponse(
                address.getId(),
                address.getAddressType().name(),
                address.getContactName(),
                address.getPhone(),
                address.getCountryCode(),
                address.getProvince(),
                address.getDistrict(),
                address.getWard(),
                address.getAddressLine(),
                address.getPostalCode(),
                address.isDefaultAddress(),
                address.getCreatedAt(),
                address.getUpdatedAt());
    }

    private ShopSettingsResponse toSettingsResponse(ShopSettingsEntity settings) {
        return new ShopSettingsResponse(
                settings.getShopId(),
                settings.getCurrencyCode(),
                settings.getTimeZone(),
                settings.getOrderAutoCancelMinutes(),
                settings.getReturnWindowDays(),
                settings.isChatEnabled(),
                settings.isVacationMode(),
                settings.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
