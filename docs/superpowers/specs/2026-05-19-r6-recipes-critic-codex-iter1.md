# Codex Critic R6 iter 1

## Verdict

ITERATE.

The R6 premise is directionally valid: recipes remain composition-only, Tier-1/Tier-2 counts stay frozen, and the PRD keeps the work inside existing L4/L2/L3 surfaces. It is not execution-ready.

The architect's 2 HIGH + 4 MEDIUM concerns stand. My independent checks found one stronger blocker than stated: `specs/crud-l0.yaml` does not exist at all, so `specs/crud-l0.yaml#CRUD-VALIDATION-002` is not merely a missing ID. `specs/audit-log-l0.yaml` and `specs/payment-l0.yaml` exist, but `AUDIT-EMIT-001` and `PAYMENT-LIFECYCLE-003` do not.

Specific verification requested:

```text
specs/crud-l0.yaml#CRUD-VALIDATION-002: FAIL - specs/crud-l0.yaml is missing
specs/audit-log-l0.yaml#AUDIT-EMIT-001: FAIL - file exists, ID absent
specs/payment-l0.yaml#PAYMENT-LIFECYCLE-003: FAIL - file exists, ID absent
```

I agree with Synthesis-A's trim pressure. I would trim to 3 for this cycle unless the planner can repair all blockers with R5-level evidence rigor. I accept the architect's suggested `booking + marketplace + b2b-admin` only as a candidate shortlist, not as approval-ready content; those three still need disk-verified spec refs, live evidence capture, and per-recipe TDD anchors.

## Architect findings disposition (6)

1. HIGH - Non-existent spec refs: STANDS. `CRUD-VALIDATION-002` is worse than an absent anchor because `specs/crud-l0.yaml` itself is absent. Existing CRUD backend IDs appear in `specs/crud-security.yaml` as `CRUD-AUTH-1`, `CRUD-VAL-1`, `CRUD-VAL-2`, `CRUD-PAG-1`, `CRUD-DEL-1`, and `CRUD-AUD-1`. `AUDIT-EMIT-001` is absent; audit IDs are `AUDIT-RECORD-*`, `AUDIT-LIST-*`, `AUDIT-RETENTION-*`, `AUDIT-EXPORT-*`, `AUDIT-PII-001`. `PAYMENT-LIFECYCLE-003` is absent; likely nearest family is `PAYMENT-STATE-*` or `PAYMENT-REFUND-*`.

2. HIGH - Korean refs proactively classified `internal_design`: STANDS. The PRD has one proactive `internal_design` Korean anchor in every recipe row and no recorded WebFetch attempts in the draft. R5 iter 3 required exact verbatim fidelity or a downgrade after fetch failure. R6 moves downgrade before evidence collection, lowering density.

3. MEDIUM - Composition disjointness contradicted by data: STANDS. `crud`, `audit-log`, `notification`, and `feature-flags` recur across clusters. The chosen grouping is a logical taxonomy, not disjoint composition. This must be renamed and paired with an append-only sorted `applied_recipes:` mutation strategy.

4. MEDIUM - Q1 is blocker plus "pre-commit no new SP": STANDS. The PRD calls Q1 a blocker for SP39 and also hides the rule/guard migration inside a pre-commit step. Current `recipe_governance_guard.sh` only greps for the literal `applied_recipe:` field; it does not validate a list-valued `applied_recipes:` contract or pattern membership. This needs an atomic prerequisite SP or a disk-backed proof that no code change is needed.

5. MEDIUM - SP42 7-verdict fan-out untested/no failure policy: STANDS. R5 has 3 sealed verdict files. R6 proposes 7 new verdicts in one final SP. The only fallback says rerun in the same SP or split SP43, but does not define tag semantics, partial-pass state, or whether `v1.4.0-recipes-complete` can ship with verdict-pending recipes.

6. MEDIUM - Per-recipe TDD anchors missing: STANDS. The matrix has one RED/GREEN per SP cluster, not per recipe. `booking`, `lms`, and `cms` share one guard RED in SP39, which repeats the R5 gap rather than fixing it.

## Criterion findings (A-L)

A. Principle-Option consistency: FAIL. The principles say "no half-shipped recipes" and "composition disjointness"; the pre-mortem later allows "all-or-survivors" per cluster, and cluster data is not disjoint. Option C therefore conflicts with its own principles.

B. Fair alternatives (5 options A-E): PASS-WEAK. Five options exist and Option E acknowledges a 3-recipe trim, but it rejects trim mostly by interpreting "계속 go" as "all 7 now." That is weaker than R5's demand-driven standard.

C. Risk mitigation clarity: PARTIAL. The PRD names real risks, but mitigations are often deferrals: "verify in SP", "pre-commit no new SP", "SP43 fast-follow", or "downgrade survivors." Those are not binary gates.

D. Testable acceptance binary: PARTIAL. `/ax-verify`, `/ax-verify-domain`, guard scripts, and sealed verdict thresholds are binary. However, evidence density, scheduler/webhook primitive availability, SP42 partial failure, and per-recipe TDD are not binary enough yet.

E. Concrete verification / `verify_skill` exists: PASS-WEAK. `skills/ax-verify/SKILL.md` exists and defines `bash skills/ax-verify/scripts/run-all.sh` as the full-suite binary signal. `skills/ax-verify-domain/SKILL.md` also exists. Weakness: the PRD references `recipe_spec_referential_integrity_guard.sh` but I did not find it under `practices/evals/` in this working tree; if it is expected from R5, the PRD must cite its actual path or include it as a prerequisite.

F. TDD anchor concreteness per recipe: FAIL. Need one row per recipe: failing test or guard fixture path, expected RED reason, first GREEN command, and target sealed-verdict score. Cluster-level RED/GREEN is not enough.

G. Pre-mortem adequacy: PARTIAL. There are 4 scenarios, meeting the visible count. They are not 4 scenarios per major PRD claim, and scenario 1 contradicts SP atomicity by allowing "all-or-survivors." Scenario 3 does not define verdict-N failure state.

H. Expanded test plan: PARTIAL. The matrix is useful, but not expanded enough for 7 recipes. Missing: per-recipe referential-integrity cases, evidence snapshot validation, `applied_recipes:` parser fixtures, SP42 failure fixtures, and scheduler/webhook primitive checks for LMS/CMS/internal-it.

I. Architect findings disposition: FAIL until fixed. All 6 architect findings stand.

J. CLAUDE.md anti-patterns: PASS-WEAK. The PRD avoids `RECIPE_DEVIATION.md`, MockMvc, release-policy enforcement, and new CI/git workflow control. Weakness: the plan risks governance churn by creating 10 ADR placeholders and moving a known blocker into a "pre-commit" side path. Keep ADRs concise and only decision-bearing.

K. Autonomous safety: PARTIAL. No destructive ops and per-SP rollback are good. But "all-or-survivors" undermines atomicity, and final tag/PR behavior is unclear if SP42 partially fails. The stop condition for Q1 after 3 cycles is acceptable only if Q1 is not known before start; here it is already known.

L. Independent steelman: The strongest case for all 7 is that R5 already preserved them as named deferred recipes, the current catalog has the L4/L2/L3 primitives needed for most composition maps, and `/ax-scaffold business <pattern>` becomes more valuable when the receiver can choose from a complete menu. The PRD also keeps the right product boundary: recipes do not ship business logic, they guide AI implementation. That steelman fails today because the evidence and verification contracts are below the R5 bar.

## My steelman

If I were defending the planner, I would argue that R6 is not speculative product expansion; it is backlog absorption. The 7 recipes were already listed, the user asked to continue, and a composition kit gets more useful when common business shapes are available by name. The plan also correctly rejects new L4 domains and new skill surfaces, preserving the ax-template architecture.

The best version of the all-7 plan is Synthesis-B, not the current draft: add an atomic Q1 prerequisite, fetch or downgrade Korean refs before PRD final, fix every spec_ref, rename clustering, add per-recipe TDD anchors, and split or policy-gate SP42.

The best version of the next cycle is Synthesis-A: ship 3 high-evidence recipes, keep 4 deferred with refreshed triggers, and preserve R5's trim-before-ship standard. This is the path I recommend unless "all 7 now" is an explicit non-negotiable requirement.

## Hard blockers

1. Fix all broken spec refs before approval. Current blockers: `specs/crud-l0.yaml#CRUD-VALIDATION-002`, `specs/audit-log-l0.yaml#AUDIT-EMIT-001`, `specs/payment-l0.yaml#PAYMENT-LIFECYCLE-003`.

2. Resolve Q1 before SP39 as an explicit atomic prerequisite. Either prove existing guards support multi-recipe values or add a small SP for rule docs, parser behavior, fixtures, and guard validation.

3. Restore R5 evidence discipline. WebFetch every claimed accessible Korean source before PRD final, capture URL/status/quote/timestamp, and downgrade only after failed fetch or documented absence. Recipes with zero verbatim external evidence should stay deferred.

4. Replace the "composition disjointness" claim. State the real rule: logical clustering with shared L4 README mutation. Add append-only, sorted `applied_recipes:` serialization and conflict handling.

5. Add per-recipe TDD anchors. Seven recipes need seven RED/GREEN anchors, not three cluster anchors.

6. Define SP42 failure semantics. Choose one: split SP42 into smaller verdict SPs, or define a manifest state such as `active-verdict-pending` and explicitly say whether the release tag can ship with that state. My recommendation: no `v1.4.0-recipes-complete` tag until all 7 verdicts pass, if all 7 remain in scope.

## Soft suggestions

- Prefer Synthesis-A for R6: `booking + marketplace + b2b-admin`, subject to the same spec/evidence/TDD repairs. Defer `community`, `lms`, `cms`, and `internal-it` with refreshed `reintroduction_trigger:` values.
- Verify `notification` scheduler/external-channel claims before using them to absorb LMS/CMS/internal-it. My local search found notification README references to UI/toast channels, but no scheduler/webhook primitive text.
- Keep ADR entries decision-bearing. Do not pre-create 10 ADR placeholders unless each records a concrete, reviewed decision.
- Add a 6-month recipe-retirement review if all 7 ship. Otherwise catalog rot has no exit path.
- Fix minor inventory hygiene: `b2b-admin` lists `audit-log-page` as L2 and notes it is L3; make the row clean before implementation.

## Re-review trigger

Send iter 2 back to critic only after:

1. Every `spec_ref:` in §4 resolves to an existing file and existing anchor, or is replaced by an explicit recipe-level invariant anchor that will be created in the same SP.
2. Korean evidence table includes fetch status, quote or downgrade rationale, and timestamp for each cited Korean source.
3. Q1 is resolved with an explicit implementation/test plan, not an open question.
4. The SP plan is updated for either Synthesis-A trim or all-7 with a defined SP42 failure policy.
5. The test plan contains per-recipe TDD anchors and parser/verdict fallback fixtures.

## ADR-ready

Not ADR-ready.

ADR candidates after iteration:

- Multi-recipe L4 membership representation: `applied_recipes:` list, backward compatibility with `applied_recipe:`, sorting, and guard semantics.
- R6 recipe scope decision: trim-to-3 versus all-7, with rejected alternative and evidence threshold.
- Sealed verdict release policy: whether recipe releases require 100% verdict pass before tagging.

Do not record ADRs for broken spec refs, unverified evidence, or placeholder recipe decisions until the PRD is internally consistent.
