---
title: An authorized action must bind its EXECUTION to the approved envelope by canonical parity hash (executed-matches-authorized), require TWO distinct human signoffs separated from the requester on the high-value path (four-eyes / NIST two-person rule), and refuse execution until every declared mandatory companion gate is recorded present (positive-gates) — all under the action's row lock so it executes exactly once
impact: HIGH
impactDescription: "When governance APPROVES one artifact but the system EXECUTES another — a wire approved for ₩1,000,000 released for ₩10,000,000, a build approved for staging deployed to prod, a procurement approved with a budget-check that never ran — the authorization is decorative and the loss is total. A single approver on a sensitive action is the maker-checker hole every dual-control standard exists to close; and a missing companion condition (no AML screen, no inventory reservation) makes the action executable in a state the policy forbade. Without the row lock two concurrent executes can both pass the gates and both fire the side effect (CWE-362)."
tags:
  - audit
  - state-machine
  - governance
  - authorization
  - concurrency
spec_ref: "specs/authorization-parity-l0.yaml#AUTHZPARITY-EXEC-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/authzparity/AuthorizationParityService.java + backend/src/main/java/com/ax/template/authblueprint/authzparity/AuthorizedAction.java + backend/src/main/java/com/ax/template/authblueprint/authzparity/ActionSignoff.java"
  pattern: "Authorizing an action records an immutable envelope (action type + EXACT authorized parameters + canonical SHA-256 parity hash over them + declared mandatory companion gate keys + high-value flag + requester, all @Column(updatable=false)); executing recomputes the parity hash from the ACTUAL execution parameters and on a MISMATCH refuses (409 PARITY_MISMATCH) while recording an immutable BlockedAttempt (offered<>authorized hash) — never moving to EXECUTED; a high-value action needs TWO DISTINCT approver signoffs each different from the requester (self → 422 SELF_SIGNOFF, duplicate → 422 DUPLICATE_SIGNOFF, fewer than two at execute → 422 INSUFFICIENT_SIGNOFFS; @Check approver_user_id <> requester_user_id backstop); execution is refused (422 MISSING_COMPANION_GATE) until every DECLARED gate key has a satisfaction record (an undeclared gate → 422 UNKNOWN_GATE); the execute path takes the action's PESSIMISTIC_WRITE row lock before the check-then-transition so exactly one of N concurrent executes wins (the rest 409 ALREADY_EXECUTED)"
upstream:
  - "https://csrc.nist.gov/glossary/term/separation_of_duty"
  - "https://csrc.nist.gov/glossary/term/dual_authorization"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "NIST Computer Security Resource Center glossary, 'Separation of Duty (SOD)' (source: NIST SP 800-192) — the separation-of-duty principle and the two-person rule as a dynamic SoD control"
    url: "https://csrc.nist.gov/glossary/term/separation_of_duty"
    quote: "refers to the principle that no user should be given enough privileges to misuse the system on their own. For example, the person authorizing a paycheck should not also be the one who can prepare them. Separation of duties can be enforced either statically (by defining conflicting roles, i.e., roles which cannot be executed by the same user) or dynamically (by enforcing the control at access time). An example of dynamic separation of duty is the two-person rule. The first user to execute a two-person operation can be any authorized user, whereas the second user can be any authorized user different from the first"
    quoted_at: "2026-06-22"
  - source_type: external
    citation: "NIST Computer Security Resource Center glossary, 'Dual Authorization' (source: NIST SP 800-172r3, adapted from NIST SP 800-53A Rev. 5) — the two-person control anchor for the four-eyes path"
    url: "https://csrc.nist.gov/glossary/term/dual_authorization"
    quote: "A system of storage and handling that is designed to prohibit individual access to certain resources by requiring the presence and actions of at least two authorized persons, each capable of detecting incorrect or unauthorized security procedures with respect to the task being performed."
    quoted_at: "2026-06-22"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition') — MITRE (concurrent execute calls racing one action's transition)"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource, but a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."
    quoted_at: "2026-06-22"
---

## The executed artifact must be the approved artifact; sensitive actions take two; mandatory companions must all be present

**Impact: HIGH — an approval that does not bind the executed parameters back to the approved envelope is decorative; a single approver on a sensitive action is the maker-checker hole; a missing companion condition lets the action fire in a forbidden state.**

Authorization that is not bound to execution is theatre. Governance approves a wire for ₩1,000,000, the executor releases ₩10,000,000; governance approves a deploy to staging, the pipeline targets prod; procurement approves a PO conditioned on a budget check that never ran. The catalog already *versions* a decision (`decision-governance`) and *sequences* signoff steps (`approval-workflow`), but neither binds the executed artifact to the approved envelope by content, requires TWO distinct approvers on the high-value path, or refuses execution until declared companion conditions are all present. Three controls close the gap:

```text
authorize(type, params, highValue, requiredGates):
                envelope RECORDS canonical params + SHA-256 parityHash + gate-key set (immutable)
execute(actualParams):  re-hash actualParams; offeredHash != parityHash → 409 PARITY_MISMATCH
                        + an immutable BlockedAttempt(offered, authorized) — NEVER EXECUTED
four-eyes:      highValue → need 2 DISTINCT approvers, each != requester
                self → 422 SELF_SIGNOFF · dup → 422 DUPLICATE_SIGNOFF · <2 at execute → 422
positive-gates: every DECLARED gate key needs a satisfaction → else 422 MISSING_COMPANION_GATE
                undeclared gate → 422 UNKNOWN_GATE
lock:           the action row, PESSIMISTIC_WRITE — check-then-transition is atomic (exactly once)
```

**1. Executed-matches-authorized (AUTHZPARITY-EXEC-001).** The envelope records the EXACT authorized parameters and a canonical SHA-256 over them. Executing recomputes the hash from the ACTUAL execution parameters; equality is structural — a substituted or escalated parameter changes the digest and is *unrepresentable-as-executed*. A mismatch is refused (409) and recorded as a `BlockedAttempt`, so the violation is loud, never silently dropped.

**2. Four-eyes is the two-person rule (AUTHZPARITY-FOUREYES-001).** NIST states the principle plainly: *"no user should be given enough privileges to misuse the system on their own"*, and the dynamic control — *"The first user to execute a two-person operation can be any authorized user, whereas the second user can be any authorized user different from the first."* A high-value action therefore needs TWO distinct approvers, each different from the requester. The `approver <> requester` separation is `@Check`-backstopped on the signoff row.

**3. Positive-gates (AUTHZPARITY-GATES-001).** The action declares its mandatory companion conditions at authorize time; execution is refused until EVERY declared gate has a satisfaction record. The gate set is a positive precondition (default-deny), not an after-the-fact audit.

**Incorrect — approve a flag, execute whatever was passed, one approver, no companions:**

```java
public void execute(UUID actionId, BigDecimal amount, String executor) {
    Action a = repo.findById(actionId).orElseThrow();
    if (a.isApproved()) {                 // ❌ "approved" is a bare boolean — not bound to amount
        wireGateway.release(amount);      // ❌ executes WHATEVER amount was passed in
        a.setStatus("EXECUTED");          // ❌ no row lock — two callers both release
    }                                     // ❌ one approver; no companion-gate check
}
```

**Correct — re-hash the execution params, enforce two distinct signoffs + every declared gate, under the row lock:**

```java
@Transactional
public AuthorizedAction execute(UUID actionId, Map<String, String> executionParams, String executor) {
    AuthorizedAction a = actions.findByIdForUpdate(actionId)        // ✅ PESSIMISTIC_WRITE row lock
        .orElseThrow(AuthorizationParityException::notFound);
    if (a.getStatus() == ActionStatus.EXECUTED) throw AuthorizationParityException.alreadyExecuted(); // 409
    // (1) executed-matches-authorized — a changed parameter changes the digest
    String offered = ParityHasher.hash(executionParams);
    if (!offered.equals(a.getParityHash())) {
        members.persistAndFlush(new BlockedAttempt(UUID.randomUUID(), actionId, offered,
            a.getParityHash(), executor, Instant.now(clock)));     // ✅ recorded, not dropped
        throw AuthorizationParityException.parityMismatch();        // 409 PARITY_MISMATCH
    }
    // (2) four-eyes — two DISTINCT approvers on the high-value path
    if (a.isHighValue()) {
        long distinct = actions.findSignoffs(actionId).stream()
            .map(ActionSignoff::getApproverUserId).distinct().count();
        if (distinct < 2) throw AuthorizationParityException.insufficientSignoffs();   // 422
    }
    // (3) positive-gates — every DECLARED companion must be present
    Set<String> satisfied = actions.findGates(actionId).stream()
        .map(GateSatisfaction::getGateKey).collect(Collectors.toSet());
    for (String required : a.getRequiredGates()) {
        if (!satisfied.contains(required)) throw AuthorizationParityException.missingCompanionGate(required); // 422
    }
    a.markExecuted(Instant.now(clock));                            // ✅ sole-mutator hook, under the lock
    return a;
}

@Transactional
public ActionSignoff signoff(UUID actionId, String approver) {
    AuthorizedAction a = actions.findByIdForUpdate(actionId).orElseThrow(AuthorizationParityException::notFound);
    if (approver.equals(a.getRequesterUserId())) throw AuthorizationParityException.selfSignoff();   // 422
    boolean dup = actions.findSignoffs(actionId).stream()
        .anyMatch(s -> s.getApproverUserId().equals(approver));
    if (dup) throw AuthorizationParityException.duplicateSignoff();                                    // 422
    return members.persistAndFlush(new ActionSignoff(UUID.randomUUID(), actionId, approver,
        a.getRequesterUserId(), Instant.now(clock)));              // ✅ @Check approver <> requester
}
```

The parity hash is the load-bearing primitive: a canonical SHA-256 (`ParityHasher`) over the sorted authorized parameters, recomputed at execute time — a structural equality the wire cannot fake. The row lock makes the all-gates-pass-then-transition sequence atomic, so exactly one of N concurrent executes wins (CWE-362: *"a timing window exists in which the shared resource can be modified by another code sequence operating concurrently."*). `ActionSignoff`, `GateSatisfaction` and `BlockedAttempt` are `@AggregateMember` of `AuthorizedAction` — root-JPQL reads, `common/MemberWriter` writes.

Verification: review-tier — confirm the envelope records canonical params + parity hash + gate-key set immutably; execute re-hashes the actual params and refuses + records a blocked attempt on mismatch; a high-value action needs two distinct approvers each different from the requester (self/duplicate rejected, `@Check approver <> requester` present); execution is refused until every declared gate is satisfied (undeclared gate rejected); the execute path takes the action's PESSIMISTIC_WRITE lock. The behavioural proofs a fork-receiver keeps green: the parity-mismatch block (409 + recorded attempt), the two-distinct-approvers requirement, the missing-companion-gate refusal, and the concurrent-execute keystone (exactly one EXECUTED).

Reference: [NIST CSRC glossary — Separation of Duty (SP 800-192)](https://csrc.nist.gov/glossary/term/separation_of_duty)

Reference: [NIST CSRC glossary — Dual Authorization](https://csrc.nist.gov/glossary/term/dual_authorization)

Reference: [CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization](https://cwe.mitre.org/data/definitions/362.html)
