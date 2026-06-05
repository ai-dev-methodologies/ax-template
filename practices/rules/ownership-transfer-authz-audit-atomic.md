---
title: An ownership reassignment MUST be initiator-authorized, written atomically all-or-nothing, and recorded in exactly one audit entry
impact: HIGH
impactDescription: "A reassignment any principal can trigger lets an attacker hand themselves another user's records; one that moves some rows then fails leaves a departing principal owning half their records and the successor the other half — a corrupt, half-orphaned state; one with no audit entry leaves a privileged ownership change with no trail. Authorization, atomicity, and audit are each load-bearing for a safe transfer."
tags:
  - ownership-transfer
  - authorization
  - atomicity
  - audit
  - access-control
spec_ref: "specs/ownership-transfer-l0.yaml#TRANSFER-ATOMIC-001"
verification:
  type: review
  source: "specs/ownership-transfer-l0.yaml#TRANSFER-ATOMIC-001"
  pattern: "Reassignment of all of a departing principal's rows MUST be ATOMIC — one @Transactional unit where either EVERY targeted row moves to the successor (and the audit entry is written) or, on any failure, NOTHING moves and the original ownership is fully intact; a partial handoff is forbidden and a failure surfaces 409 (TRANSFER-ATOMIC-001). Only an ADMIN or the current owner may initiate the transfer — any other principal gets 403 (least privilege); the named successor MUST be ACTIVE, same-tenant, and not the departing principal (inactive→409; foreign-tenant/not-found→404, IDOR-safe; access controls fail securely) (TRANSFER-AUTHZ-001). Every reassignment writes EXACTLY ONE audit record {from_owner, to_owner, actor=initiator (NOT from_owner), resource_type, count, timestamp} in the SAME transaction, with ids not treated as PII-in-the-clear (TRANSFER-AUDIT-001). Reject a transfer initiable by a non-owner non-admin, a partial/non-transactional reassignment, a successor that is inactive/foreign-tenant/the-leaver, and a reassignment with no (or a wrong-actor) audit record."
upstream:
  - "https://www.postgresql.org/docs/current/tutorial-transactions.html"
  - "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
evidence:
  - source_type: external
    citation: "PostgreSQL Documentation — Transactions (all-or-nothing)"
    url: "https://www.postgresql.org/docs/current/tutorial-transactions.html"
    quote: "The essential point of a transaction is that it bundles multiple steps into a single, all-or-nothing operation."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "PostgreSQL Documentation — Transactions (no partial effect on failure)"
    url: "https://www.postgresql.org/docs/current/tutorial-transactions.html"
    quote: "if some failure occurs that prevents the transaction from completing, then none of the steps affect the database at all."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 V4.1.3 — Access Control (least privilege)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that the principle of least privilege exists - users should only be able to access functions, data files, URLs, controllers, services, and other resources, for which they possess specific authorization."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 V4.1.5 — Access Control (fail securely)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that access controls fail securely including when an exception occurs."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## An ownership reassignment MUST be initiator-authorized, atomic all-or-nothing, and recorded in exactly one audit entry

**Impact: HIGH — When a principal is deprovisioned, their owned rows are reassigned to a successor (the `owner-deprovision-reassigns-not-orphans` rule covers WHERE they go). Three properties make that reassignment safe. Authorization: only an admin or the current owner may trigger it — ASVS demands *the principle of least privilege ... users should only be able to access ... resources for which they possess specific authorization*, and that *access controls fail securely including when an exception occurs*. Atomicity: per PostgreSQL, *the essential point of a transaction is that it bundles multiple steps into a single, all-or-nothing operation*, and *if some failure occurs ... then none of the steps affect the database at all* — so a transfer never half-completes. Audit: a privileged ownership change leaves exactly one trail entry. Miss any one and a transfer becomes a privilege-escalation, a corruption, or an untraceable change.**

There are three load-bearing requirements — the AUTHZ/AUDIT/ATOMIC items of `specs/ownership-transfer-l0.yaml` (composing `TRANSFER-REASSIGN-001`, the existing `owner-deprovision-reassigns-not-orphans` rule).

**1. Atomic, all-or-nothing (TRANSFER-ATOMIC-001).** The reassignment runs in ONE `@Transactional` unit: either every targeted row moves to the successor AND the audit entry is written, or on any failure nothing moves and the original ownership is fully intact. A partial handoff (rows split between leaver and successor) is forbidden; a failure surfaces 409, never a committed half-state.

**2. Initiator authorization + successor eligibility (TRANSFER-AUTHZ-001).** Only an ADMIN or the current owner may initiate — any other principal gets 403 (least privilege). The named successor MUST be ACTIVE, the same tenant, and not the departing principal. Failures fail securely: an inactive successor → 409; a foreign-tenant or non-existent successor → 404 (IDOR-safe — does not leak existence).

**3. Exactly one audit record, correct actor (TRANSFER-AUDIT-001).** The transfer writes EXACTLY ONE audit record `{from_owner, to_owner, actor, resource_type, count, timestamp}` in the SAME transaction. The `actor` is the INITIATOR (the admin/owner who triggered it), NOT `from_owner` — conflating them hides who acted. Identifiers are handled per the audit-PII discipline, not logged in the clear.

**Incorrect — any caller initiates; rows moved row-by-row (non-atomic); no audit:**

```java
public void transfer(Long fromOwner, Long toOwner) {       // VIOLATION: no initiator authz check (TRANSFER-AUTHZ-001)
    for (Doc d : docRepo.findByOwner(fromOwner)) {
        d.setOwner(toOwner);
        docRepo.save(d);                                    // VIOLATION: per-row commit → a mid-loop failure half-transfers (ATOMIC)
    }
    // VIOLATION: no audit record of who reassigned what (TRANSFER-AUDIT-001)
}
```

**Correct — admin/owner-gated, successor-validated, one transaction, one audit entry with the initiator as actor:**

```java
@Transactional
public TransferResult transfer(Long fromOwner, Long toOwner, Principal initiator) {
    authz.requireAdminOrOwner(initiator, fromOwner);       // 403 otherwise (TRANSFER-AUTHZ-001, least privilege)
    User successor = users.findActiveSameTenant(toOwner)   // inactive→409, foreign/absent→404 IDOR-safe (fail securely)
        .orElseThrow(() -> new SuccessorIneligible(toOwner));
    int moved = docRepo.reassignAll(fromOwner, toOwner);   // all rows in ONE statement/txn (TRANSFER-ATOMIC-001)
    auditLog.record(new OwnershipTransfer(fromOwner, toOwner,
        initiator.id(), "Doc", moved, clock.now()));       // exactly one record, actor=initiator (TRANSFER-AUDIT-001)
    return new TransferResult(moved);                      // commit together; any failure → nothing moved, 409
}
```

Verification: review-tier. Transfer safety is an authz + atomicity + audit property with no compile-time signal — a per-row, unauthorized, unaudited transfer compiles and works on the happy path while being a privilege-escalation and corruption risk. Verify by review against `specs/ownership-transfer-l0.yaml`: only admin/owner initiates (403 else); successor is active/same-tenant/not-leaver with fail-secure status codes; all rows reassign in one transaction (no partial); exactly one audit record with actor=initiator. When a fork-receiver wires real ITs (non-owner transfer → 403; forced mid-transfer failure leaves original ownership intact; one audit row written), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [PostgreSQL — Transactions](https://www.postgresql.org/docs/current/tutorial-transactions.html)

Reference: [OWASP ASVS V4 — Access Control](https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md)
