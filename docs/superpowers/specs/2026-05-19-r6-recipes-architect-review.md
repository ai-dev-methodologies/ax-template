# Architect Review — R6 Recipes PRD Draft (Round 6, Iter 0)

> **Reviewer:** Architect (ralplan consensus, DELIBERATE mode)
> **Date:** 2026-05-18
> **Subject:** `docs/superpowers/specs/2026-05-19-r6-recipes-prd.draft.md` (393 lines, Planner draft)
> **Predecessor state:** `v1.3.0-business-patterns` @ commit `b5f16b4` (3 active + 7 deferred recipes, 84 practice rules, 92 L2 blocks, 20 L3 pages, 10 L4 domains).
> **Verdict (headline):** **ITERATE** — premise (absorb 7 R5-deferred) is sound and R5 precedent supports it, but (a) several spec_ref bindings cite IDs that DO NOT EXIST on disk (HIGH), (b) Q1 multi-recipe rule extension is a hard prerequisite that the PRD acknowledges but does not gate atomically, (c) Korean reference pre-categorization regresses R5's iter 2–3 verbatim precedent, (d) SP42 7-verdict fan-out lacks fallback path.
> **Approval-safe for execution?** No, not yet. 6 specific fixes detailed in §7.

---

## §1 Strongest Steelman — Counter-arguments to the favored direction

### Steelman 1.1 — Bulk-absorbing 7 deferred recipes bypasses the fork-receiver demand signal that *justified* their deferral in R5

R5's final PRD trimmed 10 → 3 recipes precisely because Critic iter 1–3 surfaced "snowflake catalog" risk: shipping recipes without proven adoption pressure produces maintenance debt the planner has to carry forever. The 7 deferred recipes each carry an explicit `reintroduction_trigger:` in `recipes/_MANIFEST.yaml` (e.g., `"Fork-receiver demand OR public Booking.com Connectivity case study URL"`). **NONE of those triggers have fired between 2026-05-18 v1.3.0 and 2026-05-18 R6 PRD draft** (same day, no external pull request, no fork-receiver issue).

The R6 PRD §1 reframes "user said `계속 go`" as the demand signal. That is **a category change**: in R5, "fork-receiver demand" meant external adoption evidence (issue, PR, dependency request). In R6, it becomes "the same planner-user keeps building". This is the **"catalog-stable proof" leap** in PRD §1 paragraph 4 — but catalog-stability is a *prerequisite for*, not a *substitute for*, adoption demand.

**Why this is the strongest counter-argument:** if 7 deferred recipes shipped today face zero downstream consumers, they become dead artifacts that still consume:
- 7 × RECIPE.md + L4-composition.md + L2-block-recipe.md + spec-trio-template.yaml maintenance
- 7 × recipe-level spec maintenance under `evidence_guard.sh` (when scope expands per Open Question)
- 7 × sealed verdict re-runs every time L4 evolves
- L4 README `applied_recipes:` list bloat (every L4 README now declares 5–6 recipe memberships)

R5 sealed-verdict precedent (3 of 3 verdicts hitting 12/12 MUST) is **NOT** "the catalog is self-discoverable enough to ship 7 more recipes". It is "3 well-evidenced recipes pass". The 7 deferred recipes were deferred because their evidence chains were thinner (per `_MANIFEST.yaml` reasons: "Korean URL evidence for 야놀자/캐치테이블 thin", "당근마켓 has no public API docs URL", etc.). The R6 PRD's mitigation — pre-categorize Korean refs as `internal_design` upfront — is the planner accepting **lower evidence rigor by design** rather than waiting for evidence pressure to materialize.

**Steelman residual after considering Planner's defense:** the PRD's Option E (defer 4 niche, ship 3) was rejected on grounds of "user mandate `계속 go` implies all 7". `계속 go` is ambiguous — it could equally mean "keep going with the methodology" (next ralplan cycle on any topic) rather than "absorb all 7 deferred". The mandate interpretation Critic flagged in R5 (Steelman 1.3 of R5 architect review) reappears here in identical shape and the PRD does not address it.

### Steelman 1.2 — Q1 is a BLOCKER, not an Open Question

PRD §6 Pre-Mortem Scenario 2 admits Q1 is "HIGH likelihood" and mitigation requires "one targeted edit to `practices/rules/business-domain-must-declare-applied-recipe.md` + `recipe_governance_guard.sh` scanner update — slot in as SP39 prerequisite (pre-commit, no new SP)". This is the PRD trying to have it both ways: simultaneously calling Q1 a `BLOCKER for SP39 start` (§8 Q1) AND a "pre-commit, no new SP" (§6 mitigation).

Two consequences:
1. **Critic iter 1 will catch this immediately.** A blocker that resolves "without a new SP" is just an undeclared SP. The R5 SP35–SP38 pattern proves the planner is willing to use 4 SPs for 3 recipes; using 4.5 SPs for 7 recipes (with the half-SP being the rule extension) is more honest.
2. **The rule already contemplates `applied_recipe_secondary:`** (verified at `practices/rules/business-domain-must-declare-applied-recipe.md:99`). So path (a) is a half-step from the existing rule — but the guard script `recipe_governance_guard.sh` may or may not already scan `applied_recipe_secondary:`. The PRD does not verify this on disk.

**Implication:** Q1 needs to be either (i) elevated to SP38.5 (prerequisite atomic SP before SP39), or (ii) the PRD must show evidence the guard already handles `applied_recipes:` list semantics and only the rule doc needs a one-line clarification.

### Steelman 1.3 — Cluster grouping is internally defensible but the L4 overlap claim is overstated

The PRD asserts clusters "minimize L4 mutating overlap per SP" (Decision Driver 2). Cross-checking:

| SP | Recipes | L4 set used | Overlap with prior SP |
|---|---|---|---|
| SP39 | booking + lms + cms | crud, payment, notification, audit-log, feature-flags, file-storage | — |
| SP40 | community + marketplace | crud, notification, search, audit-log, payment, feature-flags | crud, payment, notification, audit-log, feature-flags (5/6 overlap with SP39!) |
| SP41 | b2b-admin + internal-it | auth, crud, audit-log, feature-flags, search, notification | crud, audit-log, feature-flags, search, notification (5/6 overlap with SP40, 4/6 with SP39) |

**The clusters are NOT L4-disjoint.** Almost every L4 (`crud`, `audit-log`, `notification`, `feature-flags`) is touched by ALL THREE SPs. The only true disjointness is `file-storage` (SP39 only), `payment` (SP39+SP40), and `auth` (SP41 only).

What the clusters actually minimize is **L4 README mutation conflicts per commit**. If SP39 ships `crud/README.md` with `applied_recipes: [booking, lms, cms]` and SP40 ships the same file with `[community, marketplace]`, the merge conflict is real. Mitigation: append-only `applied_recipes:` list with sorted serialization. The PRD does not specify this.

**Steelman residual:** the clustering choice is *defensible* (file/payment vs content/commerce vs admin/ops is a sensible logical taxonomy) but it is *not* "composition disjointness" — it is "logical-domain taxonomy with shared L4 touch". Calling it the former invites Critic to demand proof of disjointness that does not exist.

### Steelman 1.4 — Korean references pre-categorized as `internal_design` breaks the R5 verbatim precedent

R5 iter 2–3 critic blocker was "Korean URL evidence fidelity" — Coupang was downgraded to `internal_design` only AFTER WebFetch HTTP 403 was observed live. The downgrade was reactive evidence. R6 PRD §4 pre-classifies **8 of 14 Korean references** as `internal_design` PROACTIVELY, without attempting WebFetch first:

| Recipe | 야놀자 | 디시인사이드/뽐뿌 | 당근마켓/번개장터 | 인프런/패스트캠퍼스 | 채널톡/토스ID | 카카오 채널/네이버 블로그 | 잔디/채널톡 IT |
|---|---|---|---|---|---|---|---|
| Classification | internal_design | internal_design | internal_design | internal_design | internal_design | internal_design | internal_design |

This is exactly the failure mode R5 iter 2 corrected: **fabrication risk by pre-emptive downgrade**. The R5 standard was "WebFetch first; downgrade only when status≠200". R6 §5 attempts to reinstate this ("if any reference promoted from internal_design to accessible-WebFetch-verify-in-SP\* returns HTTP 4xx/5xx during SP execution, demote back to internal_design"). But the pre-classification table in §4 has ALREADY pre-classified — so there's no "promotion" pending; the recipes are born `internal_design` for 8 of 14 Korean references.

**Implication:** the recipe-level `evidence:` density falls below R5 verdict-anchored standard. R5 verdict harness consumed 1 verbatim Korean + 1–2 external + recipe-level invariants per recipe. R6 may end up with 0 verbatim Korean + 2 external per recipe — that is the **redundant-rule failure mode** the CLAUDE.md vision flags (line 71 of this PRD: "no fabrication"). Pre-classifying as `internal_design` is not fabrication, but it is "evidence-rigor abdication".

### Steelman 1.5 — SP42 7-verdict fan-out has no fallback path

R5 SP38 / SP40 produced 3 sealed verdicts via 3 sub-agent invocations. R6 SP42 must produce 7 in a single SP. The PRD §8 Q3 admits "Single SP42 with parallel sub-agent dispatch where harness supports it. Fall back to sequential within SP42 if parallel infrastructure not ready — wall-time concern only, not correctness."

Three concerns:
1. **The sealed verdict sub-agent harness has never run 7 verdicts in one session.** R5 ran 3. Scale precedent is missing.
2. **Per-verdict cost is non-trivial** (each sub-agent must read the recipe's RECIPE.md + L4 composition + spec-trio-template + cross-reference catalog state). 7× that load runs into context-window concerns even in 1M-context mode.
3. **If verdict N fails, the SP is BLOCKED but the prior N-1 verdicts are stuck in "passed but uncommitted" state.** PRD §6 Pre-Mortem 3 mitigation says "split out as SP43 fast-follow" — but that converts SP42 (FINAL) into SP42+SP43, with the tag `v1.4.0-recipes-complete` either delayed or applied to a mixed state.

**Steelman residual:** SP42 should be planned as parallel-by-default, sequential fallback, AND with an explicit policy that ANY recipe verdict below threshold causes the corresponding recipe to be marked `status: active-verdict-pending` (not blocked) — tag ships, sealed-verdict re-runs in a fast-follow. The PRD does not declare this policy.

---

## §2 Real Tradeoff Tension — Predefined recipes vs lazy fork-receiver-demand-driven

| Axis | Bulk-add 7 (R6 plan) | Lazy fork-receiver-demand (alternative) |
|---|---|---|
| **Onboarding speed** | Fork-receiver gets 10 recipes day-1; decision-by-name fast | Fork-receiver gets 3 recipes day-1; must wait/contribute for 7 more |
| **Catalog bloat risk** | High — 10 recipes is the new floor; reverting to 3 means re-deferring with explanation | Low — backlog stays explicit; only proven-demand recipes ship |
| **Maintenance** | 10 RECIPE.md + 10 specs + 10 verdicts to keep in sync with L4 evolution | 3 recipes + 7 deferred backlog entries (cheap) |
| **Evidence rigor** | 8 Korean refs proactively classified `internal_design`; lower per-recipe evidence density | Each recipe ships when evidence pressure proves the rigor floor |
| **Determinism** | Deterministic — fork-receiver picks by name from 10 | Deterministic but smaller surface |
| **Discoverability proof for AI** | 7 untested recipes shipped — sealed verdict trusts the spec-trio-template format | 3 fully-verdicted recipes prove the format; 7 deferred specify the candidate shape only |
| **Risk of fork-receiver "wrong recipe picked"** | Higher — more options = more category-confusion (e.g., booking vs lms vs cms all use crud + payment) | Lower — fewer overlapping options |
| **Catalog standard parity** | All 10 verdict-anchored (if SP42 succeeds) | 3 verdict-anchored, 7 "deferred-pending-fork-receiver-demand" — clean status |
| **Cost of mistake** | If 3 of 7 turn out unused after 6 mo: 30% catalog rot | If demand for any of the 7 surfaces: one SP per recipe |
| **R5 precedent alignment** | Inverts R5's "trim before ship" instinct | Inherits R5's "demand-driven" instinct |

**The plan picks bulk-add. The unaddressed risk is catalog rot:** if 3 of 7 new recipes see zero downstream consumers within 6 months, the catalog carries ≈30% dead artifacts. The PRD's §10 out-of-scope explicitly removes the safety valve ("Recipe stays deferred and the surface candidate is logged in `recipes/_MANIFEST.yaml#deferred_recipes:` with `blocker:` field") — but this only handles the L2-discovery case, NOT the post-ship rot case.

**Synthesis path (preserved in §4 below):** ship the 3 high-evidence recipes (booking, marketplace, b2b-admin — all with at least 2 verifiable external anchors), keep 4 as deferred with refreshed `reintroduction_trigger:` reasons. This matches R5 cadence (4 SPs → 3 recipes) and preserves the trim-before-ship instinct.

---

## §3 Architectural Soundness — 7 dimensional verdicts

### (a) Multi-recipe membership Q1 resolution — **WEAK**

The PRD acknowledges Q1 as a blocker (§8 Q1) but treats its resolution as a half-step inside SP39 (§6 Pre-Mortem 2: "slot in as SP39 prerequisite (pre-commit, no new SP)"). Disk evidence confirms `practices/rules/business-domain-must-declare-applied-recipe.md:99` already shows an example with `applied_recipe_secondary:`. So path (a) is partially-realized in docs. **Gap:** the PRD does not verify whether `recipe_governance_guard.sh` already parses multi-value or `_secondary:` variants. Without that disk verification, "pre-commit no new SP" is a wishful-thinking commitment.

**Required fix:** add §4.5 row (or §5 mid-flight gate) confirming `recipe_governance_guard.sh` scanner state for multi-value; if the guard requires code change, declare it as SP38.5 atomic prerequisite (not pre-commit slop).

### (b) Cluster disjointness for parallel-safe execution — **FAIL**

The clusters are NOT disjoint per Steelman 1.3. They are LOGICALLY TAXONOMIC (file-vertical / content-commerce / admin-ops) but they share `crud`, `audit-log`, `notification`, `feature-flags`, `search` across SPs. The PRD calls this "composition disjointness" — that is not what the data shows.

**Required fix:** rename Decision Driver 2 to "logical-domain clustering" or "L4 README mutation locality"; explicitly state the rebase / merge strategy for L4 README files touched by all 3 SPs (e.g., append-only `applied_recipes:` array with alphabetical sort). SP linearization (SP39 → SP40 → SP41) makes this safe ONLY if each SP appends, never replaces.

### (c) Korean evidence rigor (verbatim vs internal_design ratio) — **FAIL**

8 of 14 Korean references pre-classified `internal_design`. R5 standard was reactive downgrade after WebFetch failure. R6 proactive downgrade lowers evidence density per recipe. Specifically: `booking` has 1 Korean ref (야놀자, internal_design) + 1 Korean WebFetch candidate (네이버 예약 — and that URL is `https://developers.naver.com/docs/login/api/api.md`, which is Naver LOGIN docs, not 예약 docs — PRD itself flags "reservation-specific API docs may exist" without verifying). So booking ships with 0 verbatim Korean refs in the worst case.

Cross-check against R5 standard: `recipes/saas-subscription/RECIPE.md` ships with verifiable 토스 + Stripe + Linear refs. R6 recipes will average lower density.

**Required fix:** mandate ≥1 WebFetch attempt per Korean reference BEFORE SP execution, with results captured in §4 of THIS PRD (not deferred to SP execution). If WebFetch fails, the `internal_design` classification is then documented as evidence-of-attempt, matching R5 Coupang precedent. Recipes that end up with 0 verbatim Korean refs are downgraded to deferred for next cycle.

### (d) Sealed verdict scalability (7 in SP42 vs split) — **WEAK**

Single SP42 with 7 parallel sub-agents is untested. PRD mitigation is wall-time concern only. Real concern is **per-verdict iteration cycle**: if recipe N's RECIPE.md needs a "Sub-agent Hint Sheet" addition to hit threshold (§6 Pre-Mortem 3), that iter blocks the entire SP42 close. PRD says "split out as SP43 fast-follow" — but `v1.4.0-recipes-complete` tag semantics break if 1 of 7 ships verdict-pending.

**Required fix:** declare explicit policy: SP42 tags `v1.4.0-recipes-complete` when ≥5/7 verdicts pass threshold; remaining are marked `status: active-verdict-pending` in `_MANIFEST.yaml`; SP43 fast-follow within 1 SP-cycle. OR split SP42 into SP42a (booking+lms+cms verdicts) + SP42b (rest+tag) for safer atomic units.

### (e) TDD anchor concreteness per recipe — **FAIL**

PRD §4.5 has a verification column ("`recipe_governance_guard.sh` exit 0; `recipe_spec_referential_integrity_guard.sh` exit 0") but does NOT provide per-recipe TDD anchor:
- For `booking`: which test file goes RED first? What assertion? What first-GREEN command?
- Same for the other 6 recipes.

R5 architect review §3(e) called out the identical gap for SP35–SP38. The PRD has not addressed this in R6. The §4.5 "TDD anchor (RED → GREEN)" column is per-SP, not per-recipe; for SP39 the column says "`recipe_governance_guard.sh` initially RED (3 new recipes missing applied_recipe wiring) → GREEN after L4 README updates" — that's one RED state for 3 recipes shipped together, not the R5 cadence.

**Required fix:** per-recipe TDD anchor table in §4 (one row per recipe): test_file path, RED reason, first-GREEN command, expected verdict-harness MUST/SHOULD score range. Matches P1-Absorption PRD format.

### (f) Composition kit invariant (zero new L4) — **PASS-WEAK**

PRD §3 Must NOT Have correctly bans new L4. §4 carefully re-routes claimed new-L4 needs (scheduled-task, identity-verification, integration) to existing primitives (notification scheduler, auth+feature-flags, notification webhook adapter). **Gap:** the PRD admits uncertainty about whether `templates/L4/notification/` ships a scheduler primitive (§4.4 LMS: "verify in SP39: does `templates/L4/notification/` ship a scheduler? If not, recipe documents that fork-receiver must add a job scheduler from their stack"). That's a discovery deferred to SP execution.

If `notification` does NOT ship a scheduler, recipes for lms / cms (scheduled publish) lose their composition contract — they become "recipes that document things fork-receiver must build". That's a value-density drop.

**Required fix:** disk-verify the `notification` L4 scheduler primitive existence BEFORE SP39 starts. If absent, either ship a `scheduled-task` recipe-shim docs in `notification/` README (NOT a new L4) or downgrade lms / cms back to deferred.

### (g) New L2 hard rule (recipe downgrades back to deferred if needs new L2) — **PASS**

§3 Must NOT and §10 Out-of-scope both correctly state: any recipe needing a net-new L2 → recipe downgrades back to deferred. §6 Pre-Mortem 4 has explicit mitigation ("Hard rule — if an SP discovers a net-new L2 need, that recipe alone is downgraded back to deferred"). §4 includes re-verification NOTEs for each recipe confirming claimed L2 needs map to existing blocks. **Weakness:** §4.6 cms claims "`auto-save-indicator`, `dirty-guard`" but does not check whether these blocks support the cms-specific draft auto-save semantics — they exist as blocks but the recipe semantics may demand behavior the blocks lack. This is a semantic-fit risk, not a presence risk; existence verified on disk (`auto-save-indicator.tsx`, `dirty-guard.tsx`).

### Disk-state correctness (spec_ref bindings) — **FAIL (HIGH severity)**

Per disk verification:
- ✅ `AUDIT-RETENTION-001` exists (`specs/audit-log-l0.yaml:88`)
- ✅ `BILLING-AUTHZ-002` exists (`specs/billing-l0.yaml:18`)
- ✅ `PAYMENT-REFUND-001` exists (`specs/payment-l0.yaml:189`)
- ❌ **`CRUD-VALIDATION-002` DOES NOT EXIST** — cited 2× (BOOKING-INV-001, LMS-INV-001)
- ❌ **`AUDIT-EMIT-001` DOES NOT EXIST** — cited 3× (BOOKING-INV-003, B2BADMIN-INV-001, INTERNAL-IT-INV-002)
- ❌ **`PAYMENT-LIFECYCLE-003` DOES NOT EXIST** — cited 1× (MARKETPLACE-INV-001; PRD itself flags "verify ID exists; otherwise recipe-level invariant")

This is identical to the R5 architect review §3(a) finding ("`time-series-chart.tsx` already exists; PRD §5.3 mis-classifies as new"). The disk-verification discipline that R5 critic enforced has slipped in R6 draft. The PRD §4 cites 3 spec IDs that simply do not exist, including AUDIT-EMIT-001 which is foundational to 3 recipes (43% of R6 scope).

**Required fix:** disk-verify every `spec_ref:` ID in §4 BEFORE submitting to Critic. Any unresolvable ID must either (a) be replaced with a real ID, or (b) the recipe ships a recipe-level invariant explicitly declared (e.g., "`recipes/booking/spec-trio-template.yaml#BOOKING-AUDIT-EMIT-001`") and the spec gets a real entry in a follow-up SP. The current state ships 6 invariants pointing to nothing — `recipe_spec_referential_integrity_guard.sh` will exit non-zero on commit.

---

## §4 Synthesis — Preserving strengths from competing options

### Synthesis-A — Trim to 3, defer 4 (preferred)

| Aspect | R6 draft | Synthesis-A |
|---|---|---|
| Recipes shipped this cycle | 7 | 3 (e.g., booking + marketplace + b2b-admin — strongest external anchors) |
| Korean refs proactively `internal_design` | 8 of 14 | ≤3 of 6 (must WebFetch before pre-classify) |
| Spec_ref bindings | 6 cite non-existent IDs | All bindings disk-verified |
| Sealed verdict scalability | 7 in SP42 (untested at scale) | 3 in SP42 (proven at R5 scale) |
| SP count | 4 (SP39–SP42) | 4 (SP38.5 rule extension + SP39 cluster + SP40 cluster + SP41 verdict+tag) |
| Wall-time | 8–10 d | 5–7 d |
| Q1 resolution | Pre-commit slop inside SP39 | Atomic SP38.5 |
| Catalog rot risk | 30% in 6 mo | <10% in 6 mo |
| Tier-1/Tier-2 cap | Preserved | Preserved |

Synthesis-A preserves R5 cadence and R5 evidence rigor. Defers 4 recipes (community / lms / cms / internal-it) to R7+ when external adoption pressure surfaces. Re-frames `recipes/_MANIFEST.yaml#deferred_recipes:` reasons with fresh dates.

### Synthesis-B — Ship 7 but with R5-rigor (minimum-fix)

If user mandate is strictly "all 7", the minimum fix list:
1. SP38.5 atomic Q1 rule extension (rule doc + guard scanner update).
2. WebFetch every Korean URL BEFORE PRD-final commit; embed result + timestamp into §4 of THIS PRD.
3. Disk-verify and correct every `spec_ref:` ID. Replace `CRUD-VALIDATION-002`, `AUDIT-EMIT-001`, `PAYMENT-LIFECYCLE-003` with real IDs or recipe-level invariants.
4. Rename Decision Driver 2 to "logical-domain clustering" + declare append-only `applied_recipes:` list strategy for L4 READMEs touched by multiple SPs.
5. Per-recipe TDD anchor table (test_file, RED, first-GREEN, expected verdict score).
6. Disk-verify `notification` L4 scheduler primitive; if absent, downgrade lms+cms or accept value-density drop with disclosure.
7. SP42 explicit policy: ≥5/7 verdicts pass → tag ships; remaining verdict-pending; SP43 fast-follow (or split SP42a/SP42b).

That is a 7-item ITERATE list. Synthesis-A is cleaner.

---

## §5 Consensus Addendum (ralplan)

- **Antithesis (steelman):** Bulk-shipping 7 deferred recipes inverts the R5 trim-before-ship instinct that the same critic chain enforced just 1 round ago. The `계속 go` user mandate is reinterpreted as adoption pressure, but no external fork-receiver signal has fired since v1.3.0 (same day). Pre-categorizing 8 of 14 Korean refs as `internal_design` lowers evidence density below the R5 verbatim standard. 3 spec_ref IDs cited in §4 do not exist on disk.
- **Tradeoff tension:** Deterministic-by-name onboarding (10 recipes day-1) vs catalog-rot risk (≈30% dead artifacts in 6 mo if demand does not materialize). The PRD picks the former but does not declare a 6-mo retrospective gate for re-deferral.
- **Synthesis (preferred):** Synthesis-A — ship 3 highest-evidence recipes (booking + marketplace + b2b-admin) in 4 SPs at R5 cadence; keep 4 deferred with refreshed triggers. If user insists on 7, Synthesis-B applies with 7-item ITERATE list.
- **Principle violations (deliberate mode):** see §6.

---

## §6 Principle Violations (DELIBERATE mandatory)

| Principle (CLAUDE.md + R5-inherited) | Status | Severity | Detail |
|---|---|---|---|
| 1. Composition kit, not single product | ✅ Honored | — | Zero new L4 enforced. |
| 2. Spec-before-code, evidence-anchored | ❌ Violated | **HIGH** | 3 spec_ref IDs cited do not exist on disk (CRUD-VALIDATION-002, AUDIT-EMIT-001, PAYMENT-LIFECYCLE-003). 8 of 14 Korean refs proactively downgraded without WebFetch attempt. |
| 3. Binary verification per axis | ⚠️ Partial | **MEDIUM** | SP42 7-verdict fan-out untested at scale; no explicit fallback policy for verdict-N-fails-but-1..N-1-pass. |
| 4. Few exposed surfaces, dense feedback loops | ✅ Honored | — | Tier-1=4, Tier-2=8 unchanged. |
| 5. Atomic Spec-Trio rule per SP | ⚠️ Partial | **MEDIUM** | Q1 rule extension declared as "pre-commit, no new SP" — that is an undeclared SP. |
| 6. No speculative generality | ⚠️ Partial | **MEDIUM-HIGH** | 7 recipes ship simultaneously without external adoption pressure. R5 trim-precedent inverted. Catalog rot risk unmitigated. |
| 7. Recipe does not ship code; AI implements business logic | ✅ Honored | — | Recipes are composition contracts. |
| Anti-pattern: governance infinite loop | ✅ Honored | — | No new governance docs. |
| Anti-pattern: MockMvc reintroduction | ✅ Honored | — | None. |
| Anti-pattern: fork-team policy enforcement | ✅ Honored | — | Catalog-quality only. |
| R5 standard: Korean refs verbatim or reactive downgrade | ❌ Violated | **MEDIUM-HIGH** | Proactive `internal_design` classification before WebFetch attempt. |
| Disk-state correctness | ❌ Violated | **HIGH** | 3 non-existent spec IDs cited; `notification` scheduler primitive existence assumed but not verified; cluster "disjointness" claim contradicted by actual L4 set overlap. |

**Total:** 2 HIGH (spec_ref / disk-state), 2 MEDIUM-HIGH (speculative bulk-ship / Korean rigor), 3 MEDIUM (verdict scalability / atomic SP rule / cluster overlap).

---

## §7 Required Actions (ordered by severity)

**HIGH (must fix before APPROVE):**
1. Disk-verify and correct every `spec_ref:` ID in §4. Replace `CRUD-VALIDATION-002`, `AUDIT-EMIT-001`, `PAYMENT-LIFECYCLE-003` with real IDs or with recipe-level invariants explicitly authored in this PRD.
2. WebFetch every Korean URL in §4 BEFORE PRD-final. Capture URL + HTTP status + quote (or 4xx/5xx + downgrade rationale) into §4 inline. Match R5 reactive-downgrade precedent.
3. Either trim to 3 recipes (Synthesis-A) OR explicitly declare 6-mo catalog-rot retrospective gate with re-defer trigger.

**MEDIUM (should fix before APPROVE):**
4. Elevate Q1 (multi-recipe membership rule extension) to atomic SP38.5; disk-verify whether `recipe_governance_guard.sh` already parses `applied_recipes:` list.
5. Rename Decision Driver 2 to "logical-domain clustering" and add explicit append-only `applied_recipes:` strategy for L4 READMEs touched by ≥2 SPs.
6. Add per-recipe TDD anchor table (test_file / RED reason / first-GREEN command / expected verdict MUST-SHOULD range).
7. Declare SP42 fallback policy: ≥5/7 verdicts → tag ships, rest `active-verdict-pending` + SP43 fast-follow; OR split into SP42a/SP42b.

**LOW (nice to fix):**
8. Disk-verify `notification` L4 scheduler primitive; document lms+cms downgrade contingency.
9. Provenance for `applied_recipe_secondary:` example at `practices/rules/business-domain-must-declare-applied-recipe.md:99` — show whether guard already handles it.
10. Explicit emission contract for §4.5 observability_signal entries (advisory or enforced?).

---

## §8 DELIBERATE Mode Check

- **Pre-mortem ≥3 scenarios with thresholds?** **PASS**. §6 has 4 scenarios (exceeds minimum). Each has likelihood (Medium/HIGH/Low/Low) + impact + mitigation + residual risk. Thresholds present: 3 iter cycles before halt, ≥10/12 MUST + ≥5/8 SHOULD, ±5 min CMS publish, ≤30 s search lag. **Weakness:** thresholds 10/12 and 5/8 are inherited from R5 — provenance unstated; "3 iter cycles" stop condition is generic, not derived.
- **Verification Matrix `observability_signal` column?** **PASS**. §4.5 includes the column. **Weakness:** all listed as "advisory, no emitter test enforced" — that's the R5 architect-flagged "decorative column" pattern, repeated in R6.
- **Principle violations explicit?** PASS via §6 above.
- **Cost-of-failure per SP?** PARTIAL. §6 covers per-scenario impact but not per-SP "what fails if SP39 lands broken? SP42?".

---

## §9 Verdict and Recommendation

**Verdict:** ITERATE.

**Recommendation:** Planner produces iter 1 either (a) trimming to 3 recipes per Synthesis-A (preferred), or (b) addressing the full 7-item Synthesis-B ITERATE list. Highest priority: items 1–3 of §7 are blockers — the PRD cannot enter EXECUTE state while spec_ref IDs are imaginary, Korean refs are pre-downgraded, and catalog-rot is unmitigated.

**Approval-safe for execution?** No. Resolve HIGH items (1, 2, 3) and MEDIUM items 4 + 6 minimum.

**Open Questions surfaced (add to PRD §8):**
- Q4: Synthesis-A (3 recipes) vs Synthesis-B (7 with patches) — which interpretation of `계속 go` mandate is correct? Same shape as R5 Q4.
- Q5: Catalog-rot retrospective — is there a 6-mo gate that re-defers unused recipes? If not, recipe count is monotone-increasing in perpetuity.
- Q6: SP42 fallback semantics — verdict-pending tag ships vs split SP?

---

## §10 References (disk-verified 2026-05-18)

- `recipes/_MANIFEST.yaml:1-78` — 3 active recipes, 7 deferred with explicit reasons + reintroduction_triggers; schema_version 1.
- `recipes/{crm,e-commerce,saas-subscription}/` — 3 active recipe dirs confirmed.
- `skills/_tests/sealed-verdict/` — 3 verdict files (crm, e-commerce, saas-subscription). R5 precedent for SP42 fan-out.
- `practices/rules/business-domain-must-declare-applied-recipe.md:99` — example shows `applied_recipe: saas-subscription` + `applied_recipe_secondary: e-commerce`; Q1 path (a) partially-realized in doc.
- `practices/rules/{prefer-recipe-composition-over-l4-cross-import.md, recipe-invariants-must-resolve.md}` — 3 R5 enforcement rules confirmed.
- `practices/evals/{recipe_governance_guard.sh, recipe_spec_referential_integrity_guard.sh}` — both guards exist.
- `specs/audit-log-l0.yaml:88` — `AUDIT-RETENTION-001` exists ✓.
- `specs/billing-l0.yaml:18` — `BILLING-AUTHZ-002` exists ✓.
- `specs/payment-l0.yaml:189` — `PAYMENT-REFUND-001` exists ✓.
- `specs/{auth,audit-log,billing,crud,payment,notification,feature-flags,file-storage,search,practices}-l0.yaml` — **NO match for `CRUD-VALIDATION-002`, `AUDIT-EMIT-001`, `PAYMENT-LIFECYCLE-003`** (HIGH-severity factual error).
- `templates/L1/components/` — `calendar.tsx`, `date-range-picker.tsx`, `file-dropzone.tsx`, `markdown-renderer.tsx`, `progress.tsx`, `relative-time.tsx`, `rich-text-editor.tsx`, `badge.tsx` all confirmed.
- `templates/L2/blocks/` — `impersonation-banner.tsx`, `feature-flag-toggle.tsx`, `feature-gate.tsx`, `payment-checkout-form.tsx`, `saved-view.tsx`, `saved-filters.tsx`, `event-stream.tsx`, `live-presence.tsx`, `recent-searches.tsx`, `column-picker.tsx`, `column-reorder.tsx`, `bulk-actions-bar.tsx`, `bulk-export.tsx`, `kpi-card.tsx`, `time-series-chart.tsx`, `filter-bar.tsx`, `faceted-filter.tsx`, `search-input.tsx`, `search-palette.tsx`, `result-highlighter.tsx`, `expandable-row.tsx`, `notification-bell.tsx`, `notification-item.tsx`, `notification-list.tsx`, `auto-save-indicator.tsx`, `dirty-guard.tsx`, `field-wizard.tsx` — all 27 cited blocks confirmed present.
- `templates/L3/pages/` — `wizard`, `audit-log-page`, `dashboard-page`, `list-page`, `detail-page`, `create-page`, `edit-page`, `pricing-page`, `search-results-page`, `admin-overview-page`, `settings-overview` all present.

---

*Architect signoff. Iter 1 awaits Planner response to Required Actions §7.*
