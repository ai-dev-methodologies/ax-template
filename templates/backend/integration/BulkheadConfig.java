/**
 * @ax-template-meta
 * template_id: backend/integration/BulkheadConfig
 * layer: backend-infrastructure
 * domain: integration
 * anchors_rule: resilient-http-client-required.md (PRACTICES-HTTP-002)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Resilience4j Bulkhead — Semaphore bulkhead limits concurrent calls; ThreadPool bulkhead isolates calls in a dedicated thread pool so a slow downstream cannot exhaust application threads"
 *     url: "https://resilience4j.readme.io/docs/bulkhead"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Use semaphore bulkhead for reactive/non-blocking services.
 *   Use thread-pool bulkhead for synchronous / blocking IO calls.
 */
package com.example.app.integration;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j bulkhead configuration — semaphore + thread-pool variants.
 */
@Configuration
public class BulkheadConfig {

    // ── Semaphore bulkhead ─────────────────────────────────────────────────────
    private static final int      MAX_CONCURRENT_CALLS = 20;
    private static final Duration MAX_WAIT             = Duration.ofMillis(100);

    // ── Thread-pool bulkhead ───────────────────────────────────────────────────
    private static final int TP_MAX_THREAD_POOL_SIZE = 10;
    private static final int TP_CORE_THREAD_POOL_SIZE = 5;
    private static final int TP_QUEUE_CAPACITY       = 50;

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        io.github.resilience4j.bulkhead.BulkheadConfig config =
                io.github.resilience4j.bulkhead.BulkheadConfig.custom()
                        .maxConcurrentCalls(MAX_CONCURRENT_CALLS)
                        .maxWaitDuration(MAX_WAIT)
                        .build();
        return BulkheadRegistry.of(config);
    }

    @Bean
    public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(TP_MAX_THREAD_POOL_SIZE)
                .coreThreadPoolSize(TP_CORE_THREAD_POOL_SIZE)
                .queueCapacity(TP_QUEUE_CAPACITY)
                .build();
        return ThreadPoolBulkheadRegistry.of(config);
    }
}
