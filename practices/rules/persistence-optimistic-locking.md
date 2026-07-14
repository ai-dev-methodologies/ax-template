---
title: Add @Version to entities updated under concurrent traffic
impact: HIGH
impactDescription: "Without @Version, concurrent updates silently lose one of them (last-writer-wins)"
tags:
  - persistence
  - jpa
  - concurrency
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-PERS-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-PERS-004
upstream:
  - "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
evidence:
  - upstream_id: spring-jpa-locking
    section: "Spring Data JPA — @Lock annotation on query methods"
    quote: "To specify the lock mode to be used, you can use the @Lock annotation on query methods"
  - source_type: external
    citation: "Hibernate User Guide — Optimistic Locking"
    url: "https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic"
---

## Add @Version to entities updated under concurrent traffic

**Impact: HIGH — Without @Version, concurrent updates silently lose one of them (last-writer-wins)**

Two transactions that both `findById(...)`, modify the same row, and persist will both succeed under the default last-writer-wins policy. One of the two updates is gone with no exception, no log line, no record. JPA's `@Version` column closes the gap: every persist increments it, and a commit that carries a stale version throws `OptimisticLockException` (Spring Data wraps it as `ObjectOptimisticLockingFailureException`). The caller can retry the operation or surface the conflict.

**Incorrect — no version column, silent lost update:**

```java
@Entity
public class Account {
    @Id @GeneratedValue Long id;
    long balance;
    // no @Version — concurrent updates race
}
```

**Correct — @Version on the entity:**

```java
@Entity
public class Account {
    @Id @GeneratedValue Long id;
    long balance;

    @Version
    long version;          // bumped by JPA on each persist
}
```

Verification: `./gradlew testPractices --tests "*OptimisticLocking*"` persists an entity, races two stale references, and asserts the loser throws `ObjectOptimisticLockingFailureException` / `OptimisticLockException`.

Reference: [Hibernate User Guide — Optimistic Locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)
