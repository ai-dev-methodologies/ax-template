/**
 * FIXTURE: cacheable_ttl/fail_no_ttl
 *
 * Demonstrates WRONG pattern: @Cacheable used without explicit TTL configuration.
 * The CacheManager bean does not declare a TTL for "itemsCache".
 *
 * Guard must catch: CACHEABLE_NO_TTL — @Cacheable("itemsCache") found but no
 * explicit TTL is configured for this cache name in the CacheManager bean.
 *
 * Violates: cacheable-requires-explicit-ttl rule (PRACTICES-CACHE-003).
 */
package com.example.fixture.cacheable_ttl.fail_no_ttl;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // VIOLATION: CaffeineCacheManager with no explicit TTL (expireAfterWrite not set).
        // Entries are kept until size pressure evicts them — stale data persists indefinitely.
        CaffeineCacheManager manager = new CaffeineCacheManager("itemsCache");
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(500));
        return manager;
    }
}

@Service
class ItemService {

    // VIOLATION: @Cacheable annotation used but CacheManager has no TTL for "itemsCache".
    @Cacheable("itemsCache")
    public String getItem(Long id) {
        return "item-" + id;
    }
}
