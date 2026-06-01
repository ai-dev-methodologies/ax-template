---
title: When >2 roles exist and higher tiers subsume lower tiers, declare a RoleHierarchy @Bean — never enumerate hasAnyRole(...)
impact: HIGH
impactDescription: "Without a single RoleHierarchy bean, every @PreAuthorize must list every superior role by hand; the day someone forgets to add ROLE_ADMIN to a hasAnyRole(...) the admin is silently locked out, and the day someone forgets to add a new tier the gate silently fails open"
tags:
  - rbac
  - authz
  - bfla
  - role-hierarchy
  - spring-security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-SECURITY-004"
verification:
  type: review
  source: "templates/backend/security/RoleHierarchyConfig.java"
  pattern: "a single RoleHierarchy @Bean (RoleHierarchyImpl.withDefaultRolePrefix().role(\"ADMIN\").implies(\"MANAGER\")...) wired into a MethodSecurityExpressionHandler @Bean; @PreAuthorize/authorizeHttpRequests gates name ONLY the minimum tier (hasRole('MEMBER')) and rely on the hierarchy to admit superiors — no endpoint enumerates hasAnyRole('ADMIN','MANAGER','MEMBER')"
upstream:
  - "https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html"
  - "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
evidence:
  - source_type: external
    citation: "Spring Security Reference — Authorization Architecture / Hierarchical Roles (RoleHierarchyImpl)"
    url: "https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html"
    quote: "Here we have four roles in a hierarchy `ROLE_ADMIN ⇒ ROLE_STAFF ⇒ ROLE_USER ⇒ ROLE_GUEST`. A user who is authenticated with `ROLE_ADMIN`, will behave as if they have all four roles when security constraints are evaluated against any filter- or method-based rules."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
    quote: "Authorization checks for a function or resource are usually managed via configuration or code level. Implementing proper checks can be a confusing task since modern applications can contain many types of roles, groups, and complex user hierarchies (e.g. sub-users, or users with more than one role)."
    quoted_at: "2026-06-01"
---

## When >2 roles exist and higher tiers subsume lower tiers, declare a RoleHierarchy @Bean — never enumerate hasAnyRole(...)

**Impact: HIGH — an enumerated superior-role list is the silent fail-open / fail-closed seam OWASP names as "complex user hierarchies"**

The reference workload ships a flat authorization model: `ROLE_ADMIN` versus authenticated. That is correct for two tiers — `hasAuthority("ROLE_ADMIN")` on `/api/admin/**`, `.authenticated()` on everything else, and nothing in between. But the moment a fork-receiver introduces a *third* tier where the tiers form a subsumption chain (the canonical Korean-enterprise shape `ROLE_ADMIN > ROLE_MANAGER > ROLE_MEMBER`, where an admin can do everything a manager can and a manager everything a member can), the flat pattern stops scaling and a structural trap opens.

The naive way to express "a manager-or-above may reach this endpoint" is to enumerate the superior roles at the gate: `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")`. This is wrong for the same reason OWASP API5:2023 calls function-level authorization "a confusing task" once "modern applications can contain many types of roles, groups, and complex user hierarchies": the superior-role list is now copy-pasted across dozens of `@PreAuthorize` sites, and it drifts. The two failure modes are mirror images and both are silent:

1. **Fail-closed drift.** Someone adds a manager-only endpoint and writes `hasRole('MANAGER')`, forgetting to also list `'ADMIN'`. Now an admin — who should be able to do *everything* a manager can — is **locked out** of that one endpoint. The compiler says nothing; the gap surfaces only when an admin hits a 403 in production.
2. **Fail-open drift.** A new tier `ROLE_AUDITOR` is inserted between manager and member. Every gate that listed `hasAnyRole('ADMIN','MANAGER')` to mean "manager-and-above" must now be revisited to decide whether auditor counts — and the ones nobody revisits keep their stale list, **silently** admitting or excluding the new tier at the wrong boundary.

Spring Security's answer is to declare the subsumption **once**, as data, in a single `RoleHierarchy` bean, and wire it into the `MethodSecurityExpressionHandler` (and the web `AuthorizationManager`). Then every gate names **only the minimum tier it requires** — `hasRole('MEMBER')` on a member-floor endpoint, `hasRole('MANAGER')` on a manager-floor endpoint — and the hierarchy admits all superiors automatically. Per the reference doc: *"A user who is authenticated with `ROLE_ADMIN`, will behave as if they have all four roles when security constraints are evaluated against any filter- or method-based rules."* The hierarchy lives in exactly one place; adding or reordering a tier is a one-line edit to the bean, not a hunt across every annotation.

**Incorrect — superior roles enumerated at each gate; the list drifts and silently fails open or closed:**

```java
// No RoleHierarchy bean. Every gate hand-lists who is "or above".
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")           // manager-floor endpoint
public Report managerReport() { ... }

@PreAuthorize("hasRole('MANAGER')")                       // ❌ forgot 'ADMIN' →
public void approve(Long id) { ... }                      //    admin is LOCKED OUT (fail-closed)

// Insert ROLE_AUDITOR between MANAGER and MEMBER tomorrow, and every
// hasAnyRole('ADMIN','MANAGER') above must be re-audited by hand — the
// ones nobody touches keep a stale list (fail-open / fail-closed at random).
```

**Correct — declare the hierarchy ONCE as a bean; gates name only the minimum tier:**

```java
@Configuration
@EnableMethodSecurity
public class RoleHierarchyConfig {

    // The subsumption chain, declared in ONE place. ROLE_ADMIN ⇒ ROLE_MANAGER ⇒ ROLE_MEMBER.
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
            .role("ADMIN").implies("MANAGER")
            .role("MANAGER").implies("MEMBER")
            .build();
    }

    // Wire the hierarchy into method security so @PreAuthorize honors it.
    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}

// Gates now name ONLY the minimum tier; superiors are admitted by the hierarchy.
@PreAuthorize("hasRole('MANAGER')")    // ✅ ADMIN admitted automatically — no enumeration
public Report managerReport() { ... }

@PreAuthorize("hasRole('MANAGER')")    // ✅ ADMIN can approve; never silently locked out
public void approve(Long id) { ... }

@PreAuthorize("hasRole('MEMBER')")     // ✅ MANAGER and ADMIN both admitted
public List<Item> myItems() { ... }
```

The same `RoleHierarchy` bean is consulted by the web `authorizeHttpRequests` `AuthorizationManager` (Spring Security wires it automatically when the bean is present), so URL-based gates inherit the chain too — a fork-receiver does not maintain two hierarchies. The two-tier reference workload does **not** need this rule (a flat ADMIN-vs-authenticated split has no subsumption to express); it applies precisely when a fork-receiver crosses into three-or-more subsuming tiers.

Verification: review-tier. A reviewer confirms (a) a single `@Bean RoleHierarchy` declares the full chain with `.role(X).implies(Y)` (or `RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_MANAGER\nROLE_MANAGER > ROLE_MEMBER")`); (b) that bean is set on a `MethodSecurityExpressionHandler` bean (and, for URL gates, the web `AuthorizationManager` picks it up); (c) no `@PreAuthorize` or `authorizeHttpRequests` matcher enumerates a superior-role list (`hasAnyRole('ADMIN','MANAGER',...)`) to mean "or above" — every gate names only its minimum tier. No `@Tag` test is claimed: the subsumption is a runtime Spring-Security wiring property exercised only once a fork-receiver instantiates a multi-tier RBAC, not a generic backend module present in this two-tier template. The blessed bean ships at `templates/backend/security/RoleHierarchyConfig.java`.

Reference: [Spring Security Reference — Authorization Architecture / Hierarchical Roles](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)

Reference: [OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization](https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/)
