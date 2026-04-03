# Auth Blueprint Execution Command Matrix

This document defines the exact local command matrix and prerequisite matrix for the auth blueprint execution procedure.

## Prerequisites

1. **Runtime/Tooling**: Currently unknown/blocker. There is no local package.json, no local .github/workflows, no local build.gradle, gradlew*, tsconfig*.json, or vite.config.*.
2. **Secrets/Env**: Currently unknown/blocker. No local source specifies secrets or envs.
3. **Fixtures/Mocks**: Currently unknown/blocker.
4. **Service/Container/Browser**: Currently unknown/blocker.

## Command Matrix

Currently, the exact runnable commands are largely missing because the implementation (T3/T4) has not yet occurred. They are marked as `missing-command` blockers.

- **build**: `missing-command`
- **lint**: `missing-command`
- **type**: `missing-command`
- **test**: `missing-command`
- **verify**: `missing-command`
- **reject**: `missing-command`
- **chub**: `chub search "library" --json && chub get <id> --lang ts|js|py`

## Evidence Filename Mapping

Under `.sisyphus/evidence/`:
- `task-build.log`
- `task-lint.log`
- `task-type.log`
- `task-test.log`
- `procedure-verify.log`
- `curated-chub-results.md`

## Blockers

- T3/T4 implementation work must be completed to replace `missing-command` placeholders with actual commands.