# Codex PR #8 Review

## Verdict: REQUEST CHANGES

Two merge blockers:

1. `frontend/tests/recipes/internal-it-webhook-secret.spec.ts` fails 2/10 assertions. The co-shipped INV-005 test requires `envelope encryption` and `encrypted ciphertext`, but `specs/recipes/internal-it-recipe-l0.yaml:64-81` says `KMS-managed AES-256 envelope` and `encrypts via KMS-managed key` without those required phrases. Evidence: Playwright no-webServer recipe run reported 8 passed, 2 failed at `frontend/tests/recipes/internal-it-webhook-secret.spec.ts:62-73`.
2. Independent attack: webhook signing input is internally inconsistent. `specs/webhook-l0.yaml:40-47` defines `WEBHOOK-SIGN-001` as `HMAC-SHA256(secret, body)`, while `specs/webhook-l0.yaml:56-64`, `templates/L4/webhook/README.md:63-68`, and TD-025 define the sender MAC over `timestamp + "." + body`. A fork-receiver cannot satisfy both future tests for one `X-Webhook-Signature` contract.

## Verification gate

- PASS: `bash practices/evals/run-all-guards.sh` exited 0. Current script reports 9 aggregate guard passes, not the checklist's older "22+ PASS" wording.
- PASS: `bash skills/_tests/L4/scheduler-domain.test.sh` -> 12 passed, 0 failed.
- PASS: `bash skills/_tests/L4/webhook-domain.test.sh` -> 15 passed, 0 failed.
- PASS: `bash practices/evals/recipe_governance_guard.sh --fixtures`.
- PASS: `bash practices/evals/recipe_governance_guard.sh` -> all 10 recipe specs/invariants PASS.
- PASS: PyYAML-shadowed fallback, `PYTHONPATH=/private/tmp/ax-no-yaml bash practices/evals/recipe_governance_guard.sh` -> all 10 PASS.
- PASS: `bash practices/evals/recipe_spec_referential_integrity_guard.sh` -> 10/10 PASS.
- PASS: `bash skills/ax-fork-receiver/scripts/run.sh --bundle-only` -> bundle PASS.
- PASS with runner adjustment: `internal-it-compose.spec.ts` under Playwright with temporary no-webServer config -> 14 passed. Default Playwright config could not bind `0.0.0.0:3000` in this sandbox.
- FAIL: `internal-it-webhook-secret.spec.ts` under the same Playwright recipe config -> 8 passed, 2 failed.
- FAIL: `git diff --check main...feat/r9-webhook-internal-it` -> `templates/DECISIONS.md:2225: new blank line at EOF`.

## PRD traceability (3 SPs)

- SP45: present. Commit `ad823e7` adds net-new webhook Spec Trio, L4 README, backend skeleton stubs, TD-025, allowlist entry, webhook-domain test, evidence snapshot, and sealed verdict.
- SP45b: present but not fully green. Commit `51b3ab2` adds internal-it recipe trio, recipe spec, L4 README appends, webhook first-consumer annotation, TD-026/027, manifest closure, and Playwright recipe tests. The added `internal-it-webhook-secret.spec.ts` currently fails against the shipped spec.
- SP46: present. Commit `3b15f8c` adds two sealed PASS verdicts and tag `v1.7.0-webhook-internal-it`; however the failing co-shipped Playwright invariant means the PASS state is not merge-ready as-is.

## Critical contracts (12)

- PASS: webhook L4 is net-new relative to `main`; no `templates/L4/webhook`, `specs/webhook-l0.yaml`, `contracts/webhook-openapi.yaml`, or `blueprints/webhook-manifest.yaml` exists on `main`.
- PASS: RFC 2104 + ASVS V13.2.6 sender/receiver distinction is explicit in TD-025 and webhook docs.
- FAIL: HMAC contract content is inconsistent: body-only in `WEBHOOK-SIGN-001`, timestamp-body in `WEBHOOK-SIGN-002` and README implementation.
- PASS: internal-it `l2_blocks_used` has 9 entries and all resolve under `templates/L2/blocks/*.tsx`.
- PASS: INV-005 uses recipe-level `co-shipped-rule: webhook-secret-encryption`, with promotion deferred indefinitely and no new rule file.
- PASS: INV-003 cites `WEBHOOK-SIGN-001` and `WEBHOOK-RETRY-001` from the SP45 spec.
- PASS: CRUD clarification points to `specs/crud-security.yaml`, not `crud-l0.yaml`.
- PASS: webhook README has first-consumer `applied_recipes` annotation with `internal-it` and M6 pending-verdict text.
- PASS: DECISIONS format note appears before TD-025/026/027.
- PASS: TD-027 condition (c) names shipped `internal-it` plus `api-gateway-relay` forward pointer.
- PASS: Korean and English evidence quotes are preserved in evidence snapshots / recipe docs.
- PASS: active recipes are 10, deferred recipes are 0; L1/L2/L3 counts are unchanged at 49/91/20; L4 is 11 -> 12.

## Anti-pattern check (5)

- PASS: no added MockMvc usage in the PR diff.
- PASS: no deployment/CI/release workflow scope added; release/tag language is catalog metadata.
- PASS: no new `practices/rules/*.md`.
- PASS: no `RECIPE_DEVIATION.md`.
- PASS: no empty `applied_recipes: []`; composition-kit surface is preserved.

## Backward-compat regression (3)

- PASS: PyYAML-shadowed governance fallback still passes with 10 active recipe specs.
- PASS: R7 scheduler-domain holdover still passes 12/12.
- PASS: prior sealed verdict files are unchanged; only `webhook-l4-verdict.md` and `internal-it-verdict.md` are added.

## R9 milestone (queue closure)

Queue closure is structurally present: `recipes/_MANIFEST.yaml:146` has `deferred_recipes: []`, `recipes/README.md` says 10 active / 0 deferred, and the README names the R6 Synthesis-A trajectory: community in R7, lms+cms in R8, internal-it in R9.

## Branch hygiene

- PR diff has no `.env`, `.env.local`, API key, OAuth secret, private key, or obvious token additions.
- `git diff --check` fails on trailing blank line at EOF in `templates/DECISIONS.md`.
- Local worktree was dirty before review with generated `.next` artifacts and prior untracked review files; Playwright/bundle checks added more generated output. I did not treat those as PR diff contents.

## My independent attack (one)

The webhook signing contract has two incompatible MAC inputs for the same signature header. `WEBHOOK-SIGN-001` says the header is `HMAC-SHA256(secret, body)`, while `WEBHOOK-SIGN-002` says it covers `timestamp + "." + body`. The README implementation uses timestamp-body. Fix by making `WEBHOOK-SIGN-001` define the header shape and key selection without body-only input, or by updating its requirement/test notes to the timestamp-body input.

## Merge recommendation

REQUEST CHANGES. Minimum fixes before merge:

1. Update `specs/recipes/internal-it-recipe-l0.yaml` so `internal-it-webhook-secret.spec.ts` passes, or update the test only if the PRD intentionally changed the invariant wording.
2. Normalize webhook HMAC signing input across `specs/webhook-l0.yaml`, `templates/L4/webhook/README.md`, and TD-025.
3. Remove the trailing blank line at EOF in `templates/DECISIONS.md`, then rerun the failing checks.
