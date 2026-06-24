---
title: A collective weighted decision must collect IMMUTABLE one-per-voter ballots, freeze the resolution policy (threshold + quorum fraction + abstention mode + tie-break) at motion-open, measure QUORUM against ELIGIBLE weight (not cast weight) so quorum-not-met yields NO_DECISION distinct from REJECTED, compare the threshold with EXACT integer/BigDecimal arithmetic, break ties by the frozen deterministic order — and resolve as a PURE reproducible function of the immutable ballots so re-resolving returns the identical record
impact: HIGH
impactDescription: "Measuring quorum against the votes CAST instead of the ELIGIBLE membership makes quorum trivially always-met (every cast vote is 100% of casts) — the single most common collective-decision bug, silently passing motions a sparse turnout should have left undecided; a floating-point threshold makes a 1/3 or 2/3 supermajority non-reproducible so the same ballots can resolve differently on re-run; a tie broken by HashMap iteration order is non-deterministic; a mutable ballot or a policy that drifts after voting opens lets the outcome be rewritten after the fact; and conflating NO_DECISION (quorum not met) with REJECTED (quorum met, threshold not met) destroys the legal distinction between a motion that failed and a body that never had standing to decide"
tags:
  - governance
  - state-machine
  - voting
  - quorum
  - concurrency
spec_ref: "specs/quorum-resolution-l0.yaml#QR-RESOLVE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/quorumresolution/QuorumService.java + backend/src/main/java/com/ax/template/authblueprint/quorumresolution/Ballot.java + backend/src/main/java/com/ax/template/authblueprint/quorumresolution/Motion.java"
  pattern: "Ballots are append-only (all columns @Column(updatable=false), no public setter, UNIQUE(motion_id, voter_id) so a double-vote is unrepresentable); the resolution policy + eligibility roster + total_eligible_weight are snapshotted @Column(updatable=false) at motion-open; resolve acquires the motion row under PESSIMISTIC_WRITE, computes quorum as cast_eligible_weight * quorum_denominator >= quorum_numerator * total_eligible_weight (against ELIGIBLE weight) → NO_DECISION when not met, and the threshold via integer cross-multiplication (yes_weight * threshold_denominator >= threshold_numerator * base) — never floating point; an exact tie is broken by the FROZEN tie_break_mode, never by collection order; the Resolution row is @Column(updatable=false) and UNIQUE(motion_id) so a second resolve returns the identical record rather than inserting a new one"
upstream:
  - "https://github.com/bitcoin/bips/blob/master/bip-0011.mediawiki"
  - "https://csrc.nist.gov/pubs/ir/8214/final"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "BIP-11: M-of-N Standard Transactions (Gavin Andresen, 2011) — the canonical M-of-N threshold-authorization formalization: M of N weighted authorities must concur to authorize"
    url: "https://github.com/bitcoin/bips/blob/master/bip-0011.mediawiki"
    quote: "This BIP proposes M-of-N-signatures required transactions as a new \"standard\" transaction type."
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "NIST IR 8214 — Threshold Schemes for Cryptographic Primitives (Brandão, Mouha, Vassilev, 2019) — distributed t-of-n authority: the operation's security holds only with a threshold of independent parties, no single party acting alone"
    url: "https://csrc.nist.gov/pubs/ir/8214/final"
    quote: "where multiple components contribute to the operation in a way that attains the desired security goals even if f out of n of its components are compromised"
    quoted_at: "2026-06-24"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent ballot casts / a cast racing the resolve transition on one motion)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## Rule

A **collective weighted decision** — a motion resolved by a body of weighted peers (board/shareholder resolution, multi-sig authorization, condo/HOA vote, credit/investment committee, peer review) — MUST be modeled so the outcome is a **pure, reproducible function** of immutable ballots and a policy frozen before voting began. Concretely:

1. **Immutable, one-per-voter ballots.** Each ballot is append-only (`@Column(updatable=false)`, no setter); a `UNIQUE(motion_id, voter_id)` constraint makes a double-vote unrepresentable. A correction is a new motion, never an edit. `ABSTAIN` is a *cast act* (it counts toward quorum participation), distinct from a non-vote.
2. **Policy frozen at open.** The resolution rule — threshold (majority / supermajority / M-of-N / percentage), quorum fraction, abstention mode, tie-break order — plus the eligibility roster and `total_eligible_weight` are snapshotted immutably when the motion opens. They cannot drift after the first ballot (the reproducibility precondition; the same shape as a `decision-governance` basis snapshot).
3. **Quorum against ELIGIBLE weight.** Quorum is `cast_eligible_weight * quorum_denominator >= quorum_numerator * total_eligible_weight` — measured against the *eligible membership*, never the votes cast. Quorum-not-met → **`NO_DECISION`**, a terminal distinct from `REJECTED` (Robert's Rules: in the absence of a quorum no business can be transacted — the body had no standing to decide, which is not the same as deciding "no").
4. **Exact threshold arithmetic.** Compare via integer/BigDecimal cross-multiplication (`yes_weight * threshold_denominator >= threshold_numerator * base`). Never `(double) yes / base >= 0.5` — floating point makes a 1/3 or 2/3 supermajority non-reproducible.
5. **Deterministic tie-break.** An exact tie resolves by the *frozen* `tie_break_mode` (e.g. `TIE_FAILS` → REJECTED; `CHAIR_CASTING` → the named tie-break voter's choice) — never by `HashMap`/collection iteration order.
6. **Reproducible + serialized.** `resolve` reads only the frozen policy + immutable ballots; re-resolving returns the identical record (`UNIQUE(motion_id)`). Casts and the resolve transition serialize on the motion row under `PESSIMISTIC_WRITE` (CWE-362).

**Correct — pure resolve over a frozen policy + immutable ballots; quorum vs ELIGIBLE weight; integer threshold:**

```java
// backend/.../quorumresolution/QuorumService.java — resolve is a pure function of frozen policy + immutable ballots
@Transactional
public Resolution resolve(UUID motionId, String caller) {
    Motion m = motions.findByIdForUpdate(motionId)          // PESSIMISTIC_WRITE — serialize vs casts (CWE-362)
        .orElseThrow(QuorumException::motionNotFound);
    if (!m.getConvenerId().equals(caller)) throw QuorumException.motionNotFound(); // IDOR-safe 404
    Optional<Resolution> existing = motions.findResolution(motionId);
    if (existing.isPresent()) return existing.get();        // idempotent: identical record, no second insert

    long yes = 0, no = 0, abstain = 0, cast = 0;            // scalar tally — no map iteration order leaks
    for (Ballot b : motions.findBallots(motionId)) {        // immutable weight_at_cast only
        cast += b.getWeightAtCast();
        switch (b.getChoice()) {
            case YES -> yes += b.getWeightAtCast();
            case NO -> no += b.getWeightAtCast();
            case ABSTAIN -> abstain += b.getWeightAtCast();
        }
    }
    Outcome outcome;
    // QUORUM against ELIGIBLE weight (not cast) — exact integer cross-multiplication (multiplyExact: fail-closed on overflow)
    if (Math.multiplyExact(cast, m.getQuorumDenominator()) < Math.multiplyExact((long) m.getQuorumNumerator(), m.getTotalEligibleWeight())) {
        outcome = Outcome.NO_DECISION;                      // terminal distinct from REJECTED
    } else {
        long base = (m.getAbstentionMode() == AbstentionMode.COUNT_AS_NO) ? yes + no + abstain : yes + no;
        long lhs = Math.multiplyExact(yes, m.getThresholdDenominator());
        long rhs = Math.multiplyExact((long) m.getThresholdNumerator(), base);
        // The deadlock that warrants a tie-break is threshold EQUALITY (yes exactly AT the bar) —
        // NOT yes==no, which only coincides with the bar at a 1/2 majority and would spuriously
        // flip a 1/3 or 2/3 motion that cleared its threshold. PASS iff strictly over the bar.
        outcome = lhs > rhs ? Outcome.PASSED
            : lhs < rhs ? Outcome.REJECTED
            : breakTie(m);                                  // exactly at the bar → frozen tie_break_mode decides
    }
    Resolution r = members.persist(new Resolution(UUID.randomUUID(), motionId, outcome,
        yes, no, abstain, cast, m.getTotalEligibleWeight(), Instant.now(clock)));
    stateMachine.markResolved(m);
    metrics.recordResolution(outcome);
    return r;
}
```

**Incorrect — quorum measured against CAST weight (always met) + floating-point threshold (non-reproducible):**

```java
// quorum measured against CAST weight → always trivially met; floating-point threshold → non-reproducible
public Resolution resolve(UUID motionId) {
    var ballots = repo.findBallots(motionId);
    double yes = ballots.stream().filter(b -> b.getChoice() == YES).count();
    double cast = ballots.size();
    if (cast / eligibleCount < 0.5) return reject();        // WRONG: quorum vs CAST + floating point
    boolean passed = yes / cast >= 0.6667;                  // WRONG: FP supermajority is non-reproducible
    // WRONG: no frozen policy, no NO_DECISION, ties undefined, ballot/resolution mutable
    return passed ? pass() : reject();
}
```

The Incorrect form passes quorum for any non-empty turnout (every cast vote is "100% of casts"), resolves a 2/3 supermajority differently across runs because of float rounding, has no `NO_DECISION` terminal, and leaves ties to chance.

Reference: [BIP-11: M-of-N Standard Transactions](https://github.com/bitcoin/bips/blob/master/bip-0011.mediawiki)

Reference: [NIST IR 8214 — Threshold Schemes for Cryptographic Primitives](https://csrc.nist.gov/pubs/ir/8214/final)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
