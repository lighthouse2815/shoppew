package com.shoppew.auth.service;

import com.shoppew.auth.entity.AuthActionTokenEntity;
import com.shoppew.auth.entity.AuthActionTokenType;
import com.shoppew.auth.repository.AuthActionTokenRepository;
import com.shoppew.auth.repository.UserSessionRepository;
import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.entity.UserStatus;
import com.shoppew.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthActionService {

    private final UserRepository userRepository;
    private final AuthActionTokenRepository tokenRepository;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenCodec tokenCodec;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties properties;
    private final Clock clock;

    public AuthActionService(
            UserRepository userRepository,
            AuthActionTokenRepository tokenRepository,
            UserSessionRepository sessionRepository,
            RefreshTokenCodec tokenCodec,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            AppProperties properties,
            Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.tokenCodec = tokenCodec;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void requestEmailVerification(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> !user.isEmailVerified())
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(user -> issue(user, AuthActionTokenType.EMAIL_VERIFICATION, properties.emailVerificationTtl()));
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        Instant now = Instant.now(clock);
        AuthActionTokenEntity token = requireUsable(rawToken, AuthActionTokenType.EMAIL_VERIFICATION, now);
        token.getUser().verifyEmail(now);
        token.consume(now);
        tokenRepository.consumeOutstanding(token.getUser().getId(), AuthActionTokenType.EMAIL_VERIFICATION, now);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(user -> issue(user, AuthActionTokenType.PASSWORD_RESET, properties.passwordResetTtl()));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        passwordPolicy.validate(newPassword);
        Instant now = Instant.now(clock);
        AuthActionTokenEntity token = requireUsable(rawToken, AuthActionTokenType.PASSWORD_RESET, now);
        UserEntity user = token.getUser();
        if (user.getStatus() == UserStatus.SUSPENDED || user.getStatus() == UserStatus.BANNED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Tài khoản hiện không hoạt động");
        }
        user.changePassword(passwordEncoder.encode(newPassword), now);
        token.consume(now);
        tokenRepository.consumeOutstanding(user.getId(), AuthActionTokenType.PASSWORD_RESET, now);
        sessionRepository.revokeAllForUser(user.getId(), now, "PASSWORD_CHANGED");
    }

    private void issue(UserEntity user, AuthActionTokenType type, Duration ttl) {
        Instant now = Instant.now(clock);
        tokenRepository.consumeOutstanding(user.getId(), type, now);
        String rawToken = tokenCodec.newToken();
        tokenRepository.save(AuthActionTokenEntity.issue(
                user,
                type,
                tokenCodec.hash(rawToken),
                now.plus(ttl),
                now));
        eventPublisher.publishEvent(new AuthActionIssuedEvent(user.getEmail(), rawToken, type));
    }

    private AuthActionTokenEntity requireUsable(String rawToken, AuthActionTokenType type, Instant now) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidToken();
        }
        AuthActionTokenEntity token = tokenRepository.findForUse(tokenCodec.hash(rawToken), type)
                .orElseThrow(this::invalidToken);
        if (!token.isUsableAt(now)) {
            throw invalidToken();
        }
        return token;
    }

    private String normalizeEmail(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidToken() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_OR_EXPIRED_ACTION_TOKEN",
                "Liên kết không hợp lệ, đã hết hạn hoặc đã được sử dụng");
    }
}
