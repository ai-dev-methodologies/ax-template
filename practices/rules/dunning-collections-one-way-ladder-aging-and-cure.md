---
title: An overdue-receivable collections lifecycle must walk a ONE-WAY dunning ladder with EXACTLY-ONCE stage transitions (a uq(case,stage) DB backstop, never skip or reverse), compute its aging bucket DETERMINISTICALLY from days-overdue at a RECORDED as-of instant (never a bare label), open a cure window on payment that resets to CURRENT and HALTS the ladder on full cure / resumes it on lapse, and serialize concurrent advances on the case row so exactly one wins
impact: HIGH
impactDescription: "A collections ladder that can skip a rung, reverse, or fire the same notice twice produces wrong-amount/wrong-stage dunning a regulator can sanction (the FDCPA staged-notice discipline exists precisely to bound escalation); an aging bucket with no recorded basis cannot be reconciled to the allowance-for-doubtful-accounts the receivable is stated net of; and an unsynchronized advance lets two threads double-escalate one case (CWE-362) — the case ends with two transition rows for one rung or a stage that jumped a rung"
tags:
  - state-machine
  - audit
  - concurrency
  - billing
  - governance
spec_ref: "specs/dunning-collections-l0.yaml#DUNNING-LADDER-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/dunning/DunningService.java + backend/src/main/java/com/ax/template/authblueprint/dunning/DunningCase.java + backend/src/main/java/com/ax/template/authblueprint/dunning/DunningStageTransition.java"
  pattern: "The dunning ladder advances one-way REMINDER→NOTICE→FINAL_NOTICE→SUSPENDED, one rung per advance, appending an immutable DunningStageTransition whose uq(case_id, stage, kind) makes a re-emit a deterministic 409; advancing past SUSPENDED is 409; the advance gates on the OBSERVED fromStage under the case's PESSIMISTIC_WRITE row lock so concurrent advances converge to exactly one winner; the aging bucket is computed deterministically by days-overdue (CURRENT/B1_30/B2_60/B3_90_PLUS) and PERSISTED with its basis (as-of instant + due date + days-overdue) so a bare label is unrepresentable; a payment opens a cure window, a full cure within it resets aging to CURRENT + halts the ladder (recorded as a CURED transition) and is idempotent, a lapse releases the halt so the ladder resumes; NO delete path exists on the case"
upstream:
  - "https://www.law.cornell.edu/uscode/text/15/1692g"
  - "https://www.law.cornell.edu/cfr/text/17/210.5-02"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "Fair Debt Collection Practices Act, 15 U.S.C. § 1692g(a) (Cornell LII) — the staged collection-notice discipline the dunning ladder generalizes: a written notice with the amount, the creditor, and a 30-day dispute window before the debt is treated as valid"
    url: "https://www.law.cornell.edu/uscode/text/15/1692g"
    quote: "Within five days after the initial communication with a consumer in connection with the collection of any debt, a debt collector shall, unless the following information is contained in the initial communication or the consumer has paid the debt, send the consumer a written notice containing— (1) the amount of the debt; (2) the name of the creditor to whom the debt is owed; (3) a statement that unless the consumer, within thirty days after receipt of the notice, disputes the validity of the debt, or any portion thereof, the debt will be assumed to be valid by the debt collector"
    quoted_at: "2026-06-22"
  - source_type: external
    citation: "17 CFR § 210.5-02(4) (Regulation S-X, Cornell LII) — the allowance for doubtful accounts the aging bucket informs, stated separately on the balance sheet"
    url: "https://www.law.cornell.edu/cfr/text/17/210.5-02"
    quote: "Allowances for doubtful accounts and notes receivable. The amount is to be set forth separately in the balance sheet or in a note thereto."
    quoted_at: "2026-06-22"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent advances racing one dunning case)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A collections lifecycle is a one-way exactly-once ladder, a recorded-basis aging bucket, and a cure window — not an ad-hoc status flag

**Impact: HIGH — a ladder that skips/reverses/double-fires produces sanctionable dunning; an aging label with no recorded basis cannot reconcile to the allowance; an unsynchronized advance double-escalates one case (CWE-362).**

A *dunning* (overdue-receivable collections) lifecycle is the staged escalation a billing system runs against every overdue invoice. The discipline is statutory: the FDCPA requires a debt collector to *"send the consumer a written notice containing— (1) the amount of the debt; (2) the name of the creditor to whom the debt is owed; (3) a statement that unless the consumer, within thirty days after receipt of the notice, disputes the validity of the debt … the debt will be assumed to be valid"* — a STAGED, bounded escalation, not an arbitrary one. The catalog governed decisions (`decision-governance`), thresholds (`threshold-terminal`), and obligations (`deadline-obligation`) but had no primitive for the aged, cured, exactly-once collections ladder:

```text
advance(case, fromStage):  one-way REMINDER→NOTICE→FINAL_NOTICE→SUSPENDED, one rung per call
                           append an immutable DunningStageTransition; uq(case,stage,kind) =
                           the exactly-once backstop; past SUSPENDED → 409; gated on the
                           OBSERVED fromStage under the case's PESSIMISTIC_WRITE lock
aging:                     bucket = f(days-overdue): CURRENT / B1_30 / B2_60 / B3_90_PLUS,
                           PERSISTED with its basis (as-of instant + due date + days) — never bare
cure:                      a payment opens a cure window; full cure within it → aging CURRENT +
                           ladder HALTED (recorded CURED transition), idempotent; lapse → resume
locks:                     the case row, PESSIMISTIC_WRITE — concurrent advances → exactly one wins
```

**1. The ladder is one-way and exactly-once (DUNNING-LADDER-001).** Each advance moves to the single next rung and appends an immutable transition row; the `uq(case_id, stage, kind)` index makes re-emitting a reached rung a deterministic 409, and the `fromStage` precondition under the row lock makes concurrent advances converge. SUSPENDED is terminal.

**2. The aging bucket carries its own basis (DUNNING-AGING-001).** A bucket is computed deterministically from whole days overdue at a recorded as-of instant; the as-of, the due date, and the days-overdue are persisted so the bucket reconciles to the *"Allowances for doubtful accounts"* the receivable is stated net of. A bare label is unrepresentable.

**3. A payment opens a cure window; a full cure halts, a lapse resumes (DUNNING-CURE-001).** Full payment within the window resets aging to CURRENT and HALTS the ladder (recorded), idempotently; a lapsed window releases the halt so the next advance resumes the one-way walk where it left off — the reached rungs keep their exactly-once transitions.

**Incorrect — a mutable status flag, a bare aging label, an unsynchronized advance:**

```java
public void escalate(UUID caseId) {
    DunningCase c = repo.findById(caseId).orElseThrow();   // ❌ no row lock — two threads both read REMINDER
    c.setStage(nextStage(c.getStage()));                   // ❌ public setter; no exactly-once backstop;
    c.setAgingBucket("90+");                               // ❌ bare label — no as-of, no days-overdue basis
    repo.save(c);                                          // ❌ both threads write → double escalation (CWE-362)
}
```

**Correct — one-way exactly-once advance under the case lock, recorded-basis aging, cure halt:**

```java
@Transactional
public DunningCase advance(UUID caseId, DunningStage fromStage, String actor) {
    DunningCase c = cases.findByIdForUpdate(caseId).orElseThrow(DunningException::notFound); // ✅ PESSIMISTIC_WRITE
    if (c.getStage() == DunningStage.SUSPENDED) throw DunningException.ladderTerminal();      // 409 — terminal
    if (c.getStage() != fromStage) throw DunningException.stageAlreadyReached();              // ✅ exactly-once gate
    DunningStage nextStage = c.getStage().next();
    Instant now = Instant.now(clock);
    long days = daysOverdue(c.getDueDate(), now);
    try {
        members.persistAndFlush(new DunningStageTransition(UUID.randomUUID(), c.getId(),
            nextStage, "ADVANCE", days, actor, now));      // ✅ uq(case,stage,kind) backstop
    } catch (DataIntegrityViolationException dup) {
        throw DunningException.stageAlreadyReached();       // ✅ loser of any residual race → 409
    }
    c.advanceTo(nextStage);
    c.reage(AgingBucket.of(days), now, days);               // ✅ bucket + basis (as-of + days) recorded
    return c;
}

@Transactional
public DunningCase cure(UUID caseId, String actor) {
    DunningCase c = cases.findByIdForUpdate(caseId).orElseThrow(DunningException::notFound);
    if (c.isLadderHalted() && c.getAgingBucket() == AgingBucket.CURRENT) return c;            // ✅ idempotent
    Instant now = Instant.now(clock);
    boolean windowOpen = c.getCureDeadline() != null && now.isBefore(c.getCureDeadline());
    if (!windowOpen || !c.isFullyPaid()) throw DunningException.noCureWindow();               // 422
    members.persistAndFlush(new DunningStageTransition(UUID.randomUUID(), c.getId(),
        c.getStage(), "CURED", c.getDaysOverdue(), actor, now));   // ✅ the halt is recorded
    c.cure();                                                       // ✅ aging→CURRENT, ladder halted
    return c;
}
```

The case-row PESSIMISTIC_WRITE lock serializes the read-stage / write-next-stage sequence; the `fromStage` precondition makes N concurrent advances from one rung resolve to exactly one winner; the `uq(case_id, stage, kind)` index is the suspenders for any residual race (CWE-362). The aging columns carry their own basis so the bucket is reconstructible. `DunningStageTransition` rows are `@AggregateMember` of `DunningCase` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists on the case.

Verification: review-tier — confirm the ladder advances one rung one-way, the transition rows are append-only one-per-(case,stage,kind), the aging bucket persists its as-of/days-overdue basis, the cure path halts on full payment and resumes on lapse, and the advance/cure both take the case's PESSIMISTIC_WRITE lock. The behavioural proof a fork-receiver keeps green: the N-thread advance race (exactly one 2xx + N-1 409, exactly one transition row).

Reference: [FDCPA 15 U.S.C. § 1692g](https://www.law.cornell.edu/uscode/text/15/1692g)

Reference: [17 CFR § 210.5-02 (Regulation S-X — allowance for doubtful accounts)](https://www.law.cornell.edu/cfr/text/17/210.5-02)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
