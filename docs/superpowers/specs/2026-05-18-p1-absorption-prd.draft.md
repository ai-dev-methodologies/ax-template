# P1 Absorption PRD — 2026-05-18 (DRAFT, ralplan Round 4 iter 1)

> **Status:** DRAFT for `/ralplan` consensus loop (Planner → Architect → Codex Critic). Not yet approved.
> **Date:** 2026-05-18. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-17-frontend-templatization-prd.md` (CLOSED — SP1–SP12, `v1.0.0` baseline).
> - `2026-05-18-catalog-extension-prd.md` (CLOSED — SP13–SP22, tag `v1.0.0-catalog-complete`, commit `9212989`).
> - `2026-05-18-functional-extension-prd.md` (CLOSED — SP23–SP29, tag `v1.1.0-functional-complete`, commit `1ab8f54`).
> - `2026-05-18-component-catalog-completeness-audit.md` (original P0/P1/P2 inventory, 1164 lines).
> - `2026-05-18-component-catalog-completeness-audit-critic.md` (Codex rescope, P0→P1 demotions).
> **Branch (when execution starts):** `feat/p1-absorption-sp30-sp35` (NOT main).

---

## §1 RALPLAN-DR Summary

### Principles (inherited verbatim, not re-litigated)

1. **Composition kit, not single product.** Every new artifact (template / rule / skill / spec) must be fork-adoptable in isolation; no atom is single-application. (CLAUDE.md §Vision)
2. **Spec-before-code, evidence-anchored.** Every new template carries `evidence:` (frontmatter OR `@ax-template-meta`); every new rule carries `protects_template_id` + `failing_fixture_path`; every new ADR declares `provenance_class`. (Catalog Extension §1)
3. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` skill returns exit 0. No "done" on prose. No advisory-only acceptance. (PRD-1 §RALPLAN-DR)
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count is **frozen at 4** (`/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`). New capability ships as either (a) catalog atoms inside the existing 4 verify lanes, or (b) subcommands of `/ax-verify`. (Functional Extension §1)
5. **Atomic Spec-Trio rule.** Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs. Inherited from Catalog Extension §RALPLAN-DR and Functional Extension Critic Blocker 4. Applies to billing-domain SP candidate in this PRD.
6. **No speculative generality.** Every P1 item this PRD absorbs must close (a) a specific fork-receiver gap reported by current users / audit, OR (b) a self-reference (an SP21/SP29-era rule that points to an absent template), OR (c) a Korean enterprise vertical that catalogs broader composition value (사업자등록번호, 휴대폰 본인인증).

### Decision Drivers (top 3)

1. **L4-flow-unblock × frequency-of-use ranking.** The audit P1 list is 74 items long. Most rank low on either L4-unblock (the catalog can ship working forks without them) or frequency-of-use (specialized verticals). This PRD admits only items that score high on BOTH axes. The filter is enforced in §4 Inventory.
2. **Catalog-self-discoverability metric (from Memory file `payment_blueprint_status.md`).** The L4 sealed sub-agent verdict in payment SP (11/11 MUST + 6/6 SHOULD) proved that the catalog is **self-discoverable to a context-0 AI agent**. Any new P1 absorbed must preserve that property — i.e., its `evidence:` block alone must explain where it fits without referencing this PRD. Items that need additional context to discover are deferred.
3. **Korean enterprise specificity.** The audit Critic explicitly preserved 도로명/지번 (P0, shipped in SP14) and Hangul IME (P0, shipped in SP21 + SP26). The remaining P1 Korean specifics — 사업자등록번호 (B2B billing/tax-invoice), 휴대폰 본인인증 (CI/DI, regulated identity) — are vertical-tagged but underpinned by clear regulatory anchors (국세청 algorithm + KISA 본인인증 standard). They earn admission as **opt-in regulatory primitives** with rules that BLOCK unsafe alternatives (e.g., raw RRN collection) — analogous to SP21's `no-rrn-logging`.

### Mode

**DELIBERATE.** Auto-triggered by: (a) cross-stack PII implications (사업자등록번호 / 휴대폰 본인인증), (b) new L4 domain candidate (billing — full_trio), (c) wall-time ≥1 week, (d) ≥3 new failing-fixture rules. Pre-mortem + expanded test plan mandatory (see §7, §8).

### Viable Options Considered (≥2 mandatory)

- **Option A — Catalog-wide P1 sweep (5–7 SPs).** Walk the audit P1 list dimension-by-dimension (A: L1, B: L2, C: L3, D: backend, E: rules, F: skills) and absorb every item that survives the filter in Decision Driver 1.
  - Pros: complete the audit; single bounded scope; clears the "what's left" question.
  - Cons: heterogeneous; weakens atomic-Spec-Trio if billing L4 is sliced across two SPs; mixes Korean specifics with generic primitives.
  - **Rejected.** Sweep without theme dilutes priority signal; risk of accepting items that score high in audit-position but low in user-felt impact.

- **Option B — Billing-domain-first (atomic SP + supporting L2/L1/rules).** Treat billing as the highest-leverage new full_trio L4 domain (pricing-table page, plan-comparison, usage-meter, invoice-list, business-registration validation, KRW invoice render). Build the L2/L1 prerequisites + the L4 domain in one cluster of 3 SPs, then add 1–2 generic-utility SPs for the remaining high-value P1 items (rich text, signature pad if KYC required, advanced filter builder).
  - Pros: produces a fork-visible new domain (4th Spec Trio after auth/crud/payment); matches "next thing a Korean SaaS team needs after payment domain"; aligns with audit P1 #14 (billing/subscription); single atomic ridge SP for billing.
  - Cons: leaves residual P1 items un-absorbed (those go to round-5+); billing requires a payment-provider abstraction that overlaps SP6/SP7 payment domain.
  - **CHOSEN as primary axis, paired with Option C residual cluster.**

- **Option C — Korean enterprise specials cluster (사업자등록번호 + 휴대폰 본인인증 + RRN-protective rules).** A single SP focused on regulated identity / B2B specifics. No new L4 domain; only L1 primitives + 1 new rule cluster + 1 backend service for 본인인증 callback handling.
  - Pros: closes the Korean enterprise hard requirements pending since audit Critic; vertical-honest (regulatory boundaries explicit); composition-kit visible (every Korean SaaS fork picks these up day 1).
  - Cons: vertical specificity may exceed kit scope if RRN handling promotes RRN collection; mitigated by RRN-protective rule that BLOCKS unsafe collection.
  - **CHOSEN as secondary cluster paired with Option B.**

- **Option D — Admin/observability hardening (admin L2 + audit-log advanced features + observability extensions).** Polish-cluster SP focused on fork-receiver day-1 ops surface (impersonation banner, advanced filter builder, activity feed, theme switcher).
  - Pros: low-risk addition; closes audit B.2.12 "Admin" and B.2.7 "Errors/Diagnostics" residuals.
  - Cons: every item is individually small (no atomic theme); risks miscellany-SP anti-pattern.
  - **PARTIAL ADOPT.** Selected items folded into Option B/C SPs where dependency-honest; rest deferred to round-5+.

- **Option E — Defer all P1 (no round-4 SPs).** Wait for fork-receiver feedback to surface concrete P1 priorities.
  - Pros: maximizes evidence quality; avoids speculative absorption.
  - Cons: catalog stalls; current audit P1 items (e.g., business-registration-input, theme-switcher, rich-text-editor) are repeatedly requested by users; further deferral fails Composition Kit Principle 2 ("catalog 확장은 정상 활동").
  - **Rejected.**

### Recommended: **Option B + C hybrid — Billing atomic full_trio domain (3 SPs) + Korean specials cluster (1 SP) + Forms/Rich-Content cluster (1 SP) + Tables/Filters advanced cluster (1 SP) + Admin/Settings polish cluster (1 SP) = 6 SPs (SP30–SP35).**

**Sequencing:**

```
SP30 (Billing Spec Trio + L1/L2 prerequisites: KRW currency-input, pricing-table, plan-comparison, usage-meter, invoice-list)
    ↓
SP31 (Billing L4 + backend domain + rules — atomic Spec-Trio honored: depends on SP30)
    ↓
SP32 (Korean specials: business-registration-input L1 + business-registration-validate rule + 휴대폰 본인인증 panel L2 + verify-phone backend service + no-rrn-collection-without-legal-basis rule)
    ‖
SP33 (Forms/Rich-Content: rich-text-editor L1 dynamic-import + markdown-renderer L1 + signature-pad L1 + L2 form-stepper promote + onboarding-checklist + product-tour)
    ‖
SP34 (Tables/Filters advanced: advanced-filter-builder L2 + saved-view + saved-filters + faceted-filter + tree-table + bulk-export L2)
    ↓
SP35 (Admin/Settings polish: theme-switcher + locale-switcher (verify SP28 wired) + impersonation-banner + maintenance-notice + activity-feed L2 + audit-log-page L3 + admin-overview-page L3)
```

SP32/SP33/SP34 run in **parallel after SP31 lands billing**, then SP35 closes. Total: **6 SPs, ≈ 9–11 d wall-time.**

---

## §2 Context

### Completed cycles (verified disk state, 2026-05-18)

| Cycle | PRD | Tag | Commit | Surfaces touched |
|---|---|---|---|---|
| 1 | `2026-05-17-frontend-templatization-prd.md` | `v1.0.0` | (pre-tag) | SP1–SP12: monorepo scaffolding, L1/L2/L3 baseline, 4 hard gates, /ax-* Tier-1 skills |
| 2 | `2026-05-18-catalog-extension-prd.md` | `v1.0.0-catalog-complete` | `9212989` | SP13–SP22: backend cross-cutting + L1 P0 primitives + 3 atomic domains (notification/audit-log/file-storage) + email-outbox + scheduled-task backend + L3 cluster + P0 rules + /ax-fork-receiver |
| 3 | `2026-05-18-functional-extension-prd.md` | `v1.1.0-functional-complete` | `1ab8f54` | SP23–SP29: observability + cache + data layer + jobs + integration + export/import + charts + search atomic domain + realtime + forms-extended + i18n + feature-flags atomic domain + /ax-verify subcommands (policy-check, evidence-fetch, explain) |

### Current catalog totals (post-`v1.1.0-functional-complete`, verified by disk grep)

| Surface | Count (disk-verified) | Path |
|---|---|---|
| L1 primitives | **42** | `ls templates/L1/components/ \| wc -l` |
| L2 blocks | **64** | `ls templates/L2/blocks/ \| wc -l` |
| L3 page templates | **16** | `ls templates/L3/pages/ -I README.md \| wc -l` |
| L4 domain workloads | **9** (audit-log, auth, crud, feature-flags, file-storage, notification, payment, practices, search) | `ls templates/L4/` |
| Backend cross-cutting + domain | **24** subdirs | `ls templates/backend/` |
| Skills | **19** (Tier-1: 4, Tier-2: 8, Tier-3: 7) | `ls skills/` |
| Java rules | **76** | `ls practices/rules/*.md \| wc -l` |
| React rules | **77** | `ls practices-react/rules/*.md \| wc -l` |
| Spec Trios | **10** (8 full_trio: auth, crud, payment, notification, audit-log, file-storage, search, feature-flags; 1 backend_only: email-outbox + scheduled-task + ratelimit; 1 frontend_only: practices) | `ls specs/` cross-ref `contracts/` `blueprints/` |
| Guards | **19/19 GREEN** (4 hard + trio_integrity + cross_trio + per-domain) | `practices/evals/*_guard.sh` |
| Upstream snapshots | 43+ | `practices/upstream/*.snapshot.md` |

### This PRD's scope

User identified that the audit's P1 list (74 items as of 2026-05-18, after Codex rescope) is **not all equally urgent**. v1.1.0 already absorbed the high-leverage P1 items in F1–F15 (search + feature-flags domains, observability, cache, data, jobs, integration, export/import, charts, realtime, forms-extended, i18n). The **remaining P1** divides into:

1. **High-leverage residuals** — Items repeatedly requested by Korean SaaS fork receivers OR cited by audit as "production-grade fork blocker." This PRD absorbs these (≈ 25 items).
2. **Medium-leverage residuals** — Items the audit listed but lacking a current fork-receiver complaint. Deferred to round-5+.
3. **Low-leverage residuals (P2-adjacent)** — Specialized verticals. Deferred indefinitely; revisit only on first concrete fork ask.

The filter is enforced explicitly in §4. The PRD admits **6 SPs covering ~25 catalog atoms** (full count in §5 surface table).

### What v1.1.0 already absorbed from audit P1 (cross-check, prevent double-absorb)

| Audit P1 item | Status post-v1.1.0 | Evidence |
|---|---|---|
| L2 `feature-flag-toggle.tsx` | DONE | `templates/L2/blocks/feature-flag-toggle.tsx` |
| L2 `feature-gate.tsx` | DONE | `templates/L2/blocks/feature-gate.tsx` |
| L4 `feature-flags` (full_trio) | DONE | `templates/L4/feature-flags/` + Spec Trio |
| L4 `search` (full_trio) | DONE | `templates/L4/search/` + Spec Trio |
| L2 `notification-bell`, `notification-list`, `notification-item` | DONE (SP16) | `templates/L2/blocks/notification-*.tsx` |
| L2 advanced forms `field-array-extended`, `conditional-field-extended`, `form-error-summary-extended`, `form-section-extended` | DONE (SP27) | `templates/L2/blocks/*-extended.tsx` |
| L2 charts cluster (`kpi-card`, `sparkline`, `time-series-chart`, `bar-chart`, `pie-chart`, `heatmap`) | DONE (SP26) | `templates/L2/blocks/*-chart.tsx` |
| L2 `auto-save-indicator`, `dirty-guard`, `dependent-field` | DONE (SP27) | `templates/L2/blocks/*.tsx` |
| L2 `event-stream`, `live-presence`, `optimistic-update` | DONE (SP27) | `templates/L2/blocks/*.tsx` |
| L2 `virtualized-table`, `column-picker`, `bulk-actions-bar` | DONE (SP15) | `templates/L2/blocks/*.tsx` |
| L2 `search-palette`, `typeahead-search`, `recent-searches`, `result-highlighter` | DONE (SP26) | `templates/L2/blocks/*.tsx` |
| L2 `error-boundary`, `offline-banner` | DONE | `templates/L2/blocks/*.tsx` |
| L1 `combobox`, `command`, `popover`, `file-dropzone`, `address-search`, `date-picker`, `date-range-picker`, `otp-input`, `calendar`, `slider`, `progress`, `locale-switcher`, `currency-formatter` (as L1), `relative-time` | DONE (SP14 + SP28) | `templates/L1/components/*.tsx` |
| Backend `observability/` (Metrics, OpenTelemetry, MDC traceId) | DONE (SP23) | `templates/backend/observability/` |
| Backend `cache/`, `data/`, `jobs/`, `integration/`, `import-export/`, `realtime/`, `search/`, `feature-flags/`, `notification/`, `audit-log/`, `file-storage/`, `email-outbox/`, `scheduled-task/` | DONE (SP13/16/17/18/19/23/24/25/26/27/28) | `templates/backend/*/` |
| L3 `forgot-password`, `reset-password`, `mfa-setup`, `account-locked`, `wizard`, `settings-overview`, `import-csv`, `export-job-status`, `search-results-page` | DONE (SP20 + SP26) | `templates/L3/pages/*/` |
| Java rules (76 total, from 64 + SP21/SP24/SP27 additions): `no-rrn-logging`, `idempotency-key-on-mutations`, `traceid-in-error-response`, `chunked-import-required-when-rowcount-gt-1000`, `presigned-url-signature-required`, `cacheable-requires-explicit-ttl` (SP23), etc. | DONE | `practices/rules/*.md` |
| React rules (77 total): `virtualized-table-when-rowcount-gt-1000`, `no-rrn-in-form-fields`, `traceid-propagated-client`, `combobox-respects-hangul-ime-composition`, `no-l4-cross-import`, `l2-prefer-onsubmit-prop`, `l2-prefer-data-prop-over-direct-fetch` (extended) | DONE (SP21) | `practices-react/rules/*.md` |
| `/ax-verify policy-check`, `evidence-fetch`, `explain` subcommands | DONE (SP29) | `skills/ax-verify/scripts/` |

**Net result:** ~50 of audit P1 already shipped in v1.0.0 + v1.1.0. ~25 items remain that warrant absorption in this PRD; the residual ~25 either drop to P2 (low frequency-of-use) or defer to round-5+.

---

## §3 Objectives + Guardrails

### Objectives (one per SP minimum)

- **O1 (SP30 — Billing Foundation):** Make subscription / billing **L4-ready** at the L1/L2 surface so SP31 can land a full_trio billing domain atomically. Ship: L1 `currency-input.tsx` (KRW-first, configurable), `number-input.tsx` (spinner), `range-picker.tsx` (slider range); L2 `pricing-table`, `plan-comparison`, `usage-meter`, `invoice-list`; L3 `pricing-page` template. Spec Trio drafts (`specs/billing-l0.yaml`, `contracts/billing-openapi.yaml`, `blueprints/billing-manifest.yaml`) authored but Spec-Trio integrity test waits for SP31's atomic close.
- **O2 (SP31 — Billing Atomic Domain):** Ship `templates/L4/billing/` as the 9th full_trio L4 domain. Backend: `templates/backend/billing/` with subscription/plan/invoice/billing-event entities + `StripeBillingAdapter` and `TossPaymentsBillingAdapter` (provider abstraction reusing SP6 payment patterns) + `BillingService` + `WebhookBillingReceiver` reusing SP24 integration HMAC. L4 page templates wire SP30 L2 blocks. New rules: `billing-event-idempotent` (Java), `subscription-state-machine-explicit` (Java), `currency-amount-precision-explicit` (Java + React). Spec Trio atomic close at SP31 commit.
- **O3 (SP32 — Korean Specials):** Ship 사업자등록번호 + 휴대폰 본인인증 catalog atoms with regulatory anchors. L1 `business-registration-input.tsx` (XXX-XX-XXXXX mask + 국세청 checksum); L2 `phone-verification-panel.tsx` (KISA 본인인증 callback handler); backend `templates/backend/identity-verification/` (CI/DI handler + audit hook). New rules: `business-registration-checksum-required` (React), `no-rrn-collection-without-legal-basis` (Java + React) — RRN-protective, not RRN-enabling. Anti-pattern fixture: a form that collects raw RRN without `@LegalBasis(law=...)` annotation FAILS.
- **O4 (SP33 — Forms / Rich Content):** Close the rich-content gap that several audit P1 items depend on. L1 `rich-text-editor.tsx` (TipTap thin, dynamic import enforced via existing `bundle-dynamic-imports` rule), `markdown-renderer.tsx` (read-only `<RenderMarkdown>` with sanitizer), `signature-pad.tsx` (canvas-based, signature_pad lib wrapper, dynamic import); L2 `form-stepper.tsx` (promote multi-step shell from SP20 wizard L3), `onboarding-checklist.tsx`, `product-tour.tsx` (shepherd.js dynamic import), `welcome-modal.tsx`. New rule: `rich-content-must-use-dynamic-import` (React, anti-bloat).
- **O5 (SP34 — Tables / Filters Advanced):** Close the table-advanced gap audited as critical for audit-log + admin L4 surfaces. L2 `advanced-filter-builder.tsx` (AND/OR rule schema), `saved-view.tsx`, `saved-filters.tsx`, `faceted-filter.tsx`, `date-range-filter.tsx`, `filter-chips.tsx`, `tree-table.tsx`, `expandable-row.tsx`, `bulk-export.tsx` (CSV/Excel/PDF cluster pairing with SP24 export-job backend), `column-reorder.tsx`. New rule: `saved-view-must-be-url-state-or-server-persisted` (React, prevents localStorage-only patterns that lose state on logout).
- **O6 (SP35 — Admin / Settings Polish):** Ship admin and settings L2/L3 atoms that close fork-receiver day-1 ops UX. L2 `theme-switcher.tsx` (next-themes, light/dark/system), `impersonation-banner.tsx`, `maintenance-notice.tsx`, `network-status-pill.tsx`, `activity-feed.tsx`, `settings-section.tsx`, `keyboard-shortcut-help.tsx`, `skip-link.tsx` (WCAG 2.4.1), `announce-live.tsx` (WCAG 4.1.3); L3 `audit-log-page` (composes virtualized-table + advanced-filter-builder + activity-feed; the L3 audit-log-page template was a P0 in the original audit but was implicitly satisfied by SP17 audit-log L4 — this SP extracts it as a reusable L3 template). New rule: `impersonation-banner-required-when-acting-as-other-user` (React, audit-anchored).

### Guardrails — Must Have

- Every new template (L1/L2/L3/L4/backend) carries `evidence:` frontmatter OR `@ax-template-meta` comment block citing the audit row + external doc (국세청 / KISA / shadcn / TipTap / signature_pad / shepherd.js).
- Every new rule carries `protects_template_id` (specific template id under `templates/`) AND `failing_fixture_path` (specific `practices/evals/fixtures/<rule>/fail_*/` directory). Inherited from SP21 anti-bloat enforcement (Critic Blocker 5).
- **Atomic Spec-Trio rule (CRITICAL for this PRD):** Billing Spec Trio MUST land atomically in SP31 with the L4 domain — billing-l0.yaml + billing-openapi.yaml + billing-manifest.yaml + templates/L4/billing/ + templates/backend/billing/ + new billing rules ALL commit in SP31's single commit cluster. SP30 may author drafts but NOT register them in `practices/evals/trio_integrity_allowlist.yaml` — that registration happens at SP31's atomic close.
- Skill topology cap: **Tier-1 count frozen at 4.** No new Tier-1 skills. Subcommands of `/ax-verify` are acceptable but not anticipated for this PRD scope.
- Tier-2 / Tier-3 budget: zero new skills anticipated; if any SP needs a new sub-skill, it must justify against `/ax-verify` subcommand path first.
- Korean specials (SP32) must include **anti-pattern rules** (RRN-protective). The PRD does NOT ship raw RRN input; it ships a RRN-protective rule that blocks unsafe RRN collection.
- Allowlist additions (`practices/evals/trio_integrity_allowlist.yaml`): `billing: full_trio` (SP31 atomic). **Race-safe append protocol:** rebase against HEAD before commit; `yq`-sorted insertion (per Catalog Extension §6.2).
- Every new failing fixture causes the named guard to exit non-zero with the named error string; the `pass/` sibling fixture exits 0.
- Skill-orchestrated verify only — no raw `npm run xxx` or `./gradlew testXxx` exposed to AI agents as user-facing surface (ADR TD-2026-05-17-007 enforcement).
- All new artifacts are **evidence-anchored** — every L1/L2 block has shadcn/radix/external doc citation; every rule has either `upstream_id` OR `source_type: external` with citation.
- 4-iter ralplan APPROVE pattern (Planner → Architect → Codex Critic → iterate) — minimum 1 iteration before commit.

### Guardrails — Must NOT

- No GitHub Actions workflows added (out of scope; user explicitly forbade public-release prep).
- No LICENSE / CONTRIBUTING / docs-site / release.yml / Dependabot / GitHub Pages files added.
- No major architectural changes: skill topology stays 3-tier; Spec Trio schema unchanged; `ax-template-meta` shape unchanged; Tier-1 count stays at 4.
- No new sibling `BaseEntityWithSubscription` class — billing entities extend the existing SP25 `BaseEntity` with `@SQLDelete` (Functional Extension Critic Blocker 3 inheritance).
- No raw RRN input component — only a RRN-protective rule. Audit Critic explicitly demoted raw RRN input.
- No P2 items absorbed — those defer to round-5+ pending fork-receiver evidence.
- No new MockMvc tests; RestAssured only (Functional Extension Critic Blocker 6 inheritance).
- No `@SpringBootTest` slow tests in new failing-fixture suites — fixture tests use archunit OR static-script OR Vitest (fast).
- No raw billing/payment provider SDK in templates — use thin adapter abstraction (Stripe + Toss Payments) reusing SP6 payment provider pattern.
- No realtime hard requirement for billing — billing webhooks reuse SP24 integration polling-or-webhook pattern.
- No new language support (no Kotlin / Go / Rust additions in this PRD).
- No skill topology overhaul (no Tier-1 cap changes, no skill restructuring).

---

## §4 Filtered P1 Inventory (post-v1.1.0)

> Filter: each item must score ≥3/5 on (a) L4-flow-unblock × (b) frequency-of-use for Korean enterprise SaaS forks. Items below threshold defer to round-5+. Effort: S = ≤4 h, M = 4–8 h, L = 8–16 h, XL = >16 h. "Justification" cites the audit row + the specific fork-receiver gap that breaks today (or the regulatory anchor).

| # | Surface | Layer | Priority | Justification (L4 unblock OR regulatory anchor) | Effort | Ships in SP |
|---|---|---|---|---|---|---|
| P1-01 | `currency-input.tsx` | L1 | P1 | Audit A.2 P1 — ₩ prefix, 3-digit grouping, configurable to USD/JPY. Billing L4 has no working KRW input without this. | S | SP30 |
| P1-02 | `number-input.tsx` (spinner) | L1 | P1 | Audit A.2 P1 — quantity selectors, pagination size, retry budgets. Billing usage-meter needs it. | S | SP30 |
| P1-03 | `range-picker.tsx` (slider range) | L1 | P1 | Audit A.2 P1 — payment amount range filter, plan tier range. Billing plan-comparison uses it. | S | SP30 |
| P1-04 | `pricing-table.tsx` | L2 | P1 | Audit B.2.11 P1 — universal SaaS pricing surface; first thing fork receivers need. | M | SP30 |
| P1-05 | `plan-comparison.tsx` | L2 | P1 | Audit B.2.11 P2→PROMOTE P1 (feature matrix, paired with pricing-table for billing). | M | SP30 |
| P1-06 | `usage-meter.tsx` | L2 | P1 | Audit B.2.11 P1 — quota progress bar; billing-domain hard prereq. | S | SP30 |
| P1-07 | `invoice-list.tsx` | L2 | P1 | Audit B.2.11 P2→PROMOTE P1 (billing-domain hard prereq; composes data-table). | S | SP30 |
| P1-08 | `pricing-page` | L3 | P1 | Audit C.2 P1 — marketing-style pricing page; SSR-friendly. | M | SP30 |
| P1-09 | `templates/L4/billing/` (full_trio) | L4 | P1→PROMOTE | Audit Dimension E P2→PROMOTE P1 (B2B SaaS fork-receivers ship billing on day 7–14; SP6 payment domain is payment-only, no recurring subscription state). | L | SP31 |
| P1-10 | `templates/backend/billing/` (subscription/plan/invoice entities + service + provider adapter) | Backend | P1→PROMOTE | Pair of P1-09 (atomic Spec-Trio). | L | SP31 |
| P1-11 | `specs/billing-l0.yaml` + `contracts/billing-openapi.yaml` + `blueprints/billing-manifest.yaml` | Spec Trio | P1→PROMOTE | Pair of P1-09 (atomic Spec-Trio close). | M | SP31 (atomic) |
| P1-12 | Rule `billing-event-idempotent` (Java) | Rule | P1 | Subscription events MUST be idempotent (avoid double-charge); extends SP21 `idempotency-key-on-mutations` to billing webhooks. | S | SP31 |
| P1-13 | Rule `subscription-state-machine-explicit` (Java) | Rule | P1 | Subscription lifecycle (TRIAL → ACTIVE → PAST_DUE → CANCELLED) MUST be explicit state machine, not boolean fields. | S | SP31 |
| P1-14 | Rule `currency-amount-precision-explicit` (Java + React) | Rule | P1 | Currency amounts MUST be stored as integers in minor units (no BigDecimal-from-string). Failing fixture: `Money(double)` → archunit + ESLint fail. | S | SP31 |
| P1-15 | `business-registration-input.tsx` | L1 | P1 | Audit A.2 P1 — Korean B2B SaaS hard req (사업자등록번호 XXX-XX-XXXXX + 국세청 checksum); regulatory anchor: 국세청 사업자등록증명원 API doc. | S | SP32 |
| P1-16 | `phone-verification-panel.tsx` | L2 | P1 | Audit A.2 P1 (휴대폰 본인인증) — promote to L2 per PRD §4.11 (cross-cutting; data callback via props). Regulatory anchor: KISA 본인인증 가이드. | M | SP32 |
| P1-17 | `templates/backend/identity-verification/` (CI/DI callback handler) | Backend | P1 | Pair of P1-16. Receives CI/DI from PASS/KCB/SCI verification provider via webhook (reuses SP24 HMAC verify). | M | SP32 |
| P1-18 | Rule `business-registration-checksum-required` (React) | Rule | P1 | Frontend form using business-registration-input MUST run checksum before submit. Failing fixture: raw 10-digit input without `validateBizReg()` call. | S | SP32 |
| P1-19 | Rule `no-rrn-collection-without-legal-basis` (Java + React) | Rule | P1 | RRN-protective. Backend: `@PostMapping` with `@RequestBody` containing field matching RRN-shape MUST be annotated with `@LegalBasis(law=...)`. Frontend: any `<input>` with `name="rrn"` MUST be wrapped in `<LegalBasisDeclaration>` component. Anchored to 개인정보보호법 § 24-1. | S | SP32 |
| P1-20 | `rich-text-editor.tsx` (TipTap thin) | L1 | P1→PROMOTE | Audit A.2 P2→PROMOTE P1. Required by notification composer, comments-thread (deferred), audit-event-note. Dynamic-import enforced. | L | SP33 |
| P1-21 | `markdown-renderer.tsx` (read-only) | L1 | P1→PROMOTE | Audit A.2 — pairs with `rich-text-editor`; render-only path. Universal for documentation surfaces. | S | SP33 |
| P1-22 | `signature-pad.tsx` | L1 | P1→PROMOTE | Audit A.2 P2→PROMOTE P1 (Korean enterprise: KYC + 전자결재 surfaces; banks + healthcare verticals). Dynamic-import enforced. | M | SP33 |
| P1-23 | `form-stepper.tsx` | L2 | P1 | Audit B.2.1 P1 — promote multi-step shell from SP20 `wizard` L3 to reusable L2 block. | M | SP33 |
| P1-24 | `onboarding-checklist.tsx` | L2 | P1 | Audit B.2.9 P1 — first-run setup checklist; universal SaaS day-1 surface. | M | SP33 |
| P1-25 | `product-tour.tsx` | L2 | P1→PROMOTE | Audit B.2.9 P2→PROMOTE P1 (shepherd.js wrapper; first-week activation surface for forks). | L | SP33 |
| P1-26 | `welcome-modal.tsx` | L2 | P1 | Audit B.2.9 P2→PROMOTE P1 (first-run welcome dialog; small but universal). | S | SP33 |
| P1-27 | Rule `rich-content-must-use-dynamic-import` (React) | Rule | P1 | Prevents bundling TipTap / signature_pad / shepherd.js in main chunk. Failing fixture: static `import { TipTap } from 'tiptap'` in L1 → ESLint fail. | S | SP33 |
| P1-28 | `advanced-filter-builder.tsx` | L2 | P1 | Audit B.2.3 P0→DEMOTE P1 (per Critic; audit-log L4 already shipped its own filter toolbar). Promote to L2 as reusable. | L | SP34 |
| P1-29 | `saved-view.tsx`, `saved-filters.tsx`, `faceted-filter.tsx`, `date-range-filter.tsx`, `filter-chips.tsx` | L2 | P1 | Audit B.2.2 / B.2.3 P1 cluster — filter UX residual. | M (cluster) | SP34 |
| P1-30 | `tree-table.tsx`, `expandable-row.tsx`, `bulk-export.tsx`, `column-reorder.tsx` | L2 | P1 | Audit B.2.2 P1 cluster — table-advanced residual. | M (cluster) | SP34 |
| P1-31 | Rule `saved-view-must-be-url-state-or-server-persisted` (React) | Rule | P1 | Prevents localStorage-only saved views that lose state on logout. URL state pattern per CLAUDE.md `patterns.md`. | S | SP34 |
| P1-32 | `theme-switcher.tsx` | L2 | P1 | Audit B.2.10 P1 — light/dark/system selector; next-themes wrapper. | S | SP35 |
| P1-33 | `impersonation-banner.tsx` | L2 | P1 | Audit B.2.12 P1 — security-critical "you are viewing as <user>" red banner. | S | SP35 |
| P1-34 | `maintenance-notice.tsx`, `network-status-pill.tsx`, `activity-feed.tsx`, `settings-section.tsx`, `keyboard-shortcut-help.tsx`, `skip-link.tsx`, `announce-live.tsx` | L2 | P1 | Audit B.2.7 / B.2.10 / B.2.13 P1 cluster — admin/settings/a11y residual. | M (cluster) | SP35 |
| P1-35 | `audit-log-page` L3 + `admin-overview-page` L3 | L3 | P1 | Audit C.2 — extract reusable L3 templates from SP17 audit-log L4 + SP20 settings-overview. | M (cluster) | SP35 |
| P1-36 | Rule `impersonation-banner-required-when-acting-as-other-user` (React) | Rule | P1 | Anchored to admin-impersonation pattern; failing fixture: code that calls `assumeUserId()` without rendering `<ImpersonationBanner>` in shell → ESLint fail. | S | SP35 |

**Totals (this PRD absorbs):**

| Surface | Count |
|---|---|
| L1 primitives added | 7 (currency-input, number-input, range-picker, business-registration-input, rich-text-editor, markdown-renderer, signature-pad) |
| L2 blocks added | ~22 (pricing-table, plan-comparison, usage-meter, invoice-list, phone-verification-panel, form-stepper, onboarding-checklist, product-tour, welcome-modal, advanced-filter-builder, saved-view, saved-filters, faceted-filter, date-range-filter, filter-chips, tree-table, expandable-row, bulk-export, column-reorder, theme-switcher, impersonation-banner, maintenance-notice, network-status-pill, activity-feed, settings-section, keyboard-shortcut-help, skip-link, announce-live) — note: actual count slightly higher because some items are clustered. |
| L3 pages added | 3 (pricing-page, audit-log-page, admin-overview-page) |
| L4 domains added | 1 (billing — full_trio) |
| Backend cross-cutting + domain added | 2 (billing + identity-verification) |
| Spec Trios added | 1 (billing) |
| Java rules added | 4 (billing-event-idempotent, subscription-state-machine-explicit, currency-amount-precision-explicit, no-rrn-collection-without-legal-basis) |
| React rules added | 5 (business-registration-checksum-required, no-rrn-collection-without-legal-basis (react half), rich-content-must-use-dynamic-import, saved-view-must-be-url-state-or-server-persisted, impersonation-banner-required-when-acting-as-other-user) |
| Skills added | 0 (Tier-1 cap honored; no new subcommands needed) |
| Upstream snapshots added | 5 (TipTap v2, signature_pad, shepherd.js, 국세청 사업자등록 API, KISA 본인인증 가이드, next-themes) |

**Total catalog atoms added: ~40 across 6 SPs (vs 25 estimated, slightly higher due to cluster items).**

### Deferred to round-5+ (NOT in this PRD scope)

| Item | Audit row | Defer reason |
|---|---|---|
| `tree-view` L1 | A.2 P2 | Low frequency; no current fork-receiver complaint. |
| `mention` L1 | A.2 P2 | Specialized (chat/comments only). |
| `color-picker` L1 | A.2 P2 | Specialized (design tools only). |
| `cropper` L1 | A.2 P2 | Specialized (media tools only). |
| `carousel` L1 | A.2 P2 | Marketing-leaning; ax-template target is app-internal. |
| `toggle`, `toggle-group` L1 | A.2 P2 | Existing `switch.tsx` covers form-binary; toggle button is design preference. |
| `context-menu` L1 | A.2 P2 | Right-click menu; data-table row actions handled by existing dropdown-menu. |
| `drawer` (vaul) L1 | A.2 P2 | Existing `sheet.tsx` covers panel UX; mobile-bottom-drawer is specialized. |
| `rating` L1 | A.2 P2 | Review surfaces specialized. |
| `pin-input` L1 | A.2 P1 (low frequency) | OTP-input covers MFA; PIN distinction is specialized. |
| `time-picker` L1 | A.2 P1 (low frequency) | Scheduled-task admin surface specialized; defer to ops admin add. |
| `phone-input-kr.tsx` L1 | A.2 P1 (low frequency) | Phone-verification-panel (SP32) covers the verification flow; raw phone input is form-mask only and deferrable. |
| `rrn-masked-input.tsx` L1 | A.2 P1 (regulatory) | Audit Critic explicit demote; RRN-protective rule (SP32) covers the safe path. |
| `breadcrumb`, `menubar`, `navigation-menu` L1 | A.2 P1 | `app-header.tsx` (L2) already handles nav; standalone L1 primitives are speculative. |
| `pagination` L1 | A.2 P1 | L2 pagination.tsx already exists and is correct layer per PRD §4.11. |
| `chat-composer`, `comments-thread`, `inbox-list` L2 | B.2.6 P2 | No L4 messaging domain yet; defer until messaging domain demands. |
| `funnel-chart`, `heatmap` extensions L2 | B.2.4 P2 | Specialized analytics. |
| `download-button` L2 | B.2.5 P2 | Composes existing button + progress; thin wrapper not catalog-critical. |
| `image-preview-grid`, `attachment-list` L2 | B.2.5 P1 (low frequency without new domain) | Files L4 already ships file-storage UI; specialized media surfaces defer. |
| `role-editor`, `permission-matrix` L2 | B.2.12 P2 | Admin-RBAC specialized. |
| `inbox-page`, `bulk-edit-page`, `advanced-search-page`, `landing-page` L3 | C.2 P2 | Marketing or specialized surfaces. |
| L4 `messaging` (chat) | E.2 P2 | Specialized; no current fork-receiver demand. |
| L4 `analytics-tracking`, `subscription-management` | E.2 P2 | Subscription-management is absorbed into billing L4 (SP31); analytics-tracking specialized. |

---

## §5 Implementation Plan (SP30–SP35)

### §5.1 SP dependency graph

```
SP30 (Billing L1/L2 prereqs + Spec Trio drafts)
     │
     ▼
SP31 (Billing L4 atomic full_trio — backend + L4 + Spec Trio close + 3 new rules)
     │
     ├─► SP32 (Korean specials — L1 + L2 + backend identity-verification + 2 new rules)
     ├─► SP33 (Rich content + onboarding L2 + 1 new rule)
     └─► SP34 (Tables/Filters advanced cluster + 1 new rule)
                 │
                 ▼
              SP35 (Admin/Settings polish cluster + 2 new L3 + 1 new rule)
```

**Wall-time estimate:** SP30 (2 d) → SP31 (2 d) → SP32 ‖ SP33 ‖ SP34 (3 d parallel) → SP35 (2 d) = **≈ 9 d** if parallelism honored; **≈ 11 d** serial.

### §5.2 SP30 — Billing Foundation (L1/L2 prereqs + Spec Trio drafts)

| Field | Value |
|---|---|
| **Inputs** | Audit rows A.2 (P1-01, P1-02, P1-03), B.2.11 (P1-04 through P1-07), C.2 (P1-08). Billing provider docs: Stripe Billing API + Toss Payments 정기결제 API. |
| **Deliverables** | `templates/L1/components/currency-input.tsx`, `number-input.tsx`, `range-picker.tsx`; `templates/L2/blocks/pricing-table.tsx`, `plan-comparison.tsx`, `usage-meter.tsx`, `invoice-list.tsx`; `templates/L3/pages/pricing-page/{page.tsx, README.md, error.tsx, loading.tsx}`; `specs/billing-l0.yaml.draft`, `contracts/billing-openapi.yaml.draft`, `blueprints/billing-manifest.yaml.draft` (drafts — NOT yet registered in trio_integrity_allowlist). Upstream snapshots: `practices/upstream/stripe-billing-2026-05.snapshot.md`, `practices/upstream/toss-billing-2026-05.snapshot.md`. |
| **Acceptance** | `/ax-verify-L1` exits 0 with 45 L1 components (42 + 3); `/ax-verify-L2` exits 0 with ≥68 L2 blocks (64 + 4); `/ax-verify-L3` exits 0 with 17 L3 templates (16 + 1); Spec Trio drafts pass YAML syntax check but trio_integrity_guard explicitly skips `billing-l0.yaml.draft` files (suffix-based exclusion already supported in SP25). |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L1/components/currency-input.tsx templates/L2/blocks/pricing-table.tsx templates/L3/pages/pricing-page/` |
| **TDD anchor** | Test file: `templates/_tests/billing-prereq.spec.ts`. Assertion: `expect(currencyInput({ currency: 'KRW', value: 1234567 })).toMatchSnapshot('₩1,234,567')`. RED reason: file does not exist at SP30 start. First green command: `npx vitest run templates/_tests/billing-prereq.spec.ts`. |
| **Risks + mitigation** | Risk: currency-input KRW formatting collides with existing `currency-formatter.tsx` (L2 read-only formatter). Mitigation: currency-input is `<input>`-based (controlled), formatter is `<span>`-based (read-only) — disjoint surfaces, both cite the same ISO 4217 evidence. Test: both files compile and Vitest snapshot diff is zero on overlapping cases. |
| **Agent count** | 2 (L1 worker, L2 worker — disjoint files). |
| **Effort** | M (≈ 2 d). |
| **Rollback boundary** | `git revert sp30-*` reverts all SP30 commits; trio_integrity_guard unaffected because billing drafts are suffix-excluded. |

### §5.3 SP31 — Billing Atomic Domain (full_trio close)

| Field | Value |
|---|---|
| **Inputs** | SP30 outputs (L1/L2/L3 prereqs + Spec Trio drafts). SP6 payment domain (reference for provider abstraction). SP24 integration (reference for HMAC webhook). SP25 BaseEntity (extend with `@SQLDelete` per existing pattern). Audit Dimension E P2→P1 promote rows. |
| **Deliverables** | `templates/L4/billing/` (Next.js App Router segment with subscription / plans / invoices pages composing SP30 L2 blocks); `templates/backend/billing/{Subscription.java, Plan.java, Invoice.java, BillingEvent.java, BillingService.java, BillingController.java, BillingAdminController.java, StripeBillingAdapter.java, TossBillingAdapter.java, BillingProvider.java (interface), WebhookBillingReceiver.java, BillingDto.java, SubscriptionStateMachine.java}`; finalize and rename Spec Trio drafts → `specs/billing-l0.yaml`, `contracts/billing-openapi.yaml`, `blueprints/billing-manifest.yaml`; register `billing: full_trio` in `practices/evals/trio_integrity_allowlist.yaml`; new rules: `practices/rules/billing-event-idempotent.md`, `subscription-state-machine-explicit.md`, `currency-amount-precision-explicit.md` (Java + React variants). Upstream snapshots already landed in SP30. |
| **Acceptance** | `/ax-verify-L4 billing` exits 0; `/ax-verify-domain billing` exits 0; trio_integrity_guard exits 0 with `billing` entry; cross_trio_guard exits 0; new failing fixtures (e.g., `fail_billing_event_no_idempotency_key/`) exit non-zero; `pass/` siblings exit 0; all 8 existing entities + 4 new billing entities pass `BaseEntitySoftDeleteArchTest`. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh && bash skills/ax-verify-L4/scripts/run.sh billing && bash skills/ax-verify-domain/scripts/run.sh billing` |
| **TDD anchor** | Test file: `backend/src/test/java/ax/template/billing/BillingFlowIT.java`. Assertion: `whenSubscriptionCreated_thenBillingEventEmitted_andIdempotencyKeyHonored`. RED reason: `BillingService.createSubscription()` not implemented at SP31 start. First green command: `cd backend && ./gradlew testBilling` (after adding `tasks.register("testBilling")` to build.gradle.kts following SP19 scheduled-task pattern). |
| **Risks + mitigation** | Risk 1: Stripe vs Toss provider abstraction leaks provider-specific event types. Mitigation: `BillingProvider` interface emits **canonical** `BillingEvent` (event_type ∈ {SUBSCRIPTION_CREATED, INVOICE_PAID, INVOICE_FAILED, SUBSCRIPTION_CANCELLED}); adapter translates provider-specific payload to canonical. Test: adapter contract test feeds Stripe + Toss sample webhooks → emits identical canonical BillingEvent. Risk 2: subscription state machine cycle (TRIAL → ACTIVE → PAST_DUE → CANCELLED) leaks into multiple services. Mitigation: `SubscriptionStateMachine.java` is the only allowed mutator; archunit guard `OnlyStateMachineMutatesSubscriptionStatus.java`. Risk 3: atomic Spec-Trio commit may fail trio_integrity_guard if `billing-l0.yaml` lands but allowlist append is missing. Mitigation: SP31 ships **one commit** that includes Spec Trio YAML + allowlist append + L4 + backend + rules (verified by Functional Extension SP26/SP28 atomic-commit pattern). |
| **Agent count** | 4 (Spec Trio worker, backend worker, L4 worker, rules worker — sequential SpEC Trio first, then 3 parallel after Spec Trio fully written). |
| **Effort** | L (≈ 2 d). |
| **Rollback boundary** | `git revert sp31-*` reverts SP31 atomic commit; trio_integrity_allowlist auto-reverts (single line removal); no orphan billing entries. |

### §5.4 SP32 — Korean Specials (사업자등록번호 + 휴대폰 본인인증)

| Field | Value |
|---|---|
| **Inputs** | Audit A.2 P1 rows (P1-15, P1-16); A.8 Korean enterprise specifics; Critic explicit Korean specificity verdict (도로명/지번 promoted, RRN demoted, 사업자등록번호 P1, CI/DI P1); 국세청 사업자등록증명원 algorithm doc (https://www.nts.go.kr); KISA 본인인증 가이드라인 doc; 개인정보보호법 § 24-1. |
| **Deliverables** | `templates/L1/components/business-registration-input.tsx`; `templates/L2/blocks/phone-verification-panel.tsx`; `templates/backend/identity-verification/{IdentityVerificationCallbackController.java, IdentityVerificationService.java, IdentityVerificationProvider.java (interface), PassAdapter.java, KcbAdapter.java, IdentityVerificationDto.java}` — provider abstraction reuses SP24 HMAC webhook pattern; new rules: `practices/rules/no-rrn-collection-without-legal-basis.md` (Java half), `practices-react/rules/business-registration-checksum-required.md`, `practices-react/rules/no-rrn-collection-without-legal-basis.md` (React half — pairs with backend rule); upstream snapshots: `practices/upstream/nts-business-reg-2026-05.snapshot.md`, `practices/upstream/kisa-identity-verification-2026-05.snapshot.md`, `practices/upstream/pipa-article-24-2026-05.snapshot.md`. |
| **Acceptance** | `/ax-verify-L1` exits 0; `/ax-verify-L2` exits 0; `/ax-verify-java` exits 0 on `templates/backend/identity-verification/`; `/ax-verify-react` exits 0 on new rules; new failing fixtures (`fail_biz_reg_no_checksum/`, `fail_rrn_no_legal_basis/`) exit non-zero; `pass/` siblings exit 0. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L1/components/business-registration-input.tsx templates/L2/blocks/phone-verification-panel.tsx templates/backend/identity-verification/` |
| **TDD anchor** | Test file: `templates/_tests/business-reg-checksum.spec.ts`. Assertion: `expect(validateBusinessRegistration('123-45-67890')).toBe(true)` for a known-valid mock + `false` for known-invalid. RED reason: `validateBusinessRegistration` not implemented. First green command: `npx vitest run templates/_tests/business-reg-checksum.spec.ts`. **Backend TDD anchor:** `backend/src/test/java/ax/template/identityverification/IdentityVerificationFlowIT.java` — assert that an inbound mock CI/DI webhook with invalid HMAC is rejected with 401; valid HMAC produces a `VerifiedIdentity` row. First green command: `cd backend && ./gradlew testIdentityVerification`. |
| **Risks + mitigation** | Risk 1: business-registration checksum algorithm has multiple variants in the wild (different sources cite slightly different multiplier sequences). Mitigation: snapshot `nts-business-reg-2026-05.snapshot.md` cites the canonical 국세청 algorithm with explicit multiplier `[1,3,7,1,3,7,1,3,5]` and check-digit formula; failing fixture uses official test data from 국세청 사업자등록증명원 sample. Risk 2: RRN-protective rule may be misinterpreted as RRN-enabling. Mitigation: rule docstring explicitly states "this rule does NOT enable RRN collection; it BLOCKS unsafe collection. Forks must add `@LegalBasis(law=...)` annotation only when 개인정보보호법 § 24-1 legal basis exists." Risk 3: CI/DI handler design diverges across PASS / KCB / SCI providers. Mitigation: `IdentityVerificationProvider` interface emits canonical `VerifiedIdentity {ci, di, name, dob, verifiedAt, providerName}`; adapter contract test feeds 3 provider sample payloads. |
| **Agent count** | 2 (L1+L2+rules worker on frontend side, backend worker on identity-verification — disjoint surfaces, parallel-safe). |
| **Effort** | M (≈ 1.5 d). |
| **Rollback boundary** | `git revert sp32-*` reverts SP32 commits; identity-verification backend dir removal does not affect existing auth/payment domains (no shared interfaces); new rules removed cleanly. |

### §5.5 SP33 — Forms / Rich Content

| Field | Value |
|---|---|
| **Inputs** | Audit A.2 P2→P1 promotions (rich-text-editor, signature-pad); B.2.1 P1 (form-stepper); B.2.9 P1/P2→P1 promotions (onboarding-checklist, product-tour, welcome-modal); existing `practices-react/rules/bundle-dynamic-imports.md` (extend); TipTap v2 docs, signature_pad lib docs, shepherd.js docs. |
| **Deliverables** | `templates/L1/components/rich-text-editor.tsx` (TipTap thin), `markdown-renderer.tsx` (rehype-sanitize-based), `signature-pad.tsx`; `templates/L2/blocks/form-stepper.tsx` (extracted from SP20 wizard L3), `onboarding-checklist.tsx`, `product-tour.tsx` (shepherd.js dynamic-import), `welcome-modal.tsx`; new rule `practices-react/rules/rich-content-must-use-dynamic-import.md`; upstream snapshots: `practices-react/upstream/tiptap-v2-2026-05.snapshot.md`, `signature-pad-2026-05.snapshot.md`, `shepherd-js-2026-05.snapshot.md`. |
| **Acceptance** | `/ax-verify-L1` exits 0 with 48 components (45 from SP30 + 3); `/ax-verify-L2` exits 0 with ≥72 L2 blocks; new failing fixture `fail_static_tiptap_import/` exits non-zero (ESLint rule fires); `pass/` sibling exits 0. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L1/components/rich-text-editor.tsx templates/L2/blocks/form-stepper.tsx` |
| **TDD anchor** | Test file: `templates/_tests/rich-content-dynamic-import.spec.ts`. Assertion: `expect(richTextEditorImports).not.toContain('@tiptap/core')` (because import is dynamic, not static). RED reason: scaffolding writes static import; rule fixture triggers ESLint error. First green command: `npx eslint --fix templates/L1/components/rich-text-editor.tsx && npx vitest run`. |
| **Risks + mitigation** | Risk 1: TipTap pulls 200+ kB into main chunk if not lazy-loaded. Mitigation: rule `rich-content-must-use-dynamic-import` BLOCKS static imports; bundle-size guard in `practices-react/evals/bundle-size_guard.sh` (existing) enforces. Risk 2: signature-pad canvas memory leak on unmount. Mitigation: `useEffect` cleanup with `signature_pad.off()` mandated by snapshot citation; Vitest test simulates mount→unmount→GC and asserts no leaked listeners. Risk 3: product-tour shepherd.js DOM coupling may break in Next.js 16 RSC context. Mitigation: explicit `"use client"` directive; SSR test in Playwright. |
| **Agent count** | 2 (L1 worker, L2 worker — disjoint surfaces). |
| **Effort** | L (≈ 2 d). |
| **Rollback boundary** | `git revert sp33-*` reverts SP33 commits; existing forms-extended L2 (SP27) unaffected (different files). |

### §5.6 SP34 — Tables / Filters Advanced

| Field | Value |
|---|---|
| **Inputs** | Audit B.2.2 P0→P1 demotion (Critic), B.2.3 P0→P1 demotion (Critic); existing SP15 `data-table.tsx`, SP15 `virtualized-table.tsx`, SP15 `column-picker.tsx`, SP24 backend export-job; URL-state pattern from CLAUDE.md `web/patterns.md`. |
| **Deliverables** | `templates/L2/blocks/advanced-filter-builder.tsx`, `saved-view.tsx`, `saved-filters.tsx`, `faceted-filter.tsx`, `date-range-filter.tsx`, `filter-chips.tsx`, `tree-table.tsx`, `expandable-row.tsx`, `bulk-export.tsx` (CSV/Excel/PDF cluster), `column-reorder.tsx`; new rule `practices-react/rules/saved-view-must-be-url-state-or-server-persisted.md`; upstream snapshots: `@tanstack/react-table` v8 update, `react-aria-components` for column-reorder dnd. |
| **Acceptance** | `/ax-verify-L2` exits 0 with ≥81 L2 blocks; new failing fixture `fail_saved_view_localstorage_only/` exits non-zero; `pass/` sibling (URL state or server persistence) exits 0. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L2/blocks/advanced-filter-builder.tsx templates/L2/blocks/bulk-export.tsx` |
| **TDD anchor** | Test file: `templates/_tests/saved-view-persistence.spec.ts`. Assertion: `expect(savedView.persistence).toBe('url'|'server')` and `expect(savedView.persistence).not.toBe('localStorage')`. RED reason: scaffolding writes localStorage-based saved-view; rule fixture triggers ESLint error. First green command: `npx eslint --fix templates/L2/blocks/saved-view.tsx && npx vitest run`. |
| **Risks + mitigation** | Risk 1: advanced-filter-builder rule schema (AND/OR with nested operators) may grow into a DSL. Mitigation: rule schema documented in `blueprints/saved-filter-schema.yaml` (NEW supporting artifact, not a Spec Trio); schema-driven test fixtures prove a fixed grammar (5 operators: eq, neq, lt, gt, contains; 2 logical: and, or; no nesting beyond 3 levels). Risk 2: bulk-export CSV/Excel/PDF triplet may need 3 separate libraries → bundle bloat. Mitigation: PDF export via dynamic import (existing rule); CSV via in-house tiny utility (no lib); Excel via dynamic import of `exceljs`. Risk 3: tree-table state explosion at depth >5. Mitigation: API limit of depth 5 with explicit error; failing fixture demos depth 6 → archunit/Vitest fail. |
| **Agent count** | 3 (filters cluster worker, tables-advanced cluster worker, bulk-export worker — disjoint surfaces). |
| **Effort** | L (≈ 2 d). |
| **Rollback boundary** | `git revert sp34-*` reverts SP34 commits; existing data-table / virtualized-table unaffected (different files). |

### §5.7 SP35 — Admin / Settings Polish

| Field | Value |
|---|---|
| **Inputs** | Audit B.2.7 P1 (errors), B.2.10 P1 (settings), B.2.12 P1 (admin), B.2.13 P0 (a11y skip-link, announce-live promoted to P0 in audit, deferred to SP35 because SP15/27 missed them); SP28 i18n (locale-switcher already shipped at L1 but L2 wrapper may be needed); SP17 audit-log L4 (extract L3 page template); SP20 settings-overview L3 (extract admin-overview L3). |
| **Deliverables** | `templates/L2/blocks/theme-switcher.tsx` (next-themes wrapper), `impersonation-banner.tsx`, `maintenance-notice.tsx`, `network-status-pill.tsx`, `activity-feed.tsx`, `settings-section.tsx`, `keyboard-shortcut-help.tsx`, `skip-link.tsx` (WCAG 2.4.1), `announce-live.tsx` (WCAG 4.1.3); `templates/L3/pages/audit-log-page/`, `templates/L3/pages/admin-overview-page/`; new rule `practices-react/rules/impersonation-banner-required-when-acting-as-other-user.md`; upstream snapshot: `next-themes-2026-05.snapshot.md`, `wcag-22-techniques-2026-05.snapshot.md`. |
| **Acceptance** | `/ax-verify-L2` exits 0 with ≥90 L2 blocks; `/ax-verify-L3` exits 0 with 19 L3 templates; new failing fixture `fail_impersonate_no_banner/` exits non-zero; `pass/` sibling exits 0; existing 19 guards remain GREEN. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L2/blocks/impersonation-banner.tsx templates/L3/pages/audit-log-page/` |
| **TDD anchor** | Test file: `templates/_tests/impersonation-banner.spec.ts`. Assertion: `whenAssumeUserIdCalled_thenImpersonationBannerRendered`. RED reason: scaffolding has `assumeUserId()` without banner render → ESLint rule fires. First green command: `npx eslint --fix templates/L2/blocks/impersonation-banner.tsx && npx vitest run`. |
| **Risks + mitigation** | Risk 1: theme-switcher SSR hydration mismatch (server has no theme info on first render). Mitigation: next-themes recommended pattern — server returns `system` placeholder, client hydrates with stored preference; snapshot evidence cites next-themes README. Risk 2: skip-link + announce-live a11y rules may collide with existing components that already handle their own a11y. Mitigation: both ship as **opt-in app-shell composables**; SP35 wires them into `templates/L2/blocks/app-shell.tsx` (in-place edit, not new file); Playwright a11y audit asserts `<a href="#main">` is the first focusable element. Risk 3: impersonation-banner rule may false-positive on test code that calls `assumeUserId()`. Mitigation: rule has explicit `applies_to: paths_excluding_*.test.tsx_and_*.spec.tsx`. |
| **Agent count** | 2 (L2 cluster worker, L3 cluster worker + rule worker — sequential because L3 composes L2). |
| **Effort** | M (≈ 1.5 d). |
| **Rollback boundary** | `git revert sp35-*` reverts SP35 commits; existing app-shell.tsx in-place edits reverted cleanly (single hunk per file). |

### §5.8 Verification Matrix (single authoritative table)

| SP | New artifacts | Owning verify skill | TDD anchor file | First green command | Rollback safety |
|---|---|---|---|---|---|
| SP30 | 3 L1 + 4 L2 + 1 L3 + 3 Spec Trio drafts + 2 snapshots | `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-L3` | `templates/_tests/billing-prereq.spec.ts` | `npx vitest run templates/_tests/billing-prereq.spec.ts` | `git revert sp30-*` clean (drafts suffix-excluded from trio_integrity) |
| SP31 | 1 L4 + 1 backend domain + 1 Spec Trio close + 1 allowlist entry + 4 new rules | `/ax-verify-L4 billing`, `/ax-verify-domain billing`, `/ax-verify-java` | `backend/.../billing/BillingFlowIT.java` | `cd backend && ./gradlew testBilling` | `git revert sp31-*` atomic-clean (single commit cluster) |
| SP32 | 1 L1 + 1 L2 + 1 backend dir + 3 new rules + 3 snapshots | `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-java`, `/ax-verify-react` | `templates/_tests/business-reg-checksum.spec.ts` + `backend/.../identityverification/IdentityVerificationFlowIT.java` | `npx vitest run + cd backend && ./gradlew testIdentityVerification` | `git revert sp32-*` clean (disjoint surfaces) |
| SP33 | 3 L1 + 4 L2 + 1 new rule + 3 snapshots | `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-react` | `templates/_tests/rich-content-dynamic-import.spec.ts` | `npx vitest run + npx eslint --fix` | `git revert sp33-*` clean |
| SP34 | 10 L2 + 1 new rule + 2 snapshots | `/ax-verify-L2`, `/ax-verify-react` | `templates/_tests/saved-view-persistence.spec.ts` | `npx vitest run + npx eslint --fix` | `git revert sp34-*` clean |
| SP35 | 9 L2 + 2 L3 + 1 new rule + 2 snapshots + in-place app-shell.tsx edit | `/ax-verify-L2`, `/ax-verify-L3`, `/ax-verify-react` | `templates/_tests/impersonation-banner.spec.ts` | `npx vitest run + npx eslint --fix` | `git revert sp35-*` clean (in-place edits single-hunk) |

---

## §6 Autonomous Execution Safety

### §6.1 Rollback boundary per SP

Each SP commits as a single squashed commit at SP close (per Functional Extension SP23–SP29 pattern). `git revert <sp-merge-commit>` is the atomic rollback. SP31 (atomic Spec-Trio) is the highest-risk: its commit MUST include trio_integrity_allowlist append + new spec/contract/manifest YAML files + L4 segment + backend dir + new rules in a single atomic commit. If any of those is missing, the commit is rejected by the pre-merge hook (`skills/ax-verify/scripts/atomic-spec-trio-check.sh` — already present from SP26/SP28).

### §6.2 Shared-artifact ownership matrix

| Shared artifact | SP30 | SP31 | SP32 | SP33 | SP34 | SP35 |
|---|---|---|---|---|---|---|
| `templates/L1/components/*` | WRITE (new files only) | — | WRITE (new file: business-registration-input) | WRITE (new files: rich-text-editor, markdown-renderer, signature-pad) | — | — |
| `templates/L2/blocks/*` | WRITE (new files only) | — | WRITE (new file: phone-verification-panel) | WRITE (new files: form-stepper, onboarding-checklist, product-tour, welcome-modal) | WRITE (new files only — 10 blocks) | WRITE (new files + in-place app-shell.tsx) |
| `templates/L3/pages/*` | WRITE (new pricing-page) | — | — | — | — | WRITE (new audit-log-page, admin-overview-page) |
| `templates/L4/*` | — | WRITE (new billing/) | — | — | — | — |
| `templates/backend/*` | — | WRITE (new billing/) | WRITE (new identity-verification/) | — | — | — |
| `specs/*`, `contracts/*`, `blueprints/*` | WRITE (drafts only) | WRITE (finalize billing trio + register) | — | — | — | — |
| `practices/evals/trio_integrity_allowlist.yaml` | — | WRITE (append `billing: full_trio`) | — | — | — | — |
| `practices/rules/*.md` | — | WRITE (3 new) | WRITE (1 new) | — | — | — |
| `practices-react/rules/*.md` | — | WRITE (1 new variant) | WRITE (2 new) | WRITE (1 new) | WRITE (1 new) | WRITE (1 new) |
| `practices/upstream/*.snapshot.md` | WRITE (2 new) | — | WRITE (3 new) | WRITE (3 new) | WRITE (2 new) | WRITE (2 new) |
| `practices-react/upstream/*.snapshot.md` | — | — | — | WRITE (3 new) | — | — |
| `templates/L2/blocks/app-shell.tsx` | — | — | — | — | — | WRITE (in-place edit: insert SkipLink + AnnounceLive) |

**Conflict surfaces:** `app-shell.tsx` is the only in-place edit in this PRD. Sole SP modifying it = SP35 (after SP33/SP34 close). No parallel race.

### §6.3 Stale-state invalidation rule

If any SP fails its `/ax-verify-*` exit-0 acceptance, the SP is reverted and the next SP does NOT begin until the failed SP either: (a) lands successfully on retry, OR (b) is explicitly skipped with a documented Open Question entry in §9. Parallel SPs (SP32/SP33/SP34) all halt if any one of them fails until the failure is resolved.

### §6.4 Halt thresholds

- **Halt on:** 3 consecutive guard failures in any single SP → halt all SPs, escalate to maintainer.
- **Halt on:** trio_integrity_allowlist diff churn (the allowlist file modified by anyone other than SP31's atomic-commit agent) → halt, escalate.
- **Halt on:** new failing fixture passes (`fail_*/` dir exits 0 — that should fail) → halt, escalate (means rule is not actually enforced).
- **Halt on:** Tier-1 count diff (any SP attempts to create `skills/ax-<new-name>/SKILL.md`) → halt, escalate (cap violation).

### §6.5 ESCAPE valve

If after 2 ralplan iterations the PRD still has unresolved Critic blockers, the maintainer may invoke `/ralplan --escape` to lock the current iter as draft-final and proceed to execution with documented residual risk in §9.

### §6.6 Cross-stack dependency declaration

SP31 (billing L4) **does not depend on** SP6 payment domain code beyond pattern reuse. SP32 (identity-verification) **does not depend on** SP31 billing. SP33/SP34/SP35 are independent of SP31/SP32. The only hard sequence is SP30 → SP31 (billing prereqs must land before billing atomic close).

---

## §7 Pre-mortem (DELIBERATE mode — 3+ scenarios)

### Scenario 1 — Billing provider abstraction leaks Stripe-specific event types

**Failure mode:** SP31 ships `BillingProvider` interface but the canonical `BillingEvent` accidentally carries Stripe-specific fields (e.g., `stripe_subscription_id` instead of opaque `external_id`). Forks using Toss Payments adapter discover the leak only after running their first billing webhook in staging. Composition kit invariant violated: not provider-agnostic.

**Detection:** Adapter contract test `BillingProviderContractTest` MUST verify both Stripe sample webhook AND Toss Payments sample webhook produce **identical** canonical BillingEvent shape (no Stripe-only or Toss-only fields). Test runs in SP31 acceptance.

**Mitigation (executable):**
1. `BillingEvent.java` is a sealed record with exhaustive fields: `eventId (canonical UUID), externalEventId (opaque string), type (enum), occurredAt, subscriptionId (canonical), invoiceId (nullable), amountInMinorUnits, currencyCode, metadata (Map<String,String>)`.
2. Both adapters MUST translate provider payload to this shape; provider-specific data lives only in `metadata` map.
3. Contract test asserts `assertEquals(stripeEvent.toCanonical().withoutMetadata(), tossEvent.toCanonical().withoutMetadata())` for equivalent semantic events.

**Threshold:** If contract test fails in SP31 acceptance, halt and refactor `BillingEvent` shape.

### Scenario 2 — 사업자등록번호 checksum algorithm has multiple valid variants

**Failure mode:** SP32 ships `validateBusinessRegistration` based on one cited 국세청 algorithm, but production forks discover that some valid Korean business registration numbers fail validation because the actual checksum has corner cases (e.g., handling of 5th-digit special pattern for foreign-invested entities). Korean enterprise fork-receivers report false negatives.

**Detection:** Snapshot `nts-business-reg-2026-05.snapshot.md` MUST include the canonical algorithm AND a test data set of 100+ real-world valid + invalid business registration numbers from open 국세청 sources. Failing fixture and `pass/` fixture use this dataset.

**Mitigation (executable):**
1. SP32 deliverable includes `templates/_tests/business-reg-fixtures.json` — 100+ real samples from public 사업자등록증명원 dataset.
2. Unit test runs all 100+ through `validateBusinessRegistration`; expected outputs are encoded.
3. If any sample is misclassified, the algorithm is wrong — SP32 halts.
4. Snapshot URL pinned to `https://www.nts.go.kr/...` with quoted multiplier sequence.

**Threshold:** ≥1 known-valid sample misclassified → halt SP32.

### Scenario 3 — TipTap dynamic import breaks Next.js 16 RSC

**Failure mode:** SP33 ships `rich-text-editor.tsx` with `dynamic import` per the rule, but Next.js 16 App Router with Cache Components has known issues with `next/dynamic` in client-only contexts. The component crashes on first render in production.

**Detection:** Playwright integration test runs in CI against a Next.js 16 build; navigates to a page containing `<RichTextEditor>` and asserts the editor mounts within 3s without console errors.

**Mitigation (executable):**
1. Snapshot `tiptap-v2-2026-05.snapshot.md` MUST include the Next.js 16 RSC integration pattern (currently `"use client"` + `useEffect` + lazy state).
2. Component file uses explicit `"use client"` directive at top.
3. SSR fallback renders a `<textarea>` placeholder until client hydrates.
4. Playwright assertion: editor reachable within 3s; no console error.

**Threshold:** If Playwright assertion fails, halt SP33 and refactor.

### Scenario 4 — RRN-protective rule false-positives on legitimate identity-verification flows

**Failure mode:** SP32 ships `no-rrn-collection-without-legal-basis` rule. The rule triggers on any field named `rrn`, `주민번호`, `id_number`, etc. SP31 billing or SP6 payment legitimately collect identity info via PASS/KCB CI/DI (which produces CI, not raw RRN). The rule incorrectly flags `templates/backend/identity-verification/IdentityVerificationDto.java` because the DTO has a field named `verifiedIdentityNumber`.

**Detection:** SP32 ships **2 fixtures**: (a) `fail_rrn_no_legal_basis/` — raw RRN collection in a form, no legal basis annotation → rule fires; (b) `pass_ci_di_verified/` — CI/DI from identity-verification, no raw RRN → rule does NOT fire. If `pass_ci_di_verified/` triggers the rule, the rule pattern is too broad.

**Mitigation (executable):**
1. Rule matcher uses explicit pattern: field name MUST match `/^(rrn|주민(등록)?번호|id_number|residentRegistrationNumber)$/` AND `@LegalBasis` annotation MUST be missing.
2. Field names `ci`, `di`, `verifiedIdentityNumber`, `external_id` are explicitly excluded (they are post-CI/DI, not raw RRN).
3. Fixture `pass_ci_di_verified/` covers the identity-verification DTO; expected: rule does NOT fire.

**Threshold:** If `pass_ci_di_verified/` triggers the rule, SP32 halts; the rule pattern is refined.

### Scenario 5 — SP31 atomic commit fails because trio_integrity_allowlist conflict

**Failure mode:** SP31 attempts to commit `billing: full_trio` to `practices/evals/trio_integrity_allowlist.yaml` but a concurrent operation (e.g., maintainer manually editing the file) creates a conflict. Atomic SP31 commit fails; partial state left on disk: billing L4 + backend exist but allowlist entry is missing. trio_integrity_guard exits non-zero on next run.

**Detection:** Pre-merge hook `skills/ax-verify/scripts/atomic-spec-trio-check.sh` (existing from SP26) verifies that ALL of: spec YAML + contract YAML + manifest YAML + L4 dir + backend dir + allowlist entry + new rules are present in the staged commit. If any is missing, commit is rejected.

**Mitigation (executable):**
1. SP31 agent rebases against HEAD before committing (per Catalog Extension §6.2 race-safe protocol).
2. `yq` sorted-insertion ensures deterministic allowlist position.
3. Pre-merge hook (existing) blocks partial atomic commits.
4. If conflict detected, SP31 agent halts, retries rebase, reapplies all SP31 artifacts as one commit cluster.

**Threshold:** If 2 rebase-retries fail, SP31 halts and maintainer intervenes.

---

## §8 ADR Template — TD-2026-05-19-NNN

This PRD's APPROVED state will spawn ADRs (one per SP), authored at the SP close commit:

| ADR | Topic | Decision (placeholder) |
|---|---|---|
| TD-2026-05-19-001 | Billing as 4th provider-abstracted L4 domain | Adopt Stripe + Toss Payments adapter pattern reusing SP6 payment provider abstraction; canonical BillingEvent shape. |
| TD-2026-05-19-002 | SubscriptionStateMachine sole mutator | Adopt explicit state machine, archunit-guarded; no other class may mutate Subscription.status. |
| TD-2026-05-19-003 | currency-amount-precision integer-only | Adopt integer minor-unit storage; no BigDecimal-from-string. |
| TD-2026-05-19-004 | RRN-protective rule (anti-pattern) | Adopt `no-rrn-collection-without-legal-basis` as BLOCK rule with explicit field-name pattern + CI/DI exclusion. |
| TD-2026-05-19-005 | rich-content dynamic-import mandate | Adopt `rich-content-must-use-dynamic-import` ESLint rule; static imports of TipTap / signature_pad / shepherd.js BLOCKED in L1/L2. |
| TD-2026-05-19-006 | saved-view URL-state-or-server | Adopt `saved-view-must-be-url-state-or-server-persisted` rule; localStorage-only BLOCKED. |
| TD-2026-05-19-007 | impersonation-banner mandate | Adopt `impersonation-banner-required-when-acting-as-other-user` rule; assumeUserId() without `<ImpersonationBanner>` BLOCKED. |
| TD-2026-05-19-008 | a11y skip-link + announce-live as opt-in app-shell composables | Adopt SP35 in-place edit to `app-shell.tsx`; WCAG 2.4.1 + 4.1.3 enforced via Playwright a11y audit. |

Each ADR will follow the SP21/SP27 pattern: Decision, Drivers, Alternatives considered, Why chosen, Consequences, Follow-ups.

---

## §9 Open Questions

1. **Q1 — 본인인증 provider concrete adapter scope.** SP32 ships PASS + KCB stubs as `IdentityVerificationProvider` implementations. Should SP32 also ship SCI (NICE 신용평가) adapter, or defer to first concrete fork ask? **Recommendation:** ship PASS + KCB only (2 adapters prove the abstraction); SCI adapter deferred. **Risk:** if Korean fork-receivers report SCI is their primary provider, round-5 must add it. Track in `.omc/plans/open-questions.md`.

2. **Q2 — Billing recurring task scheduling.** Subscription renewals require recurring async jobs. Should SP31 reuse SP19 scheduled-task pattern (cron-based) OR SP25 job-dispatcher pattern (on-demand triggered by webhook)? **Recommendation:** webhook-triggered (provider-driven, no internal cron); fallback cron only if webhook fails. Decision deferred to SP31 architect review.

3. **Q3 — Theme switcher SSR strategy.** SP35 next-themes wrapper has known SSR hydration mismatch. Should `theme-switcher.tsx` opt out of SSR via `"use client"` + `useEffect` mount-gate, OR use Cookie-based theme storage with server-side read? **Recommendation:** Cookie-based (Next.js 16 idiomatic for App Router); snapshot evidence will cite next-themes Cookie pattern. Decision deferred to SP35.

---

## §10 Honored Constraints (cross-check vs CLAUDE.md + prior PRDs)

| Constraint | Source | This PRD's compliance |
|---|---|---|
| Composition-kit framing (no single-product slippage) | CLAUDE.md §Vision Principle 3 | All 6 SPs ship catalog atoms; no SP gates a single application. ✓ |
| Java + React equal partners | CLAUDE.md §Vision Principle 1 | SP30 (frontend), SP31 (full-stack), SP32 (full-stack), SP33 (frontend), SP34 (frontend), SP35 (frontend) — frontend-heavy but SP31/SP32 maintain Java template equity. ✓ |
| Catalog expansion is normal | CLAUDE.md §Vision Principle 2 | +40 catalog atoms; no "stop adding rules" framing. ✓ |
| Spec-before-code | Functional Ext §1 P2 | SP30 ships drafts; SP31 atomic close with full Spec Trio. ✓ |
| Evidence-anchored | Catalog Ext §1 P2 | Every new template/rule has snapshot citation. ✓ |
| Binary verification | PRD-1 §1 P3 | Every SP terminates on `/ax-verify-* exit 0`. ✓ |
| Tier-1 cap = 4 | Functional Ext §1 P4 | Zero new Tier-1 skills. ✓ |
| Atomic Spec-Trio | Functional Ext Critic Blocker 4 | SP30 drafts only; SP31 atomic close. ✓ |
| No GitHub Actions workflows | Functional Ext §11 | Out of scope this PRD too. ✓ |
| No LICENSE/CONTRIBUTING/docs-site | Functional Ext §11 | Out of scope this PRD too. ✓ |
| No major architectural changes | This PRD §3 Must NOT | No skill-topology overhaul; Spec Trio schema unchanged. ✓ |
| No raw RRN input | Audit Critic verdict | SP32 ships RRN-protective rule only; no `rrn-masked-input.tsx`. ✓ |
| No new MockMvc tests | Functional Ext Critic Blocker 6 | All new IT tests use RestAssured. ✓ |
| No `@SpringBootTest` slow tests in fixtures | Functional Ext §3 Must NOT | All fixture tests use archunit / static-script / Vitest. ✓ |
| Korean enterprise specificity (도로명/지번 promoted, RRN demoted, 사업자등록번호 P1, IME P0) | Audit Critic verdict | SP14 + SP21 (already done); SP32 absorbs 사업자등록번호 + 본인인증; SP32 RRN-protective rule honors demote. ✓ |
| 4-iter ralplan APPROVED PRD pattern | Functional Ext consensus | This PRD targets minimum 1 ralplan iteration (Planner → Architect → Critic) before commit; up to 4 iterations as needed. ✓ |
| No new MockMvc; RestAssured only | Functional Ext §3 Must NOT | Compliant. ✓ |
| `applies_to: paths_created_after_2026-05-18` rule scoping | Functional Ext Critic Blocker 4 | New rules in SP32 + SP33 + SP34 + SP35 inherit the same scoping pattern; no retroactive enforcement on pre-2026-05-18 files. ✓ |
| Race-safe trio_integrity_allowlist append | Catalog Ext §6.2 | SP31 rebase-before-commit; yq-sorted insertion. ✓ |
| No new sibling BaseEntity class | Functional Ext Critic Blocker 3 | New billing entities extend existing BaseEntity with `@SQLDelete` (in-place pattern). ✓ |

---

## §11 Out-of-Scope (explicit)

- GitHub Actions workflows (`/.github/workflows/`)
- LICENSE / CONTRIBUTING.md / CODE_OF_CONDUCT.md / SECURITY.md
- Public docs site / GitHub Pages / README hero rewrite for public release
- Dependabot config / release.yml / npm publish workflow
- v2 architectural changes: skill topology, Spec Trio schema, ax-template-meta shape, Tier-1 cap changes
- P2 catalog items (deferred to round-5+ pending fork-receiver evidence): tree-view, mention, color-picker, cropper, carousel, toggle, context-menu, drawer (vaul), rating, pin-input, time-picker, raw phone-input-kr, raw rrn-masked-input, breadcrumb, menubar, navigation-menu, L1 pagination, chat-composer, comments-thread, inbox-list, funnel-chart, heatmap extensions, download-button, image-preview-grid, attachment-list, role-editor, permission-matrix, inbox-page, bulk-edit-page, advanced-search-page, landing-page, L4 messaging, L4 analytics-tracking
- L4 subscription-management as separate domain (absorbed into billing L4 in SP31)
- New language support (Kotlin / Go / Rust)
- /react-best-practices ESLint plugin npm publish (separate workstream)

---

## §12 ADR-ready commit content (for Step 6 if APPROVE)

(Placeholders — final ADR text drafted at each SP close.)

- **Decision:** Approve P1 Absorption PRD as canonical for Round 4 / SP30–SP35. Billing 4th full_trio L4 domain + Korean specials + Forms/Rich-Content + Tables/Filters advanced + Admin/Settings polish.
- **Drivers:** Close audit P1 residuals filtered by L4-unblock × frequency-of-use; preserve composition-kit self-discoverability metric; honor atomic Spec-Trio rule; preserve Tier-1 cap; honor RRN-protective Critic verdict.
- **Alternatives considered:** Option A (catalog-wide sweep); Option C (Korean-only); Option D (admin-only polish); Option E (defer-all-P1) — all rejected with documented rationale.
- **Why chosen:** Option B+C hybrid produces a fork-visible new L4 domain (billing) + the Korean specials cluster + targeted polish without speculative absorption; 6 SPs is the right granularity (mirror of Functional Extension 7 SPs); atomic Spec-Trio for billing ridge.
- **Consequences:** +40 catalog atoms (~7 L1, ~22 L2, ~3 L3, ~1 L4, ~2 backend dirs, ~1 Spec Trio, ~9 rules total Java+React); 6 SPs over ≈ 9–11 d wall-time; v1.2.0-p1-absorbed tag at SP35 close; remaining P1 deferred to round-5+ pending fork-receiver evidence; Tier-1 count stays at 4.
- **Follow-ups:** Promote `/ax-verify policy-check` to highlight new rules; refresh `practices/upstream/_MANIFEST.yaml` with new snapshots; re-baseline AGENTS.md sha256.

---

## §13 Iteration log

- **Iter 1 (this draft, 2026-05-18):** Planner authored, awaiting Architect + Codex Critic review. Open Questions: 3 (§9). Expected iter 2 blockers: (a) billing provider abstraction completeness, (b) RRN-protective rule pattern breadth, (c) atomic Spec-Trio sequencing for SP31.

---

## §14 End of PRD draft

**Line count target:** ~900 lines (this draft).
**SP count:** 6 (SP30–SP35).
**Branch (when execution starts):** `feat/p1-absorption-sp30-sp35`.
**Next step:** Submit to `/ralplan` Round 4 iter 1 — Architect + Codex Critic review.
