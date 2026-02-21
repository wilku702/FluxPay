package com.payflow.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsServiceTest {

    private SimpleMeterRegistry registry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricsService = new MetricsService(registry);
    }

    @Test
    void recordTransactionSuccessIncrementsCounter() {
        metricsService.recordTransactionSuccess();
        metricsService.recordTransactionSuccess();

        Counter counter = registry.find("fluxpay.transactions.success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void recordTransactionFailureIncrementsCounter() {
        metricsService.recordTransactionFailure();

        Counter counter = registry.find("fluxpay.transactions.failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordCacheHitIncrementsCounter() {
        metricsService.recordCacheHit();
        metricsService.recordCacheHit();
        metricsService.recordCacheHit();

        Counter counter = registry.find("fluxpay.cache.hit").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    void recordCacheMissIncrementsCounter() {
        metricsService.recordCacheMiss();

        Counter counter = registry.find("fluxpay.cache.miss").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void recordRateLimitHitIncrementsCounter() {
        metricsService.recordRateLimitHit();

        Counter counter = registry.find("fluxpay.ratelimit.rejected").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void timeTransactionRecordsTimer() throws Exception {
        String result = metricsService.timeTransaction(() -> "done");

        assertThat(result).isEqualTo("done");
        Timer timer = registry.find("fluxpay.transactions.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }
}
