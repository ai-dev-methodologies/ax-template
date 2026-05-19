# Codex PR #6 Review

## Verdict: REQUEST CHANGES

Two merge-blocking issues remain:

1. The committed canonical PRD is not the approved iter3 artifact described by PR #6 inputs. `docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.md` starts with "ralplan iter 2" and `Status: ITER 2`, while commit `bef2cb4` claims "ralplan 3-iter consensus APPROVED". The same PRD also claims L2 = 92 and L3 = 20, but disk and `main` both show L2 = 91 and L3 = 19.
2. The INV-005 path-c guard extension does not work in the fallback parser path. Normal PyYAML-backed `recipe_governance_guard.sh` passes, but forcing `yaml` import to raise `ImportError` makes the fallback path report 7 invariant violations because it exits each invariant block at the first indented `statement:` line before seeing `spec_ref`, `rule_ref`, or `co-shipped-rule` anchors.

## Verification gate

- PASS: `bash practices/evals/run-all-guards.sh --include-fixtures` -> 22 passed, 0 failed.
- PARTIAL/BLOCKED: `bash skills/ax-verify/scripts/run-all.sh` -> guard step passed, backend Gradle step could not run in this sandbox. Default Gradle home failed on `~/.gradle/...gradle-8.5-bin.zip.lck (Operation not permitted)`; redirected `GRADLE_USER_HOME=/private/tmp/ax-template-gradle-home` then failed with `java.net.SocketException: Operation not permitted` while resolving Gradle/network.
- PASS: `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only` -> 1 passed, tarball `dist/ax-template-catalog-d2c1e94.tar.gz`, SHA256 `a88aea62b302792fe9b8355d1082f12e952747e13ca28f292fbea5572ed9d91f`.
- PASS: `bash skills/_tests/L4/scheduler-domain.test.sh` -> 12 passed, 0 failed.
- PASS: `npx playwright test tests/recipes/community-compose.spec.ts tests/recipes/community-sanitize.spec.ts` -> 18 passed.

## PRD traceability (3 SPs)

- SP41 `d26d65c`: PASS for `templates/L4/scheduled-task/`, backend skeleton, `templates/DECISIONS.md` TD-2026-05-20-020, `skills/_tests/L4/scheduler-domain.test.sh`, scheduler sealed verdict scaffold, and Spring/Quartz evidence. `templates/L4/scheduled-task/README.md` has no top-level `applied_recipes:` key.
- SP41b `7031e45`: PASS for `recipes/community/{RECIPE.md,L4-composition.md,L2-block-recipe.md,spec-trio-template.yaml}`, `specs/recipes/community-recipe-l0.yaml` with 5 invariants, community sealed verdict PASS, 5 L4 README `community` appends, manifest community active move, and lms/cms/internal-it trigger refresh.
- SP42 `d2c1e94`: PASS for scheduler verdict PENDING -> PASS and `recipes/README.md` v1.5.0 active/deferred update. Head is tagged `v1.5.0-scheduler-community`.
- FAIL: the PRD artifact itself is stale/unapproved on disk (`ITER 2`, not iter3 APPROVED), so traceability from implementation back to the canonical approved spec is not clean.

## Critical contracts (8)

- PASS: Synthesis-B is implemented as 3 SP commits: SP41, SP41b, SP42.
- PASS: Korean ledger explicitly documents zero Korean verbatim and 5 host attempts.
- PASS: Reddit external upgrade is present via `github.com/reddit-archive/reddit/wiki/API` with OAuth2 and 60 requests/minute verbatim.
- PASS: scheduler evidence includes Spring Scheduling and Quartz verbatim.
- PASS: 5 community invariants disk-resolve: `AUDIT-RECORD-001`, `SEARCH-AUTHZ-001`, `NOTIF-PREF-001`, `ASVS-V2.2.1`, `practices/rules/idempotency-key-on-mutations.md`, and `co-shipped-rule: community-html-sanitization` with `frontend/tests/recipes/community-sanitize.spec.ts`.
- PASS: L4 count is 10 -> 11 with `templates/L4/scheduled-task`.
- PASS: active recipes are 6 -> 7 and deferred recipes are 4 -> 3.
- PASS: Tier-1/Tier-2 caps are not touched; no new L1/L2/L3 rows are added by the PR.

## INV-005 path-c verification

- PASS: `bash practices/evals/recipe_spec_referential_integrity_guard.sh` reports all 7 recipe specs passing, including all 6 prior recipes plus community.
- PASS: `bash practices/evals/recipe_governance_guard.sh` reports all 7 active recipes and all invariant sets passing in the normal environment.
- PASS: `co-shipped-rule: community-html-sanitization` plus `invariant_test: frontend/tests/recipes/community-sanitize.spec.ts` is accepted when PyYAML is available.
- FAIL: `recipe_governance_guard.sh` fallback parser path is not backward-compatible. With PyYAML shadowed to force `ImportError`, it fails all 7 recipe specs before reaching invariant anchors. This violates the required "PyYAML + fallback parser both" acceptance check.
- PASS: no `practices/rules/community-html-sanitization.md` file exists.

## Anti-pattern check (5)

- PASS: no MockMvc additions in `main..HEAD`.
- PASS: no `.github` changes and no deployment/release/CI policy files added; tag references are catalog-release documentation only.
- PASS: no new `practices/rules/*.md` files.
- PASS: no `RECIPE_DEVIATION.md`.
- PASS: composition kit shape is preserved: scheduler is a self-named L4 extension axis; community composes existing L4s. No `applied_recipes: []` empty-array literal was added.

## Branch hygiene

- PASS: branch head is `d2c1e94` on `feat/r7-scheduler-community`, tag `v1.5.0-scheduler-community`.
- PASS: PR diff changes 24 files and does not include `.env`, `.env.local`, `.github`, API keys, OAuth secrets, private keys, or token-looking literals. Broad secret scan false positives were documentation words such as `disk-verified`, not credentials.
- NOTE: the local worktree contains unrelated pre-existing generated/untracked files outside this review artifact; I did not modify or revert them.

## My independent attack (one)

The PR's canonical planning artifact is internally inconsistent with both the user-provided PR input and disk reality: it is committed as `ITER 2`, not approved iter3, and it records unchanged L2/L3 counts as 92/20 while both `main` and head show 91/19. That is a claimed-vs-actual audit defect independent of the scheduler/community implementation files.

## Merge recommendation

REQUEST CHANGES.

Before merge:

1. Replace or amend the PRD file so it is the actual R7 iter3 APPROVED artifact and corrects the L2/L3 count claims.
2. Fix and test the `recipe_governance_guard.sh` fallback parser so the co-shipped-rule path and the six prior recipe specs pass without PyYAML.
3. Re-run `run-all-guards --include-fixtures`, `recipe_governance_guard.sh` with PyYAML shadowed/unavailable, `scheduler-domain.test.sh`, fork-receiver bundle, and `ax-verify all` in an environment where Gradle can access its wrapper/dependency cache.
