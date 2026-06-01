---
title: Per-tenant accumulating quota MUST be claimed atomically — never check-then-increment
impact: HIGH
impactDescription: "A read-then-write quota check admits a TOCTOU race where two concurrent consumers each pass `used < limit` and the committed total exceeds the plan cap — the tenant gets free over-allowance and the counter silently over-runs"
tags:
  - quota
  - multi-tenant
  - concurrency
  - toctou
  - resource-consumption
spec_ref: "specs/per-tenant-resource-quota-l0.yaml#QUOTA-ATOMIC-001"
verification:
  type: review
  source: "specs/per-tenant-resource-quota-l0.yaml#QUOTA-ATOMIC-001"
  pattern: "Quota consume claims headroom in ONE atomic statement — a conditional `UPDATE ... WHERE used + :delta <= limit_value` whose 0-affected-rows means refusal, or `SELECT ... FOR UPDATE` then validate-and-write in the same transaction. No code path reads `used` and writes `used + delta` in two separate statements."
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/"
  - "https://datatracker.ietf.org/doc/html/rfc6585#section-4"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API4:2023 Unrestricted Resource Consumption"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/"
    quote: "Satisfying API requests requires resources such as network bandwidth, CPU, memory, and storage."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "RFC 6585 — Additional HTTP Status Codes, Section 4 (429 Too Many Requests)"
    url: "https://datatracker.ietf.org/doc/html/rfc6585#section-4"
    quote: "The 429 status code indicates that the user has sent too many requests in a given amount of time (\"rate limiting\")."
    quoted_at: "2026-06-01"
---

## Per-tenant accumulating quota MUST be claimed atomically — never check-then-increment

**Impact: HIGH — a read-then-write quota check is a TOCTOU race that lets a tenant exceed its plan cap under concurrency**

A per-tenant accumulating quota (total seats, total stored content GB, monthly job count, AI-token allowance) is a running total bounded by the plan: `used + delta <= limit`. The naive implementation reads the current `used`, compares it in application code, and — if it fits — writes `used + delta` in a second statement. Under any concurrency this loses. Two consume requests for the last seat both `SELECT used` and both see `used = limit - 1`, both pass the in-memory `used < limit` check, and both `UPDATE` — the committed total is `limit + 1`. The tenant got a free seat the plan never sold, and the denormalised counter now over-runs the cap permanently. This is the classic time-of-check-to-time-of-use (TOCTOU) defect, and it is exactly the unbounded-consumption hole OWASP API4:2023 warns about: the cap exists in the schema but is not actually *enforced* because the check and the mutation are not one indivisible operation.

The fix is to make the check and the increment a single atomic step that the database serialises. Two equivalent forms: (1) a **conditional UPDATE** whose `WHERE` clause carries the limit predicate, so the database itself refuses the write and returns 0 affected rows when there is no headroom; or (2) a **`SELECT ... FOR UPDATE`** row lock that forces concurrent claimers to queue, then validate-and-write inside the held lock. Either way, exactly one of two racing claims for the last unit wins; the other is refused with zero increment. The application never holds a stale `used` value across a decision boundary.

This is distinct from a per-window rate limit (`ratelimit-l0`, RFC 6585 token bucket that *refills* over time — a 429 there is transient and retry succeeds) and from a per-user soft cap (single `user_id` subject). Here the subject is the **tenant aggregate** and the total does not refill on the clock — it only changes on consume, release, or billing-period reset. When the claim is refused, return 429 with an RFC 9457 `type=urn:problem:quota-exceeded` body (see `QUOTA-REJECT-001`).

**Incorrect — check-then-increment across two statements; two concurrent consumers both pass and the total exceeds the plan cap:**

```java
@Transactional
public void consume(UUID tenantId, Resource resource, long delta) {
    TenantQuota q = quotaRepo.findByTenantAndResource(tenantId, resource); // read used
    if (q.getUsed() + delta > q.getLimit()) {                              // ❌ check
        throw new QuotaExceededException(resource, q.getLimit(), q.getUsed(), delta);
    }
    q.setUsed(q.getUsed() + delta);   // ❌ separate write — TOCTOU: the row another
    quotaRepo.save(q);                //    transaction is also mutating is read stale
}
// Two consume(last-seat) calls interleave: both read used = limit-1, both pass the
// check, both write limit. Final committed used = limit+1. Cap silently breached.
```

**Correct — single conditional UPDATE; the database enforces the predicate; 0 rows = refused:**

```java
public interface TenantQuotaRepository extends JpaRepository<TenantQuota, UUID> {
    @Modifying
    @Query("""
        UPDATE TenantQuota q
           SET q.used = q.used + :delta
         WHERE q.tenantId = :tenantId
           AND q.resource = :resource
           AND q.used + :delta <= q.limit
        """)
    int tryClaim(UUID tenantId, Resource resource, long delta); // affected rows
}

@Transactional
public void consume(UUID tenantId, Resource resource, long delta) {
    int claimed = quotaRepo.tryClaim(tenantId, resource, delta);
    if (claimed == 0) {                       // ✅ DB refused: no headroom OR lost the race
        TenantQuota q = quotaRepo.findByTenantAndResource(tenantId, resource);
        throw new QuotaExceededException(resource, q.getLimit(), q.getUsed(), delta);
    }
}
// The check (`used + :delta <= limit`) and the increment are ONE statement the
// database serialises per row. Exactly one of two racing last-seat claims gets
// affected-rows = 1; the other gets 0 and is refused. used can never reach limit+1.
```

The `SELECT ... FOR UPDATE` variant is equivalent and preferable when the consume must also touch sibling rows in the same critical section: lock the quota row, re-read `used`, validate, write, all inside the held lock. Pair this rule with `QUOTA-REJECT-001` (deterministic 429 + RFC 9457 `urn:problem:quota-exceeded`) and `QUOTA-RECONCILE-001` (the counter must be reconcilable against the source-of-truth rows and must decrement on release).

Verification (review-tier): there is no static @Tag test that proves an atomic claim — atomicity is a runtime property of the SQL statement and the transaction, not a structurally-detectable shape. A reviewer confirms every quota-consume path issues the limit predicate inside a single conditional UPDATE (0-rows = refused) or a held `SELECT ... FOR UPDATE`, and that no path reads `used` and writes `used + delta` in two separate statements. The behavioural proof is the `QUOTA-ATOMIC-001` integration test: fire two concurrent consume calls against a tenant at `limit - 1` and assert exactly one 2xx + one rejection + final persisted `used == limit` (never `limit + 1`).

Reference: [OWASP API Security Top 10 (2023) — API4:2023 Unrestricted Resource Consumption](https://owasp.org/API-Security/editions/2023/en/0xa4-unrestricted-resource-consumption/)

Reference: [RFC 6585 §4 — 429 Too Many Requests](https://datatracker.ietf.org/doc/html/rfc6585#section-4)
