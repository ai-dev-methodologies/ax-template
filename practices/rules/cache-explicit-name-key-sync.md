---
title: "@Cacheable must declare value, key, and sync=true explicitly"
impact: HIGH
impactDescription: "Defaulted key on multi-arg methods is unstable; without sync=true a cold key suffers N-way stampede on bursts"
tags:
  - cache
  - caffeine
  - spring-cache
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CACHE-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CACHE-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/integration/cache.html"
evidence:
  - upstream_id: spring-cache-abstraction
    section: "@Cacheable sync attribute (cacheNames + sync)"
    quote: '@Cacheable(cacheNames="foos", sync=true)'
  - source_type: external
    citation: "Spring Framework Reference — Cache Abstraction"
    url: "https://docs.spring.io/spring-framework/reference/integration/cache.html#cache-annotations-cacheable-synchronized"
---

## @Cacheable must declare value, key, and sync=true explicitly

**Impact: HIGH — Defaulted key on multi-arg methods is unstable; without sync=true a cold key suffers N-way stampede on bursts**

Bare `@Cacheable("cacheName")` derives the key from the method's full parameter list — that's fine for a single `Long id` argument and lethal for multi-argument methods where the key becomes unstable across parameter reordering, mutable types, or boxed/primitive mismatches. And without `sync = true`, a burst of N parallel requests for the same cold key all stampede past the cache and into the backing store; each one stores its own result, and only one ends up in the cache. Declaring `value`, `key` (SpEL), and `sync = true` makes the contract explicit and the stampede impossible.

**Incorrect — defaulted key, no sync:**

```java
@Service
public class LookupService {
    @Cacheable("practices.lookup")                   // key = all args; no sync
    public String lookup(Long id, String tenantId) {
        return loadFromDb(id, tenantId);             // stampede on cold key
    }
}
```

**Correct — explicit name + SpEL key + sync:**

```java
@Service
public class LookupService {
    @Cacheable(value = "practices.lookup", key = "#tenantId + ':' + #id", sync = true)
    public String lookup(Long id, String tenantId) {
        return loadFromDb(id, tenantId);             // sync=true serializes cold-key loads
    }
}
```

Verification: `./gradlew testPractices --tests "*ExplicitNameKey*"` reflects on `CachedLookupService.lookup` and asserts `@Cacheable.value()` is non-empty, `key()` is non-blank, `sync()` is true.

Reference: [Spring Cache Abstraction — Synchronized Caching](https://docs.spring.io/spring-framework/reference/integration/cache.html#cache-annotations-cacheable-synchronized)
