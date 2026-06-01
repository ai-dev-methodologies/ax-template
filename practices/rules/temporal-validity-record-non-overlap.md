---
title: Effective-dated records MUST forbid overlapping validity windows with a DB range-exclusion constraint — never a pre-insert overlap SELECT
impact: HIGH
impactDescription: "A service-layer 'does this window overlap?' SELECT before INSERT is a CWE-367 TOCTOU race: two concurrent effective-dating writes each see no overlap and both insert, corrupting the history so a point-in-time query returns two conflicting rows for one instant"
tags:
  - temporal
  - effective-dated
  - interval-overlap
  - cwe-367
  - toctou
  - postgres-exclude
spec_ref: "specs/temporal-validity-l0.yaml#TEMPORAL-NON-OVERLAP-001"
verification:
  type: review
  source: "specs/temporal-validity-l0.yaml#TEMPORAL-NON-OVERLAP-001"
  pattern: "A scope-keyed effective-dated table (employee salary/title/manager history, price-over-time, coverage periods, config-over-time, shift rosters, leases) MUST carry a DB range-exclusion constraint — EXCLUDE USING gist (scope_key WITH =, tstzrange(valid_from, valid_to, '[)') WITH &&) requiring btree_gist — as the AUTHORITATIVE non-overlap guard. Reject any handler whose ONLY overlap defense is a service-layer 'SELECT ... WHERE ranges overlap; if none, INSERT' — that read-check-then-insert window is the CWE-367 TOCTOU race. The range MUST be half-open '[)' so touching windows (end == next start) do not collide. Violation MUST map to a deterministic 409 INTERVAL_OVERLAP, distinct from 409 CAPACITY_EXHAUSTED (bounded-capacity-claim) and 412 (optimistic-locking). This is a runtime concurrency property with no compile-time signal, so it is verified by review against the spec, not by a static @Tag test."
upstream:
  - "https://www.postgresql.org/docs/current/ddl-constraints.html"
  - "https://www.postgresql.org/docs/current/btree-gist.html"
  - "https://cwe.mitre.org/data/definitions/367.html"
  - "https://en.wikipedia.org/wiki/Temporal_database"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — 5.4 Constraints (Exclusion Constraints, EXCLUDE USING gist)"
    url: "https://www.postgresql.org/docs/current/ddl-constraints.html"
    quote: "Exclusion constraints ensure that if any two rows are compared on the specified columns or expressions using the specified operators, at least one of these operator comparisons will return false or null."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — F.8 btree_gist (GiST operator classes with B-tree behavior)"
    url: "https://www.postgresql.org/docs/current/btree-gist.html"
    quote: "In addition to the typical B-tree search operators, btree_gist also provides index support for <> (\"not equals\"). This may be useful in combination with an exclusion constraint, as described below."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition"
    url: "https://cwe.mitre.org/data/definitions/367.html"
    quote: "The product checks the state of a resource before using that resource, but the resource's state can change between the check and the use in a way that invalidates the results of the check."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Temporal database — Wikipedia (valid time semantics)"
    url: "https://en.wikipedia.org/wiki/Temporal_database"
    quote: "Valid time is the time period during or event time at which a fact is true in the real world."
    quoted_at: "2026-06-01"
decided_at: "2026-06-01"
---

## Effective-dated records MUST forbid overlapping validity windows with a DB range-exclusion constraint — never a pre-insert overlap SELECT

**Impact: HIGH — An effective-dated table is any history where a fact is true only over a bounded period: an employee's salary/title/manager over time, a product's price-over-time, an insurance coverage period, a config value over time, a shift roster, a lease. Each row carries a [validFrom, validTo) validity window under a scope key (employeeId, productId, policyId). The invariant is that no two rows for the same scope key may have overlapping windows — otherwise a point-in-time query for one instant returns two conflicting facts. The trap is to enforce that invariant in application code with a 'does this window overlap an existing one?' SELECT just before the INSERT. That is a time-of-check/time-of-use race (CWE-367): between the overlap SELECT and the INSERT a concurrent transaction can commit a colliding window the first transaction's SELECT never saw, so both pass the check and both insert. The history is now corrupt, and the corruption is invisible until someone asks 'what was the salary in force on 2026-01-14' and gets two answers.**

The failure is CWE-367: the code checks the state of a resource (no overlapping window exists) before using it (inserting), but the resource's state can change between the check and the use in a way that invalidates the check. A single-threaded test never reveals it — the pre-insert SELECT looks correct — but two concurrent effective-dating writes (a payroll batch and an HR admin editing the same employee, two price-update jobs on the same SKU) each take their snapshot before the other commits, each see no overlap, and both insert.

The authoritative fix moves the non-overlap check INTO the database as a range-exclusion constraint, so the check and the write are one atomic operation evaluated at COMMIT — no application-visible window exists to race. PostgreSQL exclusion constraints "ensure that if any two rows are compared on the specified columns or expressions using the specified operators, at least one of these operator comparisons will return false or null." For effective-dating the operators are `=` on the scope key (same entity) and `&&` (range overlap) on the validity window; the constraint rejects any pair where the scope keys are equal AND the windows overlap. The equality side requires the `btree_gist` extension, which provides "GiST index operator classes that implement B-tree equivalent behavior" so the scope-key `=` can sit in the same GiST index as the range `&&`.

Two supporting invariants make the geometry correct. The range MUST be **half-open** `[validFrom, validTo)` — `tstzrange(valid_from, valid_to, '[)')` — so two adjacent windows that touch at a shared instant (window A ending 12:00, window B starting 12:00) are contiguous WITHOUT overlapping; a closed-closed `[]` range would make 12:00 belong to both and trip a false collision. And a violation MUST surface as a deterministic **409 INTERVAL_OVERLAP**, distinct from the 409 CAPACITY_EXHAUSTED of `bounded-capacity-claim` (a shared-counter exhaustion, not interval geometry) and the 412 Precondition Failed of `optimistic-locking` (a stale If-Match validator) — three different conflicts a client branches on differently.

**Incorrect — service-layer pre-insert overlap SELECT: two concurrent effective-dating writes both pass the check and both insert (CWE-367 TOCTOU):**

```java
@Transactional
public SalaryRecord setSalary(long employeeId, BigDecimal amount, Instant validFrom, Instant validTo) {
    // VIOLATION: check the state ...
    List<SalaryRecord> overlapping = salaryRepo.findOverlapping(employeeId, validFrom, validTo);
    if (!overlapping.isEmpty()) {
        throw new IntervalOverlapException(employeeId);
    }
    // ... then use it. Between the SELECT above and this INSERT a concurrent
    // transaction can commit a colliding window this snapshot never saw —
    // both see "no overlap", both insert, the history now overlaps at one instant.
    return salaryRepo.save(new SalaryRecord(employeeId, amount, validFrom, validTo));
}
```

**Correct — DB range-exclusion constraint is the authoritative guard; the overlap check and the write are one atomic step at COMMIT, no window to race:**

```sql
-- migration (Vxxx): the exclusion constraint IS the invariant
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE salary_record (
    id          BIGSERIAL PRIMARY KEY,
    employee_id BIGINT      NOT NULL,          -- the scope key
    amount      NUMERIC(19,4) NOT NULL,
    valid_from  TIMESTAMPTZ NOT NULL,
    valid_to    TIMESTAMPTZ,                   -- NULL == open-ended current row
    -- no two rows for the SAME employee may have OVERLAPPING half-open windows.
    -- '=' on employee_id (btree_gist) + '&&' on the tstzrange (GiST).
    EXCLUDE USING gist (
        employee_id WITH =,
        tstzrange(valid_from, valid_to, '[)') WITH &&
    )
);
```

```java
@Transactional
public SalaryRecord setSalary(long employeeId, BigDecimal amount, Instant validFrom, Instant validTo) {
    try {
        // No pre-insert SELECT. The DB exclusion constraint decides atomically
        // at COMMIT; a concurrent colliding window loses deterministically.
        return salaryRepo.saveAndFlush(new SalaryRecord(employeeId, amount, validFrom, validTo));
    } catch (DataIntegrityViolationException e) {
        if (isExclusionViolation(e)) {                        // SQLSTATE 23P01
            temporalMetrics.intervalOverlap("salary");         // TEMPORAL-OBSERVABILITY-001
            throw new IntervalOverlapException(employeeId);    // → 409 INTERVAL_OVERLAP
        }
        throw e;
    }
}
```

**This is distinct from capacity and from optimistic-locking.** `bounded-capacity-claim` (CLAIM-ATOMIC-001) serializes claimants over a cardinality COUNTER — `taken < capacity` — which is set cardinality, not interval geometry; its loser gets 409 CAPACITY_EXHAUSTED. `optimistic-locking` (OPTLOCK-CONFLICT-001) rejects a stale validator with 412. This rule is interval geometry: no two windows for one scope key may overlap, enforced by a range-exclusion constraint, loser gets 409 INTERVAL_OVERLAP. A recipe whose invariant is "must not double-book overlapping time windows" (e.g. booking) belongs here — on `TEMPORAL-NON-OVERLAP-001` — NOT on the capacity counter, because double-booking is two intervals colliding, not a counter exhausting.

Verification: review-tier. Non-overlap under concurrency is a runtime property — a single-threaded test passes even when the only guard is the broken pre-insert SELECT, and no compile-time signal exists. Verify by review against `specs/temporal-validity-l0.yaml#TEMPORAL-NON-OVERLAP-001`: confirm the effective-dated table carries the `EXCLUDE USING gist (... WITH =, tstzrange(...,'[)') WITH &&)` constraint and `CREATE EXTENSION btree_gist`, and that no handler relies on a pre-insert overlap SELECT as its sole guard. Prove it with a concurrent-insert race harness (two parallel writers submitting overlapping windows for one scope key) asserting exactly one success + one 409 INTERVAL_OVERLAP + a final history with zero overlapping rows. When a fork-receiver wires a real `@Tag("TEMPORAL-NON-OVERLAP-001")` concurrency IT, this rule's verification block may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — Constraints (Exclusion Constraints, EXCLUDE USING gist)](https://www.postgresql.org/docs/current/ddl-constraints.html)

Reference: [PostgreSQL — btree_gist (F.8 — GiST operator classes with B-tree behavior)](https://www.postgresql.org/docs/current/btree-gist.html)

Reference: [CWE-367 — Time-of-check Time-of-use (TOCTOU) Race Condition](https://cwe.mitre.org/data/definitions/367.html)

Reference: [Temporal database — valid time (Wikipedia)](https://en.wikipedia.org/wiki/Temporal_database)
