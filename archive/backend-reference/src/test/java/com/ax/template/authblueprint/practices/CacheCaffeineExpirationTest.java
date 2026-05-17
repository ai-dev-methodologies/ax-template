package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-CACHE-002")
class CacheCaffeineExpirationTest {

    @Test
    void practices_CACHE_002_caffeineCacheDeclaresExplicitExpiration() {
        // Caffeine without expireAfterWrite / expireAfterAccess keeps entries until they
        // are evicted by size pressure — for a 1k-entry cache that mostly never fills, the
        // effective TTL is "forever". Stale data leaks across deploys, secrets keep being
        // returned after a rotation, and cache poisoning becomes permanent. Declaring an
        // explicit expireAfterWrite is the mechanical remedy.
        assertThat(CacheConfig.LOOKUP_TTL)
                .as("CacheConfig.LOOKUP_TTL must be a positive Duration (expireAfterWrite source)")
                .isNotNull()
                .isGreaterThan(Duration.ZERO);
        assertThat(CacheConfig.LOOKUP_MAX_SIZE)
                .as("CacheConfig.LOOKUP_MAX_SIZE must bound the cache footprint")
                .isPositive();
    }
}
