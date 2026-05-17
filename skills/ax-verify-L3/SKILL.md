---
name: ax-verify-L3
description: >
  Tier-2 layer-L3 verifier. Validates page template library under templates/L3/ —
  evidence frontmatter, slot-contract READMEs, and L1/L2-only import constraint.
  Auto-triggers when editing templates/L3/**. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: layer
  docs:
    - "https://nextjs.org/docs/app/building-your-application/routing/pages"
    - "https://nextjs.org/docs/app/building-your-application/rendering/server-components"
  pathPatterns:
    - 'templates/L3/**'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-L3
    - verify L3
    - verify page templates
    - verify page layout
    - L3 layer
  intents:
    - verify L3 page templates
    - check L3 slot contracts
    - run L3 import boundary check
    - validate L3 evidence
  entities:
    - L3
    - page templates
    - slot contract
    - Next.js App Router
---

# ax-verify-L3

Tier-2 layer-L3 verifier. Covers the page template library under `templates/L3/` —
7 families: list, detail, create/edit, auth, dashboard, settings, empty-state.
Each family has a slot-contract README that declares the required L2 blocks.

A key invariant: L3 templates may only import from `templates/L1/` or `templates/L2/`
or published npm packages — never from `templates/L4/`. This verifier enforces
that import constraint.

Auto-triggers exclusively on `templates/L3/**` changes.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run evidence_guard scoped to `templates/L3/**`
- [ ] Step 2: Run import-layer check — confirm no L4 imports in L3 files
- [ ] Step 3: Validate slot-contract READMEs — confirm each L3 family has a README with slot declarations
- [ ] Step 4: Run Vitest component tests for L3 page templates (if tests exist)

## Steps detail

### Step 1: evidence_guard (scoped)
Script: `practices/evals/evidence_guard.sh templates/L3/`.
Every `.tsx`/`.ts`/`.md` in `templates/L3/` must have `evidence:` frontmatter.

### Step 2: Import-layer check
Script: `skills/ax-verify-L3/scripts/check-imports.sh`.
Static-parses all `.tsx` imports under `templates/L3/`.
Fails with `ILLEGAL_IMPORT: <file> imports <target>` if any import resolves
to `templates/L4/`.
Exit 0 = all L3 imports are `templates/L1/`, `templates/L2/`, or external packages.

### Step 3: Slot-contract validation
Script: `skills/ax-verify-L3/scripts/check-slot-contracts.sh`.
For each of 7 L3 family directories, asserts a `README.md` exists and contains
a `## Slots` or `## Slot contract` section.
Exit 1 with `MISSING_SLOT_CONTRACT: <family>` on first failure.

### Step 4: Vitest (conditional)
If `frontend/tests/L3/` exists: `cd frontend && npx vitest run tests/L3/`.
If no test directory exists, step is skipped (advisory).

## Bundled scripts
- `skills/ax-verify-L3/scripts/run.sh` — orchestrates steps 1–3 (step 4 conditional); exit 0 iff 1–3 pass
- `skills/ax-verify-L3/scripts/check-imports.sh` — enforces L3 import boundary; exit 0 on pass
- `skills/ax-verify-L3/scripts/check-slot-contracts.sh` — slot contract presence check; exit 0 on pass

## Feedback loop
When slot-contract check fails: add a `README.md` with a `## Slot contract` section
to the named family directory. Declare which L2 blocks the template requires.
Halt threshold: 3 consecutive failures on the same family → the family's architecture
needs redesign; escalate.

## Invocation graph
- Calls (Tier-3): `ax-guard-evidence`
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-L3/scripts/run.sh
# Expected: exit 0
```
