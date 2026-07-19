---
title: Every privileged/admin mapped endpoint MUST carry a class-level or method-level authorization annotation
impact: HIGH
impactDescription: "A mapped handler with no @PreAuthorize/@PostAuthorize (class- or method-level) is reachable by any authenticated — or, if the security matcher is also wrong, any unauthenticated — caller; the BFLA gap is invisible in a diff review because the code compiles and the happy path works for the developer's own admin session"
tags:
  - rbac
  - authz
  - bfla
  - admin
  - access-control
spec_ref: "specs/crud-security.yaml#CRUD-AUTH-2"
verification:
  type: static_analysis
  guard: admin_preauthorize_guard.sh
  source: "practices/evals/admin_preauthorize_guard.sh (promoted wave-1 exit cleanup from practices/consumer-proof/scenarios/S3.b2b-admin/scenario-guards/admin_preauthorize_guard.sh — wired live+fixtures in run-all-guards.sh)"
  pattern: "every @GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@RequestMapping method inside a *AdminController (or any privileged controller) is covered by an @PreAuthorize/@PostAuthorize annotation (class- or method-level) OR a matching SecurityConfig.java requestMatchers(...).hasAuthority(...)/.hasRole(...) rule"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
    quote: "Authorization checks for a function or resource are usually managed via configuration or code level. Implementing proper checks can be a confusing task since modern applications can contain many types of roles, groups, and complex user hierarchies (e.g. sub-users, or users with more than one role)."
    quoted_at: "2026-07-20"
---

## Every privileged/admin mapped endpoint MUST carry a class-level or method-level authorization annotation

**Impact: HIGH — the missing annotation is a silent BFLA gap, not a compile error or a failing happy-path test**

`rbac-stub-default-fail-closed.md` covers the *dev-stub default* (what a not-yet-wired caller-role
lookup returns before a fork-receiver connects a real identity provider) and
`role-hierarchy-subsumes-lower-tiers.md` covers *tier subsumption* (how a superior role is admitted
once more than two roles exist). Neither states the more basic invariant those two build on top of:
**a mapped handler that exposes a privileged operation must carry an authorization check at all.**
That presence requirement had no reusable, evidence-anchored rule in the catalog — it was only
encoded ad hoc, per domain, as scattered `AUTHZ` *test items* inside individual domain specs
(`CRUD-AUTH-2`, `TAG-AUTHZ-002`, `ASVS` items, etc.), each proving the invariant for one controller
without stating it as a cross-cutting rule an AI agent can consult before writing the *next* one.

OWASP names exactly this failure mode API5:2023 Broken Function Level Authorization: "modern
applications can contain many types of roles, groups, and complex user hierarchies," which is
precisely the surface on which a developer — human or AI — most often reasons "the URL is under
`/api/admin/**` so the security matcher already covers it" and skips the annotation. That reasoning
is fragile: a matcher change, a new controller nested under a different base path, or a copy-pasted
controller that drops the class-level annotation all reopen the gap, and none of them fail a
compile or a happy-path test written against an already-authenticated admin session.

The fix is mechanical, not judgment-based: every mapped method in a privileged controller (in this
catalog's convention, any `*AdminController`) must resolve to an authorization check by one of two
routes — a single class-level `@PreAuthorize`/`@PostAuthorize` that covers every method, or an
individual annotation on every `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/
`@RequestMapping` method. A handler covered by neither is the BFLA shape: any caller who reaches the
route reaches the handler, no role check in between.

**Incorrect — one method in an otherwise-guarded admin controller has no authorization check:**

```java
@RestController
@RequestMapping("/api/admin/ledger")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")   // covers list()/get(), but...
public class LedgerAdminController {

    @GetMapping
    public List<LedgerEntryDto> list() { ... }   // ✅ covered by class-level annotation

    @PostMapping("/export")
    @PreAuthorize("permitAll()")                  // ❌ someone "temporarily" loosened this for a
    public ExportJobDto export() { ... }          //    local test and it shipped — reachable by ANY caller
}
```

**Correct — the class-level annotation covers every mapped method, with no per-method override that weakens it:**

```java
@RestController
@RequestMapping("/api/admin/ledger")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")   // ✅ one declaration, covers every method below
public class LedgerAdminController {

    @GetMapping
    public List<LedgerEntryDto> list() { ... }

    @PostMapping("/export")
    public ExportJobDto export() { ... }       // ✅ inherits the class-level ROLE_ADMIN gate

    @GetMapping("/export/{jobId}")
    public ExportJobDto status(@PathVariable UUID jobId) { ... }  // ✅ same
}
```

Verification: static-analysis-tier. `admin_preauthorize_guard.sh` walks every `*AdminController.java`
under the backend package tree and fails (`ADMIN_ENDPOINT_MISSING_PREAUTHORIZE`) the moment any
mapped method resolves to neither a class-level nor a method-level `@PreAuthorize`/`@PostAuthorize`.
It complements, rather than replaces, `role_literal_guard.sh` (which validates that an
`@PreAuthorize` authority STRING is a known-valid role — a different invariant: it never checks
whether the annotation is present at all).

Reference: [OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)
