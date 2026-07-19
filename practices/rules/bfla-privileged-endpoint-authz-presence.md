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
catalog's convention, any `*AdminController`) must resolve to an **effective** authorization check by
one of three routes — a single class-level `@PreAuthorize`/`@PostAuthorize` that covers every method,
an individual annotation on every `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/
`@PatchMapping`/`@RequestMapping` method, or a `SecurityConfig` `requestMatchers(...)` rule whose path
pattern covers the endpoint **and whose required authority is admin** (`hasAuthority('ROLE_ADMIN')` /
`hasRole('ADMIN')`). A handler covered by none is the BFLA shape: any caller who reaches the route
reaches the handler, no role check in between.

Two subtleties make the check non-obvious — and are exactly where a naive "does an `@PreAuthorize`
exist?" review passes an endpoint that is in fact open:

1. **An `@PreAuthorize`/`@PostAuthorize` is only effective if its SpEL actually requires an
   authority/role.** `@PreAuthorize("permitAll()")`, `"anonymous()"`, `"isAnonymous()"`, or an empty
   expression are *not* authorization checks — they admit any (or every) caller. Presence of the
   annotation is not coverage; the SpEL must contain `hasAuthority`/`hasRole`/`hasAnyAuthority`/
   `hasAnyRole`/`hasPermission`.
2. **A method-level annotation overrides the class-level one** (Spring method-security precedence).
   A class-level `hasAuthority('ROLE_ADMIN')` does *not* rescue a method that re-declares
   `@PreAuthorize("permitAll()")` — the method-level `permitAll()` wins.

**Incorrect — a method-level `permitAll()` overrides the class-level gate, so `export()` is open (the class-level ROLE_ADMIN does NOT rescue it):**

```java
@RestController
@RequestMapping("/api/admin/ledger")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")   // covers list() below, but...
public class LedgerAdminController {

    @GetMapping
    public List<LedgerEntryDto> list() { ... }   // ✅ covered by class-level annotation

    @PostMapping("/export")
    @PreAuthorize("permitAll()")                  // ❌ VIOLATION: method-level permitAll() is NON-authz
    public ExportJobDto export() { ... }          //    and OVERRIDES the class-level ROLE_ADMIN —
                                                  //    reachable by ANY caller. `admin_preauthorize_guard.sh`
                                                  //    flags this (it is not rescued by mere annotation presence).
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
mapped method (every `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`/`@PatchMapping`/
`@RequestMapping`) resolves to neither an **effective** class-level nor method-level
`@PreAuthorize`/`@PostAuthorize` (a SpEL that requires an authority/role — `permitAll()`/`anonymous()`
do not count, and a method-level annotation overrides the class-level one), NOR a `SecurityConfig`
`requestMatchers(...)` rule whose path pattern covers the endpoint by boundary-aware Ant semantics and
whose required authority is admin (`ROLE_ADMIN`). It complements, rather than replaces,
`role_literal_guard.sh` (which validates that an `@PreAuthorize` authority STRING is a known-valid
role — a different invariant: it never checks whether the annotation is present at all).

### Guard scope — best-effort STATIC backstop, fail-closed, NOT authoritative

`admin_preauthorize_guard.sh` is a **best-effort static heuristic**, not an exhaustive Spring-Security
authorization verifier. It models the two common coverage shapes — method-/class-level `@PreAuthorize`
and the `SecurityConfig` `requestMatchers(...)` chain — and it models the chain the way Spring evaluates
it: in **declared order**, honoring the optional leading `HttpMethod` (a verb-specific matcher matches
only that verb; a verb-agnostic matcher matches every verb), crediting an endpoint as admin-covered
**only if the first matcher that matches its (verb, path) requires admin authority**. This is what
closed the round-2 verb bypass — a verb-scoped `hasAuthority('ROLE_ADMIN')` GET matcher declared before
a verb-agnostic `.authenticated()` fallback does **not** protect a POST/PUT/PATCH/DELETE, because
Spring's first match for those verbs is the `.authenticated()` rule.

Because it is static, it **FAILS CLOSED**: whenever it cannot *prove* admin coverage for every verb an
endpoint answers — SecurityConfig absent, more than one filter chain, a `securityMatcher`-scoped chain,
a rule it does not model (`.access(...)`, a custom `AuthorizationManager`), a non-literal/variable path
argument, or a wildcard shape it cannot evaluate precisely — it does **not** credit coverage and demands
an explicit effective method-level `@PreAuthorize`, else it BLOCKS. A security guard must never credit on
uncertainty. The trade-off is that an unusual-but-safe config may be asked to add a defense-in-depth
annotation; that is acceptable for a supplementary backstop.

The guard is **not the authoritative BFLA control and not the primary non-vacuity proof**. The
authoritative control is `SecurityConfig.java` itself **plus the per-domain integration tests that assert
HTTP 403 for a non-admin caller** (e.g. `AuthzParityViolationProofTest` / `AccessGrantViolationProofTest`
and the `./gradlew test{Domain}` `AUTHZ` items). This guard is a cheap early-warning regression net for
the "annotation dropped / matcher mis-scoped" shape; it complements those tests, it does not replace them.

Reference: [OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)
