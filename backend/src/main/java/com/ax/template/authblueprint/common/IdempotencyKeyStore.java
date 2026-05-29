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
 * {@code IDEMPOTENCY-CACHE-001} (key storage + TTL),
 * {@code IDEMPOTENCY-CONCURRENT-001} (atomic write-once under a same-key race)
 * and {@code IDEMPOTENCY-SCOPE-001} (the method scope POST/PUT/PATCH/DELETE).
 * Create endpoints use {@link #findOrCreate(UUID, String, Supplier)} (returns the
 * minted id); non-create mutations (PUT/PATCH/DELETE, which have no new id to
 * return) use {@link #firstSeen(UUID, String)} / {@link #idempotent(UUID, String, Runnable)}.
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
    private final Cache<String, Boolean> seenKeys;
    private final ConcurrentHashMap<String, ReentrantLock> locks;

    public IdempotencyKeyStore() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(100_000)
            .build();
        this.seenKeys = Caffeine.newBuilder()
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

    /**
     * Non-create idempotency guard for mutations that do NOT mint a new resource
     * id — PUT / PATCH / DELETE. Anchors {@code specs/idempotency-l0.yaml}
     * IDEMPOTENCY-SCOPE-001 ("Idempotency-Key applies to POST/PUT/PATCH/DELETE
     * by default").
     *
     * <p>Records {@code (scopeId, key)} and returns {@code true} only the FIRST
     * time it is seen within the TTL window; every replay returns {@code false}.
     * A caller guards a non-idempotent side effect with it:
     *
     * <pre>{@code
     * if (idempotencyStore.firstSeen(tenantId, idempotencyKey)) {
     *     order.cancel();          // runs once
     * }                            // replay: no-op, caller returns the same 204
     * }</pre>
     *
     * <p>Why this exists (IDW2 dogfood finding): {@link #findOrCreate(UUID, String, Supplier)}
     * was designed for create endpoints — its {@code Supplier<UUID>} must return
     * the id of a newly-minted resource. For a PUT/PATCH/DELETE there is no new
     * id to return, so every persona hand-rolled a {@code Supplier} returning a
     * throwaway UUID (or a random one) purely to drive the put-if-absent — an
     * awkward, repeated workaround. This overload expresses the mutation guard
     * directly and reuses the same scoping ({@code scopeId} = per-tenant /
     * per-user discriminant) and 24h TTL mechanism.
     *
     * <p>Write-once is atomic under a same-key race via the per-key
     * {@link ReentrantLock} (same mechanism as {@link #findOrCreate}), so exactly
     * one concurrent caller observes {@code true}.
     *
     * @param scopeId per-tenant / per-user isolation discriminant
     * @param key     client-supplied Idempotency-Key value
     * @return {@code true} the first time this {@code (scopeId, key)} is recorded;
     *         {@code false} on every replay within the TTL window
     */
    public boolean firstSeen(UUID scopeId, String key) {
        String cacheKey = scopeId + ":" + key;
        if (seenKeys.getIfPresent(cacheKey) != null) {
            return false;
        }
        ReentrantLock lock = locks.computeIfAbsent(cacheKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (seenKeys.getIfPresent(cacheKey) != null) {
                return false;
            }
            seenKeys.put(cacheKey, Boolean.TRUE);
            return true;
        } finally {
            lock.unlock();
            // Best-effort cleanup; harmless if another thread holds a reference
            locks.remove(cacheKey, lock);
        }
    }

    /**
     * Convenience wrapper over {@link #firstSeen(UUID, String)} for non-create
     * mutations (PUT / PATCH / DELETE) — runs {@code action} only on the FIRST
     * call for {@code (scopeId, key)} and is a no-op on every replay within the
     * TTL window. Anchors {@code specs/idempotency-l0.yaml} IDEMPOTENCY-SCOPE-001.
     *
     * <p>Because {@link #firstSeen} grants {@code true} to exactly one caller per
     * {@code (scopeId, key)}, a same-key race executes {@code action} at most once.
     * Note the action runs after the recording lock is released, not under it, so
     * it must not itself depend on holding that lock.
     *
     * @param scopeId per-tenant / per-user isolation discriminant
     * @param key     client-supplied Idempotency-Key value
     * @param action  the one-time side effect; invoked at most once per key
     * @return {@code true} if {@code action} ran (first call); {@code false} on replay
     */
    public boolean idempotent(UUID scopeId, String key, Runnable action) {
        boolean first = firstSeen(scopeId, key);
        if (first) {
            action.run();
        }
        return first;
    }
}
