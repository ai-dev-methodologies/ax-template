# Business Pattern Recipes PRD — 2026-05-19 (Round 5, ralplan iter 1 draft)

> **Status:** DRAFT for `/ralplan` consensus loop Round 5 (Planner output). Awaits Architect + Critic review.
> **Date:** 2026-05-18 (PRD slug 2026-05-19 — planning horizon). **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-17-frontend-templatization-prd.md` (CLOSED — SP1–SP12, `v1.0.0` baseline).
> - `2026-05-18-catalog-extension-prd.md` (CLOSED — SP13–SP22, `v1.0.0-catalog-complete`, commit `9212989`).
> - `2026-05-18-functional-extension-prd.md` (CLOSED — SP23–SP29, `v1.1.0-functional-complete`, commit `1ab8f54`).
> - `2026-05-18-p1-absorption-prd.md` (CLOSED — SP30–SP34, `v1.2.0-p1-absorbed`, commit `26de945`).
> **Branch (when execution starts):** `feat/business-patterns-sp35-sp40` (6 SPs).
> **Targeted tag:** `v1.3.0-business-patterns`.

---

## §1 RALPLAN-DR Summary

### Principles (inherited verbatim, re-anchored to this PRD's surface)

1. **Composition kit, not single product.** Recipes COMPOSE existing L4 domains; they do NOT define a new L4. Every recipe atom is fork-adoptable in isolation; a fork-receiver can pick recipe `saas-subscription` without picking up `e-commerce`. (CLAUDE.md §Vision)
2. **Spec-before-code, evidence-anchored.** Every recipe carries `evidence:` block citing real-world references (Stripe Billing docs, Shopify e-commerce, Booking.com Connectivity API, Toss Payments, Coupang Marketplace pattern docs). Every new rule carries `protects_template_id` + `failing_fixture_path`. (Catalog Extension §1)
3. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` returns exit 0. Recipes verify via a NEW `/ax-verify-recipe <pattern>` Tier-2 axis (NOT a new Tier-1). (PRD-1 §RALPLAN-DR)
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count **stays frozen at 4** (`/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`). Recipe scaffolding ships as `/ax-scaffold business <pattern>` SUBCOMMAND of existing `/ax-scaffold`, NOT a new Tier-1 (precedent: SP29 made F13/14/15 subcommands of `/ax-verify`).
5. **Atomic Spec-Trio rule.** Recipes are NOT new Spec Trios — they are **composition manifests** referencing existing L4 Spec Trios. No new domain Spec Trio is created by a recipe. The `specs/recipes/<pattern>-recipe-l0.yaml` is a NEW spec-family (recipe-level), but it does NOT register in `trio_integrity_allowlist.yaml` (no backend/frontend OpenAPI; pure composition contract). Iter-strict reading: recipe-level specs land atomically with their RECIPE.md.
6. **No speculative generality.** Recipe inventory is closed at 10 patterns this PRD; each pattern is evidence-anchored to a real fork-receiver business shape (Korean SaaS / enterprise vertical). Patterns 11+ are explicit deferred items pending fork-receiver demand.
7. **Recipe does not ship code; AI implements business logic.** Per user mandate — "비지니스 로직 판단이야 AI가 잘 해줄거자나" — recipes specify WHICH catalog atoms to compose, NOT the business logic itself. Enforcement rules ensure AI uses prescribed atoms instead of inventing new ones.

### Decision Drivers (top 3)

1. **User mandate prioritization × catalog completeness.** The user's quoted mandate ("정해진 컴포넌트 구현이 어느정도 되었으면, 각 업무 비지니스 구현 방법에 대한 준비가 되어 있어야해") explicitly names this PRD's surface as the next deliverable post-v1.2.0. The catalog (50+ L1 / 92 L2 / 16 L3 / 10 L4 + identity-verification backend_only) is sufficient breadth — the gap is **composition guidance**, not more atoms.
2. **Recipe self-discoverability metric (extends payment-domain SP precedent).** The L4 sealed sub-agent verdict (11/11 MUST + 6/6 SHOULD) proved catalog is self-discoverable. The next surface — "given a business pattern, can a context-0 AI agent assemble the right L4 set?" — needs an analogous sealed verdict per recipe. SP40 ships a sealed verdict harness for 3 representative recipes (SaaS / e-commerce / CRM).
3. **Tier-1 cap preservation.** Tier-1 cap of 4 has been preserved through every cycle. Adding a NEW Tier-1 for recipe scaffolding (Option D below) breaks this invariant. Subcommand approach (Option B) honors the cap.

### Mode

**DELIBERATE.** Auto-triggered by: (a) wall-time ≥1 week (6 SPs); (b) new artifact family (`recipes/<pattern>/`); (c) 3 new enforcement rules with cross-recipe scope; (d) free-text inference command — non-trivial accuracy bar; (e) sealed verdict harness for 3 recipes. Pre-mortem (≥3 scenarios) + expanded test plan + observability_signal mandatory.

### Viable Options Considered (≥2 mandatory)

- **Option A — 10 recipes + `/ax-scaffold business <pattern>` subcommand + 3 enforcement rules.**
  - Pros: covers full pattern range identified in user mandate; subcommand keeps Tier-1 cap = 4; enforcement rules bind AI behavior; aligns with `/ax-verify` SP29 subcommand precedent.
  - Cons: 6 SPs (largest cycle); 10 patterns dilute focus on the 3 highest-value (SaaS/e-commerce/booking).
  - **CHOSEN.** Rationale: user mandate explicitly enumerates 10 patterns; deferring half creates re-litigation cycles. Volume mitigated by grouping (3+3+4 across SP35–SP37).

- **Option B — 5 recipes (highest-frequency only) + subcommand + 3 enforcement rules.**
  - Pros: tighter scope; 4 SPs; faster fork-receiver value delivery.
  - Cons: skips 5 explicit user-mandate patterns (LMS, B2B admin, CMS, internal IT, marketplace) that have clear Korean enterprise demand; creates "phase 2" backlog.
  - **Rejected.** User mandate names 10; deferring half without principled cut is arbitrary. The audit cost of choosing WHICH 5 exceeds the saved SP cost.

- **Option C — Recipes-only, no skill subcommand (catalog reference docs).**
  - Pros: minimum risk; pure documentation; no skill-layer changes.
  - Cons: no scaffolding automation — AI agent must manually assemble L4 list per pattern; fails user mandate ("AI가 잘 해줄거자나" presumes machine-verifiable enforcement, not prose).
  - **Rejected.** Without enforcement + scaffolding, recipes become advisory docs that AI ignores under prompt drift.

- **Option D — `/ax-blueprint` NEW Tier-1 skill + 10 recipes + 3 enforcement rules.**
  - Pros: dedicated top-level discoverability; clean composition-kit surface.
  - Cons: BREAKS Tier-1 cap = 4 (would become 5); violates Principle 4; violates SP29 precedent where F13/14/15 became `/ax-verify` subcommands NOT new Tier-1s; sets precedent for future cap-breaks.
  - **Rejected.** Explicit constraint violation.

- **Option E — Skill-only, no shipped recipes (LLM infers every time).**
  - Pros: zero recipe maintenance; flexible to novel patterns.
  - Cons: no shipped artifact for fork-receiver; LLM inference is non-deterministic — same business description yields different L4 compositions; violates evidence-anchoring (no `evidence:` block per recipe); violates Principle 6 (speculative generality).
  - **Rejected.** Composition-kit framing requires shipped, evidence-anchored artifacts.

- **Option F — Defer entire PRD pending fork-receiver evidence.**
  - Pros: maximum evidence quality.
  - Cons: user mandate explicitly requests this NOW; catalog is sufficiently complete to justify composition guidance; deferring blocks the obvious next progression (atoms → composition).
  - **Rejected.**

### Recommended: **Option A — 10 recipes + `/ax-scaffold business <pattern>` subcommand + 3 enforcement rules + sealed verdict harness for 3 representative recipes = 6 SPs (SP35–SP40).**

**SP count: 6.** Sequencing:

```
SP35 (recipes/ infrastructure + 3 high-frequency recipes: saas-subscription, e-commerce, booking)
    ↓
SP36 (3 mid-frequency recipes: crm, community, marketplace) ‖ SP37 (4 vertical recipes: lms, b2b-admin, cms, internal-it) — parallel after SP35 infra
    ↓
SP38 (/ax-scaffold business <pattern> subcommand + --analyze free-text inference + 50-fixture eval suite + /ax-verify-recipe Tier-2 skill)
    ↓
SP39 (3 enforcement rules: prefer-recipe-composition / declare-applied-recipe / deviation-justification + failing fixtures + practices/evals/recipe_governance_guard.sh)
    ↓
SP40 (FINAL: sealed verdict harness for 3 representative recipes + /ax-verify all + tag v1.3.0-business-patterns + PR to main)
```

SP36 + SP37 run in **parallel after SP35 infra**, then SP38 → SP39 → SP40 sequential. Total: **6 SPs, ≈ 10–12 d wall-time.**

---

## §2 Context

### Completed cycles (verified disk state, 2026-05-18)

| Cycle | PRD | Tag | Commit | Surfaces touched |
|---|---|---|---|---|
| 1 | `2026-05-17-frontend-templatization-prd.md` | `v1.0.0` | (pre-tag) | SP1–SP12: monorepo scaffolding, L1/L2/L3 baseline, 4 hard gates, /ax-* Tier-1 skills |
| 2 | `2026-05-18-catalog-extension-prd.md` | `v1.0.0-catalog-complete` | `9212989` | SP13–SP22 |
| 3 | `2026-05-18-functional-extension-prd.md` | `v1.1.0-functional-complete` | `1ab8f54` | SP23–SP29 |
| 4 | `2026-05-18-p1-absorption-prd.md` | `v1.2.0-p1-absorbed` | `26de945` | SP30–SP34: billing L4, identity-verification backend_only, Korean specials, forms/rich-content, tables/filters, admin polish |

### Current catalog totals (post-`v1.2.0-p1-absorbed`, disk-verified)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 42+ | `templates/L1/components/` |
| L2 blocks | 92 | `ls templates/L2/blocks/ \| wc -l` |
| L3 page templates | 20 (16 directories + 4 utility) | `ls templates/L3/pages/` |
| L4 domain workloads | 10 (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, search) | `ls templates/L4/` |
| Backend-only domains | 3 (email-outbox, scheduled-task, ratelimit) + 1 (identity-verification, SP31) | `practices/evals/trio_integrity_allowlist.yaml` |
| Skills | 19 (Tier-1: 4, Tier-2: 8, Tier-3: 7) | `ls skills/` |
| Java rules | 81 | `ls practices/rules/*.md \| wc -l` |
| React rules | 85 | `ls practices-react/rules/*.md \| wc -l` |
| Spec Trios | 13 | `specs/` cross-ref `contracts/` `blueprints/` |
| Guards | ≥19 GREEN | `practices/evals/*_guard.sh` |

### This PRD's scope

Admits **6 SPs covering 10 Business Pattern Recipes + 1 Tier-2 skill (`/ax-verify-recipe`) + 1 subcommand (`/ax-scaffold business`) + 3 enforcement rules + 1 sealed verdict harness**. Filter is enforced in §3 Guardrails and §4 Inventory.

---

## §3 Objectives + Guardrails

### Objectives (one per SP)

- **O1 (SP35 — Recipes Infrastructure + High-Frequency Recipes):** Ship `recipes/` directory infrastructure + 3 high-frequency recipes (`saas-subscription`, `e-commerce`, `booking`). Each recipe directory contains `RECIPE.md` + `spec-trio-template.yaml` + `L4-composition.md` + `L2-block-recipe.md` + `evidence:` block. NEW recipe-level Spec family: `specs/recipes/<pattern>-recipe-l0.yaml`.
- **O2 (SP36 — Mid-Frequency Recipes):** Ship 3 recipes (`crm`, `community`, `marketplace`). Same artifact set as SP35.
- **O3 (SP37 — Vertical Recipes):** Ship 4 recipes (`lms`, `b2b-admin`, `cms`, `internal-it`). Same artifact set.
- **O4 (SP38 — Skill Subcommand + Inference + Tier-2 Verifier):** Ship `/ax-scaffold business <pattern> <project-name>` subcommand (NOT new Tier-1). Ship `/ax-scaffold business --analyze "<free-text>"` inference variant. Ship NEW Tier-2 `/ax-verify-recipe <pattern>` skill (composes existing /ax-verify-domain calls per L4 in the recipe). Ship 50-fixture eval suite (5 fixtures per recipe × 10 = 50) validating `--analyze` matches expected pattern with ≥80% accuracy. Acceptance: `/ax-verify-recipe <each-pattern>` exit 0.
- **O5 (SP39 — Enforcement Rules):** Ship 3 enforcement rules:
  - `prefer-recipe-composition-over-l4-cross-import` — if implementing a business that matches a Pattern Recipe (detected via `applied_recipe` metadata), MUST follow Recipe L4 composition; cross-import L4 ad-hoc FAILS.
  - `business-domain-must-declare-applied-recipe` — every L4 domain wiring under a recipe must declare `applied_recipe: <pattern-name>` in README or Spec Trio metadata.
  - `recipe-deviation-requires-justification` — partial overrides need `templates/L4/<domain>/RECIPE_DEVIATION.md` with rationale (provenance_class + cited business constraint).
  Each rule has failing fixture under `practices/evals/fixtures/<rule-id>/`. NEW guard: `practices/evals/recipe_governance_guard.sh`.
- **O6 (SP40 — Sealed Verdict + Release):** Ship sealed verdict harness for 3 representative recipes (`saas-subscription`, `e-commerce`, `crm`). Each verdict runs an L4-sealed sub-agent that, given ONLY the recipe RECIPE.md + catalog AGENTS.md, must reproduce the recipe's L4 composition with ≥10/12 MUST + ≥5/8 SHOULD. Tag `v1.3.0-business-patterns`. PR to main.

### Guardrails — Must Have

- Every recipe carries `evidence:` block (provenance_class: `internal_design` for composition logic + ≥1 external citation: Stripe / Shopify / Booking.com / Toss / 쿠팡파트너스 etc.).
- Every recipe RECIPE.md declares `enabled_l4_domains:` list, `l2_blocks_used:` list, `business_invariants:` list, `business_observability:` list.
- Every L4 domain wired by a recipe carries `applied_recipe: <pattern-name>` annotation in its README.md or Spec Trio metadata (enforced by `business-domain-must-declare-applied-recipe`).
- Skill topology cap: **Tier-1 count frozen at 4.** Recipe scaffolding = `/ax-scaffold` subcommand. Recipe verification = NEW Tier-2 `/ax-verify-recipe`.
- Atomic Spec-Trio rule: recipe-level spec (`specs/recipes/<pattern>-recipe-l0.yaml`) lands ATOMICALLY with `recipes/<pattern>/RECIPE.md` in the same commit cluster.
- Recipe-level spec is NOT registered in `trio_integrity_allowlist.yaml` (no backend OpenAPI; pure composition contract). A separate guard validates recipe-level spec referential integrity (each `enabled_l4_domains:` entry MUST resolve to an existing `templates/L4/<domain>/`).
- 4-iter ralplan APPROVE pattern preserved.
- All recipes name their Korean SaaS / enterprise mapping in `evidence:` (e.g., saas-subscription → 토스페이먼츠 정기결제 + 카카오워크 multi-tenant; e-commerce → 쿠팡 / 네이버스마트스토어 reference).

### Guardrails — Must NOT

- No new Tier-1 skill (preserves cap = 4).
- No new L4 domain created BY a recipe (recipes compose existing L4 only).
- No GitHub Actions, LICENSE, CONTRIBUTING, docs-site, release.yml, Dependabot, GitHub Pages.
- No major architectural changes: skill topology stays 3-tier; Spec Trio schema unchanged; allowlist schema unchanged.
- No business logic shipped in recipes — recipes ship composition contracts only. AI implements business logic per user mandate.
- No new `templates/L4/<recipe-name>/` directory — recipes are NOT L4 domains.
- No P11+ recipes absorbed this cycle.
- No raw `npm` / `gradle` exposure to AI agents as user-facing surface — skill-orchestrated only.
- No deviation from existing L4 Spec Trios driven by a recipe — if a recipe needs an L4 change, that change ships in a SEPARATE PRD/SP (composition is downstream of catalog, never inverts it).

---

## §4 Recipe Inventory (10 patterns)

> Each recipe is documented with: business context · enabled L4 · key L2 blocks · business invariants · business observability · evidence citations.

### Recipe 1 — `saas-subscription` (SP35, high-frequency)

**Business context:** Multi-tenant B2B SaaS with plan tiers, usage metering, recurring invoicing. Common Korean stack: 카카오워크-style team SaaS, 잔디(Jandi)-style enterprise collab.

**enabled_l4_domains:** `billing`, `auth`, `feature-flags`, `notification`, `audit-log`.
**l2_blocks_used:** `pricing-table`, `plan-comparison`, `usage-meter`, `invoice-list`, `billing-history`, `feature-flag-toggle`, `feature-gate`, `kpi-card`.
**l3_pages_used:** `pricing-page`, `settings-overview`, `admin-overview-page`.
**business_invariants:**
- `subscription must have ≥1 active plan`
- `usage metering must reset on billing cycle boundary`
- `feature-gate enforcement matches plan tier`
- `tenant downgrade preserves historical usage logs`
**business_observability:**
- `saas.mrr.calculated_daily_count` (Counter)
- `saas.tenant.active_subscription_count` (Gauge)
- `saas.usage_metering.cycle_reset_total` (Counter)
- `saas.plan.upgrade_total{from,to}` (Counter)
**evidence:**
- Stripe Billing docs (https://stripe.com/docs/billing/subscriptions/overview)
- Toss Payments 정기결제 (https://docs.tosspayments.com/guides/payment/subscription)
- internal_design: composition derived from SP30 billing L4 + SP28 feature-flags L4

### Recipe 2 — `e-commerce` (SP35, high-frequency)

**Business context:** Product catalog + cart + checkout + order management + inventory. Korean reference: 쿠팡 마켓플레이스, 네이버스마트스토어 셀러.

**enabled_l4_domains:** `crud` (product), `payment`, `notification`, `audit-log`, `search`.
**l2_blocks_used:** `crud-list-adapter`, `crud-create-form`, `data-table`, `filter-bar`, `faceted-filter` (product filters), `kpi-card` (order stats), `event-stream` (order timeline).
**l3_pages_used:** `list-page` (products), `detail-page` (product/order), `create-page`, `edit-page`, `search-results-page`.
**business_invariants:**
- `order.total_amount == sum(items.unit_price × items.quantity)`
- `inventory.reservation must release on cart abandonment >30min`
- `payment.captured ⇒ order.confirmed (atomic)`
- `cancellation window enforced by order.confirmed_at + return_policy_days`
**business_observability:**
- `ecommerce.cart.abandoned_count` (Counter)
- `ecommerce.order.placed_total{channel}` (Counter)
- `ecommerce.checkout.duration_p99` (Histogram)
- `ecommerce.inventory.reservation_release_total` (Counter)
**evidence:**
- Shopify e-commerce architecture (https://shopify.dev/docs/api/admin-rest)
- 토스페이먼츠 결제 + 자동 환불 (https://docs.tosspayments.com)
- internal_design: composition derived from SP15 crud + SP30 payment

### Recipe 3 — `booking` (SP35, high-frequency)

**Business context:** Resource reservation system: hotels (Yanolja), restaurants (캐치테이블), classes (탈잉), facilities (마이리얼트립).

**enabled_l4_domains:** `crud` (resource/booking), `payment`, `notification` (reminder), `audit-log`. Backend_only: `scheduled-task` (no-show / reminder cron).
**l2_blocks_used:** `crud-list-adapter`, `data-table`, `date-range-filter`, `event-stream` (booking timeline), `kpi-card` (occupancy).
**l3_pages_used:** `list-page`, `detail-page`, `create-page` (booking form).
**business_invariants:**
- `booking.start_at < booking.end_at`
- `no double-booking for same (resource_id, time_range)` (DB unique index OR optimistic-lock)
- `cancellation.deadline == booking.start_at - cancellation_policy_hours`
- `reminder.scheduled_at must precede booking.start_at`
**business_observability:**
- `booking.created_total{resource_type}` (Counter)
- `booking.cancelled_total{reason}` (Counter)
- `booking.no_show_total` (Counter)
- `booking.occupancy_ratio` (Gauge)
**evidence:**
- Booking.com Connectivity API (https://developers.booking.com)
- 야놀자 클라우드 hotel-management blueprint (referenced via public case studies)
- internal_design: composition derived from SP15 crud + SP19 scheduled-task

### Recipe 4 — `crm` (SP36, mid-frequency)

**Business context:** Sales pipeline: lead → contact → deal → activity. Korean reference: 채널톡 CRM, 카카오싱크 marketing CRM.

**enabled_l4_domains:** `crud` (lead/contact/deal), `audit-log`, `notification`, `search`.
**l2_blocks_used:** `data-table`, `activity-feed`, `filter-bar`, `saved-view`, `kpi-card`, `event-stream`, `pipeline-kanban` (NEW L2 — see §5 surface table).
**l3_pages_used:** `list-page`, `detail-page`, `create-page`, `dashboard-page`.
**business_invariants:**
- `deal.amount must be currency-precision compliant`
- `lead → contact conversion preserves source_attribution`
- `activity.timestamp >= activity.created_at`
- `deal.stage transitions follow declared state machine`
**business_observability:**
- `crm.lead.captured_total{source}` (Counter)
- `crm.deal.stage_transitioned_total{from,to}` (Counter)
- `crm.deal.win_rate` (Gauge, calculated)
- `crm.activity.completed_total{type}` (Counter)
**evidence:**
- Salesforce CRM data model (publicly documented)
- HubSpot CRM API (https://developers.hubspot.com)
- internal_design: composition derived from SP15 crud + SP21 search

### Recipe 5 — `community` (SP36, mid-frequency)

**Business context:** Threaded discussion forum with moderation. Korean reference: 디시인사이드, 클리앙, 펨코.

**enabled_l4_domains:** `crud` (thread/post), `notification`, `search`, `audit-log`.
**l2_blocks_used:** `data-table`, `event-stream`, `filter-bar`, `bulk-actions-bar` (moderation), `confirm-dialog`, `activity-feed`.
**l3_pages_used:** `list-page`, `detail-page`, `create-page`.
**business_invariants:**
- `post.parent_thread_id must reference existing thread`
- `moderation.action must record actor + reason in audit-log`
- `reputation score is derived, not stored authoritatively`
- `soft-delete preserves audit trail`
**business_observability:**
- `community.post.created_total` (Counter)
- `community.moderation.action_total{action}` (Counter)
- `community.thread.daily_active_count` (Gauge)
- `community.search.query_total` (Counter)
**evidence:**
- Discourse Community Platform (https://docs.discourse.org)
- Reddit data model (publicly documented)
- internal_design: composition derived from SP15 crud + SP26 search + SP17 audit-log

### Recipe 6 — `marketplace` (SP36, mid-frequency)

**Business context:** Multi-sided platform with listings, bids, escrow, ratings. Korean reference: 당근마켓, 번개장터, 크몽.

**enabled_l4_domains:** `crud` (listing), `payment` (escrow), `search`, `audit-log`. Backend_only: `identity-verification` (seller KYC).
**l2_blocks_used:** `data-table`, `crud-create-form`, `faceted-filter`, `kpi-card`, `event-stream` (escrow timeline), `phone-verification-panel` (seller verification).
**l3_pages_used:** `list-page` (listings), `detail-page` (listing/transaction), `search-results-page`.
**business_invariants:**
- `escrow.held_until_buyer_confirmation`
- `seller must be identity-verified before listing publish`
- `bid.amount > current_highest_bid OR listing.is_buy_now`
- `transaction.platform_fee == listing.price × fee_rate`
**business_observability:**
- `marketplace.listing.published_total{category}` (Counter)
- `marketplace.escrow.held_amount` (Gauge)
- `marketplace.transaction.dispute_total{reason}` (Counter)
- `marketplace.seller.verified_total` (Counter)
**evidence:**
- 당근마켓 거래 시스템 (public case study)
- Etsy Marketplace API (https://developers.etsy.com)
- internal_design: composition derived from SP30 payment + SP31 identity-verification

### Recipe 7 — `lms` (SP37, vertical)

**Business context:** Online learning platform: courses, lessons, enrollment, progress tracking. Korean reference: 인프런, 클래스101, 패스트캠퍼스.

**enabled_l4_domains:** `crud` (course/lesson), `file-storage` (video/PDF), `notification` (due dates), `payment`, `audit-log`. Backend_only: `scheduled-task` (assignment deadlines).
**l2_blocks_used:** `data-table`, `event-stream` (progress timeline), `kpi-card`, `confirm-dialog`, `event-stream` (lesson playback events).
**l3_pages_used:** `list-page`, `detail-page` (course/lesson), `dashboard-page` (instructor).
**business_invariants:**
- `lesson.completion requires ≥80% video playback (or instructor-set threshold)`
- `enrollment.paid_at ⇒ course.access_granted`
- `assignment.due_at notifications fire at -24h and -1h`
- `progress.percentage is monotonically non-decreasing`
**business_observability:**
- `lms.course.enrolled_total{course_id}` (Counter)
- `lms.lesson.completed_total` (Counter)
- `lms.assignment.late_submission_total` (Counter)
- `lms.video.streaming_bandwidth_used` (Gauge)
**evidence:**
- 인프런 강의 시스템 (public product docs)
- Moodle architecture (https://docs.moodle.org)
- internal_design: composition derived from SP18 file-storage + SP19 scheduled-task

### Recipe 8 — `b2b-admin` (SP37, vertical)

**Business context:** B2B SaaS internal operations console: tenant management, analytics, audit, impersonation. Korean reference: 카카오엔터프라이즈 admin, 토스 비즈니스 admin.

**enabled_l4_domains:** `crud`, `audit-log`, `feature-flags`, `search`.
**l2_blocks_used:** `data-table`, `kpi-card`, `time-series-chart` (NEW L2 derived from existing chart blocks — see §5), `impersonation-banner`, `bulk-actions-bar`, `column-picker`, `faceted-filter`.
**l3_pages_used:** `admin-overview-page`, `audit-log-page`, `dashboard-page`, `list-page`.
**business_invariants:**
- `impersonation session must show banner + emit audit-log event`
- `admin queries must include tenant scope OR explicit cross-tenant flag`
- `사업자등록번호 validation on tenant onboarding (checksum-required rule applies)`
- `feature-flag mutations write to audit-log`
**business_observability:**
- `admin.impersonation.session_total{actor,target}` (Counter)
- `admin.tenant.active_count` (Gauge)
- `admin.feature_flag.toggle_total{flag,enabled}` (Counter)
- `admin.audit_log.query_latency_p99` (Histogram)
**evidence:**
- 토스 비즈니스 admin 패턴 (public case study)
- Linear Admin Panel design (https://linear.app)
- internal_design: composition derived from SP17 audit-log + SP28 feature-flags + SP31 사업자등록번호

### Recipe 9 — `cms` (SP37, vertical)

**Business context:** Editorial content management: articles, categories, draft/review/publish workflow, scheduled publish. Korean reference: 브런치, 네이버 블로그 에디터, 카카오 티스토리.

**enabled_l4_domains:** `crud` (article/category), `audit-log`, `search`, `file-storage` (media). Backend_only: `scheduled-task` (scheduled publish).
**l2_blocks_used:** `crud-create-form` (with `rich-text-editor` L1), `data-table`, `event-stream` (revision history), `confirm-dialog`, `bulk-actions-bar`, `filter-bar`.
**l3_pages_used:** `list-page`, `detail-page`, `create-page` (article editor), `wizard` (draft→review→publish).
**business_invariants:**
- `article.published_at ≥ article.scheduled_at`
- `revision history immutable; new revisions append-only`
- `category tree depth ≤ 5 (UX constraint)`
- `media uploads must scope to article.tenant_id`
**business_observability:**
- `cms.article.published_total{author}` (Counter)
- `cms.article.revision_total` (Counter)
- `cms.scheduled_publish.deferred_count` (Gauge)
- `cms.media.upload_size_bytes_total` (Counter)
**evidence:**
- Sanity CMS architecture (https://www.sanity.io/docs)
- Contentful API (https://www.contentful.com/developers/docs)
- internal_design: composition derived from SP18 file-storage + SP19 scheduled-task + SP32 rich-text-editor

### Recipe 10 — `internal-it` (SP37, vertical)

**Business context:** Internal IT service desk: tickets, approval workflows, integrations (Slack, Jira, ServiceNow). Korean reference: 잔디 워크플로, 카카오워크 결재 시스템.

**enabled_l4_domains:** `crud` (ticket/approval), `audit-log`, `notification`. (Webhook integration is a backend cross-cutting concern; the recipe references `templates/backend/integration/` not a new L4.)
**l2_blocks_used:** `data-table`, `event-stream` (ticket timeline), `confirm-dialog`, `approval-chain` (NEW L2 — see §5), `kpi-card`, `bulk-actions-bar`.
**l3_pages_used:** `list-page`, `detail-page`, `wizard` (approval flow).
**business_invariants:**
- `ticket.status transitions follow declared state machine`
- `approval.chain must be linearly ordered (no concurrent approvers per step)`
- `escalation triggers on SLA breach (configurable per ticket-type)`
- `integration.webhook payload signed (HMAC-SHA256)`
**business_observability:**
- `internal_it.ticket.opened_total{type}` (Counter)
- `internal_it.ticket.sla_breach_total` (Counter)
- `internal_it.approval.cycle_time_p99` (Histogram)
- `internal_it.integration.webhook_delivered_total{system}` (Counter)
**evidence:**
- Atlassian Jira Service Management API (https://developer.atlassian.com)
- ServiceNow ITSM data model (publicly documented)
- internal_design: composition derived from SP15 crud + SP17 audit-log

---

## §5 New Artifact Paths & SP Plan

### §5.1 Directory structure (NEW)

```
recipes/
├── README.md                              # recipe family index + cross-reference matrix
├── _MANIFEST.yaml                         # machine-readable list of recipes + version
├── saas-subscription/
│   ├── RECIPE.md
│   ├── spec-trio-template.yaml            # pre-filled Spec Trio fragments to copy into project
│   ├── L4-composition.md                  # which L4 to enable + wiring
│   └── L2-block-recipe.md                 # L2 block selection + composition order
├── e-commerce/...
├── booking/...
├── crm/...
├── community/...
├── marketplace/...
├── lms/...
├── b2b-admin/...
├── cms/...
└── internal-it/...

specs/recipes/
├── saas-subscription-recipe-l0.yaml       # recipe-level spec (composition contract)
├── e-commerce-recipe-l0.yaml
├── ...
└── internal-it-recipe-l0.yaml

practices/evals/recipe_governance_guard.sh  # NEW guard (SP39)
practices/evals/fixtures/
├── prefer-recipe-composition-over-l4-cross-import/
├── business-domain-must-declare-applied-recipe/
└── recipe-deviation-requires-justification/

practices/rules/
├── prefer-recipe-composition-over-l4-cross-import.md      # NEW (SP39, Java + React halves)
├── business-domain-must-declare-applied-recipe.md          # NEW (SP39)
└── recipe-deviation-requires-justification.md              # NEW (SP39)

skills/ax-verify-recipe/                                    # NEW Tier-2 (SP38)
└── SKILL.md

skills/ax-scaffold/                                         # EXTEND (SP38)
└── scripts/
    ├── new-business-recipe.sh                              # NEW: /ax-scaffold business <pattern> <project>
    └── analyze-business-text.sh                            # NEW: /ax-scaffold business --analyze
```

### §5.2 Recipe-level spec schema (NEW spec family)

`specs/recipes/<pattern>-recipe-l0.yaml`:

```yaml
schema_version: 1
pattern: saas-subscription
display_name: "Multi-tenant SaaS Subscription"
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
  # ... etc
l3_pages_used:
  - pricing-page
  - settings-overview
business_invariants:
  - id: SUB-INV-001
    statement: "subscription must have ≥1 active plan"
    verification: spec_trio_billing
  # ...
business_observability:
  - metric: saas.mrr.calculated_daily_count
    type: counter
  # ...
evidence:
  - provenance_class: external
    source: "Stripe Billing"
    url: "https://stripe.com/docs/billing/subscriptions/overview"
  - provenance_class: internal_design
    derives_from: ["SP30 billing", "SP28 feature-flags"]
```

### §5.3 New L2 blocks introduced by recipes (must ship in SP36/SP37)

- `pipeline-kanban.tsx` (Recipe 4 CRM — SP36)
- `time-series-chart.tsx` (Recipe 8 B2B admin — SP37; may already be derivable from existing chart blocks; verify and consolidate)
- `approval-chain.tsx` (Recipe 10 internal-it — SP37)

> Note: any L2 added MUST carry `evidence:` frontmatter per existing PRD-1 §1 rule.

### §5.4 SP plan (SP35–SP40)

| SP | Title | Deliverables | Acceptance |
|---|---|---|---|
| **SP35** | Recipes infra + 3 high-frequency recipes | `recipes/_MANIFEST.yaml`, `recipes/README.md`, 3 recipe directories (saas-subscription/e-commerce/booking) each with 4 artifacts, 3 recipe-level specs in `specs/recipes/`, `practices/evals/recipe_spec_referential_integrity_guard.sh` (validates `enabled_l4_domains:` resolves) | exit 0 from new referential-integrity guard + 3 recipe-level specs parse |
| **SP36** | 3 mid-frequency recipes + 1 new L2 | crm/community/marketplace recipes, `pipeline-kanban.tsx` L2 with evidence | exit 0 referential-integrity guard + L2 evidence guard pass |
| **SP37** | 4 vertical recipes + 2 new L2 | lms/b2b-admin/cms/internal-it recipes, `time-series-chart.tsx` (or consolidation note), `approval-chain.tsx` L2 with evidence | same |
| **SP38** | Skill subcommand + free-text inference + Tier-2 verifier + 50-fixture eval | `/ax-scaffold business <pattern>` script, `/ax-scaffold business --analyze` script, NEW `skills/ax-verify-recipe/SKILL.md` Tier-2, `practices/evals/fixtures/business-analyze-eval/` (50 fixtures), `practices/evals/business_analyze_accuracy_guard.sh` enforcing ≥80% accuracy | `/ax-verify-recipe <each-of-10>` exit 0 + analyze accuracy ≥80% |
| **SP39** | 3 enforcement rules + governance guard | 3 rule MDs with `protects_template_id` + `failing_fixture_path`, 3 fixture directories with failing examples, `practices/evals/recipe_governance_guard.sh` | guard exit 0 on green tree; non-zero on injected violations |
| **SP40** | Sealed verdict harness + release | sealed verdict for saas-subscription/e-commerce/crm (≥10/12 MUST + ≥5/8 SHOULD per recipe), `/ax-verify` all green, tag `v1.3.0-business-patterns`, PR to main | tag created + PR opened + all guards GREEN |

### §5.5 Verification Matrix (with observability_signal)

| SP | Surface | Verification Skill | Observability Signal |
|---|---|---|---|
| SP35 | 3 recipe dirs + specs/recipes/ | `practices/evals/recipe_spec_referential_integrity_guard.sh` | `recipe.spec.referential_integrity_pass_count` |
| SP36 | 3 recipe dirs + 1 L2 | same guard + `evidence_guard.sh` for L2 | `recipe.l2_block.added_total` |
| SP37 | 4 recipe dirs + 2 L2 | same | same |
| SP38 | subcommand + analyze + Tier-2 verifier | `/ax-verify-recipe <pattern>` + accuracy guard | `recipe.analyze.accuracy_p50`, `recipe.scaffold.invocation_total` |
| SP39 | 3 enforcement rules | `recipe_governance_guard.sh` + per-rule fixture exec | `recipe.governance.violation_total{rule}` |
| SP40 | sealed verdict + release | sealed-agent runner | `recipe.sealed_verdict.must_pass_count`, `recipe.sealed_verdict.should_pass_count` |

### §5.6 Skill topology table (post-SP40)

| Tier | Skill | Status |
|---|---|---|
| **Tier-1 (cap=4)** | `/ax-transform` | unchanged |
| **Tier-1** | `/ax-scaffold` | EXTENDED with `business` subcommand (SP38) |
| **Tier-1** | `/ax-verify` | unchanged |
| **Tier-1** | `/ax-fork-receiver` | unchanged |
| **Tier-2 (new)** | `/ax-verify-recipe` | NEW (SP38) |
| Tier-2 (existing) | `/ax-verify-{L1,L2,L3,L4,java,react,shared,domain}` | unchanged |
| Tier-3 (existing) | `/ax-guard-*` | unchanged |

**Tier-1 count after SP40: 4 (unchanged).** Constraint honored.

---

## §6 Autonomous Execution Safety

- Each SP commits in ONE atomic cluster (consistent with SP26/SP30 precedent).
- SP35 ships the directory infra in the first commit of the cluster; subsequent commits per recipe.
- SP38's free-text analyze accuracy guard is a HARD gate (≥80%) — if fixtures fail, SP38 cannot land. Recovery path: re-tune the `analyze-business-text.sh` keyword tables OR adjust the 50-fixture set with explicit critic justification.
- SP39 enforcement rules' failing fixtures MUST be reviewed for false-positive risk before landing — each fixture cites why the violation pattern is incorrect. False-positive escape hatch: a `RECIPE_DEVIATION.md` with provenance_class + rationale.
- SP40 sealed verdict: if a recipe scores <10/12 MUST, rerun verdict ONCE with a refined sub-agent prompt; if still failing, halt SP40 and open issue in DECISIONS.md.
- All SP work happens on branch `feat/business-patterns-sp35-sp40`. No direct main commits.
- All SP commits use `superpowers:writing-skills` conventions.
- Race-safe append protocol for `recipes/_MANIFEST.yaml`: rebase against HEAD before commit; `yq`-sorted insertion.

---

## §7 Pre-Mortem (≥3 scenarios)

### Scenario 1 — Recipe drift from L4 evolution (drift risk)

**Failure mode:** SP35 ships `saas-subscription` referencing `templates/L4/billing/` shape as of v1.2.0. Future PRD-N enhances `templates/L4/billing/` (adds new L2 dependency, changes Spec Trio operationIds). `recipes/saas-subscription/L4-composition.md` becomes stale.

**Detection:** `practices/evals/recipe_spec_referential_integrity_guard.sh` runs on EVERY commit, validating that `enabled_l4_domains:` resolves AND `l2_blocks_used:` resolves to existing files.

**Mitigation:**
- Guard is HARD (exit non-zero blocks commit).
- Recipe versioning: `recipes/_MANIFEST.yaml` records `compatible_with_catalog_version:` and `last_verified_at:`.
- Future PRD that touches L4 MUST include "recipe-impact-analysis" section if any recipe references that L4.

**Residual risk:** semantic drift (L4 changes meaning, not shape) — guard cannot catch. Mitigation: SP40 sealed verdict re-runs in CI quarterly.

### Scenario 2 — Free-text inference false-match (false positive risk)

**Failure mode:** User runs `/ax-scaffold business --analyze "I want to build a 결제 시스템 for video streaming"`. The system matches `e-commerce` because of "결제" keyword, but user really wanted `lms`-shaped scaffolding.

**Detection:** 50-fixture eval suite in SP38 includes adversarial fixtures (deliberately ambiguous descriptions). Accuracy guard requires ≥80% match.

**Mitigation:**
- `analyze` output is **advisory** — it prints "Top 3 matching recipes with confidence scores" and asks user to confirm before scaffolding.
- Subcommand exits with non-zero if confidence < 0.5 (threshold; tunable).
- Eval suite is extendable: fork-receivers can contribute fixtures.

**Residual risk:** Korean-language descriptions may underperform English fixtures. Mitigation: 25 of 50 fixtures are Korean.

### Scenario 3 — Enforcement rule false-positive / governance loop (process risk)

**Failure mode:** `prefer-recipe-composition-over-l4-cross-import` flags a legitimate cross-import that pre-dates the recipe. Fork-receiver is blocked from committing.

**Detection:** Each rule fixture has both `positive/` (should fail) and `negative/` (should pass) cases. SP39 acceptance: rule fires on positive ONLY.

**Mitigation:**
- `RECIPE_DEVIATION.md` escape hatch is documented; rule explicitly checks for its presence.
- Rule is `provenance_class: internal_design` — fork-receiver can disable via local config without changing catalog.
- Documentation note: rules are advisory in WARN mode for first 30 days post-v1.3.0, then HARD.

**Residual risk:** governance creep — recipes accumulate "always-deviate-justified" overrides that erode the prescriptive value. Mitigation: SP40 sealed verdict reports recipe-deviation count per recipe; recipes with >50% deviation rate get re-litigated in next PRD.

---

## §8 ADR Template (TD-2026-05-19-NNN)

Each SP that introduces a non-trivial design decision must add an ADR row to `templates/DECISIONS.md`:

```markdown
## TD-2026-05-19-001 — Business Pattern Recipes as composition layer (not L4)

**Decision:** Business Pattern Recipes ship as `recipes/<pattern>/` directories composing existing L4 domains. They do NOT register as new L4 in `templates/L4/` and do NOT register in `trio_integrity_allowlist.yaml`.

**Drivers:**
1. Composition kit framing (CLAUDE.md vision) — recipes are downstream of catalog, never invert it.
2. Atomic Spec Trio rule — recipes have no backend OpenAPI; they are pure composition contracts.
3. Tier-1 cap = 4 — recipe scaffolding ships as `/ax-scaffold business` subcommand, NOT new Tier-1.

**Alternatives considered:**
- Recipes as new L4 (rejected — duplicates atomic-Spec-Trio scope).
- Recipes as Tier-1 skill (rejected — breaks cap = 4).
- Recipes as docs-only (rejected — no machine enforcement).

**Why chosen:** Composition layer aligns with the user mandate ("AI judges business logic; system enforces catalog conformance") and preserves all existing invariants.

**Consequences:**
- New `recipes/` top-level directory + `specs/recipes/` sub-directory.
- New guard `recipe_spec_referential_integrity_guard.sh`.
- New Tier-2 `/ax-verify-recipe` skill.
- New 3 enforcement rules.

**Follow-ups:**
- Quarterly sealed verdict re-run in CI (post-SP40).
- Fork-receiver feedback drives recipes 11+ (deferred).
```

Each SP commit message references the ADR id.

---

## §9 Open Questions

(also persisted to `.omc/plans/open-questions.md` per Open_Questions protocol)

1. **`pipeline-kanban.tsx` evidence anchoring** — is there a battle-tested Korean SaaS reference (잔디 워크플로? 채널톡 deals?), or do we cite Trello/Linear and tag `provenance_class: external`? (Affects SP36 evidence requirement.)
2. **50-fixture accuracy threshold tuning** — ≥80% accuracy is a starting point. If SP38 eval yields 75%, do we (a) re-tune keyword tables, (b) lower threshold to 70%, or (c) introduce LLM-backed inference (cost+latency tradeoff)? Document explicit policy in SP38.
3. **Recipe-level spec vs RECIPE.md duplication** — should the YAML be canonical (machine-truth) and RECIPE.md a generated view, or do they live as parallel sources with cross-check guard? (Affects SP35 generation pipeline.)

---

## §10 Honored Constraints

- ✅ Tier-1 cap = 4 (preserved; `/ax-scaffold business` is subcommand).
- ✅ Composition kit framing (recipes compose existing L4, no new L4 per recipe).
- ✅ Spec Trio atomic rule (recipe-level spec is NOT a domain Spec Trio; recipes do not register in trio_integrity_allowlist).
- ✅ Evidence-anchored (each recipe cites real-world references + Korean context).
- ✅ Korean enterprise context (사업자등록번호 in b2b-admin; 토스 / 쿠팡 / 야놀자 / 인프런 / 채널톡 / 당근마켓 references).
- ✅ Out-of-scope for deployment/CI (no GH Actions, no release.yml).
- ✅ No new MockMvc tests (sealed verdict harness uses agent + RestAssured pattern).
- ✅ 4-iter ralplan APPROVE pattern (this is iter 1 draft).
- ✅ AGENTS.md sentinel preservation (no changes to AGENTS.md schema; recipes referenced FROM AGENTS.md via new section "Composition recipes" in SP40 closing commit).

---

## Final Checklist (Planner)

- [x] Recipes are composition manifests, not domain Spec Trios
- [x] Tier-1 count remains 4 after SP40
- [x] Every recipe carries evidence + Korean reference
- [x] 6 SPs total (SP35–SP40)
- [x] Sealed verdict harness present (SP40)
- [x] Free-text inference has eval gate (SP38, ≥80%)
- [x] Enforcement rules have failing fixtures (SP39)
- [x] Pre-mortem ≥3 scenarios (§7)
- [x] ADR template (§8)
- [x] Open Questions (§9)
- [x] Honored Constraints (§10)
- [x] RALPLAN-DR summary at top with ≥2 viable options + rationale
- [ ] Architect review (Round 5 next step)
- [ ] Critic review (Round 5 next step)
