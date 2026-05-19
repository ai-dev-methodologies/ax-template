# Codex PR #5 Review

## Verdict: REQUEST CHANGES

## Verification gate

- PASS: `bash practices/evals/run-all-guards.sh --include-fixtures` -> 22 passed, 0 failed.
- PASS: `bash skills/_tests/fork-receiver-bundle.test.sh` -> 31 passed, 0 failed; tarball 2MB and excluded `.git`, `frontend/.next`, `frontend/node_modules`, `.omc`, and large Spring fixtures.
- BLOCKING FAIL: `bash skills/ax-verify/scripts/run-all.sh` cannot complete. In this sandbox it first stops at Gradle startup because Gradle needs local network/file-lock sockets (`java.net.SocketException: Operation not permitted`). I retried with a writable `/tmp/ax-gradle-home`; Gradle still cannot bind sockets in this sandbox.
- BLOCKING FAIL independent of the Gradle sandbox: `bash skills/ax-verify/scripts/run-frontend-unit.sh` fails because Vitest loads the new Playwright recipe specs:
  - `frontend/tests/recipes/b2b-admin-compose.spec.ts:53`
  - `frontend/tests/recipes/booking-compose.spec.ts:68`
  - `frontend/tests/recipes/marketplace-compose.spec.ts:53`
  - Error: `Playwright Test did not expect test.describe() to be called here.`
  - Root cause: `frontend/vitest.config.ts:11` includes `tests/**/*.spec.{ts,tsx}`, and `frontend/vitest.config.ts:14-19` excludes L4/e2e/auth Playwright specs but not `tests/recipes/**`.

Required fix: exclude `tests/recipes/**` from Vitest or convert these recipe TDD anchors to Vitest. Then rerun `/ax-verify`.

## PRD traceability (2 SPs)

- SP39: PASS for artifact presence. Added 3 recipes (`booking`, `marketplace`, `b2b-admin`) with `RECIPE.md`, `L4-composition.md`, `L2-block-recipe.md`, `spec-trio-template.yaml`, plus 3 recipe L0 specs.
- SP39: PASS for guard fixtures. `pass_applied_recipes_plural` and `fail_applied_recipes_empty_list` exist and are covered by the 22/22 guard run.
- SP39: PASS for 7 L4 README plural migrations: `audit-log`, `auth`, `crud`, `feature-flags`, `notification`, `payment`, `search` all now carry `applied_recipes:` blocks while preserving legacy singular lines.
- SP39: FAIL for TDD integration because the new recipe spec files break the existing Vitest unit suite.
- SP40: PASS for sealed verdict thresholds: booking 11/12 MUST and 7/8 SHOULD in body, marketplace 12/12 and 7/8, b2b-admin 11/12 and 6/8.

## Critical contracts (7)

- PASS: Synthesis-A trim honored. Only 3 R6 recipes were created; `community`, `lms`, `cms`, and `internal-it` remain deferred.
- PARTIAL: Korean/WebFetch ledger has the required 2 external verbatim citations (Etsy, Stripe Connect), 1 Korean verbatim citation (channel.io), and 3 fetch-attempt downgrade rows (Naver, Booking.com, Jira). However `practices/upstream/r6-sp39-evidence-snapshot.md:54-55` is internally inconsistent: it says `total_internal_design_downgrades: 4` while the rationale lists six names. Fix this summary to match the actual required 3 downgrade rows or explicitly separate non-ledger internal design notes.
- PASS: Dual-form guard accepts singular and plural. `recipe_governance_guard.sh` has singular handling at lines 47-50 and plural handling at lines 51-60.
- PASS: Empty plural list fails. `fail_applied_recipes_empty_list` is included in fixture verification and `run-all-guards --include-fixtures` passed 22/22.
- PASS: Required `business_invariants` refs resolve to real artifacts, including `PAYMENT-STATE-002`, `AUDIT-RECORD-001/002`, `SEARCH-AUTHZ-001`, `FF-CRUD-003`, `FF-AUTHZ-001`, `ASVS-V4.2.1/V4.2.2`, `AUDIT-RETENTION-001`, `PAYMENT-REFUND-001`, `MARKETPLACE-ESCROW-LIFECYCLE-001`, and `BOOKING-INV-001`.
- PASS: `recipes/_MANIFEST.yaml` lists exactly 6 active recipes and 4 deferred recipes.
- PASS: Tier-1 cap is unchanged; no new skill directories were added. Override allowance is inline in recipe frontmatter/comments; no `RECIPE_DEVIATION.md` file was added.

## Anti-pattern check (5)

- PASS: No new MockMvc tests in the PR delta.
- PASS: No deployment/release/CI files in the PR delta.
- PASS: No new governance-loop artifact such as `RECIPE_DEVIATION.md`.
- PASS: Composition kit preserved. Recipes compose existing L4 domains; no new L1/L2/L3/L4 template directories were added.
- PASS: R5 singular `applied_recipe:` remains present in existing L4 READMEs and live `recipe_governance_guard.sh` passes.

## Branch hygiene

- PASS for PR delta: no `.env`, `.env.local`, GitHub workflow, deployment, release, or obvious secret-bearing file is added in `b5f16b4..27da628`.
- Note: the local worktree has unrelated generated `frontend/.next/**` modifications and prior review markdown files. I did not touch them.

## My independent attack (one)

Non-blocking metadata drift: `skills/_tests/sealed-verdict/booking-verdict.md:9` frontmatter says `should_score: 6`, but the rubric and verdict body at lines 93 and 99 say `SHOULD: 7 / 8`. The threshold still passes either way, but this is exactly the kind of sealed-verdict metadata drift that can confuse later automation.

## Merge recommendation

Do not merge yet. Fix the Vitest/Playwright test-runner collision for `frontend/tests/recipes/**`, clean up the evidence snapshot downgrade-count summary, and correct the booking verdict `should_score` frontmatter. Then rerun `run-all-guards --include-fixtures`, `skills/ax-verify/scripts/run-all.sh` in an environment that permits Gradle/local server sockets, and the fork-receiver bundle test.
