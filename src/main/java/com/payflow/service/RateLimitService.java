package com.payflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class RateLimitService {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final int requestsPerWindow;
    private final int windowSizeSeconds;

    public RateLimitService(StringRedisTemplate redisTemplate,
                            @Value("${app.rate-limit.requests-per-window:100}") int requestsPerWindow,
                            @Value("${app.rate-limit.window-size-seconds:60}") int windowSizeSeconds) {
        this.redisTemplate = redisTemplate;
        this.requestsPerWindow = requestsPerWindow;
        this.windowSizeSeconds = windowSizeSeconds;
    }

    public RateLimitResult isAllowed(String identifier) {
        try {
            String key = KEY_PREFIX + identifier;
            long now = System.currentTimeMillis();
            long windowStart = now - (windowSizeSeconds * 1000L);

            ZSetOperations<String, String> zSet = redisTemplate.opsForZSet();

            // Remove expired entries
            zSet.removeRangeByScore(key, 0, windowStart);

            // Count current entries
            Long count = zSet.zCard(key);
            int current = count != null ? count.intValue() : 0;

            if (current >= requestsPerWindow) {
                // Find oldest entry to calculate retry-after
                var oldest = zSet.rangeWithScores(key, 0, 0);
                long retryAfterMs = windowSizeSeconds * 1000L;
                if (oldest != null && !oldest.isEmpty()) {
                    double oldestScore = oldest.iterator().next().getScore();
                    retryAfterMs = (long) (oldestScore + windowSizeSeconds * 1000L - now);
                }
                int retryAfterSeconds = Math.max(1, (int) Math.ceil(retryAfterMs / 1000.0));
                return new RateLimitResult(false, requestsPerWindow, 0, retryAfterSeconds);
            }

            // Add current request
            zSet.add(key, now + ":" + Math.random(), now);
            redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds));

            int remaining = requestsPerWindow - current - 1;
            return new RateLimitResult(true, requestsPerWindow, remaining, 0);
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return new RateLimitResult(true, requestsPerWindow, requestsPerWindow, 0);
        }
    }

    public record RateLimitResult(boolean allowed, int limit, int remaining, int retryAfterSeconds) {}
}
