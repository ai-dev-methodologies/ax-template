# L4 / favorites-bookmarks — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**Status**: full-trio (R46 promoted, 2026-05-24, dogfood-iterated 2 rounds with two personas — heavy user 윤서아 P1 + Security Engineer 최도윤 P2 — until both reported GREEN). R39 shipped this domain as a backend-only stub; R46 added the Next.js list surface + an embeddable `FavoriteToggle` component for host-entity pages. **Last of the 7 R39 stubs to be promoted** — closes the upgrade sequence that began at R40 (api-key) and continued through R41 (session-management), R42 (comment-thread), R43 (approval-workflow), R44 (activity-feed), R45 (tag-categorization).

## Dogfood closure

R46 followed the 2-persona dogfood protocol for 2 iter rounds. iter1 inventory: 15 findings (high 1, medium 7, low 7). iter1–iter2 closed:

- **HIGH F1**: FavoriteToggle race-on-double-click. iter1's invalidate-only pattern left a stale window where a second click within the in-flight mutation re-read `data?.favorited` from cache and re-issued the same direction. iter2 closes with `onMutate` snapshot + direction-snapshotted variables + `onError` rollback + `if (busy) return` click guard.
- **MEDIUM F3+F2**: optimistic-update across both surfaces (list page remove + Toggle add/remove). Row disappears / star flips immediately; the server confirms; errors restore the snapshot.
- **MEDIUM F4**: Remove on a noted favorite triggers a confirm dialog with the note inlined — heavy users no longer lose 결재/follow-up context to a stray cleanup click.
- **MEDIUM F6**: `app/entity-key.ts` ships `assertSafeEntityRef` — defense-in-depth against `entityType`/`entityId` carrying `/`, `?`, `#`, `\0`, `\`, or a leading `.`. Wired into all toggle fetches + list page remove.
- **MEDIUM F7**: error.message no longer renders in the native `title` tooltip (over-the-shoulder/screenshare leak vector). Errors surface via a separate `role='alert'` aria-live span.
- **MEDIUM F8**: native `disabled` replaced by `aria-busy` + `aria-disabled` + `if (busy) return` click guard. Screen-reader users hear the busy state; keyboard focus order is preserved.
- **LOW**: PII deny-list extended with Korean RRN, mobile patterns, JWT, and GitHub PAT prefixes. `setInterval` replaced by `window.focus` listener. List-page `useCallerId()` consumed for its production hard-stop side-effect without a leftover variable that a future refactor could strip.

5 findings remain deferred with justification: F5 note add-form (would break the Toggle's single responsibility), F9 count display (optional prop, low priority), F11 quota actionable advice (backend ProblemDetail extension), F12 multi-device latency (TanStack default `refetchOnWindowFocus` is sufficient), F14 auth-token wiring (fork-receiver scope). Final convergence verdict: GREEN.

## Domain summary

Per-user polymorphic favorite via `(entity_type, entity_id)` + `UNIQUE(user_id, entity_type, entity_id)` for idempotent add and DB-enforced "exactly one favorite per user-target". HTTP DELETE returns `204` even when the target is absent (R38 RFC 9110 §9.3.5 idempotency rule). Global count is a separate query — favorites visibility is caller-only (R38 caller-authentication-only rule), with a quota enforced at the service layer.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/favoritesbookmarks/`
- Spec: [`specs/favorites-bookmarks-l0.yaml`](../../../specs/favorites-bookmarks-l0.yaml) — 12 items / 4 families (CRUD × 3, QUERY × 3, AUTHZ × 3, VALIDATION × 3)
- Tests: `./gradlew testFavorites` — GREEN (17/17 incl. iter1 violation proof)
- Anchored generic rules (R38) — favorites is the **canonical example** for two of them:
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — `FAV-AUTHZ-002` is the spec anchor
  - [`practices/rules/http-delete-idempotency-rfc9110.md`](../../../practices/rules/http-delete-idempotency-rfc9110.md) — `FAV-CRUD-002` is the spec anchor

## Frontend (R46 full-trio)

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Redirect to `/favorites` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack v5, staleTime 30s) |
| `app/(favorites)/layout.tsx` | Route-group layout: AppShell + Sidebar |
| `app/(favorites)/page.tsx` | **My favorites** — caller's starred entities with per-row Remove + inline note |
| `app/favorite-toggle.tsx` | Embeddable `<FavoriteToggle entityType="…" entityId="…">` for host-entity pages |
| `app/use-caller-id.ts` | Shared session hook with production hard-stop + one-shot dev warn |
| `app/parse-error.ts` | Shared RFC 9457 ProblemDetail unwrap + text/html fallback + PII deny-list |
| `next.config.ts` | API proxy + security headers |

UI-layer anchoring of R38 generic rules — this domain is the **canonical example** for two of them:

- **caller-authentication-only-no-userid-param** (`FAV-AUTHZ-002` is the spec anchor): every fetch on `/api/favorites` and `/api/favorites/check/...` is caller-derived from `Authentication.getName()`. The client never sends `?userId=`. The list page copy and the `FavoriteToggle` docblock both repeat the invariant so a fork-receiver reading either file alone learns the rule.
- **http-delete-idempotency-rfc9110** (`FAV-CRUD-002` is the spec anchor): `DELETE /api/favorites/{entityType}/{entityId}` returns 204 whether or not the row exists. `fetch().ok` covers 200-299 already — no dead-branch `&& res.status !== 204` check, matching the R44 P1-F17 lesson.

`FavoriteToggle` is the catalog's example of an embeddable cross-domain widget — a host entity (a market, an article, an org) drops `<FavoriteToggle entityType="market" entityId={market.id} />` into its detail page and the catalog wires the rest. The component owns its own `useQuery` for the check state plus a mutation that hits either POST or DELETE depending on current state.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.favoritesbookmarks` to your project's `<base>.favoritesbookmarks`.
2. Copy the `app/` tree above into your Next.js project's `src/app/` (preserving the `(favorites)` route group).
3. Copy `specs/favorites-bookmarks-l0.yaml` for the contract surface.
4. Replace `app/use-caller-id.ts` with your real session hook. The production hard-stop throws if you forget — no silent demo-user shipping.
5. The DELETE-on-absent → 204 behavior is RFC-mandated; do NOT change it to 404 (the iter1 backend violation proof would fail).
6. The caller-only listing endpoint MUST derive `userId` from `Authentication.getName()`; never accept a `?userId=` query parameter (IDOR / BOLA).
7. Drop `<FavoriteToggle entityType="…" entityId="…" />` into host-entity pages. The component handles check + add + remove with optimistic state; pass a `label` prop if you want text next to the star.
8. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
