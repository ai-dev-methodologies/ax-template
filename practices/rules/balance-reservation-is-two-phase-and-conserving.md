---
title: A claim against a fungible pooled balance must be TWO-PHASE — an over-reserve-safe RESERVE that places a reversible hold (reserved term, available = funded − committed − reserved), then a SETTLE that commits actual ≤ reserved AND returns the unused remainder in one transaction — never a single-phase commit, never a settle that can exceed its hold
impact: HIGH
impactDescription: "A single-phase debit (commit-on-use) over-spends a prepaid balance under concurrency: two parallel sessions both read available, both authorize, and together draw past the funds (CWE-362). A settle that does not cap actual at the reserved hold, or does not return the unused remainder atomically, silently over-charges the customer or strands their money in a forever-held reservation"
tags:
  - concurrency
  - bigdecimal
  - conservation
  - reservation
  - balance
  - state-machine
spec_ref: "specs/reserve-settle-balance-l0.yaml#RSV-RESERVE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/reservation/ReservableBalanceRepository.java + backend/src/main/java/com/ax/template/authblueprint/reservation/ReservationService.java + backend/src/main/java/com/ax/template/authblueprint/reservation/ReservationSweeper.java"
  pattern: "A reserve reads the balance row under PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) in the same transaction, computes available = funded - committed - reserved, REJECTS (409) a request that exceeds available, and otherwise increases `reserved` and inserts an OUTSTANDING hold; a settle locks the balance, requires actual <= hold.amount (else 422), sets committed += actual and reserved -= hold.amount (so available rises by hold.amount - actual) in ONE transaction and marks the hold SETTLED; a release/expire returns the WHOLE hold (reserved -= amount) and a hold has exactly one terminal transition (OUTSTANDING -> SETTLED|RELEASED|EXPIRED); the timeout sweep re-reads the hold under the balance lock and skips a hold no longer OUTSTANDING (loses the race to a live settle); a @Check (committed >= 0 AND reserved >= 0 AND committed + reserved <= funded) and (settled_amount IS NULL OR settled_amount <= amount) backstop the invariants under ddl-auto"
upstream:
  - "https://www.rfc-editor.org/rfc/rfc4006.txt"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
  - "https://martinfowler.com/eaaCatalog/money.html"
evidence:
  - source_type: external
    citation: "IETF RFC 4006 — Diameter Credit-Control Application, §4 Credit-Control Application Overview (the first/reservation interrogation)"
    url: "https://www.rfc-editor.org/rfc/rfc4006.txt"
    quote: "the credit-control server rates the request, reserves a suitable amount of money from the user's account, and returns the corresponding amount of credit resources."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "IETF RFC 4006 — Diameter Credit-Control Application, §5.4 Final Interrogation (settle: commit used, refund the unused reservation)"
    url: "https://www.rfc-editor.org/rfc/rfc4006.txt"
    quote: "After final interrogation, the credit-control server MUST refund the reserved credit amount not used to the end user's account and deduct the used monetary amount from the end user's account."
    quoted_at: "2026-06-08"
  - source_type: external
    citation: "PostgreSQL Documentation — 'Explicit Locking' (row-level FOR UPDATE serializes concurrent reservers on one balance row)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A claim against a fungible pooled balance must be two-phase: reserve a hold, then settle ≤ the hold and refund the remainder

**Impact: HIGH — a single-phase commit-on-use over-spends a prepaid balance under concurrency; a settle that does not cap actual at its hold over-charges; a settle (or abandoned session) that does not return the unused remainder strands the customer's money forever.**

This rule is the **two-phase, rejecting** counterpart to `accumulator-consume-is-atomic-non-rejecting.md`. An accumulator is single-phase: `consume()` commits immediately and never rejects (it clamps and returns a residual). A **pooled balance with in-flight claims** — a prepaid telecom account funding many concurrent data/voice sessions, a payment authorize-then-capture, a wallet or deposit hold, an EV/cloud pre-authorization — needs a **third balance term**, `reserved`, and a **two-phase lifecycle**:

```text
available = funded − committed − reserved          // the spendable headroom, holds excluded

RESERVE(amount):   require amount ≤ available  (else 409) ; reserved += amount ; hold = OUTSTANDING
SETTLE(hold, act): require act ≤ hold.amount   (else 422) ; committed += act ; reserved −= hold.amount ; hold = SETTLED
                   // available rises by exactly (hold.amount − act): the refunded remainder
RELEASE(hold):     reserved −= hold.amount ; hold = RELEASED          // whole hold returned, committed unchanged
EXPIRE(hold):      reserved −= hold.amount ; hold = EXPIRED           // timeout sweep, same return as RELEASE
// invariant at all times: funded == committed + reserved + available ; reserved == Σ OUTSTANDING holds
```

The reservation pattern is the IETF online-charging model (RFC 4006): on the first interrogation *"the credit-control server rates the request, reserves a suitable amount of money from the user's account, and returns the corresponding amount of credit resources"*; at the final interrogation *"the credit-control server MUST refund the reserved credit amount not used to the end user's account and deduct the used monetary amount from the end user's account."* Reserve, then settle-the-actual-and-refund-the-rest.

Four defects recur, and one rule closes them.

**Defect 1 — single-phase commit-on-use over-spends under concurrency (CWE-362).** Modelling the balance as `(funded, committed)` only and debiting on use is a race: two sessions both read `available = 5`, both authorize a `5`-unit session, both commit — `10` spent against `5` of funds. This is CWE-362 exactly: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."* The reserve must take the balance row under `FOR UPDATE` and net out in-flight holds so concurrent reservers serialize and the sum of granted holds never exceeds `available`.

**Defect 2 — reserve does not reject (wrong posture).** Reserve is the *rejecting* dual of accumulator-consume: a reserve that would push `committed + reserved` past `funded` must be **refused (409)**, not clamped — you cannot hand out credit you do not have. (Accumulator-consume clamps because a deductible draw is always valid; a prepaid reservation is not.)

**Defect 3 — settle does not cap actual at the hold, or commits without refunding the remainder.** The load-bearing overspend guard is `actual ≤ hold.amount` (a `@Check (settled_amount ≤ amount)` backstops it). And settle must move the WHOLE hold out of `reserved` while committing only `actual` — so `available` rises by `hold.amount − actual` in the same transaction. Committing `actual` without removing the full hold from `reserved` permanently strands `hold.amount − actual` of the customer's money.

**Defect 4 — an abandoned hold is never reclaimed, or is double-returned.** A dropped session (lost radio link, crashed gateway) leaves an OUTSTANDING hold forever, silently starving the subscriber of their own funds. A `@Scheduled` timeout sweep must reclaim it — but it is just another concurrent writer and **must lose the race to a live settle**: re-read the hold under the balance lock and skip it if it is no longer OUTSTANDING, so a swept-then-settled hold never double-returns value (`timeout-sweep-is-a-concurrent-mutator`).

**Incorrect — single-phase, races, no hold, no refund:**

```java
@Transactional
public void charge(UUID balanceId, BigDecimal amount) {
    Balance b = repo.findById(balanceId).orElseThrow();      // ❌ plain read, no lock
    if (b.getCommitted().add(amount).compareTo(b.getFunded()) > 0)
        throw new InsufficientFundsException();               // ❌ DEFECT 1: two sessions both pass this
    b.setCommitted(b.getCommitted().add(amount));             // ❌ commit-on-use: no reserve, no actual≤hold,
    repo.save(b);                                             //    no remainder to refund (DEFECT 3/4 absent entirely)
}
```

**Correct — two-phase reserve→settle under a row lock, conserving:**

```java
@Transactional
public Reservation reserve(String scopeKey, BigDecimal amount, Duration ttl) {
    ReservableBalance b = repo.findByScopeKeyForUpdate(scopeKey)      // ✅ SELECT ... FOR UPDATE, same tx
        .orElseThrow(ReservationException::notFound);
    if (amount.compareTo(b.available()) > 0)                          // ✅ DEFECT 2: reject, never clamp
        throw ReservationException.insufficientFunds();
    b.increaseReserved(amount);                                       // ✅ third term; @Check committed+reserved<=funded
    return holds.save(new Reservation(UUID.randomUUID(), b.getId(),
        amount, Instant.now(clock).plus(ttl)));                       // OUTSTANDING, server-clock expiry
}

@Transactional
public Reservation settle(UUID holdId, BigDecimal actual) {
    Reservation h = holds.findById(holdId).orElseThrow(ReservationException::notFound);
    ReservableBalance b = repo.findByIdForUpdate(h.getBalanceId())    // ✅ lock balance FIRST (deterministic order)
        .orElseThrow(ReservationException::notFound);
    h = holds.findByIdForUpdate(holdId).orElseThrow();               // ✅ re-read hold under the balance lock
    if (h.getStatus() != OUTSTANDING) return h;                      // ✅ idempotent terminal (no double-move)
    if (actual.compareTo(h.getAmount()) > 0)                         // ✅ DEFECT 3: actual ≤ hold (CHECK backstop)
        throw ReservationException.overSettle();
    b.advanceCommitted(actual);                                      // committed += actual
    b.decreaseReserved(h.getAmount());                               // reserved -= WHOLE hold → available += amount-actual
    h.settle(actual);                                                // OUTSTANDING → SETTLED, records actual
    return h;
}
```

`FOR UPDATE` serializes concurrent reservers on the one balance row (*"This prevents them from being … modified … by other transactions until the current transaction ends"*), so the sum of granted holds can never exceed `available`. Settle moves the full hold out of `reserved` while committing only `actual`, conserving exactly (the Money-pattern penny discipline: compute one side, derive the rest, exact `BigDecimal` — never two independent roundings). Release/expire is the same shape with `actual = 0`. The sweep re-checks `OUTSTANDING` under the lock so a live settle always wins.

Verification: review-tier — confirm every reserve locks the balance row (`@Lock(PESSIMISTIC_WRITE)`), rejects an over-`available` request, and increments `reserved` (not `committed`); every settle caps `actual ≤ hold.amount`, moves the whole hold out of `reserved`, and commits in one transaction; a hold has exactly one terminal transition; the timeout sweep re-reads under the lock and skips non-`OUTSTANDING` holds; and `@Check (committed + reserved ≤ funded)` + `(settled_amount ≤ amount)` are declared on the entities. The canonical proof a fork-receiver writes is a concurrency test: N reservers racing the last of `available`, asserting Σ granted ≤ funded and, after settling each with `actual < hold`, `committed == Σ actual` and `funded == committed + reserved + available`.

Reference: [IETF RFC 4006 — Diameter Credit-Control Application](https://www.rfc-editor.org/rfc/rfc4006.txt)

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)

Reference: [Martin Fowler — Money pattern](https://martinfowler.com/eaaCatalog/money.html)
