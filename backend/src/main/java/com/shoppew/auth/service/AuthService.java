package com.shoppew.auth.service;

import com.shoppew.auth.dto.AuthResponse;
import com.shoppew.auth.dto.AuthUserResponse;
import com.shoppew.auth.dto.LoginRequest;
import com.shoppew.auth.dto.RegisterRequest;
import com.shoppew.auth.dto.SessionResponse;
import com.shoppew.auth.entity.UserSessionEntity;
import com.shoppew.auth.event.UserRegisteredEvent;
import com.shoppew.auth.repository.UserSessionRepository;
import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserProfileEntity;
import com.shoppew.user.entity.UserStatus;
import com.shoppew.user.repository.UserProfileRepository;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final RefreshTokenCodec refreshTokenCodec;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptService loginAttemptService;
    private final AppProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            UserSessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            RefreshTokenCodec refreshTokenCodec,
            JwtTokenService jwtTokenService,
            LoginAttemptService loginAttemptService,
            AppProperties properties,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.refreshTokenCodec = refreshTokenCodec;
        this.jwtTokenService = jwtTokenService;
        this.loginAttemptService = loginAttemptService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("shoppew-invalid-password-placeholder");
    }

    @Transactional
    public AuthResult register(RegisterRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        String phone = normalizeNullable(request.phone());
        passwordPolicy.validate(request.password());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Email đã được sử dụng");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_USED", "Số điện thoại đã được sử dụng");
        }

        Instant now = Instant.now(clock);
        UserStatus status = properties.emailVerificationRequired()
                ? UserStatus.PENDING_VERIFICATION
                : UserStatus.ACTIVE;
        UserEntity user = UserEntity.register(
                email, phone, passwordEncoder.encode(request.password()), status, now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS", "Email hoặc số điện thoại đã được sử dụng");
        }
        UserProfileEntity profile = profileRepository.save(UserProfileEntity.create(user, request.displayName().strip(), now));
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));

        if (status == UserStatus.PENDING_VERIFICATION) {
            return new AuthResult(
                    new AuthResponse(null, "Bearer", null, true, toUserResponse(user, profile)),
                    null,
                    null);
        }
        return issue(user, profile, request.deviceName(), userAgent, UUID.randomUUID(), null);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String userAgent) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw invalidCredentials();
        }

        Instant now = Instant.now(clock);
        if (user.isLoginLockedAt(now)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "LOGIN_TEMPORARILY_LOCKED",
                    "Tài khoản tạm khóa đăng nhập. Vui lòng thử lại sau.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailure(user.getId(), now);
            throw invalidCredentials();
        }
        requireLoginAllowed(user);
        loginAttemptService.recordSuccess(user.getId(), now);
        UserProfileEntity profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ người dùng"));
        return issue(user, profile, request.deviceName(), userAgent, UUID.randomUUID(), null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult refresh(String refreshToken, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        Instant now = Instant.now(clock);
        UserSessionEntity current = sessionRepository
                .findForRefresh(refreshTokenCodec.hash(refreshToken))
                .orElseThrow(this::invalidRefreshToken);

        if (current.getRevokedAt() != null) {
            if (current.wasRotated()) {
                sessionRepository.revokeFamily(current.getTokenFamilyId(), now, "ROTATED_TOKEN_REUSE");
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "REFRESH_TOKEN_REUSED",
                        "Phiên đăng nhập đã bị thu hồi vì phát hiện token được dùng lại");
            }
            throw invalidRefreshToken();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke(now, "EXPIRED");
            throw invalidRefreshToken();
        }

        UserEntity user = current.getUser();
        requireLoginAllowed(user);
        current.touch(now);
        current.revoke(now, "ROTATED");
        UserProfileEntity profile = profileRepository.findById(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ người dùng"));
        return issue(user, profile, current.getDeviceName(), userAgent, current.getTokenFamilyId(), current);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        sessionRepository
                .findForRefresh(refreshTokenCodec.hash(refreshToken))
                .ifPresent(session -> session.revoke(Instant.now(clock), "LOGOUT"));
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
        UserProfileEntity profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ người dùng"));
        return toUserResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessions(UUID userId, UUID currentSessionId) {
        Instant now = Instant.now(clock);
        return sessionRepository.findActiveByUserId(userId, now).stream()
                .map(session -> new SessionResponse(
                        session.getId(),
                        session.getDeviceName(),
                        session.getUserAgent(),
                        session.getCreatedAt(),
                        session.getLastUsedAt(),
                        session.getExpiresAt(),
                        session.getId().equals(currentSessionId)))
                .toList();
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSessionEntity session = sessionRepository
                .findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Không tìm thấy phiên đăng nhập"));
        session.revoke(Instant.now(clock), "USER_REVOKED");
    }

    @Transactional
    public int revokeAllSessions(UUID userId) {
        return sessionRepository.revokeAllForUser(userId, Instant.now(clock), "LOGOUT_ALL");
    }

    private AuthResult issue(
            UserEntity user,
            UserProfileEntity profile,
            String deviceName,
            String userAgent,
            UUID familyId,
            UserSessionEntity rotatedFrom) {
        Instant now = Instant.now(clock);
        String refreshToken = refreshTokenCodec.newToken();
        Instant refreshExpiresAt = now.plus(properties.refreshTokenTtl());
        UserSessionEntity session = UserSessionEntity.create(
                user,
                familyId,
                refreshTokenCodec.hash(refreshToken),
                normalizeDeviceName(deviceName),
                truncate(userAgent, 1000),
                refreshExpiresAt,
                now,
                rotatedFrom);
        sessionRepository.saveAndFlush(session);
        JwtTokenService.AccessToken accessToken = jwtTokenService.issue(user, session.getId());
        AuthResponse response = new AuthResponse(
                accessToken.value(),
                "Bearer",
                accessToken.expiresAt(),
                false,
                toUserResponse(user, profile));
        return new AuthResult(response, refreshToken, refreshExpiresAt);
    }

    private AuthUserResponse toUserResponse(UserEntity user, UserProfileEntity profile) {
        Set<String> roles = user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                profile.getDisplayName(),
                roles,
                user.getStatus().name(),
                user.isEmailVerified());
    }

    private void requireLoginAllowed(UserEntity user) {
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED", "Vui lòng xác minh email trước khi đăng nhập");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản hiện không hoạt động");
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn");
    }

    private String normalizeEmail(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private String normalizeDeviceName(String value) {
        return value == null || value.isBlank() ? "Thiết bị không xác định" : truncate(value.strip(), 160);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record AuthResult(AuthResponse response, String refreshToken, Instant refreshExpiresAt) {}
}
