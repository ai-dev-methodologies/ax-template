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
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-003
failing_fixture_path: "practices/evals/fixtures/cacheable_ttl/fail_no_ttl"
passing_fixture_path: "practices/evals/fixtures/cacheable_ttl/pass"
protects_template_ids:
  - "templates/backend/cache/CaffeineConfig.java"
  - "templates/backend/cache/RedisCacheConfig.java"
upstream:
  - "https://github.com/ben-manes/caffeine/wiki/Eviction"
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: caffeine-2026-05
    section: "No Implicit TTL"
    quote: "Unlike some cache providers, Caffeine has no global default TTL. If neither expireAfterWrite nor expireAfterAccess is configured: entries are only evicted when maximumSize is exceeded"
  - upstream_id: spring-cache-2026-05
    section: "TTL / Eviction Policy"
    quote: "How can I Set the TTL/TTI/Eviction policy/XXX feature? The Spring Cache abstraction deliberately does not enforce TTL at the abstraction layer. TTL and eviction are provider-specific"
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

Verification: `./gradlew testPractices --tests "*CacheableTtl*"` asserts that every `@Cacheable`-enabled `CacheManager` bean declares a non-zero TTL.

Reference: [Caffeine Wiki — Eviction](https://github.com/ben-manes/caffeine/wiki/Eviction) | [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
