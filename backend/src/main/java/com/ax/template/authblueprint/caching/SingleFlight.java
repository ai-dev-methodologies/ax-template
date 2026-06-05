package com.ax.template.authblueprint.caching;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * CACHE-STAMPEDE-001 — single-flight (thundering-herd prevention).
 *
 * On a cache miss for a hot key, concurrent callers MUST NOT all recompute. The first caller for a
 * key computes; every other caller for the SAME key blocks on the in-flight computation and reuses
 * its result, so an N-parallel-miss burst yields exactly ONE origin recomputation, not N. This is the
 * `lock` strategy from the spec (an in-process equivalent of a Redis SETNX recompute lock).
 * Spec: specs/caching-l0.yaml#CACHE-STAMPEDE-001 (RFC 5861 §3 stale-while-revalidate sibling).
 */
public final class SingleFlight {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    /**
     * Run {@code loader} for {@code key} under a per-key lock. Concurrent callers for the same key
     * serialize; combined with a cache check inside {@code loader}, only the first caller recomputes.
     */
    public <T> T call(String key, Supplier<T> loader) {
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        try {
            synchronized (lock) {
                return loader.get();
            }
        } finally {
            locks.remove(key, lock);
        }
    }
}
