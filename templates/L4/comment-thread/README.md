# L4 / comment-thread — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R42 promoted, 2026-05-24). R39 shipped this domain as a backend-only stub; R42 added the Next.js tree under `app/(comments)/[entityType]/[entityId]/page.tsx`. Third R39 stub upgraded after `api-key` (R40) and `session-management` (R41); this one specifically demonstrates **soft-delete masking + admin-cannot-rewrite** at the UI layer.

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

## Frontend (R42 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Redirect to a sample `(entityType, entityId)` thread |
| `app/providers.tsx` | `QueryClientProvider` (TanStack Query v5, 15s staleTime) |
| `app/(comments)/layout.tsx` | Route-group layout: AppShell + entity navigator sidebar |
| `app/(comments)/[entityType]/[entityId]/page.tsx` | **Threaded view** — recursive `CommentBranch` renderer, soft-delete mask, author-only edit, author-or-admin delete |
| `next.config.ts` | Minimal Next.js config with API proxy + security headers |

UI-layer enforcement of three R38 generic rules:

- **soft-delete-audit-trail**: deleted comments stay in the tree with body shown as `[deleted]`; the deletedAt + deletedByUserId metadata is rendered so the audit trail is visible.
- **admin-cannot-rewrite-user-content**: the `Edit` button is rendered ONLY when `callerId === authorUserId && status === ACTIVE`. Admins never see Edit even when they CAN see Delete. The Spring service is the source of truth; the UI is defense-in-depth.
- **caller-authentication-only-no-userid-param**: the caller id is consumed from the session on the server; the client never sends a `?userId=` parameter.

Reply nesting depth is visually capped at 6 levels (indent stops growing); the data model itself is unbounded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.commentthread` to your project's `<base>.commentthread`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(comments)/[entityType]/[entityId]` dynamic route).
3. Copy `specs/comment-thread-l0.yaml` for the contract surface.
4. Replace the `callerId = 'demo-user'` / `isAdmin = false` stub in the page with your real session hook.
5. Do NOT hard-delete comments — the soft-delete pattern is load-bearing for the audit posture and the reply graph.
6. Admin role MAY delete but MUST NOT edit user content; the author-only edit guard is part of the GDPR Article 5 anchored rule.
7. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
