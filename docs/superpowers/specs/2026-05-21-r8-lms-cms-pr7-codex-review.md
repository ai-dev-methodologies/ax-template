# Codex PR #7 Review

## Verdict: REQUEST CHANGES

One merge-blocking documentation consistency issue remains. The implementation gates are otherwise green.

## Verification gate

- PASS: `bash practices/evals/run-all-guards.sh` exits 0 with 9 live checks passed.
- PASS: `bash practices/evals/run-all-guards.sh --include-fixtures` exits 0 with 22 passed, 0 failed; this matches the requested 22+ guard count.
- PASS: `bash skills/_tests/L4/scheduler-domain.test.sh` exits 0 with 12 passed, 0 failed.
- PASS: `bash practices/evals/recipe_governance_guard.sh --fixtures` exits 0; all recipe governance fixtures pass.
- PASS: `bash practices/evals/recipe_governance_guard.sh` exits 0; all 9 recipe specs pass.
- PASS: `PYTHONPATH=/tmp/ax-no-yaml bash practices/evals/recipe_governance_guard.sh` exits 0; PyYAML-shadowed fallback still passes all 9 recipe specs.
- PASS: `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exits 0; 9/9 recipe specs pass.
- PASS: `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only` exits 0; bundle PASS, tarball `dist/ax-template-catalog-ef2de9d.tar.gz`, SHA256 `452a75d62c1cfa7b6550d8552f9dda1cd2bb22e971ca9ae50f4ab7f0ba2c0f1a`.
- PASS: `cd frontend && npx playwright test tests/recipes/lms-compose.spec.ts tests/recipes/cms-compose.spec.ts` exits 0; 25 passed.
- Note: `npm test -- tests/recipes/lms-compose.spec.ts tests/recipes/cms-compose.spec.ts` is not the right harness because `frontend/vitest.config.ts` excludes `tests/recipes/**`; Playwright is the intended runner per both recipe test headers and `playwright.config.ts`.

## PRD traceability (2 SPs)

- PASS: commit set is exactly the expected R8 shape over `main@faaf87d`: `92dda19` PRD, `ebd96fc` SP43 atomic, `ef2de9d` SP44 final/tag.
- PASS: SP43 ships `recipes/lms/` and `recipes/cms/` full quartets, `specs/recipes/{lms,cms}-recipe-l0.yaml`, `frontend/tests/recipes/{lms,cms}-compose.spec.ts`, `practices/upstream/r8-sp43-evidence-snapshot.md`, manifest moves, L4 README recipe membership updates, and TD-022/023/024 in `templates/DECISIONS.md`.
- PASS: SP44 ships 2 sealed verdicts, both PASS at 11/12 MUST and 7/8 SHOULD, plus `recipes/README.md` showing 9 active and 1 deferred.
- WATCH: R8 PRD line 115 says no scheduler L4 README body rewrite, only the `applied_recipes:` key addition. The PR changes the scheduler README composition prose around lines 92-108. I am not using this alone as the blocker because the prose update tracks the new first-consumer state, but it raises the same local-doc consistency risk as the independent issue below.

## Critical contracts (8)

- PASS: L2 inventory contract is strict for lms and cms. `l2_blocks_used:` entries in both specs resolve to files under `templates/L2/blocks/<name>.tsx`; Playwright and `recipe_spec_referential_integrity_guard.sh` both verify this.
- PASS: L1 primitives are documented only in `recipes/{lms,cms}/L2-block-recipe.md` / comments and are not included in `l2_blocks_used:`.
- PASS: INV-005 disambiguation uses existing `spec_ref` + `rule_ref` anchors, with no R8 `co-shipped-rule` pattern.
- PASS: Required anchors resolve on disk across recipe docs/specs: `AUDIT-RECORD-001/002`, `SCHED-LOCK-001`, `SCHED-IDEMPOTENT-001`, `SCHED-EXECUTE-001`, `NOTIF-PREF-001`, `NOTIF-SEND-001`, `CRUD-VAL-1`, `ASVS-V4.1.1`, and `practices/rules/idempotency-key-on-mutations.md`.
- PASS: `templates/L4/scheduled-task/README.md` has `applied_recipes:` with `cms`, then `lms`.
- PASS: Korean verbatim evidence is preserved in `recipes/lms/RECIPE.md` and `recipes/cms/RECIPE.md`: classting and brunch.
- PASS: External verbatim evidence is preserved: Moodle, Coursera post-301, Sanity scheduled-publishing, Contentful, and Strapi.
- PASS: active recipes are 9; deferred recipes are 1 (`internal-it`). L1/L2/L3/L4 counts remain unchanged versus base (`git ls-tree` shows L2 still 92 tracked block files; L4 still 11 domains). No Tier-1/Tier-2 skill-cap changes were introduced.

## Anti-pattern check (5)

- PASS: no MockMvc additions in the PR diff.
- PASS: no deployment, release, CI policy, or `.github` files added.
- PASS: no new `practices/rules/*.md` files.
- PASS: no `RECIPE_DEVIATION.md` file or ceremony file added.
- PASS: no active R8 recipe/spec/template surface contains an `applied_recipes: []` empty-array literal; scheduler uses non-empty plural list `[cms, lms]` by block form.
- PASS: composition-kit boundary preserved; recipes compose existing L4s and do not introduce new L4/L3/L2/L1 surfaces.

## Backward-compat regression (2)

- PASS: PyYAML-shadowed `recipe_governance_guard.sh` still passes all 9 active recipe specs.
- PASS: R7 scheduler-domain holdover still passes 12/12.

## Branch hygiene

- PASS: branch head is `ef2de9d`, tag `v1.6.0-lms-cms` points at head.
- PASS: `git diff --check main..feat/r8-lms-cms` passes.
- PASS: PR diff does not add `.env`, `.env.local`, API keys, OAuth secrets, private keys, or password material.
- Note: local working tree has unrelated generated `frontend/.next` / build artifacts and prior untracked review files. I did not treat those as PR #7 branch content.

## My independent attack (one)

REQUEST CHANGES: `templates/L4/scheduled-task/README.md:146-149` still says a fuller scheduler backend skeleton "lands when the first downstream recipe (R8 LMS or CMS) consumes this domain." PR #7 is exactly that first-consumer event (`templates/L4/scheduled-task/README.md:92-102` lists lms/cms as the arriving consumers), but the PR deliberately ships recipes only and leaves `templates/L4/scheduled-task/backend/` with only `ScheduledTask.java.skeleton`. This is now a false forward-looking promise in the public L4 README.

Fix: update that paragraph to say the fuller backend skeleton is deferred to a future scheduler backend-expansion cycle, or remove the timing claim entirely. Do not add backend skeleton files in this PR; that would violate R8 scope.

## Merge recommendation

REQUEST CHANGES until the stale scheduler README backend-skeleton promise is corrected. After that one-line documentation fix, I would approve based on the green guards and contract checks above.
