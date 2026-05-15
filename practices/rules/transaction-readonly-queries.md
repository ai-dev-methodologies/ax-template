---
title: Mark read-only queries with @Transactional(readOnly = true)
impact: MEDIUM
impactDescription: "Skips dirty-checking + enables replica routing — silent perf win when set, silent overhead when forgotten"
tags:
  - transaction
  - performance
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-002"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-002
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring @Transactional attributes — readOnly"
    quote: "@Transactional"
  - source_type: external
    citation: "Spring Framework Reference — Declarative transaction management"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
---

## Mark read-only queries with @Transactional(readOnly = true)

**Impact: MEDIUM — Skips dirty-checking + enables replica routing — silent perf win when set, silent overhead when forgotten**

`@Transactional(readOnly = true)` is more than documentation. JPA / Hibernate uses the flag to skip the dirty-checking pass at flush time; a `ReplicaAwareDataSource` / `LazyConnectionDataSourceProxy` reads the flag to route the connection to a read replica. Forgetting it on a query-only path is silent — the data still returns, but with full read-write overhead and (when configured) on the primary.

**Incorrect — default @Transactional on a read-only method:**

```java
@Service
public class OrderQueryService {
    @Transactional                                // read-write semantics on a query
    public List<OrderSummary> recentOrders(...) { ... }
}
```

**Correct — explicit readOnly flag:**

```java
@Service
public class OrderQueryService {
    @Transactional(readOnly = true)               // dirty-check skipped, replica-routable
    public List<OrderSummary> recentOrders(...) { ... }
}
```

Verification: `./gradlew testPractices --tests "*TransactionReadOnly*"` asserts `TransactionSynchronizationManager.isCurrentTransactionReadOnly()` is `true` inside the read-only method and `false` inside the default method.

Reference: [Spring Framework — Declarative transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
