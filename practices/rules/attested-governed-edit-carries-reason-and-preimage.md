---
title: An edit to governed data must be an attested change — atomically recording who / when (injected clock) / old → new / a mandatory non-blank reason at the sole mutator, appended to a per-field history that never obscures a prior value
impact: HIGH
impactDescription: "Mutating a governed field with a plain setter loses who/when/why and overwrites the prior value — the record is no longer attributable, reconstructable, or audit-defensible (21 CFR 11 / ALCOA); a blank-reason edit that still commits, or a history row that can be UPDATEd, silently destroys the evidentiary chain"
tags:
  - audit
  - data-integrity
  - immutability
  - provenance
spec_ref: "specs/attested-change-record-l0.yaml#ACR-ENVELOPE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/governedrecord/GovernedRecordService.java + backend/src/main/java/com/ax/template/authblueprint/governedrecord/ChangeRecord.java"
  pattern: "A governed field is mutable ONLY through a service method that, in one transaction, reads the current value as oldValue under the row lock, requires a non-blank reason (else 422 before the value changes), and appends an immutable ChangeRecord{actor=Authentication principal, occurredAt=Instant.now(clock), entityRef, fieldName, oldValue, newValue, reason, monotonic sequence}; the change row's value/metadata columns are @Column(updatable=false) with no setter; no public setter on the governed field is reachable from a controller"
upstream:
  - "https://www.law.cornell.edu/cfr/text/21/11.10"
  - "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html"
evidence:
  - source_type: external
    citation: "FDA 21 CFR Part 11 §11.10(e) — Controls for closed systems (audit trail of operator entries/actions that create, modify, or delete electronic records)"
    url: "https://www.law.cornell.edu/cfr/text/21/11.10"
    quote: "Use of secure, computer-generated, time-stamped audit trails to independently record the date and time of operator entries and actions that create, modify, or delete electronic records."
    quoted_at: "2026-06-07"
  - source_type: external
    citation: "FDA 21 CFR Part 11 §11.10(e) — record changes must not obscure prior information (append-only audit principle)"
    url: "https://www.law.cornell.edu/cfr/text/21/11.10"
    quote: "Record changes shall not obscure previously recorded information."
    quoted_at: "2026-06-07"
  - source_type: external
    citation: "java.time.Clock — Java SE 21 API documentation (Oracle): inject a Clock for the current instant"
    url: "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html"
    quote: "Best practice for applications is to pass a Clock into any method that requires the current instant and time-zone. A dependency injection framework is one way to achieve this:"
    quoted_at: "2026-06-01"
---

## An edit to governed data must be an attested change, not a setter

**Impact: HIGH — a plain setter on a governed field loses who/when/why and overwrites the prior value; the record stops being attributable, reconstructable, and audit-defensible (21 CFR 11 / ALCOA).**

*Governed data* is any field an auditor, regulator, or reconciliation can later question: a clinical eCRF value, a ledger line correction, a payroll figure restated after the run, an EMR vital amended after sign-off, a regulated manufacturing batch parameter. For such a field, "change it" is not `entity.setValue(x)`. The regulation is explicit: keep *"secure, computer-generated, time-stamped audit trails to independently record the date and time of operator entries and actions that create, modify, or delete electronic records,"* and *"record changes shall not obscure previously recorded information."* That is an **attested change**: every edit atomically records WHO, WHEN, the OLD value, the NEW value, and a mandatory REASON, appended to a history that never overwrites what came before.

The catalog already reasons about provenance — but only on **escape-hatch** paths: a reason on break-glass and DSR-rectify and sealed-period-reopen, an old/new pair on a fraud-override, a prior input on `value-provenance`, a text pre-image on a comment edit. None of these is the *default* contract for an ordinary governed edit. This rule generalizes them: the reasoned, attributed, append-only edit is the NORMAL path, not the exception.

Three defects recur, and one rule closes them.

**Defect 1 — a plain setter loses attribution and the reason.** `datum.setValue("12.4"); repo.save(datum)` records nothing: not who changed it, not when, not from what, not why. The prior value is gone. An auditor cannot answer "who changed this lab value from 11.8 to 12.4 and why," which is the entire point of 21 CFR 11.10(e). The field must be mutable ONLY through a service that writes the change record in the same transaction.

**Defect 2 — a blank-reason edit that still commits.** Capturing who/when/old/new but letting `reason` be null/blank produces an unexplained change — the "why" the regulation and every CAPA investigation needs. The reason is mandatory: a blank reason is rejected `422 ATTESTED_REASON_REQUIRED` BEFORE the value changes, so there is never a committed-but-unexplained edit.

**Defect 3 — a mutable / overwriting history.** If the change row can be UPDATEd, or the edit overwrites the field without appending, prior information is obscured — forbidden by *"record changes shall not obscure previously recorded information."* Change records are append-only (`@Column(updatable=false)`, no setter), carry a monotonic per-(entity,field) sequence for unambiguous order, and a correction is a NEW appended record, never an edit of an old one.

**Incorrect — a plain setter: no who/when/why, prior value overwritten:**

```java
public void updateLabValue(UUID id, String newValue) {
    Datum d = repo.findById(id).orElseThrow();
    d.setValue(newValue);     // ❌ who? when? from what? why? — all lost; prior value gone
    repo.save(d);             // ❌ no audit trail; obscures previously recorded information
}
```

**Correct — attested change: atomic who/when/old/new/reason, append-only, reason-gated:**

```java
@Transactional
public Datum changeValue(UUID id, String newValue, String reason, String actor) {
    if (reason == null || reason.isBlank()) {
        throw AttestedException.reasonRequired();          // ✅ 422 BEFORE the value changes
    }
    Datum d = repo.findByIdForUpdate(id).orElseThrow();    // lock: oldValue read == pre-edit value
    String oldValue = d.getValue();
    long seq = changeRepo.nextSequence(id);                // monotonic per (entity, field), under the lock
    changeRepo.save(new ChangeRecord(UUID.randomUUID(), id, "value", seq,
        oldValue, newValue, reason, actor, Instant.now(clock)));  // ✅ who/when(clock)/old/new/why, immutable
    d.setValueInternal(newValue);                          // package-private; no public setter on the field
    return d;
}
```

This is the shape the governedrecord reference workload runs: the governed field has no public setter, `changeValue` is the sole mutator, the reason is mandatory (422 if blank, before any change), the `occurredAt` is `Instant.now(clock)` from an injected `Clock` (server-owned, never a client timestamp — *"pass a Clock into any method that requires the current instant"*), and the `ChangeRecord` is append-only and immutable with a monotonic per-field sequence so the audit chain reconstructs the field's value at any past instant and never obscures a prior value.

Verification: review-tier — confirm the governed field is mutated ONLY through the change-recording service (no public setter reachable from a controller), the reason is required (422 before the edit when blank), the change row is `@Column(updatable=false)` with no setter, and `occurredAt` is read from an injected `Clock`. The canonical proof a fork-receiver writes: edit a governed field with a reason → 200 and a change row whose oldValue/newValue/reason match; edit with a blank reason → 422 and the value is unchanged; N edits → N immutable rows whose old→new links chain unbroken from the original value.

Reference: [FDA 21 CFR Part 11 §11.10(e) — audit trails for closed systems](https://www.law.cornell.edu/cfr/text/21/11.10)

Reference: [java.time.Clock — Java SE 21 API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html)
