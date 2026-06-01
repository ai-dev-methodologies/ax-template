---
title: A claim against a bounded shared counter MUST be a single atomic statement — never read-then-insert
impact: CRITICAL
impactDescription: "Two claimants each reading 'taken < capacity == true' and both inserting oversells the resource; idempotency-key dedup does NOT close this cross-claimant race — only an atomic conditional UPDATE or pessimistic row lock does"
tags:
  - concurrency
  - capacity
  - race-condition
  - cwe-362
  - atomic-claim
  - no-oversell
spec_ref: "specs/bounded-capacity-claim-l0.yaml#CLAIM-ATOMIC-001"
verification:
  type: review
  source: "specs/bounded-capacity-claim-l0.yaml#CLAIM-ATOMIC-001"
  pattern: "A claim handler against a bounded counter MUST resolve room-and-increment in ONE statement: either (a) conditional `UPDATE capacity SET taken = taken + 1 WHERE resource_id = ? AND taken < capacity` with an affected-rows==1 check, or (b) `SELECT ... FOR UPDATE` on the single capacity row inside the same @Transactional method before the bounded write. Reject any handler that reads the count (COUNT(*) or a separate SELECT) in one statement and inserts/updates in another — that read-then-insert window is the oversell race. A DB `CHECK (taken <= capacity)` backstop MUST exist. This is a runtime concurrency property with no compile-time signal, so it is verified by review against the spec, not by a static @Tag test."
upstream:
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
  - "https://www.postgresql.org/docs/current/transaction-iso.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — 13.3 Explicit Locking (Row-Level Locks, FOR UPDATE)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 13.2 Transaction Isolation (Read Committed Isolation Level)"
    url: "https://www.postgresql.org/docs/current/transaction-iso.html"
    quote: "The search condition of the command (the WHERE clause) is re-evaluated to see if the updated version of the row still matches the search condition. If so, the second updater proceeds with its operation using the updated version of the row."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition')"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
decided_at: "2026-06-01"
---

## A claim against a bounded shared counter MUST be a single atomic statement — never read-then-insert

**Impact: CRITICAL — Two claimants racing the last unit of a bounded resource (the last seat, the last enrollment slot, the last ticket) will BOTH succeed if the claim is coded as read-the-count-then-insert-if-room. Idempotency-Key protection does not help: idempotency dedupes a RETRY of the SAME caller's SAME key, but two DIFFERENT claimants supply two DIFFERENT keys and both pass the stale precondition. The result is an oversell — a double-booked room, an over-enrolled course, a ticket sold twice — that no amount of retry-dedup catches.**

The failure is CWE-362, a race condition: a code sequence needs temporary exclusive access to a shared resource (the capacity counter), but a timing window exists where a concurrent claimant modifies it. Under PostgreSQL Read Committed (the default), a row committed by transaction B *after* transaction A took its snapshot for the `SELECT count` is invisible to A's earlier read — so A and B both observe `taken < capacity == true` and both insert. The counter ends at `capacity + 1`.

There are exactly two correct primitives. Both collapse the check-and-increment into ONE atomic step so the database — not application code — serializes the race.

**Primitive (a): conditional single-statement UPDATE.** Put the precondition *inside* the write. `UPDATE ... SET taken = taken + 1 WHERE id = ? AND taken < capacity` returns the affected-row count: `1` means you won the unit, `0` means there was no room. This is race-safe because Read Committed re-evaluates the `WHERE` clause against the freshly-committed row version — the loser's `taken < capacity` is re-checked against the bumped count and fails, yielding `0` rows.

**Primitive (b): pessimistic `SELECT ... FOR UPDATE`.** Lock the single capacity row at the top of the transaction; the row lock blocks the second claimant until the first commits, so the second reads the already-incremented value.

A `CHECK (taken <= capacity)` on the capacity row is the mandatory backstop: even a future code path that forgets the primitive cannot persist an oversell.

**Incorrect — read-then-insert: two concurrent claimants both pass the check and oversell (CWE-362):**

```java
@Transactional
public Seat claimSeat(long showId, long userId) {
    // VIOLATION: read in one statement ...
    long taken = seatRepo.countByShowId(showId);
    Capacity cap = capacityRepo.findByShowId(showId);
    if (taken >= cap.getCapacity()) {
        throw new CapacityExhaustedException(showId);
    }
    // ... act in another. Between the read and this insert a concurrent
    // transaction can commit its own claim — both see room, both insert.
    return seatRepo.save(new Seat(showId, userId)); // oversell: capacity+1
}
```

**Correct — conditional single-statement UPDATE; affected-rows decides grant vs reject; one atomic step, no window:**

```java
public interface CapacityRepository extends JpaRepository<Capacity, Long> {
    // Precondition lives INSIDE the write. Read Committed re-checks the
    // WHERE against the freshly-committed row, so the loser gets 0 rows.
    @Modifying
    @Query("UPDATE Capacity c SET c.taken = c.taken + 1 "
         + "WHERE c.resourceId = :rid AND c.taken < c.capacity")
    int tryClaim(@Param("rid") long resourceId);
}

@Transactional
public Seat claimSeat(long showId, long userId) {
    int granted = capacityRepository.tryClaim(showId); // 1 = won, 0 = no room
    if (granted == 0) {
        claimMetrics.exhausted("seat");                 // CLAIM-OBSERVABILITY-001
        throw new CapacityExhaustedException(showId);   // → 409 CAPACITY_EXHAUSTED
    }
    claimMetrics.granted("seat");
    return seatRepo.save(new Seat(showId, userId));
}
// DDL backstop (independent of code): the capacity row carries
//   CHECK (taken <= capacity) AND CHECK (taken >= 0)
// so even a path that bypasses tryClaim() cannot persist an oversell.
```

The pessimistic alternative is equally correct: a derived-query `findByResourceIdForUpdate` annotated `@Lock(LockModeType.PESSIMISTIC_WRITE)` that locks the single capacity row before the bounded UPDATE inside the same transaction. Choose conditional-UPDATE for the lowest-contention path (no lock wait on the happy path) and FOR UPDATE when the claim must read additional locked state in the same transaction.

**This is orthogonal to idempotency — ship both.** `idempotency-key-on-mutations.md` makes a single caller's *retry* return the cached grant (no double-grant to ONE caller). This rule makes two *distinct* callers racing the last unit resolve to exactly one grant and one reject (no oversell ACROSS callers). A claim POST that ships only idempotency is still oversell-vulnerable; one that ships only atomic-claim double-charges a retried winner. The catalog requires both on a side-effecting bounded-resource claim.

Verification: review-tier. A claim is a runtime concurrency property — a single-threaded test passes even on the broken read-then-insert, and no compile-time signal exists. Verify by review against `specs/bounded-capacity-claim-l0.yaml#CLAIM-ATOMIC-001`, and prove it with the negative concurrency test mandated by `#CLAIM-OVERSELL-001` (fire N+1 concurrent claims at capacity N; assert exactly N grants + 1 reject + final `taken == N`). When a fork-receiver wires a real `@Tag("CLAIM-OVERSELL-001")` concurrency IT, this rule's verification block may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — Explicit Locking (FOR UPDATE row-level locks)](https://www.postgresql.org/docs/current/explicit-locking.html)

Reference: [PostgreSQL — Transaction Isolation (Read Committed WHERE re-evaluation)](https://www.postgresql.org/docs/current/transaction-iso.html)

Reference: [CWE-362 — Race Condition (Concurrent Execution using Shared Resource with Improper Synchronization)](https://cwe.mitre.org/data/definitions/362.html)
