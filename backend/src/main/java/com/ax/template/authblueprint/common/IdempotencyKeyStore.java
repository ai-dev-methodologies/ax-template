package com.ax.template.authblueprint.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Cross-cutting idempotency-key store. Reusable by any domain whose
 * state-changing (POST / PUT / PATCH / DELETE) endpoint must survive
 * client retries and at-least-once delivery without producing a duplicate
 * side effect (a second charge, a second export job, a second outbound email…).
 *
 * <p>Anchors {@code specs/idempotency-l0.yaml} — specifically
 * {@code IDEMPOTENCY-CACHE-001} (key storage + TTL) and
 * {@code IDEMPOTENCY-CONCURRENT-001} (atomic write-once under a same-key race).
 * This is the in-memory ({@code idempotency_cache_backend: in-memory}) backend
 * referenced by that spec; verticals that need a durable audit trail
 * (payment / order) declare {@code jpa} instead and persist alongside the
 * created row.
 *
 * <p>R67-style lift: extracted from {@code payment.IdempotencyKeyStore} so reuse
 * on other domains' create endpoints no longer requires a cross-package import
 * from {@code payment} (a layering smell) or copy-paste. The IDW1 dogfood found
 * the only impl was locked inside the payment package, so personas deferred the
 * Idempotency-Key pattern entirely. Behavior here is identical to the original
 * payment store — the lift is a package move, not a rewrite.
 *
 * <p>Mechanics — a Caffeine cache with TTL = 24h (the spec default,
 * {@code idempotency_key_ttl_hours: 24}) stores the created resource id keyed by
 * ({@code scopeId}, {@code idempotencyKey}). The {@code scopeId} is the
 * per-tenant / per-user isolation discriminant mandated by
 * {@code IDEMPOTENCY-CACHE-001} ("per-tenant key isolation mandatory") — passing
 * a {@code userId} (as payment does) gives per-user isolation; a tenant id gives
 * per-tenant isolation. On a duplicate-key access,
 * {@link #findOrCreate(UUID, String, Supplier)} returns the cached id without
 * invoking the supplier — exactly-once semantics.
 *
 * <p>Concurrent semantics: a per-key {@link ReentrantLock} ensures only one
 * thread computes the supplier when multiple threads race with the same key.
 * All other threads block on the lock, then read the cached value.
 */
@Component
public class IdempotencyKeyStore {

    private final Cache<String, UUID> cache;
    private final ConcurrentHashMap<String, ReentrantLock> locks;

    public IdempotencyKeyStore() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(100_000)
            .build();
        this.locks = new ConcurrentHashMap<>();
    }

    /**
     * Atomic put-if-absent. If {@code (scopeId, key)} already has a stored
     * resource id, returns that id; otherwise invokes {@code compute}, caches
     * the result and returns it.
     *
     * @param scopeId per-tenant / per-user isolation discriminant
     * @param key     client-supplied Idempotency-Key value
     * @param compute supplier that performs the one-time side effect and returns
     *                the created resource id; invoked at most once per key
     */
    public UUID findOrCreate(UUID scopeId, String key, Supplier<UUID> compute) {
        String cacheKey = scopeId + ":" + key;
        UUID existing = cache.getIfPresent(cacheKey);
        if (existing != null) {
            return existing;
        }
        ReentrantLock lock = locks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            UUID retried = cache.getIfPresent(cacheKey);
            if (retried != null) {
                return retried;
            }
            UUID computed = compute.get();
            if (computed != null) {
                cache.put(cacheKey, computed);
            }
            return computed;
        } finally {
            lock.unlock();
            // Best-effort cleanup; harmless if another thread holds a reference
            locks.remove(cacheKey, lock);
        }
    }

    /**
     * Returns the resource id previously stored for {@code (scopeId, key)}, or
     * {@code null} when the key has never been seen (or its TTL has elapsed).
     */
    public UUID get(UUID scopeId, String key) {
        return cache.getIfPresent(scopeId + ":" + key);
    }
}
