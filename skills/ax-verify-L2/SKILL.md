---
name: ax-verify-L2
description: >
  Tier-2 layer-L2 verifier. Validates feature block library under templates/L2/ —
  evidence frontmatter, Vitest component tests, and L1-only import constraint.
  Auto-triggers when editing templates/L2/**. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: layer
  docs:
    - "https://nextjs.org/docs/app/building-your-application/rendering"
    - "https://tanstack.com/query/latest/docs/framework/react/overview"
  pathPatterns:
    - 'templates/L2/**'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-L2
    - verify L2
    - verify feature blocks
    - verify auth blocks
    - verify crud blocks
  intents:
    - verify L2 feature blocks
    - check L2 imports only from L1
    - run L2 component tests
    - validate L2 evidence
  entities:
    - L2
    - feature blocks
    - TanStack Query
    - LoginForm
    - DataTable
---

# ax-verify-L2

Tier-2 layer-L2 verifier. Covers the feature block library under `templates/L2/` —
auth (6 blocks), CRUD generic (8 blocks), payment (5 blocks), practices viewer (3 blocks),
cross-cutting (4 blocks). Total ~26 blocks.

A key invariant: L2 blocks may only import from `templates/L1/` or published npm
packages — never from `templates/L3/` or `templates/L4/`. This verifier enforces
that import constraint.

Auto-triggers exclusively on `templates/L2/**` changes.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run evidence_guard scoped to `templates/L2/**`
- [ ] Step 2: Run import-layer check — confirm no L3/L4 imports in L2 files
- [ ] Step 3: Run Vitest component tests for L2 blocks
- [ ] Step 4: Run `cross_trio_guard.sh` if any L4 domain imports a changed L2 block

## Steps detail

### Step 1: evidence_guard (scoped)
Script: `practices/evals/evidence_guard.sh templates/L2/`.
Every `.tsx`/`.ts`/`.md` in `templates/L2/` must have `evidence:` frontmatter.
Exit non-zero with `MISSING_EVIDENCE: <path>`.

### Step 2: Import-layer check
Script: `skills/ax-verify-L2/scripts/check-imports.sh`.
Static-parses all `.tsx` imports under `templates/L2/`.
Fails with `ILLEGAL_IMPORT: <file> imports <target>` if any import resolves
to `templates/L3/` or `templates/L4/`.
Exit 0 = all L2 imports are `templates/L1/` or external packages.

### Step 3: Vitest component tests
`cd frontend && npx vitest run tests/L2/`.
Tests cover each block family: auth, crud, payment, practices-viewer, cross-cutting.
MSW is used for API mocking — no real backend required for L2 unit tests.

### Step 4: cross_trio_guard (conditional)
Delegate to `ax-guard-cross-trio`. Only necessary if L4 imports a changed L2 file.

## Bundled scripts
- `skills/ax-verify-L2/scripts/run.sh` — orchestrates steps 1–3 (step 4 conditional); exit 0 iff 1–3 pass
- `skills/ax-verify-L2/scripts/check-imports.sh` — static import parser; enforces L2 import boundary; exit 0 on pass

## Feedback loop
When import-layer check fails: the file named in `ILLEGAL_IMPORT` has a forbidden
cross-layer import. Remove or reroute to an L1 abstraction.
When Vitest fails: fix the component, then re-run the single failing spec file.
Halt threshold: 3 failures on the same block → review the block's L1 dependency design.

## Invocation graph
- Calls (Tier-3): `ax-guard-evidence`, `ax-guard-cross-trio` (conditional)
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-L2/scripts/run.sh
# Expected: exit 0
```
