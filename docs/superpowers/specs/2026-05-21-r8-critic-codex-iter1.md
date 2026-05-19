# Codex Critic R8 iter 1

## Verdict

**ITERATE.**

The R8 draft is structurally close and the 2-SP atomic-2 shape is defensible. I do not require Synthesis-A. However Architect H1 + M1 + M2 all still stand in this iter-1 draft, so the verdict cannot be APPROVE. I also found one new blocking issue: the §4.1/§4.2 "L2 blocks used" inventories mix L1 primitives and missing IDs into a surface that the live `recipe_spec_referential_integrity_guard.sh` resolves only as `templates/L2/blocks/<name>.tsx`.

## Architect findings disposition (3)

1. **HIGH H1 — Korean attempt count regression: STANDS.** R8 has 2 Korean attempts: Inflearn and Naver Blog. R7 iter 3 established 5 distinct Korean host attempts. R8 should meet >=5 total attempts split across lms + cms before SP43.

2. **MEDIUM M1 — Sanity scheduled-publish topic mismatch: STANDS.** The Sanity quote is `"Real-time database for structured content"`, which describes storage, not scheduled publishing. Contentful and Strapi quotes are also generic content-management/API quotes. Add one topic-relevant scheduled-publish or cron/job fetch attempt.

3. **MEDIUM M2 — INV-005 co-shipped-rule disambiguation: STANDS.** R7 `COMMUNITY-INV-005` used `co-shipped-rule: community-html-sanitization`. R8 lms/cms use existing `spec_ref` + `rule_ref` paths, which is fine, but §4.1/§4.2 should explicitly say why no co-shipped-rule is needed.

## Criterion findings (A-L)

**A. Principle-Option consistency — PASS.** The chosen atomic-2 option matches the 8 principles: no new L4/L3/L2/L1/skill, recipes compose existing surfaces, and scheduler consumption follows the R7 follow-up.

**B. Fair alternatives — PASS.** The draft evaluates lms-only, atomic-2, and split SP43/SP43b with bounded pros/cons. Rejection of split is defensible because R8 has no new L4 and no new verdict harness shape.

**C. Risk mitigation clarity — PASS.** Pre-flight, mid-flight, rollback, and 3-iteration stop conditions are named. The partial-tag path is explicit enough for planning, though I suggest tightening the failed-recipe README/list mutation wording.

**D. Testable acceptance binary — PASS.** Guards, compose specs, sealed verdict thresholds, `/ax-verify`, tag policy, and scheduler README key checks are binary.

**E. Concrete verification — PASS.** Disk checks: `templates/L4` count is 11; `skills/ax-verify`, `skills/ax-verify-domain`, `skills/ax-verify-L4`, and `skills/ax-verify-L2` exist; `skills/ax-verify-domain/scripts/run.sh` and `skills/ax-verify/scripts/run-all.sh` are executable; both recipe guards exist and currently pass on 7 active recipes.

**F. TDD anchor concreteness — PASS.** lms and cms each name a compose spec, expected RED reason, first GREEN command, and owning SP. This meets the R6/R7 per-recipe anchor standard.

**G. Pre-mortem adequacy — PASS.** DELIBERATE mode claims 4 scenarios and all 4 are real: edX 404, scheduler README first-consumer future extension, cms rich-text-editor drift, and zero-Korean-verbatim recurrence.

**H. Expanded test plan — PASS with one execution caveat.** Unit/integration/E2E/observability rows are present. Caveat is my steelman blocker below: recipe specs must not place L1 or missing IDs in `l2_blocks_used`, or the integrity guard will fail.

**I. Architect findings disposition — FAIL for iter 1.** H1/M1/M2 are not yet closed.

**J. CLAUDE.md anti-patterns — PASS.** No governance promotion process, no MockMvc-only test mandate, no fork-team git/CI/release policy enforcement, and no `RECIPE_DEVIATION.md` ceremony.

**K. Autonomous safety — PASS.** No destructive ops, rollback is per-SP, no force push/reset, failed guards halt after 3 cycles, and tag policy holds full release until sealed verdicts pass.

**L. Independent steelman attack — BLOCKING.** See next section.

## My steelman attack (one new)

**BLOCKING — §4.1/§4.2 mix L1 primitives and missing IDs into the recipe-spec L2 inventory.**

The live `recipe_spec_referential_integrity_guard.sh` resolves `l2_blocks_used:` strictly as `templates/L2/blocks/<name>.tsx`. Existing R7 precedent handles this explicitly: `specs/recipes/community-recipe-l0.yaml` excludes `rich-text-editor`, `markdown-renderer`, and `relative-time` from `l2_blocks_used`; `recipes/community/L2-block-recipe.md` documents them under "L1 primitives consumed".

R8 §4.1/§4.2 currently list these under "L2 blocks used":

- `calendar`, `date-range-picker`, `relative-time`, `rich-text-editor` are real but live under `templates/L1/components/`, not `templates/L2/blocks/`.
- `progress-bar` and `tag-input` do not exist as named L2 blocks; disk has `templates/L1/components/progress.tsx`, not `progress-bar.tsx`, and no `tag-input.tsx`.

If SP43 copies these inventories into `specs/recipes/{lms,cms}-recipe-l0.yaml`, `recipe_spec_referential_integrity_guard.sh` will fail. Iter 2 should mirror booking/community: keep only real L2 files in recipe-spec `l2_blocks_used`, and document L1 primitives separately in `L2-block-recipe.md` or a dedicated "L1 primitives consumed" subsection.

## Hard blockers

1. Add >=5 Korean attempts total split across lms + cms, with URL, HTTP/fetch result, timestamp, and downgrade/verbatim rationale.
2. Add one topic-relevant scheduled-publish/cron fetch attempt for CMS.
3. Add the co-shipped-rule vs existing-rule disambiguation paragraph for both lms and cms INV-005.
4. Fix the L2/L1 inventory contract so the planned recipe specs only include guard-resolvable L2 block IDs.

## Soft suggestions

- Clarify the evidence counter wording: §4.4 table has 10 rows if redirect and alternate Sanity 404 rows are counted, while the deliverable says "8 WebFetch attempts." This is not blocking, but use "8 logical attempts plus redirect/alternate rows" or similar.
- In the partial-tag policy, add one sentence saying what happens to `applied_recipes:` lists when exactly one recipe verdict fails. The current `active-verdict-pending` path is plausible, but a small table would avoid future ambiguity.
- If topic-relevant scheduled-publish fetches all 4xx, use a distinct `topic_relevant_internal_design` rationale so M1 closure is visible to sealed verdict reviewers.

## Re-review trigger

Re-review after iter 2 updates the draft with:

- >=5 Korean attempt ledger rows split across lms + cms.
- At least one CMS scheduled-publish/cron topic-relevant fetch attempt.
- §4.1/§4.2 INV-005 disambiguation against R7 `COMMUNITY-INV-005`.
- Corrected lms/cms L2 inventory that follows the booking/community L1 exclusion precedent.

## ADR-ready (if APPROVE)

Not ADR-ready yet because verdict is ITERATE. After the blockers close, TD-2026-05-21-022 and TD-2026-05-21-023 look ADR-ready. TD-2026-05-21-024 is also acceptable as the scheduler first-consumer convention, provided the PRD clarifies partial-verdict list handling.
