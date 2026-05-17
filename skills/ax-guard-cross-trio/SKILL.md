---
name: ax-guard-cross-trio
description: >
  Tier-3 cross_trio guard wrapper. Thin skill wrapper around practices/evals/cross_trio_guard.sh.
  Verifies that every L4 domain's imports of L1/L2/L3 artifacts resolve to evidence-anchored files.
  New guard implemented in SP3. Invoked by Tier-2 only — NOT pathPattern-triggered.
metadata:
  priority: 4
  tier: 3
  axis: concern
  docs: []
  pathPatterns: []
  bashPatterns:
    - 'bash practices/evals/cross_trio_guard.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-guard-cross-trio
    - cross trio guard
    - L4 import check
    - verify L4 imports
    - cross-layer evidence check
  intents:
    - verify L4 imports are evidence-anchored
    - check cross-layer import integrity
    - guard against unevidenced L1/L2/L3 artifacts being consumed by L4
  entities:
    - cross_trio_guard.sh
    - L4 imports
    - evidence block
    - templates/L4
    - ZERO_SCAN
---

# ax-guard-cross-trio

Tier-3 cross_trio guard wrapper. Wraps `practices/evals/cross_trio_guard.sh`.
New guard implemented in SP3 (not a placeholder). Verifies that when an L4 domain
workload imports from `templates/L1/`, `templates/L2/`, or `templates/L3/`, each
imported file carries an `evidence:` block.

This prevents L4 workloads from depending on under-specified lower-layer artifacts.
The `evidence_guard.sh` is delegated to for the actual evidence check on each
imported file.

Domain allowlist from `practices/evals/trio_integrity_allowlist.yaml` applies:
backend-only domains are exempt (no `templates/L4/<domain>/` expected).

Zero-scan guard: if no L4 domain directory is found under `templates/L4/`, fails
with `ZERO_SCAN` (prevents silent no-op execution).

NOT pathPattern-triggered. Invoked exclusively by Tier-2 skills.

## Workflow checklist (copyable per Anthropic best-practices)

- [ ] Step 1: Run `bash practices/evals/cross_trio_guard.sh [--domain <domain>]`
- [ ] Step 2: Read output — violations are `UNEVIDENCED_IMPORT: <L4-file> → <L1/L2/L3-file>`
- [ ] Step 3: Open the named L1/L2/L3 file and add the missing `evidence:` block
- [ ] Step 4: Re-run guard — confirm exit 0

## Steps detail

### Step 1: cross_trio_guard.sh
Script: `practices/evals/cross_trio_guard.sh`.
Algorithm (per PRD §4.8.5):
1. For each L4 domain directory under `templates/L4/`:
   a. Static-parse all `.tsx` imports.
   b. For each import resolving to `templates/L{1,2,3}/`, record the imported file path.
2. For each recorded imported file: delegate to `evidence_guard.sh <file>`.
3. FAIL with `UNEVIDENCED_IMPORT: <L4-file> → <L1/L2/L3-file>` if evidence check fails.
4. Zero-scan guard: FAIL with `ZERO_SCAN` if no L4 domain directory was walked.
Optional `--domain <domain>` to scope to one L4 domain directory.
Exit 0 = all cross-layer imports are evidence-anchored.

### Step 3: Fix pattern
The fix is ALWAYS on the L1/L2/L3 file (the dependency), not on the L4 file.
Add the `evidence:` block per the format validated by `ax-guard-evidence`.

## Bundled scripts
- `skills/ax-guard-cross-trio/scripts/run.sh` — thin wrapper; passes args to `cross_trio_guard.sh`; exits with guard's exit code

## Feedback loop
When `UNEVIDENCED_IMPORT` fires: the named L1/L2/L3 file is missing evidence.
This is the same fix as `ax-guard-evidence` — add `evidence:` frontmatter to
that lower-layer artifact.
When `ZERO_SCAN` fires: `templates/L4/` has no domain subdirectory. This means
either no SP has landed L4 content yet (expected before SP7/SP8) or the directory
was accidentally deleted.
Halt threshold: if `ZERO_SCAN` fires after an L4 domain should exist → the L4
directory creation is missing from the relevant SP; escalate.

## Invocation graph
- Calls: `practices/evals/cross_trio_guard.sh` (implemented in SP3)
- Delegates to: `practices/evals/evidence_guard.sh` (per-file evidence check)
- Called by (Tier-2): `ax-verify-react`, `ax-verify-L2` (conditional), `ax-verify-L4`, `ax-verify-domain`

## Acceptance (binary)
```bash
bash practices/evals/cross_trio_guard.sh
# Expected: exit 0 on green repo (with pass/ fixtures passing)
```
