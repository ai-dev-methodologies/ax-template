---
title: Remeasured values must supersede append-only (new row + pointer, no ACTUAL→ESTIMATED downgrade), settlement runs must be versioned with their input basis recorded and recompute idempotently, and a CLOSED period is corrected only by posting the NET delta forward into an open period — the run-of-record is never rewritten
impact: HIGH
impactDescription: "Overwriting a reading in place destroys the proof of what was known when the original settlement was issued; an unversioned recompute makes the settled number irreproducible and silently divergent; and mutating a closed period's run-of-record retro-falsifies every statement, report, and payment that already consumed it — the correction becomes indistinguishable from tampering"
tags:
  - audit
  - state-machine
  - conservation
  - governance
  - concurrency
spec_ref: "specs/remeasurement-trueup-l0.yaml#TUP-SUPERSEDE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/trueup/TrueUpService.java + backend/src/main/java/com/ax/template/authblueprint/trueup/MeterReading.java + backend/src/main/java/com/ax/template/authblueprint/trueup/SettlementPeriod.java + backend/src/main/java/com/ax/template/authblueprint/trueup/SettlementRun.java + backend/src/main/java/com/ax/template/authblueprint/trueup/TrueUpPosting.java"
  pattern: "Reading rows are immutable (value/source/estimationMethod updatable=false); a better value appends slot_version+1 with a forward pointer on the superseded row; ACTUAL→ESTIMATED is 422; every run persists version + input basis (reading rows at versions) + basis hash + total, uq(period_id, run_version), no update path; unchanged-basis recompute returns the existing run; a CLOSED period's recompute posts the NET delta (recalculated − previously settled) as an immutable TrueUpPosting into an OPEN target, uq(run_id), conservation run_of_record + Σpostings == latest total; period walks OPEN→CLOSED→SEALED one-way (@Check closed⇒run-of-record), SEALED fail-closed 409; a run is blocked (422 naming slots) over an incomplete grid unless gaps are explicitly estimated with the method recorded; the period row's PESSIMISTIC_WRITE lock serializes concurrent recomputes onto one new version"
upstream:
  - "https://www.ifrs.org/issued-standards/list-of-standards/ias-8-accounting-policies-changes-in-accounting-estimates-and-errors/"
  - "https://www.pjm.com/-/media/documents/manuals/m29.ashx"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "IAS 8 Accounting Policies, Changes in Accounting Estimates and Errors — IFRS Foundation standard summary (estimate changes are new information, recognised prospectively)"
    url: "https://www.ifrs.org/issued-standards/list-of-standards/ias-8-accounting-policies-changes-in-accounting-estimates-and-errors/"
    quote: "Changes in accounting estimates result from new information or new developments and, accordingly, are not corrections of errors."
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "IAS 8 Accounting Policies, Changes in Accounting Estimates and Errors — IFRS Foundation standard summary (prospective recognition of estimate changes)"
    url: "https://www.ifrs.org/issued-standards/list-of-standards/ias-8-accounting-policies-changes-in-accounting-estimates-and-errors/"
    quote: "The effect of a change in an accounting estimate is recognised prospectively by including it in profit or loss in:"
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "PJM Manual 29: Billing, Revision 32 (Effective May 21, 2025), §1.5 Billing Adjustments — corrections to an already-settled line item ride a later statement as the NET of recalculated vs previously billed"
    url: "https://www.pjm.com/-/media/documents/manuals/m29.ashx"
    quote: "The adjustment that appears on the billing statement will be the net of the recalculated billing line item charge/credit and the previously billed line item charge/credit."
    quoted_at: "2026-06-11"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent recomputes racing one period)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## Better measurements supersede, never overwrite; runs are versioned with their basis; closed periods are corrected forward

**Impact: HIGH — an in-place overwrite erases what was known when; an unversioned recompute is irreproducible; a mutated run-of-record retro-falsifies everything that consumed it.**

A settlement computed from measurements that improve over time (utility bill, payroll retro-pay, premium audit, tax reassessment, royalty true-up) has THREE truths to keep simultaneously: what was measured *then*, what is known *now*, and what was actually *settled*. IAS 8 draws the governing line: *"Changes in accounting estimates result from new information or new developments and, accordingly, are not corrections of errors"* — and their effect *"is recognised prospectively"*. The grid-settlement world operationalizes the same discipline (PJM Manual 29 §1.5): *"The adjustment that appears on the billing statement will be the net of the recalculated billing line item charge/credit and the previously billed line item charge/credit."* The catalog's conservation family conserved a counter (`register`), a transfer (`balanced-posting`), and a set (`netting`) — none governed the measurement itself improving after settlement:

```text
supersede:  better value = NEW row (slot_version+1) + forward pointer on the old;
            value/source/method immutable; ACTUAL→ESTIMATED → 422
run:        version unique per period + input basis (reading rows @ versions) + hash + total;
            unchanged basis → SAME run (idempotent); changed → version+1; runs never updated
true-up:    CLOSED period correction = NET delta (recalculated − previously settled)
            posted into an OPEN period; run-of-record untouched;
            conservation: run_of_record + Σpostings == latest total
grid:       run over N declared slots blocks (422, naming gaps) unless every slot has an
            ACTIVE reading; gap-fill is EXPLICIT (ESTIMATED + method recorded per row)
lifecycle:  OPEN→CLOSED→SEALED one-way; CLOSED ⇒ run-of-record (@Check); SEALED → 409
locks:      period row PESSIMISTIC_WRITE = serialization point; uq(period,version) +
            uq(run_id) on postings = DB backstops
```

**Incorrect — overwrite the reading, recompute in place, rewrite the closed bill:**

```java
public void correctReading(UUID readingId, BigDecimal actual) {
    Reading r = readings.findById(readingId).orElseThrow();
    r.setValue(actual);                       // ❌ what was known at settlement time — gone
    r.setSource("ACTUAL");                    // ❌ estimate trail erased
    SettlementRun run = runs.findByPeriod(r.getPeriodId());
    run.setTotal(recalc(r.getPeriodId()));    // ❌ the settled number mutates in place —
    // every statement that consumed it is now retro-falsified, and nothing says why
}
```

**Correct — append-only supersession; versioned idempotent recompute; forward-posted net delta:**

```java
@Transactional
public MeterReading recordReading(UUID periodId, int slotIndex, BigDecimal value,
                                  ReadingSource source, String estimationMethod) {
    SettlementPeriod period = periods.findByIdForUpdate(periodId).orElseThrow(TrueUpException::notFound);
    requireNotSealed(period);                                 // SEALED → 409
    MeterReading prior = readings.findActive(periodId, slotIndex).orElse(null);
    if (prior != null && prior.getSource() == ReadingSource.ACTUAL && source == ReadingSource.ESTIMATED) {
        throw TrueUpException.downgrade();                    // ✅ estimates never overwrite facts
    }
    MeterReading next = readings.save(new MeterReading(UUID.randomUUID(), periodId, slotIndex,
        prior == null ? 1 : prior.getSlotVersion() + 1, value, source,
        source == ReadingSource.ESTIMATED ? estimationMethod : null, Instant.now(clock)));
    if (prior != null) {
        prior.supersededBy(next.getId());                     // ✅ pointer; value retained verbatim
    }
    return next;
}

@Transactional
public SettlementRun recompute(UUID periodId, UUID targetPeriodId) {
    LockedPair locked = lockPeriods(periodId, targetPeriodId); // ✅ source + target upfront,
    SettlementPeriod period = locked.source();                 //    ascending-id order (deadlock guard)
    requireNotSealed(period);
    List<MeterReading> grid = new ArrayList<>(readings.findActiveByPeriod(periodId));
    List<Integer> missing = missingSlots(period, grid);
    if (!missing.isEmpty()) {
        throw TrueUpException.gridIncomplete(missing);        // ✅ 422 NAMING the gaps
    }
    String basisJson = basisJson(grid);                       // reading rows @ slot versions
    String basisHash = sha256(basisJson);
    SettlementRun latest = runs.findTopByPeriodIdOrderByRunVersionDesc(periodId).orElse(null);
    if (latest != null && latest.getBasisHash().equals(basisHash)) {
        return latest;                                        // ✅ unchanged basis → idempotent
    }
    BigDecimal total = grid.stream().map(MeterReading::getReadingValue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    SettlementRun next = runs.saveAndFlush(new SettlementRun(UUID.randomUUID(), periodId,
        latest == null ? 1 : latest.getRunVersion() + 1,      // ✅ uq(period_id, run_version) backstop
        basisJson, basisHash, total, Instant.now(clock)));
    if (period.getStatus() == PeriodStatus.CLOSED) {          // ✅ run-of-record NEVER touched —
        postTrueUp(period, locked.target(), latest, next);    //    the NET delta rides forward
    }
    return next;
}

private void postTrueUp(SettlementPeriod source, SettlementPeriod target,
                        SettlementRun previousLatest, SettlementRun next) {
    if (target == null) {
        throw TrueUpException.targetRequired();               // closed-period recompute names a target
    }
    if (target.getStatus() != PeriodStatus.OPEN) {
        throw TrueUpException.targetNotOpen();                // 422 — true-ups land in OPEN periods
    }
    SettlementRun runOfRecord = runs.findById(source.getRunOfRecordId())
        .orElseThrow(TrueUpException::notFound);
    BigDecimal settled = runOfRecord.getTotalValue()
        .add(runs.sumPostingsForSource(source.getId()));      // everything previously settled
    BigDecimal delta = next.getTotalValue().subtract(settled); // ✅ NET of recalculated vs settled
    if (delta.signum() != 0) {
        members.persist(new TrueUpPosting(UUID.randomUUID(), next.getId(), source.getId(),
            target.getId(), previousLatest.getRunVersion(), next.getRunVersion(), delta,
            Instant.now(clock)));                             // uq(run_id) — double-post unrepresentable
    }
}
```

Both period rows are locked UP FRONT in ascending-id order (`lockPeriods`) even though the
target is only written on the CLOSED branch — the source's status is unknowable before its
lock is held, and acquiring the target lazily after the source would break the ordering when
`target < source` (the classic circular wait). The trade-off is deliberate: a momentarily
over-wide lock beats a deadlock.

**1. Supersession is the audit trail (TUP-SUPERSEDE-001).** The estimate was *right when made* — the row trail proves what was known when. A new row with `slot_version+1` and a forward pointer keeps both truths; `@Column(updatable=false)` on value/source/method makes the in-place rewrite unrepresentable.

**2. The basis makes the run defensible (TUP-RUNVERSION-001).** A settled number you cannot re-derive is a number you cannot defend. Each run pins the exact reading rows (at versions) it consumed; the basis hash makes unchanged-input recompute a no-op instead of a phantom version.

**3. Corrections ride forward (TUP-DELTA-001).** The run-of-record is what was actually settled — history. The correction is the *net* against everything previously settled (run-of-record plus earlier true-ups), posted into an open period. Conservation (`run_of_record + Σpostings == latest total`) is the invariant a regression cannot silently break: `uq(run_id)` makes double-posting unrepresentable even if the lock discipline regresses.

**4. No silent gaps (TUP-GRID-001); finality is explicit (TUP-SEALED-001).** A run over a declared grid blocks on a missing slot, naming it; gap-filling appends ESTIMATED rows with the method recorded. The period walks OPEN→CLOSED→SEALED one-way — sealing trades correction capacity for finality, and that loss is the point.

Verification: review-tier — confirm readings supersede append-only with the downgrade guard; runs carry version/basis/hash/total with `uq(period_id, run_version)` and no update path; closed-period recompute posts the net delta into an open target and the run-of-record never mutates; grid completeness blocks with named gaps; the period FSM is one-way with the `@Check` closed⇒run-of-record backstop; concurrent recomputes serialize on the period lock and converge (the behavioural proof a fork-receiver keeps green: N concurrent recomputes → exactly one new version, exactly one posting).

Reference: [IAS 8 — IFRS Foundation](https://www.ifrs.org/issued-standards/list-of-standards/ias-8-accounting-policies-changes-in-accounting-estimates-and-errors/)

Reference: [PJM Manual 29: Billing §1.5 Billing Adjustments](https://www.pjm.com/-/media/documents/manuals/m29.ashx)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
