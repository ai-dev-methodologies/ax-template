---
title: Non-owner access to another subject's resource MUST be a grant-table lookup — not owner-equality, not a static role
impact: HIGH
impactDescription: "A relationship-scoped resource guarded by owner-equality (or a global role) either locks out legitimate grantees or fails open to anyone holding the role; the structural fix is a join-table lookup whose missing row denies as 404"
tags:
  - authz
  - rebac
  - bola
  - grant
  - least-privilege
spec_ref: "specs/relationship-authz-l0.yaml#REBAC-LOOKUP-001"
verification:
  type: review
  source: "templates/L0/fork-receiver-kit/use-caller-id.ts"
  pattern: "caller id derives from Authentication (server-side, never a client-supplied subject param); non-owner access resolved by a parameterized lookup against a grant/membership join-table; missing grant row → 404 existence-hiding; LIST endpoints PRE-FILTER by the caller's grant set inside the @Query (no post-filter)"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - "https://owasp.org/www-project-application-security-verification-standard/"
  - "https://en.wikipedia.org/wiki/Google_Zanzibar"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 Broken Object Level Authorization (BOLA)"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
    quote: "Every API endpoint that receives an ID of an object, and performs any action on the object, should implement object-level authorization checks."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP ASVS v4.0.3 — V4.1.3 General Access Control Design (principle of least privilege)"
    url: "https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md"
    quote: "Verify that the principle of least privilege exists - users should only be able to access functions, data files, URLs, controllers, services, and other resources, for which they possess specific authorization."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "Google Zanzibar — relationship-based access control (ReBAC) tuple model (USENIX ATC 2019)"
    url: "https://en.wikipedia.org/wiki/Google_Zanzibar"
    quote: "It processes access control queries from client applications and stores access control lists (ACLs) expressed as relationship tuples under a relationship-based access control (ReBAC) model."
    quoted_at: "2026-06-01"
---

## Non-owner access to another subject's resource MUST be a grant-table lookup — not owner-equality, not a static role

**Impact: HIGH — relationship-scoped resources are the blind spot between owner-equality and tenant-equality**

The catalog already covers three authorization shapes. Owner-equality (`caller-authentication-only-no-userid-param.md`) answers *"is this MY row"* by deriving the caller from `Authentication` and never accepting a `userId` parameter. Tenant-equality (`multi-tenant-l0` ISOLATION-001/003) answers *"is this MY tenant's row"*. Single-designated-principal (`approval-workflow` WF-AUTHZ-003) answers *"am I the one approver named on this step"*. None of them answers the fourth, extremely common question: **"has someone GRANTED me a relation to a resource I do not own?"** — the course-staff member who must read every enrolled learner's submissions, the project member added to someone else's board, the clinician on a patient's care team, the collaborator on a shared document.

Guarding that resource with owner-equality locks the legitimate grantee out. Guarding it with a static global role (`hasAuthority("ROLE_STAFF")`) fails open — *every* staff member can now read *every* learner, not just the ones they were granted. The structural answer, the one Google's Zanzibar popularized as ReBAC, is to make authorization **a lookup against a stored relationship row**, not a branch in code and not a role bit: access is data. The grant/membership join-table row is the single source of truth — its presence authorizes, its absence denies — and the caller id fed into that lookup still comes from `Authentication.getName()` server-side, never from a client-supplied subject id.

Three invariants follow, and they are testable:

1. **The gate is a lookup.** A non-owner, non-admin accessor is authorized iff a grant row exists for `(callerId, resourceScope)`. No grant row → denied. This runs on the trusted service layer for *every* accessor of the object (ASVS V4.1.1; OWASP BOLA requires an object-level check on every endpoint that receives an object id).
2. **A missing grant denies as 404, not 403.** Returning 403 tells the attacker the object exists and turns id-enumeration into an oracle (403 = exists-no-access vs 404 = absent). The catalog default collapses both into an indistinguishable 404 — existence-hiding, deny-by-default, fail-secure (ASVS V4.1.5).
3. **LIST endpoints pre-filter in the query.** Collections JOIN the grant table and bind the caller id *inside the `@Query`*, returning only granted rows. Post-filtering a broad result set in application code is forbidden: it leaks counts/pagination, and the day someone forgets the post-filter the endpoint ships every row.

**Incorrect — owner-equality on a relationship-scoped resource (locks out grantees) or a global role (fails open), plus 403 + post-filtered list:**

```java
@GetMapping("/api/submissions/{id}")
public Submission get(Authentication auth, @PathVariable Long id) {
    Submission s = repo.findById(id).orElseThrow();
    // owner-equality: the assigned course-staff grader can NEVER read it
    if (!s.getOwnerId().equals(auth.getName())) {
        throw new AccessDeniedException("forbidden");   // ❌ 403 leaks existence
    }
    return s;
}

@GetMapping("/api/submissions")
public List<Submission> list(Authentication auth) {
    return repo.findAll().stream()                      // ❌ fetches EVERY row
        .filter(s -> canSee(auth.getName(), s))         // ❌ post-filter; forget it → leak
        .toList();
}
// Swapping the owner check for hasAuthority("ROLE_STAFF") is no better:
// it fails OPEN — every staffer reads every learner, not the granted ones.
```

**Correct — grant-table lookup; missing grant → 404; list pre-filtered inside the query:**

```java
// Grant join-table: one row = "callerId has <relation> to <resourceScope>".
interface GrantRepository extends JpaRepository<Grant, Long> {
    boolean existsByGranteeIdAndScopeId(String granteeId, Long scopeId);

    // LIST pre-filter: the JOIN happens IN the query, bound to the caller.
    @Query("""
        select s from Submission s
        join Grant g on g.scopeId = s.courseId
        where g.granteeId = :caller
    """)
    Page<Submission> findGranted(@Param("caller") String caller, Pageable page);
}

@GetMapping("/api/submissions/{id}")
public Submission get(Authentication auth, @PathVariable Long id) {
    String caller = auth.getName();                       // server-side identity
    Submission s = repo.findById(id).orElseThrow(NotFound::new);   // 404
    boolean owner = s.getOwnerId().equals(caller);
    boolean granted = grants.existsByGranteeIdAndScopeId(caller, s.getCourseId());
    if (!owner && !granted) {
        throw new NotFound();    // ✅ 404 existence-hiding — never 403, no oracle
    }
    return s;
}

@GetMapping("/api/submissions")
public Page<Submission> list(Authentication auth, Pageable page) {
    return repo.findGranted(auth.getName(), page);        // ✅ only granted rows; total reflects them
}
```

The negative test that proves the invariant: with no grant row, `GET /api/submissions/{id}` returns **404** and the list returns **0**; INSERT one grant row for `(caller, courseId)` and the *same* `GET` flips to **200** and the list returns **1**; DELETE the grant row and it flips back to 404 / 0. The authorization decision moved entirely into the presence/absence of a relationship row — that 404→200→404 flip on a single INSERT/DELETE is the signature of correct ReBAC. Verify that the admin path (`/api/admin/...` gated by `hasAuthority("ROLE_ADMIN")`) remains the *only* place a global role substitutes for a grant, and that no endpoint accepts a client-supplied `granteeId`/subject filter that could widen the grant set.

Verification: review-tier. A reviewer confirms (a) non-owner access is decided by a `Grant`/membership repository lookup keyed on `Authentication.getName()`, not owner-equality and not a bare `hasAuthority`; (b) the no-grant branch throws the 404/NotFound path, never a 403; (c) every collection endpoint binds the caller into a grant-joined `@Query` and contains no post-`findAll()` `.filter(...)` over authorization. The caller-identity seam this composes with ships at `templates/L0/fork-receiver-kit/use-caller-id.ts`. No `@Tag` test is claimed because the runtime grant table and its 404→200 flip are recipe-instantiated, not present as a generic backend module in this template.

Reference: [OWASP API Security Top 10 (2023) — API1:2023 BOLA](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)

Reference: [OWASP ASVS v4.0.3 — V4 Access Control](https://raw.githubusercontent.com/OWASP/ASVS/v4.0.3/4.0/en/0x12-V4-Access-Control.md)

Reference: [Google Zanzibar — relationship-based access control (ReBAC)](https://en.wikipedia.org/wiki/Google_Zanzibar)
