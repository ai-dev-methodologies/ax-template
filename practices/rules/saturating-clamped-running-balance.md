---
title: A saturating balance clamps AT its ceiling on accrual and AT zero on debit — it never errors and never stores an out-of-range value — while every operation records BOTH the requested and applied (post-clamp) amount, append-only, and concurrent accrual near the ceiling converges to EXACTLY the cap
impact: HIGH
impactDescription: "A clamp-on-write balance that errors instead of clamping breaks the 'accumulate up to a max, spend down to zero' contract callers rely on (a leave-accrual or points system that rejects instead of capping surprises users); one that clamps but does not record the requested-vs-applied split silently discards the excess with no audit trail; and without row-level serialization, concurrent accrual near the ceiling can overshoot the cap or silently drop a caller's request (CWE-362)"
tags:
  - conservation
  - concurrency
  - audit
spec_ref: "specs/saturating-balance-l0.yaml#SATBAL-CEILING-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/saturatingbalance/SaturatingBalanceService.java + backend/src/main/java/com/ax/template/authblueprint/saturatingbalance/Balance.java + backend/src/main/java/com/ax/template/authblueprint/saturatingbalance/LedgerEntry.java"
  pattern: "accrue/debit compute the clamped applied amount (min(requested, cap-current) for accrual; min(requested, current) for debit) under the balance row's PESSIMISTIC_WRITE lock; the stored balance is updated by the applied amount only, never the raw requested amount; a @Check(balance BETWEEN 0 AND cap) DB backstop makes an out-of-range stored value unrepresentable; every operation appends an immutable LedgerEntry recording both requested and applied amounts, no update path; Σ(applied) for a balance always reconciles to its current stored value"
upstream:
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent accrual/debit racing the same balance row near its bound)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-07-14"
---

## Clamp, don't reject — but never clamp silently

**Impact: HIGH — a balance that errors instead of clamping breaks the "accumulate to a cap, spend to zero" contract; clamping without recording the requested-vs-applied split silently discards the excess; unsynchronized concurrent accrual near the cap can overshoot it or drop a caller's request (CWE-362).**

The catalog already has a balance that REJECTS an operation that would breach a bound (`two-axis-inventory-reservation-l0` — insufficient stock is an explicit 409/422 denial) and a counter that only ever accrues with no ceiling (`monotone-register-l0`). Neither models the shape many real balances actually have: a leave/PTO accrual capped at a policy maximum, a loyalty-points balance that never overdraws below zero. Both directions of this balance ABSORB instead of rejecting — and the primitive's whole value is making sure "absorbed" never means "silently lost".

**Incorrect — errors on overflow, or clamps without recording what was actually asked for:**

```java
// <!-- catalog-example-ok: BalanceService — illustrative anti-pattern, not a shipped symbol -->
@Transactional
public void accrue(UUID balanceId, BigDecimal amount) {
    Balance b = balances.findByIdForUpdate(balanceId).orElseThrow();
    if (b.getCurrent().add(amount).compareTo(b.getCap()) > 0) {
        throw new IllegalStateException("would exceed cap");   // ❌ this balance shape should CLAMP, not reject
    }
    b.setCurrent(b.getCurrent().add(amount));                  // ❌ no ledger entry — requested vs applied is lost
}
```

**Correct — clamp under the row lock; record requested AND applied on an immutable ledger entry:**

```java
@Transactional
public LedgerEntry accrue(UUID balanceId, BigDecimal requested) {
    Balance b = balances.findByIdForUpdate(balanceId).orElseThrow(SaturatingBalanceException::notFound);
    BigDecimal headroom = b.getCap().subtract(b.getCurrent());
    BigDecimal applied = requested.min(headroom).max(BigDecimal.ZERO);   // SATBAL-CEILING-001 — clamp, never error
    b.applyAccrual(applied);                                             // stored value moves by `applied` only
    return members.persist(new LedgerEntry(UUID.randomUUID(), b.getId(),
        LedgerOp.ACCRUE, requested, applied, Instant.now(clock)));       // SATBAL-LEDGER-003 — both amounts, append-only
}
```

**1. Ceiling clamp (SATBAL-CEILING-001) / floor clamp (SATBAL-FLOOR-002).** Accrual clamps at the cap; debit clamps at zero. Neither ever throws or rejects — a `@Check` DB backstop makes an out-of-range stored value unrepresentable even if the application logic regresses.

**2. Requested vs applied, append-only (SATBAL-LEDGER-003).** Every operation's ledger entry carries BOTH values. When clamping occurred, `requested != applied`, and the gap is a permanent, auditable fact — never silently dropped. `Σ(applied)` for a balance always reconciles to its current stored value.

**3. Concurrency (SATBAL-CONCURRENT-004 — keystone).** The balance row's `PESSIMISTIC_WRITE` lock is the same serialization discipline `remeasurement-trueup-l0` (TUP-CONCURRENT-001) and `external-reconciliation-l0` (RECON-CONCURRENT-001) already established: under N concurrent accruals near the cap, the final balance converges to EXACTLY the cap, and every caller still gets its own ledger entry — none silently dropped (CWE-362).

Verification: review-tier — confirm accrue/debit never throw on an in-range-violating request (they clamp), the `@Check` constraint exists on both the entity and the migration, every ledger entry records requested and applied with no update path, and the balance row is locked before any read-modify-write.

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
