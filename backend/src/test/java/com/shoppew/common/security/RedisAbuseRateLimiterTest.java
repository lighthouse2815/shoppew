package com.shoppew.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisAbuseRateLimiterTest {

    private final StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);

    @BeforeEach
    void unavailableRedis() {
        when(redis.execute(any(), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("unavailable"));
    }

    @Test
    void enforcesPerIdentityLimitWhenRedisIsUnavailable() {
        RedisAbuseRateLimiter limiter = new RedisAbuseRateLimiter(redis, fixedClock(), 10);

        assertThat(limiter.allow("login", "198.51.100.10", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.allow("login", "198.51.100.10", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.allow("login", "198.51.100.10", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void failsClosedForNewIdentityWhenBoundedFallbackIsFull() {
        RedisAbuseRateLimiter limiter = new RedisAbuseRateLimiter(redis, fixedClock(), 2);

        assertThat(limiter.allow("login", "198.51.100.10", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.allow("login", "198.51.100.11", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.allow("login", "198.51.100.12", 2, Duration.ofMinutes(1))).isFalse();
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-08-11T04:00:00Z"), ZoneOffset.UTC);
    }
}
