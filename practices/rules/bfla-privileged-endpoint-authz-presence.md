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
  pattern: "every REQUIRED mutating mapped endpoint (@PostMapping/@PutMapping/@PatchMapping/@DeleteMapping, or @RequestMapping with a mutating/absent method) on an admin surface carries an EFFECTIVE admin @PreAuthorize (class- or method-level SpEL requiring ROLE_ADMIN). Admin surface = class name *AdminController, OR a class-level @RequestMapping path containing '/admin', OR (widened) any mutating handler with a method-level mapping path under /api/admin (then only those /api/admin methods are required). @PostAuthorize does NOT gate a mutation (authz runs after the side effect). The guard is a purely-LOCAL static LINT: it does NOT parse SecurityConfig and does NOT perform adversarial SpEL evaluation (a deliberately-crafted always-true weakener is out of lint scope — the authoritative BFLA control is the domain 403 integration tests + SecurityConfig). It catches MISSING annotations and OBVIOUSLY-ineffective ones (permitAll/anonymous/authenticated-only/@PostAuthorize-for-mutation/negation/trivial-disjunction)."
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
absent HTTP method) on an **admin surface** must carry an **effective** admin `@PreAuthorize` — a
class-level annotation that covers every method, or an individual annotation on the endpoint — whose SpEL
requires admin authority (`hasAuthority('ROLE_ADMIN')` / `hasRole('ADMIN')` / an all-admin
`hasAnyAuthority(...)` / `denyAll()`). A controller is an **admin surface** if its name ends
`AdminController`, **or** its class-level `@RequestMapping` path contains `/admin` (decoupling the control
from the naming convention), **or** — the round-4 widening — any of its mutating handlers carries a
method-level mapping path under `/api/admin` (so a mixed controller such as `OfferEligibilityController`
or `TaxApplicationController`, which is *not* named `*AdminController` and has *no* class-level `/admin`
mapping but exposes a `POST /api/admin/...` mutation, is no longer invisible). For a name-/class-path
admin surface every mutating endpoint is required; for a controller detected *only* by a method-level
`/api/admin` mapping, only the `/api/admin` mutations are required (a sibling non-admin mutation such as
`POST /api/offers/{id}/evaluate` is intentionally left to SecurityConfig's `authenticated()` rule and is
NOT forced to be admin). **`@PostAuthorize` does NOT gate a mutation** — it runs *after* the handler body,
so the side effect has already happened before authorization is evaluated; only `@PreAuthorize` counts for
a mutation. A required mutating handler covered by none is the BFLA shape: any caller who reaches the route
reaches the handler, no role check in between. The `SecurityConfig` path-matcher is a complementary defense
layer, but it is **not** accepted as a substitute — see "Guard scope" below for why.

Three subtleties make the check non-obvious — and are exactly where a naive "does an `@PreAuthorize`
exist?" review passes an endpoint that is in fact open:

1. **A `@PreAuthorize` is only effective if its SpEL actually requires the ADMIN authority.**
   `@PreAuthorize("permitAll()")`, `"anonymous()"`, `"isAnonymous()"`,
   `"isAuthenticated()"`/`"authenticated()"` alone, `hasAuthority('ROLE_USER')`, an empty expression, a
   **negated** admin predicate (`"!hasAuthority('ROLE_ADMIN')"`), or a **trivial always-true disjunction**
   (`"hasAuthority('ROLE_ADMIN') or true"`, `"... or permitAll()"`, `"... or isAnonymous()"`) are *not*
   admin checks — they admit some non-admin (or every) caller. Presence of the annotation is not coverage;
   the SpEL must require `ROLE_ADMIN` as a **positive required term** (`hasAuthority('ROLE_ADMIN')` /
   `hasRole('ADMIN')` / an all-admin `hasAnyAuthority(...)`/`hasAnyRole(...)` / `denyAll()`).
2. **`@PostAuthorize` does NOT gate a mutation.** It is evaluated *after* the handler returns — the write
   has already been committed by the time the check runs. A mutating endpoint "protected" only by
   `@PostAuthorize` is the BFLA shape; it must carry a `@PreAuthorize`.
3. **A method-level annotation overrides the class-level one** (Spring method-security precedence).
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
path contains `/admin`, **or** a controller with any method-level `/api/admin` mutating mapping) under the
backend package tree and fails (`ADMIN_ENDPOINT_MISSING_PREAUTHORIZE`) the moment any **required**
**mutating** mapped endpoint (`@PostMapping`/`@PutMapping`/`@PatchMapping`/`@DeleteMapping`, or a
`@RequestMapping` with a mutating or absent method) resolves to no **effective** class-level or
method-level `@PreAuthorize` — a SpEL requiring `ROLE_ADMIN`; `permitAll()`, `anonymous()`,
`authenticated()`/`isAuthenticated()` alone, a non-admin authority, a negated admin predicate, a trivial
always-true disjunction, an empty/non-literal expression, a `@PostAuthorize`-only "gate" on a mutation, or
no annotation all fail, and a method-level annotation overrides the class-level one. Fully-qualified
(`@org...PostMapping`) and multiline mapping annotations and non-public handler methods are all scanned.
The guard does **not** read `SecurityConfig.java` at all. It complements, rather than replaces,
`role_literal_guard.sh` (which validates that an `@PreAuthorize` authority STRING is a known-valid
role — a different invariant: it never checks whether the annotation is present at all).

### Guard scope — a purely-LOCAL static LINT, fail-closed, complementary (NOT authoritative)

`admin_preauthorize_guard.sh` is a **static lint**, not an authorization proof. It catches exactly two
shapes on a *required* mutating admin endpoint: **(a) a MISSING** effective admin `@PreAuthorize`, and
**(b) an OBVIOUSLY-ineffective** one that is cheaply decidable by inspection —
`permitAll()`/`anonymous()`/`isAnonymous()`, `authenticated()`/`isAuthenticated()`/`rememberMe()` alone, a
non-admin authority, a mixed `hasAny*(...)`, a **`@PostAuthorize` used to "gate" a mutation** (authz runs
after the side effect), a **negated** admin predicate (`!hasAuthority('ROLE_ADMIN')`), a **trivial
always-true disjunction** (`... or true`/`... or permitAll()`/`... or isAnonymous()`), or an
empty/non-literal SpEL argument.

**Out of lint scope — adversarial SpEL evaluation.** A bash/regex lint cannot decide arbitrary Spring
SpEL. A **deliberately-crafted** weakening expression that is not one of the cheap shapes above — e.g.
`@PreAuthorize("hasAuthority('ROLE_ADMIN') or someComplexAlwaysTrue()")` where `someComplexAlwaysTrue()`
is an opaque bean method that always returns `true` — **will PASS this lint**. That is a
**code-review / malicious-insider** concern, not what a static presence-lint defends. The claim this guard
makes is exactly its behavior: it is a presence-and-obvious-weakener lint, nothing more. The
**authoritative** BFLA control is the per-domain integration tests that assert HTTP 403 for a non-admin
caller **plus** `SecurityConfig.java` — see the closing paragraph below; the coverage-map non-vacuity for
`S2.AUTHZ.BE` stays those ViolationProof / AUTHZ integration tests, and this guard is a supplementary net.

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
(a named constant we cannot resolve to an authority), `permitAll()`/`anonymous()`/`authenticated()`, a
`hasAny*(...)` that mixes in any non-admin alternative, a negated admin predicate, a trivial always-true
disjunction, or a `@PostAuthorize`-only "gate" on a mutation are all treated as NOT admin and BLOCK. A
security guard must never credit on uncertainty; the trade-off is that a legitimate constant-based
annotation may be asked to inline a literal SpEL — acceptable for a defense-in-depth backstop.

The `SecurityConfig` path-matchers stay in the real repo as a **complementary** layer (belt +
suspenders), and `@EnableMethodSecurity` is active so the annotation this guard requires is a genuine
second runtime gate. The guard is **not the authoritative BFLA control and not the primary non-vacuity
proof**. The authoritative control is `SecurityConfig.java` **plus the per-domain integration tests that
assert HTTP 403 for a non-admin caller** (e.g. `AuthzParityViolationProofTest` /
`AccessGrantViolationProofTest` and the `./gradlew test{Domain}` `AUTHZ` items). This guard is a cheap,
precise local regression net for the "annotation dropped from a mutating admin endpoint" shape; it
complements those tests, it does not replace them.

Reference: [OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)
