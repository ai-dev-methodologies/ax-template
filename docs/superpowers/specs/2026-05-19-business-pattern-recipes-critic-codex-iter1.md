# Codex Critic R5 iter 1

## Verdict: ITERATE

Direction is right: a business-pattern composition layer fits the ax-template vision after the L4 catalog is broad enough. The iter 1 draft is not approval-safe for autonomous execution. I ratify all 5 named Architect concerns, with one correction outside that list: `templates/backend/integration/` does exist on disk, so the Architect's optional internal-it integration objection should not be carried as a disk-state blocker.

The blockers are concentrated and fixable: correct one factual disk-state error, trim or fully verify the recipe count, remove free-text inference from this cycle, drop `/ax-verify-recipe` unless it proves a new verification axis, bind recipe invariants to executable specs/rules, add per-SP TDD anchors, and remove the `RECIPE_DEVIATION.md` governance loop shape.

## Architect findings disposition (5)

1. **HIGH disk-state error: RATIFY.** The required check confirms the Architect claim:
   - Command run: `grep -r time-series-chart templates/L2/blocks/`
   - Output: `templates/L2/blocks/time-series-chart.tsx:template_id: L2/blocks/time-series-chart`
   - PRD still calls `time-series-chart.tsx` a new recipe-introduced L2 at `2026-05-19-business-pattern-recipes-prd.draft.md:324,477-489`. SP37 must cite the existing block, not recreate or "consolidate" it as a deliverable.

2. **HIGH SP38 over-scope: RATIFY.** SP38 bundles 5 deliverables: deterministic business subcommand, free-text analyzer, new Tier-2 verifier, 50-fixture eval suite, and accuracy guard (`...prd.draft.md:78,128,490`). That is too much for one SP, and the highest-risk piece (`--analyze`) is optional to the main value path. Decompose or defer `--analyze`; preferably keep SP38 to deterministic `/ax-scaffold business <pattern>`.

3. **HIGH missing TDD anchor table: RATIFY.** §5.4 has acceptance criteria but not a RED/GREEN table (`...prd.draft.md:483-492`). The recipe artifacts are markdown/YAML plus guards; that can be TDD-shaped, but the draft must name test file, assertion, RED reason, first green command, and owning SP for each recipe/guard/subcommand.

4. **MEDIUM business_invariants unbound: RATIFY and upgrade to hard blocker if recipes remain contractual.** The schema example uses `verification: spec_trio_billing` (`...prd.draft.md:458-462`), but that is not a resolvable assertion id. Recipe inventory lists many invariants (`...prd.draft.md:172-176`, `194-198`, etc.) with no `spec_ref:` or `rule_ref:`. If `business_invariants` are contracts, the guard must verify every invariant resolves to an executable Spec Trio assertion or rule. If they are guidance, rename them accordingly.

5. **MEDIUM `RECIPE_DEVIATION.md` governance anti-pattern: RATIFY and reject current shape.** CLAUDE.md forbids promotion/process documents and "evidence bundle" documents (`CLAUDE.md:113-125`). The draft requires `templates/L4/<domain>/RECIPE_DEVIATION.md` with rationale/provenance (`...prd.draft.md:132,526,568-573`) and adds a 30-day WARN-to-HARD policy plus >50% deviation re-litigation. That is a governance ceremony trap. Remove the rule or replace it with a small inline `recipe_overrides:` metadata field checked by the same recipe guard.

## Criterion findings (A-L)

### A. Principle-option consistency - WEAK

Composition-kit and Tier-1-cap principles are coherent (`...prd.draft.md:19-25`). The chosen option conflicts with "no speculative generality" by shipping 10 recipes while only 3 get sealed verdicts (`...prd.draft.md:30,69-82,492`). It also conflicts with "few exposed surfaces" by adding `/ax-verify-recipe` even though the draft defines it as a wrapper over `/ax-verify-domain` (`...prd.draft.md:128,513`).

### B. Fair alternatives - WEAK

Options A-F are present, but the best alternative is missing: **3 verdict-anchored recipes now, 7 evidence notes deferred**. The draft rejects 5 recipes as arbitrary because it claims the user mandate enumerates 10 (`...prd.draft.md:42-47`), but the quoted Korean mandate at `...prd.draft.md:29` does not enumerate 10. Architect's Synthesis-A is the fairer alternative because it aligns recipe count with sealed-verdict coverage.

### C. Executable risk mitigation - FAIL

The pre-mortem has scenarios, but key mitigations are not executable enough:

- Semantic drift detection validates paths, not invariant meaning (`...prd.draft.md:540-547`).
- Free-text false-match mitigation depends on user confirmation after an analyzer that can still be wrong 20% of the time at the stated threshold (`...prd.draft.md:553-560`).
- Governance-loop mitigation creates the loop it claims to prevent (`...prd.draft.md:568-573`).

Every risk needs owner, command, threshold, and recovery. "Re-litigated in next PRD" is not recovery.

### D. Testable acceptance criteria - PASS-WEAK

Most SP rows have binary acceptance (`...prd.draft.md:487-492`). Weaknesses:

- SP37's acceptance includes an already-existing file as a deliverable.
- SP38's acceptance mixes deterministic scaffolding with analyzer accuracy and a new verifier.
- SP40 releases after sealed verdict for only 3 of 10 recipes.

### E. Concrete verification steps - WEAK

The draft names guards and verify skills, but the verification axis is incomplete. `recipe_spec_referential_integrity_guard.sh` checks path existence only (`...prd.draft.md:143,487,540`), not evidence shape, invariant binding, or observability obligation. `/ax-verify-recipe` is not justified as a new skill because the PRD defines it as fan-out over existing domain verification.

### F. TDD concreteness - FAIL

No per-SP TDD anchor table exists. SP35 should start RED with missing/invalid recipe schema and missing L4/L2 refs; SP36/SP37 should have RED cases for new L2 evidence and recipe refs; SP38 should have deterministic CLI fixture REDs if kept; SP39 should have positive/negative rule fixtures; SP40 should have sealed verdict fixture expectations. The PRD currently only describes green acceptance.

### G. Pre-mortem adequacy - WEAK

Three scenarios satisfy the DELIBERATE shape (`...prd.draft.md:534-573`), but thresholds lack provenance. The `--analyze` threshold is especially weak: §6 says `<80%` blocks SP38, but Open Question 2 asks whether to lower to 70% (`...prd.draft.md:525,617-619`). If accuracy is 70%, the correct action is to defer analyzer or keep it experimental, not lower the bar and ship a worse-than-baseline UX.

### H. Expanded test plan adequacy - WEAK

The verification matrix includes `observability_signal` (`...prd.draft.md:494-503`), which fixes a prior-round pattern. But signals are decorative unless guards emit them in a defined format. Add an emission contract such as JSON lines from each guard, or rename the column to "expected diagnostic" if no telemetry is emitted.

### I. Architect findings disposition - PASS

I agree with the 5 user-named Architect concerns. I also agree with Synthesis-A as the preferred revision path: 3 recipes (`saas-subscription`, `e-commerce`, `crm`), no free-text inference this cycle, no `/ax-verify-recipe` unless it proves a new axis, and no `RECIPE_DEVIATION.md` rule.

One correction: Architect's optional claim that `templates/backend/integration/` does not exist is false. Disk state shows:

- `templates/backend/integration/BulkheadConfig.java`
- `templates/backend/integration/ExternalApiTemplate.java`
- `templates/backend/integration/WebClientConfig.java`
- `templates/backend/integration/WebhookOutbox.java`
- `templates/backend/integration/WebhookReceiver.java`
- `templates/backend/integration/WebhookSender.java`

Do not use that point as a re-review blocker.

### J. CLAUDE.md anti-pattern resistance - FAIL

The draft preserves the Tier-1 cap and avoids MockMvc, CI, release, and fork-team policy creep (`...prd.draft.md:147-157,625-633`). It fails the governance-loop check through the deviation ceremony. CLAUDE.md says not to create promotion/check/evidence-bundle documents and to prefer code plus binary tests (`CLAUDE.md:119-125`). `RECIPE_DEVIATION.md` plus 30-day WARN-to-HARD plus >50% re-litigation recreates process around process.

### K. Autonomous execution safety - WEAK

Branching and atomic-cluster language exist (`...prd.draft.md:521-530`), but execution safety is undermined by:

- SP36 and SP37 parallel recipe work both writing `recipes/_MANIFEST.yaml` after SP35; `yq` sorted insertion is helpful but no conflict halt threshold is given.
- SP37 owns an already-existing L2 block.
- SP38 is too broad to revert safely.
- SP40 can tag/release while 7 recipes lack sealed verdicts.

### L. My independent steelman - FAIL

The Korean evidence claim is weaker than the PRD says. The Guardrails promise all recipes name Korean SaaS/enterprise mappings in `evidence:` (`...prd.draft.md:145`) and §10 claims 토스/쿠팡/야놀자/인프런/채널톡/당근마켓 references (`...prd.draft.md:628-629`). In the actual recipe list, many Korean references are business-context prose, not structured `source_type: external` or `provenance_class: external` entries with URLs.

Examples:

- `쿠팡` appears in e-commerce context but the evidence URLs are Shopify and Toss, not Coupang (`...prd.draft.md:189,204-207`).
- `야놀자` evidence says "referenced via public case studies" without a URL (`...prd.draft.md:226-229`).
- `채널톡` appears in CRM context, but CRM evidence is Salesforce and HubSpot only (`...prd.draft.md:233,248-251`).
- `당근마켓` evidence is a public case study without URL (`...prd.draft.md:292-295`).
- `인프런` evidence is public product docs without URL (`...prd.draft.md:314-317`).

If Korean references are part of the evidence claim, each must be in the recipe schema as `provenance_class: external` or `source_type: external` with a URL/citation. Otherwise soften §10 to "Korean context examples, not evidence anchors."

## My independent steelman

The strongest case against the PRD is not "recipes are bad"; it is that recipes are **valuable only when they are verdict-anchored and boring**. A deterministic recipe chosen by name is auditable and useful. A 10-recipe catalog plus free-text inference plus deviation ceremonies plus a wrapper verifier is not boring; it creates four maintenance surfaces before the first fork-receiver proves the need.

The lean version is stronger:

- Ship 3 recipes now: `saas-subscription`, `e-commerce`, `crm`.
- Give all 3 sealed verdicts in SP40.
- Make `/ax-scaffold business <pattern>` deterministic.
- Bind every invariant to `spec_ref:` or `rule_ref:`.
- Mark observability as advisory until an emitter test exists.
- Defer inference until real prompt logs justify it.
- Defer the remaining 7 recipes as candidate rows in `recipes/README.md`, not full recipe artifacts.

This preserves the user's direction while avoiding speculative catalog bloat.

## Hard blockers

1. Fix `time-series-chart.tsx` status everywhere. It exists at `templates/L2/blocks/time-series-chart.tsx`; remove it from "new L2" deliverables and cite it as an existing dependency.
2. Trim to 3 verdict-anchored recipes or expand sealed verdict coverage to all 10. Preferred: trim to `saas-subscription`, `e-commerce`, `crm`.
3. Remove `--analyze` free-text inference from this PRD, or split it into its own SP with a no-downgrade policy: accuracy below 80% means defer, not lower threshold to 70%.
4. Drop `/ax-verify-recipe` as a new Tier-2 skill unless Planner specifies behavior beyond "loop over enabled L4 domains plus referential guard."
5. Add a per-SP TDD anchor table: test/fixture file, assertion, RED reason, first green command, owning SP.
6. Bind every `business_invariants` entry to a resolvable `spec_ref:` or `rule_ref:`, and extend the recipe guard to enforce it.
7. Remove `RECIPE_DEVIATION.md` and the 30-day WARN-to-HARD governance process. Use inline metadata if override documentation is needed.
8. Make Korean/external evidence structurally true: every claimed Korean reference must carry `provenance_class: external` or `source_type: external` plus URL/citation, or the claim must be softened.

## Soft suggestions

1. Keep `pipeline-kanban` and `approval-chain` as either a small prerequisite L2 SP or defer them. Do not hide new reusable L2 blocks inside recipe SPs.
2. Update `skills/ax-scaffold/SKILL.md` and METHODOLOGY.md if `/ax-scaffold business` remains the entry point; current skill text is explicitly L4-domain scaffolding.
3. Define `recipes/_MANIFEST.yaml` schema with `compatible_with_catalog_version`, `last_verified_at`, and sorted insertion conflict handling.
4. Add an explicit `evidence_guard.sh` scope decision for `specs/recipes/*.yaml`.
5. Define observability signal emission format, or downgrade those matrix entries to diagnostics.

## Re-review trigger

Planner iter 2 should be narrow. Re-review only these surfaces:

1. Revised §1 options and recommendation showing either Synthesis-A (3 recipes) or a fully verified 10-recipe path.
2. Revised §3 and §5 removing `time-series-chart` as a new deliverable.
3. Revised SP plan decomposing SP38 and removing or isolating `--analyze`.
4. Revised recipe schema with invariant `spec_ref:`/`rule_ref:` binding and evidence structure.
5. Revised TDD anchor table for every SP.
6. Revised anti-pattern section removing `RECIPE_DEVIATION.md` ceremony.
7. Revised evidence claims for Korean references with URLs or softened wording.

## ADR-ready content

Not ADR-ready yet.

Provisional ADR after blockers close:

- **Decision:** Add Business Pattern Recipes as a verdict-anchored composition layer above existing L4 domains, starting with 3 deterministic recipes.
- **Drivers:** Preserve composition-kit framing, keep Tier-1 surface at 4, make business-pattern selection auditable, and keep every shipped recipe covered by sealed verdict evidence.
- **Alternatives considered:** 10 recipes rejected for unverified catalog bloat; free-text inference rejected for premature NLP surface and weak 80% accuracy UX; `/ax-verify-recipe` rejected unless it proves a new axis beyond fan-out.
- **Consequences:** `recipes/` and `specs/recipes/` become new guarded artifact families; recipe invariants must resolve to existing Spec Trio assertions or rules; recipe observability remains advisory until executable telemetry tests exist.
- **Follow-ups:** Add the remaining 7 patterns only after fork-receiver demand or after the 3-recipe framework proves stable; revisit analyzer using real prompt logs rather than synthetic fixtures.
