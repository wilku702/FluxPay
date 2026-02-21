package com.payflow.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

@Service
public class MetricsService {

    private final Counter transactionSuccess;
    private final Counter transactionFailure;
    private final Counter cacheHit;
    private final Counter cacheMiss;
    private final Counter rateLimitRejected;
    private final Timer transactionDuration;

    public MetricsService(MeterRegistry registry) {
        this.transactionSuccess = Counter.builder("fluxpay.transactions.success")
                .description("Successful transactions")
                .register(registry);

        this.transactionFailure = Counter.builder("fluxpay.transactions.failure")
                .description("Failed transactions")
                .register(registry);

        this.cacheHit = Counter.builder("fluxpay.cache.hit")
                .description("Cache hits")
                .register(registry);

        this.cacheMiss = Counter.builder("fluxpay.cache.miss")
                .description("Cache misses")
                .register(registry);

        this.rateLimitRejected = Counter.builder("fluxpay.ratelimit.rejected")
                .description("Rate-limited requests")
                .register(registry);

        this.transactionDuration = Timer.builder("fluxpay.transactions.duration")
                .description("Transaction processing time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordTransactionSuccess() {
        transactionSuccess.increment();
    }

    public void recordTransactionFailure() {
        transactionFailure.increment();
    }

    public void recordCacheHit() {
        cacheHit.increment();
    }

    public void recordCacheMiss() {
        cacheMiss.increment();
    }

    public void recordRateLimitHit() {
        rateLimitRejected.increment();
    }

    public <T> T timeTransaction(Callable<T> operation) throws Exception {
        return transactionDuration.recordCallable(operation);
    }
}
