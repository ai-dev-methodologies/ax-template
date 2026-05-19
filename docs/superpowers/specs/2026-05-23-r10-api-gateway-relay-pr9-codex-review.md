# Codex PR #9 Review

## Verdict: APPROVE

No merge blockers found.

## Verification gate

- PASS: `bash practices/evals/run-all-guards.sh` exited 0 (`9 passed, 0 failed` aggregate live guard count). `bash practices/evals/run-all-guards.sh --include-fixtures` exited 0 with the requested expanded count: `22 passed, 0 failed`.
- PASS: `bash skills/_tests/L4/scheduler-domain.test.sh` -> `12 passed, 0 failed`.
- PASS: `bash skills/_tests/L4/webhook-domain.test.sh` -> `15 passed, 0 failed`.
- PASS: `bash practices/evals/recipe_governance_guard.sh --fixtures` -> all fixture checks PASS.
- PASS: `bash practices/evals/recipe_governance_guard.sh` -> all live checks PASS, including all 11 recipe specs/invariants.
- PASS: `PYTHONPATH=/private/tmp/ax-no-yaml bash practices/evals/recipe_governance_guard.sh` -> all live checks PASS with PyYAML forced unavailable.
- PASS: `bash practices/evals/recipe_spec_referential_integrity_guard.sh` -> 11/11 recipe specs PASS.
- PASS: `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only` -> bundle PASS, tarball `dist/ax-template-catalog-a72b0a0.tar.gz`, SHA256 `8807e154fbaa8778cc1a40a97df32c0ec05dfd9237d3f969cd6320575f317504`.
- PASS: `npm --prefix frontend exec playwright test tests/recipes/api-gateway-relay-compose.spec.ts` -> `19 passed`.

## PRD traceability (2 SPs)

- PASS: SP47 atomic is present in commit `18d9384`: new `recipes/api-gateway-relay/` quartet, `specs/recipes/api-gateway-relay-recipe-l0.yaml`, Playwright TDD anchor, seven L4 README appends, sealed verdict scaffold, TD-028/TD-029, and evidence snapshot with 5 English + 2 Korean verbatim anchors.
- PASS: SP48 FINAL is present in commit `a72b0a0`: sealed verdict is `12/12 MUST, 7/8 SHOULD, PASS`; `recipes/README.md` reports 11 active recipes and deferred queue closed; tag `v1.8.0-api-gateway-relay` points at head.
- PASS: planning commit `22db3b5` adds the canonical R10 PRD, 429 lines.

## Critical contracts (10)

- PASS: no new L4. `templates/L4` directory count is 12 on both `main` and PR head.
- PASS: `deferred_recipes: []` remains present and unchanged as the PASS/FAIL policy in `_MANIFEST.yaml` and `recipes/README.md`.
- PASS: mandatory composition is `audit-log`, `auth`, `crud`, `scheduled-task`, `webhook`; optional `notification` and `feature-flags` are documented via override/add paths.
- PASS: INV-003 binds `specs/ratelimit-l0.yaml#RATELIMIT-1` and `#RATELIMIT-2`; referential guard resolves by file/ID without requiring `templates/L4/ratelimit/`.
- PASS: INV-005 uses existing anchors (`CRUD-VAL-1`, `AUDIT-RECORD-002`, `idempotency-key-on-mutations.md`); no `co-shipped-rule:` field is added in R10 artifacts.
- PASS: L2 inventory is strict. The eight `l2_blocks_used` entries all resolve to existing `templates/L2/blocks/*.tsx`.
- PASS: disambiguation sentence is preserved in `RECIPE.md`, the Playwright TDD anchor constant, PRD §7 P3/§8 TD-028, and TD-028.
- PASS: 2 Korean verbatim anchors are preserved: Toss Payments and NAVER Cloud Platform. 5 English anchors are preserved: Kong, AWS, Cloudflare API Shield, Tyk, Apigee.
- PASS: active recipes move 10 -> 11 with `api-gateway-relay`; deferred remains 0.
- PASS: Tier-1/Tier-2 skill surfaces and L1/L2/L3 inventories are unchanged relative to `main`; no `templates/L1`, `templates/L2`, `templates/L3`, or skill surface files appear in the PR diff.

## Anti-pattern check (5)

- PASS: no added `MockMvc`.
- PASS: no deployment, CI, or release workflow files are added.
- PASS: no new `practices/rules/*.md`.
- PASS: no `RECIPE_DEVIATION.md`.
- PASS: no empty `applied_recipes: []`; composition kit is preserved and there is no new L4/L3/L2/L1/skill surface.

## Backward-compat regression (3)

- PASS: PyYAML-shadowed governance fallback still passes with 11 active recipe specs.
- PASS: R7 scheduler-domain and R9 webhook-domain holdovers still pass at 12/12 and 15/15.
- PASS: prior sealed verdicts are unchanged; the only sealed-verdict diff is the new `api-gateway-relay-verdict.md`.

## TD-027 (c) retroactive closure

PASS. R10 ships `api-gateway-relay` as a recipe-only second shipped consumer of webhook L4. R9 `internal-it` remains the first consumer, and R10 closes the R9 Architect H1 self-fulfilling-risk concern without adding a new L4 in the same cycle.

## Branch hygiene

- PASS: branch head is `a72b0a0`, tag `v1.8.0-api-gateway-relay` points at head, and expected PR commits are present.
- PASS: `git diff --check main...feat/r10-api-gateway-relay` is clean.
- PASS: no `.env`, key, credential, workflow, or obvious secret-bearing file is added. Secret keyword scan only hit documentation terms such as signing-secret/token in recipe text.
- Note: local worktree was already dirty with generated `.next` artifacts and prior review docs before this review. I reviewed committed `main...feat/r10-api-gateway-relay` diff and did not treat unrelated local dirt as PR content.

## My independent attack (one)

I attacked the two easiest ways this PR could have cheated the R10 contract: treating `ratelimit-l0.yaml` as an implicit new L4, and using optional `notification` / `feature-flags` README appends to smuggle mandatory composition expansion. Both held. The ratelimit binding is spec-only and guard-compatible; optional L4s are only override/add surfaces, while the active recipe spec keeps the mandatory five-domain composition.

No new merge-blocking issue found.

## Merge recommendation

APPROVE. The R10 api-gateway-relay recipe satisfies the PRD, preserves the composition kit, keeps `deferred_recipes: []` closed, validates TD-027(c) retroactively, and passes the required regression gates.
