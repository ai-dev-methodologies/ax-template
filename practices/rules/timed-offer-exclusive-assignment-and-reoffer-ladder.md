---
title: A timed-assignment workflow must extend an offer to a candidate with a DEADLINE (OPEN until accept/decline/deadline; a @Scheduled sweep expires past-deadline OPEN offers EXACTLY ONCE, recorded SYSTEM/when), hold EXCLUSIVITY so at most ONE offer per subject is accepted (the loser of a competing accept gets 409 via a uq(subject_id) backstop under the subject row lock), and re-offer a declined/expired offer to the next candidate as a NEW row in an ordered append-only ladder
impact: HIGH
impactDescription: "A timed-offer workflow with no exclusivity backstop double-assigns one subject when two candidates accept competing offers at the same instant (CWE-362) — two drivers dispatched to one ride, two bidders winning one lot, two clinicians paged for one bed; a sweep that self-invokes its own @Transactional method silently runs WITHOUT the row lock and REQUIRES_NEW on the production tick (green in every test, broken only in prod); and a re-offer that MUTATES the prior offer row instead of appending loses the auditable record of who was offered the subject and in what order"
tags:
  - state-machine
  - concurrency
  - audit
  - governance
spec_ref: "specs/timed-offer-exclusive-assignment-l0.yaml#TIMEDOFFER-EXCLUSIVE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/timedoffer/TimedOfferService.java + backend/src/main/java/com/ax/template/authblueprint/timedoffer/TimedOfferSweeper.java + backend/src/main/java/com/ax/template/authblueprint/timedoffer/Assignment.java + backend/src/main/java/com/ax/template/authblueprint/timedoffer/TimedOffer.java"
  pattern: "A TimedOffer is extended to a candidate for a subject with a recorded deadline and stays OPEN until accept/decline/deadline; the accept path locks EVERY offer row for the subject (PESSIMISTIC_WRITE) and creates an Assignment row whose uq(subject_id) makes a competing accept a deterministic 409, so exactly one offer per subject is ACCEPTED (CWE-362 lock + unique-index suspenders); the @Scheduled deadline sweep expires past-deadline OPEN offers EXACTLY ONCE recording SYSTEM + when, reaching its REQUIRES_NEW per-row handler through an @Lazy self proxy (never a bare self-invocation) and LOSING cleanly to a live accept; a declined/expired offer is re-offered as a NEW append-only TimedOffer row referencing the prior with a strictly monotonic attemptSeq; NO delete path exists"
upstream:
  - "https://cwe.mitre.org/data/definitions/362.html"
  - "https://www.rfc-editor.org/rfc/rfc9110.html"
  - "https://www.law.cornell.edu/uscode/text/15/1692g"
evidence:
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (the concurrent-accept race for one subject across competing timed offers)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "RFC 9110 (HTTP Semantics) §15.5.10 — the 409 Conflict the loser of a competing accept receives because the subject's current state (already assigned) conflicts with the request"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html"
    quote: "The 409 (Conflict) status code indicates that the request could not be completed due to a conflict with the current state of the target resource."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "Fair Debt Collection Practices Act, 15 U.S.C. § 1692g(a) (Cornell LII) — the timed offer-then-escalate discipline the deadline + re-offer ladder generalizes: a statutory five-day clock and a thirty-day window gate escalation"
    url: "https://www.law.cornell.edu/uscode/text/15/1692g"
    quote: "Within five days after the initial communication with a consumer in connection with the collection of any debt, a debt collector shall, unless the following information is contained in the initial communication or the consumer has paid the debt, send the consumer a written notice containing— (1) the amount of the debt;"
    quoted_at: "2026-06-23"
---

## A timed assignment is an exclusive offer with a deadline and an append-only re-offer ladder — not a mutable status flag

**Impact: HIGH — no exclusivity backstop double-assigns one subject under a concurrent accept (CWE-362); a self-invoked @Transactional sweep runs without its lock on the production tick; a mutated re-offer destroys the attempt audit trail.**

A *timed offer* is the shape every timed-assignment workflow runs: extend an offer to a candidate for a *subject* (the thing being assigned) with a *deadline*, keep it OPEN until the candidate accepts/declines or the deadline passes, and on decline/timeout re-offer to the next candidate. The IDW9 dispatch dogfood realized this — but welded it to the Provider/ServiceRequest/Offer reference workload. Lifted out, the primitive is:

```text
extend(subject, candidate, deadline): a NEW OPEN TimedOffer; attemptSeq monotonic for the subject
accept(offer, candidate):  lock EVERY offer row for the subject (PESSIMISTIC_WRITE); re-check OPEN +
                           deadline; create an Assignment whose uq(subject_id) is the exactly-one
                           backstop → the competing accept is a deterministic 409 (CWE-362)
decline(offer):            OPEN → DECLINED through the state machine (records who/when)
sweep:                     a @Scheduled poller; expires past-deadline OPEN offers EXACTLY ONCE
                           (records SYSTEM/when) THROUGH an @Lazy self proxy so REQUIRES_NEW + the
                           row lock survive on the production tick; LOSES cleanly to a live accept
reoffer(prior, next):      a NEW append-only row referencing prior; attemptSeq strictly monotonic
```

**1. Exclusivity is a subject-wide lock plus a unique-index backstop (TIMEDOFFER-EXCLUSIVE-001).** Concurrent accepts of *competing* offers for one subject must serialize on the SUBJECT, not on one offer row — so the accept path locks every offer row for the subject (`PESSIMISTIC_WRITE`) and inserts an `Assignment` whose `uq(subject_id)` makes any residual-race second insert a deterministic 409. *"The 409 (Conflict) status code indicates that the request could not be completed due to a conflict with the current state of the target resource."*

**2. The deadline sweep is a concurrent writer reached through a proxy (TIMEDOFFER-LIFECYCLE-001).** A `@Scheduled` sweep expires past-deadline OPEN offers exactly once, recording the SYSTEM actor and when. It MUST reach its `REQUIRES_NEW` per-row handler through an `@Lazy` self proxy — a bare `this.expireOne(...)` self-invocation bypasses the `@Transactional` proxy, silently dropping the lock on the production tick while every tested path keeps it.

**3. The re-offer ladder is append-only (TIMEDOFFER-LADDER-001).** A declined/expired offer is re-offered as a NEW row referencing the prior with a strictly monotonic `attemptSeq`; no existing row is ever mutated to point at a different candidate or deleted.

**Incorrect — no exclusivity backstop, a self-invoked sweep, a mutated re-offer:**

```java
public void accept(UUID offerId, String candidate) {
    var o = offerRepo.findById(offerId).orElseThrow();     // ❌ no subject lock — two competing accepts read "unassigned"
    o.setStatus(OfferStatus.ACCEPTED);                     // ❌ public setter; no uq(subject_id) backstop
    offerRepo.save(o);                                     // ❌ both threads assign one subject (CWE-362)
}
@Scheduled(fixedDelayString = "60000")
public void sweep() {
    for (UUID id : dueOfferIds()) this.expireOne(id);      // ❌ self-invocation bypasses @Transactional → no lock in prod
}
public void reoffer(UUID priorId, String next) {
    var prior = offerRepo.findById(priorId).orElseThrow();
    prior.setCandidate(next);                              // ❌ mutates the prior row — destroys the attempt trail
    offerRepo.save(prior);
}
```

**Correct — subject-locked exclusive accept with a uq backstop, @Lazy-proxied sweep, append-only re-offer:**

```java
@Transactional
public TimedOffer accept(UUID offerId, String candidate) {
    TimedOffer o = offers.findById(offerId).orElseThrow(TimedOfferException::notFound);
    offers.findBySubjectIdForUpdate(o.getSubjectId());                  // ✅ lock the whole subject
    TimedOffer fresh = offers.findById(offerId).orElseThrow(TimedOfferException::notFound);
    Instant now = Instant.now(clock);
    if (fresh.getStatus() != OfferStatus.OPEN) throw TimedOfferException.notOpen(fresh.getStatus().name());
    if (fresh.isPastDeadline(now)) throw TimedOfferException.offerExpired();
    if (assignments.findBySubjectId(fresh.getSubjectId()).isPresent())
        throw TimedOfferException.subjectAlreadyAssigned();             // ✅ exclusivity loser → 409
    try {
        assignments.saveAndFlush(new Assignment(UUID.randomUUID(), fresh.getSubjectId(),
            fresh.getId(), candidate, now));                            // ✅ uq(subject_id) backstop
    } catch (DataIntegrityViolationException dup) {
        throw TimedOfferException.subjectAlreadyAssigned();             // ✅ residual-race loser → 409
    }
    sm.accept(fresh, candidate, now);                                   // ✅ status through the state machine
    return fresh;
}

// ✅ the @Scheduled tick reaches expireOne THROUGH the @Lazy self proxy (REQUIRES_NEW + row lock honored)
public int sweepOnce() {
    for (UUID offerId : service.dueOfferIds(SWEEP_BATCH)) {
        try { self.expireOne(offerId); } catch (RuntimeException ex) { /* live accept won — skip */ }
    }
    return /* swept count */ 0;
}
```

The subject-wide `PESSIMISTIC_WRITE` lock serializes competing accepts; the `uq(subject_id)` Assignment index is the suspenders for any residual race (CWE-362). The sweep reaches its `REQUIRES_NEW` handler through the `@Lazy` self proxy so the lock survives the production tick. `Assignment` references the winning `TimedOffer` by IDENTITY (a `UUID offerId`, never an object pointer — HG-AGG-REF); the re-offer ladder is append-only; no delete path exists.

Verification: review-tier — confirm the accept locks the subject and the `uq(subject_id)` backstop is present, the sweep reaches `expireOne` through the `@Lazy self` proxy (not a bare self-invocation), and re-offer appends a new row. The behavioural proof a fork-receiver keeps green: the N-thread competing-accept race (exactly one 2xx + N-1 409, exactly one Assignment row).

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)

Reference: [RFC 9110 §15.5.10 (409 Conflict)](https://www.rfc-editor.org/rfc/rfc9110.html)

Reference: [FDCPA 15 U.S.C. § 1692g](https://www.law.cornell.edu/uscode/text/15/1692g)
