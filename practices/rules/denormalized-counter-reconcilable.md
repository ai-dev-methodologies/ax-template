---
title: A denormalized usage counter MUST be reconcilable against its source rows — recompute, detect drift, repair, and decrement on release
impact: MEDIUM
impactDescription: "A denormalized counter (a tenant's seat count, stored-bytes total, jobs-this-period) drifts from the source rows it summarizes: a crash between the row write and the counter increment, a release path that forgot to decrement, a double-count. Drift silently over- or under-charges quota — a tenant blocked below their real usage, or allowed past their paid limit. Without a reconciliation routine that recomputes the truth from the source rows and repairs the counter, the drift is permanent and invisible."
tags:
  - reconciliation
  - denormalization
  - quota
  - counter
  - data-integrity
spec_ref: "specs/per-tenant-resource-quota-l0.yaml#QUOTA-RECONCILE-001"
verification:
  type: review
  source: "specs/per-tenant-resource-quota-l0.yaml#QUOTA-RECONCILE-001"
  pattern: "A denormalized accumulated-usage counter MUST be reconcilable against the source-of-truth rows it summarizes (count of active seats / SUM of stored bytes / count of jobs in the period). A reconciliation routine MUST recompute the authoritative total from the source rows (a COUNT/SUM aggregate) and compare it to the stored counter; a non-zero difference is DRIFT and MUST be reported (WARN log / metric) and repaired (the counter reset to the recomputed truth). The counter MUST decrement on the release path — a consume-only counter that never decrements is forbidden (it guarantees monotonic drift upward). Reconciliation MUST be idempotent and run off the hot path (a scheduled sweep). Reject a counter with no reconcile routine, a consume-only counter that never decrements, and a reconcile that silently overwrites without reporting drift."
upstream:
  - "https://www.postgresql.org/docs/current/functions-aggregate.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — Aggregate Functions"
    url: "https://www.postgresql.org/docs/current/functions-aggregate.html"
    quote: "Aggregate functions compute a single result from a set of input values."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A denormalized usage counter MUST be reconcilable against its source rows — recompute, detect drift, repair, decrement on release

**Impact: MEDIUM — A quota check reads a denormalized counter (the tenant's current seat count or stored-bytes total) because recomputing it from the source rows on every check is too slow. But a denormalized value drifts from its source: a crash between writing a source row and incrementing the counter, a release path that deleted the row but forgot to decrement, a double-increment under a retry. The drift is silent and one-directional in practice — usually upward — so a tenant gets blocked below the resources they actually hold, or (drifting down) slips past their paid limit. The defense is reconciliation: the source rows are the truth, and an aggregate recomputes it — per PostgreSQL, *aggregate functions compute a single result from a set of input values* (COUNT/SUM over the source rows) — which a routine compares to the stored counter to detect and repair drift.**

There is one load-bearing requirement for `QUOTA-RECONCILE-001` (composing `quota-atomic-tenant-claim`, which keeps the counter atomic in the first place).

**1. Recomputable from source.** The authoritative total is always derivable from the source-of-truth rows via a COUNT/SUM aggregate. The denormalized counter is an optimization, never the truth.

**2. Drift detection.** A reconciliation routine recomputes the aggregate and compares it to the stored counter. A non-zero difference is DRIFT — it is reported (a WARN log / a metric), not silently swallowed, because sustained drift is a code defect (a missing decrement, a double-count), not normal operation.

**3. Repair.** On detected drift the counter is reset to the recomputed truth, so the system self-heals rather than accumulating error.

**4. Decrement on release.** The counter MUST decrement on the release path (seat removed, object deleted, period rolled). A consume-only counter that only ever increments guarantees monotonic upward drift and is forbidden.

**5. Idempotent, off the hot path.** Reconciliation is idempotent (a no-drift run mutates nothing) and runs as a scheduled sweep, never inline on the quota-check request path.

**Incorrect — consume-only counter, never reconciled; release forgets to decrement:**

```java
void consume(String tenant, long n) { quotaRepo.increment(tenant, n); }     // increments
void release(String tenant, Seat s) { seatRepo.delete(s); }                  // VIOLATION: counter NOT decremented (drifts up)
// VIOLATION: no reconcile routine → drift between counter and active-seat rows is permanent (QUOTA-RECONCILE-001)
```

**Correct — decrement on release; scheduled idempotent reconcile recomputes from source and repairs drift:**

```java
void release(String tenant, Seat s) {                       // decrement on release (QUOTA-RECONCILE-001)
    seatRepo.delete(s);
    quotaRepo.decrement(tenant, 1);
}
@Scheduled(fixedDelay = RECONCILE_INTERVAL)
void reconcile() {                                          // off the hot path, idempotent
    for (String tenant : tenants.all()) {
        long truth = seatRepo.countActiveByTenant(tenant);  // COUNT aggregate over source rows = authoritative
        long stored = quotaRepo.current(tenant);
        if (truth != stored) {
            log.warn("quota drift tenant={} stored={} truth={}", tenant, stored, truth); // detect + report
            quotaRepo.set(tenant, truth);                   // repair to the recomputed truth
        }
    }
}
```

Verification: review-tier. Counter correctness is a data-integrity property with no compile-time signal — a consume-only counter compiles and works until a release path drifts it. Verify by review against `specs/per-tenant-resource-quota-l0.yaml#QUOTA-RECONCILE-001`: the counter is recomputable from source rows; a reconcile routine detects drift, reports it, and repairs to the recomputed truth; the counter decrements on release; reconciliation is idempotent and off the hot path. When a fork-receiver wires a real IT (induce drift, run reconcile, assert the counter equals the source aggregate), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — Aggregate Functions](https://www.postgresql.org/docs/current/functions-aggregate.html)
