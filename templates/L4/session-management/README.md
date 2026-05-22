# L4 / session-management — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

Explicit per-login session record with forensic metadata (raw IP/UA stored as `@JsonIgnore` columns; only masked/summarized forms reach DTOs — R38 PII rule). `SessionRecord` is idempotent on `UNIQUE(user_id, jti)`. Lifecycle: `ACTIVE → REVOKED` via sole-mutator service; status flip + `revokedAt` + `revokedByUserId`. `SessionRevocationCheck` SPI is fail-closed: an unknown jti is treated as revoked. Max-sessions enforcement auto-revokes the oldest ACTIVE.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/sessionmanagement/`
- Spec: [`specs/session-management-l0.yaml`](../../../specs/session-management-l0.yaml) — 14 items / 4 families (LIFECYCLE × 5, REVOCATION × 3, INTROSPECTION × 3, AUTHZ × 3)
- Tests: `./gradlew testSessionManagement` — GREEN (23/23 incl. iter1 dogfood closure)
- Anchored generic rules (R38):
  - [`practices/rules/pii-masked-at-dto-boundary.md`](../../../practices/rules/pii-masked-at-dto-boundary.md) — IP masked + UA summarized at DTO layer
  - [`practices/rules/soft-delete-audit-trail.md`](../../../practices/rules/soft-delete-audit-trail.md) — status-flip preserves who-revoked-what-when

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.sessionmanagement` to your project's `<base>.sessionmanagement`.
2. Copy `specs/session-management-l0.yaml` for the contract surface.
3. Wire the `SessionRevocationCheck` SPI into your JWT filter — fail-closed default MUST be preserved.
4. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
