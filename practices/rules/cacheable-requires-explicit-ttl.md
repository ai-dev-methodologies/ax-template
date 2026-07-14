---
title: "@Cacheable caches must have explicit TTL configured on the CacheManager"
impact: HIGH
impactDescription: "Without explicit TTL, cache entries persist until process restart — secret rotations and feature flag changes take effect only after the process is killed"
tags:
  - cache
  - ttl
  - caffeine
  - redis
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-004"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/practices/CacheConfig.java"
  pattern: "Every CacheManager bean declares provider TTL — Caffeine .expireAfterWrite(LOOKUP_TTL), machine-checked by CacheCaffeineExpirationTest (PRACTICES-CACHE-002); Redis .entryTtl(...) reviewed (the reference backend ships a Caffeine-only CacheConfig — no Redis manager to assert against)"
protects_template_ids:
  - "templates/backend/cache/CaffeineConfig.java"
  - "templates/backend/cache/RedisCacheConfig.java"
upstream:
  - "https://github.com/ben-manes/caffeine/wiki/Eviction"
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: caffeine-2026-05
    section: "Time-based eviction — expireAfterWrite"
    quote: "expireAfterWrite(long, TimeUnit): Expire entries after the specified duration has passed since the entry was created, or the most recent replacement of the value."
  - upstream_id: spring-cache-2026-05
    section: "FAQ — TTL/Eviction is provider-specific"
    quote: "How can I Set the TTL/TTI/Eviction policy/XXX feature? Directly through your cache provider. The cache abstraction is an abstraction, not a cache implementation."
  - source_type: external
    citation: "Caffeine Wiki/Eviction — expireAfterWrite: Expire entries after the specified duration has passed since the entry was created, or the most recent replacement of the value."
    url: "https://github.com/ben-manes/caffeine/wiki/Eviction"
---

## @Cacheable caches must have explicit TTL configured on the CacheManager

**Impact: HIGH — Without explicit TTL, cache entries persist until process restart — secret rotations and feature flag changes take effect only after the process is killed**

Spring's `@Cacheable` abstraction deliberately delegates TTL enforcement to the underlying provider. Neither Caffeine nor Redis applies any implicit TTL when one is not configured. A `CaffeineCacheManager` built without `expireAfterWrite` and a `RedisCacheManager` built without `entryTtl()` will both keep entries indefinitely — or until size-based eviction pressure removes them.

The practical consequences:
1. **Security:** An API key or secret cached at startup remains cached after rotation. The service keeps using the old credential until restarted.
2. **Feature flags:** A cached `false` flag value stays `false` even after the flag is flipped.
3. **Configuration:** Application configuration cached at startup becomes stale after a live config update.

**Incorrect — Caffeine without expireAfterWrite:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder().maximumSize(1_000));
    // No expireAfterWrite — entries kept until size pressure evicts them
    return mgr;
}
```

**Incorrect — Redis without entryTtl:**

```java
@Bean
public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
    return RedisCacheManager.builder(factory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            // No entryTtl — entries stored with no Redis TTL, persist forever
            .build();
}
```

**Correct — Caffeine with explicit expireAfterWrite:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))  // time-based eviction is part of the contract
            .maximumSize(1_000));
    return mgr;
}
```

**Correct — Redis with explicit entryTtl:**

```java
@Bean
public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))     // explicit TTL — REQUIRED
            .disableCachingNullValues();
    return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
}
```

Use named constants for TTL values so they are visible at code-review time:

```java
public static final Duration LOOKUP_TTL = Duration.ofMinutes(5);
public static final Duration CONFIG_TTL  = Duration.ofHours(1);
```

See reference templates:
- `templates/backend/cache/CaffeineConfig.java` — process-local cache with per-cache TTL map
- `templates/backend/cache/RedisCacheConfig.java` — distributed cache with per-cache TTL map

Verification (review): inspect every `CacheManager` bean. The Caffeine slice (`expireAfterWrite`) is machine-checked by `./gradlew testPractices --tests "*CaffeineExpiration*"` (PRACTICES-CACHE-002, asserts `CacheConfig.LOOKUP_TTL > Duration.ZERO`); the Redis `entryTtl` slice is review-only, because the reference backend ships a Caffeine-only `CacheConfig` with no Redis cache manager to assert against.

Reference: [Caffeine Wiki — Eviction](https://github.com/ben-manes/caffeine/wiki/Eviction) | [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
