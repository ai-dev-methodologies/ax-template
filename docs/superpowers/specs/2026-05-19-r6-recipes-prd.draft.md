# R6 — 7 Deferred Recipes Absorption PRD — 2026-05-18 (Round 6, ralplan iter 0/draft)

> **Status:** DRAFT (Planner initial). Round 6 in the ax-template ralplan series. Absorbs the 7 deferred recipes documented in R5 PRD `recipes/_MANIFEST.yaml#deferred_recipes` and §4 deferred table.
> **Date:** 2026-05-18. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-17-frontend-templatization-prd.md` (CLOSED — SP1–SP12, `v1.0.0`).
> - `2026-05-18-catalog-extension-prd.md` (CLOSED — SP13–SP22, `v1.0.0-catalog-complete`, `9212989`).
> - `2026-05-18-functional-extension-prd.md` (CLOSED — SP23–SP29, `v1.1.0-functional-complete`, `1ab8f54`).
> - `2026-05-18-p1-absorption-prd.md` (CLOSED — SP30–SP34, `v1.2.0-p1-absorbed`, `26de945`).
> - `2026-05-19-business-pattern-recipes-prd.md` (CLOSED — SP35–SP38, `v1.3.0-business-patterns`, `b5f16b4`).
> **Branch (when execution starts):** `feat/r6-recipes-sp39-sp42`.
> **Targeted tag:** `v1.4.0-recipes-complete`.
> **Mandate trigger:** User said "계속 go" after R5 v1.3.0 release. R5 §11 listed 7 deferred recipes explicitly for "round 6+ pending fork-receiver demand". Receiver demand is now treated as catalog-stable proof (R5 sealed-verdict precedent) — round 6 absorbs all 7 with the same atomic Spec-Trio pattern.

---

## §1 RALPLAN-DR Summary

### Principles (R5 inheritance + R6 specialization)

1. **Composition kit, not single product.** Recipes COMPOSE existing L4 domains. R6 introduces ZERO new L4 — every recipe binds to one or more of the 10 existing L4 domains (`auth`, `billing`, `crud`, `payment`, `notification`, `audit-log`, `feature-flags`, `file-storage`, `search`, `practices`).
2. **Spec-before-code, evidence-anchored.** Every R6 recipe carries `evidence:` block. Korean references are either WebFetch-verified verbatim OR explicitly downgraded to `provenance_class: internal_design` (no fabrication, per R5 iter 2–3 critic learning on Coupang downgrade).
3. **Binary verification per axis.** Each SP terminates when `/ax-verify` exits 0, every domain `/ax-verify-domain <enabled-l4>` exits 0, and the SP42 sealed verdict harness records ≥10/12 MUST + ≥5/8 SHOULD per recipe.
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count **stays at 4** (`/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`). Tier-2 count **stays at 8 (unchanged)**. R6 ships NO new skill surfaces — recipes plug into the existing `/ax-scaffold business <pattern>` subcommand from R5 SP36.
5. **Atomic Spec-Trio rule.** Each recipe lands atomically: `recipes/<pattern>/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}` + `specs/recipes/<pattern>-recipe-l0.yaml` + `skills/_tests/sealed-verdict/<pattern>-verdict.md` + `recipes/_MANIFEST.yaml` upgrade row (deferred → active) — all in the same SP. **No half-shipped recipes.**
6. **Cluster by composition disjointness.** Recipes that share NO mutating L4 logic can ship in the same SP without risking cross-recipe contamination. R6 groups 7 recipes into 3 SPs (SP39/40/41) by composition disjointness, then sealed-verdict harness + tag in SP42.
7. **Recipe does not ship code; AI implements business logic.** Inherited from R5 §1.7 — recipes specify WHICH catalog atoms compose, NOT the business logic.
8. **R5 `applied_recipe:` rule honored.** SP37 enforcement rule `business-domain-must-declare-applied-recipe` MUST pass for every L4 domain in every R6 recipe's `enabled_l4_domains:` list. Domains that already declare `applied_recipe: saas-subscription | e-commerce | crm` may need multi-recipe support — see Open Question Q1.
9. **No new L2 / L3 this cycle.** All 7 R6 recipes cite EXISTING L2 blocks (92 shipped) and EXISTING L3 pages (20 shipped). Any recipe that would require a net-new L2/L3 is downgraded back to deferred — see Out-of-scope.

### Decision Drivers (top 3)

1. **Verdict-anchored standard parity.** R5 established 100% sealed-verdict coverage as the catalog standard. All 7 R6 recipes ship with sealed verdicts (no partial coverage) or stay deferred. This is binding.
2. **Composition disjointness drives clustering.** Recipes that touch overlapping L4 mutating logic (e.g., two recipes both extending `payment`) cannot ship in the same SP without risking contamination of the L4 README's `applied_recipe:` field. Clustering MUST minimize L4 overlap per SP.
3. **Korean reference verifiability.** R5 iter 2–3 critic blocker was Korean URL evidence fidelity. R6 pre-categorizes each Korean reference into one of three classes (verified-verbatim / accessible-WebFetch-verify-in-SP / internal_design-only) BEFORE SP execution to prevent iteration cycles.

### Viable Options Considered (≥2 mandatory)

- **Option A — All 7 recipes in one mega-SP.**
  - Pros: One atomic commit; minimal SP overhead.
  - Cons: Mega-SP violates the SP atomicity rule (R5 §1.5); one failed recipe blocks the other 6; 7 sealed verdicts in one SP overwhelms the harness; Critic iter cycles balloon.
  - **Rejected.**

- **Option B — 7 individual SPs (one per recipe) + sealed verdict harness SP.**
  - Pros: Smallest possible atomic unit.
  - Cons: 8 SPs vs R5's 4 SPs is 2× the wall-time; no clustering benefit when recipes share NO mutating L4 logic; over-decomposition.
  - **Rejected.** Atomicity over-applied.

- **Option C — 3 clustered SPs by composition disjointness + 1 sealed verdict harness SP (CHOSEN).**
  - SP39: **high-priority verticals with file/payment** — `booking + lms + cms` (file-storage + scheduled-task heavy, partial payment overlap acceptable since recipes touch different `payment` scenarios: booking deposit vs lms enrollment).
  - SP40: **content + commerce** — `community + marketplace` (search + crud heavy, marketplace adds payment(escrow) + identity-verification but community has no payment).
  - SP41: **admin/ops** — `b2b-admin + internal-it` (audit-log + crud heavy; b2b-admin has feature-flags + search; internal-it has notification + integration).
  - SP42: sealed verdict harness for 7 recipes + `/ax-verify` all + tag `v1.4.0-recipes-complete` + PR to main.
  - Pros: 4 SPs same as R5; minimizes L4 mutating overlap per SP; matches R5 cadence.
  - Cons: 3-recipe SP39 is larger surface than R5 SPs — mitigation: SP39 ships `booking + lms + cms` only as RECIPE.md + spec + manifest entry; sealed verdict still deferred to SP42.
  - **CHOSEN.**

- **Option D — 2 SPs (high-priority cluster + remaining cluster) + sealed verdict harness SP.**
  - Pros: 3 SPs vs 4.
  - Cons: 4-recipe SP (remaining cluster) violates atomicity; Critic iter cycles likely high.
  - **Rejected.**

- **Option E — Defer 4 niche (lms / cms / b2b-admin / internal-it) until v2 retrospective; ship only booking + community + marketplace.**
  - Pros: Smaller scope (3 recipes); matches R5 cadence exactly.
  - Cons: Leaves 4 recipes in `deferred-pending-fork-receiver-demand` indefinitely; user mandate "계속 go" implies absorbing all 7.
  - **Rejected.** User mandate prevails.

### Mode

**DELIBERATE.** Retained because: (a) 7 recipes × sealed-verdict harness is the largest verification surface in any cycle so far; (b) `applied_recipe:` rule has multi-recipe ambiguity (Open Question Q1) that needs explicit resolution; (c) Korean reference verifiability requires pre-categorization gate; (d) wall-time ≈1–1.5 weeks. Pre-mortem (≥3 scenarios) + expanded test plan + observability_signal table mandatory.

### Recommended: **Option C — 3 clustered SPs + 1 sealed-verdict harness SP = 4 SPs (SP39–SP42), tag `v1.4.0-recipes-complete`.**

```
SP39 (atomic — booking + lms + cms cluster: file/scheduled-task verticals)
    ↓
SP40 (atomic — community + marketplace cluster: content + commerce)
    ↓
SP41 (atomic — b2b-admin + internal-it cluster: admin/ops)
    ↓
SP42 (FINAL — sealed verdict harness × 7 + /ax-verify all + tag v1.4.0-recipes-complete + PR)
```

All SP linear. Total: **4 SPs, ≈ 8–10 d wall-time.**

---

## §2 Context

### R5 v1.3.0 state (disk-verified 2026-05-18)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 48 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | 10 | `templates/L4/` (`auth, billing, crud, payment, notification, audit-log, feature-flags, file-storage, search, practices`) |
| Active recipes | 3 | `recipes/{saas-subscription, e-commerce, crm}/` |
| Deferred recipes | 7 | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Practices rules | 84 | `practices/rules/` (R5 added 3 enforcement rules: `prefer-recipe-composition-over-l4-cross-import`, `business-domain-must-declare-applied-recipe`, `recipe-invariants-must-resolve`) |
| Sealed verdicts | 3 | `skills/_tests/sealed-verdict/{saas-subscription, e-commerce, crm}-verdict.md` |
| Tier-1 skills | 4 (FROZEN) | `/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver` |
| Tier-2 skills | 8 (FROZEN) | unchanged |

### R6 scope

Absorb all 7 deferred recipes from `recipes/_MANIFEST.yaml#deferred_recipes`:

1. **booking** — calendar + availability + reservation + cancellation
2. **community** — threads + posts + moderation + reputation
3. **marketplace** — listings + bids + escrow + ratings
4. **lms** — courses + lessons + enrollment + progress
5. **b2b-admin** — multi-tenant ops + analytics + audit
6. **cms** — articles + categories + drafts + publishing workflow
7. **internal-it** — ticketing + approval workflows + integration

Same atomic Spec-Trio pattern as R5. NO new L2 / L3 / L4 / skill surface. Tier counts frozen.

---

## §3 Objectives + Guardrails

### Must Have

- 7 recipes shipped to `recipes/<pattern>/` with full Spec-Trio (`RECIPE.md`, `L4-composition.md`, `L2-block-recipe.md`, `spec-trio-template.yaml`).
- 7 recipe-level specs at `specs/recipes/<pattern>-recipe-l0.yaml`.
- 7 sealed verdicts at `skills/_tests/sealed-verdict/<pattern>-verdict.md`, each ≥10/12 MUST + ≥5/8 SHOULD.
- `recipes/_MANIFEST.yaml`: all 7 entries moved from `deferred_recipes:` to `recipes:` with `status: active`; `deferred_recipes:` list emptied (or reduced to empty array with comment "all R5 deferred recipes absorbed in R6 — v1.4.0").
- Every L4 domain in every recipe's `enabled_l4_domains:` list has `applied_recipe: <pattern>` (or `applied_recipes: [<pattern1>, <pattern2>]` if Q1 resolves to multi-value) in its `templates/L4/<domain>/README.md` frontmatter.
- `/ax-verify` exits 0.
- `practices/evals/recipe_governance_guard.sh` exits 0 for all 10 (3 R5 + 7 R6) active recipes.
- `recipes/README.md` updated to list all 10 active recipes; `deferred_recipes:` section either removed or marked empty.
- Korean references either WebFetch-verified verbatim with URL+quote+timestamp OR explicitly carry `provenance_class: internal_design` with rationale.
- Tag `v1.4.0-recipes-complete` on the merge commit.

### Must NOT Have

- NO new L4 domain (composition kit principle — recipes compose existing L4 only).
- NO new L2 block or L3 page (any recipe needing one downgrades to deferred — see Out-of-scope).
- NO new Tier-1 or Tier-2 skill.
- NO `RECIPE_DEVIATION.md` governance file (R5 §1.4 rejection stands — inline `override_allowed:` only).
- NO `--analyze` free-text NLP inference on `/ax-scaffold` (R5 rejection stands).
- NO new enforcement rule unless a recipe demonstrably needs one (R5 SP37's 3 rules cover the composition contract; R6 only EXTENDS them if Q1 resolves to multi-recipe).
- NO Korean reference fabrication. WebFetch HTTP 403 / 404 → downgrade to `internal_design` with explicit rationale (R5 Coupang precedent).
- NO change to git workflow, CI policy, release process (skill stays catalog-quality probe; team policy fork-receiver autonomy).

---

## §4 Recipe Inventory (7 patterns)

> Each recipe row below specifies: L4 composition (existing only), L2 blocks used (existing only), Korean evidence anchor with pre-classification, business invariants bound to spec_ref/rule_ref, and override allowance scope.

### 4.1 `booking`

- **L4 composition** (5 existing): `crud` (resource entity), `payment` (deposit/full payment at booking), `notification` (reminders), `audit-log` (cancellation trail), `feature-flags` (e.g., guest-checkout-allowed).
- **L2 blocks used** (existing only): `calendar` (L1 — confirmed at `templates/L1/components/calendar.tsx`), `date-range-picker` (L1), `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `confirm-dialog`, `notification-list`, `payment-checkout-form`, `payment-method-picker`, `kpi-card`, `time-series-chart`, `relative-time` (L1).
  - **NOTE:** R5 deferred-table mentioned a need for a `BookingForm` / `AvailabilityGrid` L2 block. Re-verification: no such L2 exists. Recipe uses `crud-create-form` + `calendar` L1 + `date-range-picker` L1 composition. No new L2 required.
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Korean reference anchor pre-classification:**
  - 야놀자: `provenance_class: internal_design` (no public API docs URL; reservation lifecycle pattern derived from generic OTA observation).
  - 네이버 예약: `provenance_class: accessible-WebFetch-verify-in-SP39` (`https://developers.naver.com/docs/login/api/api.md` — Naver Login docs are public; reservation-specific API docs may exist under `https://developers.naver.com/docs/`).
  - External anchor: Booking.com Connectivity API public case study (verify URL during SP39 if reachable; otherwise OpenTable / Airbnb host docs).
- **Business invariants (bound to spec_ref/rule_ref):**
  - BOOKING-INV-001: Reservation must not double-book a resource for overlapping time windows. Binding: `spec_ref: specs/crud-l0.yaml#CRUD-VALIDATION-002` + `rule_ref: practices/rules/idempotency-key-on-mutations.md`.
  - BOOKING-INV-002: Cancellation within free-window does not charge deposit. Binding: `spec_ref: specs/billing-l0.yaml#BILLING-AUTHZ-002` + recipe-level invariant `recipes/booking/spec-trio-template.yaml#BOOKING-LIFECYCLE-001`.
  - BOOKING-INV-003: No-show triggers audit-log event with operator and timestamp. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-EMIT-001`.
- **Override allowance:** inline `override_allowed:` block (R5 standard) — fork-receiver may skip `payment` if booking is free or `feature-flags` if no guest-checkout. No separate deviation file.

### 4.2 `community`

- **L4 composition** (4 existing): `crud` (thread + post entities), `notification` (reply/mention alerts), `search` (full-text on threads + posts), `audit-log` (moderation actions, deletions).
- **L2 blocks used** (existing only): `rich-text-editor` (L1), `crud-create-form`, `crud-list-adapter`, `data-table`, `expandable-row`, `notification-list`, `notification-bell`, `notification-item`, `search-input`, `search-palette`, `result-highlighter`, `recent-searches`, `live-presence`, `confirm-dialog`.
  - **NOTE:** R5 deferred-table mentioned `ThreadList`, `PostComposer`, `ModeratorPanel`. Re-verification: no L2 with those exact names. Recipe uses `crud-list-adapter` + `rich-text-editor` + `expandable-row` composition. No new L2 required.
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `search-results-page`.
- **Korean reference anchor pre-classification:**
  - 디시인사이드 / 뽐뿌: `provenance_class: internal_design` (no public API/dev portal; thread lifecycle from generic forum observation).
  - 카카오 채팅 dev docs: `provenance_class: accessible-WebFetch-verify-in-SP40` (Kakao Developers — `https://developers.kakao.com/` — verify community/messaging API exists; if 403, downgrade).
  - External anchor: Discourse REST API docs (`https://docs.discourse.org/`) — verify in SP40.
- **Business invariants:**
  - COMMUNITY-INV-001: Soft-deleted posts retain audit trail visible to moderators. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001`.
  - COMMUNITY-INV-002: Reputation score updates are eventually consistent and idempotent per event_id. Binding: `rule_ref: practices/rules/idempotency-key-on-mutations.md`.
  - COMMUNITY-INV-003: Search index updates lag thread/post writes by ≤30 s (advisory observability_signal). Binding: recipe-level `recipes/community/spec-trio-template.yaml#COMMUNITY-SEARCH-LAG-001`.
- **Override allowance:** inline — skip `search` for very small communities; skip `notification` for read-only forums.

### 4.3 `marketplace`

- **L4 composition** (5 existing): `crud` (listing entity), `payment` (escrow lifecycle), `search` (listing discovery), `notification` (bid alerts), `audit-log` (transaction trail). **`feature-flags`** optional for KYC gating.
  - **KYC scope clarification:** R5 deferred-table mentioned "identity-verification(KYC)". There is NO `identity-verification` L4. Recipe binds KYC requirement to `feature-flags` (e.g., `kyc-required: true`) + reference to upstream KYC provider via `audit-log` event source — no new L4.
- **L2 blocks used** (existing only): `crud-list-adapter`, `crud-create-form`, `crud-edit-form`, `data-table`, `filter-bar`, `faceted-filter`, `search-input`, `search-palette`, `payment-checkout-form`, `payment-method-picker`, `notification-list`, `notification-bell`, `kpi-card`, `confirm-dialog`, `time-series-chart`.
  - **NOTE:** R5 mentioned `ListingCard`, `BidForm`, `EscrowTimeline` L2. Re-verification: no such L2. Recipe uses `crud-create-form` + `payment-checkout-form` + recipe-level escrow state diagram (documented in `recipes/marketplace/RECIPE.md`, NOT as L2 block).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `search-results-page`, `dashboard-page`.
- **Korean reference anchor pre-classification:**
  - 당근마켓 / 번개장터: `provenance_class: internal_design` (no public API/dev docs; secondhand-marketplace pattern from generic observation).
  - External anchor: Etsy Open API public docs (`https://developers.etsy.com/documentation/`) — verify in SP40.
  - External anchor 2: Stripe Connect (escrow split payments) — `https://stripe.com/docs/connect` — verify in SP40.
- **Business invariants:**
  - MARKETPLACE-INV-001: Escrow funds released only after buyer confirmation OR dispute-window expiry. Binding: `spec_ref: specs/payment-l0.yaml#PAYMENT-LIFECYCLE-003` (verify ID exists; otherwise recipe-level invariant).
  - MARKETPLACE-INV-002: Listing search results exclude soft-deleted + moderator-hidden listings. Binding: `spec_ref: specs/search-l0.yaml`.
  - MARKETPLACE-INV-003: KYC required before high-value listing creation (threshold configurable via feature-flag). Binding: `rule_ref: practices/rules/no-rrn-in-form-fields.md` (KYC must not log RRN, per 개인정보보호법) + `spec_ref: specs/feature-flags-l0.yaml`.
- **Override allowance:** inline — skip `feature-flags`/KYC for C2C-only no-fiat marketplaces; swap `payment` to a marketplace-specific PSP via blueprint adapter.

### 4.4 `lms`

- **L4 composition** (5 existing): `crud` (course + lesson + enrollment entities), `file-storage` (video / PDF / slide assets), `notification` (enrollment confirmations, due-date reminders), `payment` (course purchase), `audit-log` (enrollment + completion trail).
  - **scheduled-task scope:** R5 deferred-table mentioned `scheduled-task` L4. Re-verification: no such L4. Due-date reminder logic is handled by `notification` L4's scheduling primitive (verify in SP39: does `templates/L4/notification/` ship a scheduler? If not, recipe documents that fork-receiver must add a job scheduler from their stack — `applied_recipe` constraint still binds).
- **L2 blocks used** (existing only): `crud-list-adapter`, `crud-create-form`, `crud-edit-form`, `data-table`, `file-dropzone` (L1), `payment-checkout-form`, `payment-method-picker`, `notification-list`, `progress` (L1), `kpi-card`, `markdown-renderer` (L1), `confirm-dialog`.
  - **NOTE:** R5 mentioned `CourseCard`, `LessonPlayer`, `ProgressBar` L2. Re-verification: no such L2. `progress` L1 + `markdown-renderer` L1 + recipe-level video embedding (in RECIPE.md guidance, NOT as L2).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`, `pricing-page`.
- **Korean reference anchor pre-classification:**
  - 인프런 / 패스트캠퍼스: `provenance_class: internal_design` (no public API docs; LMS pattern from generic observation).
  - External anchor: Moodle developer docs (`https://docs.moodle.org/dev/`) — verify in SP39.
  - External anchor 2: Coursera for Campus public docs — verify in SP39.
- **Business invariants:**
  - LMS-INV-001: Course completion certificate issued only when ALL required lessons marked complete. Binding: `spec_ref: specs/crud-l0.yaml#CRUD-VALIDATION-002` + recipe-level invariant.
  - LMS-INV-002: Refund window (configurable per course) auto-closes payment dispute path. Binding: `spec_ref: specs/payment-l0.yaml#PAYMENT-REFUND-001` (verify ID exists).
  - LMS-INV-003: Video / PDF assets served via signed URL (≤15 min TTL). Binding: `rule_ref: practices/rules/presigned-url-signature-required.md`.
- **Override allowance:** inline — skip `payment` for free LMS; swap `file-storage` adapter to alternate object store.

### 4.5 `b2b-admin`

- **L4 composition** (5 existing): `crud` (resource entities), `audit-log` (admin action trail), `feature-flags` (tenant-scoped toggles), `search` (cross-tenant lookup), `auth` (impersonation + RBAC).
  - **identity-verification scope:** R5 mentioned `identity-verification` L4. NOT shipped. Recipe binds verification to `auth` L4 (admin role gating) + `feature-flags` (per-tenant rollout). No new L4.
- **L2 blocks used** (existing only): `impersonation-banner`, `kpi-card`, `time-series-chart`, `data-table`, `filter-bar`, `bulk-actions-bar`, `bulk-export`, `search-palette`, `feature-flag-toggle`, `feature-gate`, `audit-log-page` (L3, not L2 — adjust), `column-picker`, `column-reorder`, `saved-view`, `saved-filters`.
- **L3 pages used:** `dashboard-page`, `admin-overview-page`, `list-page`, `detail-page`, `audit-log-page`, `settings-overview`.
- **Korean reference anchor pre-classification:**
  - 채널톡 internal admin / 토스ID: `provenance_class: internal_design` (channel.io ko already cited in R5 — reuse only if scope identical; otherwise downgrade).
  - External anchor: Auth0 Dashboard / Okta Workforce Identity public docs — verify in SP41.
  - External anchor 2: Salesforce Setup Audit Trail docs — verify in SP41.
- **Business invariants:**
  - B2BADMIN-INV-001: Impersonation events always emit audit-log row with `impersonator_id`, `impersonated_id`, `started_at`, `ended_at`. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-EMIT-001` + `rule_ref: practices/rules/business-domain-must-declare-applied-recipe.md`.
  - B2BADMIN-INV-002: Tenant-scoped feature-flag changes are immutable history (no destructive delete). Binding: `spec_ref: specs/feature-flags-l0.yaml`.
  - B2BADMIN-INV-003: KPI aggregation respects tenant boundary (no cross-tenant leakage). Binding: `spec_ref: specs/auth-asvs-l1.yaml` (tenant isolation).
- **Override allowance:** inline — skip `search` for single-tenant deployments; skip `feature-flags` for static config.

### 4.6 `cms`

- **L4 composition** (4 existing): `crud` (article + category entities), `audit-log` (publishing workflow trail), `search` (article discovery), `file-storage` (image / media assets).
  - **scheduled-task scope (scheduled publish):** R5 mentioned `scheduled-task` for "scheduled publish". Same handling as LMS — no such L4. Recipe documents fork-receiver responsibility for cron/scheduler + notification L4 emit-on-publish.
- **L2 blocks used** (existing only): `rich-text-editor` (L1, already shipped in SP32), `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `markdown-renderer` (L1), `file-dropzone` (L1), `auto-save-indicator`, `dirty-guard`, `confirm-dialog`, `search-input`, `result-highlighter`, `field-wizard`.
  - **L3 `wizard` page used for draft→review→publish workflow.**
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `wizard` (draft→review→publish).
- **Korean reference anchor pre-classification:**
  - 카카오 채널 / 네이버 블로그: `provenance_class: internal_design` (no public CMS API docs at user-blog tier; pattern from generic observation).
  - External anchor: Sanity.io docs (`https://www.sanity.io/docs/`) — verify in SP39.
  - External anchor 2: Contentful public docs (`https://www.contentful.com/developers/docs/`) — verify in SP39.
- **Business invariants:**
  - CMS-INV-001: Published article cannot be hard-deleted; soft-delete + audit-log only. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-RETENTION-001`.
  - CMS-INV-002: Draft→review→publish state transitions require role authorization. Binding: `spec_ref: specs/auth-asvs-l1.yaml` + recipe-level state-machine spec.
  - CMS-INV-003: Scheduled publish time honored within ±5 min (advisory observability_signal). Binding: recipe-level invariant.
- **Override allowance:** inline — skip `search` for small-volume CMS; swap `file-storage` adapter.

### 4.7 `internal-it`

- **L4 composition** (4 existing): `crud` (ticket entity), `audit-log` (workflow trail), `notification` (assignee alerts, SLA reminders), `feature-flags` (workflow variations per department).
  - **integration(webhook) scope:** R5 mentioned `integration` L4. NOT shipped. Recipe binds webhook outbound to existing `notification` L4's external-channel adapter primitive (verify in SP41). Inbound webhook receipt is fork-receiver's API gateway concern — recipe documents but does not enforce.
- **L2 blocks used** (existing only): `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `notification-list`, `notification-bell`, `confirm-dialog`, `field-wizard`, `expandable-row`, `kpi-card`, `time-series-chart`, `event-stream`.
  - **NOTE:** R5 mentioned `TicketCard`, `ApprovalChain`, `StatusBadge` L2. Re-verification: `badge` L1 exists (`templates/L1/components/badge.tsx`). `ApprovalChain` does NOT — recipe uses `field-wizard` + `expandable-row` composition for approval visualization. No new L2.
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`, `wizard` (multi-step approval).
- **Korean reference anchor pre-classification:**
  - 잔디 / 채널톡 IT 워크플로우: `provenance_class: internal_design` (no public API docs for IT-ticket workflow tier).
  - External anchor: Jira REST API public docs (`https://developer.atlassian.com/cloud/jira/platform/rest/v3/`) — verify in SP41.
  - External anchor 2: ServiceNow public dev docs (`https://developer.servicenow.com/`) — verify in SP41.
- **Business invariants:**
  - INTERNAL-IT-INV-001: Ticket state transitions require approver role for protected transitions. Binding: `spec_ref: specs/auth-asvs-l1.yaml` + recipe-level state-machine.
  - INTERNAL-IT-INV-002: SLA breach emits audit-log + notification event. Binding: `spec_ref: specs/audit-log-l0.yaml#AUDIT-EMIT-001`.
  - INTERNAL-IT-INV-003: Outbound webhook deliveries are retried with exponential backoff and recorded. Binding: `rule_ref: practices/rules/idempotency-key-on-mutations.md`.
- **Override allowance:** inline — skip `feature-flags` for single-workflow IT teams; skip webhook integration for closed networks.

---

## §4.5 SP Plan + Verification Matrix

| SP | Atomic deliverables | TDD anchor (RED → GREEN) | Verification | Observability_signal (advisory) |
|---|---|---|---|---|
| **SP39** (booking + lms + cms cluster) | 3 × `recipes/<pattern>/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; 3 × `specs/recipes/<pattern>-recipe-l0.yaml`; `recipes/_MANIFEST.yaml` moves 3 entries deferred→active; `templates/L4/{crud,payment,notification,audit-log,feature-flags,file-storage}/README.md` updated with `applied_recipes:` list (R5 rule extension if Q1 → multi). Korean WebFetch verifications captured to `practices/upstream/r6-sp39-evidence-snapshot.md`. | `practices/evals/recipe_governance_guard.sh` initially RED (3 new recipes missing applied_recipe wiring) → GREEN after L4 README updates. | `/ax-verify-domain` × 6 L4 exit 0; `recipe_governance_guard.sh` exit 0; `recipe_spec_referential_integrity_guard.sh` exit 0 (R5 SP35 guard). | `recipe.booking.active_total`, `recipe.lms.enrollment_total`, `recipe.cms.scheduled_publish_lag_p95` — advisory, no emitter test enforced. |
| **SP40** (community + marketplace cluster) | 2 × recipe directories; 2 × recipe-level specs; `recipes/_MANIFEST.yaml` moves 2 entries deferred→active; `templates/L4/{crud,notification,search,audit-log,payment,feature-flags}/README.md` updated. Korean WebFetch evidence captured. | `recipe_governance_guard.sh` RED for 2 new recipes → GREEN after L4 README updates. | `/ax-verify-domain` × 6 L4 exit 0; `recipe_governance_guard.sh` exit 0; `recipe_spec_referential_integrity_guard.sh` exit 0. | `recipe.community.post_total`, `recipe.marketplace.escrow_pending_total` — advisory. |
| **SP41** (b2b-admin + internal-it cluster) | 2 × recipe directories; 2 × recipe-level specs; `recipes/_MANIFEST.yaml` moves last 2 entries deferred→active AND empties `deferred_recipes:` (or comments "all absorbed in R6"); `templates/L4/{auth,crud,audit-log,feature-flags,search,notification}/README.md` updated. Korean WebFetch evidence captured. | `recipe_governance_guard.sh` RED for 2 new recipes → GREEN. | `/ax-verify-domain` × 6 L4 exit 0; `recipe_governance_guard.sh` exit 0; `recipe_spec_referential_integrity_guard.sh` exit 0; `recipes/_MANIFEST.yaml` schema validation passes (deferred list empty). | `recipe.b2b_admin.impersonation_total`, `recipe.internal_it.sla_breach_total` — advisory. |
| **SP42** (FINAL — sealed verdict harness + tag + PR) | 7 × `skills/_tests/sealed-verdict/<pattern>-verdict.md` (each ≥10/12 MUST + ≥5/8 SHOULD); `recipes/README.md` rewrite — all 10 active recipes listed, `deferred_recipes` section removed or "(none — all absorbed in v1.4.0)"; `/ax-verify` full suite exit 0; tag `v1.4.0-recipes-complete`; PR to `main`. | Sealed verdict sub-agent harness × 7 RED (verdict file absent) → GREEN (each verdict file present, scores meet threshold). | `/ax-verify` exit 0 (all guards green); manual review of 7 sealed verdicts. | Aggregated `recipes.active_total: 10`; `recipes.deferred_total: 0`. |

**SP atomicity rule:** Within a single SP, all recipes in the cluster ship together OR all fail together. No half-shipped recipe within a cluster.

**SP linearization:** SP39 → SP40 → SP41 → SP42. No parallel branches.

---

## §5 Autonomous Execution Safety

- **Pre-flight gate (before SP39 starts):** Korean URL pre-categorization confirmed for all 7 recipes (this PRD §4 above). If any reference promoted from `internal_design` to `accessible-WebFetch-verify-in-SP*` returns HTTP 4xx/5xx during SP execution, demote back to `internal_design` with rationale captured in `practices/upstream/r6-sp<N>-evidence-snapshot.md` — no fabrication.
- **Mid-flight gate (between SPs):** `git status` clean; `/ax-verify-domain` × all touched L4 exit 0; commit message references SP number.
- **Stop conditions:** If `recipe_governance_guard.sh` cannot reach GREEN within 3 iter cycles per SP, halt the SP and escalate to user (likely indicates `applied_recipe:` → `applied_recipes:` multi-value schema migration needed — see Q1).
- **Rollback:** Each SP is one squash-mergeable commit. Revert single SP if downstream issue detected without disturbing prior SPs.
- **No destructive ops:** No `git reset --hard`, no `git push --force`. `recipes/_MANIFEST.yaml deferred_recipes:` entries are MOVED (not deleted before move), preserving git history.

---

## §6 Pre-Mortem (≥3 failure scenarios)

1. **Korean reference WebFetch returns HTTP 403 for primary anchor of multiple recipes.**
   - Likelihood: Medium (R5 Coupang precedent).
   - Impact: Iter cycles balloon if Critic flags low evidence density.
   - Mitigation: Pre-categorization in §4 makes downgrade path explicit BEFORE SP starts. Each recipe carries ≥2 external anchors (Korean + non-Korean), and ≥1 must be verbatim-verified. If only `internal_design` survives for a recipe, that recipe alone is downgraded back to deferred — cluster ships the rest; SP atomicity scoped to "all-or-survivors" per cluster (clarified in SP commit message).
2. **`applied_recipe:` field cannot hold multiple recipe values for a domain (e.g., `crud` is in 6 of 7 R6 recipes + 2 of 3 R5 recipes).**
   - Likelihood: HIGH — current SP37 rule expects a single string value.
   - Impact: `recipe_governance_guard.sh` exits non-zero on every L4 README in R6.
   - Mitigation: Open Question Q1 must resolve BEFORE SP39 starts. Two paths: (a) extend SP37 rule to accept `applied_recipes: [<pattern1>, <pattern2>, ...]` list (preferred, backward-compat); (b) add new rule `business-domain-may-declare-multiple-recipes` with conflict resolution. Path (a) is one targeted edit to `practices/rules/business-domain-must-declare-applied-recipe.md` + `recipe_governance_guard.sh` scanner update — slot in as SP39 prerequisite (pre-commit, no new SP).
3. **Sealed verdict sub-agent for one recipe scores below threshold (e.g., 8/12 MUST).**
   - Likelihood: Low (R5 3-recipe precedent all hit 12/12). Risk concentrates on recipes with weakest external anchors (booking, b2b-admin, internal-it).
   - Impact: SP42 blocks tag.
   - Mitigation: RECIPE.md authoring guidance (R5 standard) prioritizes "context-0 discoverability": clearly enumerate L4 + L2 + L3 + invariants + override paths within ONE file. If a verdict fails, return to the recipe's RECIPE.md and add a missing section (typically "Sub-agent Hint Sheet"). Re-run verdict harness — same SP42 if iter budget allows, else split out as SP43 fast-follow.
4. **Net-new L2 dependency surfaces during SP execution (e.g., `LessonPlayer` not actually substitutable by `markdown-renderer` + recipe guidance).**
   - Likelihood: Low (§4 pre-verified existing L2 catalog covers all 7 recipes).
   - Impact: Recipe cannot ship without new L2; R6 scope creep.
   - Mitigation: Hard rule — if an SP discovers a net-new L2 need, that recipe alone is downgraded back to deferred and the L2 is hinted as a candidate for SP43+ in `recipes/_MANIFEST.yaml#deferred_recipes:` with a new `blocker:` field. Cluster ships the remaining recipes.

---

## §7 ADR Template

Each recipe + the multi-recipe rule extension gets its own ADR in `practices/DECISIONS.md`. Pre-allocated IDs:

- **TD-2026-05-18-016** — Recipe `booking` shipped via composition of `crud + payment + notification + audit-log + feature-flags`; no new L4; Korean anchor downgraded to `internal_design` if WebFetch fails.
- **TD-2026-05-18-017** — Recipe `community` shipped via `crud + notification + search + audit-log`.
- **TD-2026-05-18-018** — Recipe `marketplace` shipped via `crud + payment + search + notification + audit-log + (optional feature-flags for KYC)`.
- **TD-2026-05-18-019** — Recipe `lms` shipped via `crud + file-storage + notification + payment + audit-log`.
- **TD-2026-05-18-020** — Recipe `b2b-admin` shipped via `crud + audit-log + feature-flags + search + auth`.
- **TD-2026-05-18-021** — Recipe `cms` shipped via `crud + audit-log + search + file-storage`.
- **TD-2026-05-18-022** — Recipe `internal-it` shipped via `crud + audit-log + notification + feature-flags`.
- **TD-2026-05-18-023** — `applied_recipe:` field extended to `applied_recipes: [<list>]` to support multi-recipe L4 participation (Q1 resolution path A).
- **TD-2026-05-18-024** — `recipes/_MANIFEST.yaml#deferred_recipes:` emptied; backlog now expressed as new-recipe-proposal commits, not standing deferred list.
- **TD-2026-05-18-025** — Sealed verdict harness extended to 7 recipes (10 total active); harness output remains under `skills/_tests/sealed-verdict/`.

Each ADR populated with: Decision, Drivers, Alternatives considered, Why chosen, Consequences, Follow-ups (final-PRD requirement, R5 standard).

---

## §8 Open Questions

1. **Q1 (BLOCKER for SP39 start): Multi-recipe membership for L4 domains.** The R5 SP37 rule `business-domain-must-declare-applied-recipe` expects a single `applied_recipe: <pattern>` value. With R6 adding 7 recipes — 6 of which include `crud` and 4 of which include `audit-log` — most L4 domains will participate in multiple recipes. Two resolution paths:
   - (a) Extend rule and guard to accept `applied_recipes:` (plural, list value), retain single-value `applied_recipe:` for backward compat. ADR TD-2026-05-18-023 captures this.
   - (b) Pin only ONE primary recipe per L4 and document secondary participation as advisory metadata.
   - **Planner recommendation:** Path (a). Path (b) bleeds composition info into prose-only state, breaking the machine-verifiability invariant.
   - **Owner:** Critic (iter 1) to confirm; Planner to lock in iter 2.
2. **Q2: Korean reference verbatim verification cadence.** Should each accessible Korean URL be re-WebFetched at SP execution time (live), or is pre-categorization in §4 sufficient as evidence-of-intent?
   - **Planner recommendation:** Live WebFetch at SP execution. Capture URL + quote + timestamp + HTTP status to `practices/upstream/r6-sp<N>-evidence-snapshot.md`. If status≠200, downgrade to `internal_design` in the recipe's evidence block — no fabrication.
3. **Q3: Sealed verdict harness fan-out cost in SP42.** 7 sealed sub-agents in one SP is the largest harness load yet. Should SP42 ship in two phases (4 + 3), or stay single-SP?
   - **Planner recommendation:** Single SP42 with parallel sub-agent dispatch where harness supports it. Fall back to sequential within SP42 if parallel infrastructure not ready — wall-time concern only, not correctness.

(Will be written to `.omc/plans/open-questions.md` per planner protocol after PRD approval.)

---

## §9 Honored Constraints

- Tier-1 cap **= 4** (FROZEN).
- Tier-2 count **= 8** (UNCHANGED).
- L4 domain count **= 10** (UNCHANGED — composition kit principle).
- L2 catalog **= 92** (UNCHANGED in R6 — no new blocks).
- L3 catalog **= 20** (UNCHANGED in R6).
- L1 catalog **= 48** (UNCHANGED in R6).
- Spec Trio atomic rule per SP.
- Composition kit framing — recipes COMPOSE, do NOT add L4.
- Korean references — WebFetch-verified verbatim OR `internal_design` (no fabrication).
- Out-of-scope: deployment/CI/release/docs site/new skills/new L2/new L3/new L4/RECIPE_DEVIATION.md ceremony.
- R5 SP37 rule `business-domain-must-declare-applied-recipe` honored AND extended (TD-2026-05-18-023) for multi-recipe membership.
- R5 sealed verdict threshold (≥10/12 MUST + ≥5/8 SHOULD) honored for all 7 R6 recipes.

---

## §10 Out-of-scope (R6 explicit)

- Adding any new L1 / L2 / L3 / L4 surface. If a recipe truly needs one, recipe stays deferred and the surface candidate is logged in `recipes/_MANIFEST.yaml#deferred_recipes:` with `blocker:` field.
- Adding new Tier-1 / Tier-2 skill.
- New enforcement rule families. Only the targeted multi-recipe extension to the SP37 rule (TD-2026-05-18-023) is in-scope, and only if Q1 path (a) selected.
- `RECIPE_DEVIATION.md` governance ceremony (rejected in R5 §1.4).
- `/ax-scaffold business <pattern> --analyze` free-text NLP inference (rejected in R5).
- Backend API ENDPOINT implementations for any recipe. Recipes specify spec_ref bindings; fork-receiver implements code.
- Recipe ordering/priority weighting in `recipes/_MANIFEST.yaml` (manifest is alphabetical or insertion-ordered; no rank field).
- Cross-recipe interaction patterns (e.g., booking↔payment shared invariants). Each recipe is independently composable. Composition-of-recipes is R7+ scope.

---

## §11 Branch + path summary

- **Branch:** `feat/r6-recipes-sp39-sp42` (cut from `main` at `b5f16b4` — `v1.3.0-business-patterns` tag).
- **PRD path:** `docs/superpowers/specs/2026-05-19-r6-recipes-prd.draft.md` (this file).
- **Manifest target:** `recipes/_MANIFEST.yaml` (R6 ends with empty `deferred_recipes:`).
- **Final tag:** `v1.4.0-recipes-complete` (10 active recipes, 0 deferred).

---

## §12 Verdict line

R6 absorbs the 7 R5-deferred recipes via the same atomic Spec-Trio + sealed-verdict pattern that shipped 3 recipes in v1.3.0, with composition-disjoint clustering (3 SPs) plus one finalization SP. Tier counts frozen; no new L1/L2/L3/L4/skill; Korean references pre-categorized; multi-recipe membership rule extension (TD-2026-05-18-023) gates SP39 start.
