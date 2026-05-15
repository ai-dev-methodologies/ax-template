---
title: Caffeine cache must declare explicit expireAfterWrite and maximumSize
impact: HIGH
impactDescription: "Without expireAfterWrite, sparse caches keep entries forever — stale data leaks across deploys and secret rotations"
tags:
  - cache
  - caffeine
  - ttl
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-002
upstream:
  - "https://docs.spring.io/spring-boot/reference/io/caching.html"
evidence:
  - upstream_id: spring-boot-cache
    section: "Spring Boot — Caffeine Cache configuration"
    quote: "expireAfter"
  - source_type: external
    citation: "Spring Boot Reference — Caching with Caffeine"
    url: "https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.caffeine"
---

## Caffeine cache must declare explicit expireAfterWrite and maximumSize

**Impact: HIGH — Without expireAfterWrite, sparse caches keep entries forever — stale data leaks across deploys and secret rotations**

Caffeine has no implicit TTL. A `Caffeine.newBuilder().maximumSize(1_000).build()` keeps every entry until size pressure evicts it; for a cache that mostly holds 100 entries against a 1k cap, the *effective* TTL is "until the process restarts." That means a secret rotation doesn't take effect (the old secret is cached), a feature-flag flip doesn't take effect (the old flag is cached), and any cache poisoning incident becomes permanent until restart. Declaring `expireAfterWrite` makes time-based eviction part of the cache contract, and `maximumSize` bounds the heap footprint.

**Incorrect — no expireAfterWrite:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder().maximumSize(1_000));   // no TTL — entries kept until size pressure
    return mgr;
}
```

**Correct — explicit expireAfterWrite + maximumSize:**

```java
@Bean
public CacheManager cacheManager() {
    CaffeineCacheManager mgr = new CaffeineCacheManager("lookup");
    mgr.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))             // time-based eviction part of contract
            .maximumSize(1_000));                                // heap footprint bounded
    return mgr;
}
```

Verification: `./gradlew testPractices --tests "*CaffeineExpiration*"` asserts `CacheConfig.LOOKUP_TTL > Duration.ZERO` and `LOOKUP_MAX_SIZE > 0`.

Reference: [Spring Boot — Caching with Caffeine](https://docs.spring.io/spring-boot/reference/io/caching.html#io.caching.provider.caffeine)
