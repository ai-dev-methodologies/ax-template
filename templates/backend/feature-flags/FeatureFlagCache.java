/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlagCache
 * layer: backend-domain
 * domain: feature-flags
 * anchors_rule: cacheable-requires-explicit-ttl.md (PRACTICES-CACHE-003)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Cacheable, @CacheEvict annotations"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html"
 *   - source_type: external
 *     citation: "Caffeine cache — expireAfterWrite configuration"
 *     url: "https://github.com/ben-manes/caffeine/wiki/Eviction#time-based"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Configure Caffeine bean in CacheConfig with 30s TTL for 'featureFlags' cache:
 *     Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(500)
 *   @CacheEvict is called by FeatureFlagService on create/update/delete.
 */
package com.example.app.featureflags;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Cache layer for feature flag evaluation.
 *
 * <p>Caffeine in-process cache with 30-second TTL (see PRACTICES-CACHE-003).
 * Cache name: {@code featureFlags}. Key: flag name.
 *
 * <p>Fail-closed: returns {@code false} for unknown flags (never throws).
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml#FF-EVAL-003
 */
@Component
public class FeatureFlagCache {

    private final FeatureFlagRepository repository;

    public FeatureFlagCache(FeatureFlagRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the active state for {@code name} from cache (or DB on miss).
     * Unknown flags return {@code false} (fail-closed — FF-EVAL-002).
     */
    @Cacheable(value = "featureFlags", key = "#name")
    public boolean isActive(String name) {
        return repository.findById(name)
                .map(FeatureFlag::isEnabled)
                .orElse(false);
    }

    /**
     * Evict a specific flag from cache.
     * Called by FeatureFlagService on create, update, or delete.
     */
    @CacheEvict(value = "featureFlags", key = "#name")
    public void evict(String name) {
        // eviction only — no body needed
    }

    /**
     * Evict all entries from the featureFlags cache.
     * Use sparingly — prefer per-key eviction.
     */
    @CacheEvict(value = "featureFlags", allEntries = true)
    public void evictAll() {
        // eviction only — no body needed
    }
}
