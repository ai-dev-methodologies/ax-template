# L4 / api-key — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree (`app/`, `next.config.ts`, `providers`) is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

API key issuance + verification for machine-to-machine authentication. SHA-256-hashed secret storage with `MessageDigest.isEqual` constant-time comparison; `ACTIVE / REVOKED` lifecycle with atomic rotate; `READ / WRITE` scope grants; servlet filter (`X-API-Key`) wired explicitly into the security chain (NOT auto-registered) to interop with the JWT-first auth posture.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/apikey/`
- Spec: [`specs/api-key-l0.yaml`](../../../specs/api-key-l0.yaml) — 12 items / 4 families (AUTHN, STORAGE, LIFECYCLE, AUTHZ)
- Tests: `./gradlew testApiKey` — GREEN (16/16)
- Anchored generic rules (R38):
  - [`practices/rules/pii-masked-at-dto-boundary.md`](../../../practices/rules/pii-masked-at-dto-boundary.md) — plaintext secrets never returned after creation
  - [`practices/rules/http-delete-idempotency-rfc9110.md`](../../../practices/rules/http-delete-idempotency-rfc9110.md) — DELETE on absent key returns 204

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

A future PR may flesh the Next.js tree out using the same shape as `templates/L4/audit-log/` (`app/layout.tsx` + `app/page.tsx` + route group + providers). For now, fork-receivers needing a UI should clone one of the existing full-trio L4 templates and adapt to this domain's REST endpoints.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.apikey` to your project's `<base>.apikey`.
2. Copy `specs/api-key-l0.yaml` for the contract surface.
3. Wire `ApiKeyAuthenticationFilter` into your Spring Security chain as a `FilterRegistrationBean` with `setEnabled(false)` (to suppress servlet auto-registration before `SecurityContextHolderFilter`).
4. If your composition declares `tenant_model: multi`, adopt one of the `MULTI-TENANT-ISOLATION-00{1,2,3}` modes before production.
