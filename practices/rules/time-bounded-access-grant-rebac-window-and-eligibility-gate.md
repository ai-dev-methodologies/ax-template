---
title: A time-bounded relationship grant (ReBAC) must decide access by RECOMPUTING the window predicate over the injected Clock (now ∈ [validFrom, validUntil) AND ACTIVE) — never a stored 'expired' flag — be append-only + revocable (who/when recorded, no delete, fail-closed when revoked), and a multi-credential eligibility gate must pass ONLY when EVERY required credential class is held and non-expired at now (a single missing/expired class fails closed naming the class)
impact: HIGH
impactDescription: "A stored 'expired'/'active' boolean goes stale the instant the wall clock crosses validUntil with no writer to flip it — the grant keeps admitting a subject whose authorization has lapsed (NIST SP 800-53 AC-2 expiry-disable, AC-3 enforce-approved-authorizations); a half-open off-by-one that admits the instant equal to validUntil over-grants by one tick; an eligibility gate that passes on ANY held credential instead of requiring ALL required classes lets an un-licensed/un-insured subject act; and a delete path on grants destroys the revoke audit trail a regulator relies on"
tags:
  - authorization
  - access-control
  - time-bound
  - concurrency
  - governance
spec_ref: "specs/time-bounded-access-grant-l0.yaml#AGRANT-WINDOW-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/accessgrant/AccessGrant.java + backend/src/main/java/com/ax/template/authblueprint/accessgrant/AccessGrantService.java + backend/src/main/java/com/ax/template/authblueprint/accessgrant/Credential.java"
  pattern: "AccessGrant.isActiveAt(now) recomputes the verdict from (status, validFrom, validUntil) against the injected Clock — there is NO stored expired/active boolean column; access is allowed only while now ∈ [validFrom, validUntil) (half-open — the instant equal to validUntil is denied) AND status == ACTIVE; a check before the window is GRANT_NOT_YET_VALID, at/after is GRANT_EXPIRED, a revoked grant is GRANT_REVOKED regardless of the window; revoke records (revokedBy, revokedAt) write-once (idempotent — never overwriting a prior revoke) under the grant row's PESSIMISTIC_WRITE lock; there is NO delete path; AccessGrantService.requireEligible passes only when every required class is held by a credential whose Credential.isValidAt(now) is true, else throws CREDENTIAL_INELIGIBLE naming the first missing/expired class"
upstream:
  - "https://csrc.nist.gov/glossary/term/attribute_based_access_control"
  - "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-3/"
  - "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-2/"
evidence:
  - source_type: external
    citation: "NIST SP 800-162 Attribute Based Access Control, via the NIST CSRC glossary — the access decision is computed from attributes AND environment conditions at the time of access (a validity window is an environment condition the policy evaluates, not a stored verdict)"
    url: "https://csrc.nist.gov/glossary/term/attribute_based_access_control"
    quote: "An access control method where subject requests to perform operations on objects are granted or denied based on assigned attributes of the subject, assigned attributes of the object, environment conditions, and a set of policies that are specified in terms of those attributes and conditions."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "NIST SP 800-53 Rev 5, AC-3 Access Enforcement (csf.tools mirror) — the gate enforces ALL approved authorizations, i.e. every required attribute must hold, not just one"
    url: "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-3/"
    quote: "Enforce approved authorizations for logical access to information and system resources in accordance with applicable access control policies."
    quoted_at: "2026-06-23"
  - source_type: external
    citation: "NIST SP 800-53 Rev 5, AC-2(3) Disable Accounts (csf.tools mirror) — access is disabled on a defined trigger including expiry, the revoke discipline a time-bounded grant generalizes"
    url: "https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-2/"
    quote: "Disable accounts within [Assignment: organization-defined time period] when the accounts: Have expired; Are no longer associated with a user or individual; Are in violation of organizational policy; or Have been inactive for [Assignment: organization-defined time period]."
    quoted_at: "2026-06-23"
---

## A time-bounded grant is a RECOMPUTED predicate over the clock, not a stored 'expired' flag

**Impact: HIGH — a stored expiry flag goes stale the moment the wall clock crosses validUntil; a half-open off-by-one over-grants by one tick; an ANY-of eligibility gate admits an un-licensed subject; a delete path destroys the revoke trail.**

A *time-bounded relationship grant* binds a subject to a `(resourceRef, relation)` for a window `[validFrom, validUntil)` and is the composition the catalog lacked: relationship-authorization was ATEMPORAL (a tuple is present or absent) and the capability token was a caller-less bearer — nothing carried an EXPIRY a check recomputes. ABAC is precisely *"an access control method where subject requests … are granted or denied based on assigned attributes of the subject, … environment conditions, and a set of policies"* — a validity window is an *environment condition* the policy evaluates AT the time of access, so it MUST NOT be frozen into a stored verdict:

```text
check(grant):       allowed IFF status == ACTIVE AND now ∈ [validFrom, validUntil)
                    recomputed every call over the injected Clock — NO stored 'expired' column;
                    before validFrom → 403 GRANT_NOT_YET_VALID; at/after validUntil → 403 GRANT_EXPIRED;
                    revoked → 403 GRANT_REVOKED regardless of the window
revoke(grant):      append-only — record (revokedBy, revokedAt) write-once under PESSIMISTIC_WRITE;
                    idempotent; NO delete path anywhere in the domain
eligibility(set):   pass IFF for EVERY required class the subject holds a credential valid at now
                    (each credential's own [validFrom, validUntil) recomputed) — a single missing/
                    expired class → 403 CREDENTIAL_INELIGIBLE naming that class (AC-3: enforce ALL)
```

**1. The window is a recomputed predicate (AGRANT-WINDOW/BOUNDARY-001).** `isActiveAt(now)` is a pure function of `(status, validFrom, validUntil, now)`; the SAME row is allowed at `T` and denied at `validUntil` with zero intervening write. The interval is HALF-OPEN — the instant equal to `validUntil` is the first instant of the denied side (no off-by-one that admits it).

**2. Grants are append-only + revocable (AGRANT-REVOKE-001).** A revoke records who and when (write-once — the idempotent hook never overwrites a prior revoke; the columns are deliberately NOT `@Column(updatable=false)`, since the single revoke UPDATE must write them) under the grant row's `PESSIMISTIC_WRITE` lock; a revoked grant fails closed regardless of the window; there is NO delete path — the revoke trail is what AC-2 *"Disable accounts … when … Have expired"* relies on.

**3. The eligibility gate requires ALL classes (AGRANT-ELIGIBILITY-001).** AC-3 says *"Enforce approved authorizations … in accordance with applicable access control policies"* — ALL of them. The gate passes only when EVERY required class is held by a credential non-expired at `now`; one missing/expired class fails closed naming the class.

**Incorrect — a stored 'expired' flag, an inclusive upper bound, an ANY-of eligibility gate:**

```java
// catalog-example-ok: AccessGrantRepo — illustrative anti-pattern, not the shipped repository shape
public boolean canAccess(UUID grantId, List<String> required) {
    AccessGrant g = repo.findById(grantId).orElseThrow();
    if (g.isExpired()) return false;                       // ❌ stored boolean — stale once now passes validUntil
    if (now().isAfter(g.getValidUntil())) return true;     // ❌ inclusive upper bound admits the validUntil instant
    return required.stream().anyMatch(this::subjectHas);   // ❌ ANY-of — admits an un-insured/un-licensed subject
}
```

**Correct — recompute the window over the injected Clock, fail closed, require every credential class:**

```java
@Transactional(readOnly = true)
public AccessGrant check(UUID grantId) {
    AccessGrant g = grants.findById(grantId).orElseThrow(AccessGrantException::notFound);
    Instant now = Instant.now(clock);
    if (g.isRevoked()) throw AccessGrantException.revoked();          // ✅ fail closed regardless of window
    if (g.isBeforeWindow(now)) throw AccessGrantException.notYetValid();
    if (!g.isActiveAt(now)) throw AccessGrantException.expired();     // ✅ now >= validUntil → 403 (half-open)
    return g;                                                        // ✅ allowed only inside [from, until) + ACTIVE
}

// AccessGrant — the recomputed predicate; NO stored 'expired'/'active' boolean column
public boolean isActiveAt(Instant now) {
    return status == GrantStatus.ACTIVE
        && !now.isBefore(validFrom)        // now >= validFrom
        && now.isBefore(validUntil);        // now <  validUntil (the instant == validUntil is OUTSIDE)
}

@Transactional(readOnly = true)
public void requireEligible(String subjectId, List<String> requiredClasses) {
    Instant now = Instant.now(clock);
    Set<String> validClasses = new LinkedHashSet<>();
    for (Credential c : credentials.findBySubjectId(subjectId)) {
        if (c.isValidAt(now)) validClasses.add(c.getCredentialClass());   // ✅ each credential recomputed over now
    }
    for (String required : requiredClasses) {
        if (!validClasses.contains(required))
            throw AccessGrantException.credentialIneligible(required);    // ✅ ALL required — name the missing class
    }
}
```

The grant carries no stored expiry; `isActiveAt(now)` recomputes the verdict so the boundary keystone (advance the injected Clock to `validUntil` with no write → the same grant flips allowed→denied) holds. Revoke is append-only under the row lock; no delete path exists. The eligibility gate is AND-over-the-set, naming the first missing/expired class.

Verification: review-tier — confirm there is no `expired`/`active` boolean column, the window check is half-open and recomputed over the injected Clock, revoke records `(revokedBy, revokedAt)` write-once with no delete path, and the eligibility gate requires every class (not any). The behavioural proof a fork-receiver keeps green: the boundary test (at `validUntil.minusSeconds(1)` allowed; at exactly `validUntil` denied on the SAME row).

Reference: [NIST SP 800-162 ABAC (CSRC glossary)](https://csrc.nist.gov/glossary/term/attribute_based_access_control)

Reference: [NIST SP 800-53 Rev 5 AC-3 Access Enforcement](https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-3/)

Reference: [NIST SP 800-53 Rev 5 AC-2 Account Management (Disable Accounts)](https://csf.tools/reference/nist-sp-800-53/r5/ac/ac-2/)
