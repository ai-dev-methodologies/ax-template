/**
 * FIXTURE: cacheable_ttl/pass
 *
 * Demonstrates CORRECT pattern: @Cacheable used with explicit TTL configured
 * in the CacheManager bean via expireAfterWrite.
 *
 * Guard exits 0: CacheManager declares explicit TTL for every cache name used
 * by @Cacheable annotations.
 *
 * Complies with: cacheable-requires-explicit-ttl rule (PRACTICES-CACHE-003).
 */
package com.example.fixture.cacheable_ttl.pass;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // CORRECT: explicit expireAfterWrite (TTL) and maximumSize declared.
        // Time-based eviction is part of the cache contract; no silent stale data.
        CaffeineCacheManager manager = new CaffeineCacheManager("itemsCache");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))  // explicit TTL
                .maximumSize(500));
        return manager;
    }
}

@Service
class ItemService {

    // CORRECT: @Cacheable used with cacheNames that has explicit TTL in CacheManager.
    @Cacheable(cacheNames = "itemsCache")
    public String getItem(Long id) {
        return "item-" + id;
    }
}
