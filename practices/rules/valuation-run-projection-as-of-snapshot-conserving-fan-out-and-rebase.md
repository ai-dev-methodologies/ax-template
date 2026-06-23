---
title: A versioned valuation run must be pinned to an AS-OF instant + recorded basis and IMMUTABLE once computed (an as-of read returns the GREATEST as-of ≤ T, never a later run), fan out to N per-position outputs whose values SUM EXACTLY to the run total (a DB @Check AND an INDEPENDENT repo-SUM cross-check, never a by-construction tautology), and rebase by creating a NEW baseline run that RETAINS every prior run verbatim via a forward pointer — all serialized on the subject row so concurrent recompute/rebase create exactly one new version
impact: HIGH
impactDescription: "A valuation read that returns the latest run instead of the one current AS OF the queried time reports a value that did not exist at T — a point-in-time correctness break a fund/NAV regulator can sanction (Rule 2a-4 requires the current net asset value as of a time); a fan-out whose per-position outputs do not sum to the run total silently loses or invents value (and a by-construction total := Σ outputs proves nothing — it is tautological); a rebase that rewrites the prior run instead of appending a new baseline destroys the reconstructible history Regulation S-X requires; and an unserialized recompute lets two threads both write version n+1 (CWE-362), leaving the subject with two runs at one version"
tags:
  - state-machine
  - audit
  - concurrency
  - governance
  - billing
spec_ref: "specs/valuation-run-projection-l0.yaml#VALRUN-ASOF-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/valuationrun/ValuationRunService.java + backend/src/main/java/com/ax/template/authblueprint/valuationrun/ValuationRun.java + backend/src/main/java/com/ax/template/authblueprint/valuationrun/ValuationOutput.java"
  pattern: "Every recompute/rebase takes the subject's PESSIMISTIC_WRITE row lock (findByIdForUpdate) before reading the current head and writing version+1, and a uq(subject_id, run_version) backstop makes the loser of any residual race a deterministic 409 (CWE-362); a run records its as-of instant + basis + total as @Column(updatable=false) with @Version and NO public setter, so a correction is a new run, never an edit; the as-of read selects the run with the GREATEST as-of ≤ T (404 if none); a run fans out to ValuationOutput rows in the same transaction and conservation is checked TWICE — the run's persisted output_sum column carries a DB @Check that it equals the total, AND the service derives Σ a SECOND way via a repo SUM(value) query before commit, rejecting a disagreement with 422; a rebase appends a NEW baseline run with a forward rebasedFromRunVersion pointer while retaining prior runs verbatim, and resolving current follows the forward chain; NO delete path exists on the run"
upstream:
  - "https://www.law.cornell.edu/cfr/text/17/270.2a-4"
  - "https://www.law.cornell.edu/cfr/text/17/210.3-04"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "17 CFR § 270.2a-4(a)(1) (Rule 2a-4 under the Investment Company Act, Cornell LII) — the as-of valuation discipline: portfolio securities with readily available market quotations are valued at current market value, and the current net asset value is computed as of a time"
    url: "https://www.law.cornell.edu/cfr/text/17/270.2a-4"
    quote: "Portfolio securities with respect to which market quotations are readily available shall be valued at current market value"
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "17 CFR § 210.3-04 (Regulation S-X, Cornell LII) — the rebase-with-retained-history discipline: a retroactive adjustment restates the opening balance while prior periods remain disclosed"
    url: "https://www.law.cornell.edu/cfr/text/17/210.3-04"
    quote: "Also, state separately the adjustments to the balance at the beginning of the earliest period presented for items which were retroactively applied to periods prior to that period."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent recompute/rebase racing one subject's version counter)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-23"
---

## A valuation run is an as-of-pinned, immutable, conserving, rebase-with-history fact — not a mutable scalar you overwrite

**Impact: HIGH — an as-of read that returns the latest run reports a value that did not exist at T (a point-in-time break); a fan-out that does not conserve loses or invents value (and total := Σ outputs is a tautology); a rebase that rewrites the prior run destroys reconstructible history; an unserialized recompute writes two runs at one version (CWE-362).**

A *valuation run* is the versioned computation a portfolio/NAV system runs against a position book: it values a subject AS OF an instant, against a recorded basis, and projects the total down to per-position outputs. The discipline is regulatory: Rule 2a-4 requires *"Portfolio securities with respect to which market quotations are readily available shall be valued at current market value"* — the current net asset value computed *as of* a time, so a read for time T must return the run that was current at T, not a later one. The catalog versioned a governed decision (`decision-governance`) and a settlement run (`remeasurement-trueup`) but had no primitive for the as-of-pinned, conserving-fan-out, rebase-with-history valuation run:

```text
recompute(subject):  read current head under the subject's PESSIMISTIC_WRITE lock; append version+1
                     pinned to an as-of instant + recorded basis; fan out to N ValuationOutput rows;
                     uq(subject_id, run_version) = the exactly-once backstop
fan-out:             Σ output values MUST equal the run total — @Check on a persisted output_sum
                     column AND an INDEPENDENT repo SUM(value) cross-check (NOT total := Σ; tautology)
as-of read:          return the run with the GREATEST as-of ≤ T (404 if none) — never the latest
rebase(subject):     append a NEW baseline run with a forward rebasedFromRunVersion pointer; prior
                     runs retained VERBATIM; resolve current by following the forward chain
locks:               the subject row, PESSIMISTIC_WRITE — concurrent recompute/rebase → exactly one wins
```

**1. As-of point-in-time read (VALRUN-ASOF-001).** A run records its as-of instant, basis, version, and total as immutable columns. An as-of-T read returns the run with the greatest as-of ≤ T — a run that did not yet exist at T can never answer the query.

**2. Fan-out conserves, checked twice (VALRUN-FANOUT-001).** The run fans out to one output row per position; the persisted `output_sum` carries a DB `@Check` that it equals the total, AND the service derives Σ a SECOND way via a repo `SUM(value)` query before commit. A by-construction `total := Σ outputs` is tautological and proves nothing — the independent derivation is the netting/true-up lesson.

**3. Rebase appends a baseline, retains history (VALRUN-REBASE-001).** A rebase resets the basis and appends a NEW baseline run with a forward `rebasedFromRunVersion` pointer; every prior run stays readable verbatim at its own as-of, and resolving current follows the forward chain.

**Incorrect — a mutable scalar, a tautological sum, a rewriting rebase, an unsynchronized recompute:**

```java
public void revalue(UUID subjectId, BigDecimal total, Map<String, BigDecimal> positions) {
    Subject s = repo.findById(subjectId).orElseThrow();   // ❌ no row lock — two threads both read version n
    s.setTotal(total);                                    // ❌ public setter; overwrites the as-of fact in place
    s.setOutputSum(positions.values().stream()            // ❌ total := Σ outputs is a by-construction tautology —
        .reduce(BigDecimal.ZERO, BigDecimal::add));       //    it can NEVER detect a lost/invented position
    s.setBasis(newSplitRatioBasis());                     // ❌ rebase rewrites the prior run — history destroyed
    repo.save(s);                                         // ❌ both threads write version n+1 (CWE-362)
}
```

**Correct — immutable versioned run under the subject lock, independent conservation cross-check, rebase appends a baseline:**

```java
@Transactional
public ValuationRun recompute(UUID subjectId, int expectedHeadVersion, BigDecimal declaredTotal,
                              String basis, Map<String, BigDecimal> positions) {
    ValuationSubject subject = subjects.findByIdForUpdate(subjectId)               // ✅ PESSIMISTIC_WRITE
        .orElseThrow(ValuationRunException::notFound);
    if (subject.getHeadRunVersion() != expectedHeadVersion) {
        throw ValuationRunException.versionConflict();          // ✅ N concurrent recomputes → exactly one wins (409)
    }
    return appendVersion(subject, declaredTotal, basis, positions, null);          // forward pointer null on recompute
}

@Transactional
public ValuationRun rebase(UUID subjectId, int fromRunVersion, BigDecimal declaredTotal,
                           String newBasis, Map<String, BigDecimal> positions) {
    ValuationSubject subject = subjects.findByIdForUpdate(subjectId)
        .orElseThrow(ValuationRunException::notFound);
    if (subject.getHeadRunVersion() != fromRunVersion) {
        throw ValuationRunException.notCurrent();               // ✅ rebase only from the current head — chain stays linear
    }
    return appendVersion(subject, declaredTotal, newBasis, positions, fromRunVersion); // ✅ NEW baseline, prior retained
}

private ValuationRun appendVersion(ValuationSubject subject, BigDecimal declaredTotal, String basis,
                                   Map<String, BigDecimal> positions, Integer rebasedFrom) {
    int nextVersion = subject.getHeadRunVersion() + 1;
    Instant now = Instant.now(clock);
    ValuationRun run;
    try {
        run = runs.saveAndFlush(new ValuationRun(UUID.randomUUID(), subject.getId(), nextVersion,
            now, basis, declaredTotal, declaredTotal, rebasedFrom, now));  // total + output_sum (== DB @Check)
    } catch (DataIntegrityViolationException dup) {
        throw ValuationRunException.versionConflict();          // ✅ uq(subject,version) loser → 409 (CWE-362)
    }
    for (Map.Entry<String, BigDecimal> e : positions.entrySet()) {
        members.persist(new ValuationOutput(UUID.randomUUID(), run.getId(), e.getKey(), e.getValue()));
    }
    BigDecimal crossCheck = runs.sumOutputValues(run.getId());  // ✅ INDEPENDENT repo-SUM, a different code path
    if (crossCheck.compareTo(run.getTotalValue()) != 0) {
        throw ValuationRunException.fanOutNotConserved();       // ✅ 422; tx rolls back → partial fan-out unrepresentable
    }
    subject.advanceHead(run.getRunVersion());
    return run;
}
```

The subject-row PESSIMISTIC_WRITE lock serializes the read-version / write-next-version sequence; the `uq(subject_id, run_version)` index is the suspenders for any residual race (CWE-362). Conservation is derived twice — a DB `@Check` on the persisted `output_sum` column AND an independent repo `SUM(value)` — so a tautological equality can never pass for a real check. A rebase appends a baseline with a forward pointer and never rewrites a prior run. `ValuationOutput` rows are `@AggregateMember` of `ValuationRun` — root-JPQL reads, `common/MemberWriter` writes; no delete path exists.

Verification: review-tier — confirm the as-of read selects the greatest as-of ≤ T (404 if none), the run columns are `@Column(updatable=false)` with `@Version` and no public setter, the fan-out conservation is checked both by the DB `@Check` and an independent repo SUM before commit, the rebase appends a forward-pointing baseline retaining prior runs, and every write path takes the subject's PESSIMISTIC_WRITE lock. The behavioural proof a fork-receiver keeps green: the N-thread recompute race (exactly one 2xx + N-1 409, exactly one run at the next version).

Reference: [17 CFR § 270.2a-4 (Rule 2a-4 — current net asset value)](https://www.law.cornell.edu/cfr/text/17/270.2a-4)

Reference: [17 CFR § 210.3-04 (Regulation S-X — retroactive adjustment of opening balance)](https://www.law.cornell.edu/cfr/text/17/210.3-04)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
