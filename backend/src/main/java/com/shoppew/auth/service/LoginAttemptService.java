package com.shoppew.auth.service;

import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    static final int LOCK_THRESHOLD = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId, Instant now) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        user.recordFailedLogin(now, LOCK_THRESHOLD, now.plus(LOCK_DURATION));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId, Instant now) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        user.recordSuccessfulLogin(now);
    }
}
