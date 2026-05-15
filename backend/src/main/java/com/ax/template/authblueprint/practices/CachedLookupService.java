package com.ax.template.authblueprint.practices;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CachedLookupService {

    @Cacheable(value = CacheConfig.LOOKUP_CACHE, key = "#id", sync = true)
    public String lookup(Long id) {
        // sync=true serializes concurrent loaders for the same key, preventing the cache
        // stampede where N parallel requests for a cold key all hit the backing store.
        return "lookup:" + id;
    }
}
