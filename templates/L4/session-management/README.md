# L4 / session-management — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R41 promoted, 2026-05-24). R39 shipped this domain as a backend-only stub; R41 added the Next.js admin tree under `app/(sessions)/`. This is the second R39 stub upgraded after `api-key` (R40), and it specifically demonstrates the PII-at-DTO-boundary handling at the UI layer.

## Domain summary

Explicit per-login session record with forensic metadata (raw IP/UA stored as `@JsonIgnore` columns; only masked/summarized forms reach DTOs — R38 PII rule). `SessionRecord` is idempotent on `UNIQUE(user_id, jti)`. Lifecycle: `ACTIVE → REVOKED` via sole-mutator service; status flip + `revokedAt` + `revokedByUserId`. `SessionRevocationCheck` SPI is fail-closed: an unknown jti is treated as revoked. Max-sessions enforcement auto-revokes the oldest ACTIVE.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/sessionmanagement/`
- Spec: [`specs/session-management-l0.yaml`](../../../specs/session-management-l0.yaml) — 14 items / 4 families (LIFECYCLE × 5, REVOCATION × 3, INTROSPECTION × 3, AUTHZ × 3)
- Tests: `./gradlew testSessionManagement` — GREEN (23/23 incl. iter1 dogfood closure)
- Anchored generic rules (R38):
  - [`practices/rules/pii-masked-at-dto-boundary.md`](../../../practices/rules/pii-masked-at-dto-boundary.md) — IP masked + UA summarized at DTO layer
  - [`practices/rules/soft-delete-audit-trail.md`](../../../practices/rules/soft-delete-audit-trail.md) — status-flip preserves who-revoked-what-when

## Frontend (R41 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Root redirect to `/sessions` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack Query v5, 10s staleTime — sessions need fresh status) |
| `app/(sessions)/layout.tsx` | Route-group layout: AppShell + Sidebar (My / Admin tabs) |
| `app/(sessions)/page.tsx` | **Caller-only inventory** — own sessions with masked IP + summarized UA, per-row Revoke + global Revoke-others |
| `next.config.ts` | Minimal Next.js config with API proxy + security headers |

PII contract at the UI layer (anchors `practices/rules/pii-masked-at-dto-boundary.md`):

- The `SessionSummary` DTO carries `ipAddressMasked` (last octet stripped) and `userAgentSummary` (e.g. `"Chrome 124 on macOS"`).
- The raw IP and User-Agent strings exist on the backend entity (`SessionRecord`) as `@JsonIgnore` columns — they are stored for forensics but **never reach the wire**.
- The list page derives the caller's id from the request session, NEVER from a query parameter (anchors `practices/rules/caller-authentication-only-no-userid-param.md`).
- `revokeSession()` treats HTTP 204 as success regardless of prior state (anchors `practices/rules/http-delete-idempotency-rfc9110.md`).

The `/sessions/admin` admin force-logout view is intentionally NOT yet shipped — the caller-only inventory is the load-bearing PII demonstration; the admin view (`AdminSessionController` already exists server-side) is a follow-up PR.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.sessionmanagement` to your project's `<base>.sessionmanagement`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(sessions)` route group).
3. Copy `specs/session-management-l0.yaml` for the contract surface.
4. Wire the `SessionRevocationCheck` SPI into your JWT filter — fail-closed default MUST be preserved.
5. The masked IP / summarized UA contract is part of the audit posture; DO NOT widen the DTO to include raw values.
6. If your composition declares `tenant_model: multi`, adopt one of the `MULTI-TENANT-ISOLATION-00{1,2,3}` modes before production.
