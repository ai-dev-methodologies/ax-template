---
title: Which fields of an aggregate are mutable must be a function of its CURRENT STATE — a DECLARED per-(state,field) authority table (never a blanket "editable while not terminal", never an if-scatter), monotonically tightened by forward transitions with widening only through a RECORDED governed re-open, and re-checked under the row's PESSIMISTIC_WRITE lock so a concurrent state advance cannot let a stale-state edit through
impact: HIGH
impactDescription: "A 'editable while not terminal' blanket lets a reviewer rewrite the BODY of a SUBMITTED regulatory filing that is supposed to be frozen, or edit an APPROVED batch record that must be read-only — a control failure a regulator can sanction. An if-scatter authority (field checks sprinkled through the edit path) drifts: one path forgets the freeze and the field leaks through. And a field edit that checks the state the CALLER observed, not the state under the row lock, lets a title-edit race a concurrent SUBMIT and land AFTER the field was frozen (CWE-367 time-of-check/time-of-use) — the record ends with a value written into a state that forbids it"
tags:
  - state-machine
  - authorization
  - audit
  - concurrency
  - governance
spec_ref: "specs/state-conditional-mutability-l0.yaml#STATEMUTATION-AUTHORITY-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/statemutation/StateFieldPolicy.java + backend/src/main/java/com/ax/template/authblueprint/statemutation/StateMutationService.java + backend/src/main/java/com/ax/template/authblueprint/statemutation/GovernedForm.java + backend/src/main/java/com/ax/template/authblueprint/statemutation/GovernedFormStateMachine.java"
  pattern: "The mutable-field-set is a DECLARED EnumMap<FormState, Set<FormField>> (StateFieldPolicy) the service looks up by the form's current state — NOT an if-scatter; the SAME table the GET surfaces (mutableFields) is the one the edit path enforces; an edit of a field not in the current state's mutable-set is a 409 FIELD_LOCKED_IN_STATE naming the field + state; the table tightens monotonically DRAFT ⊇ SUBMITTED ⊇ APPROVED ⊇ LOCKED (asserted), widening is an explicit recorded REOPEN governed transition through GovernedFormStateMachine (the sole status mutator) appending an immutable FormTransition, and a LOCKED form is terminal; the edit takes the form's PESSIMISTIC_WRITE row lock and re-checks the authority against the state UNDER the lock so a concurrent advance makes the racing edit 409 rather than a stale-state write (CWE-367)"
upstream:
  - "https://csrc.nist.gov/glossary/term/access_control"
  - "https://cwe.mitre.org/data/definitions/367.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
  - "https://www.rfc-editor.org/rfc/rfc9457"
  - "https://httpwg.org/specs/rfc9110.html"
evidence:
  - source_type: external
    citation: "NIST Computer Security Resource Center Glossary — 'access control' (FIPS 201-3, sourced from CNSSI 4009-2015): an authorization decision granting or denying specific requests; here the request is a field mutation and the deciding attribute is the resource's STATE"
    url: "https://csrc.nist.gov/glossary/term/access_control"
    quote: "The process of granting or denying specific requests to 1) obtain and use information and related information processing services and 2) enter specific physical facilities (e.g., federal buildings, military establishments, border crossing entrances)."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition — MITRE (a field edit that checks the state the caller observed, then writes against a state a concurrent transition has already advanced)"
    url: "https://cwe.mitre.org/data/definitions/367.html"
    quote: "The product checks the state of a resource before using that resource, but the resource's state can change between the check and the use in a way that invalidates the results of the check."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (a concurrent field-edit and state-advance racing one form row)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "RFC 9457 §1 (Problem Details for HTTP APIs) — a FIELD_LOCKED_IN_STATE rejection is returned as a machine-readable problem+json carrying the offending field + state"
    url: "https://www.rfc-editor.org/rfc/rfc9457"
    quote: "This document defines a \"problem detail\" to carry machine-readable details of errors in HTTP response content"
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "RFC 9110 §15.5.10 (HTTP Semantics) — 409 Conflict, the status for a field edit that conflicts with the form's current state"
    url: "https://httpwg.org/specs/rfc9110.html"
    quote: "The 409 (Conflict) status code indicates that the request could not be completed due to a conflict with the current state of the target resource."
    quoted_at: "2026-06-23"
---

## Mutation authority is per-(state,field), declared as a table — not a blanket "editable while not terminal"

**Impact: HIGH — a blanket freeze lets a SUBMITTED filing's body be rewritten; an if-scatter authority drifts a field through a state that should freeze it; an edit that checks the observed (not the under-lock) state lands a write after a concurrent transition froze the field (CWE-367).**

Which FIELDS of an aggregate you may mutate is a function of its CURRENT STATE — and that function is an authorization decision (NIST: *"the process of granting or denying specific requests"*). A `GovernedForm` walks `DRAFT → SUBMITTED → APPROVED → LOCKED`. The mutable field-set is NOT "everything while not terminal": in `DRAFT` all editable fields are mutable; in `SUBMITTED` only a declared subset (the reviewer note) is; in `APPROVED`/`LOCKED` none are. The catalog governs lifecycle STATUS (`approvalworkflow`, `dunning`) and four-eyes SIGN-OFF (`authorization-parity`) but had no primitive for state-conditional FIELD-level authority:

```text
edit(form, field, value):  look up StateFieldPolicy.mutableFields(form.state);
                           field ∉ that set → 409 FIELD_LOCKED_IN_STATE (names field + state)
declared table:            EnumMap<FormState, Set<FormField>> — the ONE place the authority lives;
                           the GET surfaces it (mutableFields) AND the edit path enforces it — same object
monotone:                  DRAFT ⊇ SUBMITTED ⊇ APPROVED ⊇ LOCKED — forward transitions only SHRINK
widening:                  re-open = an explicit RECORDED governed transition (reason + actor), never a silent unlock
lock:                      the form row, PESSIMISTIC_WRITE — the edit re-checks state UNDER the lock (CWE-367)
```

**1. The authority is a DECLARED table, not an if-scatter (STATEMUTATION-AUTHORITY/DECLARED-001).** The per-state mutable-set is a single `EnumMap<FormState, Set<FormField>>` the service looks up by the form's current state. The same table the form's GET surfaces (`mutableFields`) is the one the edit path enforces — they cannot diverge. A field not in the current state's set is a 409 naming the field and the state.

**2. Forward transitions tighten monotonically; widening is recorded (STATEMUTATION-MONOTONE-001).** `DRAFT ⊇ SUBMITTED ⊇ APPROVED ⊇ LOCKED` — each forward step removes fields. Re-opening a tightened form (so frozen fields become editable again) is an explicit governed transition through the state machine, recorded as an immutable `FormTransition`. `LOCKED` is terminal.

**3. The edit re-checks state under the row lock (STATEMUTATION-TOCTOU-001).** The edit takes the form's `PESSIMISTIC_WRITE` lock and evaluates the field authority against the state that holds UNDER the lock — so a concurrent `SUBMIT` that froze the field makes a racing edit a deterministic 409, never a stale-state write.

**Incorrect — a blanket "editable while not terminal", an if-scatter authority, an unlocked check:**

```java
public void editField(UUID formId, String field, String value) {
    GovernedForm f = repo.findById(formId).orElseThrow();   // ❌ no row lock — checks the OBSERVED state (CWE-367)
    if (f.getState() == FormState.LOCKED) {                 // ❌ blanket "editable while not terminal":
        throw new IllegalStateException("locked");          //    SUBMITTED is allowed to edit the body — wrong
    }
    if (field.equals("title")) { f.setTitle(value); }       // ❌ if-scatter — one path forgets the per-state freeze
    else if (field.equals("body")) { f.setBody(value); }    //    and the field leaks through a state that froze it
    repo.save(f);
}
```

**Correct — a declared per-(state,field) table, looked up under the row lock, monotone, recorded widening:**

```java
// StateFieldPolicy — the ONE declared authority table; the GET surfaces it, the edit enforces it
public final class StateFieldPolicy {
    private static final Map<FormState, Set<FormField>> MUTABLE = new EnumMap<>(FormState.class);
    static {
        MUTABLE.put(FormState.DRAFT,     EnumSet.of(FormField.TITLE, FormField.BODY, FormField.REVIEWER_NOTE));
        MUTABLE.put(FormState.SUBMITTED, EnumSet.of(FormField.REVIEWER_NOTE));   // body/title frozen
        MUTABLE.put(FormState.APPROVED,  EnumSet.noneOf(FormField.class));        // read-only
        MUTABLE.put(FormState.LOCKED,    EnumSet.noneOf(FormField.class));        // terminal, read-only
    }
    public static Set<FormField> mutableFields(FormState state) {
        return MUTABLE.getOrDefault(state, EnumSet.noneOf(FormField.class));
    }
}

@Transactional
public GovernedForm editField(UUID formId, FormField field, String value) {
    GovernedForm f = forms.findByIdForUpdate(formId).orElseThrow(StateMutationException::notFound); // ✅ PESSIMISTIC_WRITE
    if (!StateFieldPolicy.mutableFields(f.getState()).contains(field)) {          // ✅ re-check UNDER the lock
        throw StateMutationException.fieldLocked(field, f.getState());            // 409 — names field + state (CWE-367)
    }
    f.applyEdit(field, value, Instant.now(clock));                               // ✅ package-private sole mutator
    return f;
}
```

The `PESSIMISTIC_WRITE` lock serializes the read-state / enforce / write sequence so the authority is evaluated against the state that actually holds; a concurrent `SUBMIT` between the caller's read and the edit makes the racing edit 409 rather than a stale-state write (CWE-367). `GovernedFormStateMachine` is the sole status mutator: it tightens forward along the declared graph and records a widening (re-open) as an immutable `FormTransition` (root-JPQL reads, `common/MemberWriter` writes); no delete path exists.

Verification: review-tier — confirm the mutable-field authority is a single declared `EnumMap` (the GET's `mutableFields` and the edit path consult the SAME table, no field-name if-scatter), the table tightens monotonically `DRAFT ⊇ SUBMITTED ⊇ APPROVED ⊇ LOCKED`, widening goes only through the recorded governed re-open, and the edit takes the `PESSIMISTIC_WRITE` lock and re-checks the state under it. The behavioural proof a fork-receiver keeps green: the concurrent submit-vs-edit race (the frozen field never accepts a write timestamped after the freeze).

Reference: [NIST CSRC Glossary — access control](https://csrc.nist.gov/glossary/term/access_control)

Reference: [CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition](https://cwe.mitre.org/data/definitions/367.html)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)

Reference: [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)

Reference: [RFC 9110 §15.5.10: 409 Conflict](https://httpwg.org/specs/rfc9110.html)
