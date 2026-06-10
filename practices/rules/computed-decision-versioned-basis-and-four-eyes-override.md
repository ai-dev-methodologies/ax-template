---
title: A computed decision (quote / rate / score / eligibility) must snapshot its appraisal-sufficient basis immutably, re-determine only by appending a reasoned NEW version (never overwrite), and gate a manual override behind a justification plus a four-eyes approver distinct from the requester — DB-backstopped via @Check (approved_by <> decided_by)
impact: HIGH
impactDescription: "A decision without its basis snapshot cannot be appraised or defended after a dispute (the rate table has moved on); overwriting a determination on recompute destroys what was actually decided and when; an override without a distinct approver lets one actor both request and authorize the deviation — the exact single-actor misuse separation-of-duty exists to prevent — and an un-backstopped four-eyes check silently dies the first time a code path forgets it"
tags:
  - audit
  - state-machine
  - four-eyes
  - versioning
  - governance
spec_ref: "specs/decision-governance-l0.yaml#DG-BASIS-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/decisiongov/DecisionService.java + backend/src/main/java/com/ax/template/authblueprint/decisiongov/DecisionVersion.java"
  pattern: "Every decision version row persists its basis snapshot in the SAME transaction, all version columns @Column(updatable=false) with no public setter (append-only); recompute acquires the scope row under PESSIMISTIC_WRITE, appends version = current + 1 with a mandatory non-blank reason (422 DECISION_REASON_REQUIRED before any change), never UPDATEs a committed version; a manual override additionally requires approved_by NON-NULL and <> decided_by (422 DECISION_FOUR_EYES_REQUIRED) and the entity carries @Check ((kind <> 'OVERRIDE') OR (approved_by IS NOT NULL AND approved_by <> decided_by)) plus UNIQUE(scope_id, version_no) so the gate and the chain hold even under ddl-auto"
upstream:
  - "http://www.actuarialstandardsboard.org/asops/actuarial-communications/"
  - "https://csrc.nist.gov/glossary/term/separation_of_duty"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "ASOP No. 41 — Actuarial Communications, §3.2 Actuarial Report (the canonical decision-basis-documentation standard: findings + methods/assumptions/data, appraisal-sufficient)"
    url: "http://www.actuarialstandardsboard.org/asops/actuarial-communications/"
    quote: "the actuary should state the actuarial findings, and identify the methods, procedures, assumptions, and data used by the actuary with sufficient clarity that another actuary qualified in the same practice area could make an objective appraisal of the reasonableness of the actuary's work"
    quoted_at: "2026-06-10"
  - source_type: external
    citation: "NIST SP 800-192 — Separation of Duty (SOD), via the NIST CSRC glossary (the four-eyes anchor: requester and authorizer must be different people)"
    url: "https://csrc.nist.gov/glossary/term/separation_of_duty"
    quote: "No user should be given enough privileges to misuse the system on their own. For example, the person authorizing a paycheck should not also be the one who can prepare them."
    quoted_at: "2026-06-10"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent re-determinations racing the version counter)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-01"
---

## A computed decision carries its basis, re-determines by appending a reasoned version, and is overridden only under four eyes

**Impact: HIGH — without the basis snapshot the determination cannot be appraised after the inputs move on; overwrite-on-recompute destroys the decision history; a self-approved override is the single-actor misuse separation-of-duty exists to prevent.**

A *governed computed decision* — an insurance premium quote, a credit limit, a price/rate calculation, a risk score, an eligibility verdict — is a VALUE the system determined from inputs at a moment in time. Three properties keep it defensible, and the catalog's existing primitives cover none of them as a unit (`attested-change-record` governs field edits on a datum; `approval-workflow` routes a request through approvers — neither versions a computed value nor pins its basis):

```text
decide(scope, basis, outcome):     version 1, basis snapshotted IMMUTABLY in the same tx
recompute(scope, basis', reason):  version = current + 1 (NEW row, own basis, reason REQUIRED)
override(scope, outcome', reason, approver):
                                   version + 1, kind=OVERRIDE, approver REQUIRED and ≠ actor
invariant (DB):  UNIQUE(scope_id, version_no)
                 CHECK ((kind <> 'OVERRIDE') OR (approved_by IS NOT NULL AND approved_by <> decided_by))
```

**1. Appraisal-sufficient basis, snapshotted immutably (DG-BASIS-001).** ASOP No. 41 §3.2 states the bar: the record must *"identify the methods, procedures, assumptions, and data used … with sufficient clarity that another actuary qualified in the same practice area could make an objective appraisal of the reasonableness of the … work."* Persist the basis (inputs + assumptions + method/rate-table version) in the SAME transaction as the decision, `@Column(updatable=false)`, no setter. A later dispute is answered from the row, not from "what the rate table probably was."

**2. Re-determination appends; it never overwrites (DG-RECOMPUTE-001).** New inputs or a corrected table produce version `current + 1` with its OWN basis and a mandatory non-blank reason (422 before anything commits). The prior row is untouched — the chain preserves what was decided, on what basis, at every point in time. This is `attested-change-record`'s correction-by-append posture applied to whole determinations.

**3. Manual override = justification + four eyes, DB-backstopped (DG-OVERRIDE-001).** NIST's separation-of-duty definition is the whole argument: *"No user should be given enough privileges to misuse the system on their own. For example, the person authorizing a paycheck should not also be the one who can prepare them."* The override row records the justification, the basis it deviated FROM, and BOTH identities; `approved_by` must be present and different from `decided_by`. The inequality is also a `@Check` constraint, so a code path that forgets the gate fails at flush instead of committing a self-approved override. **Stated limit:** four-eyes here means the RECORDED approver identity differs from the requester's — it does NOT prove the approver is a distinct authenticated, authorized human (a fabricated name or a second self-owned account passes the inequality). A fork-receiver needing strong four-eyes resolves `approved_by` against the user store and asserts an approver role; the reference workload deliberately stops at the recorded-identity contract (fork-receiver autonomy).

**Incorrect — overwrite on recompute, self-approvable override, no basis:**

```java
@Transactional
public Decision requote(String scope, BigDecimal outcome) {
    Decision d = repo.findByScope(scope).orElseThrow();
    d.setOutcome(outcome);                       // ❌ overwrites the determination — history gone
    d.setUpdatedAt(now());                       // ❌ no basis snapshot, no reason, no version
    return repo.save(d);
}
public void override(String scope, BigDecimal outcome, String actor) {
    Decision d = repo.findByScope(scope).orElseThrow();
    d.setOutcome(outcome);                       // ❌ no justification, no approver — one actor
    repo.save(d);                                //    both requests and authorizes the deviation
}
```

**Correct — append-only versions under the scope lock; reasoned recompute; four-eyes override:**

```java
@Transactional
public DecisionVersion recompute(String scopeKey, String basisJson, String outcome,
                                 String reason, String actor) {
    requireNonBlank(reason, DecisionException::reasonRequired);          // 422 BEFORE any change
    DecisionScope s = scopes.findByScopeKeyForUpdate(scopeKey)           // ✅ row lock (CWE-362)
        .orElseThrow(DecisionException::notFound);
    int next = s.getCurrentVersion() + 1;
    DecisionVersion v = members.persist(new DecisionVersion(UUID.randomUUID(), s.getId(), next,
        DecisionKind.RECOMPUTED, basisJson, outcome, reason, actor, null, Instant.now(clock)));
    s.advanceVersion(next);                                              // root tracks the latest
    return v;                                                            // prior rows untouched
}

@Transactional
public DecisionVersion override(String scopeKey, String outcome, String reason,
                                String actor, String approver) {
    requireNonBlank(reason, DecisionException::reasonRequired);
    if (approver == null || approver.isBlank()
            || approver.strip().equals(actor.strip())) {                 // strip closes the
        throw DecisionException.fourEyesRequired();                      //   whitespace-padding bypass
    }
    DecisionScope s = scopes.findByScopeKeyForUpdate(scopeKey)
        .orElseThrow(DecisionException::notFound);
    DecisionVersion prior = scopes.findVersion(s.getId(), s.getCurrentVersion()).orElseThrow();
    int next = s.getCurrentVersion() + 1;
    DecisionVersion v = members.persist(new DecisionVersion(UUID.randomUUID(), s.getId(), next,
        DecisionKind.OVERRIDE, prior.getBasisJson(),                     // the basis deviated FROM
        outcome, reason, actor, approver, Instant.now(clock)));
    s.advanceVersion(next);
    return v;
}
// ✅ DB backstop (holds under ddl-auto):
//    @Check ((kind <> 'OVERRIDE') OR (approved_by IS NOT NULL AND approved_by <> decided_by))
//    + UNIQUE (scope_id, version_no)
```

The scope row lock serializes concurrent re-determinations so version slots are never lost or duplicated (CWE-362: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently"*); the root's `currentVersion` is the cheap latest pointer kept consistent under the same lock. Version rows are `@AggregateMember` of the scope root — reads are JPQL on the ROOT's repository and writes go through the shared `common/MemberWriter` seam (the AX-DDD-MEMBER-REPO end-state, applied from birth).

Verification: review-tier — confirm the basis column is immutable and written in the decision's transaction; recompute/override lock the scope row, append `current + 1`, and never UPDATE a committed version; the reason gate (422) precedes any change; the four-eyes inequality is enforced in code AND as the entity/migration `@Check`; `UNIQUE(scope_id, version_no)` exists. The behavioural proofs a fork-receiver keeps green: the concurrency test (N concurrent recomputes → N distinct consecutive versions) and the violation-proof (a native write of a self-approved OVERRIDE row → constraint violation).

Reference: [ASOP No. 41 — Actuarial Communications §3.2](http://www.actuarialstandardsboard.org/asops/actuarial-communications/)

Reference: [NIST CSRC Glossary — Separation of Duty (SP 800-192)](https://csrc.nist.gov/glossary/term/separation_of_duty)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
