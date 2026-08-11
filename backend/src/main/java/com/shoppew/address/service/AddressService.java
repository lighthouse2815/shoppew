package com.shoppew.address.service;

import com.shoppew.address.dto.AddressRequest;
import com.shoppew.address.dto.AddressResponse;
import com.shoppew.address.entity.UserAddressEntity;
import com.shoppew.address.repository.UserAddressRepository;
import com.shoppew.common.exception.ApiException;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AddressService(UserAddressRepository addressRepository, UserRepository userRepository, Clock clock) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(UUID userId) {
        return addressRepository.findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(UUID userId, AddressRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
        boolean makeDefault = request.defaultAddress() || !addressRepository.existsByUserId(userId);
        if (makeDefault) {
            addressRepository.clearDefault(userId);
        }
        Instant now = Instant.now(clock);
        UserAddressEntity address = UserAddressEntity.create(user, values(request), makeDefault, now);
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(UUID userId, UUID addressId, AddressRequest request) {
        UserAddressEntity address = findOwned(userId, addressId);
        if (request.defaultAddress() && !address.isDefaultAddress()) {
            addressRepository.clearDefault(userId);
            address.makeDefault(Instant.now(clock));
        }
        address.update(values(request), Instant.now(clock));
        return toResponse(address);
    }

    @Transactional
    public AddressResponse setDefault(UUID userId, UUID addressId) {
        UserAddressEntity address = findOwned(userId, addressId);
        if (!address.isDefaultAddress()) {
            addressRepository.clearDefault(userId);
            address.makeDefault(Instant.now(clock));
        }
        return toResponse(address);
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        UserAddressEntity address = findOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();
        if (wasDefault) {
            List<UserAddressEntity> remaining = addressRepository.findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                remaining.getFirst().makeDefault(Instant.now(clock));
            }
        }
    }

    private UserAddressEntity findOwned(UUID userId, UUID addressId) {
        return addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "Không tìm thấy địa chỉ"));
    }

    private UserAddressEntity.AddressValues values(AddressRequest request) {
        return new UserAddressEntity.AddressValues(
                trimToNull(request.label()),
                request.recipientName().strip(),
                request.phone().strip(),
                request.countryCode(),
                request.province().strip(),
                request.district().strip(),
                trimToNull(request.ward()),
                request.addressLine().strip(),
                trimToNull(request.postalCode()));
    }

    private AddressResponse toResponse(UserAddressEntity address) {
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
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

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
