package com.ax.template.authblueprint.payment;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * PAYMENT-IDEMP-002 / PAYMENT-IDEMP-003: idempotency key store.
 *
 * <p>Caffeine cache with TTL=24h (per blueprints/payment-manifest.yaml#idempotency).
 * Stores payment IDs keyed by ({@code userId}, {@code idempotencyKey}). On
 * duplicate-key access {@link #findOrCreate(UUID, String, Supplier)} returns the
 * cached id without invoking the supplier — exactly-once charge semantics.
 *
 * <p>Concurrent semantics: per-key {@link ReentrantLock} ensures only one thread
 * computes the supplier when multiple threads race with the same key. All other
 * threads block on the lock, then read the cached value.
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
     * Atomic put-if-absent. If {@code (userId, key)} already has a stored
     * payment id, returns that id; otherwise invokes {@code compute}, caches
     * the result and returns it.
     */
    public UUID findOrCreate(UUID userId, String key, Supplier<UUID> compute) {
        String cacheKey = userId + ":" + key;
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

    public UUID get(UUID userId, String key) {
        return cache.getIfPresent(userId + ":" + key);
    }
}
