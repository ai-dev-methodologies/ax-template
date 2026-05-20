package com.ax.template.authblueprint.featureflags;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Function;

/**
 * In-process Caffeine cache for flag evaluation hot path.
 * <p>
 * Trace:
 * <ul>
 *   <li>FF-EVAL-003 — 30-second TTL; entries are evicted on create/update/delete.</li>
 *   <li>blueprints/feature-flags-manifest.yaml#caching — cache_name=featureFlags,
 *       ttl_seconds=30, evict_on=[create, update, delete].</li>
 * </ul>
 * <p>
 * Uses Caffeine directly (not Spring {@code @Cacheable}) because the existing
 * {@link com.ax.template.authblueprint.practices.CacheConfig} {@code CacheManager}
 * publishes a different TTL/scope. Wiring a second cache name through that
 * manager would force every cache to share the longest TTL; a dedicated
 * instance keeps each domain's policy local.
 */
@Component
public class FeatureFlagCache {

    static final Duration TTL = Duration.ofSeconds(30);

    private final Cache<String, Boolean> cache;

    public FeatureFlagCache() {
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(10_000L)
            .build();
    }

    /** FF-EVAL-001/002 — load-through; loader returns active state. */
    public boolean getActive(String name, Function<String, Boolean> loader) {
        return cache.get(name, loader);
    }

    /** FF-EVAL-003 — invalidate on write (create / update / delete). */
    public void invalidate(String name) {
        cache.invalidate(name);
    }
}
