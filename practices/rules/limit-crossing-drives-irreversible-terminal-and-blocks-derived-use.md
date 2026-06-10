---
title: A cumulative register with a mandatory limit (life-limit / usage ceiling) must convert the crossing accrual into an IRREVERSIBLE terminal state in the SAME transaction — zero outgoing edges, late accrual rejected (409), and the DERIVED capability (install / dispatch / use) fail-closed on the same locked row; never a live row whose anchor ≥ limit
impact: HIGH
impactDescription: "Committing an anchor at/over the limit while the lifecycle stays live leaves a window in which an expired asset keeps accruing and keeps being USED (an over-life part installed, an over-hours driver dispatched); checking the limit in a separate transaction from the accrual races (CWE-362) and admits a use concurrent with the crossing; a reversible terminal (un-expire / reset) silently erases the retirement evidence the whole control exists to keep"
tags:
  - concurrency
  - bigdecimal
  - conservation
  - state-machine
  - metering
spec_ref: "specs/threshold-terminal-derivation-l0.yaml#TTD-CROSS-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/thresholdterminal/ThresholdRegisterService.java + backend/src/main/java/com/ax/template/authblueprint/thresholdterminal/ThresholdRegister.java"
  pattern: "Every accrual and every derived use reads the register row under PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) in the same transaction; the accrual that makes anchor + delta ≥ limit is ACCEPTED (exact overshoot recorded) and drives status to EXPIRED via the sole-mutator state machine within that same transaction; EXPIRED has zero outgoing edges (no service method, endpoint, or transition leaves it); an accrual or use on an EXPIRED register is rejected 409 THRESHOLD_TERMINAL with the anchor unchanged; the entity declares @Check (anchor < \"limit\" OR status = 'EXPIRED') and an immutable positive limit so the implication holds under ddl-auto; no check-the-limit-then-write-in-a-separate-statement appears on any path"
upstream:
  - "https://www.law.cornell.edu/cfr/text/14/43.10"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "US 14 CFR § 43.10 — Disposition of life-limited aircraft parts (the canonical mandatory-replacement-limit + life-status formalization: crossing the limit retires the part)"
    url: "https://www.law.cornell.edu/cfr/text/14/43.10"
    quote: "Life-limited part means any part for which a mandatory replacement limit is specified in the type design, the Instructions for Continued Airworthiness, or the maintenance manual."
    quoted_at: "2026-06-10"
  - source_type: external
    citation: "US 14 CFR § 43.10(c) — a removed life-limited part must be CONTROLLED so it cannot silently return to service (the irreversibility requirement)"
    url: "https://www.law.cornell.edu/cfr/text/14/43.10"
    quote: "Except as provided in paragraph (b) of this section, after April 15, 2002 each person who removes a life-limited part from a type-certificated product must ensure that the part is controlled using one of the methods in this paragraph."
    quoted_at: "2026-06-10"
  - source_type: external
    citation: "PostgreSQL Documentation — 'Explicit Locking' (row-level FOR UPDATE is the single serialization point for the crossing accrual, late accruals, and the derived use)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (the crossing accrual, a late accrual, and a derived use race on one register)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A limit-crossing accrual is an atomic retirement: same-tx terminal, zero outgoing edges, derived use fail-closed

**Impact: HIGH — a live row whose anchor is at/over the limit lets an expired asset keep accruing and keep being USED; a separate-transaction limit check races with the crossing; a reversible terminal erases the retirement evidence.**

A *threshold register* is a cumulative register that carries a **mandatory limit** fixed at registration — an aviation life-limited part's cycle limit, a driver's hours-of-service cap, a dosimeter's exposure limit, a tool's calibration-use count, a warranty's usage cap. 14 CFR § 43.10 gives the canonical shape: a *"mandatory replacement limit"* whose *"life status"* (the accumulated cycles/hours) governs the part — and once removed at limit, the part *"must [be] controlled"* so it cannot silently return to service.

The signature this rule encodes — and the reason the catalog's existing primitives do NOT cover it — is the **derivation**: a VALUE fact (`anchor ≥ limit`) is converted into an irreversible LIFECYCLE fact (`EXPIRED`) **inside the crossing write itself**, and a **second, derived read-path** (install / dispatch / use) is blocked by it:

```text
accrue(delta):  anchor' = anchor + delta            // under the register's row lock
                if anchor' ≥ limit  ⇒  status := EXPIRED   // SAME transaction — one atomic fact
use():          requires status == ACTIVE on the LOCKED row  // fail-closed derivation
invariant (DB): CHECK (anchor < limit OR status = 'EXPIRED') // never a live over-limit row
```

`monotone-register` accrues with **no ceiling** (its modulus *wraps* — the opposite of terminal); `accumulator-consume` floors at zero and rejects per call (no lifecycle conversion); `shared-counter-claim` refuses past a cap but the aggregate **stays live** (retryable, reversible); the FSM rules govern transitions that are **caller-commanded**, never derived from a value crossing.

Three defects recur, and one rule closes them.

**Defect 1 — the crossing and the terminal are two transactions.** Accrue in one transaction, then "notice" the over-limit state in a poller or a follow-up call: between the two commits there is a real window in which the register is over-limit AND live — a use admitted in that window installs an expired part (CWE-362: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently"*).

**Defect 2 — a reversible terminal.** An `unexpire()` / `reset()` / status setter lets the retirement be undone, erasing the recorded life status at retirement — exactly what § 43.10(c)'s control requirement exists to prevent. The terminal state must have **zero outgoing edges**; a replacement asset is a NEW register with its own identity and a zero anchor.

**Defect 3 — the derived use checks a different copy of the state.** A `use()` path that consults a cache, a DTO, or a non-locked read races the crossing accrual: both commit, and an expired asset was dispatched. The derivation must be **fail-closed on the same locked row** both write-paths serialize on.

**Incorrect — separate-tx check, reversible terminal, cached derivation:**

```java
public void accrue(String key, BigDecimal delta) {
    ThresholdRegister r = repo.findByScopeKey(key).orElseThrow();  // ❌ no lock
    r.setAnchor(r.getAnchor().add(delta));                         // ❌ commits over-limit AND live
    repo.save(r);
}
@Scheduled(fixedDelay = 60_000)
public void expireSweep() {                                        // ❌ DEFECT 1: the window is a minute wide
    repo.findOverLimit().forEach(r -> r.setStatus(Status.EXPIRED)); // ❌ DEFECT 2: raw status setter
}
public void use(String key) {
    if (cache.get(key).isActive()) { issueToService(key); }        // ❌ DEFECT 3: races the crossing
}
```

**Correct — one lock, same-tx derivation, zero outgoing edges, fail-closed use:**

```java
@Transactional
public AccrualResult accrue(String key, BigDecimal delta) {
    ThresholdRegister r = repo.findByScopeKeyForUpdate(key)        // ✅ SELECT ... FOR UPDATE
        .orElseThrow(ThresholdException::notFound);
    if (r.getStatus() == Status.EXPIRED)
        throw ThresholdException.terminal();                       // ✅ late accrual → 409, anchor untouched
    BigDecimal next = r.getAnchor().add(delta);                    // exact NUMERIC(19,4)
    r.advanceAnchor(next);                                         // overshoot recorded exactly
    if (next.compareTo(r.getLimit()) >= 0) {
        stateMachine.expire(r);                                    // ✅ DEFECT 1 closed: SAME transaction
    }                                                              //    (sole mutator; no public setter)
    return AccrualResult.of(r);                                    // caller sees EXPIRED immediately
}

@Transactional
public void use(String key) {
    ThresholdRegister r = repo.findByScopeKeyForUpdate(key)        // ✅ DEFECT 3 closed: same locked row
        .orElseThrow(ThresholdException::notFound);
    if (r.getStatus() == Status.EXPIRED)
        throw ThresholdException.terminal();                       // ✅ fail-closed derivation → 409
    issueToService(r);                                             // using is not accruing — anchor untouched
}
// ✅ DEFECT 2 closed: the state machine defines NO transition out of EXPIRED, and the entity
//    backstops the implication under ddl-auto:
//    @Check(constraints = "anchor < \"limit\" OR status = 'EXPIRED'")  +  @Check("\"limit\" > 0")
//    with the limit @Column(updatable = false).
```

`FOR UPDATE` makes the register row the single serialization point for BOTH write-paths (*"This prevents them from being locked, modified or deleted by other transactions until the current transaction ends"* — PostgreSQL, Explicit Locking), so under any interleaving exactly **one** accrual is the crossing, every accrual or use serialized after it sees `EXPIRED` and gets a deterministic 409, and the final anchor equals the sum of the accepted deltas exactly. The `@Check` implication is the DB backstop for the one path code review can miss — an accrual that advances the anchor past the limit but forgets the transition fails at flush instead of committing a live over-limit row.

Verification: review-tier — confirm both `accrue` and `use` lock the register row (`@Lock(PESSIMISTIC_WRITE)`), the crossing accrual drives `EXPIRED` via the sole-mutator state machine in the same transaction, `EXPIRED` has zero outgoing edges (no method or transition leaves it), late accruals and uses return 409 `THRESHOLD_TERMINAL` with the anchor unchanged, and the entity declares the `@Check` implication plus an immutable positive limit. The canonical proof a fork-receiver writes is the concurrency test: N concurrent accruals whose total crosses the limit → exactly one crossing, `anchor == Σ accepted deltas`, every post-crossing accrual/use 409.

Reference: [US 14 CFR § 43.10 — Disposition of life-limited aircraft parts](https://www.law.cornell.edu/cfr/text/14/43.10)

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
