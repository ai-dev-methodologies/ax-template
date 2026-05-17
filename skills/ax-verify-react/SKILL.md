---
name: ax-verify-react
description: >
  Tier-2 React/Next.js axis verifier. Runs Vitest unit tests + Playwright E2E +
  time_decay_guard and evidence_guard scoped to frontend/ and practices-react/.
  Auto-triggers when editing frontend source or React rule catalog. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: language
  docs:
    - "https://react.dev/reference/react"
    - "https://nextjs.org/docs/app"
    - "https://vitest.dev/guide/"
    - "https://playwright.dev/docs/intro"
  pathPatterns:
    - 'frontend/**'
    - 'practices-react/rules/**'
  bashPatterns: []
  importPatterns:
    - 'from "react"'
    - 'from "next/'
retrieval:
  aliases:
    - ax-verify-react
    - verify react
    - verify frontend
    - verify nextjs
    - react axis
  intents:
    - verify frontend code changes
    - run Vitest tests
    - run Playwright tests
    - check React rules compliance
  entities:
    - Vitest
    - Playwright
    - React 19
    - Next.js 16
    - practices-react
---

# ax-verify-react

Tier-2 React/Next.js axis verifier. Auto-triggers when any file under `frontend/**`
or `practices-react/rules/**` is modified. Scoped to the React/Next.js language axis
— it does not run Gradle or backend guard scripts.

Part of the 3-tier topology: called by `/ax-verify` (Tier-1); may delegate to
Tier-3 guards for evidence and time-decay checks.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run evidence_guard scoped to `practices-react/rules/**` and `templates/**`
- [ ] Step 2: Run time_decay_guard on `practices-react/upstream/**` snapshots
- [ ] Step 3: Run `npm run build` — confirm Next.js build succeeds
- [ ] Step 4: Run `npm run test -- --run` — Vitest non-watch; confirm unit tests pass
- [ ] Step 5: Run `npx playwright test` — E2E; confirm auth flow + protected routes pass
- [ ] Step 6: Run `cross_trio_guard.sh` if any L4 import changed

## Steps detail

### Step 1: evidence_guard (scoped)
Script: `practices/evals/evidence_guard.sh`
Scope: `practices-react/rules/**/*.md` and `templates/**/*.{tsx,ts,md}`.
Exit 0 = all artifacts have `evidence:` block; non-zero = path + `MISSING_EVIDENCE` on stderr.

### Step 2: time_decay_guard
Script: `practices/evals/time_decay_guard.sh`
Scope: `practices-react/upstream/**/*.snapshot.md`.
Checks `fetched_at` field; fails if any snapshot > 90 days old.

### Step 3: Next.js build
`cd frontend && npm run build`. Exit 0 = production build succeeds.

### Step 4: Vitest
`cd frontend && npm run test -- --run`. Covers unit tests in `frontend/tests/`.

### Step 5: Playwright
`cd frontend && npx playwright test`.
Critical: auth OAuth round-trip (signup → email verify → login → callback → protected route).

### Step 6: cross_trio_guard (conditional)
Run only if L4 `*.tsx` imports changed. Script: `practices/evals/cross_trio_guard.sh`.

## Bundled scripts
- `skills/ax-verify-react/scripts/run.sh` — orchestrates steps 1–5 (step 6 conditional); exit 0 iff all pass
- `skills/ax-verify-react/scripts/run-e2e.sh` — Playwright runner with test path arg; exit 0 on pass

## Feedback loop
When Vitest fails: read the failing `FAIL src/...` line. Fix the component/hook,
then re-run `npm run test -- --run <failing-file>`.
When Playwright fails: read the trace URL emitted to stderr, open the trace viewer.
Halt threshold: 3 consecutive E2E failures on the same spec file → escalate.

## Invocation graph
- Calls (Tier-3): `ax-guard-evidence`, `ax-guard-time-decay`, `ax-guard-cross-trio`
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-react/scripts/run.sh
# Expected: exit 0
```
