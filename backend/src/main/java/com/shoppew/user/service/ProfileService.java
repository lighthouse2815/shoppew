package com.shoppew.user.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.user.dto.ProfileResponse;
import com.shoppew.user.dto.UpdateProfileRequest;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserProfileEntity;
import com.shoppew.user.repository.UserProfileRepository;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final Clock clock;

    public ProfileService(UserRepository userRepository, UserProfileRepository profileRepository, Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) {
        return load(userId);
    }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request) {
        UserEntity user = requireUser(userId);
        UserProfileEntity profile = requireProfile(userId);
        String phone = trimToNull(request.phone());
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_USED", "Số điện thoại đã được sử dụng");
        }
        Instant now = Instant.now(clock);
        user.updateContact(phone, now);
        profile.update(
                request.displayName().strip(),
                trimToNull(request.avatarUrl()),
                request.dateOfBirth(),
                request.gender(),
                request.locale(),
                now);
        return toResponse(user, profile);
    }

    private ProfileResponse load(UUID userId) {
        return toResponse(requireUser(userId), requireProfile(userId));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
    }

    private UserProfileEntity requireProfile(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ người dùng"));
    }

    private ProfileResponse toResponse(UserEntity user, UserProfileEntity profile) {
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                profile.getDisplayName(),
                profile.getAvatarUrl(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getLocale(),
                roles,
                user.getStatus().name(),
                user.isEmailVerified());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
