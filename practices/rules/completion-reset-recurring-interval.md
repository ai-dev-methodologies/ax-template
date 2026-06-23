---
title: A recurring obligation whose interval RESETS ON COMPLETION must advance its window FROM the completion instant (not on a fixed calendar grid), carry at most one append-only occurrence per window (exactly-once 409), recompute due/overdue from the clock rather than a stored boolean, let a @Lazy-self @Scheduled sweep record only a non-authoritative overdue flag (never auto-complete), and serialize concurrent completes on the row lock so exactly one advances
impact: HIGH
impactDescription: "Modelling a self-resetting recurring window as a fixed calendar grid silently breaks the schedule the moment a task is done early or late (the next inspection must run from the LAST inspection, not from a frozen grid); a stored due/overdue boolean goes stale the instant the clock passes the window with no write, so the obligation reads 'fine' while actually overdue; without a per-window UNIQUE backstop two racing completes append two occurrences for one window and double-advance; and a sweep that auto-completes (or a @Scheduled tick that self-invokes past the @Transactional proxy) either satisfies an obligation nobody actually performed or silently runs unlocked in production while every test stays green"
tags:
  - state-machine
  - scheduling
  - concurrency
  - audit
  - temporal
spec_ref: "specs/completion-reset-recurring-interval-l0.yaml#CRI-RESET-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/recurringinterval/RecurringObligationService.java + backend/src/main/java/com/ax/template/authblueprint/recurringinterval/RecurringIntervalSweeper.java"
  pattern: "No API accepts a windowStart, nextDueAt, or due/overdue value — the obligation carries an immutable intervalSeconds and a windowStart mutated ONLY by the service under the obligation's PESSIMISTIC_WRITE row lock; completing appends an immutable Occurrence (recording the closed windowStart, completedBy, completedAt) and advances windowStart := completedAt in the SAME locked transaction, so the next window is [completedAt, completedAt + interval) — measured from the completion, not a fixed grid; UNIQUE(obligation_id, window_start) makes a second complete on the same window a deterministic 409; due/overdue is recomputed on read from now vs windowStart + interval (a persisted swept overdueFlag is non-authoritative, operational only); the @Scheduled sweep invokes its per-row worker through an injected @Lazy self-reference, locks the row exactly like the complete path, records the overdue flag, and NEVER completes/advances; completedBy is Authentication.getName() (blank-422 is service-level defensive)"
upstream:
  - "https://www.law.cornell.edu/cfr/text/14/91.409"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "US 14 CFR §91.409(b) — the COMPLETION-RESET interval: the next 100 hours of time in service is computed FROM the last inspection (a self-resetting window), not on a fixed calendar grid"
    url: "https://www.law.cornell.edu/cfr/text/14/91.409"
    quote: "The excess time used to reach a place where the inspection can be done must be included in computing the next 100 hours of time in service."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (two completes, or a complete and the sweep, racing one obligation row)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A completion-reset recurring window slides forward from each completion, recomputes its own due-ness, and is advanced by exactly one writer

**Impact: HIGH — a fixed-grid model of a self-resetting interval, a latched due/overdue boolean, a missing per-window UNIQUE backstop, or a sweep that auto-completes (or self-invokes past its transactional proxy) each defeats the reason the recurring obligation was tracked.**

A *completion-reset recurring obligation* — a usage/condition-based maintenance interval (the next inspection runs from the last), a recurring re-attestation due N days after the last attestation, a periodic re-certification, a recurring safety walk-down — is a deadline that RECURS by **resetting from each completion**. It is the gap `deadline-obligation-l0` does not cover (that governs a ONE-SHOT deadline closed by a single ack) and is fundamentally distinct from a FIXED-CADENCE grid:

```text
FIXED CADENCE:  nextWindowStart = f(previousWindowStart)   — a pure calendar function;
                completing EARLY changes nothing (the grid is frozen)
RESET-ON-DONE:  windowStart := completedAt                 — the window slides forward;
                completing EARLY moves the whole future schedule earlier
```

**1. Reset on completion (CRI-RESET-001).** 14 CFR §91.409(b) is the regulated reality this generalizes — *"The excess time used to reach a place where the inspection can be done must be included in computing the next 100 hours of time in service."* The NEXT interval is computed FROM the last inspection, not on a fixed grid. Completing appends an immutable `Occurrence` (the closed window's start, who, when) and advances `windowStart := completedAt` in one locked transaction.

**2. Exactly-once per window (CRI-ONCE-001).** Each window carries at most one occurrence; `UNIQUE(obligation_id, window_start)` makes a racing second complete on the same window a deterministic 409. After a completion the obligation is OPEN again on the next window — it is never "done".

**3. Due/overdue is recomputed, never latched (CRI-DUE-001).** `overdue iff now >= windowStart + interval AND the current window has no occurrence` is computed on every read from the clock and `windowStart`, so it flips from time passing ALONE — no write. A persisted swept flag is non-authoritative, operational only. A caller cannot supply a `due`/`nextDueAt`.

**4. The sweep only flags, never completes (CRI-SWEEP-001).** A `@Scheduled` sweep records the non-authoritative `overdueFlag` for visibility and NEVER advances or completes — only a real completion slides the window. The `@Scheduled` tick calls its per-row worker through an injected `@Lazy` self-reference: a bare `this.sweepOne(...)` would BYPASS the `@Transactional` proxy (self-invocation), dropping `REQUIRES_NEW` and the row lock in production while every test stayed green.

**Incorrect — fixed-grid recurrence, stored due flag, no per-window backstop, auto-completing self-invoking sweep:**

```java
public Obligation complete(String key) {
    Obligation o = repo.findByKey(key);                       // ❌ no row lock (CWE-362)
    o.setNextDueAt(o.getNextDueAt().plus(o.getInterval()));   // ❌ fixed grid from PREVIOUS due,
    o.setOverdue(false);                                      //    not from the completion instant;
    return repo.save(o);                                      // ❌ latched boolean goes stale on its own
}                                                             // ❌ no Occurrence, no per-window UNIQUE → races double-advance

@Scheduled(fixedDelay = 60_000)
public void sweep() {
    for (Obligation o : repo.findOverdue(now())) {
        this.complete(o.getKey());                            // ❌ self-invocation bypasses @Transactional;
    }                                                         // ❌ the sweep auto-completes work nobody did
}
```

**Correct — window resets from completion, append-only occurrence, recomputed due-ness, flag-only @Lazy-self sweep:**

```java
@Transactional
public RecurringObligation complete(String obligationKey, String completedBy) {
    requireNonBlank(completedBy);                                  // 422 (defensive; API derives it)
    RecurringObligation o = obligations.findByObligationKeyForUpdate(obligationKey) // ✅ row lock (CWE-362)
        .orElseThrow(RecurringIntervalException::notFound);
    Instant now = Instant.now(clock);
    // ✅ at most one completion per window OCCUPANCY: completing again before the just-opened
    //    window is due again is a duplicate of the current cycle → 409. Under the lock this
    //    serializes N concurrent completes so exactly ONE advances and the rest 409.
    if (o.getLastCompletedAt() != null && now.isBefore(o.nextDueAt())) {
        throw RecurringIntervalException.windowAlreadyCompleted(); // 409 — already completed this window
    }
    try {
        members.persistAndFlush(new Occurrence(UUID.randomUUID(), o.getId(),
            o.getWindowStart(), completedBy, now));                // ✅ append-only; uq(obligation_id, closed_window_start)
    } catch (DataIntegrityViolationException dup) {
        throw RecurringIntervalException.windowAlreadyCompleted(); // ✅ DB backstop if the lock slipped
    }
    o.completeAndAdvance(now);                                     // ✅ windowStart := completedAt (reset, not grid)
    return o;                                                      // next window = [now, now + interval)
}

public boolean isOverdue(RecurringObligation o, Instant now) {     // ✅ recomputed, never a stored verdict
    return !now.isBefore(o.getWindowStart().plusSeconds(o.getIntervalSeconds()));
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public boolean sweepOne(UUID obligationId) {                       // the @Scheduled tick calls this
    RecurringObligation o = obligations.findByIdForUpdate(obligationId) //  THROUGH the injected @Lazy proxy
        .orElseThrow(RecurringIntervalException::notFound);        //  self-reference (a bare this.sweepOne
    boolean overdue = sweepClockOverdue(o);                        //  would bypass the @Transactional proxy)
    o.recordSweptOverdue(overdue);                                 // ✅ NON-authoritative flag only
    return overdue;                                                // ✅ NEVER completes / advances
}
```

The sweep is a CONCURRENT MUTATOR in the sense of `timeout-sweep-is-a-concurrent-mutator` (that rule names the optimistic `@Version` lose-the-race shape; this sweeper satisfies the same requirement by the STRONGER means — the same `PESSIMISTIC_WRITE` row lock as the complete path), so two completes serialize (exactly one advances, the other 409) and a complete and the sweep never race. `Occurrence` rows are `@AggregateMember` of the `RecurringObligation` root — root-JPQL reads, `common/MemberWriter` writes (the AX-DDD-MEMBER-REPO end-state, applied from birth).

Verification: review-tier — confirm no API accepts a `windowStart`/`nextDueAt`/`due` value; completing appends an immutable `Occurrence` and advances `windowStart := completedAt` under the row lock; the next window is measured from the completion (early completion slides the schedule forward); `UNIQUE(obligation_id, window_start)` backstops exactly-once (409); due/overdue is recomputed on read; the sweep records only the non-authoritative flag, locks the row, injects `@Lazy` self, and never completes. The behavioural proofs a fork-receiver keeps green: the concurrency test (N racing completes → one occurrence, one advance, N-1 → 409) and the time-only-overdue test (overdue flips with no write).

Reference: [US 14 CFR §91.409 — Inspections](https://www.law.cornell.edu/cfr/text/14/91.409)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
