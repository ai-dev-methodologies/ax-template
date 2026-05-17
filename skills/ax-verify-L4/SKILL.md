---
name: ax-verify-L4
description: >
  Tier-2 layer-L4 verifier. Validates domain workload directories under templates/L4/ —
  evidence frontmatter, cross_trio_guard, Playwright E2E per domain, and trio_integrity
  coverage. Auto-triggers when editing templates/L4/**. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: layer
  docs:
    - "https://nextjs.org/docs/app/building-your-application/routing"
    - "https://playwright.dev/docs/intro"
  pathPatterns:
    - 'templates/L4/**'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-L4
    - verify L4
    - verify domain workloads
    - verify auth workload
    - verify crud workload
  intents:
    - verify L4 domain workloads
    - run Playwright against L4 domain
    - check cross-trio integrity for L4
    - verify L4 evidence
  entities:
    - L4
    - domain workload
    - cross_trio_guard
    - Playwright
    - auth
    - crud
    - payment
    - practices
---

# ax-verify-L4

Tier-2 layer-L4 verifier. Covers domain workload directories under `templates/L4/`
(auth, crud, payment, practices — 4 L4-eligible domains). These are the full
assembled vertical slices: each L4 workload imports from L1, L2, L3 and wires
to the backend Spec Trio (or to static sources for `frontend_only` domains).

The critical check here is `cross_trio_guard.sh` — it verifies that every L4
import of L1/L2/L3 artifacts resolves to evidence-anchored files.

Auto-triggers exclusively on `templates/L4/**` changes.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run evidence_guard scoped to `templates/L4/**`
- [ ] Step 2: Run cross_trio_guard — verify all L4 → L1/L2/L3 imports are evidence-anchored
- [ ] Step 3: Run trio_integrity_guard — verify Spec Trio coverage for each L4 domain
- [ ] Step 4: Run Playwright E2E for each L4 domain — `npx playwright test tests/L4/`
- [ ] Step 5: Confirm zero `PATH_LEAK` in L4 (no imports outside templates/ or npm packages)

## Steps detail

### Step 1: evidence_guard (scoped)
Script: `practices/evals/evidence_guard.sh templates/L4/`.
Every `.tsx`/`.ts`/`.md` in `templates/L4/` must have `evidence:` frontmatter.

### Step 2: cross_trio_guard
Script: `practices/evals/cross_trio_guard.sh`.
Algorithm: static-parse all `.tsx` imports in each L4 domain; for each import
resolving to `templates/L{1,2,3}/`, verify the target has `evidence:`.
Zero-scan guard: fails if no L4 domain directory is found.
Exit 0 = all L4 imports are evidence-anchored.

### Step 3: trio_integrity_guard
Script: `practices/evals/trio_integrity_guard.sh`.
Reads `practices/evals/trio_integrity_allowlist.yaml`.
For each `full_trio` L4 domain: backend Spec Trio + frontend Spec Trio coverage match.
For `frontend_only` (practices): `static_source_ref` items resolve to existing files.

### Step 4: Playwright E2E
`cd frontend && npx playwright test tests/L4/`.
For each domain: tests critical user flows (auth round-trip, CRUD list/detail/create,
payment initiation, practices catalog view).

### Step 5: PATH_LEAK check (via fork-receiver smoke)
For each L4 domain: `bash verify/fork-receiver-smoke.sh --domain <domain>` (if supported).
Confirms no inter-domain file imports.

## Bundled scripts
- `skills/ax-verify-L4/scripts/run.sh` — orchestrates steps 1–4; exit 0 iff all pass
- `skills/ax-verify-L4/scripts/run-playwright-L4.sh` — scoped Playwright runner for `tests/L4/`; exit 0 on pass

## Feedback loop
When cross_trio_guard fails: the named imported file lacks evidence. Add the
`evidence:` block to that L1/L2/L3 file, not to the L4 file.
When trio_integrity fails: add or fix the Spec Trio artifact for the named domain.
Halt threshold: 3 consecutive cross_trio failures on the same import path → the
L3/L2 artifact is architecturally incomplete; escalate.

## Invocation graph
- Calls (Tier-3): `ax-guard-evidence`, `ax-guard-cross-trio`, `ax-guard-trio-integrity`
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-L4/scripts/run.sh
# Expected: exit 0
```
