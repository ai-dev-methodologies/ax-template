# Architect Review — P1 Absorption PRD (Round 4 iter 1)

**Reviewer:** Architect (oh-my-claudecode:architect)
**Date:** 2026-05-18
**Target:** `docs/superpowers/specs/2026-05-18-p1-absorption-prd.draft.md` (610 lines)
**Verdict:** **ITERATE (recommend Planner revision before Codex Critic pass)**
**Mode:** DELIBERATE inherited from PRD §1

---

## §1 Steelman Antithesis (strongest counter-argument)

> **The PRD is theming a P1 sweep as a "billing-first" cycle while quietly absorbing 5 unrelated clusters under that banner — the result is Option A (catalog-wide sweep) cosplaying as Option B. The "atomic-Spec-Trio honor" framing hides the fact that only 1 of 6 SPs (SP31) is actually atomic-trio sensitive; the other 5 SPs (SP30, SP32–SP35) are heterogeneous polish bundles with no theme beyond "audit P1 row exists."**

Decomposed:

1. **Option B's stated cons are accepted then violated.** §1 lists Option A as "rejected because sweep without theme dilutes priority signal" yet the recommended scope is Option B+C+D-partial — a 6-SP cluster covering billing + Korean specials + rich content + tables/filters + admin polish. By PRD §4 Inventory's own count, **only 11 of ~40 absorbed atoms (P1-04 → P1-14) are billing-domain** atoms. The remaining ~29 atoms span 4 unrelated themes. Calling this "billing-first" is rhetorical; structurally it is the Option A sweep PRD §1 said was rejected.

2. **SP31 is the only SP that exercises the atomic-Spec-Trio machinery.** SP32 ships a backend `identity-verification/` dir with NO Spec Trio (no `specs/identity-verification-l0.yaml`, no contract, no manifest, no allowlist entry). The PRD treats it as fine because there is no L4 frontend domain. But §3 Honored Constraints line 562 explicitly inherits "Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs." A new backend service handling regulated PII (CI/DI from PASS/KCB) is exactly the kind of cross-cutting backend domain that the catalog has previously framed with backend_only Spec Trio (cf. `email-outbox: backend_only`, `scheduled-task: backend_only`, `ratelimit: backend_only` in the disk-verified allowlist). **SP32 silently skips the backend_only Spec Trio that prior Cycle precedent demands.**

3. **The SP30 / SP31 split is the strongest atomic-rule grey-zone in 4 cycles.** SP30 ships `currency-input`, `pricing-table`, `plan-comparison`, `usage-meter`, `invoice-list`, `pricing-page` AND drafts `specs/billing-l0.yaml.draft`. SP31 then closes the Spec Trio atomically with backend + L4 + rules + allowlist append. The Functional Extension Critic Blocker 4 wording (PRD §3 inheritance) says "Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs." Strict reading: a *draft* spec is not a landed spec. PRD §5.2 acceptance says drafts "pass YAML syntax check but trio_integrity_guard explicitly skips `billing-l0.yaml.draft` files (suffix-based exclusion)." But §3 line 165 states "SP30 may author drafts but NOT register them in `practices/evals/trio_integrity_allowlist.yaml`." **This wording is permissive enough to let SP30 ship 7 billing-shaped L1/L2/L3 atoms (`pricing-table`, `plan-comparison`, `usage-meter`, `invoice-list`, `pricing-page` are unambiguously billing-domain) without the trio in force.** That is exactly the slippage Critic Blocker 4 was written to prevent. Compare to SP26 Search (prior cycle) which shipped Spec Trio + backend + L2 + L4 in ONE SP — not split across two.

4. **Deferral list contains 2 internal-dependency footguns.** The deferred list (PRD lines 251–278) includes:
   - **`breadcrumb` L1 (P1, deferred):** PRD justification "app-header.tsx already handles nav." But SP35's new L3 templates `audit-log-page` and `admin-overview-page` will almost certainly need breadcrumbs for a multi-page audit-log + admin nav hierarchy. Deferring breadcrumb to round-5+ creates a dependency cycle: SP35 either (a) reinvents breadcrumb inline (rule violation: L3 can compose only existing L1/L2 atoms), or (b) accepts no-breadcrumb admin UX (UX regression).
   - **`L1 pagination` (P1, deferred — "L2 pagination.tsx already exists"):** disk-verified that current L2 is `pagination.tsx` (a block, not a primitive). SP34's new `tree-table.tsx`, `expandable-row.tsx`, `bulk-export.tsx` will need primitive pagination plumbing. The deferral is technically correct (L2 covers the case), but creates a slight asymmetry vs. shadcn convention where pagination is L1.
   - **`time-picker` (P1, deferred):** SP31 billing entities require `nextBillingDate` UX (subscription renewal date editor). If admin-overview-page or audit-log-page needs date input granular to time, the deferral becomes a SP35 gap.

5. **Parallel SP32/SP33/SP34 claim is mostly true but has one race.** §6.2 Shared-artifact ownership matrix is clean: each SP writes disjoint files. **However:** all three SPs append to `practices/upstream/_MANIFEST.yaml` (the snapshot manifest must be re-sorted on every snapshot add — see Functional Extension PRD line 320). If SP32 + SP33 + SP34 run truly in parallel, three workers will rebase against HEAD and three append-conflicts will fire. The PRD does not call out manifest race-safety for parallel SPs (only for `trio_integrity_allowlist.yaml` in §6.4 third bullet). **This is a real but small race.**

6. **Critic Blocker 4 i18n-scope-contradiction pattern repeats.** Functional Extension Critic flagged that SP28 said "migrate all existing L4 strings" AND "existing L4 migration out of scope" in the same PRD. PRD §11 line 580 says "L4 subscription-management as separate domain (absorbed into billing L4 in SP31)." But §4 P1-09 row says billing-l4 wires "subscription / plans / invoices pages composing SP30 L2 blocks" — billing-l4 contains subscription. So the "subscription-management absorbed" statement is consistent. **However**, deferred-list line 277 says "L4 `analytics-tracking`, `subscription-management`" — listing subscription-management as deferred AND saying it is absorbed in §11. **This is a verbal inconsistency, not yet a hard contradiction, but is exactly the failure mode Critic flagged.**

7. **Korean Specials cluster has a regulatory-scope vagueness.** SP32 ships `IdentityVerificationProvider` interface with PASS + KCB adapters. The PRD admits SCI (NICE 신용평가) is deferred (Open Question Q1). But the rule `no-rrn-collection-without-legal-basis` Java half lives in `practices/rules/`, and the React half in `practices-react/rules/` — split rules with the same name and shared semantics have historically (Functional Extension Critic) created scope-drift. The PRD does not specify which of the two rules is the canonical reference, or how failing-fixture sets stay in sync.

---

## §2 Real Tradeoff Tension

**Side A (PRD's choice):** Group P1 absorption into 6 SPs themed around billing-as-flagship-domain, accept that 5 of 6 SPs are heterogeneous polish bundles, gain wall-time efficiency via SP32/SP33/SP34 parallelism, accept SP30/SP31 split as "drafts-then-atomic."

**Side B (alternative):** Strict atomic-Spec-Trio compliance — collapse SP30+SP31 into a single SP (call it SP30-atomic) shipping L1+L2+L3+L4+backend+Spec Trio+allowlist+rules in ONE commit, like prior Cycle SP26 Search did. Defer SP33/SP34/SP35 polish clusters to a separate, properly-themed PRD (e.g., "P1 forms / tables / admin polish — Round 5") with fork-receiver evidence first.

**Both sides have real merit:**
- Side A wins on wall-time (9–11 days vs 12+) and bundles regulatory work (SP32 Korean specials) with structurally-similar work (SP31 billing) for context-warmth.
- Side B wins on adherence to Critic Blocker 4 atomic-Spec-Trio rule (no draft loophole), avoids the dependency footguns above (breadcrumb / pagination / time-picker), and forces fork-receiver evidence to gate the polish clusters (preventing speculative absorption — PRD Principle 6).

**The PRD picked Side A without explicitly naming Side B as an alternative.** Option E (defer-all-P1) is named and rejected; Option A (catalog-wide sweep) is named and rejected. But "strict-atomic single-SP billing + defer polish" is not in the Options list. This is exactly the alternative-hiding pattern Codex Critic flagged on the prior PRD ("hides best alternative in a risk fallback at line 550 rather than presenting it in §1 Options").

---

## §3 Architectural Soundness Verdict (PASS / WEAK / FAIL per dimension)

### (a) SP atomicity — **WEAK**
The SP30/SP31 split is the central concern. SP30 ships unambiguously billing-domain L1/L2/L3 atoms (`currency-input`, `pricing-table`, `plan-comparison`, `usage-meter`, `invoice-list`, `pricing-page`) before SP31 closes the Spec Trio atomically. Strict atomic-Spec-Trio reading: a draft is not a registered spec. The `.draft` suffix exclusion mechanism is real (PRD §5.2) and the SP30 outputs are functionally unverifiable against a frozen spec until SP31 commits. Prior cycle precedent (SP26 Search) shipped Spec Trio + backend + L2 + L4 atomically — not split. PRD justification is wall-time + worker parallelism, which is mechanical rather than principled.

SP32 additionally ships a backend `identity-verification/` dir with **no** backend_only Spec Trio (compare to prior precedent: `email-outbox: backend_only`, `scheduled-task: backend_only` are registered in `trio_integrity_allowlist.yaml`). Either SP32 must add `identity-verification: backend_only` to the allowlist OR the PRD must explicitly justify why this backend service is exempt (perhaps "no DB table, only ephemeral webhook"). The PRD does neither.

### (b) Verification closure — **PASS-WEAK**
Every new template + rule + L3/L4 has a named `/ax-verify-*` skill (PRD §5.8). Tier-3 guards (trio_integrity_guard, cross_trio_guard, evidence_guard) walk all new paths via standard `practices/evals/*_guard.sh` invocation. **Weakness:** SP32 backend `identity-verification/` is verified by `/ax-verify-java` exiting 0 (PRD §5.4 acceptance), but `/ax-verify-domain identity-verification` is NOT named — which is correct only if identity-verification is not a domain, which contradicts the structural intuition that a backend dir with CRUD + adapter pattern + webhook IS a domain. Same gap as (a).

### (c) Evidence chain density — **PASS**
Every new rule lists `protects_template_id` + `failing_fixture_path` (4 Java rules + 5 React rules — PRD §3 guardrail line 163). Every new L1/L2 has a snapshot citation target (TipTap, signature_pad, shepherd.js, 국세청, KISA, next-themes — PRD §4 totals row). The chain is dense and traceable.

### (d) Anti-pattern resistance — **PASS-WEAK**
No governance loops (no curated-promotion docs, no evidence-bundle docs, no TEMPLATE-GOVERNANCE.md). Korean specials are correctly fenced as RRN-protective (not RRN-enabling) — PRD §4 P1-19 wording explicit. Pre-mortem Scenario 4 covers false-positive on legit CI/DI paths. **Weakness:** SP35's `impersonation-banner-required-when-acting-as-other-user` rule has the failure mode that any helper utility renaming `assumeUserId()` (e.g., `runAsUser()`, `becomeUser()`) will bypass the rule. PRD does not address rule-bypass-via-renaming.

### (e) TDD anchor concreteness — **PASS**
Every SP names a test_file + assertion + RED reason + first_green_command in §5.8 Verification Matrix and in each SP card. SP32 has both a frontend (`business-reg-checksum.spec.ts`) and backend (`IdentityVerificationFlowIT.java`) TDD anchor. SP31 names `BillingFlowIT.java` with explicit RED reason ("BillingService.createSubscription() not implemented"). This is the strongest dimension in the PRD.

### (f) Korean enterprise — **PASS-WEAK**
RRN-protective approach is correct (rule BLOCKS, does not enable). 사업자등록번호 checksum sound — Scenario 2 calls out the multi-variant risk and pre-mortem mitigation requires a 100+ real-sample dataset from public 사업자등록증명원. 휴대폰 본인인증 vendor-agnostic via `IdentityVerificationProvider` interface with PASS + KCB adapters. **Weakness:** SCI (NICE 신용평가) is deferred to round-5+ (Q1) — this is a real fork-receiver risk because NICE has the largest market share among Korean identity-verification providers in regulated industries (banking, healthcare). Shipping PASS + KCB without NICE may force the first banking fork-receiver to add NICE themselves, which proves the abstraction but defeats the "production-ready day 1" framing. Recommendation: ship all 3 adapters or document the abstraction-proving-only intent explicitly in SP32.

### (g) Parallelizability — **WEAK**
SP32 ‖ SP33 ‖ SP34 claim relies on disjoint file ownership (PRD §6.2). The disjointness IS real for template files. **However:**
1. All three SPs append to `practices/upstream/_MANIFEST.yaml` (snapshot manifest — line 320 of Functional Extension PRD shows manifest is regenerated atomically).
2. All three SPs append to `practices-react/AGENTS.md` sentinel (if rule additions are made — SP32 adds 2 React rules, SP33 adds 1, SP34 adds 1; the sentinel sha256 must be re-baselined after each rule add).
3. SP32 and SP33 both add `practices/upstream/*.snapshot.md` files (SP32: 3 snapshots, SP33: 3 snapshots, SP34: 2 snapshots).

These are race-prone surfaces NOT covered by the §6.4 halt thresholds. PRD §6.4 covers `trio_integrity_allowlist.yaml` race but not `_MANIFEST.yaml` race or AGENTS.md sha256 re-baselining race. **This is the same pattern Codex Critic flagged on the prior PRD ("SP24/SP25/SP26 false parallelism").**

---

## §4 Synthesis (proposed fix without rejecting plan)

The PRD has the right intent (clear audit P1 residuals before round-5) and the right inventory filter (L4-unblock × frequency-of-use). Three minimal revisions would convert it from ITERATE to APPROVE-ready:

### Fix 1: Collapse SP30 + SP31 into single atomic SP30
- Ship billing L1 + L2 + L3 + L4 + backend + Spec Trio + allowlist append + rules in ONE atomic commit.
- This honors Critic Blocker 4 atomic-Spec-Trio rule with no draft loophole.
- Wall-time impact: SP30+SP31 becomes ~3 d instead of 2+2; saves the inter-SP review checkpoint.
- Renumber subsequent SPs (SP31 Korean → was SP32; SP32 Rich Content → was SP33; etc).
- Add `identity-verification: backend_only` to `trio_integrity_allowlist.yaml` in the renumbered SP31, OR explicitly document why backend_only is not warranted (e.g., "ephemeral webhook handler, no DB persistence").

### Fix 2: Name "strict-atomic + defer polish" as Option F in §1 Viable Options, then explicitly reject with reason
- This closes the alternative-hiding pattern Codex Critic flagged on prior PRD.
- The PRD already has the data (Option B+C is "hybrid" and acknowledges 4 unrelated polish themes); just elevate the alternative to top-level §1.
- Recommended rejection rationale: "Strict-atomic single-SP billing + defer polish would force ax-template into a 1-domain-per-cycle cadence, slowing catalog completeness; 6-SP polish bundle accepted because each polish cluster (forms/tables/admin) is internally cohesive (≥3 audit rows per cluster) AND each ships at least 1 new failing-fixture rule that anchors it."

### Fix 3: Add §6.4 halt threshold for `_MANIFEST.yaml` race + AGENTS.md sha256 re-baselining race
- Add rule: "Snapshot manifest append (`practices/upstream/_MANIFEST.yaml` OR `practices-react/upstream/_MANIFEST.yaml`) by any parallel SP must rebase against HEAD; conflict triggers serialize-mode for remaining parallel SPs."
- Add rule: "AGENTS.md sentinel sha256 re-baseline is the LAST commit step of any SP that adds rules; if two parallel SPs both add rules, the second one MUST rebase and re-compute sha256 against the first one's commit."
- This converts the soft "parallel claim" into binary-verifiable race-safety.

### Fix 4 (smaller): Resolve subscription-management verbal inconsistency
- §11 line 580 says "subscription-management absorbed into billing L4"; PRD deferred-list line 277 says "subscription-management deferred." Pick one. Recommended: keep §11 wording, strike line 277.

### Fix 5 (smaller): Address deferred-list dependency footguns
- Decide whether SP35's L3 admin-overview-page + audit-log-page need breadcrumb. If yes, promote breadcrumb back to SP35 (M effort, S size). If no, justify explicitly (e.g., "L3 admin uses flat tab navigation, no breadcrumb").
- Same call on time-picker for SP31's subscription nextBillingDate UX.

### Fix 6 (smaller): SP35 rule-bypass-via-renaming
- Strengthen `impersonation-banner-required-when-acting-as-other-user` rule pattern. Consider: matcher on `req.session.actingAsUserId` or similar canonical session-shape, not on a specific function name. OR document in rule that the canonical helper is `assumeUserId()` and any rename requires updating the rule.

---

## §5 DELIBERATE mode check

### §7 Pre-mortem coverage
PRD §7 contains **5 scenarios** (matches PRD §1 claim). Each has explicit detection mechanism + executable mitigation + threshold:
- Scenario 1 (Stripe leak in canonical event): contract test, threshold "contract test fails → halt"
- Scenario 2 (사업자등록번호 multi-variant): 100+ sample dataset, threshold "≥1 known-valid misclassified → halt"
- Scenario 3 (TipTap RSC break): Playwright 3s mount assertion, threshold "Playwright fail → halt"
- Scenario 4 (RRN rule false-positive): pass/fail fixture pair, threshold "`pass_ci_di_verified/` triggers rule → halt"
- Scenario 5 (atomic commit conflict): rebase-retry protocol, threshold "2 rebase-retries fail → halt"

**Verdict:** PASS. 5 ≥ 3 mandatory; each scenario has owner + command + threshold.

### Verification Matrix observability_signal column
PRD §5.8 Verification Matrix has columns: SP / New artifacts / Owning verify skill / TDD anchor file / First green command / Rollback safety. **Missing column: observability_signal.** Functional Extension PRD §6.5 included an observability_signal column (cited in Critic disposition table line H). This PRD's matrix does not.

**Verdict:** FAIL (DELIBERATE mode requirement). Architect requires Planner to either add `observability_signal` column to §5.8 OR explicitly justify why this PRD's deliverables emit no telemetry worth tracking (which is unlikely given SP31 billing-event metrics, SP32 identity-verification audit-log hook, SP35 impersonation banner audit-log hook).

---

## §6 Summary verdict per dimension

| Dimension | Verdict |
|---|---|
| (a) SP atomicity | **WEAK** — SP30/SP31 draft-then-atomic split is grey-zone; SP32 backend lacks backend_only Spec Trio registration |
| (b) Verification closure | **PASS-WEAK** — Tier-3 guards walk new paths; missing `/ax-verify-domain identity-verification` if backend_only is added |
| (c) Evidence chain density | **PASS** — every rule has protects_template_id + failing_fixture_path; every template has snapshot citation |
| (d) Anti-pattern resistance | **PASS-WEAK** — RRN-protective correct; impersonation rule bypass-via-rename concern |
| (e) TDD anchor concreteness | **PASS** — strongest dimension; every SP has test_file + assertion + RED + first_green |
| (f) Korean enterprise | **PASS-WEAK** — RRN-protective + 사업자등록번호 checksum sound; NICE adapter deferral may be aggressive |
| (g) Parallelizability | **WEAK** — disjoint files true; `_MANIFEST.yaml` + AGENTS.md sentinel race not covered in halt thresholds |
| §7 Pre-mortem | **PASS** — 5 scenarios ≥3 with thresholds |
| DELIBERATE observability_signal column | **FAIL** — missing from §5.8 Verification Matrix |

---

## §7 Recommendation

**Likely-ITERATE.**

Required Planner revisions for next iteration to flip to APPROVE-ready:

1. **Critical:** Collapse SP30 + SP31 into single atomic SP30 (Fix 1) — closes atomic-Spec-Trio grey-zone.
2. **Critical:** Add `identity-verification: backend_only` to allowlist in renumbered SP31 OR explicitly document exemption (Fix 1 follow-up).
3. **Critical:** Add `observability_signal` column to §5.8 Verification Matrix (DELIBERATE mode requirement).
4. **Critical:** Add halt threshold for `_MANIFEST.yaml` + AGENTS.md sentinel race in §6.4 (Fix 3) — converts soft parallel-safety claim to binary.
5. **Should:** Add Option F "strict-atomic + defer polish" to §1 Viable Options with documented rejection (Fix 2) — closes alternative-hiding pattern.
6. **Should:** Resolve subscription-management verbal inconsistency (Fix 4) — closes Critic's repeat-failure-mode pattern.
7. **Nice:** Decide breadcrumb / time-picker dependency footguns explicitly (Fix 5).
8. **Nice:** Strengthen impersonation-banner rule pattern against rename-bypass (Fix 6).

If Fixes 1–4 land, the PRD is APPROVE-ready for Codex Critic next iteration. Fixes 5–6 are quality improvements; Fixes 7–8 are polish.

**Antithesis headline:** "Option B billing-first is structurally Option A polish-sweep with SP30/SP31 split serving as the atomic-rule loophole."

---

## §8 References

- PRD draft: `docs/superpowers/specs/2026-05-18-p1-absorption-prd.draft.md:1-610`
- Atomic Spec-Trio rule provenance: `docs/superpowers/specs/2026-05-18-functional-extension-prd.md:168,597,633`
- Critic Blocker 4 (atomic Spec-Trio): `docs/superpowers/specs/2026-05-18-functional-extension-critic-codex-iter1.md:22-23`
- Critic Blocker on false parallelism (precedent): `docs/superpowers/specs/2026-05-18-functional-extension-critic-codex-iter1.md:10,55`
- Critic Blocker on alternative-hiding (precedent): `docs/superpowers/specs/2026-05-18-functional-extension-critic-codex-iter1.md:35`
- Critic Blocker on scope contradiction (precedent): `docs/superpowers/specs/2026-05-18-functional-extension-critic-codex-iter1.md:73-81`
- Disk-verified L4 domains (9): `templates/L4/{audit-log,auth,crud,feature-flags,file-storage,notification,payment,practices,search}/`
- Disk-verified trio_integrity_allowlist: `practices/evals/trio_integrity_allowlist.yaml` (12 domain entries, 8 full_trio + 3 backend_only + 1 frontend_only)
- Disk-verified L1 count: 42 (`ls templates/L1/components/`)
- Disk-verified L2 count: 64 (`ls templates/L2/blocks/`)
- Disk-verified skills count: 19 (Tier-1: 4 + Tier-2: 8 + Tier-3 guards: 7)
- Backend backend_only Spec Trio precedent: `email-outbox: backend_only`, `scheduled-task: backend_only`, `ratelimit: backend_only` — pattern that SP32 identity-verification should follow
