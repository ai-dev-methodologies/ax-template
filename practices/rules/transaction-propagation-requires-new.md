---
title: Use Propagation.REQUIRES_NEW for writes that must persist independently
impact: MEDIUM
impactDescription: "Audit logs / side-effect writes lost when the caller's outer transaction rolls back"
tags:
  - transaction
  - propagation
  - audit
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TX-004"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TX-004
upstream:
  - "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html"
evidence:
  - upstream_id: spring-tx-declarative
    section: "Spring @Transactional propagation — REQUIRED vs REQUIRES_NEW"
    quote: "propagation"
  - source_type: external
    citation: "Spring Framework Reference — Transaction Propagation"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html"
---

## Use Propagation.REQUIRES_NEW for writes that must persist independently

**Impact: MEDIUM — Audit logs / side-effect writes lost when the caller's outer transaction rolls back**

The default propagation is `REQUIRED`: the method joins the caller's transaction (or starts one if none exists). That is correct for almost every business operation. But audit logs, billing side-effects, outbox writes, and other "this must commit regardless of the caller's success/failure" writes must run in `Propagation.REQUIRES_NEW` — the framework suspends the outer transaction, opens a new one for the inner method, commits it, and resumes the outer. Otherwise an outer rollback silently swallows the audit record that was supposed to survive.

**Incorrect — default REQUIRED for an audit write:**

```java
@Service
public class TransferService {
    @Transactional
    public void transfer(...) {
        accounts.debit(...);
        accounts.credit(...);
        auditWriter.record(...);   // joins this tx — outer rollback loses the audit row
        throw new BusinessRuleViolation();
    }
}
```

**Correct — REQUIRES_NEW for the audit write:**

```java
@Service
public class AuditWriter {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent e) {
        auditRepo.save(e);
    }
}
```

Verification: `./gradlew testPractices --tests "*PropagationRequiresNew*"` opens an outer `@Transactional` test method, calls a `REQUIRES_NEW` bean, and asserts the inner transaction name differs from the outer (proving the suspend / new-tx semantics).

Reference: [Spring Framework — Transaction Propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
