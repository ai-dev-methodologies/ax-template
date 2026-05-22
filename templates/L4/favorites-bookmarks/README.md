# L4 / favorites-bookmarks — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

Per-user polymorphic favorite via `(entity_type, entity_id)` + `UNIQUE(user_id, entity_type, entity_id)` for idempotent add and DB-enforced "exactly one favorite per user-target". HTTP DELETE returns `204` even when the target is absent (R38 RFC 9110 §9.3.5 idempotency rule). Global count is a separate query — favorites visibility is caller-only (R38 caller-authentication-only rule), with a quota enforced at the service layer.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/favoritesbookmarks/`
- Spec: [`specs/favorites-bookmarks-l0.yaml`](../../../specs/favorites-bookmarks-l0.yaml) — 12 items / 4 families (CRUD × 3, QUERY × 3, AUTHZ × 3, VALIDATION × 3)
- Tests: `./gradlew testFavorites` — GREEN (17/17 incl. iter1 violation proof)
- Anchored generic rules (R38) — favorites is the **canonical example** for two of them:
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — `FAV-AUTHZ-002` is the spec anchor
  - [`practices/rules/http-delete-idempotency-rfc9110.md`](../../../practices/rules/http-delete-idempotency-rfc9110.md) — `FAV-CRUD-002` is the spec anchor

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.favoritesbookmarks` to your project's `<base>.favoritesbookmarks`.
2. Copy `specs/favorites-bookmarks-l0.yaml` for the contract surface.
3. The DELETE-on-absent → 204 behavior is RFC-mandated; do NOT change it to 404 (the iter1 violation proof would fail).
4. The caller-only listing endpoint MUST derive `userId` from `Authentication.getName()`; never accept a `?userId=` query parameter (IDOR / BOLA).
5. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
