---
title: Deprovisioning a record owner MUST reassign their rows to a named successor — never orphan them
impact: HIGH
impactDescription: "When a principal is offboarded, any row that still points at them via owner_id / created_by / assignee becomes unreachable: no live owner can list, edit, or govern it, and it silently evades owner-scoped authorization and retention sweeps. The records do not disappear — they become un-owned liabilities."
tags:
  - ownership-transfer
  - offboarding
  - account-lifecycle
  - authz
  - data-integrity
spec_ref: "specs/ownership-transfer-l0.yaml#TRANSFER-REASSIGN-001"
verification:
  type: review
  source: "specs/ownership-transfer-l0.yaml#TRANSFER-REASSIGN-001"
  pattern: "Deprovision handler reassigns every owner_id == departing-principal row to an explicitly-named active successor in one @Transactional; a request without a successor is rejected (400 TRANSFER_SUCCESSOR_REQUIRED); post-transfer zero rows remain owned by the departed principal. owner_id is never nulled and never left pointing at the inactive principal."
upstream:
  - "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-2/"
  - "https://github.com/OWASP/ASVS/blob/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
evidence:
  - source_type: external
    citation: "NIST SP 800-53 Rev.5 — AC-2 Account Management (Discussion: account management aligned with personnel transfer/termination)"
    url: "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-2/"
    quote: "Align account management processes with personnel termination and transfer processes."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 — V4.1.3 General Access Control Design (principle of least privilege)"
    url: "https://github.com/OWASP/ASVS/blob/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that the principle of least privilege exists - users should only be able to access functions, data files, URLs, controllers, services, and other resources, for which they possess specific authorization."
    quoted_at: "2026-06-01"
---

## Deprovisioning a record owner MUST reassign their rows to a named successor — never orphan them

**Impact: HIGH — orphaned-on-departure rows are silent, unreachable liabilities**

Many domains attach a record to the principal who owns it: a course to its
author, an approval line to its manager, a ticket to its agent, a listing to
its seller. The owner reference (`owner_id` / `created_by` / `assignee`) is
what owner-scoped authorization, owner-filtered list queries, and ownership
retention sweeps all key on. When that principal is offboarded, deactivated,
or moved out of the tenant, the records they owned do not move with them.
If the deprovision path forgets them, they are left pointing at a principal
who can no longer act — un-owned, unreachable, and outside every governance
loop that assumed a live owner.

The two tempting wrong fixes are both defects. **Nulling `owner_id`** turns
the rows into genuine orphans that no owner-scoped query will ever surface.
**Leaving `owner_id` pointing at the now-inactive principal** keeps the rows
out of any active owner's reach and breaks owner-based authorization (the
gate evaluates against a principal who is gone). The correct handoff is a
*reassignment to a named, active successor*: the caller MUST say where the
records go, and the server moves all of them in one transaction.

NIST SP 800-53 AC-2 ties account lifecycle directly to personnel transfer —
account management must be aligned with the transfer process, not an
afterthought triggered later. The least-privilege principle (ASVS V4.1.3)
adds the second constraint: the successor inherits exactly the records being
handed off, named explicitly, never a wildcard sweep that widens their
access beyond the departing owner's set.

This rule is the REASSIGN keystone of `specs/ownership-transfer-l0.yaml`
(`backend_only`). It is distinct from data-subject-rights: there the leaver
is the data *subject* exercising rights over *their own* personal data; here
the leaver is the record *owner* and the obligation is to keep the business
records they owned reachable under a successor.

**Incorrect — deprovision nulls the owner (orphans) or leaves the dead owner in place:**

```java
// ❌ Orphans every owned row — no live owner-scoped query will find them again.
@Transactional
public void deprovision(UUID departingId) {
    courseRepo.clearOwner(departingId);   // UPDATE course SET owner_id = NULL WHERE owner_id = ?
    // approvals, tickets, listings owned by departingId are simply forgotten.
}

// ❌ Or: deactivate the principal but leave owner_id pointing at them.
// The rows now belong to a principal who can never act on them again,
// and owner-based @PreAuthorize evaluates against a ghost.
principalRepo.deactivate(departingId);    // owned rows untouched
```

**Correct — reassign every owned row to a named, active successor in one transaction:**

```java
@Transactional
public OwnershipTransferResult deprovision(UUID departingId, UUID successorId, Principal actor) {
    // 1. A successor MUST be named — refuse to proceed without one.
    if (successorId == null) {
        throw new OwnershipTransferException(SUCCESSOR_REQUIRED); // -> 400 TRANSFER_SUCCESSOR_REQUIRED
    }
    // 2. Successor must be active + same tenant + not the leaver (TRANSFER-AUTHZ-001).
    Principal successor = principalRepo.findActiveInTenant(successorId, tenantOf(departingId))
        .orElseThrow(() -> new OwnershipTransferException(SUCCESSOR_INACTIVE_OR_FOREIGN));
    if (successor.id().equals(departingId)) {
        throw new OwnershipTransferException(SUCCESSOR_IS_LEAVER);
    }
    // 3. Reassign ALL owned rows across every owning module — one transaction.
    int moved = 0;
    moved += courseRepo.reassignOwner(departingId, successorId);
    moved += approvalRepo.reassignOwner(departingId, successorId);
    moved += listingRepo.reassignOwner(departingId, successorId);
    // 4. Audit inside the SAME transaction (from/to/actor/count) — never PII.
    auditLog.record("OWNERSHIP_TRANSFER", Map.of(
        "from_owner", departingId.toString(),
        "to_owner",   successorId.toString(),
        "actor",      actor.getName(),
        "count",      moved));
    // 5. Invariant: zero rows may remain owned by the departed principal.
    assert courseRepo.countByOwner(departingId) == 0;
    return new OwnershipTransferResult(departingId, successorId, moved);
}
```

Verification: review that the deprovision handler reassigns every
`owner_id == departing-principal` row to an explicitly-named active successor
in one `@Transactional`, rejects a successor-less request with 400, and
leaves zero rows owned by the departed principal — `owner_id` is never nulled
and never left on the inactive principal (`specs/ownership-transfer-l0.yaml#TRANSFER-REASSIGN-001`).

Reference: [NIST SP 800-53 Rev.5 — AC-2 Account Management](https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-2/)
Reference: [OWASP ASVS v4.0.3 — V4.1 Access Control](https://github.com/OWASP/ASVS/blob/v4.0.3/4.0/en/0x12-V4-Access-Control.md)
