# Codex PR #4 Review

## Verdict: BLOCK

The PR is close, but not merge-ready. One critical contract is broken: recipes wire `search`, but the `search` L4 has no `README.md` or Spec Trio metadata declaring `applied_recipe`, and the governance guard skips that missing README instead of failing.

## Verification gate

- PASS: `bash practices/evals/run-all-guards.sh --include-fixtures` -> `22 passed, 0 failed`.
- PARTIAL/BLOCKED BY SANDBOX: `bash skills/ax-verify/scripts/run-all.sh`
  - Guards passed inside `ax-verify`.
  - Backend step could not execute because this sandbox forbids Gradle's local socket/file-lock behavior. First run failed on `~/.gradle` lock permission; retry with `GRADLE_USER_HOME=/private/tmp/ax-template-gradle` and copied Gradle cache failed with `java.net.SocketException: Operation not permitted`, including with `--offline --stacktrace`.
  - Frontend unit sub-step run separately passed: Vitest `8` files / `265` tests.
  - Playwright E2E sub-step run separately could not start Next.js because the sandbox forbids binding `0.0.0.0:3000` (`listen EPERM`).
- PASS: `bash skills/_tests/fork-receiver-bundle.test.sh` -> `31 passed, 0 failed`, tarball `2MB`, excluded `.git`, `frontend/.next`, `frontend/node_modules`, `.omc`, and large Spring fixtures.

## PRD traceability (4 SPs)

- SP35 present: `recipes/` infrastructure, `_MANIFEST.yaml`, `_check-anchors.sh`, 3 recipe directories, and 3 `specs/recipes/*-recipe-l0.yaml` files landed.
- SP36 present: `/ax-scaffold business <pattern> <project-name>` is implemented in `skills/ax-scaffold/scripts/new-business-recipe.sh`; dry-runs passed for `saas-subscription`, `e-commerce`, and `crm`. Tier-1 remains the existing four user surfaces by directory: `/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`.
- SP37 present but incomplete: 3 rule docs and failing fixtures exist, and the guard is wired into `run-all-guards.sh`. The live guard has a false negative for missing L4 READMEs, detailed below.
- SP38 present: 3 sealed verdicts exist:
  - `saas-subscription`: `12/12 MUST`, `8/8 SHOULD`
  - `e-commerce`: `12/12 MUST`, `7/8 SHOULD`
  - `crm`: `11/12 MUST`, `6/8 SHOULD`

## Critical contracts

- Korean evidence fidelity: PASS. Checked against source pages:
  - Toss billing citation is verbatim from `https://docs.tosspayments.com/guides/v2/billing`.
  - Toss payment-widget citation is verbatim from `https://docs.tosspayments.com/guides/v2/payment-widget`.
  - Channel Talk citation is verbatim from `https://channel.io/ko`.
- Coupang: PASS. It is `provenance_class: internal_design` with rationale, not an external verbatim claim.
- Inline override: PASS. `override_allowed:` is in each recipe frontmatter; no `RECIPE_DEVIATION.md` file found.
- Business invariant refs: PASS for existence. The referential-integrity guard validates the recipe spec refs/rule refs.
- 3 recipes only: PASS. Recipe dirs are `crm`, `e-commerce`, `saas-subscription`.
- No `/ax-verify-recipe`: PASS. No `skills/ax-verify-recipe` directory exists.
- Tier-1 cap: PASS by surface count. No new Tier-1 command was added.
- BLOCKER: `specs/recipes/e-commerce-recipe-l0.yaml:8` and `specs/recipes/e-commerce-recipe-l0.yaml:13` enable `search`; `specs/recipes/crm-recipe-l0.yaml:8` and `specs/recipes/crm-recipe-l0.yaml:12` also enable `search`. But `templates/L4/search/README.md` does not exist, and no `applied_recipe` metadata exists under the search Spec Trio. This violates `specs/spring-practices-l0.yaml:126`, which requires every participating L4 README to declare `applied_recipe: <pattern-name>`.

## Anti-pattern check

- No new MockMvc tests in the PR diff. Existing MockMvc references are already present on `main` and in upstream snapshots/fixtures; this PR does not add backend tests.
- No deployment/release/CI files added in `main..HEAD`.
- No `RECIPE_DEVIATION.md`, no 30-day WARN->HARD loop, and no `/ax-verify-recipe`.
- Composition kit preserved: no deletions in `main..HEAD`; current layer counts remain L2 `91`, L3 `19`, L4 `10`.

## Branch hygiene

- Branch/head matches expected: `feat/business-patterns-sp35-sp38` at `348c140`, tag `v1.3.0-business-patterns`; base `main` is `26de945`.
- Committed PR diff has no obvious generated artifacts, deployment files, release files, or binary bundle artifacts.
- Secret scan over `git diff main..HEAD` found no obvious API keys, private keys, bearer tokens, or `.env` additions. It did match ordinary prose containing words like `token`, not secrets.
- Local checkout is dirty with generated `frontend/.next` artifacts and prior review markdown files. Those are not in `main..HEAD`, but this checkout should not be used for a clean merge commit without cleaning or isolating local artifacts.

## My independent attack

**BLOCKING false negative in recipe governance.**

`practices/evals/recipe_governance_guard.sh:276` loops over each recipe's enabled L4 domains, but `practices/evals/recipe_governance_guard.sh:278` to `practices/evals/recipe_governance_guard.sh:280` treats a missing `templates/L4/<domain>/README.md` as `SKIP` instead of a violation. That masks the real broken state for `search`.

Evidence:

- `e-commerce` and `crm` both enable `search`.
- `find templates/L4/search -maxdepth 1 -type f` returns only `middleware.ts` and `next.config.ts`.
- `rg applied_recipe specs contracts blueprints templates/L4/search` finds no `search` applied-recipe metadata.
- Therefore the guard says green while the PRD's `business-domain-must-declare-applied-recipe` contract is not actually enforced for one enabled L4 domain.

Fix required:

- Add `templates/L4/search/README.md` with recipe metadata for both `e-commerce` and `crm`, or add an accepted Spec Trio metadata location and teach the guard to read it.
- Change `recipe_governance_guard.sh` so a missing README/metadata for an enabled L4 is a failure, not a skip.
- Add a failing fixture for "enabled L4 domain has no README/metadata" so this regression stays closed.

## Merge recommendation

Do not merge yet. Fix the `search` recipe metadata + guard false negative, then rerun `run-all-guards 22/22`, fork-receiver bundle, and the full `/ax-verify all` in an environment that permits Gradle and Playwright local socket binding.
