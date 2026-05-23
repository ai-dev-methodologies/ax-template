# L4 / activity-feed — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R44 promoted, 2026-05-24, dogfood-iterated 4 rounds with two personas — marketing-mgr heavy user P1 + Compliance Officer P2 — until both reported GREEN). R39 shipped this domain as a backend-only stub; R44 added the Next.js feed surface (caller's activity inbox with mark-read + mark-all-read + unread filter) plus the shared `use-caller-id.ts` / `parse-error.ts` modules established in R43. Fifth R39 stub upgraded after api-key (R40), session-management (R41), comment-thread (R42), approval-workflow (R43).

## Dogfood closure

R44 followed the 2-persona dogfood protocol (heavy user P1 마케팅 매니저 + Compliance Officer P2 박서윤) for 4 iter rounds. iter1 inventory: 32 findings (critical 2, high 14, medium 8, low 8). iter1–iter4 closed:

- critical band: P1-F3 (mark-all-read UI lie about scope — claimed "this page" while server marked entire account) + P2-F12 (UI fabricated readAt from `new Date()` → forensic audit-timeline tampering surface). Both eliminated.
- high frontend-fixable band: explicit Mark-read button (no row-click footgun), keyboard a11y + role + aria-labels, snapshot-and-rollback on mutation failure (no silent UI lies), unread→top secondary sort attempted then dropped after iter2 found it created within-page false trust, production throw message scrubbed (no internal path leak to Sentry/DataDog), parseError text/html fallback with PII deny-list + 120-char ceiling.
- medium band: page-param NaN guard, mark-all-read empty-page reset, youAreSubject "you" mapping, family-key invalidation pattern replacing closure-captured cache writes, typed pendingReadIds Set replacing empty-string sentinel.
- low band: dead `&& res.status !== 204` branch removed, trailing `?` in URL fixed, error banners split per mutation with Dismiss/.reset(), docstring + README synced to actual behavior.

13 backend-contract findings remained explicitly deferred: ActivityEvent.id entropy (UUID enforcement), closed metadata schema (no PII leak through `Record<string, unknown>`), audience peer leak via DTO surface (background closure on server scoping), mass-mark-read audit verb (BULK distinct from individual READ), readAt first-write-wins immutability, server-time anchor header (forensic clock-tampering guard), delegation subject visibility scoping, Cache-Control: no-store, metadata preview UI (depends on closed schema), ISO timezone enforcement, push channel (SSE/WebSocket), global unread count endpoint. Each requires a backend DTO / response-header / endpoint change outside this template's scope. Final convergence verdict: GREEN.

## Domain summary

Per-user activity inbox using the ActivityStreams 2.0 vocabulary (`actor / verb / object / audience`). Polymorphic addressing via `(object_type, object_id)`. Publication is idempotent on `UNIQUE(actor, idempotencyKey)`. Read state is a separate `ActivityRead` row per `(event, user)` (fan-out-on-read). Visibility rule: an event is visible to caller IFF `actor == caller OR audience.contains(caller)` — enforced at the service layer.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/activityfeed/`
- Spec: [`specs/activity-feed-l0.yaml`](../../../specs/activity-feed-l0.yaml) — 12 items / 4 families (PUBLISH × 3, READ × 3, MARK × 3, AUTHZ × 3)
- Tests: `./gradlew testActivityFeed` — GREEN (18/18 incl. iter1 violation proof)
- Anchored generic rules (R38):
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — `Authentication.getName()` only; no `?userId=` parameter

## Frontend (R44 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Redirect to `/activities` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack v5, staleTime 10s + refetchInterval 30s for background polling) |
| `app/(activities)/layout.tsx` | Route-group layout: AppShell + Sidebar (All activity / Unread) |
| `app/(activities)/page.tsx` | **Feed** — paginated list with per-row "Mark read" button, mark-all-read CTA (always confirms, marks the entire account), unread filter via URL |
| `app/use-caller-id.ts` | Shared session hook + sameUser helpers (R43 pattern, production hard-stop) |
| `app/parse-error.ts` | Shared RFC 9457 ProblemDetail unwrap + text/html fallback (R43 pattern) |
| `next.config.ts` | API proxy + security headers |

UI-layer enforcement of R38 generic rules:

- **caller-authentication-only-no-userid-param**: the feed query NEVER passes `?userId=`. Visibility is server-derived from `Authentication.getName()` filtered against `(actor === caller) OR (audience contains caller)`.
- **http-delete-idempotency-rfc9110** (spirit applied to mark-read): `POST /api/activities/{id}/read` is idempotent — repeated calls return the same 204 and the client treats 204 on already-read events as success.

ActivityStreams 2.0 vocabulary:
- The DTO surfaces raw verbs (`create`, `mention`, `approve`, `comment` etc.) plus a UI mapping for the common ones. Unknown verbs are surfaced verbatim rather than silently dropped — a fork-receiver extending the verb taxonomy sees their new verb appear in the UI as-is until they add a mapping.
- Polymorphic addressing: `(objectType, objectId)` + optional `(subjectType, subjectId)` mirrors the backend entity. The UI renders both inline so a third-party-on-behalf-of action stays attributable.

Mark-all-read scope: the catalog backend `POST /api/activities/mark-all-read` marks **every unread activity on the caller's account** (not just the visible page) and returns `markedCount`. iter1 dogfood (P1-F3) found that an earlier UI label said "on this page" — a falsehood that could silently wipe unread evidence outside the visible window. The current implementation confirms unconditionally with explicit copy: *"Mark every unread activity on your account as read? This cannot be undone from this UI and includes items not visible on the current page."* Korean enterprise users (the audit-trust posture P2 raised) get a hard friction point before destructive bulk action.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.activityfeed` to your project's `<base>.activityfeed`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(activities)` route group).
3. Copy `specs/activity-feed-l0.yaml` for the contract surface.
4. Replace `app/use-caller-id.ts` with your real session hook — the production hard-stop throws if you forget.
5. Replace the 30s polling in `app/providers.tsx` with SSE / WebSocket push if your stack supports it (polling is the catalog baseline).
6. Extend `verbLabel()` in `app/(activities)/page.tsx` with your domain's verbs (mention, assign, react, etc.).
7. The visibility rule (actor OR audience contains caller) is a structural invariant — do NOT widen it to "anyone can read" without re-validating the IDOR test surface.
8. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
