/**
 * @ax-template-meta
 * template_id: backend/cache/CaffeineConfig
 * layer: backend-cross-cutting
 * anchors_rule: cacheable-requires-explicit-ttl.md (PRACTICES-CACHE-003)
 * protects_template_id: backend/cache/CaffeineConfig
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Caffeine Wiki/Eviction — expireAfterWrite: Expire entries after the specified duration has passed since the entry was created, or the most recent replacement of the value."
 *     url: "https://github.com/ben-manes/caffeine/wiki/Eviction"
 *   - source_type: external
 *     citation: "Spring Boot Reference — Caching with Caffeine; spring.cache.caffeine.spec=maximumSize=500,expireAfterAccess=600s"
 *     url: "https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.caffeine"
 * usage: |
 *   1. Replace 'com.example.app' with your base package.
 *   2. Add com.github.ben-manes.caffeine:caffeine to dependencies.
 *   3. Add @EnableCaching to a @Configuration class.
 *   4. Tune TTL and maximumSize per cache name in PER_CACHE_SPECS.
 *   5. For Redis-based distributed caching, use RedisCacheConfig.java instead.
 *
 *   Failing fixture: practices/evals/fixtures/cacheable_ttl/fail_no_ttl/
 *   Passing fixture:  practices/evals/fixtures/cacheable_ttl/pass/
 */
package com.example.app.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Process-local Caffeine cache configuration with explicit TTL per cache.
 *
 * <p>Every cache declares both {@code expireAfterWrite} and {@code maximumSize}.
 * Omitting {@code expireAfterWrite} causes stale entries to persist until process
 * restart — secret rotations, feature flags, and configuration changes do not
 * take effect until then.
 *
 * <p>Rule protected: {@code cacheable-requires-explicit-ttl.md} (PRACTICES-CACHE-003).
 *
 * <p>Per-cache TTL is configured via the {@code per-cache} map below. Add a new
 * cache by adding an entry — the fallback spec is applied to caches not listed.
 *
 * @see <a href="https://github.com/ben-manes/caffeine/wiki/Eviction">Caffeine Eviction</a>
 */
@Configuration
public class CaffeineConfig {

    // ── Constants — declare TTLs as named durations ───────────────────────────

    /** Default TTL for general-purpose caches. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    /** Default maximum entries per cache — bound heap footprint. */
    public static final long DEFAULT_MAX_SIZE = 1_000L;

    /**
     * Per-cache override specs.
     *
     * <p>Example entries:
     * <ul>
     *   <li>{@code "lookup"} — short-lived, frequently updated data</li>
     *   <li>{@code "config"} — long-lived application configuration</li>
     *   <li>{@code "session"} — user session-bound data</li>
     * </ul>
     *
     * <p>Adjust TTLs to match the data's staleness tolerance.
     */
    private static final Map<String, Caffeine<Object, Object>> PER_CACHE_SPECS = Map.of(
            "lookup", Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))   // REQUIRED — no implicit TTL in Caffeine
                    .maximumSize(1_000L)
                    .recordStats(),
            "config", Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofHours(1))
                    .maximumSize(200L)
                    .recordStats(),
            "session", Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofMinutes(30)) // access-based for session data
                    .maximumSize(5_000L)
                    .recordStats()
    );

    /**
     * Configures a {@code CaffeineCacheManager} with per-cache specs.
     *
     * <p>Spring Boot's {@code CacheMetrics} auto-registers Micrometer hit/miss
     * counters for each cache when {@code recordStats()} is enabled.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Default spec applied to caches not explicitly listed above
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_TTL)        // explicit TTL — REQUIRED
                .maximumSize(DEFAULT_MAX_SIZE)
                .recordStats());

        // Register per-cache overrides
        PER_CACHE_SPECS.forEach((name, spec) -> manager.registerCustomCache(
                name, spec.build()));

        return manager;
    }
}
