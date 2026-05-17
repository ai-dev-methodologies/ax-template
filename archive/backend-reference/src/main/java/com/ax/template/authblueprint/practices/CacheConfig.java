package com.ax.template.authblueprint.practices;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String LOOKUP_CACHE = "practices.lookup";
    public static final Duration LOOKUP_TTL = Duration.ofMinutes(5);
    public static final long LOOKUP_MAX_SIZE = 1_000L;

    @Bean
    public CacheManager cacheManager() {
        // expireAfterWrite is mandatory: without it Caffeine treats entries as unbounded
        // in time, which silently turns the cache into a memory leak. maximumSize bounds
        // the heap footprint.
        CaffeineCacheManager mgr = new CaffeineCacheManager(LOOKUP_CACHE);
        mgr.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(LOOKUP_TTL)
                .maximumSize(LOOKUP_MAX_SIZE));
        return mgr;
    }
}
