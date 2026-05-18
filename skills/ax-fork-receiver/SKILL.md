---
name: ax-fork-receiver
description: Bundle ax-template's catalog (templates/L1-L4 + backend + skills + rules + spec trios) into a self-contained tarball, ship to a target directory, install dependencies, and run fork-receiver smoke tests. Use when adopting ax-template in a new project.
metadata:
  priority: 1
  tier: 1
  axis: fork-handoff
  docs:
    - "templates/L1/PEER_DEPS.md"
    - "verify/fork-receiver-smoke.sh"
    - "verify/fork-receiver-full-tree-smoke.sh"
  pathPatterns:
    - 'skills/ax-fork-receiver/SKILL.md'
    - 'skills/ax-fork-receiver/scripts/'
  bashPatterns:
    - 'bash skills/ax-fork-receiver/scripts/run.sh'
    - 'bash skills/ax-fork-receiver/scripts/bundle.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-fork-receiver
    - ax-fork
    - fork-bootstrap
    - fork-receiver
    - adopt-template
  intents:
    - "I want to start a new project from ax-template"
    - "bundle the catalog as a tarball"
    - "verify fork-receiver portability before shipping"
    - "ship the template to a target directory"
    - "bootstrap a fork receiver"
  entities:
    - composition kit
    - fork
    - tarball
    - bootstrap
    - portability
---

# /ax-fork-receiver

Bundle ax-template's catalog (L1-L4 templates + backend stubs + skills + rules + Spec Trios + AGENTS.md sentinels) into a self-contained tarball, ship to a target directory, install deps, and validate portability.

This is the Tier-1 skill that closes the iter4 portability steelman: the composition kit must be consumable by external projects without manual path surgery.

## Workflow checklist (copy and check off)

- [ ] Step 1: Validate source repo is GREEN — `/ax-verify all` exit 0
- [ ] Step 2: Bundle catalog → `dist/ax-template-catalog-<sha>.tar.gz`
- [ ] Step 3: (Optional) Ship to target — `--target=<path>` extracts + installs deps
- [ ] Step 4: Run fork-receiver smoke at target — verifies imports + build pass
- [ ] Step 5: Print install instructions for fork receiver

## Bundled scripts

| Script | Purpose |
|--------|---------|
| `scripts/bundle.sh <output-tarball>` | Creates tarball from source repo |
| `scripts/ship-to.sh <tarball> <target-dir>` | Extract tarball + print install instructions |
| `scripts/smoke.sh <target-dir>` | Run all fork-receiver smoke tests at target |
| `scripts/run.sh [--bundle-only \| --target=<path>]` | Master orchestrator |

## Steps detail

### Step 1: Source GREEN check
`run.sh` calls `bash skills/ax-verify/scripts/run-all.sh` before bundling.
Skip with `--bundle-only` to build without full suite (useful in CI where only
the catalog artifacts are needed, not the backend/E2E tests).

### Step 2: Bundle
Script: `scripts/bundle.sh <output-tarball>`
Creates `dist/ax-template-catalog-<sha-of-HEAD>.tar.gz` containing:
- `templates/` (L1+L2+L3+L4+backend, with AGENTS.md sentinel)
- `skills/` (all Tier-1/2/3 skills)
- `practices/` (rules + evals + upstream; excludes large Spring portability fixtures)
- `practices-react/` (rules + upstream; excludes node_modules)
- `specs/` + `contracts/` + `blueprints/` (full Spec Trios)
- `verify/` (fork-receiver smoke scripts)
- `frontend/` config files only (`package.json`, `eslint.config.mjs`, `tsconfig.json`)
- `backend/` config files only (`build.gradle.kts`, `settings.gradle.kts`)
- `METHODOLOGY.md`, `CLAUDE.md`, `README.md`

Excluded: `.git/`, `.omc/`, `docs/superpowers/`, `frontend/.next/`,
`frontend/node_modules/`, `frontend/src/app/(auth)/`, `frontend/src/app/(authenticated)/`,
`backend/build/`, `backend/.gradle/`, `dist/`,
`practices/evals/fixtures/spring-realworld/`,
`practices/evals/fixtures/spring-petclinic/`,
`practices/evals/fixtures/spring-modulith-example/`,
`practices-react/eslint-plugin-ax/node_modules/`.

Output: tarball path printed to stdout + SHA256 of tarball.

### Step 3: Ship to target
Script: `scripts/ship-to.sh <tarball> <target-dir>`
Validates target-dir is empty (or `--force` to overwrite), extracts tarball,
prints receiver setup instructions.

### Step 4: Fork-receiver smoke
Script: `scripts/smoke.sh <target-dir>`
At target dir:
1. `bash verify/fork-receiver-smoke.sh` — L1 portability (path-leak + tsc)
2. `bash verify/fork-receiver-full-tree-smoke.sh` — L1+L2+L3+L4 tree check
3. `bash practices/evals/run-all-guards.sh --include-fixtures` — catalog guards

### Step 5: Install instructions
Printed by `run.sh` after successful smoke:
```
Fork receiver setup:
  cd <target-dir>
  npm install -g pnpm
  # Install frontend peer deps (see templates/L1/PEER_DEPS.md)
  /ax-verify-L1   # confirm L1 is portable
```

## Invocation graph
- Calls (Tier-2): `/ax-verify` (source GREEN check)
- Calls (smoke): `verify/fork-receiver-smoke.sh`, `verify/fork-receiver-full-tree-smoke.sh`
- Called by (Tier-1): user directly; also called by `skills/ax-verify/scripts/run-all.sh` (--bundle-only step)

## Acceptance (binary)
```bash
# Step 1: TDD anchor
bash skills/_tests/fork-receiver-bundle.test.sh
# Expected: exit 0

# Step 2: Bundle only (no full ax-verify)
bash skills/ax-fork-receiver/scripts/run.sh --bundle-only
# Expected: exit 0, dist/ax-template-catalog-<sha>.tar.gz created

# Step 3: Full flow (bundle + ship + smoke)
bash skills/ax-fork-receiver/scripts/run.sh --target=/tmp/fork-receiver-test
# Expected: exit 0, smoke tests pass at target

# Step 4: Integration in master orchestrator
bash skills/ax-verify/scripts/run-all.sh
# Expected: still exits 0 (ax-fork-receiver --bundle-only as final step)
```
