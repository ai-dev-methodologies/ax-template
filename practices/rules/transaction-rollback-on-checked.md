---
title: Declare rollbackFor when the method throws a checked exception
impact: HIGH
impactDescription: "Default rollback policy ignores checked exceptions — half-done writes commit"
tags:
  - transaction
  - rollback
  - exception
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-003
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring @Transactional — rollbackFor attribute"
    quote: "rollback"
  - source_type: external
    citation: "Spring Framework Reference — Rolling back a declarative transaction"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html"
---

## Declare rollbackFor when the method throws a checked exception

**Impact: HIGH — Default rollback policy ignores checked exceptions — half-done writes commit**

`@Transactional` rolls back the transaction only when the method throws an *unchecked* exception (`RuntimeException` or `Error`). A checked exception that escapes the method — `IOException`, `JsonProcessingException`, the project's own domain checked exceptions — is treated as a successful return: the transaction commits, half-done writes persist, and the caller sees an exception thrown over fully-committed state. The remedy is to declare `rollbackFor = Exception.class` (or the narrower checked types) on the annotation.

**Incorrect — default rollback policy hides checked-exception failures:**

```java
@Transactional
public void persistReport(Report r) throws IOException {
    repo.save(r);
    fileSink.write(r);          // throws IOException — repo.save already committed
}
```

**Correct — rollbackFor declares the contract:**

```java
@Transactional(rollbackFor = Exception.class)
public void persistReport(Report r) throws IOException {
    repo.save(r);
    fileSink.write(r);          // IOException now rolls back the save
}
```

Verification: `./gradlew testPractices --tests "*RollbackForChecked*"` asserts via reflection that the correct fixture declares `rollbackFor = Exception.class` and the anti-pattern fixture leaves it empty.

Reference: [Spring Framework — Rolling back declarative transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html)
