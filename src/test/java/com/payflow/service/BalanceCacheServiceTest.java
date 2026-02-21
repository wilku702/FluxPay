package com.payflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private MetricsService metricsService;

    private BalanceCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheService = new BalanceCacheService(redisTemplate, metricsService, 300);
    }

    @Test
    void getCacheHitReturnsBalance() {
        when(valueOps.get("balance:1")).thenReturn("1000.5000");

        Optional<BigDecimal> result = cacheService.get(1L);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("1000.5000"));
    }

    @Test
    void getCacheMissReturnsEmpty() {
        when(valueOps.get("balance:1")).thenReturn(null);

        Optional<BigDecimal> result = cacheService.get(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void putStoresBalanceWithTtl() {
        cacheService.put(1L, new BigDecimal("500.0000"));

        verify(valueOps).set(eq("balance:1"), eq("500.0000"), eq(Duration.ofSeconds(300)));
    }

    @Test
    void evictDeletesKey() {
        cacheService.evict(1L);

        verify(redisTemplate).delete("balance:1");
    }

    @Test
    void getReturnsEmptyOnRedisFailure() {
        when(valueOps.get(any())).thenThrow(new RuntimeException("Redis down"));

        Optional<BigDecimal> result = cacheService.get(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void putDoesNotThrowOnRedisFailure() {
        doThrow(new RuntimeException("Redis down")).when(valueOps).set(any(), any(), any(Duration.class));

        cacheService.put(1L, BigDecimal.TEN);
        // No exception thrown
    }
}
