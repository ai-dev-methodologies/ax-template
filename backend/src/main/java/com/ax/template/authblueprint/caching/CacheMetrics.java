package com.ax.template.authblueprint.caching;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * CACHE-OBSERVABILITY-001 — exactly 3 canonical Micrometer meters with bounded-cardinality labels.
 *
 *   cache_hit_rate{tenant, cache_name}            — gauge (hits / total)
 *   cache_eviction_total{tenant, cache_name, reason} — counter (reason ∈ ttl|capacity|manual)
 *   cache_operation_latency{tenant, cache_name, op}  — timer  (op ∈ get|put|evict)
 *
 * Labels deliberately EXCLUDE cache key / resource_id / user_id / payload (unbounded + PII). Spec:
 * specs/caching-l0.yaml#CACHE-OBSERVABILITY-001 (OpenTelemetry low-cardinality attribute discipline).
 */
@Component
public class CacheMetrics {

    public static final String HIT_RATE = "cache_hit_rate";
    public static final String EVICTION_TOTAL = "cache_eviction_total";
    public static final String OP_LATENCY = "cache_operation_latency";

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, HitStats> hitStats = new ConcurrentHashMap<>();

    public CacheMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    private static final class HitStats {
        final LongAdder hits = new LongAdder();
        final LongAdder total = new LongAdder();
        double rate() {
            long t = total.sum();
            return t == 0 ? 0.0 : (double) hits.sum() / t;
        }
    }

    private HitStats stats(String tenant, String cacheName) {
        return hitStats.computeIfAbsent(tenant + '|' + cacheName, k -> {
            HitStats s = new HitStats();
            Gauge.builder(HIT_RATE, s, HitStats::rate)
                .tag("tenant", tenant)
                .tag("cache_name", cacheName)
                .register(registry);
            return s;
        });
    }

    public void recordHit(String tenant, String cacheName) {
        HitStats s = stats(tenant, cacheName);
        s.hits.increment();
        s.total.increment();
    }

    public void recordMiss(String tenant, String cacheName) {
        HitStats s = stats(tenant, cacheName);
        s.total.increment();
    }

    public void recordEviction(String tenant, String cacheName, String reason) {
        Counter.builder(EVICTION_TOTAL)
            .tag("tenant", tenant)
            .tag("cache_name", cacheName)
            .tag("reason", reason)
            .register(registry)
            .increment();
    }

    public void recordLatency(String tenant, String cacheName, String op, Duration elapsed) {
        Timer.builder(OP_LATENCY)
            .tag("tenant", tenant)
            .tag("cache_name", cacheName)
            .tag("op", op)
            .register(registry)
            .record(elapsed);
    }
}
