---
name: ax-verify-L1
description: >
  Tier-2 layer-L1 verifier. Validates the shadcn/ui primitive library under
  templates/L1/ — design token contracts, shadcn drift check, evidence frontmatter,
  and fork-receiver smoke test. Auto-triggers when editing templates/L1/**. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: layer
  docs:
    - "https://ui.shadcn.com/docs"
    - "https://nextjs.org/docs/app/building-your-application/styling"
  pathPatterns:
    - 'templates/L1/**'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-L1
    - verify L1
    - verify primitives
    - verify shadcn
    - verify design tokens
  intents:
    - verify L1 UI primitives
    - check shadcn drift
    - verify design token contracts
    - run fork-receiver smoke
  entities:
    - L1
    - shadcn/ui
    - design tokens
    - _check-shadcn-drift.sh
    - fork-receiver-smoke.sh
---

# ax-verify-L1

Tier-2 layer-L1 verifier. Covers the shadcn/ui primitive library under
`templates/L1/` — the lowest composition layer. Verifies design token contracts,
evidence frontmatter on every L1 file, shadcn component drift against the frozen
snapshot, and portability via the fork-receiver smoke test.

Auto-triggers exclusively on `templates/L1/**` changes — pathPattern is
non-overlapping with L2/L3/L4 skills per §4.14.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run evidence_guard scoped to `templates/L1/**`
- [ ] Step 2: Run `bash templates/L1/_check-shadcn-drift.sh` — snapshot drift check
- [ ] Step 3: Run `frontend/tests/L1/token-contract.spec.ts` via Vitest
- [ ] Step 4: Run fork-receiver smoke — `bash verify/fork-receiver-smoke.sh`
- [ ] Step 5: Confirm all 32 blessed shadcn components are exported from L1 index

## Steps detail

### Step 1: evidence_guard (scoped)
Script: `practices/evals/evidence_guard.sh templates/L1/`.
Every `.tsx`, `.ts`, `.md` file under `templates/L1/` must have `evidence:` frontmatter
(or an inline `// @evidence` annotation for TypeScript files — per SP3 convention).
Exit non-zero with `MISSING_EVIDENCE: <path>` on first failure.

### Step 2: shadcn drift check
Script: `templates/L1/_check-shadcn-drift.sh`.
Diffs current L1 component files against `practices-react/upstream/shadcn-registry-2026-05.snapshot.md`.
Exit 0 = no drift. Exit 1 = `DRIFT_DETECTED: <component>` on first changed component.
`time_decay_guard.sh` also runs against this snapshot; drift > 90 days flags FAIL.

### Step 3: token-contract spec
Vitest spec: `frontend/tests/L1/token-contract.spec.ts`.
Asserts each of 32 blessed shadcn components renders with `--color-*`, `--text-*`,
`--space-*` CSS variables present (no hardcoded values).
Run: `cd frontend && npx vitest run tests/L1/token-contract.spec.ts`.

### Step 4: fork-receiver smoke
Script: `verify/fork-receiver-smoke.sh`.
Creates a temp dir, copies only `templates/L1/` + minimal `package.json`, runs
`pnpm install && pnpm tsc --noEmit`. Exit 0 = L1 is self-contained.
Exit 1 with `PATH_LEAK: <path>` if any L1 file imports outside `templates/L1/`.

### Step 5: export completeness
Script: `skills/ax-verify-L1/scripts/check-exports.sh`.
Reads `templates/L1/index.ts` (or equivalent); asserts 32 named exports present.

## Bundled scripts
- `skills/ax-verify-L1/scripts/run.sh` — orchestrates steps 1–5; exit 0 iff all pass
- `skills/ax-verify-L1/scripts/check-exports.sh` — counts L1 exports; exit 0 if count == 32

## Feedback loop
When drift check fails: inspect the diff output, update the snapshot or revert the
component change. When smoke fails with `PATH_LEAK`: remove the cross-layer import
from the L1 file.
Halt threshold: 3 consecutive smoke failures → the L1 artifact has a structural
dependency problem; escalate before continuing.

## Invocation graph
- Calls (Tier-3): `ax-guard-evidence`, `ax-guard-time-decay`
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-L1/scripts/run.sh
# Expected: exit 0
```
