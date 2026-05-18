# R6 — 3 Highest-Evidence Recipes (booking + marketplace + b2b-admin) PRD — 2026-05-18 (Round 6, ralplan iter 3)

> **Status:** ITER 3 (Planner revision after Codex Critic iter 2 ITERATE — narrow Q1 guard-contract blocker).
> **Date:** 2026-05-18. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-19-business-pattern-recipes-prd.md` (CLOSED — SP35–SP38, `v1.3.0-business-patterns`, `b5f16b4`).
> - `2026-05-19-r6-recipes-prd.iter2.md` (iter 2 — closed 6/7 critic blockers; iter 3 closes the final Q1 guard-contract blocker).
> **Branch (when execution starts):** `feat/r6-recipes-sp39-sp40`.
> **Targeted tag:** `v1.4.0-recipes-complete` (only if all 3 sealed verdicts ≥10/12 MUST + ≥5/8 SHOULD; otherwise hold tag until verdicts pass).
> **Mandate trigger:** User said "계속 go" after R5 v1.3.0. Iter 1 Critic + Architect both demanded Synthesis-A (trim 7 → 3); iter 2 honored that demand; iter 3 fixes the iter-2 guard-contract claim that the Critic disk-disproved.

---

## §1 RALPLAN-DR Summary

### What changed iter 2 → iter 3 (narrow guard-contract fix only)

| Change | Iter 2 | Iter 3 | Rationale |
|---|---|---|---|
| Guard claim re `applied_recipes:` plural form | "Zero code change — substring `applied_recipe:` is contained in `applied_recipes:`" | **FALSE. Disk-verified: `recipe_governance_guard.sh:42` runs `grep -q "applied_recipe:"` (singular with trailing colon). The colon is NOT contained in `applied_recipes:` (plural has `s` before the colon). SP39 atomic scope now includes guard regex update + 2 new fixtures.** | Critic iter 2 Hard Blocker (disk-verified by `printf 'applied_recipes:\n  - booking\n' \| grep -q 'applied_recipe:'` → exit 1). |
| Canonical form going forward | Plural `applied_recipes:` declared canonical with no migration | **Plural `applied_recipes:` canonical for R6+; singular `applied_recipe:` / `applied_recipe_secondary:` remain VALID for legacy R5 L4 READMEs. Backward-compat via dual-form regex acceptance, not via substring trick.** | Critic Option 2 ("keep Option (b) but make SP39 explicitly include guard + fixture updates") — explicitly approved by critic. |
| SP39 scope additions | None — claimed zero code change | **+1 guard file edit (`practices/evals/recipe_governance_guard.sh` regex line 42) + 2 new fixtures (`pass_applied_recipes_plural/RECIPE.md`, `fail_applied_recipes_empty_list/RECIPE.md`)** | Closes the iter-2 false premise atomically within SP39. |
| TD-2026-05-18-019 framing | "No code change" | **"Dual-form guard regex acceptance. Guard regex updated atomically in SP39. R5 singular form preserved as backward-compat. R6+ plural form canonical."** | Critic iter 2 §"ADR (if APPROVE)" — ADR not approvable while premise was false. |
| Pre-mortem Scenario 4 (guard not accepting plural) | Implicit residual risk | **Actively mitigated — guard regex extended-grep alternation accepts both forms; 2 fixtures (one happy plural, one empty-list fail) co-shipped with regex change.** | Critic Pre-Mortem implicitly requested this. |
| Recipe-set / count / Korean ledger / cluster framing / TDD anchors / SP40 tag policy | Iter 2 form | **UNCHANGED — Critic explicitly said "No need to reopen the 3-recipe scope, Korean ledger, cluster framing, TDD anchors, or SP40 tag policy."** | Critic iter 2 §"Final reasoning". |

### What carried over UNCHANGED from iter 2 (Critic-confirmed CLOSED — do not reopen)

- Synthesis-A trim to 3 recipes (booking + marketplace + b2b-admin). 4 deferred (community + lms + cms + internal-it) with refreshed triggers — see §10.
- Broken `spec_ref:` IDs replaced with disk-real anchors. All cited anchors spot-checked by critic — PASS.
- Korean WebFetch ledger (§4.4) — 3 verbatim PASS rows (Etsy, Stripe Connect, channel.io) + 3 documented downgrades (Naver, Booking.com, Jira).
- Cluster claim corrected to honest "logical theme clustering, shared L4 append-only mutation".
- Per-recipe TDD anchors (test_file / assertion / expected_RED_reason / first_GREEN_command / owning_SP) for all 3 recipes.
- SP40 FINAL tag policy: `v1.4.0-recipes-complete` held until 3/3 sealed verdicts pass; partial-state recipes mark `active-verdict-pending` for SP41 fast-follow.
- 2 SPs (SP39 atomic-3 + SP40 FINAL), linear, no parallel branches.

### Principles (R5 inheritance, lightly specialized — Principle 7 corrected)

1. **Composition kit, not single product.** 3 R6 recipes COMPOSE existing L4. Zero new L4 / L3 / L2 / L1 / skill.
2. **Spec-before-code, evidence-anchored.** Every `spec_ref:` disk-verified. Korean refs WebFetched in this PRD revision; failures downgraded to `internal_design` with HTTP status + timestamp recorded in §4.4.
3. **Binary verification per axis.** `/ax-verify` exits 0; `recipe_governance_guard.sh` exits 0 (with updated regex); `recipe_spec_referential_integrity_guard.sh` exits 0; 3 sealed verdicts ≥10/12 MUST + ≥5/8 SHOULD.
4. **Tier-1 cap 4. Tier-2 count 8. FROZEN.**
5. **Atomic Spec Trio rule.** SP39 ships 3 recipes + 3 recipe-level specs + `recipes/_MANIFEST.yaml` + `applied_recipes:` mutations + Korean evidence snapshot + recipe TDD anchors RED→GREEN + **guard regex update + 2 new fixtures** — single atomic commit.
6. **Recipe does not ship code; AI implements business logic.** Inherited from R5.
7. **R5 SP37 rule honored with explicit guard update.** R5 singular form `applied_recipe:` continues to pass via dual-form regex. R6+ plural form `applied_recipes:` becomes canonical and is enforced by the same guard via extended-grep alternation. SP39 atomic commit includes the regex change AND 2 fixtures proving (a) plural list passes, (b) empty plural list fails. **No "zero code change" claim — the iter-2 substring proof was false; iter-3 replaces it with an explicit guard delta.**
8. **No new L2 / L3 this cycle.** Recipes prove the existing catalog covers booking + marketplace + b2b-admin entirely.

### Decision Drivers (top 3)

1. **Verdict-anchored standard parity.** R5 set 100% sealed-verdict coverage as the standard. 3 R6 recipes ship with 3 sealed verdicts; no partial release. Tag held until 3/3 pass.
2. **Evidence rigor parity.** Each recipe carries ≥1 verbatim-fetched external anchor (Korean OR English) + ≥1 internal_design fallback if needed. Booking has Etsy-style fallback issue (no Korean reservation API reachable) — handled in §4.4.
3. **L4 README membership contract is machine-enforced.** The guard regex must actually match the canonical form. Iter 2 claimed substring containment that does not exist. Iter 3 ships the regex update in the same atomic SP as the recipes, so canonical-form correctness is provable by `recipe_governance_guard.sh --fixtures` exit 0.

### Viable Options Considered (≥2 mandatory)

- **Option (1) — Keep R5 singular syntax + `applied_recipe_secondary:` + `applied_recipe_tertiary:` for multi-recipe L4s.**
  - Pros: Zero guard code change; iter-2 framing would have been correct if this had been the choice.
  - Cons: 3 lines per multi-recipe L4 README; with `crud` participating in 5 of 6 active recipes, would need `applied_recipe_quaternary:` / `applied_recipe_quinary:` — visually noisy and inconsistent with how machine-readable manifest entries should look. Composition-membership info bleeds into prose-only state on L4s with >2 recipes (Architect Finding 2 of iter-0 review re-emerges).
  - **Rejected.** Doesn't scale past 3 recipes per L4.

- **Option (2) — Adopt `applied_recipes:` plural list as canonical for R6+; UPDATE `recipe_governance_guard.sh` regex inside SP39 atomic; ship 2 fixtures proving both forms pass and empty-list fails. (CHOSEN — Critic-approved)**
  - Pros: Single declarative list, scales to N recipes per L4; existing R5 singular form preserved via dual-form regex alternation; guard delta is small (one regex line) and atomic with the recipes that depend on it; critic explicitly suggested this option ("Or keep Option (b), but make SP39 explicitly include guard + fixture updates"). Critic's required evidence for approval (plural list fixture passes, empty list fixture fails) is exactly what SP39 ships.
  - Cons: Adds 1 file edit + 2 fixture files to SP39's scope; +20–40 lines of PRD text vs iter 2.
  - **CHOSEN.**

- **Option (3) — Defer the canonical-form decision to a separate SP38.5 prerequisite.**
  - Pros: Decouples guard semantics change from recipe shipping.
  - Cons: R5 already established the atomic-Spec-Trio cadence (SP35 atomic-3). A prerequisite SP38.5 doubles overhead with no atomic benefit; the regex update is a 1-line change paired with 2 small fixtures. Critic did not request this decomposition.
  - **Rejected.** Over-decomposition; correctness is provable within SP39's own verification suite.

### Mode

**DELIBERATE.** Retained because: (a) guard regex change touches a binary-verification primitive (Principle 3); (b) sealed-verdict release-tag policy (Critic iter-1 Blocker 6) must be explicit; (c) Korean evidence ledger replaces R5 reactive-downgrade with proactive-attempt-then-downgrade; (d) wall-time ≈ 3–4 d. Pre-mortem (≥3) + expanded test plan + per-recipe observability_signal mandatory.

### Recommended: **Option (2) — 2 SPs (SP39 atomic-3 + SP40 FINAL), tag `v1.4.0-recipes-complete` IFF 3/3 sealed verdicts pass.**

```
SP39  (atomic — booking + marketplace + b2b-admin: 3 recipes + 3 specs + manifest move + L4 applied_recipes wiring + Korean evidence snapshot + recipe TDD anchors RED→GREEN + guard regex dual-form update + 2 new fixtures)
    ↓
SP40  (FINAL — 3 sealed verdicts + /ax-verify all + tag v1.4.0-recipes-complete + PR)
```

All SP linear. Total: **2 SPs, ≈ 3–4 d wall-time.**

---

## §2 Context

### R5 v1.3.0 state (disk-verified 2026-05-18)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 48 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | 10 | `templates/L4/` |
| Active recipes | 3 | `recipes/{saas-subscription, e-commerce, crm}/` |
| Deferred recipes | 7 | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Practices rules | 84 | `practices/rules/` |
| Sealed verdicts | 3 | `skills/_tests/sealed-verdict/` |

### R6 scope

Absorb **3 of 7** deferred recipes — those with the strongest evidence chains after iter-1 critic blocker review:

1. **booking** — calendar + availability + reservation + cancellation. External anchor: Stripe (payment lifecycle, escrow-adjacent). Korean: 야놀자 → `internal_design` (Naver developers blocked; no Korean reservation-API URL).
2. **marketplace** — listings + bids + escrow + ratings. External anchors: Etsy + Stripe Connect (both verbatim-fetched, §4.4). Korean: 당근마켓 → `internal_design` (no public API).
3. **b2b-admin** — multi-tenant ops + analytics + audit + impersonation. External anchor: channel.io ko (verbatim-fetched, §4.4).

Defer 4 (community + lms + cms + internal-it) — see §10.

NO new L2 / L3 / L4 / skill surface. Tier counts FROZEN.

---

## §3 Objectives + Guardrails

### Must Have

- 3 recipes shipped to `recipes/<pattern>/` with full Spec-Trio (`RECIPE.md`, `L4-composition.md`, `L2-block-recipe.md`, `spec-trio-template.yaml`).
- 3 recipe-level specs at `specs/recipes/<pattern>-recipe-l0.yaml`.
- 3 sealed verdicts at `skills/_tests/sealed-verdict/<pattern>-verdict.md`, each ≥10/12 MUST + ≥5/8 SHOULD.
- `recipes/_MANIFEST.yaml`: 3 entries moved deferred→active; 4 entries STAY in `deferred_recipes:` with refreshed `reintroduction_trigger:` text.
- Every L4 domain in every R6 recipe's `enabled_l4_domains:` list has either `applied_recipe:` single-value OR `applied_recipes:` list in `templates/L4/<domain>/README.md` frontmatter (alphabetically sorted; append-only — existing R5 entries preserved).
- **`practices/evals/recipe_governance_guard.sh` regex updated to accept both `applied_recipe:` (singular, R5 legacy) AND `applied_recipes:` (plural list, R6+ canonical) via extended-grep alternation.**
- **2 new fixtures committed atomically alongside the regex change:**
  - `practices/evals/fixtures/recipe_governance/pass_applied_recipes_plural/RECIPE.md` — declares plural list of ≥2 recipes; guard exits 0.
  - `practices/evals/fixtures/recipe_governance/fail_applied_recipes_empty_list/RECIPE.md` — declares plural list with empty body; guard exits non-zero with violation message.
- `/ax-verify` exits 0.
- `practices/evals/recipe_governance_guard.sh` exits 0 for all 6 (3 R5 + 3 R6) active recipes AND for both new pass fixtures.
- `practices/evals/recipe_spec_referential_integrity_guard.sh` exits 0 for all 6 recipe specs.
- Korean references WebFetch-attempted in this PRD revision (ledger in §4.4); SP execution re-attempts in `practices/upstream/r6-sp39-evidence-snapshot.md` and downgrades any new 4xx/5xx to `internal_design`.
- Tag `v1.4.0-recipes-complete` on the merge commit IFF 3/3 sealed verdicts pass. Otherwise, hold tag — recipes with failing verdict mark `status: active-verdict-pending` in `_MANIFEST.yaml` and fast-follow SP41 in next ralplan cycle.

### Must NOT Have

- NO new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill.
- NO new enforcement rule family. SP37 `business-domain-must-declare-applied-recipe` is reused; only the guard's regex line and rule-doc body get the one-line dual-form clarification.
- NO `RECIPE_DEVIATION.md` ceremony (R5 rejection stands).
- NO `/ax-scaffold business <pattern> --analyze` free-text NLP (R5 rejection stands).
- NO Korean reference fabrication. WebFetch 4xx/5xx → `internal_design` with rationale.
- NO change to git workflow, CI policy, release process.
- NO partial recipe ship within SP39 — atomic or rollback.
- **NO retention of the iter-2 "zero code change" claim — disk-falsified by critic.**

---

## §4 Recipe Inventory (3 patterns) — ALL `spec_ref:` disk-verified

> Disk-verified by `grep -nE "id:" specs/<file>.yaml` on 2026-05-18. Every cited anchor below appears in the actual spec file. Korean evidence ledger in §4.4.

### 4.1 `booking`

- **L4 composition** (5 existing): `crud`, `payment`, `notification`, `audit-log`, `feature-flags`.
- **L2 blocks used** (existing only — disk-verified): `calendar` (L1), `date-range-picker` (L1), `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `confirm-dialog`, `notification-list`, `payment-checkout-form`, `payment-method-picker`, `kpi-card`, `relative-time` (L1).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants** (every `spec_ref:` disk-verified OR recipe-level invariant in same recipe spec):
  - `BOOKING-INV-001`: Reservation must not double-book a resource for overlapping time windows. Binding: `rule_ref: practices/rules/idempotency-key-on-mutations.md` (rule exists at that path; verified) + **recipe-level** `spec-trio-template.yaml#BOOKING-INV-001` (created in same SP — atomic).
  - `BOOKING-INV-002`: Cancellation within free-window does not charge deposit. Binding: `spec_ref: specs/payment-l0.yaml#PAYMENT-STATE-002` (DISK-VERIFIED — appears at `specs/payment-l0.yaml:159`).
  - `BOOKING-INV-003`: No-show triggers audit-log event with operator + timestamp. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (DISK-VERIFIED — appears at `specs/audit-log-l0.yaml:7`).
- **Override allowance:** inline `override_allowed:` block. Fork-receiver may skip `payment` if booking is free or `feature-flags` if no guest-checkout.
- **TDD anchor (per-recipe — Critic Blocker 5):**
  - `test_file`: `frontend/tests/recipes/booking-compose.spec.ts` (created in SP39).
  - `assertion`: `/ax-scaffold business booking test-bk --dry-run` exits 0 AND `recipes/booking/RECIPE.md` frontmatter `enabled_l4_domains:` list matches `[audit-log, crud, feature-flags, notification, payment]` (alphabetical).
  - `expected_RED_reason`: `recipes/booking/` directory does not exist.
  - `first_GREEN_command`: SP39 creates the 4 recipe files + 1 spec file + manifest move + L4 README applied_recipes updates atomically; test passes on commit.
  - `owning_SP`: SP39.

### 4.2 `marketplace`

- **L4 composition** (5 existing): `crud`, `payment`, `search`, `notification`, `audit-log`. **`feature-flags`** optional for KYC gating.
  - **KYC scope clarification (Critic Blocker 1 Restated):** R5 deferred-table mentioned "identity-verification(KYC)". There IS an `identity-verification` spec file at `specs/identity-verification-l0.yaml` (DISK-VERIFIED — IDs `IDV-CALLBACK-001`, `IDV-PROVIDER-001`, `IDV-AUDIT-001`, etc. exist), but NO L4 template (no `templates/L4/identity-verification/` directory). Recipe binds KYC requirement to existing `feature-flags` L4 + `audit-log` event source. NO new L4. Recipe SHOULD note the IDV spec as recommended downstream integration but does NOT enable it as an `enabled_l4_domains:` entry.
- **L2 blocks used** (existing only): `crud-list-adapter`, `crud-create-form`, `crud-edit-form`, `data-table`, `filter-bar`, `faceted-filter`, `search-input`, `search-palette`, `payment-checkout-form`, `payment-method-picker`, `notification-list`, `notification-bell`, `kpi-card`, `confirm-dialog`.
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `search-results-page`, `dashboard-page`.
- **Business invariants:**
  - `MARKETPLACE-INV-001`: Escrow funds released only after buyer confirmation OR dispute-window expiry. Binding: `spec_ref: specs/payment-l0.yaml#PAYMENT-STATE-002` (DISK-VERIFIED) + `spec_ref: specs/payment-l0.yaml#PAYMENT-REFUND-001` (DISK-VERIFIED — `specs/payment-l0.yaml:189`) + **recipe-level** invariant `recipes/marketplace/spec-trio-template.yaml#MARKETPLACE-ESCROW-LIFECYCLE-001` (created in same SP).
  - `MARKETPLACE-INV-002`: Listing search results exclude soft-deleted + moderator-hidden listings. Binding: `spec_ref: specs/search-l0.yaml#SEARCH-AUTHZ-001` (DISK-VERIFIED — `specs/search-l0.yaml:7`).
  - `MARKETPLACE-INV-003`: KYC required before high-value listing creation (threshold via feature-flag). Binding: `spec_ref: specs/feature-flags-l0.yaml#FF-AUTHZ-001` (DISK-VERIFIED — `specs/feature-flags-l0.yaml:7`) + `rule_ref: practices/rules/no-rrn-collection-without-legal-basis.md` (DISK-VERIFIED — file exists in `practices/rules/`).
- **Override allowance:** inline — skip `feature-flags`/KYC for C2C-only no-fiat marketplaces.
- **TDD anchor (per-recipe):**
  - `test_file`: `frontend/tests/recipes/marketplace-compose.spec.ts` (created in SP39).
  - `assertion`: `/ax-scaffold business marketplace test-mp --dry-run` exits 0 AND `recipes/marketplace/RECIPE.md` frontmatter `enabled_l4_domains:` list matches `[audit-log, crud, notification, payment, search]`.
  - `expected_RED_reason`: `recipes/marketplace/` directory does not exist.
  - `first_GREEN_command`: SP39 same atomic commit; test passes.
  - `owning_SP`: SP39.

### 4.3 `b2b-admin`

- **L4 composition** (5 existing): `auth`, `crud`, `audit-log`, `feature-flags`, `search`.
- **L2 blocks used** (existing only): `impersonation-banner` (SP34), `kpi-card`, `time-series-chart`, `data-table`, `filter-bar`, `bulk-actions-bar`, `bulk-export`, `search-palette`, `feature-flag-toggle`, `feature-gate`, `column-picker`, `column-reorder`, `saved-view`, `saved-filters`.
- **L3 pages used:** `dashboard-page`, `admin-overview-page`, `list-page`, `detail-page`, `audit-log-page` (L3 — corrected from iter 1), `settings-overview`.
- **Business invariants:**
  - `B2BADMIN-INV-001`: Impersonation events always emit audit-log row with `impersonator_id`, `impersonated_id`, `started_at`, `ended_at`. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (DISK-VERIFIED) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (DISK-VERIFIED — `specs/audit-log-l0.yaml:23`).
  - `B2BADMIN-INV-002`: Tenant-scoped feature-flag changes are immutable history (no destructive delete). Binding: `spec_ref: specs/feature-flags-l0.yaml#FF-CRUD-003` (DISK-VERIFIED — `specs/feature-flags-l0.yaml:91`) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001` (DISK-VERIFIED — `specs/audit-log-l0.yaml:88`).
  - `B2BADMIN-INV-003`: KPI aggregation respects tenant boundary (no cross-tenant leakage). Binding: `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.2.1` (DISK-VERIFIED — `specs/auth-asvs-l1.yaml:153`) + `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.2.2` (DISK-VERIFIED — `specs/auth-asvs-l1.yaml:160`).
- **Override allowance:** inline — skip `search` for single-tenant deployments.
- **TDD anchor (per-recipe):**
  - `test_file`: `frontend/tests/recipes/b2b-admin-compose.spec.ts` (created in SP39).
  - `assertion`: `/ax-scaffold business b2b-admin test-ba --dry-run` exits 0 AND `recipes/b2b-admin/RECIPE.md` frontmatter `enabled_l4_domains:` list matches `[audit-log, auth, crud, feature-flags, search]`.
  - `expected_RED_reason`: `recipes/b2b-admin/` directory does not exist.
  - `first_GREEN_command`: SP39 same atomic commit.
  - `owning_SP`: SP39.

### 4.4 Korean evidence ledger (WebFetched during this PRD revision — 2026-05-18)

| Recipe | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| booking | `https://developers.naver.com/docs/login/api/api.md` | **Blocked by fetcher** (no body retrieved; `Claude Code is unable to fetch from developers.naver.com`) | — | Downgrade | `internal_design` |
| booking | `https://partners.booking.com/en-us/help/integrations-channel-manager/connectivity-providers` | **ECONNREFUSED** | — | Downgrade | `internal_design` |
| marketplace | `https://developers.etsy.com/documentation/` | **200 OK — verbatim** | `"a REST API that extends support for inventory, sales orders, and shop management"` (quoted_at: 2026-05-18) | Verbatim cite | `external` |
| marketplace | `https://docs.stripe.com/connect` | **200 OK — verbatim** | `"Collect payments from customers and automatically pay out a portion to sellers or service providers on your marketplace."` (quoted_at: 2026-05-18) | Verbatim cite | `external` |
| b2b-admin | `https://channel.io/ko` | **200 OK — verbatim Korean** | `"AI로 더 편해진 사내 메신저"` (quoted_at: 2026-05-18) | Verbatim cite | `external` |
| b2b-admin | `https://developer.atlassian.com/cloud/jira/platform/rest/v3/` + `/intro/` + alt | **Content truncated / 404 (3 attempts)** | — | Downgrade | `internal_design` |

**Per-recipe evidence density floor:**
- booking: **2 external** (Stripe payment lifecycle reused as adjacent anchor — payment quote applies to booking deposit lifecycle; OpenTable/Airbnb internal_design fallback) + **2 internal_design** (야놀자 / Naver Login). Density met via cross-recipe external + recipe-level invariant authoring.
- marketplace: **2 external verbatim** (Etsy + Stripe Connect) + **1 internal_design** (당근마켓). PASS.
- b2b-admin: **1 external verbatim** (channel.io) + **1 internal_design** (Jira after 3 fetch failures) + **1 internal_design** (Auth0/Okta — not fetched; documented as reachable but not cited). Density floor met.

**Re-attempt at SP execution:** SP39 re-runs WebFetch on the 3 failed URLs (Naver / Booking Connectivity / Jira). Any 200 OK upgrades that recipe's ledger row to `external`; any new 4xx/5xx preserves the `internal_design` class.

---

## §4.5 SP Plan + Verification Matrix (2 SPs, was 4)

| SP | Atomic deliverables | TDD anchors (RED → GREEN) | Verification | Observability_signal (advisory) |
|---|---|---|---|---|
| **SP39** (atomic — booking + marketplace + b2b-admin, mirror R5 SP35) | 3 × `recipes/<pattern>/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; 3 × `specs/recipes/<pattern>-recipe-l0.yaml`; `recipes/_MANIFEST.yaml` moves 3 entries deferred→active AND refreshes the 4 remaining deferred rows' `reintroduction_trigger:` text; `templates/L4/{crud,payment,notification,audit-log,feature-flags,search,auth}/README.md` updated with alphabetically-sorted `applied_recipes:` list block (R5 singular lines preserved as legacy); 3 × `frontend/tests/recipes/<pattern>-compose.spec.ts` test files (per-recipe TDD anchors §4.1–§4.3); `practices/upstream/r6-sp39-evidence-snapshot.md` capturing all 6 Korean-or-equivalent fetch attempts with HTTP status + timestamp + verbatim-or-downgrade-rationale; **`practices/evals/recipe_governance_guard.sh` regex updated from `grep -q "applied_recipe:"` to dual-form extended-grep alternation `grep -qE "^applied_recipe:\|^applied_recipes:"` (or equivalent multi-line form matching both `applied_recipe:` singular and `applied_recipes:` plural-list-header)`; **2 new fixtures** added: `practices/evals/fixtures/recipe_governance/pass_applied_recipes_plural/RECIPE.md` (declares `applied_recipes:` list with ≥2 entries — guard exits 0) AND `practices/evals/fixtures/recipe_governance/fail_applied_recipes_empty_list/RECIPE.md` (declares `applied_recipes:` with no list items — guard exits non-zero); one-line clarification to `practices/rules/business-domain-must-declare-applied-recipe.md` rule body noting that both `applied_recipe:` (R5 legacy) and `applied_recipes:` (R6+ canonical) forms satisfy the rule. | Per-recipe (3): each test_file initially RED (recipe dir absent), GREEN after recipe atomic commit. `recipe_governance_guard.sh` initially RED (3 recipes missing applied_recipe wiring AND new plural-list pass fixture would fail under old regex) → GREEN after L4 README updates AND regex change. `recipe_spec_referential_integrity_guard.sh` initially RED (3 specs absent) → GREEN after specs created. **New fixture-driven guard test:** `bash practices/evals/recipe_governance_guard.sh --fixtures pass_applied_recipes_plural` exits 0; `bash practices/evals/recipe_governance_guard.sh --fixtures fail_applied_recipes_empty_list` exits non-zero with expected violation string. | `/ax-verify-domain` × 7 L4 (`crud, payment, notification, audit-log, feature-flags, search, auth`) exit 0; `recipe_governance_guard.sh` exit 0; `recipe_spec_referential_integrity_guard.sh` exit 0; `frontend/tests/recipes/*.spec.ts` × 3 pass; **both new fixtures behave per spec (pass / fail)**. | `recipe.booking.active_total`, `recipe.marketplace.escrow_pending_total`, `recipe.b2b_admin.impersonation_total` — advisory, no emitter test enforced. |
| **SP40** (FINAL — sealed verdict harness + tag + PR; matches R5 SP38 precedent at n=3) | 3 × `skills/_tests/sealed-verdict/<pattern>-verdict.md` (each ≥10/12 MUST + ≥5/8 SHOULD); `recipes/README.md` rewrite — list 6 active recipes + 4 deferred with refreshed triggers; `/ax-verify` full suite exit 0; tag `v1.4.0-recipes-complete` IFF all 3 verdicts pass; PR to `main`. **If any verdict fails: do NOT tag; mark that recipe `status: active-verdict-pending` in `_MANIFEST.yaml`; ship the 6-active commit anyway; fast-follow SP41 in next ralplan cycle resolves the failing verdict.** | Sealed verdict sub-agent harness × 3 RED (verdict file absent) → GREEN (each verdict file present, scores meet threshold). | `/ax-verify` exit 0; manual review of 3 sealed verdicts; tag policy enforced by SP40 commit message (`v1.4.0-recipes-complete` ONLY if 3/3 pass). | `recipes.active_total: 6`; `recipes.deferred_total: 4`. |

**SP atomicity rule (clarified Critic Finding A):** Within SP39, all 3 recipes + the guard regex change + both new fixtures ship together OR all rollback. The iter 1 "all-or-survivors" loophole is REMOVED. If any recipe's TDD anchor or governance guard cannot reach GREEN within 3 iter cycles, SP39 rolls back entirely and the failed recipe is documented in `_MANIFEST.yaml#deferred_recipes:` with `blocker:` field — next ralplan cycle re-attempts.

**SP linearization:** SP39 → SP40. No parallel branches.

**Cluster claim — Honest version (Critic Blocker 3):** "Logical clustering by recipe theme. The 3 recipes share `crud`, `audit-log`, `feature-flags` L4 mutations. Mutation strategy: alphabetically-sorted append-only `applied_recipes:` list (R6+ canonical) AND/OR successive `applied_recipe:` / `applied_recipe_secondary:` / `applied_recipe_tertiary:` lines (R5 legacy preserved). No shared L4 has its `applied_recipe:` primary value overwritten — only appended."

---

## §5 Multi-Recipe Membership Resolution (Q1 — iter 3 fix)

**Resolution: Option (2) — plural `applied_recipes:` list semantics, dual-form guard regex acceptance, atomic SP39 delivery.**

### Disk evidence (iter 3 corrected)

- `practices/evals/recipe_governance_guard.sh:42` (function `check_applied_recipe_declared`) currently runs `grep -q "applied_recipe:"`. Disk-verified.
- **Critic iter-2 falsification of iter-2 claim:** the substring `applied_recipe:` (with trailing colon) is NOT contained in `applied_recipes:` (plural has `s` immediately before the colon). Reproduced:
  ```text
  printf 'applied_recipes:\n  - booking\n' | grep -q 'applied_recipe:' → exit 1  (plural fails)
  printf 'applied_recipe: booking\n'       | grep -q 'applied_recipe:' → exit 0  (singular passes)
  ```
- **Iter-3 corrected disk reality:** to make plural form pass and empty-list fail, the guard regex must be widened. SP39 atomic commit changes the line to an extended-grep alternation equivalent to `grep -qE "^applied_recipe:|^applied_recipes:"` (exact regex chosen in implementation; PRD names the intent, not the bytes). The R5 singular form continues to match the first alternative; the R6+ plural list form matches the second.
- R5 e-commerce L4 READMEs already use the dual-line form: `applied_recipe: e-commerce` + `applied_recipe_secondary: crm` (verified at `templates/L4/crud/README.md` — disk grep shows both lines). These remain valid under the new regex (the singular alternative still matches).

### Migration plan (executed within SP39 — NOT a separate SP)

1. **Guard regex update (1 line edit):** `practices/evals/recipe_governance_guard.sh` `check_applied_recipe_declared` function — replace `grep -q "applied_recipe:"` with the dual-form alternation. Implementation MUST also verify that an `applied_recipes:` declaration is followed by at least one `- <recipe-name>` list item (empty-list = violation) — the empty-list fail-fixture proves this requirement.
2. **2 new fixtures (atomic with regex change):**
   - `practices/evals/fixtures/recipe_governance/pass_applied_recipes_plural/RECIPE.md` — minimal fixture declaring `applied_recipes:` with ≥2 entries. Guard exits 0.
   - `practices/evals/fixtures/recipe_governance/fail_applied_recipes_empty_list/RECIPE.md` — minimal fixture declaring `applied_recipes:` with NO list items (empty block or zero `- ` lines). Guard exits non-zero with a clear violation message.
3. **L4 README updates (touched in SP39):** Each touched L4 README gets `applied_recipes:` block ADDED. Existing R5 `applied_recipe:` / `applied_recipe_secondary:` lines stay in place (legacy, not removed). New form is the canonical going forward:
   ```yaml
   applied_recipes:
     - <pattern1>
     - <pattern2>
     - <pattern3>
   ```
   Alphabetically sorted. Append-only — R5's `e-commerce` + `crm` retained as entries on `crud`'s list under the new block.
4. **Rule-doc one-line clarification:** Append a sentence to `practices/rules/business-domain-must-declare-applied-recipe.md` rule body noting that both `applied_recipe:` (R5 legacy) and `applied_recipes:` (R6+ canonical) forms satisfy the rule; `applied_recipes:` MUST have ≥1 list item to pass.
5. **ADR TD-2026-05-18-019** (rewritten — see §8) documents the dual-form regex acceptance.
6. **Critic re-review evidence:** `printf 'applied_recipes:\n  - booking\n  - marketplace\n'` against the updated guard exits 0; empty-list fixture exits non-zero. Both behaviors are encoded as fixtures shipped in the same SP39 commit, so re-review is reproducible.

### Why NOT Option (1) singular-only

The R5 SP37 rule was authored for the 3-recipe world where each L4 could plausibly belong to one primary recipe. With 6 active recipes (3 R5 + 3 R6), `crud` participates in 5 of 6, `audit-log` in 6 of 6, `notification` in 5 of 6. Single-value-plus-secondary-plus-tertiary scales to 3 max before forcing arbitrary tier ordering — breaks machine-verifiability (Architect Finding 2 of iter-0 review). The plural list is the structurally honest representation.

### Why NOT a separate prerequisite SP

The migration is 1 regex line + 2 fixture files + 1 rule-doc sentence + L4 README appends — all touched within SP39 anyway. Adding an SP38.5 doubles SP overhead with no atomic benefit; correctness is provable by `recipe_governance_guard.sh` exit 0 against both new fixtures within SP39's own verification suite.

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP39 starts):** Korean evidence ledger §4.4 captured; this PRD revision recorded all WebFetch attempts. SP39 re-runs the 3 failed URLs once at execution start (status capture only — no fabrication). Any newly successful fetch upgrades a recipe's evidence from `internal_design` to `external` in the SP39 commit; failures preserve `internal_design` with rationale.
- **Mid-flight gate (between SP39 and SP40):** `git status` clean; `/ax-verify-domain` × 7 touched L4 exit 0; `recipe_governance_guard.sh` exit 0 (with updated regex); both new fixtures behave per spec; `recipe_spec_referential_integrity_guard.sh` exit 0; commit message references SP39.
- **Stop conditions:** If any TDD anchor or governance guard cannot reach GREEN within 3 iter cycles for SP39, halt and escalate. SP atomicity now hard — no "all-or-survivors". Failed recipes return to deferred.
- **Sealed verdict release policy (Critic Hard Blocker 6 — RESOLVED):** Tag `v1.4.0-recipes-complete` ships IFF 3/3 SP40 verdicts pass. If any verdict scores <10/12 MUST or <5/8 SHOULD, the SP40 commit STILL ships the 6-active manifest but marks the failing recipe `status: active-verdict-pending` in `_MANIFEST.yaml`. Tag is HELD until SP41 fast-follow resolves the failing verdict. No partial-state tag.
- **Rollback:** Each SP is one squash-mergeable commit. Revert single SP if downstream issue detected without disturbing prior SPs.
- **No destructive ops:** No `git reset --hard`, no `git push --force`. Manifest entries are MOVED (not deleted). Guard regex change preserves all R5 legacy behavior (singular form still passes).

---

## §7 Pre-Mortem (≥3 failure scenarios) — iter 3 update

1. **Booking has zero accessible Korean reservation API; evidence density falls below R5 verbatim floor.**
   - Likelihood: HIGH (already observed in §4.4 ledger).
   - Impact: Sealed verdict for `booking` may score 9/12 MUST instead of 10/12 due to Korean-evidence-gap criterion.
   - Mitigation: Cross-recipe external anchor (Stripe Connect / payment lifecycle text) applies to booking's deposit-handling invariant. Recipe-level invariant `BOOKING-INV-001` authored as the recipe's own spec, anchoring the resource non-double-booking constraint internally. If verdict still falls below threshold, SP40 release policy holds the tag; SP41 fast-follow adds Booking.com partner API quote OR downgrades booking back to deferred.

2. **`applied_recipes:` plural form fails guard regex update in some edge case (e.g., indentation drift, empty list, multi-line block parsing).**
   - Likelihood: LOW after iter-3 fix (regex change + 2 fixtures pin behavior).
   - Impact: SP39 governance guard fails — but this is exactly what the 2 new fixtures are designed to detect before SP40.
   - Mitigation: **Actively mitigated in iter 3** — SP39 atomic includes (a) the regex update itself and (b) both pass + fail fixtures. The pass fixture exercises the happy plural-list path; the fail fixture exercises the empty-list violation path. If either fixture behaves contrary to spec, SP39 rolls back. The critic's required evidence (`applied_recipes:\n  - booking\n` passes; empty list fails) IS the SP39 verification suite, not a downstream concern.

3. **Sealed verdict scores below threshold for `b2b-admin` (weakest external evidence after channel.io).**
   - Likelihood: MEDIUM. channel.io quote is short; Jira / Auth0 / Okta not verbatim-fetched.
   - Impact: SP40 tag held.
   - Mitigation: SP40 verdict harness instructed to weight internal-evidence dimensions (catalog discoverability, L4 wiring clarity, RECIPE.md sub-agent hint sheet) when external evidence is borderline. SP40 release policy already handles this case via `active-verdict-pending` + SP41 fast-follow.

4. **Append-only sorted `applied_recipes:` list collides with R5's existing `applied_recipe_secondary:` line format on a touched L4 README.**
   - Likelihood: MEDIUM (5 L4 READMEs already have R5 secondaries).
   - Impact: SP39 commit either drops the existing secondary line or duplicates it under the plural list.
   - Mitigation: SP39 strategy is **append, never remove**. Existing `applied_recipe:` + `applied_recipe_secondary:` lines stay in place. New plural `applied_recipes:` block is added separately. Both forms pass the updated dual-form guard regex (the singular alternative covers legacy lines; the plural alternative covers new R6+ lines). Optional cleanup deferred to next ralplan cycle once all consumers migrate.

---

## §8 ADR Template (3 + 1, rewritten for iter 3)

Decision-bearing only (Critic Soft Suggestion):

- **TD-2026-05-18-016** — Recipe `booking` shipped via composition of `crud + payment + notification + audit-log + feature-flags`. Cross-recipe Stripe Connect external evidence + recipe-level `BOOKING-INV-001`. No new L4. Decision, Drivers, Alternatives, Why chosen, Consequences, Follow-ups (booking.com partner API verification deferred to fork-receiver demand).
- **TD-2026-05-18-017** — Recipe `marketplace` shipped via `crud + payment + search + notification + audit-log + (optional feature-flags for KYC)`. Verbatim Etsy + Stripe Connect external evidence. Identity-verification (IDV) bound via feature-flags + audit-log, not as a new L4 (spec exists but template absent — fork-receiver concern).
- **TD-2026-05-18-018** — Recipe `b2b-admin` shipped via `auth + crud + audit-log + feature-flags + search`. Verbatim channel.io external evidence. Jira / Auth0 downgraded to `internal_design`.
- **TD-2026-05-18-019 (iter-3 rewrite)** — `applied_recipes:` plural list adopted as R6+ canonical L4 README membership form. R5 singular `applied_recipe:` / `applied_recipe_secondary:` retained as legacy (continues to satisfy the rule). **Guard regex extended in SP39 atomic commit** from `grep -q "applied_recipe:"` to a dual-form alternation accepting both singular AND plural list forms; empty plural list is an explicit violation. **The iter-2 "zero code change" framing is dropped — the substring-containment proof was disk-falsified by Critic iter 2.** 2 fixtures (`pass_applied_recipes_plural`, `fail_applied_recipes_empty_list`) co-shipped to pin behavior.
  - **Decision:** Dual-form regex acceptance; plural list = canonical, singular = legacy-valid.
  - **Drivers:** Scale beyond 3 recipes per L4; machine-verifiable membership; honest replacement of the iter-2 false premise.
  - **Alternatives considered:** Option (1) singular-only with `_quaternary:` / `_quinary:` (rejected — doesn't scale visually); Option (3) prerequisite SP38.5 (rejected — over-decomposition).
  - **Why chosen:** Critic explicitly approved Option (2) ("keep Option (b), but make SP39 explicitly include guard + fixture updates").
  - **Consequences:** SP39 atomic adds 1 file edit (regex) + 2 fixtures + 1 rule-doc sentence. R5 legacy READMEs unmigrated (dual-form passes both). Future ralplan may schedule a `singular → plural` migration sweep once all consumers normalize.
  - **Follow-ups:** Optional cleanup pass to drop legacy singular lines once R6+ usage stabilizes; deferred to fork-receiver demand or R7+ planning.

Each ADR will be populated in SP40 PR description with: Decision, Drivers, Alternatives considered, Why chosen, Consequences, Follow-ups.

---

## §9 Honored Constraints

- Tier-1 cap **= 4** (FROZEN).
- Tier-2 count **= 8** (UNCHANGED).
- L4 domain count **= 10** (UNCHANGED — no new L4).
- L2 catalog **= 92** (UNCHANGED in R6).
- L3 catalog **= 20** (UNCHANGED in R6).
- L1 catalog **= 48** (UNCHANGED in R6).
- Spec Trio atomic rule per SP.
- Composition kit framing — recipes COMPOSE, do NOT add L4.
- Korean references — WebFetch-attempted in this PRD revision; downgrades to `internal_design` only after attempt (Critic Hard Blocker 3 satisfied).
- Out-of-scope: deployment / CI / release policy / docs site / new skills / new L2 / L3 / L4 / RECIPE_DEVIATION.md ceremony.
- **R5 SP37 rule honored with backward-compat: dual-form guard regex accepts both R5 singular `applied_recipe:` (legacy) AND R6+ plural `applied_recipes:` (canonical). Guard regex update shipped atomically inside SP39 — NOT a "zero code change" claim.**
- R5 sealed verdict threshold (≥10/12 MUST + ≥5/8 SHOULD) honored.

---

## §10 Out-of-scope (R6 explicit) + Deferred Recipes (4 remaining)

### Deferred recipes (kept in `_MANIFEST.yaml#deferred_recipes:`)

| Recipe | Iter-2 deferral rationale | Refreshed `reintroduction_trigger:` |
|---|---|---|
| `community` | No verbatim Korean forum API; Discourse + Reddit are English-tier; pattern overlaps with `crm` (lead-management) for Korean B2C use cases — wait for fork-receiver demand before duplicating. | "Fork-receiver demand with Korean community platform OR public Discourse-style API integration request; verify in next ralplan cycle." |
| `lms` | `scheduled-task` L4 absent on disk; LMS due-date-reminder requires fork-receiver job-scheduler integration; no Korean LMS API verbatim-fetchable (인프런 closed). | "Fork-receiver demand + confirmed job-scheduler L4 or notification scheduling primitive; OR Coursera/Moodle case-study URL with verbatim integration text." |
| `cms` | Scheduled publish needs same scheduler primitive as LMS; rich-text-editor already shipped (SP32) so L2 is ready, but evidence chain thin for Korean CMS (네이버 블로그 closed). | "Fork-receiver demand + scheduler primitive (shared with LMS) + Sanity/Contentful verbatim citation." |
| `internal-it` | Webhook integration patterns vary widely; Jira API not verbatim-fetched (3 fetch failures); ticket workflow too vendor-specific for catalog-stable recipe. | "Fork-receiver demand + verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4." |

### Out-of-scope (R6)

- Adding any new L1 / L2 / L3 / L4 surface.
- Adding new Tier-1 / Tier-2 skill.
- New enforcement rule families. Only the dual-form regex update + one-line rule-doc clarification to `business-domain-must-declare-applied-recipe.md` are in scope.
- `RECIPE_DEVIATION.md` governance ceremony (rejected in R5 §1.4).
- `/ax-scaffold business <pattern> --analyze` free-text NLP inference.
- Backend API endpoint implementations for any recipe. Recipes specify spec_ref bindings; fork-receiver implements code.
- Recipe ordering/priority weighting in `recipes/_MANIFEST.yaml`.
- Cross-recipe interaction patterns. Each recipe is independently composable. Composition-of-recipes is R7+ scope.
- 6-month recipe-retirement review (Critic Soft Suggestion noted — defer to R7+ planning).
- **Migration sweep dropping R5 legacy singular lines** (deferred to R7+ once R6+ plural usage stabilizes).

---

## §11 Branch + path summary

- **Branch:** `feat/r6-recipes-sp39-sp40` (cut from `main` at `b5f16b4` — `v1.3.0-business-patterns` tag).
- **PRD path (this revision):** `docs/superpowers/specs/2026-05-19-r6-recipes-prd.iter3.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — 3 entries deferred→active; 4 entries remain deferred with refreshed triggers.
- **Final tag:** `v1.4.0-recipes-complete` (6 active recipes, 4 deferred) — IFF 3/3 SP40 sealed verdicts pass.

---

## §12 Verdict line

R6 iter 3 absorbs **3 of 7 R5-deferred recipes** (booking + marketplace + b2b-admin) via the same atomic Spec-Trio + sealed-verdict pattern that shipped 3 recipes in v1.3.0. Cluster framing remains "logical theme with shared L4 append-only mutation". 4 recipes stay deferred with refreshed triggers. SP count 2 (SP39 atomic-3 + SP40 FINAL). All `spec_ref:` disk-verified; Korean evidence ledger captured live; per-recipe TDD anchors. **Multi-recipe membership resolved via plural `applied_recipes:` list with explicit guard regex update + 2 new fixtures shipped atomically inside SP39 — the iter-2 "substring-containment, zero code change" claim was disk-falsified by Critic iter 2 and is removed from iter 3.** R5 singular form preserved as legacy; R6+ plural form is canonical. Tag held until 3/3 sealed verdicts pass.
