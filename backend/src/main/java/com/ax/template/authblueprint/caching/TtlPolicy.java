package com.ax.template.authblueprint.caching;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CACHE-TTL-001 — bounded TTL with expiry jitter.
 *
 * Every entry MUST have a bounded TTL (a non-positive base is a contract REJECT — unbounded entries
 * leak memory and risk permanent staleness). The effective TTL is jittered by ±jitterPct so a cohort
 * written in the same window does not expire simultaneously and stampede the origin (the "TTL cliff").
 * Spec: specs/caching-l0.yaml#CACHE-TTL-001 (RFC 5861 §4 stale-if-error sibling).
 */
public final class TtlPolicy {

    public static final int MAX_JITTER_PCT = 50;

    private TtlPolicy() {}

    /** @return a bounded, jittered TTL in [base*(1-j), base*(1+j)]. Throws if base ≤ 0 or jitter out of [0,50]. */
    public static Duration effectiveTtl(Duration baseTtl, int jitterPct) {
        if (baseTtl == null || baseTtl.isZero() || baseTtl.isNegative()) {
            throw new IllegalArgumentException("CACHE-TTL-001: TTL must be bounded (> 0) — unbounded entries are rejected");
        }
        if (jitterPct < 0 || jitterPct > MAX_JITTER_PCT) {
            throw new IllegalArgumentException("CACHE-TTL-001: jitterPct must be in [0," + MAX_JITTER_PCT + "], got " + jitterPct);
        }
        long baseMs = baseTtl.toMillis();
        long span = baseMs * jitterPct / 100;
        long delta = span == 0 ? 0 : ThreadLocalRandom.current().nextLong(-span, span + 1);
        return Duration.ofMillis(baseMs + delta);
    }
}
