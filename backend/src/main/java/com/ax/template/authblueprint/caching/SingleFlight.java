package com.ax.template.authblueprint.caching;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * CACHE-STAMPEDE-001 — per-key single-flight LOCK (thundering-herd prevention).
 *
 * Concurrent callers for the SAME key serialize on a per-key lock, recomputing one-at-a-time rather
 * than all at once. IMPORTANT: this is a mutex, NOT a result memoizer — the lock is released once the
 * loader returns, so a caller arriving afterwards gets a fresh lock. De-duplication therefore REQUIRES
 * the loader to re-check the cache first and return the winner's stored value instead of recomputing
 * (see {@code CachedItemService} for the reference caller). Used that way, an N-parallel-miss burst
 * yields exactly ONE origin recomputation. For standalone de-dup independent of the cache, memoize a
 * {@code CompletableFuture} per key instead. (A distributed deployment would use a Redis SETNX lock.)
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
