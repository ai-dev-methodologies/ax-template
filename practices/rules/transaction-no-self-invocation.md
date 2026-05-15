---
title: Do not self-invoke @Transactional methods
impact: HIGH
impactDescription: "Self-invocation silently bypasses transaction advice, breaking atomicity"
tags:
  - transaction
  - aop
  - spring-proxy
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: Self-invocation and the proxy
    quote: ed. This means that self-invocation (in effect, a method within the target object calling another method of the target object) does not lead to an actual transaction at runtime even if the invoked method is marked with @Transactional . Also, the proxy must be
  - upstream_id: spring-aop-proxying
    section: Understanding AOP proxies
    quote: ng do not have this self-invocation issue because they apply advice within the bytecode instead of via a proxy. Mixing Aspect Types Programmatic Creation of @AspectJ Proxies Spring Framework Stable 7.0.7 6.2.18 Snapshot 7.1.0-SNAPSHOT 7.0.8-SNAPSHOT 6.2.19-SNA
  - source_type: external
    citation: 'Spring Framework Reference — §Declarative transaction management: Method visibility (proxy mechanism)'
    url: 'https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html'
  - source_type: external
    citation: 'Spring Framework Reference — §Understanding AOP proxies (self-invocation)'
    url: 'https://docs.spring.io/spring-framework/reference/core/aop/proxying.html'
---

## Do not self-invoke @Transactional methods

**Impact: HIGH — Self-invocation silently bypasses transaction advice, breaking atomicity**

Spring `@Transactional` is implemented by an AOP proxy. When a public method on a service calls another method on the same instance via `this.method()`, the call goes directly to the underlying class — the proxy never sees it, and the `@Transactional` advice is skipped. The bug is silent: no exception, no log, just no transaction. Failures partway through never roll back, dirty reads slip through, and audit logs lose causality.

**Incorrect — self-invocation skips the proxy:**

```java
@Service
public class ReportService {
    public void generate() {
        this.persistReport();   // direct call, proxy bypassed → no transaction
    }

    @Transactional
    public void persistReport() {
        repo.saveAll(...);
    }
}
```

**Correct — invoke through a separate bean (proxy is honored):**

```java
@Service
public class ReportService {
    private final ReportPersistence persistence;
    public ReportService(ReportPersistence persistence) {
        this.persistence = persistence;
    }
    public void generate() {
        persistence.persistReport();   // through proxy → @Transactional honored
    }
}

@Service
public class ReportPersistence {
    @Transactional
    public void persistReport() {
        repo.saveAll(...);
    }
}
```

Verification: `./gradlew testPractices --tests "*SelfInvocation*"` asserts `TransactionSynchronizationManager.isActualTransactionActive()` is `false` after self-invocation and `true` after proxy invocation.

Reference: [Spring Framework — Declarative transaction management](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
