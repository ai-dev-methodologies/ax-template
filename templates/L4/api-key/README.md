# L4 / api-key — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R40 promoted, 2026-05-24). R39 shipped this domain as a backend-only stub; R40 added the Next.js admin tree (`app/layout.tsx`, `app/page.tsx`, `app/providers.tsx`, `app/(api-key)/layout.tsx`, `app/(api-key)/page.tsx`) — demonstrating the stub → full-trio upgrade path that the remaining 6 R39 stubs will follow.

## Domain summary

API key issuance + verification for machine-to-machine authentication. SHA-256-hashed secret storage with `MessageDigest.isEqual` constant-time comparison; `ACTIVE / REVOKED` lifecycle with atomic rotate; `READ / WRITE` scope grants; servlet filter (`X-API-Key`) wired explicitly into the security chain (NOT auto-registered) to interop with the JWT-first auth posture.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/apikey/`
- Spec: [`specs/api-key-l0.yaml`](../../../specs/api-key-l0.yaml) — 12 items / 4 families (AUTHN, STORAGE, LIFECYCLE, AUTHZ)
- Tests: `./gradlew testApiKey` — GREEN (16/16)
- Anchored generic rules (R38):
  - [`practices/rules/pii-masked-at-dto-boundary.md`](../../../practices/rules/pii-masked-at-dto-boundary.md) — plaintext secrets never returned after creation
  - [`practices/rules/http-delete-idempotency-rfc9110.md`](../../../practices/rules/http-delete-idempotency-rfc9110.md) — DELETE on absent key returns 204

## Frontend (R40 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Root redirect to `/api-key` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack Query v5) |
| `app/(api-key)/layout.tsx` | Route-group layout: AppShell + Sidebar (ROLE_ADMIN gate slot) |
| `app/(api-key)/page.tsx` | **List page** — paginated `ApiKeySummary` table with prefix + scope + status + last-used |
| `next.config.ts` | Minimal Next.js config with API proxy + security headers |

PII contract at the UI layer (anchors `practices/rules/pii-masked-at-dto-boundary.md`):

- The `ApiKeySummary` DTO surfaces `prefix` (first ~8 chars) but **never** the plaintext secret.
- The plaintext appears exactly once — at the `/api-key/new` create-flow response — and is never re-fetchable. The backend stores only `SHA-256(secret)`.
- DELETE on a missing key returns 204 (R38 RFC 9110 §9.3.5 rule); the UI should treat 204 as success regardless of prior state.

`/api-key/new` (plaintext-once create flow) and `/api-key/[id]` (rotate / revoke) are intentionally NOT yet shipped — R40 demonstrates the list surface as the upgrade-path proof; the create + detail flows are good next-PR candidates.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.apikey` to your project's `<base>.apikey`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(api-key)` route group).
3. Copy `specs/api-key-l0.yaml` for the contract surface.
4. Wire `ApiKeyAuthenticationFilter` into your Spring Security chain as a `FilterRegistrationBean` with `setEnabled(false)` (to suppress servlet auto-registration before `SecurityContextHolderFilter`).
5. Add a ROLE_ADMIN gate to `app/(api-key)/layout.tsx` (the Spring `@PreAuthorize` is the source of truth — UI gate is a defense-in-depth).
6. If your composition declares `tenant_model: multi`, adopt one of the `MULTI-TENANT-ISOLATION-00{1,2,3}` modes before production.
