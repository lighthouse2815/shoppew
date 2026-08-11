package com.shoppew.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisAbuseRateLimiter {

    static final int LOCAL_FALLBACK_MAX_IDENTITIES = 10_000;
    private static final Logger log = LoggerFactory.getLogger(RedisAbuseRateLimiter.class);
    private static final long WARNING_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); "
                    + "if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return current;",
            Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final int localFallbackCapacity;
    private final Map<String, LocalWindow> localWindows = new HashMap<>();
    private final AtomicLong lastStoreWarningAt = new AtomicLong();

    @Autowired
    public RedisAbuseRateLimiter(StringRedisTemplate redis, Clock clock) {
        this(redis, clock, LOCAL_FALLBACK_MAX_IDENTITIES);
    }

    RedisAbuseRateLimiter(StringRedisTemplate redis, Clock clock, int localFallbackCapacity) {
        this.redis = redis;
        this.clock = clock;
        if (localFallbackCapacity < 1) throw new IllegalArgumentException("Local fallback capacity must be positive");
        this.localFallbackCapacity = localFallbackCapacity;
    }

    public boolean allow(String namespace, String identity, int limit, Duration window) {
        if (limit < 1) return false;
        String key = "shoppew:rate-limit:" + namespace + ":" + sha256(identity);
        try {
            Long count = redis.execute(
                    INCREMENT,
                    List.of(key),
                    Long.toString(Math.max(window.toMillis(), 1)));
            return count == null || count <= limit;
        } catch (RuntimeException exception) {
            warnStoreUnavailable(exception);
            return allowLocally(key, limit, window);
        }
    }

    private synchronized boolean allowLocally(String key, int limit, Duration window) {
        long now = clock.millis();
        LocalWindow existing = localWindows.get(key);
        if (existing != null && existing.expiresAt() > now) {
            int count = existing.count() + 1;
            localWindows.put(key, new LocalWindow(count, existing.expiresAt()));
            return count <= limit;
        }
        if (existing != null) localWindows.remove(key);

        if (localWindows.size() >= localFallbackCapacity) {
            localWindows.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        }
        if (localWindows.size() >= localFallbackCapacity) {
            // During a Redis outage, fail closed for new identities once the bounded
            // per-instance fallback is full. This avoids silently disabling protection
            // without allowing an attacker to grow process memory without bound.
            return false;
        }
        localWindows.put(key, new LocalWindow(1, now + Math.max(window.toMillis(), 1)));
        return true;
    }

    private void warnStoreUnavailable(RuntimeException exception) {
        long now = clock.millis();
        long previous = lastStoreWarningAt.get();
        if (now - previous >= WARNING_INTERVAL_MILLIS && lastStoreWarningAt.compareAndSet(previous, now)) {
            log.warn("Rate-limit store unavailable; using bounded local fallback (type={})",
                    exception.getClass().getSimpleName());
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record LocalWindow(int count, long expiresAt) {}
}
