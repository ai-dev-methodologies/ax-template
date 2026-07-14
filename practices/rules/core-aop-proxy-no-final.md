---
title: Do not mark proxied beans (or their public methods) as final
impact: HIGH
impactDescription: "CGLIB cannot subclass a final type — @Transactional / @Async advice is silently dropped"
tags:
  - core
  - aop
  - proxy
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-CORE-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/core/aop/proxying.html"
evidence:
  - upstream_id: spring-aop-proxying
    section: Proxying mechanisms — CGLIB final-class/method restriction
    quote: "final classes cannot be proxied, because they cannot be extended. final methods cannot be advised, because they cannot be overridden."
  - source_type: external
    citation: 'Spring Framework Reference — §Proxying mechanisms (CGLIB final-class restriction)'
    url: 'https://docs.spring.io/spring-framework/reference/core/aop/proxying.html'
  - source_type: external
    citation: "Baeldung — Spring's CGLIB Proxy Limitations"
    url: 'https://www.baeldung.com/spring-aop-vs-aspectj#proxy-types'
---

## Do not mark proxied beans (or their public methods) as final

**Impact: HIGH — CGLIB cannot subclass a final type — @Transactional / @Async advice is silently dropped**

Spring applies AOP advice (`@Transactional`, `@Async`, `@Cacheable`, custom aspects) by wrapping the bean in a CGLIB-generated subclass that overrides the public methods. When the bean's class is `final` — or a public method is `final` — CGLIB cannot subclass / override, the proxy is not produced, and the annotation is effectively a no-op. The bug is silent: no exception at startup, no log line at the call site, just missing advice.

**Incorrect — final on the bean class:**

```java
@Service
public final class ReportService {       // CGLIB cannot subclass — @Transactional dropped
    @Transactional
    public void persist() { ... }
}
```

**Correct — non-final class and non-final public methods:**

```java
@Service
public class ReportService {              // subclass-able by CGLIB; @Transactional honored
    @Transactional
    public void persist() { ... }
}
```

Verification: `./gradlew testPractices --tests "*AopFinalClass*"` asserts `AopUtils.isAopProxy(bean)` is true and that neither the class nor its public methods are final.

Reference: [Spring Framework — Proxying mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
