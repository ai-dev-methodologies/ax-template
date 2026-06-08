# Frontend Decomposition — Phases 1-3 Implementation Plan (ralplan)

- Date: 2026-06-08
- Spec: docs/superpowers/specs/2026-06-08-frontend-decomposition-rules-design.md (Phase 0 shipped 63b787d)
- Goal: implement Phase 1 (barrel normalization) + Phase 2 (TIER-1 flip) + Phase 3 (scaffold/docs).
- Consensus: Planner -> Architect -> Critic(codex). Execution opted-in by user ("phase 1~3 모두 다해").

## Grounding facts (recon)
- features/auth = 4 slices, each ONLY an index.ts; NO feature-level barrel; NO `@/features/*` imports anywhere in src.
- app/ routes: NONE call fetch/useSWR/useQuery/axios. But 3 are FAT client routes: dashboard 239 / signup 144 / login 105 lines (`"use client"`, inline form/business logic).
- practices-react/generate_agents.sh counts practices-react/rules (99), NOT eslint-plugin-ax rules -> React sentinel unaffected by new ESLint rules.

## RALPLAN-DR
### Principles
1. GREEN-on-existing or it doesn't flip. A TIER-1 rule flips to `error` only if the current tree passes; otherwise it stays advisory or the existing violations are governed with expiry (never silently break the reference app).
2. Respect the framework. Next.js App Router server components fetching via `await fetch()` is IDIOMATIC — the route-thin rule must NOT ban it. Target CLIENT data-orchestration in routes, not server-component data loading.
3. Heuristics are honest. Size/business-logic "god-route" checks are gameable/false-positive-prone -> advisory or TIER-2, never a silent block (mirrors backend god-service/state-mutator honest limits).
4. Non-vacuity. Every new rule ships pass/fail RuleTester fixtures that prove it fires.

### Decision drivers
1. The reference app has fat client routes -> a size/business-logic block would break it; only the data-fetching-delegation rule is cleanly GREEN-flippable.
2. Tiny feature surface (features/auth) -> Phase 1 barrel normalization is low-risk.
3. ESLint-rule additions don't touch the React sentinel/headline-rule-count -> only the ESLint headline count bumps.

### Viable options for Phase 2 (the contentious phase)
- A. Flip BOTH FE-ROUTE-THIN(size) + FE-STATE-BOUNDARY to error now. Rejected — size flags 3 existing fat routes (not GREEN) and Next-server-fetch ban is wrong.
- B (CHOSEN). Flip the GREEN-able core to error (no CLIENT data-fetching in route files); keep size/business-logic + state-boundary as ADVISORY (warn) with documented honest limits + a TIER-2 review note; governed-list the 3 fat routes as known debt. Matches Principle 1-3.
- C. Defer Phase 2 entirely. Rejected — leaves the spec's TIER-1 unstarted; the data-fetching-delegation core IS cleanly flippable now.

## Phase 1 — barrel normalization
- Add `frontend/src/features/auth/index.ts` re-exporting the 4 slice barrels (feature-level published surface).
- Update `practices-react/feature_boundary_allowlist.yaml` published_api.auth to include `@/features/auth`.
- Verify: barrel resolves, `feature_boundary_allowlist_guard` GREEN, frontend lint GREEN.

## Phase 2 — TIER-1 flip (Option B)
- NEW ESLint rule `no-route-client-data-fetching` (FE-ROUTE-THIN core, ERROR): in `app/**/{page,layout}.tsx`, ban `useSWR`/`useQuery`/`axios`/`useQueryClient` calls and raw `fetch(` ONLY inside a `"use client"` route file (server components may `await fetch`). Route delegates client data to a feature hook/container. GREEN now (none use these).
- NEW ESLint rule `no-server-state-in-local-state` (FE-STATE-BOUNDARY, WARN/advisory): flag `useState(<query>.data)` / copying a SWR/query result into useState. Honest limit documented; advisory because the heuristic is narrow and the signal is rare.
- Size/business-logic god-route check: NOT a rule — recorded as a TIER-2 review item + the 3 fat routes (dashboard/signup/login) listed as known debt in the spec/checklist (remediation = extract to features/auth/* containers).
- Both new rules: pass/fail RuleTester fixtures. Wire `no-route-client-data-fetching: error` + `no-server-state-in-local-state: warn` in frontend configs.

## Phase 3 — scaffold + docs
- NEW `docs/NEW-FRONTEND-FEATURE-CHECKLIST.md` (mirror of NEW-DOMAIN-CHECKLIST §1b): slice dir + index.ts barrel + cross-feature via barrel + layer direction + route delegates data + the 5 ESLint decomposition rules + allowlist usage.
- headline: bump ESLint rule count for the 2 new Phase-2 rules (11 -> 13) in README/CLAUDE; doc_headline_count GREEN.
- React sentinel: confirm practices-react AGENTS.md/SKILL.md unaffected (ESLint rules not counted there) — verify guard GREEN, no regen needed.
- grandfather remediation: none required (clean tree); the 3 fat routes are documented TIER-2 debt, not grandfathered allowlist entries (they violate no flipped rule).

## Architect amendments (consensus round 1, self-run due to Anthropic subagent rate-limit)
- **AM1 — make Phase 2 enforce the REAL smell, not a vacuous rule.** Implement a `no-god-route`
  heuristic (a `"use client"` route file whose logic exceeds a generous threshold) and GOVERN the
  3 existing fat routes in feature_boundary_allowlist.yaml (kind: god-route, expiry, ticket
  AX-FE-FAT-ROUTE) so NEW fat routes are blocked while the 3 demos are honest grandfathered debt —
  the backend governed-god-service pattern. OPEN QUESTION for the Critic: error+governed (forcing
  function, but line-count is gameable) vs advisory (Principle-3 honest heuristic). Critic decides.
- **AM2 — verify the feature barrel doesn't break the server/client boundary or Phase-0 rules.**
  After adding `features/auth/index.ts`, run the full `npm run build` + lint; a barrel re-exporting
  `"use client"` slices must not pull client code into a server context. Only wire an app/ import of
  the barrel if it stays GREEN.

## Consensus resolution (codex Critic verdict ITERATE → resolved)
The Critic decided the god-route open question: **ADVISORY, not error+governed** (line-count is a
gameable proxy; error+allowlist contradicts Principle 3 + adds governance overhead). Folded in:
- `no-god-route` ships as a real ESLint rule at **warn (advisory)** with pass/fail fixtures — it
  surfaces the 3 fat routes as WARNINGS (visible TIER-2 signal) without breaking the build or
  needing allowlist governance. Best of both: a real rule (non-vacuous), advisory level.
- Final ESLint rule count: 11 → **14** (Phase 2 adds 3: `no-route-client-data-fetching` error,
  `no-server-state-in-local-state` warn, `no-god-route` warn).
- The 3 fat routes (dashboard/signup/login) are documented TIER-2 remediation debt in
  `docs/NEW-FRONTEND-FEATURE-CHECKLIST.md` — NOT allowlist grandfather entries (they violate no
  `error` rule).

## Acceptance criteria
1. Phase 1: `@/features/auth` barrel resolves + exported; allowlist guard GREEN.
2. Phase 2: 2 new rules with passing pass/fail fixtures (plugin tests green); `no-route-client-data-fetching` at error is GREEN on existing app/ routes (0 violations); state-boundary at warn.
3. Phase 3: checklist doc exists; headline ESLint count = disk (13); React sentinel guard GREEN.
4. Whole gate: frontend lint GREEN, lint_own_blocks GREEN, run-all-guards all PASS, R25 verify-completion PASS.
5. Non-vacuity: each new rule's fail fixture trips it.

## Risks
- R1 `no-route-client-data-fetching` false-positives on a legit client route that must call a client data hook. Mitigation: scope to the data-fetching primitives only; a route needing client data should call a FEATURE hook (which encapsulates SWR) — that's the intended pattern, not a false positive.
- R2 state-boundary heuristic noise -> kept advisory (warn), not error.
- R3 editing during a running verify -> sequence all edits before R25.

## ADR
- Decision: Option B — flip the GREEN-able route-data-fetching-delegation core to error; keep size/state-boundary advisory; document fat routes as TIER-2 debt.
- Why: honors GREEN-on-existing + Next.js App Router idioms + heuristic-honesty; lands real TIER-1 enforcement without breaking the reference app.
- Consequences: god-route size enforcement remains human-review (TIER-2); 2 ESLint rules added (1 error, 1 advisory).
- Follow-ups: extract the 3 fat client routes into features/auth/* containers (separate refactor); promote state-boundary to error once a clean signal + GREEN is established.

---

## Post-audit hardening (2026-06-08 — 59-agent adversarial audit, 24 confirmed findings)
A full-system adversarial audit (6 dimensions, refute-by-default verify) ran over the
backend+frontend decomposition enforcement. Triage + response:

**FIXED (real frontend rule bypasses — HIGH):**
- Dynamic `import('@/features/B/internal')` and `require(...)` bypassed the `ImportDeclaration`-only
  checks → added `importVisitors()` (handles static + dynamic + require) to no-cross-feature-deep-import,
  no-upward-layer-import, no-feature-internal-import. Regression fixtures added.
- Renamed/aliased data-lib import (`import { useSWR as useFetch } from 'swr'`, `import http from 'axios'`)
  bypassed no-route-client-data-fetching → now tracks imported bindings name-agnostically. Fixtures added.
- `index.mjs`/`.cjs` barrel misclassified → index regex widened to `/^index(\.[mc]?[tj]sx?)?$/`.

**DOCUMENTED honest limit:** a route calling a LOCAL wrapper hook that internally uses useSWR is not
caught (needs data-flow analysis) — that wrapper IS the intended feature hook, so acceptable.

**REJECTED (audit over-reach):** "Favorite → @AggregateMember of User" is the god-aggregate fallacy —
being queried by `userId` no more makes Favorite a member of User than Order (queried by customerId) is
a member of Customer. Favorite has an independent lifecycle and is NOT in User's transactional
consistency boundary → stays @AggregateRoot. (The 4 real re-verification reclassifications were genuine
composition children inside a parent's transaction; Favorite is not.)

**TIER-2 (borderline, deferred):** "ActivityRead → member of ActivityEvent" — ActivityRead is a 2-FK
(event,user) read-receipt/read-model, not a single-parent composition child; kept @AggregateRoot pending
human review (recorded as a TIER-2 boundary-adequacy item, consistent with spec §7).

**ACCEPTED policy (MEDIUM):** uniform 2026-12-31 expiry + the shared AX-DDD-AUDITLOG-ENTITY ticket are a
single coordinated remediation window/campaign — acceptable; complexity-differentiated expiries are a
nice-to-have, not a defect.
