# Codex PR #5 RE-review

## Verdict

APPROVE.

## Issue closure (3)

1. BLOCKING closed. `frontend/vitest.config.ts:14-20` now excludes `tests/recipes/**`. Verified from `frontend/` with `npx vitest list --run`: no `frontend/tests/recipes/*.spec.ts` entries are collected.
2. PARTIAL closed. `practices/upstream/r6-sp39-evidence-snapshot.md:54-58` now splits the six internal-design evidence entries into `fetch_attempt_downgrades: 3` for Naver, Booking.com, Jira and `no_public_api_internal_design: 3` for 당근마켓, 번개장터, 토스ID.
3. NON-BLOCKING closed. `skills/_tests/sealed-verdict/booking-verdict.md:9` now has `should_score: 7`, matching the body verdict `SHOULD: 7 / 8`; PASS unchanged.

## Regression check

- `bash practices/evals/run-all-guards.sh --include-fixtures` passes: 22 passed, 0 failed.
- The three PR #5 sealed verdict thresholds remain above gate: marketplace 12/12 MUST + 7/8 SHOULD, booking 11/12 + 7/8, b2b-admin 11/12 + 6/8.
- L4 plural `applied_recipes:` READMEs still parse; the live recipe governance guard passed, and all seven migrated L4 READMEs have non-empty plural lists.
- `recipes/_MANIFEST.yaml` is unchanged in fix-cycle commit `3cef347`.

## Independent attack

No new fix-cycle issue found.

Attack target: counter split in `r6-sp39-evidence-snapshot.md` against actual recipe evidence blocks. The split is coherent:

- Fetch-attempt downgrades: booking Naver/Yanolja fetcher block, booking Booking.com ECONNREFUSED, b2b-admin Jira 3x fetch failure.
- No-public-API internal design: marketplace 당근마켓, marketplace 번개장터, b2b-admin 토스ID.

Grade: INFORMATIONAL clean check. The recipe files also include local `derives_from` internal-design composition entries, but those are not part of the six URL/API evidence entries summarized by this fix-cycle counter.

## Final reasoning

The prior blocking Vitest/Playwright collision is fixed at collection level, the evidence counter inconsistency is repaired, and the booking sealed-verdict metadata now matches the rubric body. Regression spot-checks passed and the independent attack found no new blocker.

## Merge recommendation

Safe to merge `feat/r6-recipes-sp39-sp40` into `main`.
