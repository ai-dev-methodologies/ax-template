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
  pattern: "every MUTATING mapped endpoint (@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping, or @RequestMapping with a mutating/absent method) inside an admin-surface controller (class name *AdminController, OR a class-level @RequestMapping whose path contains '/admin') carries an EFFECTIVE admin @PreAuthorize/@PostAuthorize (class- or method-level SpEL requiring ROLE_ADMIN). The guard is purely LOCAL — it does NOT parse SecurityConfig; the path-matchers are a complementary layer, not a substitute for the annotation."
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

The fix is mechanical, not judgment-based: every **mutating** mapped endpoint
(`@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping`, or a `@RequestMapping` with a mutating or
absent HTTP method) in an admin-surface controller — a class whose name ends `AdminController`, **or**
whose class-level `@RequestMapping` path contains `/admin` (so the control is decoupled from the naming
convention) — must carry an **effective** admin authorization annotation: a class-level
`@PreAuthorize`/`@PostAuthorize` that covers every method, or an individual annotation on the endpoint,
whose SpEL requires admin authority (`hasAuthority('ROLE_ADMIN')` / `hasRole('ADMIN')` / an all-admin
`hasAnyAuthority(...)` / `denyAll()`). A mutating handler covered by none is the BFLA shape: any caller
who reaches the route reaches the handler, no role check in between. The `SecurityConfig` path-matcher is
a complementary defense layer, but it is **not** accepted as a substitute — see "Guard scope" below for
why.

Two subtleties make the check non-obvious — and are exactly where a naive "does an `@PreAuthorize`
exist?" review passes an endpoint that is in fact open:

1. **An `@PreAuthorize`/`@PostAuthorize` is only effective if its SpEL actually requires the ADMIN
   authority.** `@PreAuthorize("permitAll()")`, `"anonymous()"`, `"isAnonymous()"`,
   `"isAuthenticated()"`/`"authenticated()"` alone, `hasAuthority('ROLE_USER')`, or an empty expression
   are *not* admin checks — they admit some non-admin (or every) caller. Presence of the annotation is
   not coverage; the SpEL must require `ROLE_ADMIN` (`hasAuthority('ROLE_ADMIN')` / `hasRole('ADMIN')` /
   an all-admin `hasAnyAuthority(...)`/`hasAnyRole(...)` / `denyAll()`).
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

Verification: static-analysis-tier, purely LOCAL. `admin_preauthorize_guard.sh` walks every
admin-surface controller (class name `*AdminController`, **or** a class-level `@RequestMapping` whose
path contains `/admin`) under the backend package tree and fails (`ADMIN_ENDPOINT_MISSING_PREAUTHORIZE`)
the moment any **mutating** mapped endpoint (`@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping`,
or a `@RequestMapping` with a mutating or absent method) resolves to neither an **effective** class-level
nor method-level `@PreAuthorize`/`@PostAuthorize` — a SpEL requiring `ROLE_ADMIN`; `permitAll()`,
`anonymous()`, `authenticated()`/`isAuthenticated()` alone, a non-admin authority, an empty/non-literal
expression, or no annotation all fail, and a method-level annotation overrides the class-level one. The
guard does **not** read `SecurityConfig.java` at all. It complements, rather than replaces,
`role_literal_guard.sh` (which validates that an `@PreAuthorize` authority STRING is a known-valid
role — a different invariant: it never checks whether the annotation is present at all).

### Guard scope — a purely-LOCAL static check, fail-closed, complementary (NOT authoritative)

`admin_preauthorize_guard.sh` reads **only the controller file**. It does **not** parse
`SecurityConfig.java` and does **not** credit the `authorizeHttpRequests` matcher chain for coverage.
That is a deliberate design choice. An earlier revision tried to statically model the Spring Security
matcher chain (path Ant-matching, declared order, verb scoping) so it could credit a path-matcher as
coverage, and a cross-family reviewer then found **four distinct static-analysis bypasses across three
rounds** (a verb-scoped matcher; multiple / unscoped filter chains; a `@RequestMapping(path = ...)`
alias resolving to the wrong matcher; …). Exhaustively deciding Spring's authorization from source is
not statically decidable — every hardening left another shape to exploit. Removing SecurityConfig
crediting **entirely** ends that whack-a-mole class: with no config chain to model, there is nothing to
bypass. The required control is a purely-local, decidable property — a method-/class-level
`@PreAuthorize` requiring `ROLE_ADMIN`.

Because it inspects only the SpEL, it **FAILS CLOSED**: an empty expression, a non-literal SpEL argument
(a named constant we cannot resolve to an authority), `permitAll()`/`anonymous()`/`authenticated()`, or a
`hasAny*(...)` that mixes in any non-admin alternative are all treated as NOT admin and BLOCK. A security
guard must never credit on uncertainty; the trade-off is that a legitimate constant-based annotation may
be asked to inline a literal SpEL — acceptable for a defense-in-depth backstop.

The `SecurityConfig` path-matchers stay in the real repo as a **complementary** layer (belt +
suspenders), and `@EnableMethodSecurity` is active so the annotation this guard requires is a genuine
second runtime gate. The guard is **not the authoritative BFLA control and not the primary non-vacuity
proof**. The authoritative control is `SecurityConfig.java` **plus the per-domain integration tests that
assert HTTP 403 for a non-admin caller** (e.g. `AuthzParityViolationProofTest` /
`AccessGrantViolationProofTest` and the `./gradlew test{Domain}` `AUTHZ` items). This guard is a cheap,
precise local regression net for the "annotation dropped from a mutating admin endpoint" shape; it
complements those tests, it does not replace them.

Reference: [OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)
