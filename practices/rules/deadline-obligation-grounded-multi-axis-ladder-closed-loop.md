---
title: A governed deadline obligation must derive its deadline from a recorded anchor+rule (never a free-typed date), take the EARLIEST candidate when multiple axes govern it, fire ordered escalation rungs exactly once as appended additive events, and reach its terminal ONLY through an explicit who/when acknowledgment — the sweep never auto-expires it
impact: HIGH
impactDescription: "A free-typed deadline cannot be audited or re-derived once the inputs move on; tracking only one axis of a multi-axis obligation silently misses the axis that exhausts first (an aircraft annual that ignores flight hours); a re-firing escalation rung spams or — worse — a never-firing one hides the breach; and a sweep that auto-expires an unacknowledged critical obligation closes the loop NOBODY actually closed, which is the exact failure mode closed-loop communication exists to prevent"
tags:
  - state-machine
  - scheduling
  - audit
  - concurrency
  - escalation
spec_ref: "specs/deadline-obligation-l0.yaml#OBL-GROUND-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/obligation/ObligationService.java + backend/src/main/java/com/ax/template/authblueprint/obligation/ObligationSweeper.java"
  pattern: "No API accepts a raw deadline — every axis row records its candidate AND a recomputable derivation string, and the effective deadline is always min(candidates) re-evaluated under the obligation's PESSIMISTIC_WRITE row lock; a usage advance recomputes its axis candidate + appends the new derivation in the same locked transaction; the sweep locks the row exactly like the API paths (timeout-sweep-is-a-concurrent-mutator), fires each ladder rung at most once (UNIQUE(obligation_id, rung) backstop) in order as APPENDED immutable escalation events, skips ACKNOWLEDGED rows, and NEVER writes the terminal state; the only OPEN→ACKNOWLEDGED writer is the explicit ack path recording ack_by/ack_at (double-ack 409; the acknowledger comes from Authentication — the blank-422 guard is service-level defensive)"
upstream:
  - "https://www.law.cornell.edu/cfr/text/14/91.409"
  - "https://pmc.ncbi.nlm.nih.gov/articles/PMC7510293/"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "US 14 CFR §91.409(a) — annual inspection: the CALENDAR axis of the canonical multi-axis airworthiness obligation"
    url: "https://www.law.cornell.edu/cfr/text/14/91.409"
    quote: "no person may operate an aircraft unless, within the preceding 12 calendar months, it has had—(1) An annual inspection in accordance with part 43"
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "US 14 CFR §91.409(b) — 100-hour inspection: the USAGE axis governing the SAME obligation (whichever axis exhausts first governs)"
    url: "https://www.law.cornell.edu/cfr/text/14/91.409"
    quote: "unless within the preceding 100 hours of time in service the aircraft has received an annual or 100-hour inspection and been approved for return to service"
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "Closing the loop on test results to reduce communication failures (BMJ Open Quality, PMC7510293) — peer-reviewed closed-loop anchor: a result is done only when acknowledged, not when sent"
    url: "https://pmc.ncbi.nlm.nih.gov/articles/PMC7510293/"
    quote: "The review highlighted the complex challenge of ensuring that test results are sent, received, acknowledged and acted upon."
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (sweep vs ack vs usage-update racing one obligation row)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A deadline obligation is grounded, multi-axis, laddered, and closed only by a human

**Impact: HIGH — an unauditable free-typed date, a missed faster axis, a double-firing or silent rung, or a sweep that auto-closes an unacknowledged critical obligation each defeats the reason the obligation was tracked at all.**

A *governed deadline obligation* — an aircraft inspection due, a critical lab result awaiting follow-up, a regulatory filing window, a contract cure period, a certificate expiry — is a deadline someone MUST act on. Four properties make it governable, and the catalog's existing primitives cover none of them (`scheduled-task` runs jobs with no obligation lifecycle; webhook retry/backoff auto-terminates on exhaustion — the opposite posture; `threshold-terminal` derives an irreversible terminal from one value with no ladder and no acknowledgment):

```text
GROUNDED:   every axis row records candidate + derivation ("CALENDAR: anchor + P365D";
            "USAGE: now + (limit − used) / rate") — no free-typed deadline exists
MULTI-AXIS: effective = min(candidates); a usage advance re-derives its axis, re-records,
            re-evaluates — under the obligation row lock
LADDER:     APPROACH(50%) → IMMINENT(80%) → BREACH(100%), each fired EXACTLY ONCE,
            in order, as appended additive events — UNIQUE(obligation_id, rung)
CLOSED-LOOP: OPEN → ACKNOWLEDGED(ack_by, ack_at) is the ONLY terminal edge;
            the sweep skips ACKNOWLEDGED rows and NEVER writes the terminal
```

**1. Grounded derivation (OBL-GROUND-001).** The deadline's provenance is part of the record: the caller supplies axes (anchor + interval, or limit + used + declared rate) and the SYSTEM computes each candidate and stores the formula it used. An auditor re-derives every deadline from the row alone — the temporal analog of decision-governance's basis snapshot.

**2. The earliest axis governs (OBL-AXIS-001).** 14 CFR §91.409 is the regulated reality this generalizes — one airworthiness obligation governed by a calendar axis, *"within the preceding 12 calendar months"*, AND a usage axis, *"within the preceding 100 hours of time in service"*. Track only one and the other exhausts silently. Every usage advance recomputes that axis's candidate from the new remaining budget, appends the new derivation, and re-evaluates `min(candidates)` in the same locked transaction.

**3. Exactly-once additive ladder (OBL-LADDER-001).** Each rung fires once — recorded as an APPENDED immutable escalation event, backstopped by `UNIQUE(obligation_id, rung)` so a racing second sweep cannot double-fire. A pass that finds several rungs due fires the missing ones in order. Escalation only ADDS visibility; no rung mutates the lifecycle or the deadline.

**4. Only a human closes the loop (OBL-ACK-001).** The clinical literature states the bar: the challenge is *"ensuring that test results are sent, received, acknowledged and acted upon"* — sent is not done. The ONLY terminal edge is an explicit acknowledgment recording who and when. Past the deadline the obligation stays OPEN with BREACH fired, escalating until someone acknowledges. A sweep that auto-expires the obligation closes a loop nobody closed.

**Incorrect — free-typed deadline, single axis, re-firing rung, auto-expiring sweep:**

```java
public Obligation create(String key, Instant deadline) {     // ❌ free-typed — underivable
    return repo.save(new Obligation(key, deadline));         // ❌ one axis only
}
@Scheduled(fixedDelay = 60_000)
public void sweep() {
    for (Obligation o : repo.findByDeadlineBefore(now())) {  // ❌ no lock (CWE-362)
        notifyOps(o);                                        // ❌ re-fires EVERY pass
        o.setStatus(EXPIRED);                                // ❌ auto-expires — nobody acknowledged
        repo.save(o);
    }
}
```

**Correct — grounded axes, min-of-candidates, exactly-once rungs, ack-only terminal:**

```java
@Transactional
public Obligation advanceUsage(String key, BigDecimal units) {
    Obligation o = obligations.findByObligationKeyForUpdate(key) // ✅ row lock (CWE-362)
        .orElseThrow(ObligationException::notFound);
    ObligationAxis axis = obligations.findAxis(o.getId(), AxisKind.USAGE)
        .orElseThrow(ObligationException::notFound);
    Instant now = Instant.now(clock);
    Instant candidate = axis.advanceUsage(units, now);        // recompute from remaining/rate
    members.persist(new DerivationRecord(UUID.randomUUID(), o.getId(), axis.getId(),
        candidate, axis.derivationFormula(now), now));        // ✅ derivation re-recorded
    o.reevaluate(earliestCandidate(o.getId()));               // ✅ min(candidates)
    return o;
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public int processOne(UUID id) {                              // the @Scheduled tick calls this
    Obligation o = obligations.findByIdForUpdate(id)          //   THROUGH the injected proxy
        .orElseThrow(ObligationException::notFound);          //   self-reference (a bare
    if (o.getStatus() == ObligationStatus.ACKNOWLEDGED) {     //   this.processOne would bypass
        return 0;                                             //   the @Transactional proxy)
    }                                                         // ✅ skips closed loops
    Instant now = Instant.now(clock);
    int fired = 0;
    for (EscalationRung rung : EscalationRung.LADDER) {       // APPROACH → IMMINENT → BREACH
        boolean due = !now.isBefore(rung.dueAt(o.getWindowStart(), o.getEffectiveDeadline()));
        if (due && !obligations.rungFired(o.getId(), rung)) {
            members.persist(new EscalationEvent(UUID.randomUUID(), o.getId(), rung, now,
                o.getEffectiveDeadline()));                   // ✅ appended, additive,
            fired++;                                          //    UNIQUE(obligation_id, rung)
        }
    }
    return fired;                                             // ✅ NEVER writes the terminal
}

@Transactional
public Obligation acknowledge(String key, String acknowledger) {
    requireNonBlank(acknowledger);                            // 422
    Obligation o = repo.findByObligationKeyForUpdate(key)
        .orElseThrow(ObligationException::notFound);
    if (o.getStatus() == Status.ACKNOWLEDGED) {
        throw ObligationException.alreadyAcknowledged();      // 409 — the loop closes once
    }
    o.acknowledge(acknowledger, Instant.now(clock));          // ✅ the ONLY terminal writer
    return o;
}
```

The sweep is a CONCURRENT MUTATOR in the sense of `timeout-sweep-is-a-concurrent-mutator` (that rule's pattern names the optimistic @Version lose-the-race shape; this sweeper satisfies the same requirement by the STRONGER means — the same `PESSIMISTIC_WRITE` row lock as the API paths), so a rung fires exactly once across racing passes, an ack and a sweep serialize, and a usage advance and the sweep agree on one effective deadline per evaluation. Axes and escalation events are `@AggregateMember` rows — root-JPQL reads, `common/MemberWriter` writes (the AX-DDD-MEMBER-REPO end-state, applied from birth).

Verification: review-tier — confirm no API accepts a raw deadline; every axis row carries candidate + derivation and every recompute appends a new derivation; the effective deadline is `min(candidates)` re-evaluated under the lock; the sweep locks the row, fires rungs at most once each (UNIQUE backstop), in order, and never writes ACKNOWLEDGED; the ack path is the sole terminal writer with who/when recorded, 409 on double-ack. The behavioural proofs a fork-receiver keeps green: the concurrency test (N racing sweeps → one event per due rung) and the past-deadline test (obligation stays OPEN with BREACH fired).

Reference: [US 14 CFR §91.409 — Inspections](https://www.law.cornell.edu/cfr/text/14/91.409)

Reference: [Closing the loop on test results to reduce communication failures (PMC7510293)](https://pmc.ncbi.nlm.nih.gov/articles/PMC7510293/)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
