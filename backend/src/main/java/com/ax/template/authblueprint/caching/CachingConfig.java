package com.ax.template.authblueprint.caching;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.time.Duration;

/**
 * Caching reference workload — the Caffeine store wired with the catalog's TTL + observability
 * contracts. Per-entry expiry comes from {@link TtlPolicy} (bounded + jittered, CACHE-TTL-001); every
 * removal feeds {@link CacheMetrics} with a bounded {tenant, cache_name, reason} label set
 * (CACHE-OBSERVABILITY-001). Spec: specs/caching-l0.yaml. (Named CachingConfig to avoid the
 * component-scan bean-name clash with practices/CacheConfig.)
 */
@Configuration
public class CachingConfig {

    public static final String CACHE_NAME = "ax-demo";
    public static final int BASE_TTL_SECONDS = 60;
    public static final int JITTER_PCT = 10;

    @Bean
    public Cache<String, String> axDemoCache(CacheMetrics metrics) {
        return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfter(new Expiry<String, String>() {
                @Override
                public long expireAfterCreate(@NonNull String key, @NonNull String value, long currentTime) {
                    return TtlPolicy.effectiveTtl(Duration.ofSeconds(BASE_TTL_SECONDS), JITTER_PCT).toNanos();
                }

                @Override
                public long expireAfterUpdate(@NonNull String key, @NonNull String value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(@NonNull String key, @NonNull String value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .removalListener((String key, String value, RemovalCause cause) -> {
                if (key == null) {
                    return;
                }
                String reason = switch (cause) {
                    case EXPIRED -> "ttl";
                    case SIZE -> "capacity";
                    default -> "manual";
                };
                metrics.recordEviction(tenantOf(key), CACHE_NAME, reason);
            })
            .build();
    }

    /** Extracts the tenant component from a {@code ax:{tenant}:...} key for bounded-label metrics. */
    static String tenantOf(String key) {
        String[] parts = key.split(":");
        return parts.length >= 2 ? parts[1] : "_";
    }
}
