---
title: A scheduled timeout sweep is a concurrent mutator — it must re-check in its own transaction, carry @Version so it LOSES the race against a live action, run REQUIRES_NEW per row, and be idempotent
impact: HIGH
impactDescription: "A @Scheduled deadline sweep that force-transitions a row a live user can also transition will EXPIRE work the user just accepted (or double-act on crash-rerun) unless it re-checks under optimistic lock and loses the accept race — a silent data-corruption / lost-work bug invisible to single-threaded tests"
tags:
  - concurrency
  - scheduling
  - persistence
  - optimistic-locking
spec_ref: "specs/timed-offer-l0.yaml#AVAIL-SWEEP-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/dispatch/DispatchSweeper.java"
  pattern: "The @Scheduled sweep selects candidate rows, then for EACH row calls a cross-bean handler annotated @Transactional(REQUIRES_NEW) that re-reads the row, re-checks the trigger condition (still PENDING AND now(clock) past the deadline) inside that transaction, and performs a @Version-guarded update so a live accept committed in the gap makes the sweep's update bump zero rows and SKIP; never one class-level @Transactional spanning the loop, never Instant.now() without the injected clock"
upstream:
  - "https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/version"
  - "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
  - "https://cwe.mitre.org/data/definitions/367.html"
evidence:
  - source_type: external
    citation: "Jakarta Persistence 3.1 — @Version annotation API documentation"
    url: "https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/version"
    quote: "Specifies the version field or property of an entity class that serves as its optimistic lock value."
    quoted_at: "2026-06-07"
  - source_type: external
    citation: "Spring Framework Reference — Task Execution and Scheduling (@Scheduled)"
    url: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
    quote: "You can add the @Scheduled annotation to a method, along with trigger metadata."
    quoted_at: "2026-06-07"
  - source_type: external
    citation: "CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition — MITRE Common Weakness Enumeration"
    url: "https://cwe.mitre.org/data/definitions/367.html"
    quote: "The product checks the state of a resource before using that resource, but the resource's state can change between the check and the use in a way that invalidates the results of the check."
    quoted_at: "2026-06-07"
---

## A scheduled timeout sweep is a concurrent mutator — it must lose the race against a live action

**Impact: HIGH — a deadline sweep that force-transitions a row a user can also transition will expire work the user just accepted, unless it re-checks under optimistic lock and loses the accept race.**

A *timeout sweep* is a `@Scheduled` job that drives a state transition because a deadline passed: expire a `PENDING` offer past its `offer_expires_at`, release a reservation hold past its TTL, mark a stale-`AVAILABLE` provider `OFFLINE`, flag an SLA breach, dun a subscription. The naïve implementation reads "rows past their deadline" and writes them all to the terminal state in one batch transaction. That is wrong in four ways the moment a **live user action can transition the same row** — and a single-threaded test never shows it, because the bug only appears when the sweep and the live action overlap.

The governing fact is that the sweep is just another writer. Spring's scheduling support is explicit that *"You can add the @Scheduled annotation to a method, along with trigger metadata"* — it gives you a periodic invocation, nothing more; it confers no isolation from concurrent writers. So a sweep that fires at the exact moment a provider taps **Accept** on an offer whose deadline just elapsed is two transactions racing one row. If the sweep wins blindly, it force-`EXPIRED`s an offer the user successfully accepted — the user sees a confirmed job that silently vanishes (CWE-367: *"the resource's state can change between the check and the use in a way that invalidates the results of the check"*).

The fix is to make the sweep **lose** that race by routing its write through the entity's optimistic-lock version. Jakarta Persistence defines `@Version` as the field that *"serves as its optimistic lock value"*: an update is applied only if the version the writer loaded still matches the row, otherwise it fails. So if a live accept commits in the gap (bumping the version and moving the offer to `ACCEPTED`), the sweep's stale-version update bumps **zero rows** and the sweep skips that offer instead of clobbering it. The accept wins; the sweep yields. This is the same affected-rows discipline as a status-guarded claim, applied to the timeout direction.

Four requirements, together:

1. **Re-check the trigger inside the sweep's own transaction** — not from the list the scheduler read a moment ago. Re-read the row and confirm it is *still* in the pre-trigger state AND *still* past the deadline (`now(clock).isAfter(expiresAt)`), reading `now` from an injected `Clock`.
2. **Carry `@Version` and let the sweep lose** — the terminal-transition update is version-guarded; a live action that committed in the gap makes the sweep update zero rows → skip, never force.
3. **One transaction per swept row (`REQUIRES_NEW`), not one batch transaction** — so a single conflicted/failed row neither rolls back the others nor holds one giant lock; and a crash mid-sweep leaves a clean prefix.
4. **Be idempotent** — re-running the sweep (after a crash, or on the next tick) must not double-act: a row already in its terminal state is simply skipped by requirement 1.

Because `REQUIRES_NEW` must start a genuinely new transaction, the per-row handler has to be a **cross-bean** call (a self-invocation inside the same bean is not proxied, so the new-transaction boundary is silently ignored). The sweep component iterates; a separate transactional service method handles one row.

**Incorrect — one batch transaction, no re-check, no version guard (force-expires accepted work):**

```java
@Component
public class OfferSweeper {
    @Scheduled(fixedDelay = 1000)
    @Transactional                                   // ❌ ONE tx spans the whole loop
    public void expireDueOffers() {
        // ❌ reads a snapshot, then writes blindly — an offer accepted between
        //    the SELECT and the UPDATE is force-EXPIRED (lost work, CWE-367)
        for (Offer o : offerRepo.findByStatusAndExpiresAtBefore(PENDING, Instant.now())) {
            o.setStatus(EXPIRED);                     // ❌ no version guard: the sweep always wins
            offerRepo.save(o);
        }
    }
}
```

**Correct — per-row `REQUIRES_NEW`, re-check under the injected clock, `@Version` makes the sweep lose:**

```java
@Component
public class DispatchSweeper {
    private final OfferRepository offerRepo;
    private final DispatchService dispatch;          // cross-bean → REQUIRES_NEW is honored

    @Scheduled(fixedDelay = 1000)
    public void sweep() {                             // ✅ no @Transactional spanning the loop
        for (UUID offerId : offerRepo.findDueOfferIds(Instant.now(clock))) {
            dispatch.expireOneOffer(offerId);         // ✅ one tx per row
        }
    }
}

@Service
public class DispatchService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)   // ✅ its own transaction
    public void expireOneOffer(UUID offerId) {
        Offer o = offerRepo.findById(offerId).orElse(null);
        if (o == null) return;                        // ✅ idempotent: already gone
        // ✅ re-check the trigger INSIDE this tx, against the injected clock
        if (o.getStatus() != PENDING || !Instant.now(clock).isAfter(o.getExpiresAt())) {
            return;                                   // a live accept won — skip, do not clobber
        }
        offerStateMachine.expire(o);                  // @Version-guarded write
        // ... if the live accept committed first, this update bumps 0 rows / throws
        //     OptimisticLockException → this row is skipped; the accept stands
        reOfferToNextCandidate(o);                    // release hold + next PENDING, same tx
    }
}
```

This is the shape the dispatch reference workload runs (`DispatchSweeper` + `DispatchService.expireOneOffer`), and the same contract the read-only `DsrSlaSweeper` already follows for the SLA-breach direction. The keystone proof a fork-receiver writes is a concurrency test: fire an `Accept` and the sweep at the same offer whose deadline just passed, and assert **exactly one** outcome — the accept stands and the sweep skipped, or the sweep expired and the accept got a deterministic 409/410 — never both, and never a force-expired accepted offer.

Verification: review-tier — confirm the sweep has no loop-spanning `@Transactional`, delegates each row to a cross-bean `REQUIRES_NEW` handler, re-checks the trigger against an injected `Clock` inside that handler, and writes through a `@Version`-guarded path so a concurrent live action makes the sweep update zero rows.

Reference: [Jakarta Persistence 3.1 — @Version (optimistic lock value)](https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/version)

Reference: [Spring Framework — Task Execution and Scheduling (@Scheduled)](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

Reference: [CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition](https://cwe.mitre.org/data/definitions/367.html)
