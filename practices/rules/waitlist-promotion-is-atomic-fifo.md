---
title: Waitlist promotion MUST be one atomic FIFO transaction — never read-then-promote
impact: HIGH
impactDescription: "Releasing a seat and promoting the next waiter in two statements re-opens the read-then-act race (CWE-362) one layer above the seat claim — two concurrent releases promote the same entry twice or skip a waiter, and wall-clock ordering silently breaks FIFO fairness"
tags:
  - waitlist
  - concurrency
  - fifo
  - row-lock
  - state-machine
spec_ref: "specs/waitlist-promotion-l0.yaml#WAITLIST-PROMOTE-001"
verification:
  type: review
  source: "specs/waitlist-promotion-l0.yaml (WAITLIST-PROMOTE-001 — release+promote single transaction, FOR UPDATE on freed capacity row, ordered by monotonic enqueue position)"
  pattern: "Release-a-seat and promote-the-head-of-line execute in ONE @Transactional method that (1) SELECT ... FOR UPDATE locks the freed capacity row, (2) selects the longest-waiting entry by a monotonic enqueue ordinal (NOT created_at wall-clock), (3) flips it waiting->enrolled through the sole-mutator state machine. Reject any code path that reads the head outside the lock, promotes by timestamp, or splits release and promote across two transactions."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc970"
  - "https://www.postgresql.org/docs/current/explicit-locking.html"
evidence:
  - source_type: external
    citation: "RFC 970 — On Packet Switches With Infinite Storage (J. Nagle, FACC Palo Alto, December 1985)"
    url: "https://www.rfc-editor.org/rfc/rfc970"
    quote: "Initially, we will assume that queues are managed in a first in, first out manner."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "PostgreSQL Documentation — 13.3. Explicit Locking (Row-Level Locks, FOR UPDATE)"
    url: "https://www.postgresql.org/docs/current/explicit-locking.html"
    quote: "FOR UPDATE causes the rows retrieved by the SELECT statement to be locked as though for update. This prevents them from being locked, modified or deleted by other transactions until the current transaction ends."
    quoted_at: "2026-06-01"
---

## Waitlist promotion MUST be one atomic FIFO transaction — never read-then-promote

**Impact: HIGH — the promote step re-opens the exact race the seat-claim primitive closed, one layer up**

A capacity-gated waitlist has two atomic obligations, not one. The first — claiming the last open seat without overselling — is solved by `bounded-capacity-claim-l0` (`CLAIM-ATOMIC-001`): a single conditional `UPDATE` whose affected-row count decides winner vs. loser. But the moment a held seat is *released* and the next waiter must be *promoted* into it, a second race appears. If release-the-seat and promote-the-head-of-line are two separate statements — read the longest-waiting entry, then flip it `enrolled` — then two concurrent releases each read the *same* head-of-line waiter before either commits, and both promote that one entry: a double-promote that grants two seats to one person while a genuine waiter is silently skipped. This is `CWE-362` (a timing window in which a shared resource is mutated by a concurrent sequence), re-opened one layer above the seat claim.

The correct shape is a single `@Transactional` critical section that (1) takes a `SELECT ... FOR UPDATE` row lock on the *freed capacity row* so concurrent releases serialize, (2) selects the head-of-line waiter ordered by a **monotonic enqueue ordinal** — an auto-increment sequence assigned at enqueue, *never* `created_at` wall-clock — and (3) flips it `waiting → enrolled` through the sole-mutator state machine whose `@Version` column rejects a concurrent promote of the same entry. Release and promote net zero capacity change (the freed seat is immediately re-taken by the promotee), which is precisely *why* they must commit together: any window in which the seat is free-but-unassigned is a window in which a fresh claimant can steal it ahead of the queue, breaking FIFO fairness.

Wall-clock ordering deserves its own warning. `ORDER BY created_at` looks like FIFO but is not: equal millisecond timestamps under load are non-deterministic, clock skew across app nodes reorders entries, and a backward clock step (NTP correction, DST) can promote a later arrival first. FIFO — *first in, first out* — is defined over enqueue *position*, a strictly increasing ordinal, not over a timestamp that the database does not guarantee to be unique or monotonic.

**Incorrect — release and promote split; head read outside the lock; wall-clock ordering:**

```java
@Transactional
public void releaseSeat(Long entryId) {
    WaitlistEntry held = repo.findById(entryId).orElseThrow();
    fsm.transition(held, DROPPED);
    capacityRepo.decrementTaken(held.getResourceId());   // seat is now free-but-unassigned
}

// ...later, a different transaction / scheduler tick:
@Transactional
public void promoteNext(Long resourceId) {
    // ❌ no FOR UPDATE — two concurrent promotes read the same head
    WaitlistEntry head = repo
        .findFirstByResourceIdAndStatusOrderByCreatedAtAsc(resourceId, WAITING)  // ❌ wall-clock FIFO
        .orElse(null);
    if (head == null) return;
    fsm.transition(head, ENROLLED);          // ❌ double-promote under concurrency; window before promote lets a fresh claimant steal the seat
    capacityRepo.incrementTaken(resourceId);
}
```

**Correct — one transaction; FOR UPDATE on the freed capacity row; ordered by monotonic enqueue ordinal; sole-mutator @Version transition:**

```java
@Transactional
public void releaseAndPromote(Long enrolledEntryId) {
    WaitlistEntry releasing = repo.findById(enrolledEntryId).orElseThrow();

    // (1) serialize concurrent releases on the SAME freed capacity row
    Capacity cap = capacityRepo.lockForUpdate(releasing.getResourceId());  // SELECT ... FOR UPDATE

    fsm.transition(releasing, DROPPED);      // sole mutator; @Version guards the entry

    // (2) head-of-line by MONOTONIC enqueue ordinal — never created_at
    WaitlistEntry head = repo
        .findFirstByResourceIdAndStatusOrderByEnqueueOrdinalAsc(cap.getResourceId(), WAITING)
        .orElse(null);

    if (head != null) {
        fsm.transition(head, ENROLLED);      // (3) freed seat re-taken atomically — taken nets unchanged
        // promotion notification rides the SAME tx via the transactional outbox
    } else {
        cap.decrementTaken();                // empty queue → seat returns to free capacity; CHECK(taken>=0) backstops
    }
}
// release + promote commit together: no free-but-unassigned window, exactly-one promote per release,
// strict FIFO by enqueue ordinal, double-promote impossible (row lock + @Version).
```

This composition is what makes the waitlist fair *and* safe: `bounded-capacity-claim-l0#CLAIM-ATOMIC-001` wins the seat without oversell, this rule promotes the next waiter without a double-grant or a stolen seat, and `transactional-outbox-l0#OUTBOX-WRITE-001` carries the promotion notice in the same transaction so the promoted user is reliably told. Drop any one and the chain leaks: ad-hoc promote re-opens `CWE-362`, direct broker publish loses the notification on a crash, wall-clock ordering breaks FIFO.

Verification (review-tier): inspect the promote path and reject it unless release-and-promote share one `@Transactional` method, the freed capacity row is taken `FOR UPDATE`, the head-of-line query orders by a monotonic enqueue ordinal (not `created_at`), and the `waiting → enrolled` flip goes through the sole-mutator state machine. A concurrent-release harness (N parallel releases against a queue of M ≥ N waiters) MUST promote exactly N *distinct* FIFO heads — never the same entry twice, never zero for a non-empty queue.

Reference: [RFC 970 — On Packet Switches With Infinite Storage (J. Nagle)](https://www.rfc-editor.org/rfc/rfc970)

Reference: [PostgreSQL Documentation — 13.3. Explicit Locking (FOR UPDATE)](https://www.postgresql.org/docs/current/explicit-locking.html)
