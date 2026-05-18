/**
 * @ax-template-meta
 * template_id: backend/cache/RedisCacheConfig
 * layer: backend-cross-cutting
 * anchors_rule: cacheable-requires-explicit-ttl.md (PRACTICES-CACHE-003)
 * protects_template_id: backend/cache/RedisCacheConfig
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "Spring Cache Abstraction — TTL/eviction is provider-specific: for Redis use spring.cache.redis.time-to-live or RedisCacheConfiguration.entryTtl()"
 *     url: "https://docs.spring.io/spring-framework/reference/integration/cache.html"
 *   - source_type: external
 *     citation: "Spring Boot Reference — Redis cache time-to-live: spring.cache.redis.time-to-live"
 *     url: "https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.redis"
 * usage: |
 *   1. Replace 'com.example.app' with your base package.
 *   2. Add spring-boot-starter-data-redis to dependencies.
 *   3. Configure spring.data.redis.host and spring.data.redis.port (or
 *      spring.data.redis.url) in application.yml.
 *   4. Add @EnableCaching to a @Configuration class.
 *   5. Tune per-cache TTL in CACHE_TTL_MAP.
 *   6. For process-local caching (no Redis), use CaffeineConfig.java instead.
 *
 *   Failing fixture: practices/evals/fixtures/cacheable_ttl/fail_no_ttl/
 *   Passing fixture:  practices/evals/fixtures/cacheable_ttl/pass/
 */
package com.example.app.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Distributed Redis cache configuration with explicit TTL per cache.
 *
 * <p>Every cache declares an explicit TTL via {@code RedisCacheConfiguration.entryTtl()}.
 * Without an explicit TTL, Redis stores entries with no expiry — entries persist
 * until manually evicted or the Redis instance is flushed.
 *
 * <p>Rule protected: {@code cacheable-requires-explicit-ttl.md} (PRACTICES-CACHE-003).
 *
 * <p>Serialization uses JSON (Jackson) for human-readable Redis inspection and
 * compatibility across service versions. Binary serializers (Kryo, JDK) break
 * when model classes change.
 *
 * @see <a href="https://docs.spring.io/spring-framework/reference/integration/cache.html">Spring Cache Abstraction</a>
 */
@Configuration
public class RedisCacheConfig {

    // ── TTL constants ─────────────────────────────────────────────────────────

    /** Default TTL — applied to caches not listed in CACHE_TTL_MAP. */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    /** Per-cache TTL overrides. */
    private static final Map<String, Duration> CACHE_TTL_MAP = Map.of(
            "lookup",  Duration.ofMinutes(5),       // REQUIRED — no implicit TTL in Redis
            "config",  Duration.ofHours(1),
            "session", Duration.ofMinutes(30),
            "token",   Duration.ofMinutes(15)
    );

    /**
     * Redis cache manager with per-cache TTL configuration.
     *
     * <p>The default configuration disables caching null values (prevents
     * cache poisoning from transient failures) and uses JSON serialization
     * for cache values.
     *
     * <p>Keys are serialized as plain strings (no prefix by default — add
     * a key prefix when multiple applications share a Redis instance).
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaults = buildDefaultConfig(DEFAULT_TTL);

        Map<String, RedisCacheConfiguration> perCacheConfigs = new HashMap<>();
        CACHE_TTL_MAP.forEach((name, ttl) ->
                perCacheConfigs.put(name, buildDefaultConfig(ttl)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCacheConfigs)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static RedisCacheConfiguration buildDefaultConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)                                      // explicit TTL — REQUIRED
                .disableCachingNullValues()                          // no null caching
                .serializeKeysWith(
                        SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));
    }
}
