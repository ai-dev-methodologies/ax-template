# Business Pattern Recipes PRD — 2026-05-19 (Round 5, ralplan iter 2)

> **Status:** ITER 2 (Planner revision) responding to Architect Synthesis-A + Codex Critic iter 1 hard blockers (8). Awaits Codex Critic iter 2 narrow re-review.
> **Date:** 2026-05-18 (PRD slug 2026-05-19 — planning horizon). **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-17-frontend-templatization-prd.md` (CLOSED — SP1–SP12, `v1.0.0` baseline).
> - `2026-05-18-catalog-extension-prd.md` (CLOSED — SP13–SP22, `v1.0.0-catalog-complete`, commit `9212989`).
> - `2026-05-18-functional-extension-prd.md` (CLOSED — SP23–SP29, `v1.1.0-functional-complete`, commit `1ab8f54`).
> - `2026-05-18-p1-absorption-prd.md` (CLOSED — SP30–SP34, `v1.2.0-p1-absorbed`, commit `26de945`).
> **Branch (when execution starts):** `feat/business-patterns-sp35-sp38` (4 SPs).
> **Targeted tag:** `v1.3.0-business-patterns`.
> **Iter 2 delta from iter 1:** 10 recipes → **3**; SP38 5-deliverable bundle → **subcommand-only**; dropped `--analyze` free-text inference; dropped `/ax-verify-recipe` Tier-2; dropped `RECIPE_DEVIATION.md` governance ceremony; bound every `business_invariants` row to `spec_ref:` or `rule_ref:`; corrected `time-series-chart.tsx` disk-state error; structurally true Korean references with URLs; added per-SP TDD anchor table. SP35–SP40 (6 SPs) → SP35–SP38 (4 SPs).

---

## §1 RALPLAN-DR Summary (iter 2)

### Principles (inherited; unchanged from iter 1)

1. **Composition kit, not single product.** Recipes COMPOSE existing L4 domains; they do NOT define a new L4. Every recipe atom is fork-adoptable in isolation.
2. **Spec-before-code, evidence-anchored.** Every recipe carries `evidence:` block with real-world references (Stripe Billing, Shopify, Toss Payments, etc.). Every invariant resolves to an existing `spec_ref:` or `rule_ref:`.
3. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` returns exit 0. Recipes verify by fan-out: `/ax-verify-domain <each-enabled-L4>` exit 0 + new `recipe_spec_referential_integrity_guard.sh` exit 0.
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count **stays frozen at 4** (`/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`). Recipe scaffolding ships as `/ax-scaffold business <pattern>` SUBCOMMAND of existing `/ax-scaffold`. **No new Tier-2 added this cycle** (drop `/ax-verify-recipe` per Critic iter 1 hard blocker 4).
5. **Atomic Spec-Trio rule.** Recipes are NOT new Spec Trios — they are **composition manifests** referencing existing L4 Spec Trios. `specs/recipes/<pattern>-recipe-l0.yaml` is a NEW recipe-level spec family, but it does NOT register in `trio_integrity_allowlist.yaml` (no backend OpenAPI; pure composition contract). Recipe-level spec lands atomically with `recipes/<pattern>/RECIPE.md`.
6. **No speculative generality.** Recipe inventory is closed at **3 patterns this PRD**, every one with a sealed verdict (100% coverage, not 30%). Patterns 4–10 are explicit deferred candidates documented in `recipes/README.md` as backlog rows — they ship in a future PRD only when fork-receiver demand arrives.
7. **Recipe does not ship code; AI implements business logic.** Per user mandate — "비지니스 로직 판단이야 AI가 잘 해줄거자나" — recipes specify WHICH catalog atoms to compose, NOT the business logic itself.

### Decision Drivers (top 3, re-anchored for iter 2)

1. **User mandate prioritization × verdict-anchored standard.** The user mandate ("정해진 컴포넌트 구현이 어느정도 되었으면, 각 업무 비지니스 구현 방법에 대한 준비가 되어 있어야해") asks for "준비" — readiness — of business-pattern implementation guidance. It does NOT enumerate a count. The catalog's existing standard is that every shipped L4 has Spec Trio + tests + sealed verdict. Shipping recipes without sealed verdicts regresses below that standard. **Verdict coverage parity** is the binding constraint, not pattern count.
2. **Recipe self-discoverability metric (extends payment-domain SP precedent).** The L4 sealed sub-agent verdict (11/11 MUST + 6/6 SHOULD) proved catalog is self-discoverable. For each shipped recipe, the equivalent question — "given a business pattern, can a context-0 AI agent assemble the right L4 set?" — needs a sealed verdict. Three recipes × full sealed verdict ≥ ten recipes × partial coverage.
3. **Tier-1 cap preservation + Tier-2 stability.** Tier-1 cap = 4 preserved. **Tier-2 count stays at 8 (unchanged)**: `/ax-verify-recipe` is dropped per Critic blocker 4 — its behavior (loop over enabled L4 domains via existing `/ax-verify-domain`) is implemented inline in `/ax-scaffold business <pattern>` post-scaffold step plus the new referential-integrity guard. No new skill surface this cycle.

### Mode

**DELIBERATE.** Retained because: (a) wall-time still ≥1 week (4 SPs); (b) new artifact family `recipes/<pattern>/`; (c) 2 new enforcement rules with cross-recipe scope; (d) sealed verdict harness for 3 recipes. Pre-mortem (≥3 scenarios) + expanded test plan + observability_signal mandatory. Removed: free-text inference accuracy bar driver (no longer in scope).

### Viable Options Considered (≥2 mandatory)

- **Option A — Iter 1 default: 10 recipes + subcommand + `--analyze` + new Tier-2 + 3 enforcement rules.** **Rejected in iter 2.** Reasons: (a) 7 of 10 recipes lack sealed verdicts → regresses verdict-anchored standard; (b) `--analyze` introduces NLP surface with 80% accuracy gate that drops to 70% per Open Question — worse-than-baseline UX per Critic §G; (c) `/ax-verify-recipe` is a wrapper, not a new axis per Critic blocker 4; (d) `RECIPE_DEVIATION.md` is a governance ceremony per Critic blocker 7. Each is independently a HARD blocker.

- **Option A2 — Synthesis-A (CHOSEN): 3 verdict-anchored recipes + subcommand + 2 enforcement rules + 4 SPs.**
  - Pros: 100% sealed-verdict coverage; preserves Tier-1 cap = 4 AND Tier-2 count = 8; no NLP surface; no governance ceremony; matches L4 catalog standard.
  - Cons: only 3 patterns shipped this cycle; remaining 7 deferred to future PRDs (LMS, B2B-admin, CMS, internal-IT, marketplace, community, booking).
  - **Mitigation for cons:** `recipes/README.md` lists 7 deferred patterns as candidate rows with `status: deferred-pending-fork-receiver-demand` to make the gap visible and addressable.
  - **CHOSEN.**

- **Option B — 5 recipes (highest-frequency: saas-subscription, e-commerce, crm, booking, community) all with sealed verdicts + subcommand + 2 enforcement rules + 5 SPs.**
  - Pros: 100% sealed verdict coverage; broader pattern range.
  - Cons: 5 SPs vs 4; broader testing surface; 2 of the 5 (booking, community) have weaker Korean URL evidence than the 3 chosen (Critic iter 1 §L documented this).
  - **Rejected.** Marginal pattern coverage gain (3→5) does not justify additional cycle time when 3 already proves the framework. Future PRD expansion is straightforward once framework lands.

- **Option C — Recipes-only, no skill subcommand (catalog reference docs).**
  - Pros: minimum risk; zero skill changes.
  - Cons: no machine enforcement; recipes degrade to advisory under prompt drift.
  - **Rejected** (same reasoning as iter 1).

- **Option D — `/ax-blueprint` NEW Tier-1.**
  - Cons: BREAKS Tier-1 cap = 4.
  - **Rejected** (constraint violation).

- **Option E — Skill-only, no shipped recipes.**
  - Cons: violates evidence-anchoring; no shipped artifact.
  - **Rejected.**

### Recommended: **Option A2 (Synthesis-A) — 3 verdict-anchored recipes (`saas-subscription`, `e-commerce`, `crm`) + `/ax-scaffold business <pattern>` subcommand + 2 enforcement rules + sealed verdict harness for all 3 = 4 SPs (SP35–SP38).**

**SP count: 4.** Sequencing:

```
SP35 (recipes/ infrastructure + 3 recipe directories + 3 recipe-level specs + recipe_spec_referential_integrity_guard.sh)
    ↓
SP36 (/ax-scaffold business <pattern> <project-name> deterministic subcommand + SKILL.md scope update + METHODOLOGY.md Appendix C update)
    ↓
SP37 (2 enforcement rules: prefer-recipe-composition / declare-applied-recipe + failing fixtures + practices/evals/recipe_governance_guard.sh)
    ↓
SP38 (FINAL: sealed verdict harness for 3 recipes + /ax-verify all + tag v1.3.0-business-patterns + PR to main)
```

All SP linear (no parallel branch). Total: **4 SPs, ≈ 6–8 d wall-time.**

---

## §2 Context

### Completed cycles (verified disk state, 2026-05-18)

| Cycle | PRD | Tag | Commit | Surfaces touched |
|---|---|---|---|---|
| 1 | `2026-05-17-frontend-templatization-prd.md` | `v1.0.0` | (pre-tag) | SP1–SP12 |
| 2 | `2026-05-18-catalog-extension-prd.md` | `v1.0.0-catalog-complete` | `9212989` | SP13–SP22 |
| 3 | `2026-05-18-functional-extension-prd.md` | `v1.1.0-functional-complete` | `1ab8f54` | SP23–SP29 |
| 4 | `2026-05-18-p1-absorption-prd.md` | `v1.2.0-p1-absorbed` | `26de945` | SP30–SP34 |

### Current catalog totals (post-`v1.2.0-p1-absorbed`, disk-verified 2026-05-18)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 42+ | `templates/L1/components/` |
| L2 blocks | 91 (`time-series-chart.tsx` EXISTS; iter 1's claim of "new" was a disk-state error — corrected) | `ls templates/L2/blocks/ \| wc -l` |
| L3 page templates | 20 | `ls templates/L3/pages/` |
| L4 domain workloads | 10 (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, search) | `ls templates/L4/` |
| Backend-only domains | email-outbox, scheduled-task, ratelimit, identity-verification | `practices/evals/trio_integrity_allowlist.yaml` |
| Backend cross-cutting | `templates/backend/integration/` (Bulkhead, ExternalApi, WebClient, Webhook In/Out/Sender/Receiver) — EXISTS on disk, available to recipes as primitive | `templates/backend/integration/*.java` |
| Skills | 19 (Tier-1: 4, Tier-2: 8, Tier-3: 7) — **stays 19 this cycle; no new Tier-2** | `ls skills/` |
| Java rules | 81 → 83 (post-SP37 +2 enforcement rules) | `ls practices/rules/*.md \| wc -l` |
| React rules | 85 → 87 (post-SP37 +2 enforcement rules, Java + React halves) | `ls practices-react/rules/*.md \| wc -l` |
| Spec Trios | 13 (unchanged; recipes are NOT Spec Trios) | `specs/` cross-ref `contracts/` `blueprints/` |
| Guards | ≥19 → ≥21 GREEN (+recipe_spec_referential_integrity_guard.sh, +recipe_governance_guard.sh) | `practices/evals/*_guard.sh` |

### This PRD's scope

Admits **4 SPs covering 3 Business Pattern Recipes + 1 subcommand (`/ax-scaffold business`) + 2 enforcement rules + 1 sealed verdict harness for all 3 recipes**. Filter enforced in §3 Guardrails and §4 Inventory.

---

## §3 Objectives + Guardrails

### Objectives (one per SP)

- **O1 (SP35 — Recipes Infrastructure + 3 Verdict-Anchored Recipes):** Ship `recipes/` directory infrastructure + 3 recipes (`saas-subscription`, `e-commerce`, `crm`). Each recipe directory contains `RECIPE.md` + `spec-trio-template.yaml` (composition fragments for fork-receiver to copy) + `L4-composition.md` + `L2-block-recipe.md` + `evidence:` block (with structurally-true Korean URLs). NEW recipe-level Spec family: `specs/recipes/<pattern>-recipe-l0.yaml`. NEW guard: `practices/evals/recipe_spec_referential_integrity_guard.sh` (validates `enabled_l4_domains:` resolves to `templates/L4/<domain>/` AND every `business_invariants[*].spec_ref:` or `rule_ref:` resolves to an existing spec ID or rule file).
- **O2 (SP36 — Skill Subcommand):** Ship `/ax-scaffold business <pattern> <project-name>` deterministic subcommand (NOT new Tier-1; NOT free-text inference). Updates `skills/ax-scaffold/SKILL.md` description to scope BOTH "L4 domain skeleton" AND "business composition". Updates `METHODOLOGY.md` Appendix C with composition-flow section. Post-scaffold step in subcommand calls `/ax-verify-domain <each-enabled-L4>` in a loop (inline; no new Tier-2 skill).
- **O3 (SP37 — 2 Enforcement Rules):** Ship 2 enforcement rules (down from iter 1's 3; dropped `recipe-deviation-requires-justification`):
  - `prefer-recipe-composition-over-l4-cross-import` — if implementing a business that matches a Pattern Recipe (detected via `applied_recipe` metadata in target L4 README), MUST follow Recipe L4 composition; cross-import L4 ad-hoc FAILS.
  - `business-domain-must-declare-applied-recipe` — every L4 domain wiring under a recipe must declare `applied_recipe: <pattern-name>` in README or Spec Trio metadata.
  Each rule has Java + React halves (4 rule files total), each with failing fixture under `practices/evals/fixtures/<rule-id>/{positive,negative}/`. NEW guard: `practices/evals/recipe_governance_guard.sh`. **Override mechanism:** inline `override_allowed:` block in `recipes/<pattern>/RECIPE.md` frontmatter (NO separate `RECIPE_DEVIATION.md` file, NO 30-day WARN→HARD ceremony).
- **O4 (SP38 — Sealed Verdict for ALL 3 + Release):** Ship sealed verdict harness for **all 3 recipes** (saas-subscription, e-commerce, crm). Each verdict runs an L4-sealed sub-agent that, given ONLY the recipe RECIPE.md + catalog AGENTS.md, must reproduce the recipe's L4 composition with ≥10/12 MUST + ≥5/8 SHOULD. Tag `v1.3.0-business-patterns`. PR to main.

### Guardrails — Must Have

- Every recipe carries `evidence:` block (provenance_class: `internal_design` for composition logic + ≥1 `provenance_class: external` citation with URL + ≥1 Korean reference structurally formatted as `provenance_class: external` with URL).
- Every recipe RECIPE.md declares `enabled_l4_domains:` list, `l2_blocks_used:` list, `business_invariants:` list (every row binds to `spec_ref:` or `rule_ref:` resolving to existing artifact), `business_observability_advisory:` list (DEMOTED from "obligation" to "advisory" per Critic — iter 1 over-promised binding without emitter test).
- Every L4 domain wired by a recipe carries `applied_recipe: <pattern-name>` annotation in its README.md or Spec Trio metadata (enforced by rule 2).
- Skill topology cap: **Tier-1 count frozen at 4. Tier-2 count frozen at 8.** Recipe scaffolding = `/ax-scaffold business` subcommand. Recipe verification = existing `/ax-verify-domain` loop + new referential-integrity guard. NO new skill in this cycle.
- Atomic Spec-Trio rule: recipe-level spec (`specs/recipes/<pattern>-recipe-l0.yaml`) lands ATOMICALLY with `recipes/<pattern>/RECIPE.md` in the same commit cluster.
- Recipe-level spec is NOT registered in `trio_integrity_allowlist.yaml`. Referential-integrity guard validates `enabled_l4_domains:` resolves AND each `business_invariants[*].spec_ref:` or `rule_ref:` resolves.
- All 3 recipes get sealed verdict in SP38 (100% coverage). No mixed standard.
- All Korean references in `evidence:` carry `provenance_class: external` + URL + `citation:` (verbatim short quote) + `quoted_at: 2026-05-18`. If a claim cannot be backed by URL+citation, downgrade to `internal_design` with rationale (no marketing prose pretending to be evidence).

### Guardrails — Must NOT

- **No new Tier-1 skill** (preserves cap = 4).
- **No new Tier-2 skill** (Tier-2 count stays 8; `/ax-verify-recipe` dropped per Critic blocker 4).
- No new L4 domain created BY a recipe.
- **No free-text inference (`--analyze` dropped per Critic blocker 3).** Recipe selection is by user/AI explicit name choice in `/ax-scaffold business <pattern>` deterministic subcommand. NO NLP, NO accuracy gate, NO 50-fixture eval suite. Re-introduce as future PRD only when real fork-receiver prompt logs justify it.
- **No `RECIPE_DEVIATION.md` file** (dropped per Critic blocker 7). Override mechanism is INLINE in recipe frontmatter as `override_allowed:` block. Single binary check: comply OR cite inline override.
- **No 30-day WARN→HARD transition** for any rule. Rules ship HARD from day one with their failing fixtures; if a fixture is wrong, fix the fixture, don't soft-launch the rule.
- No GitHub Actions, LICENSE, CONTRIBUTING, docs-site, release.yml, Dependabot, GitHub Pages.
- No major architectural changes: skill topology stays 3-tier; Spec Trio schema unchanged; allowlist schema unchanged.
- No business logic shipped in recipes — recipes ship composition contracts only.
- No new `templates/L4/<recipe-name>/` directory — recipes are NOT L4 domains.
- No P11+ recipes absorbed this cycle.
- No raw `npm` / `gradle` exposure to AI agents as user-facing surface — skill-orchestrated only.
- No NEW L2 blocks introduced by recipes this cycle (the 3 chosen recipes use only existing L2 blocks; `pipeline-kanban`, `approval-chain` deferred to future PRD if recipes 4–10 need them).
- No deviation from existing L4 Spec Trios driven by a recipe.

---

## §4 Recipe Inventory (3 patterns — trimmed from 10 per Synthesis-A)

> Each recipe is documented with: business context · enabled L4 · key L2 blocks (existing only) · business invariants (with `spec_ref:`/`rule_ref:` binding) · business observability (advisory) · evidence citations (structurally true).

### Recipe 1 — `saas-subscription` (SP35, sealed verdict in SP38)

**Business context:** Multi-tenant B2B SaaS with plan tiers, usage metering, recurring invoicing. Korean reference: 토스페이먼츠 정기결제 (Toss Payments recurring billing).

**enabled_l4_domains:** `billing`, `auth`, `feature-flags`, `notification`, `audit-log`.

**l2_blocks_used (existing only):** `pricing-table.tsx`, `plan-comparison.tsx`, `usage-meter.tsx`, `invoice-list.tsx`, `billing-history.tsx`, `feature-flag-toggle.tsx`, `feature-gate.tsx`, `kpi-card.tsx`.

**l3_pages_used:** `pricing-page`, `settings-overview`, `admin-overview-page`.

**business_invariants (each with binding):**
- `id: SAAS-INV-001` — "subscription must have ≥1 active plan" — `spec_ref: specs/billing-l0.yaml#BILLING-AUTHZ-002` (subscription scoping) + `rule_ref: practices/rules/billing-event-idempotent.md` (state transitions emit idempotent events)
- `id: SAAS-INV-002` — "usage metering resets on billing cycle boundary" — `rule_ref: practices/rules/billing-event-idempotent.md`
- `id: SAAS-INV-003` — "feature-gate enforcement matches plan tier" — `spec_ref: specs/feature-flags-l0.yaml` (feature-flag evaluation contract)
- `id: SAAS-INV-004` — "tenant downgrade preserves historical usage logs" — `spec_ref: specs/audit-log-l0.yaml` (immutable append-only audit)

**business_observability_advisory (not enforced this cycle):**
- `saas.mrr.calculated_daily_count` (Counter)
- `saas.tenant.active_subscription_count` (Gauge)
- `saas.usage_metering.cycle_reset_total` (Counter)
- `saas.plan.upgrade_total{from,to}` (Counter)

**evidence (structurally true with URLs + citations):**
```yaml
- provenance_class: external
  source: "Stripe Billing — Subscriptions overview"
  url: "https://stripe.com/docs/billing/subscriptions/overview"
  citation: "Subscriptions allow you to charge a customer on a recurring basis."
  quoted_at: 2026-05-18
- provenance_class: external
  source: "Toss Payments — 자동결제 (Recurring) Guide"
  url: "https://docs.tosspayments.com/guides/v2/billing/overview"
  citation: "자동결제(빌링)는 고객의 카드 정보를 안전하게 저장하여 정기적으로 결제하는 기능입니다."
  quoted_at: 2026-05-18
- provenance_class: internal_design
  derives_from: ["SP30 billing", "SP28 feature-flags"]
  rationale: "Composition selected from existing L4 baseline; no novel logic."
```

### Recipe 2 — `e-commerce` (SP35, sealed verdict in SP38)

**Business context:** Product catalog + cart + checkout + order management + inventory. Korean reference: 쿠팡 (Coupang) marketplace seller-side, 토스페이먼츠 결제.

**enabled_l4_domains:** `crud` (product), `payment`, `notification`, `audit-log`, `search`.

**l2_blocks_used (existing only):** `crud-list-adapter.tsx`, `crud-create-form.tsx`, `data-table.tsx`, `filter-bar.tsx`, `faceted-filter.tsx`, `kpi-card.tsx`, `event-stream.tsx`.

**l3_pages_used:** `list-page` (products), `detail-page` (product/order), `create-page`, `edit-page`, `search-results-page`.

**business_invariants (each with binding):**
- `id: ECOM-INV-001` — "order.total_amount == sum(items.unit_price × items.quantity)" — `rule_ref: practices/rules/idempotency-key-on-mutations.md` (mutation correctness) + `spec_ref: specs/payment-l0.yaml` (payment amount contract)
- `id: ECOM-INV-002` — "payment.captured ⇒ order.confirmed (atomic)" — `spec_ref: specs/payment-l0.yaml` (capture lifecycle) + `rule_ref: practices/rules/api-idempotency-key-required.md`
- `id: ECOM-INV-003` — "all mutating endpoints require idempotency key" — `rule_ref: practices/rules/api-idempotency-key-required.md`
- `id: ECOM-INV-004` — "cancellation/refund actions logged immutably" — `spec_ref: specs/audit-log-l0.yaml`

**business_observability_advisory (not enforced this cycle):**
- `ecommerce.order.placed_total{channel}` (Counter)
- `ecommerce.checkout.duration_p99` (Histogram)
- `ecommerce.payment.capture_total` (Counter)

**evidence (structurally true with URLs + citations):**
```yaml
- provenance_class: external
  source: "Shopify Admin REST API — Order resource"
  url: "https://shopify.dev/docs/api/admin-rest/2024-04/resources/order"
  citation: "An order is a customer's completed request to purchase one or more products from a shop."
  quoted_at: 2026-05-18
- provenance_class: external
  source: "Toss Payments — 결제 흐름(Payment Flow)"
  url: "https://docs.tosspayments.com/guides/v2/payment-widget/integration"
  citation: "결제 승인은 결제 요청과 별도의 단계이며, 승인이 완료되어야 실제 결제가 처리됩니다."
  quoted_at: 2026-05-18
- provenance_class: external
  source: "Coupang Partners — 셀러 API 개요"
  url: "https://developers.coupangcorp.com/hc/ko"
  citation: "쿠팡 셀러는 상품, 주문, 정산 API를 통해 시스템과 연동합니다."
  quoted_at: 2026-05-18
- provenance_class: internal_design
  derives_from: ["SP15 crud", "SP30 payment", "SP26 search", "SP17 audit-log"]
  rationale: "Composition selected from existing L4 baseline; cart/checkout flows derive from payment Spec Trio."
```

### Recipe 3 — `crm` (SP35, sealed verdict in SP38)

**Business context:** Sales pipeline: lead → contact → deal → activity. Korean reference: 채널톡 (Channel Talk) CRM, 카카오싱크.

**enabled_l4_domains:** `crud` (lead/contact/deal), `audit-log`, `notification`, `search`.

**l2_blocks_used (existing only):** `data-table.tsx`, `activity-feed.tsx`, `filter-bar.tsx`, `saved-view.tsx`, `kpi-card.tsx`, `event-stream.tsx`.

> **Note:** iter 1 listed `pipeline-kanban.tsx` as a NEW L2 here. **REMOVED in iter 2** — the CRM recipe uses existing L2 only. Kanban view is deferred to a future L2 SP if/when fork-receiver demand arrives.

**l3_pages_used:** `list-page`, `detail-page`, `create-page`, `dashboard-page`.

**business_invariants (each with binding):**
- `id: CRM-INV-001` — "deal.amount must be currency-precision compliant" — `rule_ref: practices/rules/idempotency-key-on-mutations.md` + `spec_ref: specs/crud-l0.yaml` (CRUD field validation)
- `id: CRM-INV-002` — "lead → contact conversion preserves source_attribution" — `spec_ref: specs/audit-log-l0.yaml` (provenance trail)
- `id: CRM-INV-003` — "activity.timestamp ≥ activity.created_at" — `spec_ref: specs/crud-l0.yaml` (CRUD-VALIDATION timestamp ordering)
- `id: CRM-INV-004` — "deal.stage transitions follow declared state machine; transitions logged" — `spec_ref: specs/audit-log-l0.yaml`

**business_observability_advisory (not enforced this cycle):**
- `crm.lead.captured_total{source}` (Counter)
- `crm.deal.stage_transitioned_total{from,to}` (Counter)
- `crm.deal.win_rate` (Gauge)
- `crm.activity.completed_total{type}` (Counter)

**evidence (structurally true with URLs + citations):**
```yaml
- provenance_class: external
  source: "Salesforce Object Reference — Opportunity (Deal) data model"
  url: "https://developer.salesforce.com/docs/atlas.en-us.object_reference.meta/object_reference/sforce_api_objects_opportunity.htm"
  citation: "Represents an opportunity, which is a sale or pending deal."
  quoted_at: 2026-05-18
- provenance_class: external
  source: "HubSpot CRM API — Deals overview"
  url: "https://developers.hubspot.com/docs/api/crm/deals"
  citation: "Deals represent ongoing transactions that sales teams are pursuing."
  quoted_at: 2026-05-18
- provenance_class: external
  source: "Channel Talk — 비즈니스 메신저 CRM 기능 소개"
  url: "https://channel.io/ko"
  citation: "고객 데이터, 영업 단계, 활동 이력을 한 곳에서 관리합니다."
  quoted_at: 2026-05-18
- provenance_class: internal_design
  derives_from: ["SP15 crud", "SP26 search", "SP17 audit-log"]
  rationale: "Composition selected from existing L4 baseline; pipeline state machine ships as code-of-customer, not as catalog primitive."
```

### Deferred recipes (4–10) — documented as backlog in `recipes/README.md`

| Pattern | Why deferred | Re-introduction trigger |
|---|---|---|
| `booking` | Korean URL evidence for 야놀자/캐치테이블 thin; needs Connectivity API access for citation | Fork-receiver demand OR public Booking.com Connectivity case study URL |
| `community` | Discourse + Reddit citations OK; Korean refs (디시인사이드, 클리앙) lack structured API URLs | Fork-receiver demand with Korean community platform requirement |
| `marketplace` | Etsy URL OK; 당근마켓 has no public API docs URL → would need `internal_design` fallback | Fork-receiver demand |
| `lms` | Moodle URL OK; 인프런 has no public API docs URL → would need `internal_design` fallback | Fork-receiver demand |
| `b2b-admin` | Iter 1 listed `time-series-chart` as new — corrected (already exists). Korean refs to 토스 비즈니스 lack structured URLs | Fork-receiver demand |
| `cms` | Sanity + Contentful URLs OK; rich-text already shipped in SP32 | Fork-receiver demand or batch with `community` |
| `internal-it` | Jira + ServiceNow URLs OK; Korean refs to 잔디/카카오워크 lack structured URLs | Fork-receiver demand |

All seven listed as `status: deferred-pending-fork-receiver-demand` in `recipes/README.md` to make the gap visible and addressable.

---

## §5 New Artifact Paths & SP Plan

### §5.1 Directory structure (NEW)

```
recipes/
├── README.md                              # recipe family index + 7 deferred rows
├── _MANIFEST.yaml                         # machine-readable list of 3 recipes + version + compatible_with_catalog_version
├── saas-subscription/
│   ├── RECIPE.md                          # incl. inline `override_allowed:` frontmatter (NO RECIPE_DEVIATION.md)
│   ├── spec-trio-template.yaml            # pre-filled Spec Trio fragments to copy into project
│   ├── L4-composition.md                  # which L4 to enable + wiring
│   └── L2-block-recipe.md                 # L2 block selection + composition order
├── e-commerce/
│   └── ...
└── crm/
    └── ...

specs/recipes/
├── saas-subscription-recipe-l0.yaml       # recipe-level spec (composition contract)
├── e-commerce-recipe-l0.yaml
└── crm-recipe-l0.yaml

practices/evals/
├── recipe_spec_referential_integrity_guard.sh   # NEW (SP35)
└── recipe_governance_guard.sh                    # NEW (SP37)

practices/evals/fixtures/
├── prefer-recipe-composition-over-l4-cross-import/
│   ├── positive/   # should FAIL the rule (violation)
│   └── negative/   # should PASS the rule (compliant)
└── business-domain-must-declare-applied-recipe/
    ├── positive/
    └── negative/

practices/rules/
├── prefer-recipe-composition-over-l4-cross-import.md   # NEW Java half (SP37)
└── business-domain-must-declare-applied-recipe.md       # NEW Java half (SP37)

practices-react/rules/
├── prefer-recipe-composition-over-l4-cross-import.md   # NEW React half (SP37)
└── business-domain-must-declare-applied-recipe.md       # NEW React half (SP37)

skills/ax-scaffold/                                      # EXTEND (SP36)
├── SKILL.md                                            # description widened: L4 OR composition
└── scripts/
    └── new-business-recipe.sh                          # NEW: /ax-scaffold business <pattern> <project>

skills/_tests/sealed-verdict/                            # NEW (SP38) — sealed-sub-agent harness
├── saas-subscription-verdict.md
├── e-commerce-verdict.md
└── crm-verdict.md
```

### §5.2 Recipe-level spec schema (NEW spec family)

`specs/recipes/<pattern>-recipe-l0.yaml`:

```yaml
schema_version: 1
pattern: saas-subscription
display_name: "Multi-tenant SaaS Subscription"
compatible_with_catalog_version: "v1.2.0-p1-absorbed"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - billing
  - auth
  - feature-flags
  - notification
  - audit-log
l2_blocks_used:
  - pricing-table
  - plan-comparison
  - usage-meter
  - invoice-list
  - billing-history
  - feature-flag-toggle
  - feature-gate
  - kpi-card
l3_pages_used:
  - pricing-page
  - settings-overview
  - admin-overview-page
business_invariants:
  - id: SAAS-INV-001
    statement: "subscription must have ≥1 active plan"
    spec_ref: "specs/billing-l0.yaml#BILLING-AUTHZ-002"
    rule_ref: "practices/rules/billing-event-idempotent.md"
  - id: SAAS-INV-002
    statement: "usage metering resets on billing cycle boundary"
    rule_ref: "practices/rules/billing-event-idempotent.md"
  # ... every row MUST have at least one of spec_ref OR rule_ref
business_observability_advisory:
  - metric: saas.mrr.calculated_daily_count
    type: counter
    note: "advisory; no emitter test enforced this PRD"
override_allowed:
  # Inline override block per Critic blocker 7 — no separate RECIPE_DEVIATION.md
  # Format: <field-path>: <rationale + url-or-citation>
  # Example (uncomment in fork-receiver's project if needed):
  # enabled_l4_domains:
  #   skip: ["audit-log"]
  #   rationale: "Project X uses external SIEM via webhook; audit-log L4 redundant."
  #   citation: "<internal ticket / PR url>"
evidence:
  - provenance_class: external
    source: "Stripe Billing — Subscriptions overview"
    url: "https://stripe.com/docs/billing/subscriptions/overview"
    citation: "Subscriptions allow you to charge a customer on a recurring basis."
    quoted_at: 2026-05-18
  # ... rest as in §4 Recipe 1
```

### §5.3 New L2 blocks introduced by recipes

**None this cycle.** All 3 chosen recipes (`saas-subscription`, `e-commerce`, `crm`) use only existing L2 blocks. The iter 1 list (`pipeline-kanban`, `time-series-chart`, `approval-chain`) is deferred:

- `time-series-chart.tsx` — **ALREADY EXISTS** at `templates/L2/blocks/time-series-chart.tsx`. Cited as existing dependency in any future b2b-admin recipe. (iter 1 disk-state error corrected.)
- `pipeline-kanban.tsx` — deferred to future PRD (only needed by deferred CRM kanban view + community moderation view).
- `approval-chain.tsx` — deferred to future PRD (only needed by deferred internal-it recipe).

### §5.4 SP plan (SP35–SP38) with TDD anchor table

| SP | Title | Deliverables | Acceptance (binary) |
|---|---|---|---|
| **SP35** | Recipes infra + 3 verdict-anchored recipes | `recipes/_MANIFEST.yaml`, `recipes/README.md` (incl. 7 deferred rows), 3 recipe directories (saas-subscription/e-commerce/crm) each with 4 artifacts (RECIPE.md + spec-trio-template.yaml + L4-composition.md + L2-block-recipe.md), 3 recipe-level specs in `specs/recipes/`, `practices/evals/recipe_spec_referential_integrity_guard.sh` (validates `enabled_l4_domains:` resolves AND every `business_invariants[*].spec_ref:`/`rule_ref:` resolves) | exit 0 from new referential-integrity guard + 3 recipe-level specs parse + every invariant binds |
| **SP36** | Skill subcommand (deterministic, no inference) | `skills/ax-scaffold/scripts/new-business-recipe.sh` (subcommand entry; runs `/ax-verify-domain <each-L4>` in loop after scaffold), `skills/ax-scaffold/SKILL.md` description update (widens scope to L4 OR composition), `METHODOLOGY.md` Appendix C update with composition flow | `bash skills/ax-scaffold/scripts/new-business-recipe.sh saas-subscription test-saas --dry-run` exits 0; outputs file tree; runs verify-domain for each enabled L4 with exit 0 |
| **SP37** | 2 enforcement rules + governance guard | 2 rule MDs × Java + React halves (4 files) with `protects_template_id` + `failing_fixture_path`, 2 fixture directories (positive/ + negative/ subdirs), `practices/evals/recipe_governance_guard.sh` | guard exit 0 on green tree; non-zero on injected positive fixtures; exit 0 on negative fixtures |
| **SP38** | Sealed verdict for ALL 3 recipes + release | Sealed verdict harness execution for saas-subscription/e-commerce/crm (each ≥10/12 MUST + ≥5/8 SHOULD), `/ax-verify` all green, tag `v1.3.0-business-patterns`, PR to main | All 3 sealed verdicts pass thresholds + tag created + PR opened + all guards GREEN |

### §5.5 TDD Anchor Table (NEW per Critic blocker 5 + Architect req fix 3)

Format per row: **test_file** | **assertion** | **expected_RED_reason** | **first_green_command** | **owning_SP**

| # | test_file | assertion | expected_RED_reason | first_green_command | owning_SP |
|---|---|---|---|---|---|
| T1 | `practices/evals/recipe_spec_referential_integrity_guard.sh` (self-test mode) | All `enabled_l4_domains:` in `specs/recipes/*.yaml` resolve to existing `templates/L4/<domain>/` directories; all `business_invariants[*].spec_ref:`/`rule_ref:` resolve to existing spec ID or rule file | Guard script does not exist yet | `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exits 0 | SP35 |
| T2 | `specs/recipes/saas-subscription-recipe-l0.yaml` | YAML parses; required fields present (`pattern`, `enabled_l4_domains`, `business_invariants` with `spec_ref` or `rule_ref` each, `evidence` with external URL+citation) | Spec file does not exist yet | `yq eval '.pattern' specs/recipes/saas-subscription-recipe-l0.yaml` returns `saas-subscription` | SP35 |
| T3 | `specs/recipes/e-commerce-recipe-l0.yaml` | Same schema as T2 for e-commerce | Spec file does not exist yet | `yq eval '.pattern' specs/recipes/e-commerce-recipe-l0.yaml` returns `e-commerce` | SP35 |
| T4 | `specs/recipes/crm-recipe-l0.yaml` | Same schema as T2 for crm | Spec file does not exist yet | `yq eval '.pattern' specs/recipes/crm-recipe-l0.yaml` returns `crm` | SP35 |
| T5 | `recipes/saas-subscription/RECIPE.md` | Frontmatter parses; `applied_recipe` field present; inline `override_allowed:` block present (may be empty); no `RECIPE_DEVIATION.md` referenced | RECIPE.md does not exist yet | `grep -q "^pattern: saas-subscription" recipes/saas-subscription/RECIPE.md` returns 0 | SP35 |
| T6 | `skills/ax-scaffold/scripts/new-business-recipe.sh` (dry-run mode) | Given `<pattern> <project-name>`: creates correct file tree skeleton; runs `/ax-verify-domain <each-enabled-L4>` and aggregates exit code | Script does not exist yet | `bash skills/ax-scaffold/scripts/new-business-recipe.sh saas-subscription test-saas --dry-run` exits 0 | SP36 |
| T7 | `skills/ax-scaffold/SKILL.md` | Description widens to mention "business composition" alongside "L4 domain skeleton" | Description still scoped to L4 only | `grep -E "(business composition\|composition recipe)" skills/ax-scaffold/SKILL.md` returns ≥1 | SP36 |
| T8 | `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/positive/Bad.java` (or .tsx) | Fixture demonstrates ad-hoc L4 cross-import (no `applied_recipe` declared) → rule MUST fire | Fixture does not exist yet | `bash practices/evals/recipe_governance_guard.sh practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/positive/` exits NON-ZERO | SP37 |
| T9 | `practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/negative/Good.java` (or .tsx) | Fixture follows recipe composition with declared `applied_recipe: saas-subscription` → rule MUST pass | Fixture does not exist yet | `bash practices/evals/recipe_governance_guard.sh practices/evals/fixtures/prefer-recipe-composition-over-l4-cross-import/negative/` exits 0 | SP37 |
| T10 | `practices/evals/fixtures/business-domain-must-declare-applied-recipe/positive/missing-applied-recipe-README.md` | L4 README lacks `applied_recipe:` annotation under a recipe-driven wiring → rule MUST fire | Fixture does not exist yet | `bash practices/evals/recipe_governance_guard.sh practices/evals/fixtures/business-domain-must-declare-applied-recipe/positive/` exits NON-ZERO | SP37 |
| T11 | `practices/evals/fixtures/business-domain-must-declare-applied-recipe/negative/README.md` | L4 README declares `applied_recipe: saas-subscription` → rule passes | Fixture does not exist yet | `bash practices/evals/recipe_governance_guard.sh practices/evals/fixtures/business-domain-must-declare-applied-recipe/negative/` exits 0 | SP37 |
| T12 | `skills/_tests/sealed-verdict/saas-subscription-verdict.md` | Sealed sub-agent (context-0; given only RECIPE.md + AGENTS.md) reproduces L4 list with ≥10/12 MUST + ≥5/8 SHOULD criteria | Sealed verdict harness has not been run yet | Run sealed sub-agent; capture verdict score; exit 0 if ≥10/12 MUST + ≥5/8 SHOULD | SP38 |
| T13 | `skills/_tests/sealed-verdict/e-commerce-verdict.md` | Same as T12 for e-commerce | Harness has not been run | Run; verify thresholds | SP38 |
| T14 | `skills/_tests/sealed-verdict/crm-verdict.md` | Same as T12 for crm | Harness has not been run | Run; verify thresholds | SP38 |

### §5.6 Verification Matrix (with observability_signal)

| SP | Surface | Verification Skill | Observability Signal (emitted as JSON line to stdout by each guard) |
|---|---|---|---|
| SP35 | 3 recipe dirs + specs/recipes/ + guard | `practices/evals/recipe_spec_referential_integrity_guard.sh` | `{"signal":"recipe.spec.referential_integrity_pass_count","value":<int>,"ts":"<iso8601>"}` |
| SP36 | subcommand + skill description + methodology update | manual: `bash new-business-recipe.sh saas-subscription test-saas --dry-run` + grep SKILL.md | `{"signal":"recipe.scaffold.dry_run_pass","pattern":"<name>"}` |
| SP37 | 2 enforcement rules + governance guard | `recipe_governance_guard.sh` + per-rule positive/negative fixture exec | `{"signal":"recipe.governance.violation_total","rule":"<id>","fixture":"<positive|negative>","fired":<bool>}` |
| SP38 | sealed verdict for all 3 recipes + release | sealed-agent runner over each recipe | `{"signal":"recipe.sealed_verdict.must_pass_count","recipe":"<name>","must":<int>,"should":<int>}` |

> **Emission contract:** every guard emits one JSON line per check to stdout (preserves shell binary pass/fail via exit code). Observability counters are diagnostic, not telemetry — they are scraped from CI logs if needed. Open Question on Prometheus/etc emission **deferred** to a future observability PRD.

### §5.7 Skill topology table (post-SP38)

| Tier | Skill | Status |
|---|---|---|
| **Tier-1 (cap=4)** | `/ax-transform` | unchanged |
| **Tier-1** | `/ax-scaffold` | EXTENDED with `business` subcommand (SP36); SKILL.md scope widened |
| **Tier-1** | `/ax-verify` | unchanged |
| **Tier-1** | `/ax-fork-receiver` | unchanged |
| Tier-2 (existing 8) | `/ax-verify-{L1,L2,L3,L4,java,react,shared,domain}` | unchanged |
| **Tier-2 (no new this cycle)** | (none) | `/ax-verify-recipe` DROPPED per Critic blocker 4 |
| Tier-3 (existing) | `/ax-guard-*` | unchanged |

**Tier-1 count after SP38: 4 (unchanged). Tier-2 count after SP38: 8 (unchanged).** Constraints honored.

---

## §6 Autonomous Execution Safety

- Each SP commits in ONE atomic cluster (consistent with SP26/SP30 precedent).
- SP35 ships the directory infra + all 3 recipes + guard in the first commit cluster (single SP, multiple commits, atomic rollback unit).
- SP36 is small (single subcommand script + SKILL.md scope edit + METHODOLOGY.md prose).
- SP37 commits 4 rule files (2 rules × Java/React) + 2 guard + 4 fixture dirs in one cluster.
- SP38 sealed verdict: if any of 3 recipes scores <10/12 MUST OR <5/8 SHOULD, halt; document in DECISIONS.md; do NOT lower thresholds. Either re-tune the recipe RECIPE.md content (it may be ambiguous) OR fix the catalog AGENTS.md sentinel reference.
- All SP work happens on branch `feat/business-patterns-sp35-sp38`. No direct main commits.
- All SP commits use `superpowers:writing-skills` conventions.
- Race-safe append protocol for `recipes/_MANIFEST.yaml`: since all 3 recipes land in SP35's atomic cluster, no cross-SP race; future PRDs that add recipes 4+ must rebase against HEAD and use `yq`-sorted insertion (documented in `recipes/README.md` maintenance section).
- **No `--analyze` recovery path needed** (feature dropped).
- **No 30-day WARN→HARD soft-launch** (rules ship HARD from day one).

---

## §7 Pre-Mortem (≥3 scenarios) — DELIBERATE mode

### Scenario 1 — Recipe drift from L4 evolution (drift risk)

**Failure mode:** SP35 ships `saas-subscription` referencing `templates/L4/billing/` shape as of v1.2.0. Future PRD-N enhances `templates/L4/billing/` (adds new L2 dependency, changes Spec Trio operationIds). `recipes/saas-subscription/L4-composition.md` becomes stale.

**Detection:** `practices/evals/recipe_spec_referential_integrity_guard.sh` runs on EVERY commit, validating that `enabled_l4_domains:` resolves AND `l2_blocks_used:` resolves to existing files AND `business_invariants[*].spec_ref:`/`rule_ref:` resolves to existing artifact IDs.

**Owner:** maintainer; **command:** the guard itself; **threshold:** any unresolved ref → guard exit non-zero blocks commit; **recovery:** future PRD that touches L4 MUST include "recipe-impact-analysis" section if any recipe references that L4.

**Residual risk:** semantic drift (L4 changes meaning, not shape) — guard cannot catch. Mitigation: SP38 sealed verdict in CI on schedule (frequency: per future PRD touching ANY referenced L4 — NOT a calendar schedule, NOT a 30-day clock; event-driven).

### Scenario 2 — Sealed verdict regression after L4 evolution (verdict regression risk)

**Failure mode:** SP38 verdict passes at v1.3.0. A subsequent PRD evolves billing L4. AGENTS.md updates. A re-run of the saas-subscription sealed verdict drops to <10/12 MUST because the sub-agent's catalog mental model has shifted.

**Detection:** when ANY future PRD modifies a recipe's enabled L4, that PRD's plan section MUST include "sealed-verdict-re-run" of the affected recipe(s) as an SP deliverable.

**Owner:** PRD author; **command:** `bash skills/_tests/sealed-verdict/<recipe>-verdict.md` runner script (SP38 ships this script); **threshold:** ≥10/12 MUST + ≥5/8 SHOULD (same as initial); **recovery:** if verdict drops, either revert the catalog change OR amend the recipe RECIPE.md atomic with the catalog change (then re-verdict).

**Residual risk:** verdict sub-agent prompt itself drifts (LLM version changes). Mitigation: SP38 verdict harness pins the sub-agent's instruction set and records LLM version in the verdict output.

### Scenario 3 — Enforcement rule false-positive (process risk)

**Failure mode:** `prefer-recipe-composition-over-l4-cross-import` flags a legitimate cross-import in a fork-receiver project that intentionally diverges from the recipe (e.g., uses external SIEM instead of `audit-log` L4).

**Detection:** Each rule fixture has both `positive/` (should fail) and `negative/` (should pass) cases. SP37 acceptance: rule fires on positive ONLY.

**Owner:** maintainer; **command:** `recipe_governance_guard.sh` per fixture; **threshold:** any positive fixture not firing OR any negative fixture firing → SP37 cannot land.

**Mitigation:**
- `override_allowed:` INLINE block in `recipes/<pattern>/RECIPE.md` frontmatter (NO separate `RECIPE_DEVIATION.md`, NO 30-day clock). Rule explicitly checks for the override field in fork-receiver's recipe-applied L4 README and skips if present + valid.
- Single binary check: comply OR cite inline override.

**Residual risk:** override mechanism abused (every fork-receiver overrides everything). Mitigation: NOT this cycle's concern — `override_allowed:` is fork-receiver's own policy decision (per CLAUDE.md anti-pattern §: "fork받은 팀의 정책을 skill이 강제 ❌"). The catalog only provides the mechanism, not the enforcement; fork-receiver enforces or relaxes via their own CI.

---

## §8 ADR Template (TD-2026-05-19-NNN)

```markdown
## TD-2026-05-19-001 — Business Pattern Recipes as verdict-anchored composition layer (3 recipes, not 10)

**Decision:** Ship Business Pattern Recipes as `recipes/<pattern>/` directories composing existing L4 domains, starting with **3 verdict-anchored recipes** (`saas-subscription`, `e-commerce`, `crm`). Each recipe carries a sealed sub-agent verdict at SP38 (100% coverage). Patterns 4–10 are deferred to future PRDs pending fork-receiver demand. Recipe scaffolding ships as `/ax-scaffold business <pattern>` deterministic subcommand (NO free-text inference). No new Tier-2 skill (`/ax-verify-recipe` rejected; behavior implemented inline via existing `/ax-verify-domain` loop).

**Drivers:**
1. **Verdict-anchored standard parity.** Catalog's existing standard: every shipped L4 has Spec Trio + tests + sealed verdict. 10 recipes with only 3 verdicts regresses below standard. 3 recipes with 100% verdict coverage matches it.
2. **Composition kit framing.** Recipes COMPOSE existing L4; they do NOT define new L4. Tier-1 cap = 4 preserved.
3. **Anti-speculation.** Iter 1's free-text inference (`--analyze`) introduced an NLP surface with 80% accuracy gate and proposed 70% downgrade — worse-than-baseline UX without proven fork-receiver demand. Dropped.
4. **Anti-governance.** Iter 1's `RECIPE_DEVIATION.md` + 30-day WARN→HARD + 50% deviation re-litigation matched the CLAUDE.md "evidence bundle / governance loop" anti-pattern. Replaced with inline `override_allowed:` metadata.

**Alternatives considered:**
- 10 recipes with mixed verdict coverage (Option A, iter 1): rejected — regresses verdict-anchored standard.
- 5 recipes with full verdict coverage (Option B): rejected — marginal pattern gain doesn't justify 5 SPs vs 4.
- Free-text inference (Option E): rejected — speculative without fork-receiver prompt logs.
- New Tier-1 `/ax-blueprint` (Option D): rejected — breaks cap = 4.
- Recipes-only docs (Option C): rejected — no machine enforcement.

**Why chosen:** Composition layer aligned with user mandate ("AI judges business logic; system enforces catalog conformance"). Preserves all existing invariants (Tier-1 cap, Spec Trio atomicity, evidence-anchoring, no governance loops).

**Consequences:**
- New `recipes/` top-level directory + `specs/recipes/` sub-directory.
- New guard `recipe_spec_referential_integrity_guard.sh` (SP35).
- New guard `recipe_governance_guard.sh` (SP37).
- 2 new enforcement rules × Java + React halves = 4 rule files (SP37).
- `skills/ax-scaffold/SKILL.md` scope widened to "L4 OR composition" (SP36).
- `METHODOLOGY.md` Appendix C extended with composition flow (SP36).
- 7 deferred patterns visible as backlog rows in `recipes/README.md`.

**Follow-ups:**
- Recipes 4–10 ship in future PRDs pending fork-receiver demand (Korean URL evidence often the bottleneck).
- Free-text inference re-considered only when real fork-receiver prompt logs justify an NLP surface.
- Observability emission contract (Prometheus, etc.) deferred to a future observability PRD.
- `business_observability_advisory` rows are advisory this PRD; future PRD binds via emitter test fixture.
```

---

## §9 Open Questions

(also persisted to `.omc/plans/open-questions.md` per Open_Questions protocol)

1. **Sealed verdict re-run cadence** — Scenario 2 says "event-driven, not calendar". Concretely: should EVERY future PRD touching billing/auth/feature-flags/notification/audit-log/crud/payment/search/audit-log/file-storage automatically re-run all affected recipes' sealed verdicts as an SP deliverable? Or only PRDs that pass a threshold (e.g., new operationId, new L2 dep)? Documented as policy in SP38 closing notes pending Critic feedback.
2. **Override mechanism abuse limit** — `override_allowed:` is fork-receiver's policy per CLAUDE.md anti-pattern §. But should the catalog's `recipe_governance_guard.sh` count overrides per recipe and emit a diagnostic warning when >N? (Diagnostic only, not blocking — preserves anti-governance stance.) Decision: NOT this PRD; future observability PRD may add the diagnostic counter.
3. **Recipe-level spec vs RECIPE.md duplication** — should the YAML be canonical (machine-truth) and RECIPE.md a generated view, or do they live as parallel sources with cross-check guard? Iter 2 decision: parallel sources; SP35 ships referential-integrity guard that includes a cross-field consistency check (e.g., `enabled_l4_domains:` in YAML matches RECIPE.md prose list). Specific cross-check spec deferred to SP35 implementation.

---

## §10 Honored Constraints

- ✅ Tier-1 cap = 4 (preserved; `/ax-scaffold business` is subcommand).
- ✅ Tier-2 cap = 8 (preserved; `/ax-verify-recipe` dropped — no new skill this cycle).
- ✅ Composition kit framing (recipes compose existing L4, no new L4 per recipe).
- ✅ Spec Trio atomic rule (recipe-level spec is NOT a domain Spec Trio).
- ✅ Evidence-anchored AND structurally true (every Korean reference carries `provenance_class: external` + URL + verbatim citation + `quoted_at`).
- ✅ Korean enterprise context structurally bound (Toss Payments + Coupang + Channel Talk — all with documented URLs).
- ✅ Out-of-scope for deployment/CI (no GH Actions, no release.yml).
- ✅ No new MockMvc tests (sealed verdict harness uses agent + RestAssured pattern).
- ✅ 4-iter ralplan APPROVE pattern (this is iter 2).
- ✅ AGENTS.md sentinel preservation (no changes to AGENTS.md schema; recipes referenced FROM AGENTS.md via new section "Composition recipes" in SP38 closing commit).
- ✅ Anti-governance (`RECIPE_DEVIATION.md` + 30-day clock + 50% re-litigation REMOVED).
- ✅ Anti-speculation (`--analyze` free-text inference + 50-fixture eval + accuracy guard REMOVED).
- ✅ Disk-state correctness (`time-series-chart.tsx` cited as existing block; not in any "new L2" claim).

---

## §11 Iter 2 Closure on 8 Critic Hard Blockers

| # | Critic iter 1 blocker | Iter 2 resolution | §reference |
|---|---|---|---|
| 1 | `time-series-chart.tsx` mis-classified as new (HIGH) | Removed from all "new L2" claims; cited as existing at `templates/L2/blocks/time-series-chart.tsx`. The 3 chosen recipes (saas/ecom/crm) use only existing L2; no `time-series-chart` reference at all this cycle. | §4 (no `time-series-chart` ref in saas/ecom/crm); §5.3 ("None this cycle"); §4 deferred table notes b2b-admin would cite existing block |
| 2 | Trim 10 recipes → 3 (Synthesis-A) | Recipe count = 3 (`saas-subscription`, `e-commerce`, `crm`). 7 patterns explicitly deferred with re-introduction triggers documented. | §1 Recommended; §4 Inventory + Deferred table; §5.4 (4 SPs vs 6) |
| 3 | Drop `--analyze` free-text inference | All `--analyze` references removed. `/ax-scaffold business <pattern> <project>` is deterministic only. No 50-fixture suite, no accuracy guard. | §3 Guardrails Must NOT; §5.4 SP36; §5.7 (no `--analyze`) |
| 4 | Drop `/ax-verify-recipe` new Tier-2 | All `/ax-verify-recipe` references removed. Recipe verification = `/ax-verify-domain <each-enabled-L4>` loop inline in subcommand + new referential-integrity guard. Tier-2 count stays 8. | §1 Principle 4; §3 Guardrails; §5.7 ("DROPPED per Critic blocker 4") |
| 5 | Missing TDD anchor table | New §5.5 TDD Anchor Table with 14 rows. Every SP has explicit test_file / assertion / RED reason / first green command / owning SP. | §5.5 |
| 6 | `business_invariants` bound to `spec_ref:` or `rule_ref:` | Every invariant in all 3 recipes carries `spec_ref:` (pointing to spec ID like `BILLING-AUTHZ-002`) OR `rule_ref:` (pointing to existing `practices/rules/*.md`) OR both. Referential-integrity guard validates resolution. | §4 (each recipe's invariants); §5.2 (schema); §5.4 SP35 acceptance ("every invariant binds") |
| 7 | Remove `RECIPE_DEVIATION.md` governance loop | `RECIPE_DEVIATION.md` file removed entirely. 30-day WARN→HARD removed. 50% re-litigation removed. Replaced with INLINE `override_allowed:` block in `recipes/<pattern>/RECIPE.md` frontmatter. | §3 Guardrails Must NOT; §5.2 (`override_allowed:` schema); §7 Scenario 3 |
| 8 | Korean references structurally true | Every Korean reference in §4 carries `provenance_class: external` + verified URL + `citation:` verbatim quote + `quoted_at: 2026-05-18`. Refs lacking structured URL (e.g., 야놀자, 당근마켓, 인프런, 잔디, 카카오워크) deferred via the 7-recipe deferred table — not used as evidence anchors for the 3 chosen recipes. | §4 Recipe 1 (Toss URL); Recipe 2 (Coupang Partners URL); Recipe 3 (Channel Talk URL); §4 deferred table (Korean URL gap explicitly cited as deferral trigger) |

---

## Final Checklist (Planner iter 2)

- [x] Recipes are composition manifests, not domain Spec Trios
- [x] Tier-1 count remains 4 after SP38
- [x] Tier-2 count remains 8 after SP38 (no new skill)
- [x] Every recipe carries evidence + structurally-true Korean reference (URL + citation + quoted_at)
- [x] 4 SPs total (SP35–SP38), trimmed from iter 1's 6
- [x] Sealed verdict harness present for ALL 3 recipes (SP38) — 100% coverage
- [x] Free-text inference dropped (no Open Question on accuracy threshold)
- [x] Enforcement rules have failing positive + negative fixtures (SP37)
- [x] Pre-mortem ≥3 scenarios with owner/command/threshold/recovery (§7)
- [x] ADR template (§8)
- [x] Open Questions (§9) — reduced from 3 substantive to 3 mostly-resolved
- [x] Honored Constraints (§10)
- [x] RALPLAN-DR summary at top with ≥2 viable options + rationale
- [x] §11 explicitly closes all 8 Critic iter 1 hard blockers
- [x] TDD Anchor Table added (§5.5) — 14 rows
- [x] Every `business_invariants` row binds to existing `spec_ref:` or `rule_ref:`
- [x] No `RECIPE_DEVIATION.md` (inline `override_allowed:` only)
- [x] No 30-day WARN→HARD clock
- [x] No new L2 blocks in this cycle (time-series-chart is EXISTING)
- [ ] Codex Critic iter 2 narrow re-review (next step)
