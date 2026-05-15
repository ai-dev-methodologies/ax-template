---
title: Singleton beans must not carry unsynchronized mutable state
impact: HIGH
impactDescription: "Default singleton scope + plain int/HashMap = silent lost updates under concurrency"
tags:
  - core
  - concurrency
  - thread-safety
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CORE-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html"
evidence:
  - upstream_id: spring-beans-scopes
    section: Singleton scope semantics in Spring beans
    quote: s Scope Description singleton (Default) Scopes a single bean definition to a single object instance for each Spring IoC container. prototype Scopes a single bean definition to any number of object instances. request Scopes a single bean definition to the lifec
  - source_type: external
    citation: 'Spring Framework Reference — §Bean scopes (singleton default, thread-safety implications)'
    url: 'https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html'
  - source_type: external
    citation: 'Java Concurrency in Practice (Goetz et al., 2006) — §Chapter 3: Sharing Objects'
    url: 'https://jcip.net/'
---

## Singleton beans must not carry unsynchronized mutable state

**Impact: HIGH — Default singleton scope + plain int/HashMap = silent lost updates under concurrency**

Spring's default scope is singleton. A `@Component` with a plain mutable field is one shared instance across all requests; `int count; count++;` is a read-modify-write that races. The result is silent: counters drift, caches miss, audit logs lose entries, with no exception to point at. Either make the state thread-safe (`AtomicLong`, `ConcurrentHashMap`, immutable copies), guard it explicitly, or change the scope (`@Scope("prototype")` per request).

**Incorrect — singleton with unsynchronized mutation:**

```java
@Component
public class MutableSingletonCounter {
    private int count;                    // shared mutable state, races on increment
    public void increment() { count++; }
    public int get() { return count; }
}
```

**Correct — atomic primitive guards the read-modify-write:**

```java
@Component
public class AtomicSingletonCounter {
    private final AtomicLong count = new AtomicLong();
    public void increment() { count.incrementAndGet(); }
    public long get() { return count.get(); }
}
```

Verification: `./gradlew testPractices --tests "*SingletonState*"` runs 32 × 1000 concurrent increments and asserts the atomic counter is exactly equal to the expected total; the unsynchronized counterpart is bounded above by the same total and typically loses updates on real hardware.

Reference: [Spring Framework — Bean scopes](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html)
