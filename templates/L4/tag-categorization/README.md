# L4 / tag-categorization — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R45 promoted, 2026-05-24, dogfood-iterated 2 rounds with two personas — admin content-ops 정유리 P1 + read-only user 박지호 P2 — until both reported GREEN). R39 shipped this domain as a backend-only stub; R45 added the Next.js library surface (hierarchical tag tree + admin CRUD + live slug preview + role gating) plus the shared `use-caller-id.ts` / `parse-error.ts` / `slug-preview.ts` modules. Sixth R39 stub upgraded after R40 (api-key) / R41 (session-management) / R42 (comment-thread) / R43 (approval-workflow) / R44 (activity-feed).

## Dogfood closure

R45 followed the 2-persona dogfood protocol for 2 iter rounds. iter1 inventory: 12 findings (high 2, medium 4, low 6) plus 5 deferred-backend items. iter1–iter2 closed:

- **HIGH F1**: client-side slug preview Korean fallback dropped — preview returns `''` for empty-ASCII inputs and the form shows *"(server will generate a tag-XXXX slug)"*. Removes the false-confidence case where preview showed `tag-긴급` but server actually wrote `tag-a3f1`.
- **HIGH F2**: `useCallerRole` dev stub defaults to `'user'` (fail-closed) rather than `'admin'`. Admin path requires explicit `NEXT_PUBLIC_DEV_AS_ADMIN=1` opt-in. Staging deployments that forget to wire RBAC no longer expose admin UI to non-admin users.
- **MEDIUM**: parent-immutable in edit mode now shown as an explicit amber notice; color input gets a live swatch + `CSS.supports()` validity check; Add-tag dirty draft confirms before being overwritten on Edit pivot.
- **LOW**: slug column hidden for non-admin readers; empty-slug message accurately describes backend `tag-XXXX` fallback; distinct stub-warning messages per hook with one-shot console.warn.

5 backend-contract findings remain deferred: client-server slug exact parity (POST `/api/tags/preview-slug` endpoint), tag usage count on DTO, `TagHasChildrenException` enrichment with blocking child names, parent move on edit (`UpdateTagRequest` does not carry `parentTagId`), CSS color validation as a shared validation lib. Final convergence verdict: GREEN.

## Domain summary

`Tag` definitions + polymorphic `TagAttachment` via `(entity_type, entity_id)` pair, exactly mirroring the comment-thread / activity-feed polymorphism. Slugging uses NFKD normalization → ASCII filter → hyphenate, with Korean fallback when the resulting slug is empty. `ROLE_ADMIN` is required for tag-definition mutations via `@PreAuthorize`. Attachments are caller-scoped at the application layer.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/tagcategorization/`
- Spec: [`specs/tag-categorization-l0.yaml`](../../../specs/tag-categorization-l0.yaml) — 12 items / 4 families (CRUD × 4, ATTACHMENT × 3, HIERARCHY × 3, AUTHZ × 2)
- Tests: `./gradlew testTagCategorization` — GREEN (27/27 incl. iter1 violation proof — 8 deliberate-break checks)

## Frontend (R45 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Redirect to `/tags` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack v5, staleTime 60s — tag taxonomy mostly static) |
| `app/(tags)/layout.tsx` | Route-group layout: AppShell + Sidebar |
| `app/(tags)/page.tsx` | **Tag library** — hierarchical tree (parent → children, capped visual indent at 6 levels), inline create/edit form with live slug preview, role-gated admin CRUD, delete with verbatim "will be rejected if has children" warning |
| `app/use-caller-id.ts` | Shared session hook + `useCallerRole()` for the ROLE_ADMIN gate |
| `app/parse-error.ts` | Shared RFC 9457 ProblemDetail unwrap + text/html fallback + PII deny-list |
| `app/slug-preview.ts` | Client-side mirror of backend `TagSlugger` (NFKD normalize + ASCII filter + hyphenate + Korean fallback). **Preview-only — backend is source of truth.** |
| `next.config.ts` | API proxy + security headers |

UI-layer enforcement of catalog invariants:

- **ROLE_ADMIN gate** (`useCallerRole`): non-admin viewers see the tree read-only — no Create/Edit/Delete buttons render. The Spring `@PreAuthorize` on `POST/PUT/DELETE /api/tags` is the source of truth; the UI gate is defense-in-depth.
- **Delete refuses when tag has children** (R32 invariant): the confirm dialog states this verbatim, and the server's `TagHasChildrenException` surfaces via `parseError` as a readable message instead of "HTTP 409".
- **Slug preview, not slug authority**: `slug-preview.ts` runs the same NFKD+hyphenate logic as the Spring `TagSlugger` so the user can see what slug their name will produce. The form caption says *"Slug preview (server is source of truth)"* and the create response's `slug` field is what the row renders.
- **Korean fallback** (R32 backend invariant): when ASCII slugging empties the input, the client preview shows `tag-{hangul-prefix}` and the backend writes the canonical version.

Hierarchy depth caps the visible indent at 6 levels (matching R42 comment-thread); the data model is unbounded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.tagcategorization` to your project's `<base>.tagcategorization`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(tags)` route group).
3. Copy `specs/tag-categorization-l0.yaml` for the contract surface.
4. Replace `app/use-caller-id.ts` with your real session + role source. The production hard-stop in both hooks throws if you forget — no silent demo-user shipping.
5. The `TagSlugger` Korean fallback (returns a sanitized prefix when ASCII slugger empties the input) MUST be preserved on the backend — removing it breaks Korean tag input. The client `slug-preview.ts` mirrors the rule but is preview-only.
6. Tag definition mutations require `ROLE_ADMIN`; do NOT relax this without re-running `TagViolationProofTest`.
7. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
