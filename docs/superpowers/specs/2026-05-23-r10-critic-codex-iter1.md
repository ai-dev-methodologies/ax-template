# Codex Critic R10 iter 1

## Verdict

ITERATE.

R10 is structurally close: the 2-SP recipe-only shape is consistent with R6/R8, TD-027 is honored by not adding a new L4, the requested L4 count remains 12, and the planned `l2_blocks_used` list is disk-resolvable. However Architect H1 still stands, M1-M4 should be applied, and I found one new BLOCKING issue: the fail/rollback path reopens `deferred_recipes:` despite the R10 invariant that the queue stays closed.

## Architect findings disposition

1. **H1 - Toss Payments re-use is Korean evidence-chain reach: STANDS.** The PRD has 3 Korean cloud-native source families with 0 verbatim gateway-product PASS and falls back to Toss Payments (`docs.tosspayments.com/reference`) as the only Korean verbatim row. The cloud-native downgrade is honestly documented, but Toss is still an adjacent API-consumer reference, not an API gateway/proxy/rate-limit operating reference. Add one fresh-vendor Korean adjacent attempt, as Architect requested.

2. **H2 - `ratelimit-l0.yaml` cross-cutting binding is harness-unverified: PARTIALLY STANDS, doc fix still needed.** I audited `practices/evals/recipe_spec_referential_integrity_guard.sh`: `enabled_l4_domains` checks `templates/L4/<domain>/`, but `spec_ref` only checks file existence and ID presence. That means `specs/ratelimit-l0.yaml#RATELIMIT-1/2` is compatible with the current guard even without `templates/L4/ratelimit/`. The PRD should still add this explicit pre-flight proof so future reviewers do not reopen it.

3. **M1 - Ledger arithmetic drift: STANDS.** §4.4 mixes logical attempts, raw probe rows, redirect rows, alternate rows, and adjacent fallback rows. The summary line "8 logical attempts + 3 redirect/alternate rows = 11 raw table rows" does not match the visible ledger, which includes many more raw rows once Korean alternates are counted.

4. **M2 - Same-vendor Korean reuse needs ADR follow-up: STANDS.** Toss is now doing too much precedent work across R9 and R10. Add the Korean adjacent fallback rotation follow-up to TD-028.

5. **M3 - Sealed verdict disambiguation under-specified: STANDS.** The draft says RECIPE.md will distinguish gateway composer vs webhook primitive, but should pin the exact sentence so the context-0 verdict runner sees it.

6. **M4 - INV-005 anchor count wording drift: STANDS.** Replace "5 business_invariants, ALL spec_ref/rule_ref" with "5 INVs, each with >=1 anchor; all anchors disk-resolvable."

## Criterion findings (A-L)

**A. Principle-Option consistency - PASS with one failure-path exception.** Option 1 honors TD-027 and no-L4-split discipline. Exception: the fail path reopens `deferred_recipes:` while principles and §10 say the queue stays closed.

**B. Fair alternatives - PASS.** The draft has >=2 alternatives, including explicit "new L4 + recipe same SP" rejection because it self-fulfills TD-027. Rate-limit-as-L4 is also fairly rejected for zero shipped consumers.

**C. Risk mitigation clarity - PARTIAL.** Pre-flight/mid-flight gates are concrete. The rollback/fail-state mitigation is inconsistent with the closed-queue invariant.

**D. Testable acceptance binary - PASS.** `/ax-verify`, both recipe guards, a compose spec, sealed verdict threshold, and tag/no-tag policy are binary.

**E. Concrete verification - PASS-PARTIAL.** Disk checks performed: `ls templates/L4 | wc -l` returns 12; requested invariant anchors resolve in `webhook-l0.yaml`, `audit-log-l0.yaml`, `scheduled-task-l0.yaml`, and `auth-asvs-l1.yaml`; `ratelimit-l0.yaml` has `RATELIMIT-1..4`; all 8 planned L2 block IDs exist under `templates/L2/blocks/*.tsx`; `recipes/_MANIFEST.yaml` currently has `deferred_recipes: []`. Ledger arithmetic remains partial.

**F. TDD anchor concreteness - PASS.** The anchor names the compose spec, expected RED reason, first GREEN command, and owning SP.

**G. Pre-mortem adequacy - PASS.** Four scenarios meet SHORT-mode floor and cover Korean evidence, ratelimit framing, sealed verdict disambiguation, and webhook README insertion.

**H. Expanded test plan - PASS-PARTIAL.** Unit/integration/E2E/observability surfaces are present. Add the guard-audit proof for spec-only ratelimit binding and fix fail-state manifest semantics.

**I. Architect findings disposition - ITERATE.** H1 and all MEDIUM findings need planner iter 2 edits. H2 can be downgraded by documenting the guard audit.

**J. CLAUDE.md anti-patterns - PASS.** No governance promotion loop, no MockMvc-only mandate, no fork-team git/CI policy enforcement, and recipe-no-code scope is preserved.

**K. Autonomous safety - FAIL until fail-state fixed.** No destructive ops are planned, but the rollback path creates a new manifest queue state after saying R10 must not reopen the queue. Autonomous execution needs one unambiguous stop state.

**L. Independent steelman - BLOCKING.** See next section.

## My steelman attack (one new)

**BLOCKING - R10 creates a second recipe-queue category despite saying `deferred_recipes: []` stays closed.**

The draft repeatedly states that R10 is voluntary, not an R6 trim continuation, and that `deferred_recipes: []` remains unchanged. Examples: §3 Must Have says `deferred_recipes: []` is unchanged; §9 says Deferred 0 -> 0 and queue not reopened; §10 says the queue stays closed post-SP47.

But the execution safety and binary failure policy contradict this:

- Stop condition: if `recipe_spec_referential_integrity_guard.sh` cannot reach GREEN, "recipe authored as NEW deferred-recipes entry with `blocker:` field."
- SP48 fail row: `0/1 FAIL` returns `api-gateway-relay` to a NEW `deferred_recipes:` entry.

That is not just wording drift. It creates a new category: voluntary pretrigger recipes that become deferred entries on failure, separate from the closed R6 Synthesis-A trim queue. This undermines the requested invariant that `recipes/_MANIFEST.yaml deferred_recipes: []` stays empty and gives future planners a path to repopulate the queue without a fork-receiver demand signal.

Fix: choose exactly one policy before approval. My recommendation: if R10 fails, revert SP47 and leave `api-gateway-relay` absent from both active recipes and `deferred_recipes:`. A future R11+ proposal can reintroduce it only with a fresh evidence chain and explicit trigger. If the planner wants failed voluntary recipes to become deferred, that needs an explicit ADR and should no longer claim Deferred 0 -> 0.

## Hard blockers

1. Add one fresh-vendor Korean adjacent attempt or otherwise close Architect H1 with stronger Korean gateway/proxy/relay/rate-limit evidence.
2. Resolve the `deferred_recipes:` contradiction. The closed queue cannot both stay `[]` and receive `api-gateway-relay` on fail.

## Soft suggestions

- Add one sentence proving the current recipe spec guard validates `spec_ref` by file/ID only and does not require an L4 directory for `ratelimit-l0.yaml`.
- Replace the §4.4 source-class arithmetic with counts that separate logical source families, raw probe rows, redirect rows, alternate rows, and adjacent fallback rows.
- Add the exact gateway-composer-vs-webhook-primitive sentence to §4.1 and TD-028.
- Keep TD-029; it is not overreach if framed as a decision record for honoring TD-027 by refusing a new L4 split.

## Re-review trigger

Re-review after iter 2:

- closes Architect H1 and M1-M4,
- documents or patches H2 based on the guard audit,
- preserves `templates/L4` at 12,
- keeps all `l2_blocks_used` values disk-resolvable,
- and removes the deferred-queue fail-state contradiction.

## ADR-ready (if APPROVE)

Not ADR-ready yet. TD-028 and TD-029 are directionally useful, but TD-028 needs Korean evidence/fallback-rotation edits, and both ADRs must align with the final `deferred_recipes:` policy before they are decision-record ready.
