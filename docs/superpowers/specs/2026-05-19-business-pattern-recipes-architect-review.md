# Architect Review — Business Pattern Recipes PRD (Round 5, Iter 1)

> **Reviewer:** Architect (ralplan consensus, DELIBERATE mode)
> **Date:** 2026-05-18
> **Subject:** `docs/superpowers/specs/2026-05-19-business-pattern-recipes-prd.draft.md` (652 lines, Planner output)
> **Predecessor state:** `v1.2.0-p1-absorbed` @ commit `26de945`
> **Verdict (headline):** **ITERATE** — directionally sound, but blocked by (a) one disk-state factual error, (b) `/ax-scaffold` semantic overload, (c) several SP38 over-scope tensions, (d) sealed-verdict harness under-specified.
> **Approval-safe for execution?** No, not yet. 4–6 specific fixes detailed below.

---

## §1 Strongest Steelman — Counter-arguments to the favored direction

The PRD asks for endorsement of **Option A (10 recipes + subcommand + 3 rules + sealed verdict, 6 SPs)**. Below is the strongest antithesis the Planner has not refuted.

### Steelman 1.1 — "10 Recipes shipped" may be a snowflake catalog, not a moat

The L4 sealed-verdict precedent (PAYMENT-PROVIDER, 11/11 MUST + 6/6 SHOULD) **already proves the catalog is self-discoverable to a context-0 sub-agent** (CLAUDE.md MEMORY, payment_blueprint_status). If an AI agent can correctly compose `payment` L4 from `AGENTS.md` + Spec Trio alone, the same agent should be able to compose `billing + auth + feature-flags + notification + audit-log` for a SaaS shape from the same primitives.

**If true**, shipped Recipes become **redundant artifacts**. The actual gap is not "missing composition guidance" — it is **missing a forcing function that makes the L4 composition decision auditable when the agent has to make it**. That is a discriminator/verdict problem (Recipe verification), not an artifact-supply problem (Recipe shipping).

**Implication:** Option E (LLM-infers) was rejected for being "non-deterministic", but the determinism is a property of the **verdict harness**, not of the artifact list. A leaner cut would be: zero shipped recipes, plus `/ax-verify-recipe --infer-and-verify <business-text>` which runs the sealed verdict against whatever composition the agent proposes. The 10 shipped artifacts then become **fixture inputs** for the verdict harness, not consumable templates.

The Planner does not address this because Option E is reduced to "no shipped artifact" — which is a strawman of "no enforcement". Enforcement can live in the verdict, not in the artifact.

**Why this is the strongest counter-argument:** the entire PRD framing rests on "atoms → composition" being the next progression, but the L4 sealed-verdict result already showed atoms ARE composable without intermediate composition artifacts when the verdict is sharp enough. Adding 10 RECIPE.md files risks becoming a **snowflake catalog** the planner has to maintain forever — exactly the "stop adding rules" anti-pattern the CLAUDE.md vision explicitly excludes (which makes the framing defensible) but it does NOT exclude "stop adding **redundant** rules". 10 RECIPE.md without proven adoption pressure is in the redundant-rules failure mode.

**Steelman residual after considering Planner's defense:** Planner's defense (Decision Driver 2) actually leans into recipes-as-fixtures: it says SP40 ships sealed verdicts for 3 of the 10. That admits 7 of the 10 are unverdicted artifacts — the snowflake risk is concrete, not hypothetical. **Recommendation forming below: cut to 3 verdict-anchored recipes; document 7 deferred as evidence-bundle proposals; defer their RECIPE.md until fork-receiver demand arrives.**

### Steelman 1.2 — `recipes/` is a new artifact category with under-specified verification closure

The PRD adds a **new top-level directory** (`recipes/`) and a **new spec family** (`specs/recipes/`) that explicitly does NOT register in `trio_integrity_allowlist.yaml`. This is a new category. New categories require new guards. The PRD lists `recipe_spec_referential_integrity_guard.sh` (validates path resolution) and `recipe_governance_guard.sh` (validates the 3 enforcement rules) — but **neither validates that the recipe semantics match the L4 it composes**. Specifically:

- A recipe asserting `business_invariants: subscription must have ≥1 active plan` is **not bound to any executable check**. The PRD says `verification: spec_trio_billing` (§5.2 example) but the billing Spec Trio does not enforce that invariant — billing enforces lifecycle states, not "plan presence".
- A recipe asserting `business_observability: saas.mrr.calculated_daily_count` (Counter) creates **no obligation** on the agent implementing the SaaS to emit that metric. The enforcement rule `business-domain-must-declare-applied-recipe` only requires declaring the recipe name, not honoring its invariants/observability.

**Implication:** the recipe artifact carries a verification debt the PRD does not pay. Either:
- (a) extend the referential-integrity guard to validate that every `business_invariants` row has a `verification:` field that resolves to an existing Spec Trio assertion or rule, **or**
- (b) downgrade invariants/observability from "guarantee" to "guidance" (advisory only).

Path (b) is fine if labeled, but the PRD currently sells invariants as enforceable contracts (§3 "Every recipe RECIPE.md declares ... `business_invariants:` list ... `business_observability:` list"), which is overpromising.

### Steelman 1.3 — Are 10 the right cut? Why not 3 or 20?

The PRD's defense ("user mandate names 10") in Decision Driver 1 is **factually unverifiable from the artifact text quoted** ("정해진 컴포넌트 구현이 어느정도 되었으면, 각 업무 비지니스 구현 방법에 대한 준비가 되어 있어야해"). The mandate quoted does NOT enumerate 10 patterns — it says "각 업무 비지니스 구현 방법에 대한 준비". The 10 patterns are the Planner's chosen cut, not the user's.

A principled cut on **3 evidence-rich + verdict-anchored recipes** (SaaS-subscription / e-commerce / CRM, which are the SP40 sealed-verdict targets) is more defensible than 10 where only 3 carry a sealed verdict. The 7 unverdicted recipes are evidence-anchored but not verification-anchored — that's a regression from the catalog's existing standard (every L4 has Spec Trio + tests).

### Steelman 1.4 — Three new L2 blocks belong in PRD-5, not PRD-business-recipes

The PRD §5.3 quietly introduces **3 new L2 blocks** (`pipeline-kanban`, `time-series-chart`, `approval-chain`) tucked inside recipe SPs. Two architectural problems:

1. **`time-series-chart.tsx` ALREADY EXISTS** at `templates/L2/blocks/time-series-chart.tsx` (verified on disk, line 1: `template_id: L2/blocks/time-series-chart`). The PRD §5.3 says "may already be derivable from existing chart blocks; verify and consolidate" — this is wrong. It is not derivable; it is present. SP37's deliverable list (§5.4) names `time-series-chart.tsx` as a deliverable, which would either re-create or shadow the existing block. **This is a disk-state factual error and a blocker.**
2. The other two (`pipeline-kanban`, `approval-chain`) are legitimate L2 additions, but bundling them inside recipe SPs **violates the SP-atomic principle**: an L2 block has its own evidence requirement, its own L2 evidence-guard verification axis, and its own deprecation lifecycle. A recipe-SP rollback that also rolls back an L2 block ripples to anyone using the block outside the recipe. They should be **upstream pre-requisites** (separate atomic L2 SP before SP36/SP37), not in-line additions.

### Steelman 1.5 — `/ax-verify-recipe` is `/ax-verify-domain` re-applied (compositional redundancy)

The PRD defines `/ax-verify-recipe <pattern>` as "composes existing /ax-verify-domain calls per L4 in the recipe" (§3 Objectives O4). That is **literally a foreach loop over `enabled_l4_domains`** plus the recipe-spec referential check. There is no new verification axis — only a new fan-out wrapper.

This is **NOT** the SP29 F13/F14/F15 precedent the PRD invokes. SP29 added genuinely new axes (policy-check / evidence-fetch / explain) as `/ax-verify` subcommands. A wrapper that calls existing skills is more naturally implemented as a shell loop in `/ax-scaffold business --verify-after-scaffold`, not as a new Tier-2 skill.

**Implication:** the new Tier-2 surface increases the skill count from 19 → 20 (the PRD's §2 reports 19 today) without adding verification capability. The Tier-1 cap = 4 is preserved (✅), but the Tier-2 count drifts upward unnecessarily.

---

## §2 Real Tradeoff Tension — Predetermined recipes vs LLM inference

The PRD claims to pick **both** (shipped recipes for determinism + free-text inference for flexibility). This is the most interesting design call in the document. The real tension:

| Axis | Shipped 10 RECIPE.md | LLM inference (Option E-style) |
|---|---|---|
| **Determinism** | Same fork-receiver invocation always yields the same composition. | Two identical free-text prompts may yield different compositions across LLM versions. |
| **Auditability** | Recipe is a citable artifact in PRs; reviewers diff against fixed shape. | Inference output is ephemeral; auditing requires running the prompt again. |
| **Evidence anchoring** | Each recipe carries a static `evidence:` block. | Inference may drift from cited evidence as model versions change. |
| **Maintenance cost** | 10 RECIPE.md to keep in sync with L4 evolution (Pre-Mortem Scenario 1 admits this debt). | Zero artifacts; prompt + verdict suffice. |
| **Catalog bloat risk** | High — 10 → 30 recipes if each Korean vertical asks for theirs. | Zero. |
| **Forcing function** | None automatic — recipe is consulted only when agent chooses to. | The `--analyze` step makes consultation explicit. |
| **Verification closure** | Easier on the artifact (static schema); harder on the invariant (need binding to L4 tests). | Easier on the verdict (sealed sub-agent); the artifact disappears. |
| **Fork-receiver value at t=0** | High — paste-able templates. | Low — requires the inference step. |

**Cost of taking both, as the PRD does:**
- **Surface area doubles**: maintain 10 RECIPE.md AND a free-text inference engine AND a 50-fixture eval harness AND a ≥80% accuracy guard. Each component has independent failure modes.
- **The forcing function ambiguity** is the real liability. If a fork-receiver scaffolds via `/ax-scaffold business saas-subscription` (recipe-driven), they get deterministic output but they ALSO have to choose the recipe correctly. If they use `--analyze "build me a multi-tenant SaaS"`, the inference may match `saas-subscription` at confidence 0.6 — below the 0.5 reject threshold but only marginally above. **Which path is the "blessed" path?** The PRD does not say.
- **Inference accuracy 80% means 1 in 5 invocations is wrong.** The PRD treats this as acceptable and recovers via "advisory output + user confirmation". That converts the inference from a guidance tool into a 4-shot multiple-choice quiz the user runs against the recipe catalog. At 1 in 5 false positives across 10 patterns, you average ~12% wrong-first-pick across the lifetime of the tool. Compared to "user reads the 10-line recipe index and picks one in 30 seconds", inference accuracy ≥80% has poor expected value.

**Synthesis (path forward):** Pick ONE primary forcing function and demote the other.
- **Recommended primary:** shipped recipes that a fork-receiver picks by name (`/ax-scaffold business saas-subscription`). High auditability, deterministic, citable.
- **Recommended secondary (demoted):** drop free-text inference entirely from this PRD, OR ship as Tier-3 advisory only with no accuracy guard, no eval suite, and explicit "experimental" labeling. Re-introduce as a Tier-2 feature in a future PRD once 3+ fork-receivers report friction.
- **What this saves:** SP38's 50-fixture eval suite + accuracy guard + Korean fixture mix + threshold-tuning open question. That is approximately half of SP38's scope. SP38 collapses to "subcommand only", maybe folds into SP35 or SP40.

---

## §3 Architectural Soundness — 7 dimensional verdicts

### (a) Recipe vs L4 boundary — **WEAK**

The PRD asserts boundary cleanly: "recipes compose existing L4 only, no new L4" (§3 Must NOT). But three boundary leaks remain:

1. **§5.3 introduces 3 new L2 blocks inside recipe SPs.** L2 is below L4 in the catalog hierarchy; recipes are ABOVE L4. Bundling L2 additions inside recipe SPs inverts the hierarchy.
2. **`time-series-chart.tsx` is mis-classified as new** when it already exists on disk. This means the "recipes only compose" assertion is being violated in the very first cycle: SP37 would re-create or shadow an existing L2.
3. **Recipe 10 (`internal-it`) references `templates/backend/integration/`** which is "not a new L4" (§4 Recipe 10 last paragraph). But `templates/backend/integration/` does not exist on disk today (verify: not in `templates/L4/`, not in `trio_integrity_allowlist.yaml` schema_version 2). If `internal-it` requires it, the PRD is implicitly committing to create a new backend cross-cutting concern. That should be a separate atomic surface, not implied.

**Required fixes:**
- Remove `time-series-chart` from §5.3 and §5.4 SP37 deliverables. Cite existing block.
- Extract `pipeline-kanban` and `approval-chain` into a **separate L2 SP that lands BEFORE SP36/SP37** (could be SP34.5 / SP35.0). Make them atomic L2 additions with their own evidence + L2 evidence-guard verification.
- Either ship `templates/backend/integration/` explicitly (with its own atomic SP) or rework Recipe 10 to use existing primitives only (the recipe's webhook signing claim is realistic with existing `audit-log` + `notification` only; integration adapters can defer).

### (b) Verification closure — **WEAK**

Closure gaps:
1. `business_invariants` are declared in RECIPE.md but **no guard validates they bind to executable assertions** in the composed L4 Spec Trios. Steelman 1.2 elaborates.
2. `business_observability` (metric declarations) have **no test enforcing the metric is actually emitted** when the composed L4 runs. Pre-Mortem Scenario 1 acknowledges semantic drift but mitigation defers it to "SP40 sealed verdict re-runs in CI quarterly" — quarterly is too long for a hard gate.
3. SP38's `analyze` accuracy guard is binary on the 50-fixture suite. The suite is **synthetic** (Planner-authored). Real fork-receiver prompts are out-of-distribution. Accuracy ≥80% on synthetic does not transfer.
4. SP40 sealed verdict for ONLY 3 of 10 recipes leaves **7 recipes with no machine-verified composition correctness**. The Planner's defense (Decision Driver 2) appeals to the PAYMENT-PROVIDER precedent, but that precedent has a single domain with a single sealed verdict — not 7 domains relying on transitive trust.

**Required fixes:**
- Add `business_invariants: verification:` schema requirement: each invariant row MUST cite a `spec_ref:` resolving to a Spec Trio assertion ID OR a `rule_ref:` resolving to a `practices/rules/*.md` ID. Extend `recipe_spec_referential_integrity_guard.sh` to check resolution.
- Demote `business_observability` to advisory in this PRD; add a follow-up SP that binds it to an observability-emit test fixture (no obligation this cycle).
- Either expand SP40 sealed verdict to all 10 recipes (cost) or trim the recipe count to 3 (synthesis). Mixed coverage erodes the standard.

### (c) Evidence chain density — **PASS-WEAK**

Every recipe carries `evidence:` with at least 1 external + 1 internal_design citation (verified §4 Recipes 1–10). Korean references are present (토스 / 쿠팡 / 야놀자 / 인프런 / 채널톡 / 당근마켓 / 카카오엔터프라이즈). Open Question 1 admits `pipeline-kanban` lacks a Korean SaaS reference — that is a minor evidence gap, not a structural one. **Weak**: `evidence:` block format is asserted but no schema is shown for `specs/recipes/<pattern>-recipe-l0.yaml` `evidence:` field validation — `evidence_guard.sh` today validates `practices/rules/*.md` and `practices-react/rules/*.md`; it does not scan `specs/recipes/`. Either extend `evidence_guard.sh` to the new spec family or document that recipe-level evidence is `internal_design` only and not subject to the binary guard.

**Required fix:** explicit policy on which guard validates `specs/recipes/*.yaml evidence:` blocks.

### (d) Anti-pattern resistance — **PASS-WEAK**

CLAUDE.md anti-patterns to avoid:
- ✅ No governance loop document — RECIPE.md is artifact, not process.
- ⚠️ **`RECIPE_DEVIATION.md` IS a governance artifact**. Pre-Mortem Scenario 3 explicitly creates a deviation justification document with provenance_class + rationale. This is exactly the "evidence bundle / curated promotion" anti-pattern shape CLAUDE.md flags (lines 113–139). The mitigation ("recipes accumulating >50% deviation get re-litigated") is itself a governance feedback loop.
- ⚠️ The 30-day WARN→HARD transition for `prefer-recipe-composition-over-l4-cross-import` (Pre-Mortem 3 mitigation) is a soft launch policy — fine in isolation, but the PRD does not say WHO flips the switch, WHEN, or HOW the date is enforced. That's a process-without-owner.
- ✅ No MockMvc reintroduction.
- ✅ Tier-1 cap honored (4 unchanged).

**Required fix:** rename `RECIPE_DEVIATION.md` to something less governance-shaped (`recipe-override-note.md`?) AND make it metadata in the L4 README rather than a separate document. Document the 30-day clock owner (probably `practices/AGENTS.md` sentinel update with a tagged TODO).

### (e) TDD anchor concreteness — **FAIL**

How do you test a Recipe? The PRD answers via SP40 sealed verdict for 3 of 10 recipes. The PRD does NOT answer:
- How do you write a RED test for `recipes/saas-subscription/RECIPE.md` itself? The artifact is markdown + YAML. The "test" is `recipe_spec_referential_integrity_guard.sh` exits 0 — that's a static lint, not a TDD anchor.
- What is the SP35 RED → GREEN cycle? The PRD says "exit 0 from new referential-integrity guard + 3 recipe-level specs parse" (§5.4 SP35) — that's the GREEN state. The RED state is "guard does not exist yet" → write guard → write recipe → guard passes. This is workable, but the PRD does not name it.
- SP38's `--analyze` accuracy guard has a TDD shape (write 50 fixtures → run analyzer → measure accuracy). But the PRD does not say which fixtures are RED (low-confidence) and which are GREEN (high-confidence). The 25 Korean fixtures are mentioned but not anchored.

**Required fix:** add a per-SP TDD anchor table similar to prior PRDs (P1-Absorption SP30/SP31/SP32 each had test-file:assertion:RED-reason:first-green-command rows). The current PRD §5.4 has acceptance criteria but not TDD anchors.

### (f) Skill cap preservation — **PASS-WEAK**

Tier-1 stays at 4 (verified). **Weakness**: `/ax-scaffold business <pattern>` overloads a skill currently scoped to "scaffold a new L4 domain per METHODOLOGY.md Appendix C". The current `skills/ax-scaffold/SKILL.md` description says "Generates an L4 domain skeleton". Adding a `business` subcommand that does NOT scaffold L4 (recipes are NOT L4 per Principle 1) creates a semantic split:

- `/ax-scaffold <domain>` → scaffolds L4 (existing).
- `/ax-scaffold business <pattern>` → composes existing L4 (new).

These are different operations. The SP29 precedent (`/ax-verify policy-check`) added orthogonal axes to a recursive verification skill — all subcommands stayed within the "verification" semantic. `/ax-scaffold business` is closer to a NEW operation than a NEW axis. Calling it a subcommand is technically true but architecturally muddy.

**Required fix (one of):**
- Option α: Rename the entry point to `/ax-compose business <pattern>` (still Tier-1? — no, this would break cap). Instead, ship as new Tier-2 `/ax-compose business <pattern>` calling existing `/ax-scaffold` internally if needed. Tier-1 cap preserved.
- Option β: Keep `/ax-scaffold business <pattern>` but explicitly redefine `/ax-scaffold`'s scope in its SKILL.md as "skeleton generation, whether L4 or composition". Document the broadened scope in METHODOLOGY.md Appendix C.

Option β is lower-risk and matches the PRD's intent. Required: explicit SKILL.md description update in SP38 deliverables.

### (g) SP38 over-scope — **FAIL**

SP38 deliverables (§5.4):
1. `/ax-scaffold business <pattern>` subcommand
2. `/ax-scaffold business --analyze` free-text inference
3. NEW Tier-2 `/ax-verify-recipe` skill
4. 50-fixture eval suite for `--analyze`
5. `business_analyze_accuracy_guard.sh` with ≥80% threshold

That is **5 deliverables in one SP**, including a free-text inference engine (NLP-adjacent), an accuracy gate (calibration problem), and a new Tier-2 skill. The PRD §3 Mode flags this as DELIBERATE because of (a)–(e), but the SP atomic boundary is still violated:

- SP38 atomic rollback would revert all 5 in a single revert commit, including the Tier-2 skill, breaking any post-SP38 references.
- If the accuracy guard fails post-merge (because fixtures were too generous), the only recovery is rewriting fixtures and re-shipping — which under §6 ESCAPE rules requires Critic re-review. The "recovery path" mentioned (§6) is "re-tune keyword tables OR adjust 50-fixture set with explicit critic justification" — that's a soft loop.

**Required fix:** decompose SP38 into either:
- **SP38a**: `/ax-scaffold business <pattern>` subcommand only (deterministic, no inference, no accuracy guard).
- **SP38b**: `/ax-verify-recipe` Tier-2 — **but consider dropping per Steelman 1.5** (it's a wrapper, not a new axis).
- **SP38c (optional, defer-able)**: `--analyze` inference + eval suite + accuracy guard. **Recommended: defer to a future PRD.**

This reduces SP38 from 5 deliverables to 1, freeing budget. If `--analyze` is essential, it gets its own SP with proper pre-mortem.

---

## §4 Synthesis — How to preserve the strengths from competing options

The Planner's instinct (ship recipes + enforce composition + provide forcing function) is correct. The execution can be sharpened:

### Synthesis-A — Trim recipe count + tighten verification

| Aspect | Planner draft | Synthesis-A |
|---|---|---|
| Recipe count | 10 | 3 (saas-subscription / e-commerce / crm) |
| Sealed verdict coverage | 3 of 10 (30%) | 3 of 3 (100%) |
| Free-text inference | Yes (≥80% accuracy guard) | **Defer** (no accuracy gate to maintain) |
| Subcommand | `/ax-scaffold business <pattern>` | Same |
| New Tier-2 | `/ax-verify-recipe` | **Drop** (use loop in subcommand instead) |
| Enforcement rules | 3 | 2 (drop `recipe-deviation-requires-justification` — its escape valve is the governance anti-pattern) |
| New L2 blocks | 3 (one already exists) | 2 (`pipeline-kanban`, `approval-chain`) in a separate atomic L2 SP |
| Recipe-level `business_invariants` | Asserted but unbound | Bound via `verification: spec_ref:` field |
| Recipe-level `business_observability` | Asserted as guarantee | Demoted to advisory; future PRD binds |
| SP count | 6 (SP35–SP40) | 4 (L2 pre-req + 3 recipes atomic + subcommand + sealed verdict-and-release) |
| Wall-time | 10–12 d | ~7–8 d |
| Tier-1 cap | Preserved (4) | Preserved (4) |
| Tier-2 count drift | +1 (19 → 20) | 0 (19 unchanged) |

**Why this preserves strengths:**
- Composition-kit framing intact: recipes still ship, still enforce.
- Korean enterprise evidence intact: 3 chosen recipes all have strong Korean refs.
- Sealed verdict precedent fully honored (not partially).
- Pre-Mortem Scenario 1 (drift) easier with 3 recipes than 10.
- Future expansion (7 deferred patterns) becomes a feature-extension PRD when fork-receiver demand arrives, with the framework already proven.

### Synthesis-B — If Planner refuses to trim count

If the user mandate is interpreted strictly as "10 patterns now", then the minimum changes are:
- Fix the `time-series-chart` factual error (REMOVE from §5.3 / §5.4 SP37; cite existing block).
- Extract 2 actually-new L2 blocks into a pre-requisite atomic SP.
- Bind `business_invariants` to `spec_ref:` / `rule_ref:`.
- Add TDD anchor table to §5.4 (each SP gets test-file:assertion:RED-reason:first-green-command).
- Decompose SP38 (drop `--analyze` OR give it own SP).
- Resolve `RECIPE_DEVIATION.md` governance shape.
- Rename `business_observability` to `business_observability_advisory` to avoid overpromising.

That's a non-cosmetic ITERATE list (7 items). Synthesis-A is cleaner.

---

## §5 DELIBERATE Mode Check

- **Pre-mortem ≥3 scenarios with thresholds?** **PASS**. §7 has exactly 3 scenarios. Each has detection + mitigation + residual risk. Thresholds are present: ≥80% accuracy, confidence ≥0.5, 50% deviation re-litigation, 30-day WARN→HARD. **Weakness**: thresholds 80% and 0.5 are stated without provenance — why not 90% / 0.7? PRD does not say.
- **Verification Matrix `observability_signal` column?** **PASS**. §5.5 includes `observability_signal` column for every SP. (Recall: prior PRD-4 critic flagged this as missing; this PRD has it.) **Weakness**: some signals are aspirational — `recipe.spec.referential_integrity_pass_count`, `recipe.governance.violation_total{rule}` — these counters are not currently emitted by any guard. The PRD doesn't specify the emission mechanism (stdout? JSON to a file? Prometheus?). Without an emission contract, the column is decorative.

**DELIBERATE-mode supplementals (not in Planner draft, should add):**
- Principle-violation explicit flags (see §6 below).
- Cost-of-failure analysis per SP (what fails if SP35 lands broken? SP38? SP40?). PRD has acceptance criteria but not consequence analysis.
- Explicit `compatible_with_catalog_version:` and `last_verified_at:` policy on `recipes/_MANIFEST.yaml` (Scenario 1 mentions this but does not give a binding format).

---

## §6 Principle Violations (DELIBERATE mode mandatory section)

| Principle (CLAUDE.md) | Status | Severity | Detail |
|---|---|---|---|
| 1. Composition kit, not single product | ✅ Honored | — | Recipes compose, do not invert hierarchy. |
| 2. Spec-before-code, evidence-anchored | ⚠️ Partial | **MEDIUM** | `business_invariants` lack `spec_ref:` binding; `evidence_guard.sh` does not cover `specs/recipes/`. |
| 3. Binary verification per axis | ⚠️ Partial | **MEDIUM** | 7 of 10 recipes have no sealed verdict; SP38 accuracy guard is binary but on synthetic fixtures only. |
| 4. Few exposed surfaces, dense feedback loops | ⚠️ Partial | **LOW-MEDIUM** | Tier-1 cap preserved (✅). Tier-2 +1 (`/ax-verify-recipe` is a wrapper, not a new axis). |
| 5. Atomic Spec-Trio rule | ✅ Honored | — | Recipe-level spec lands atomically with RECIPE.md. |
| 6. No speculative generality | ❌ Violated | **MEDIUM-HIGH** | 7 of 10 recipes ship without verdict-anchored verification. `--analyze` inference engine is speculative without proven demand. `RECIPE_DEVIATION.md` is governance-shape speculation. |
| 7. Recipe does not ship code; AI implements business logic | ✅ Honored | — | Recipes are composition contracts. |
| Anti-pattern: governance infinite loop | ⚠️ Risk | **MEDIUM** | `RECIPE_DEVIATION.md` + 30-day WARN→HARD + 50% deviation re-litigation collectively shape a governance loop. |
| Anti-pattern: MockMvc | ✅ Honored | — | No MockMvc reintroduction. |
| Anti-pattern: fork-team policy enforcement | ✅ Honored | — | All gates are catalog-quality only. |
| CLAUDE.md disk-state correctness | ❌ Violated | **HIGH** | `time-series-chart.tsx` already exists on disk; PRD §5.3 / §5.4 SP37 mis-classifies as "new". |

**Total principle violations:** 1 HIGH (disk state), 2 MEDIUM-HIGH (speculative generality, governance loop risk), 3 MEDIUM (spec-binding, verdict coverage, evidence guard scope), 1 LOW-MEDIUM (tier-2 drift).

---

## §7 Required Actions (ordered by severity)

**HIGH (must fix before APPROVE):**
1. Correct `time-series-chart.tsx` status in §5.3 and SP37 deliverables. It exists; cite path `templates/L2/blocks/time-series-chart.tsx`. (Disk-state factual error.)
2. Decompose SP38: drop `--analyze` from this PRD OR give it its own SP with own pre-mortem.
3. Add TDD anchor table to §5.4 (matching P1-Absorption PRD format).

**MEDIUM (should fix before APPROVE):**
4. Bind `business_invariants` to `spec_ref:` or `rule_ref:` resolution in `recipe_spec_referential_integrity_guard.sh`.
5. Either trim to 3 verdict-anchored recipes (Synthesis-A) OR expand SP40 sealed verdict to all 10.
6. Extract `pipeline-kanban` and `approval-chain` into a pre-requisite atomic L2 SP.
7. Rename `RECIPE_DEVIATION.md` or fold into L4 README metadata; remove 30-day WARN→HARD process without owner.
8. Reconsider `/ax-verify-recipe` Tier-2 — either prove a new axis or implement as a loop in `/ax-scaffold business`.

**LOW (nice to fix):**
9. Explicit emission contract for the observability_signal column entries.
10. Provenance for thresholds (80%, 0.5, 50%).
11. Either `templates/backend/integration/` becomes an explicit SP or Recipe 10 internal-it reworks to use existing primitives only.
12. Document policy for `evidence_guard.sh` scope over `specs/recipes/`.

---

## §8 Verdict and Recommendation

**Verdict:** ITERATE.

**Recommendation:** Planner produces iter 2 incorporating either Synthesis-A (preferred) or Synthesis-B (minimum-fix). Synthesis-A produces a leaner, more verdict-anchored PRD that better matches the L4 sealed-verdict precedent that the catalog has already proven. Synthesis-B preserves the 10-recipe count but accepts a 7-item ITERATE list, including a HIGH-severity disk-state correction.

**Approval-safe for execution?** No — at least the HIGH-severity items (1, 2, 3) must be resolved before any SP35 commit. Recommend not entering iter 2 EXECUTE state until at least items 1, 2, 3, 4, 7 are addressed.

**Open Questions surfaced (add to §9 of PRD):**
- Q4: Synthesis-A (3 recipes) vs Synthesis-B (10 recipes with patches) — which interpretation of user mandate is correct?
- Q5: `/ax-verify-recipe` as new Tier-2 vs loop-in-subcommand — what's the test for "new axis" vs "wrapper"?
- Q6: Is `templates/backend/integration/` in scope this cycle or deferred?

---

## §9 References (disk-verified)

- `templates/L2/blocks/time-series-chart.tsx:1-3` — block exists; `template_id: L2/blocks/time-series-chart`; provenance_class: external_canonical.
- `templates/L2/blocks/` — 91 .tsx files (`ls | wc -l`), not 92 as PRD §2 reports.
- `templates/L4/` — 10 entries: audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, search. ✓ matches PRD.
- `skills/` — 19 entries: ax-fork-receiver, ax-guard-cross-trio, ax-guard-evidence, ax-guard-spec-ref, ax-guard-substance, ax-guard-time-decay, ax-guard-trio-integrity, ax-scaffold, ax-transform, ax-verify, ax-verify-L1, ax-verify-L2, ax-verify-L3, ax-verify-L4, ax-verify-domain, ax-verify-java, ax-verify-react, ax-verify-shared, _tests. (Tier-1 = 4 verified.)
- `skills/ax-scaffold/SKILL.md:1-15` — current description scoped to "L4 domain skeleton per METHODOLOGY.md Appendix C".
- `skills/ax-verify/SKILL.md` (subcommands section) — SP29 precedent confirmed: F13 policy-check, F14 evidence-fetch, F15 explain are all axes within the verification semantic.
- `practices/evals/trio_integrity_allowlist.yaml:1-30` — schema_version 2; current domains: auth, crud, payment, practices, ratelimit, audit-log, notification, file-storage, email-outbox, scheduled-task, search, feature-flags, billing, identity-verification.
- `practices/rules/` — 81 .md files; `practices-react/rules/` — 85 .md files. (Matches PRD.)
- `practices/evals/evidence_guard.sh` — exists; scope is `practices/rules/` and `practices-react/rules/` (not `specs/recipes/`).

---

*Architect signoff. Iter 2 awaits Critic review of this critique alongside Planner's response.*
