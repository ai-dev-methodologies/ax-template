# New Frontend Feature — Single-Entry Scaffold Checklist

The React/Next.js mirror of `docs/NEW-DOMAIN-CHECKLIST.md` (backend). Enforces the
frontend decomposition rules (spec: `docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md`),
mechanically checked by `@ax/eslint-plugin-ax` (the frontend "ArchUnit").

## 1. Required structure (every feature)
Create under `frontend/src/features/<feature>/`:

- [ ] **Slice directories** `features/<feature>/<slice>/` — each a cohesive unit
      (login, signup, …) with an `index.ts` barrel as its public surface.
- [ ] **Feature barrel** `features/<feature>/index.ts` — re-exports the slice barrels;
      this is the feature's published API. Outside code imports `@/features/<feature>`
      (or a slice barrel), never a slice's internal files.
- [ ] **Container / presentational split** — data fetching + side effects in a hook or
      container; pure presentation in child components.

## 2. Decomposition rules (MANDATORY — ESLint, mostly `error`)
| Rule | Enforces | Level |
|---|---|---|
| `ax/no-cross-feature-deep-import` | a feature must not deep-import another feature's internals (use its barrel / the kernel) | error |
| `ax/no-upward-layer-import` | single-direction layers: `app/` → `features/` → `components/` + `lib/` | error |
| `ax/no-feature-internal-import` | outside a feature, import only its published barrel (never a slice internal) | error |
| `ax/no-route-client-data-fetching` | a `"use client"` route (`app/**/page\|layout`) must not call `useSWR`/`useQuery`/`axios`/`fetch` — delegate to a feature hook | error |
| `ax/no-server-state-in-local-state` | don't seed `useState` from a query/SWR `.data` | warn (advisory) |
| `ax/no-god-route` | a fat `"use client"` route (> 100 lines) belongs in a feature container | warn (advisory) |

> App Router note: SERVER components (no `"use client"`) may `await fetch()` — that is the
> idiomatic server data layer and is NOT flagged.

## 3. Cross-feature / boundary
- [ ] Cross-feature reuse goes through the target's **barrel** or the shared kernel
      (`@/components/**`, `@/lib/**`, `@ax/ui`, `@ax/blocks`) — never deep into another feature.
- [ ] A genuine grandfather/composition deep-import is recorded in
      `practices-react/feature_boundary_allowlist.yaml` (exact path + owner + rationale +
      expiry + remediation_ticket); validated by `feature_boundary_allowlist_guard.sh`.

## 4. Required tests
- [ ] If you add a new ESLint rule, ship a `tests/<rule>.test.js` with **pass/fail**
      RuleTester fixtures (the `fail` fixture must trip the rule — non-vacuity).

## 5. Verify (binary)
```bash
(cd practices-react/eslint-plugin-ax && npm test)   # all rule fixtures green
cd frontend && npx eslint --config eslint.config.mjs 'src/**/*.{ts,tsx}'   # 0 errors
bash practices-react/evals/lint_own_blocks_guard.sh # catalog blocks satisfy ax/* rules
bash practices/evals/run-all-guards.sh              # incl. feature_boundary_allowlist
```

## 6. Fat-route TIER-2 debt — REMEDIATED (2026-06-08)
The 3 fat client routes that `ax/no-god-route` surfaced as advisory warnings have been
extracted into feature-slice containers, leaving each route a thin 2-line barrel re-export:
- `app/(authenticated)/dashboard/page.tsx` (239 → 2) → `features/auth/dashboard/DashboardPage.tsx`
- `app/(auth)/signup/page.tsx` (144 → 2) → `features/auth/signup/SignupPage.tsx`
- `app/(auth)/login/page.tsx` (105 → 2) → `features/auth/login/LoginPage.tsx`

Each route now `export { XPage as default } from '@/features/auth/<slice>'` — `ax/no-god-route`
warnings are 0, and the routes obey `no-feature-internal-import` (barrel-only). This is the
intended end-state: the route is the routing seam; the feature container owns the UI/logic.
Verified: tsc clean, 24 vitest tests pass, frontend 0 ax errors.

> The pattern: a route delegates to a feature container via its published barrel; the
> container (in `features/<f>/<slice>/`) is not a route file, so the size heuristic does not
> apply to it. New features should start in this shape (don't grow a fat route in the first place).
