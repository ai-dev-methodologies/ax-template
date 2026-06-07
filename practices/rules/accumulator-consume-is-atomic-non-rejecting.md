---
title: A consume against a drawdown threshold (deductible / copay / budget / data-cap) must be ONE atomic non-rejecting partial draw — applied = min(delta, headroom), advance by applied, return the residual — never a read-then-write, never a total refusal
impact: HIGH
impactDescription: "An accumulator drawn with a check-then-write loses concurrent draws (two family-member claims both read the same headroom and both apply it, over-drawing the deductible — CWE-362); rejecting a valid covered draw, or rounding applied and residual independently, silently destroys or conjures money on every claim"
tags:
  - concurrency
  - bigdecimal
  - allocation
  - conservation
  - accumulator
spec_ref: "specs/accumulator-consume-l0.yaml#ACC-ATOMIC-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/costshare/AccumulatorRepository.java + backend/src/main/java/com/ax/template/authblueprint/costshare/CostShareService.java"
  pattern: "A consume reads the accumulator row under PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) inside the same transaction (or issues a single conditional UPDATE ... SET used = used + LEAST(:delta, limit-used) ... RETURNING), computes applied = min(delta, limit-used) and residual = delta-applied in BigDecimal, advances used by exactly applied, and NEVER returns a rejection for an over-the-limit valid draw; no read-the-used-then-write-in-a-separate-statement appears on any consume path; applied+residual==delta is asserted (compareTo==0)"
upstream:
  - "https://www.postgresql.org/docs/current/transaction-iso.html"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
  - "https://martinfowler.com/eaaCatalog/money.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — 'Transaction Isolation' (Read Committed: the WHERE clause is re-evaluated against the freshly-committed row)"
    url: "https://www.postgresql.org/docs/current/transaction-iso.html"
    quote: "The search condition of the command (the WHERE clause) is re-evaluated to see if the updated version of the row still matches the search condition. If so, the second updater proceeds with its operation using the updated version of the row."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 'Explicit Locking' (row-level FOR UPDATE)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Martin Fowler — Patterns of Enterprise Application Architecture, Money pattern (the penny-conservation rounding hazard)"
    url: "https://martinfowler.com/eaaCatalog/money.html"
    quote: "The more subtle problem is with rounding. Monetary calculations are often rounded to the smallest currency unit. When you do this it's easy to lose pennies (or your local equivalent) because of rounding errors."
    quoted_at: "2026-06-01"
---

## A consume against a drawdown threshold must be ONE atomic non-rejecting partial draw

**Impact: HIGH — a check-then-write accumulator over-draws under concurrency; rejecting a valid covered draw, or rounding `applied`/`residual` independently, destroys or conjures money on every claim.**

This rule is the **non-rejecting dual** of `shared-counter-claim-must-be-atomic.md`. That rule governs a counter that must REFUSE the loser (a seat, an inventory unit, a tenant quota: `affected-rows == 0 → 409`, "refusal is total"). An *accumulator* is the opposite posture: a deductible, a copay, an out-of-pocket-max, a metered-overage allowance, a cloud-budget burndown, a loyalty-tier meter. A draw against it must **never reject** a valid covered amount — it absorbs as much as fits under the limit and hands back the rest:

```text
applied  = min(delta, limit - used)     // how much fits under the watermark
used    += applied                       // advance by exactly that
residual = delta - applied               // what spilled over, for the caller to carry forward
// invariant: applied + residual == delta   (exact BigDecimal, never two independent roundings)
```

Three defects recur, and one rule closes them.

**Defect 1 — check-then-write over-draws (CWE-362).** Reading `used` in one statement and writing `used + applied` in a later statement is a race: two family-member claims both read `used = 1460` against a `1500` deductible, both compute `applied = 40`, both write `1500` — but `40 + 40` of member money was applied against `40` of headroom, over-drawing the deductible and under-charging the member by `$40`. This is CWE-362 exactly: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."* A single-threaded test never shows it.

**Defect 2 — rejecting a valid draw (wrong posture).** Copying the bounded-capacity-claim reflex — "over the limit → 409" — is wrong here. A claim that exceeds the remaining deductible is still a valid claim; the accumulator must absorb the headroom and return the residual so it flows to the next tier (coinsurance, OOP-max — see `ordered-waterfall-l0`). Refusing it strands the claim.

**Defect 3 — independently rounding `applied` and `residual` loses a unit.** Computing `applied` and `residual` as two separately-rounded BigDecimals breaks `applied + residual == delta` — the Money pattern's penny hazard: *"it's easy to lose pennies … because of rounding errors."* Compute one side, derive the other as the exact difference.

**Incorrect — read-then-write (races) + reject-on-over (wrong posture) + double rounding:**

```java
public BigDecimal applyDeductible(String key, BigDecimal delta) {
    Accumulator a = repo.findByScopeKey(key).orElseThrow();   // ❌ plain read
    BigDecimal headroom = a.getLimit().subtract(a.getUsed());
    if (delta.compareTo(headroom) > 0) {
        throw new QuotaExceededException();                    // ❌ DEFECT 2: rejects a valid claim
    }
    // ❌ DEFECT 1: another tx committed between the read above and the save below
    a.setUsed(a.getUsed().add(delta));
    repo.save(a);
    return BigDecimal.ZERO;                                    // (no residual concept at all)
}
```

**Correct — pessimistic row lock + non-rejecting partial draw + residual as the exact difference:**

```java
@Transactional
public ConsumeResult consume(String key, BigDecimal delta) {
    Accumulator a = repo.findByScopeKeyForUpdate(key)         // ✅ SELECT ... FOR UPDATE, same tx
        .orElseThrow(CostShareException::notFound);
    BigDecimal headroom = a.getLimit().subtract(a.getUsed());
    BigDecimal applied  = delta.min(headroom).max(BigDecimal.ZERO);  // ✅ min(delta, headroom), never rejects
    BigDecimal residual = delta.subtract(applied);            // ✅ exact difference → applied+residual==delta
    a.advanceUsed(applied);                                   // used += applied (CHECK used<=limit backstop)
    return new ConsumeResult(applied, residual);              // ✅ caller carries residual to the next tier
}
```

The pessimistic `FOR UPDATE` serializes concurrent consumers on the one accumulator row (*"This prevents them from being … modified … by other transactions until the current transaction ends"*); each racing claim is served a deterministic partial `applied`, the partials sum to exactly the headroom, and `used` lands exactly on `limit` — never past it. The single conditional `UPDATE ... SET used = used + LEAST(:delta, limit-used) ... RETURNING` is the equivalent one-statement form on PostgreSQL (Read Committed re-evaluates the arithmetic against the freshly-committed row). A reversal/clawback runs the same atomic shape with a negative adjustment floored by `CHECK (used >= 0)`.

Verification: review-tier — confirm every consume path locks the row (`FOR UPDATE` / `@Lock(PESSIMISTIC_WRITE)`) or uses the single `LEAST` UPDATE, computes `applied = min(delta, limit-used)` and `residual` as the exact difference in `BigDecimal`, never rejects a valid over-limit draw, and never reads-then-writes across two statements. The canonical proof a fork-receiver writes is a concurrency test: N claims racing the last `H` of headroom, asserting Σ`applied` == `H` and final `used` == `limit`.

Reference: [PostgreSQL — Transaction Isolation (Read Committed)](https://www.postgresql.org/docs/current/transaction-iso.html)

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)

Reference: [Martin Fowler — Money pattern](https://martinfowler.com/eaaCatalog/money.html)
