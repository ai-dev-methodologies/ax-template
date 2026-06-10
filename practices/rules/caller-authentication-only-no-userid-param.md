---
title: Caller identity derives from Authentication only — never accept userId via path or query
impact: HIGH
impactDescription: "Accepting userId via path/query opens structural IDOR — a bug-free check is harder than removing the parameter"
tags:
  - api
  - authz
  - idor
  - owner-scoped
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-AUTHZ-002"
verification:
  gradle_task: testFavorites
  tag: FAV-AUTHZ-002
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
evidence:
  - source_type: external
    citation: "OWASP ASVS 4.0.3 — V1.4.4 Access Control Architecture"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x10-V1-Architecture.md"
    quote: "Verify the application uses a single and well-vetted access control mechanism for accessing protected data and resources."
    quoted_at: "2026-05-22"
---

## Caller identity derives from Authentication only — never accept userId via path or query

**Impact: HIGH — Accepting userId via path/query opens structural IDOR**

The canonical Broken Object Level Authorization (BOLA / IDOR) pattern — OWASP API Top 10's #1 risk — is endpoints that take a userId-shaped parameter and check it against the caller's authority. The check works when it works. The check fails open the moment a developer forgets to add it, mis-orders the filter chain, or accepts the parameter as a hint and trusts it elsewhere in the request flow.

The structural defense is simpler: **do not accept a userId parameter at all**. Derive the caller from `Authentication.getName()` server-side. There is no parameter for an attacker to enumerate. There is no "did we remember to check it" question because there is no input to check. This pattern is uniform across favorites (R34), activity-feed (R35), comment-thread (R36), session-management (R33), api-key management (R30), file-storage, and approval-workflow — every owner-scoped surface in the catalog.

**Incorrect — accepts userId in path, then "checks" it:**

```java
@GetMapping("/api/favorites/by-user/{userId}")
public List<Favorite> myFavorites(Authentication auth, @PathVariable String userId) {
    if (!auth.getName().equals(userId)) {
        throw new AccessDeniedException("not your favorites");
    }
    return service.list(userId);
}
```

The check is correct, but the *structure* invites failure. A second endpoint forgets the check; a code reviewer misses it; a refactor moves the path variable into the service layer where the check no longer applies.

**Correct — derive caller from Authentication, no userId parameter:**

```java
@GetMapping("/api/favorites")
public List<Favorite> myFavorites(Authentication auth) {
    return service.list(auth.getName());
}
```

There is nothing for an attacker to flip. The userId is server-side, end-to-end. Cross-user enumeration is *structurally impossible*, not just *currently checked*.

This rule applies to read endpoints, mutation endpoints, and aggregation endpoints alike. For admin endpoints that legitimately need to act on arbitrary users, use a dedicated `/api/admin/...` path gated by `hasAuthority("ROLE_ADMIN")` AND record the actor in the resource's `actedByUserId` column for audit — the admin path is the only place where another user's identifier appears.

**Scope — shared-terminal operator attribution is NOT banned (closes IDW11-G23).** This rule governs the request's AUTHORIZATION SUBJECT: who the caller IS must come from `Authentication`, never a parameter. It does NOT ban a separately-named operator-attribution field on shared-terminal flows (a kiosk POS, a clinical workstation, an MES shop-floor terminal) where ONE authenticated device session is operated by MULTIPLE real humans — there, recording WHO physically acted requires an explicit field. The distinction that keeps it safe: (a) authorization still derives ONLY from `Authentication` (the terminal's session/service identity); (b) the operator field is attribution-only — it MUST never select the resource owner, widen access, or substitute for the caller identity; (c) AS A REQUEST FIELD it is named for what it is (`operatorBadgeId` / `actorId` — never `userId` as a parameter name; persisted audit COLUMNS like `actedByUserId` follow their own convention and are out of this clause's scope), so it cannot masquerade as the caller; (d) it is recorded into the audit/event trail alongside the authenticated caller, not instead of it; and (e) the server MUST validate the submitted value against a trusted operator registry for the authenticated terminal (the badge IDs enrolled for that device/tenant) and reject an unrecognized value with 400 — never store a free-text operator string, or the field becomes an audit-log poisoning vector.

Reference: [OWASP API Security Top 10 (2023) — API1:2023 BOLA](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)

Reference: [OWASP ASVS V4 — Access Control](https://owasp.org/www-project-application-security-verification-standard/)
