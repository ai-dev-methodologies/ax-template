# L4 / comment-thread — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

Polymorphic comments attachable to any `(entity_type, entity_id)` pair, with reply hierarchy via `parentCommentId`. Soft-delete via status flip (`ACTIVE → DELETED`) + body NULLed; DTO masks the body as `[deleted]` so replies still resolve. Edit history (`CommentEdit`) is immutable and survives soft-delete (audit posture). Author-only edit; author-OR-admin delete; admin CANNOT edit (R38 admin-cannot-rewrite rule).

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/commentthread/`
- Spec: [`specs/comment-thread-l0.yaml`](../../../specs/comment-thread-l0.yaml) — 12 items / 4 families (CRUD × 3, THREAD × 3, AUTHZ × 3, HISTORY × 3)
- Tests: `./gradlew testCommentThread` — GREEN (18/18 incl. iter1 violation proof)
- Anchored generic rules (R38):
  - [`practices/rules/soft-delete-audit-trail.md`](../../../practices/rules/soft-delete-audit-trail.md) — canonical example
  - [`practices/rules/admin-cannot-rewrite-user-content.md`](../../../practices/rules/admin-cannot-rewrite-user-content.md) — canonical example
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — author-only edit via `Authentication.getName()`

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.commentthread` to your project's `<base>.commentthread`.
2. Copy `specs/comment-thread-l0.yaml` for the contract surface.
3. Do NOT hard-delete comments — the soft-delete pattern is load-bearing for the audit posture and the reply graph.
4. Admin role MAY delete but MUST NOT edit user content; the author-only edit guard is part of the GDPR Article 5 anchored rule.
5. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
