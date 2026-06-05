package com.ax.template.authblueprint.caching;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Caching reference service — composes every caching-l0 contract over one cacheable "expensive"
 * resource: tenant-prefixed key (CACHE-KEY-001), single-flight recompute (CACHE-STAMPEDE-001),
 * version-bump invalidation (CACHE-INVALIDATION-001), and bounded-label metrics
 * (CACHE-OBSERVABILITY-001). The recompute counter is exposed so the stampede test can assert that an
 * N-parallel-miss burst recomputes exactly once. Spec: specs/caching-l0.yaml.
 */
@Service
public class CachedItemService {

    private final Cache<String, String> cache;
    private final CacheMetrics metrics;
    private final CacheKeyBuilder keyBuilder = new CacheKeyBuilder(false);
    private final SingleFlight singleFlight = new SingleFlight();
    private final ConcurrentHashMap<String, AtomicLong> versions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> recomputes = new ConcurrentHashMap<>();

    public CachedItemService(Cache<String, String> axDemoCache, CacheMetrics metrics) {
        this.cache = axDemoCache;
        this.metrics = metrics;
    }

    /** Read-through with single-flight: a hit returns immediately; a miss recomputes exactly once. */
    public String get(String tenant, String id) {
        long start = System.nanoTime();
        long version = version(tenant, id).get();
        String key = keyBuilder.build(tenant, "item", id, version);

        String cached = cache.getIfPresent(key);
        if (cached != null) {
            metrics.recordHit(tenant, CachingConfig.CACHE_NAME);
            metrics.recordLatency(tenant, CachingConfig.CACHE_NAME, "get", Duration.ofNanos(System.nanoTime() - start));
            return cached;
        }

        String value = singleFlight.call(key, () -> {
            String again = cache.getIfPresent(key);
            if (again != null) {
                return again;
            }
            recomputes.computeIfAbsent(logical(tenant, id), k -> new AtomicInteger()).incrementAndGet();
            String v = "item:" + id + "@tenant:" + tenant + "@v" + version; // deterministic, version-reflecting payload
            cache.put(key, v);
            return v;
        });
        metrics.recordMiss(tenant, CachingConfig.CACHE_NAME);
        metrics.recordLatency(tenant, CachingConfig.CACHE_NAME, "put", Duration.ofNanos(System.nanoTime() - start));
        return value;
    }

    /** CACHE-INVALIDATION-001 — version bump folds a new token into the key (read-your-writes safe). */
    public void invalidate(String tenant, String id) {
        long bumped = version(tenant, id).incrementAndGet();
        cache.invalidate(keyBuilder.build(tenant, "item", id, bumped - 1)); // evict the now-orphaned prior version
    }

    public int recomputeCount(String tenant, String id) {
        AtomicInteger c = recomputes.get(logical(tenant, id));
        return c == null ? 0 : c.get();
    }

    private AtomicLong version(String tenant, String id) {
        return versions.computeIfAbsent(logical(tenant, id), k -> new AtomicLong());
    }

    private static String logical(String tenant, String id) {
        return tenant + '|' + id;
    }
}
