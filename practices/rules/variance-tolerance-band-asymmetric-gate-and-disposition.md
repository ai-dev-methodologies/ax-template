---
title: A standard-vs-actual appraisal must DERIVE the variance (actual − standard, never an entered field), PIN the asymmetric tolerance band that governed THIS verdict on the row, render the verdict by a two-sided gate (WITHIN_TOLERANCE iff variance ∈ [−lowerTolerance, +upperTolerance], else OUT_OF_TOLERANCE), BLOCK any dependent operation on a breach (422 naming the variance + band) until an explicit who/when/reason DISPOSITION is recorded, and serialize concurrent dispositions on the appraisal row so exactly one wins
impact: HIGH
impactDescription: "A variance entered as a field rather than derived can silently disagree with actual − standard (the breach is hidden); a verdict stored as a bare pass/fail boolean with no recorded standard/actual/variance/band cannot be reconciled to the allowance the books are stated against or the spec the lot is released against; a SYMMETRIC ±band collapses an asymmetric favorable/unfavorable tolerance and passes values that should breach (or breaches values that should pass); a breach that flows downstream with no block lets an out-of-tolerance cost post / lot release / budget draw proceed unaccountably; and an unsynchronized disposition lets two threads both override one breach (CWE-362), leaving two disposition rows for one accountable decision"
tags:
  - state-machine
  - audit
  - concurrency
  - governance
  - validation
spec_ref: "specs/variance-tolerance-band-l0.yaml#VG-DERIVE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/variancegate/VarianceService.java + backend/src/main/java/com/ax/template/authblueprint/variancegate/VarianceAppraisal.java + backend/src/main/java/com/ax/template/authblueprint/variancegate/VarianceDisposition.java"
  pattern: "The appraisal DERIVES variance = actualValue.subtract(standardValue) (BigDecimal at a recorded scale; never a caller-supplied field) and PERSISTS it immutably alongside the standard, the actual, and the band in force (lowerTolerance, upperTolerance) so the verdict is re-derivable; the verdict is the ASYMMETRIC two-sided gate WITHIN_TOLERANCE iff variance ≥ −lowerTolerance AND variance ≤ +upperTolerance (compareTo, inclusive), else OUT_OF_TOLERANCE; a dependent operation on an OUT_OF_TOLERANCE appraisal is blocked 422 (VARIANCE_OUT_OF_TOLERANCE) with the variance + band named, until an explicit VarianceDisposition (actor + injected-Clock timestamp + non-blank reason) is recorded; the disposition is append-only one-per-appraisal (uq(appraisal_id)) and never rewrites the verdict to WITHIN_TOLERANCE; the dispose path takes the appraisal's PESSIMISTIC_WRITE row lock so concurrent dispositions converge to exactly one; NO delete path exists on the appraisal"
upstream:
  - "https://www.itl.nist.gov/div898/handbook/pmc/section1/pmc16.htm"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "NIST/SEMATECH e-Handbook of Statistical Methods, §6.1.6 'What is Process Capability?' — the SPC tolerance band (a measured value within the upper/lower specification limits) that the asymmetric variance gate generalizes"
    url: "https://www.itl.nist.gov/div898/handbook/pmc/section1/pmc16.htm"
    quote: "A process where almost all the measurements fall inside the specification limits is a capable process."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent dispositions racing one breached appraisal)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-23"
---

## A standard-vs-actual appraisal is a derived variance, a pinned asymmetric band, a two-sided verdict, and a breach that blocks until disposed — not a bare pass/fail flag

**Impact: HIGH — an entered (vs derived) variance hides the breach; a bare pass/fail with no recorded basis cannot reconcile to the allowance/spec; a symmetric ±band mis-gates an asymmetric tolerance; an un-blocked breach flows downstream unaccountably; an unsynchronized disposition double-overrides one breach (CWE-362).**

A *variance appraisal* compares a measured ACTUAL against a recorded STANDARD and gates a dependent operation on the result. The discipline is standard from two directions: managerial standard-cost variance analysis (the favorable/unfavorable variance = actual − standard, feeding the allowance the books are stated against) and statistical process control, where — per the NIST/SEMATECH handbook — *"A process where almost all the measurements fall inside the specification limits is a capable process."* The catalog conserved transformations, versioned computed values, and crossed irreversible thresholds, but had no primitive for the asymmetric appraise-and-gate:

```text
appraise(standard, actual, lower, upper):  variance = actual − standard (DERIVED, BigDecimal, recorded scale)
                                           PERSIST standard, actual, variance, AND the band (lower, upper) — basis
                                           verdict = WITHIN_TOLERANCE iff variance ∈ [−lower, +upper] (asymmetric, inclusive)
gate(dependent op):                        OUT_OF_TOLERANCE + not disposed → block 422 naming variance + band
dispose(breach, reason):                   record actor + injected-Clock when + non-blank reason; one per appraisal
                                           verdict is NOT rewritten — the breach stays visible WITH an override on record
locks:                                     the appraisal row, PESSIMISTIC_WRITE — concurrent dispositions → exactly one wins
```

**1. The variance is derived and the band is pinned (VG-DERIVE-001).** The variance is `actual.subtract(standard)` — never a caller field — and the standard, actual, variance, and the band in force (lower/upper) are all persisted immutably, so the verdict is re-derivable. Bands drift; the row pins which band governed THIS verdict (the way remeasurement-trueup pins the thresholds that governed a run).

**2. The gate is asymmetric (VG-GATE-001).** `WITHIN_TOLERANCE` iff `variance ≥ −lower AND variance ≤ +upper` by `BigDecimal.compareTo` (inclusive both bounds); the favorable allowance (lower) and unfavorable allowance (upper) are independent magnitudes — collapsing them into a symmetric ±band mis-gates.

**3. A breach blocks the dependent operation until disposed (VG-BLOCK-001 / VG-DISPOSE-001).** An operation depending on an OUT_OF_TOLERANCE appraisal is blocked 422 naming the variance and band, until an explicit `VarianceDisposition` (actor + timestamp + non-blank reason) is recorded. The override never erases the breach — it records an accountable decision to proceed despite it.

**Incorrect — an entered variance, a bare boolean verdict, a symmetric band, an unsynchronized override:**

```java
public boolean appraise(BigDecimal standard, BigDecimal actual, BigDecimal variance, BigDecimal tol) {
    boolean pass = variance.abs().compareTo(tol) <= 0;   // ❌ symmetric ±tol — collapses favorable/unfavorable
    repo.save(new Row(standard, actual, variance, pass));// ❌ variance is an ENTERED field, may disagree with actual−standard
    return pass;                                          // ❌ bare boolean — band not pinned, verdict not re-derivable
}
public void override(UUID id) {
    Row r = repo.findById(id).orElseThrow();             // ❌ no row lock — two threads both override (CWE-362)
    r.setVerdict("WITHIN_TOLERANCE");                    // ❌ rewrites the verdict — the breach vanishes from the record
    repo.save(r);                                        // ❌ no who/when/reason — silent acceptance, not a disposition
}
```

**Correct — derived variance, pinned asymmetric band, recorded breach, audited disposition under the appraisal lock:**

```java
@Transactional
public VarianceAppraisal appraise(String subject, BigDecimal standard, BigDecimal actual,
                                  BigDecimal lower, BigDecimal upper) {
    BigDecimal variance = actual.subtract(standard);                       // ✅ DERIVED, never entered
    VarianceVerdict verdict = (variance.compareTo(lower.negate()) >= 0
            && variance.compareTo(upper) <= 0)
        ? VarianceVerdict.WITHIN_TOLERANCE : VarianceVerdict.OUT_OF_TOLERANCE;   // ✅ asymmetric, inclusive
    return appraisals.save(new VarianceAppraisal(UUID.randomUUID(), subject, standard, actual,
        variance, lower, upper, verdict, Instant.now(clock)));             // ✅ standard/actual/variance/band pinned
}

@Transactional
public VarianceAppraisal dispose(UUID id, String actor, String reason) {
    VarianceAppraisal a = appraisals.findByIdForUpdate(id).orElseThrow(VarianceException::notFound); // ✅ PESSIMISTIC_WRITE
    if (a.getVerdict() != VarianceVerdict.OUT_OF_TOLERANCE) throw VarianceException.nothingToDispose();// 422
    if (reason == null || reason.isBlank()) throw VarianceException.blankReason();                     // 422
    if (appraisals.findDisposition(id).isPresent()) return a;             // ✅ idempotent — one per appraisal
    try {
        members.persistAndFlush(new VarianceDisposition(UUID.randomUUID(), a.getId(),
            DispositionDecision.OVERRIDE, actor, reason, Instant.now(clock)));   // ✅ uq(appraisal_id) backstop
    } catch (DataIntegrityViolationException dup) {
        throw VarianceException.alreadyDisposed();                        // ✅ loser of any residual race → 409
    }
    return a;                                                             // ✅ verdict stays OUT_OF_TOLERANCE — breach visible
}
```

The appraisal-row `PESSIMISTIC_WRITE` lock serializes the check-not-disposed / write-disposition sequence; the `uq(appraisal_id)` index is the suspenders for any residual race (CWE-362). The standard/actual/variance/band columns carry the basis so the verdict is reconstructible. `VarianceDisposition` rows are `@AggregateMember` of `VarianceAppraisal` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists on the appraisal.

Verification: review-tier — confirm the variance is derived (not entered), the band (lower/upper) is persisted on the row, the gate is the asymmetric two-sided inclusive comparison, a dependent operation on a breach is blocked 422 with the variance + band named, the disposition records actor/when/non-blank-reason and never rewrites the verdict, and the dispose path takes the appraisal's `PESSIMISTIC_WRITE` lock. The behavioural proof a fork-receiver keeps green: the N-thread disposition race (exactly one accountable disposition row).

Reference: [NIST/SEMATECH e-Handbook §6.1.6 — Process Capability](https://www.itl.nist.gov/div898/handbook/pmc/section1/pmc16.htm)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
