package com.payflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class BalanceCacheService {

    private static final String KEY_PREFIX = "balance:";

    private final StringRedisTemplate redisTemplate;
    private final MetricsService metricsService;
    private final Duration ttl;

    public BalanceCacheService(StringRedisTemplate redisTemplate,
                               MetricsService metricsService,
                               @Value("${app.cache.balance-ttl-seconds:300}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.metricsService = metricsService;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<BigDecimal> get(Long accountId) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + accountId);
            if (value != null) {
                metricsService.recordCacheHit();
                return Optional.of(new BigDecimal(value));
            }
            metricsService.recordCacheMiss();
        } catch (Exception e) {
            log.warn("Redis GET failed for account {}: {}", accountId, e.getMessage());
            metricsService.recordCacheMiss();
        }
        return Optional.empty();
    }

    public void put(Long accountId, BigDecimal balance) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + accountId, balance.toPlainString(), ttl);
        } catch (Exception e) {
            log.warn("Redis SET failed for account {}: {}", accountId, e.getMessage());
        }
    }

    public void evict(Long accountId) {
        try {
            redisTemplate.delete(KEY_PREFIX + accountId);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for account {}: {}", accountId, e.getMessage());
        }
    }
}
