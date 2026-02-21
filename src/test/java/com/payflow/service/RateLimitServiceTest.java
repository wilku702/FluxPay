package com.payflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOps;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        rateLimitService = new RateLimitService(redisTemplate, 100, 60);
    }

    @Test
    void allowsRequestWhenUnderLimit() {
        when(zSetOps.zCard("ratelimit:user:1")).thenReturn(50L);

        RateLimitService.RateLimitResult result = rateLimitService.isAllowed("user:1");

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(100);
        assertThat(result.remaining()).isEqualTo(49);
    }

    @Test
    void rejectsRequestWhenAtLimit() {
        when(zSetOps.zCard("ratelimit:user:1")).thenReturn(100L);
        when(zSetOps.rangeWithScores(eq("ratelimit:user:1"), eq(0L), eq(0L))).thenReturn(java.util.Collections.emptySet());

        RateLimitService.RateLimitResult result = rateLimitService.isAllowed("user:1");

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isEqualTo(0);
    }

    @Test
    void failsOpenOnRedisException() {
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis down"));

        RateLimitService.RateLimitResult result = rateLimitService.isAllowed("user:1");

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(100);
    }
}
