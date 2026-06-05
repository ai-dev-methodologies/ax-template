package com.ax.template.authblueprint.idempotency;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IDEMPOTENCY-OBSERVABILITY-001 — exactly 3 canonical Micrometer metrics with bounded-cardinality
 * labels:
 * <ul>
 *   <li>{@code idempotency_requests_total{tenant, outcome}} — outcome ∈ the CLOSED set
 *       {first_seen, replayed, conflict, fingerprint_mismatch};</li>
 *   <li>{@code idempotency_cache_hit_rate{tenant}} — gauge of the replay fraction (replayed/total);</li>
 *   <li>{@code idempotency_lock_wait_seconds{tenant}} — timer of the critical-section (in-flight) duration.</li>
 * </ul>
 * The key value, fingerprint, and payload are NEVER labels. {@code outcome} is a fixed enum; the
 * only open dimension is {@code tenant} (the isolation axis the spec mandates on all three).
 *
 * <p>Spec: specs/idempotency-l0.yaml#IDEMPOTENCY-OBSERVABILITY-001.
 */
@Component
public class IdempotencyMetrics {

    public static final String REQUESTS = "idempotency_requests_total";
    public static final String HIT_RATE = "idempotency_cache_hit_rate";
    public static final String LOCK_WAIT = "idempotency_lock_wait_seconds";

    static final String TAG_TENANT = "tenant";
    static final String TAG_OUTCOME = "outcome";

    /** Per-tenant replay-fraction accounting backing the {@link #HIT_RATE} gauge. */
    private static final class Stats {
        final AtomicLong total = new AtomicLong();
        final AtomicLong replayed = new AtomicLong();
        double rate() {
            long t = total.get();
            return t == 0 ? 0.0 : (double) replayed.get() / t;
        }
    }

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Stats> perTenant = new ConcurrentHashMap<>();

    public IdempotencyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Record one resolved idempotency request (one of the four closed outcomes). */
    public void outcome(String tenant, String outcome) {
        Counter.builder(REQUESTS).tag(TAG_TENANT, tenant).tag(TAG_OUTCOME, outcome)
                .register(registry).increment();
        Stats stats = statsFor(tenant);
        stats.total.incrementAndGet();
        if ("replayed".equals(outcome)) {
            stats.replayed.incrementAndGet();
        }
    }

    /** Record the critical-section (in-flight) duration the winning request held the key. */
    public void lockWait(String tenant, Duration elapsed) {
        Timer.builder(LOCK_WAIT).tag(TAG_TENANT, tenant).register(registry).record(elapsed);
    }

    private Stats statsFor(String tenant) {
        return perTenant.computeIfAbsent(tenant, t -> {
            Stats s = new Stats();
            Gauge.builder(HIT_RATE, s, Stats::rate).tag(TAG_TENANT, t).register(registry);
            return s;
        });
    }
}
