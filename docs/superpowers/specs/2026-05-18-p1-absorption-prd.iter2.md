# P1 Absorption PRD — 2026-05-18 (iter 2, ralplan Round 4)

> **Status:** ITER 2 revision for `/ralplan` consensus loop. Applies Codex Critic iter-1 hard blockers (6) + independent steelman (1).
> **Date:** 2026-05-18. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-17-frontend-templatization-prd.md` (CLOSED — SP1–SP12, `v1.0.0` baseline).
> - `2026-05-18-catalog-extension-prd.md` (CLOSED — SP13–SP22, tag `v1.0.0-catalog-complete`, commit `9212989`).
> - `2026-05-18-functional-extension-prd.md` (CLOSED — SP23–SP29, tag `v1.1.0-functional-complete`, commit `1ab8f54`).
> - `2026-05-18-component-catalog-completeness-audit.md` (original P0/P1/P2 inventory).
> - `2026-05-18-component-catalog-completeness-audit-critic.md` (Codex rescope).
> - `2026-05-18-p1-absorption-architect-review.md` (Architect iter 1 — ITERATE).
> - `2026-05-18-p1-absorption-critic-codex-iter1.md` (Codex Critic iter 1 — ITERATE; 6 hard blockers + 1 steelman).
> **Branch (when execution starts):** `feat/p1-absorption-sp30-sp34` (renumbered — 5 SPs, NOT main).

---

## §1 RALPLAN-DR Summary

### Principles (inherited verbatim, not re-litigated)

1. **Composition kit, not single product.** Every new artifact (template / rule / skill / spec) must be fork-adoptable in isolation; no atom is single-application. (CLAUDE.md §Vision)
2. **Spec-before-code, evidence-anchored.** Every new template carries `evidence:` (frontmatter OR `@ax-template-meta`); every new rule carries `protects_template_id` + `failing_fixture_path`; every new ADR declares `provenance_class`. (Catalog Extension §1)
3. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` skill returns exit 0. No "done" on prose. No advisory-only acceptance. (PRD-1 §RALPLAN-DR)
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count is **frozen at 4** (`/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`). New capability ships as either (a) catalog atoms inside the existing 4 verify lanes, or (b) subcommands of `/ax-verify`. (Functional Extension §1)
5. **Atomic Spec-Trio rule.** Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs. **Iter-2 strict reading (Critic Blocker 1):** a `.draft` suffix is NOT a landed spec; any SP that ships billing-shaped L1/L2/L3 atoms must also ship the billing Spec Trio in the same atomic commit. Inherited from Catalog Extension §RALPLAN-DR (SP16/SP17/SP18/SP19) and Functional Extension SP26 (Search) / SP28 (feature-flags) atomic precedent.
6. **No speculative generality.** Every P1 item this PRD absorbs must close (a) a specific fork-receiver gap reported by current users / audit, OR (b) a self-reference (an SP21/SP29-era rule that points to an absent template), OR (c) a Korean enterprise vertical that catalogs broader composition value (사업자등록번호, 휴대폰 본인인증).

### Decision Drivers (top 3)

1. **L4-flow-unblock × frequency-of-use ranking.** The audit P1 list is 74 items long. Most rank low on either L4-unblock or frequency-of-use. This PRD admits only items that score high on BOTH axes. The filter is enforced in §4 Inventory.
2. **Catalog-self-discoverability metric (from Memory file `payment_blueprint_status.md`).** The L4 sealed sub-agent verdict in payment SP (11/11 MUST + 6/6 SHOULD) proved the catalog is **self-discoverable to a context-0 AI agent**. Any new P1 absorbed must preserve that property.
3. **Korean enterprise specificity.** The audit Critic explicitly preserved 도로명/지번 (P0, shipped in SP14) and Hangul IME (P0, shipped in SP21 + SP26). The remaining P1 Korean specifics — 사업자등록번호 (B2B billing/tax-invoice), 휴대폰 본인인증 (CI/DI, regulated identity) — are vertical-tagged but underpinned by clear regulatory anchors (국세청 algorithm + KISA 본인인증 standard). They earn admission as **opt-in regulatory primitives** with rules that BLOCK unsafe alternatives.

### Mode

**DELIBERATE.** Auto-triggered by: (a) cross-stack PII implications (사업자등록번호 / 휴대폰 본인인증), (b) new L4 domain candidate (billing — full_trio), (c) wall-time ≥1 week, (d) ≥4 new failing-fixture rules, (e) new backend_only domain (identity-verification). Pre-mortem (≥5 scenarios) + expanded test plan + observability_signal mandatory (see §5.8, §7, §8).

### Viable Options Considered (≥2 mandatory)

- **Option A — Catalog-wide P1 sweep (5–7 SPs, no theme).** Walk the audit P1 list dimension-by-dimension and absorb every item that survives the filter.
  - Pros: complete the audit; clears "what's left."
  - Cons: heterogeneous; weakens atomic-Spec-Trio if billing L4 is sliced; mixes Korean specifics with generic primitives.
  - **Rejected.** Sweep without theme dilutes priority signal.

- **Option B — Billing-domain-first atomic SP + supporting clusters.** Treat billing as the highest-leverage new full_trio L4 domain. Build the **atomic** billing Spec Trio + L4 + backend + L1/L2/L3 prereqs + 4 new rules in ONE SP (per SP26 Search precedent). Then add residual P1 clusters (Korean specials, forms/rich-content, tables/filters, admin polish) in 4 follow-on SPs.
  - Pros: produces a fork-visible 4th full_trio L4 domain atomically; aligns with audit P1 #14 (billing/subscription); matches "next thing a Korean SaaS team needs after payment domain"; honors atomic-Spec-Trio strict reading.
  - Cons: SP30 carries higher single-SP scope (~12 deliverables); requires careful agent-count budgeting.
  - **CHOSEN as primary axis, paired with Option C/D residual clusters.**

- **Option C — Korean enterprise specials cluster (사업자등록번호 + 휴대폰 본인인증 + RRN-protective rules).** L1 primitives + L2 + backend service for 본인인증 callback handling. **Iter-2 addition (Critic Blocker 2):** the backend `identity-verification/` directory MUST register as `backend_only` Spec Trio with `specs/identity-verification-l0.yaml` + `contracts/identity-verification-openapi.yaml` + `blueprints/identity-verification-manifest.yaml`, mirroring the `email-outbox` / `scheduled-task` / `ratelimit` precedent.
  - Pros: closes Korean enterprise hard requirements; vertical-honest; composition-kit visible (every Korean SaaS fork picks these up day 1).
  - Cons: vertical specificity may exceed kit scope if RRN handling promotes RRN collection; mitigated by RRN-protective rule that BLOCKS unsafe collection.
  - **CHOSEN as secondary cluster paired with Option B.**

- **Option D — Admin/observability hardening (admin L2 + audit-log advanced features + observability extensions).** Polish-cluster SP focused on fork-receiver day-1 ops surface.
  - Pros: low-risk; closes audit B.2.12 "Admin" + B.2.7 "Errors/Diagnostics" residuals.
  - Cons: every item is individually small (no atomic theme).
  - **PARTIAL ADOPT.** Selected items folded into SP34 (admin polish); rest deferred.

- **Option E — Defer all P1 (no round-4 SPs).** Wait for fork-receiver feedback.
  - Pros: maximizes evidence quality.
  - Cons: catalog stalls; current audit P1 items repeatedly requested.
  - **Rejected.**

- **Option F (NEW — Critic Soft Suggestion 1) — Strict atomic billing SP + defer all polish clusters.** SP30 ships atomic billing full_trio ONLY. All other clusters (Korean specials, forms/rich-content, tables/filters, admin polish) defer to round-5+ pending fork-receiver evidence.
  - Pros: maximum atomic discipline; minimum speculative absorption; every follow-on SP gated by real fork-receiver complaint.
  - Cons: leaves 사업자등록번호 + 휴대폰 본인인증 (regulatory anchor, already-justified Korean fork-receiver gap) un-absorbed for ≥1 more cycle; the Korean specials in particular have explicit regulatory anchors (국세청, KISA) not subject to fork-receiver-evidence gating.
  - **Rejected** (explicitly): Korean specials cluster has regulatory-anchor evidence (국세청 사업자등록증명원 API doc + KISA 본인인증 가이드라인 + 개인정보보호법 § 24-1) that exceeds the fork-receiver-evidence threshold. Forms/Tables/Admin clusters individually pass the L4-unblock × frequency filter (rich-text-editor unblocks notification composer + comments; saved-view unblocks audit-log + admin L3 pages; impersonation-banner is security-critical). Deferring them after explicit audit-Critic P1 admission would create a 2-cycle absorption pipeline without principled benefit.

### Recommended: **Option B + C hybrid — Billing atomic full_trio domain (1 SP, SP30) + Korean specials cluster (1 SP, SP31) + Forms/Rich-Content cluster (1 SP, SP32) + Tables/Filters advanced cluster (1 SP, SP33) + Admin/Settings polish cluster (1 SP, SP34) = 5 SPs (SP30–SP34).**

**SP count: 5 (down from iter-1's 6).** Rationale: Critic Blocker 1 collapsed iter-1 SP30 (billing prereqs + drafts) + SP31 (billing atomic close) into single atomic SP30. The atomic-billing SP carries higher agent count (5 workers) but ships in one commit cluster per SP26 Search precedent. SP31–SP34 are renumbered (was SP32–SP35 in iter-1).

**Sequencing:**

```
SP30 (Billing atomic full_trio — 1 commit cluster: Spec Trio + 6 backend templates + 5 L2 + L4 + allowlist append + 4 new rules + L1 prereqs + L3 pricing-page)
    ↓
SP31 (Korean specials — backend_only identity-verification Spec Trio + L1 사업자등록번호 + L2 휴대폰 본인인증 + backend identity-verification + 3 new rules + allowlist append)
    ‖
SP32 (Forms/Rich-Content — TipTap + signature-pad + form-stepper + onboarding L2 + 1 new rule)
    ‖
SP33 (Tables/Filters advanced — 10 L2 blocks + 1 new rule)
    ↓
SP34 (Admin/Settings polish — 9 L2 + 2 L3 + 1 new rule)
```

SP31/SP32/SP33 run in **parallel after SP30 lands billing**, then SP34 closes. Total: **5 SPs, ≈ 9–11 d wall-time.**

---

## §2 Context

### Completed cycles (verified disk state, 2026-05-18)

| Cycle | PRD | Tag | Commit | Surfaces touched |
|---|---|---|---|---|
| 1 | `2026-05-17-frontend-templatization-prd.md` | `v1.0.0` | (pre-tag) | SP1–SP12: monorepo scaffolding, L1/L2/L3 baseline, 4 hard gates, /ax-* Tier-1 skills |
| 2 | `2026-05-18-catalog-extension-prd.md` | `v1.0.0-catalog-complete` | `9212989` | SP13–SP22: backend cross-cutting + L1 P0 primitives + 3 atomic domains (notification/audit-log/file-storage) + email-outbox + scheduled-task backend + L3 cluster + P0 rules + /ax-fork-receiver |
| 3 | `2026-05-18-functional-extension-prd.md` | `v1.1.0-functional-complete` | `1ab8f54` | SP23–SP29: observability + cache + data layer + jobs + integration + export/import + charts + search atomic domain + realtime + forms-extended + i18n + feature-flags atomic domain + /ax-verify subcommands |

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
| Spec Trios | **10** (8 full_trio, 3 backend_only — ratelimit/email-outbox/scheduled-task, 1 frontend_only — practices) | `ls specs/` cross-ref `contracts/` `blueprints/` |
| Guards | **19/19 GREEN** | `practices/evals/*_guard.sh` |
| Upstream snapshots | 43+ | `practices/upstream/*.snapshot.md` |

### This PRD's scope

This PRD admits **5 SPs covering ~40 catalog atoms** (full count in §5 surface table) from the audit P1 residuals not absorbed by v1.0.0 + v1.1.0. Filter is enforced explicitly in §4.

---

## §3 Objectives + Guardrails

### Objectives (one per SP)

- **O1 (SP30 — Billing Atomic Full_Trio Domain):** Ship `templates/L4/billing/` as the **9th** full_trio L4 domain **in a single atomic commit cluster** per SP26 Search precedent. Deliverables include: (a) L1 `currency-input.tsx` + `number-input.tsx` + `range-picker.tsx`; (b) L2 `pricing-table.tsx` + `plan-comparison.tsx` + `usage-meter.tsx` + `invoice-list.tsx` + `billing-history.tsx`; (c) L3 `pricing-page` template; (d) backend `templates/backend/billing/` with 6 entity/service/adapter files; (e) Spec Trio (`specs/billing-l0.yaml` + `contracts/billing-openapi.yaml` + `blueprints/billing-manifest.yaml`); (f) allowlist append `billing: full_trio`; (g) 4 new rules (`billing-event-idempotent` Java, `subscription-state-machine-explicit` Java, `currency-amount-precision-explicit` Java+React, `no-billing-cross-import-from-payment` Java+React — **NEW per Critic steelman boundary section §5.2.6**). Spec Trio is **landed** at SP30 commit, not drafted. **No `.draft` suffix loophole.**

- **O2 (SP31 — Korean Specials with backend_only Spec Trio):** Ship 사업자등록번호 + 휴대폰 본인인증 catalog atoms with regulatory anchors. **Iter-2 addition (Critic Blocker 2):** identity-verification ships as **`backend_only` Spec Trio** mirroring `email-outbox` / `scheduled-task` / `ratelimit` precedent. Deliverables: (a) L1 `business-registration-input.tsx`; (b) L2 `phone-verification-panel.tsx`; (c) backend `templates/backend/identity-verification/` (CI/DI handler + audit hook + 2 provider adapters PASS + KCB); (d) backend_only Spec Trio (`specs/identity-verification-l0.yaml` + `contracts/identity-verification-openapi.yaml` + `blueprints/identity-verification-manifest.yaml`); (e) allowlist append `identity-verification: backend_only`; (f) 3 new rules (`business-registration-checksum-required` React, `no-rrn-collection-without-legal-basis` Java + React halves). Anti-pattern fixture: a form that collects raw RRN without `@LegalBasis(law=...)` annotation FAILS.

- **O3 (SP32 — Forms / Rich Content):** Close the rich-content gap. L1 `rich-text-editor.tsx` (TipTap thin, dynamic-import enforced), `markdown-renderer.tsx`, `signature-pad.tsx`; L2 `form-stepper.tsx` (promote from SP20 wizard L3), `onboarding-checklist.tsx`, `product-tour.tsx` (shepherd.js dynamic import), `welcome-modal.tsx`, `confirm-modal.tsx`, `field-wizard.tsx`, `code-block.tsx`. New rule: `rich-content-must-use-dynamic-import` (React).

- **O4 (SP33 — Tables / Filters Advanced):** Close table-advanced gap. L2 `advanced-filter-builder.tsx`, `saved-view.tsx`, `saved-filters.tsx`, `faceted-filter.tsx`, `date-range-filter.tsx`, `filter-chips.tsx`, `tree-table.tsx`, `expandable-row.tsx`, `bulk-export.tsx`, `column-reorder.tsx`. New rule: `saved-view-must-be-url-state-or-server-persisted` (React).

- **O5 (SP34 — Admin / Settings Polish):** Ship admin + settings L2/L3 atoms. L2 `theme-switcher.tsx`, `impersonation-banner.tsx`, `maintenance-notice.tsx`, `network-status-pill.tsx`, `activity-feed.tsx`, `settings-section.tsx`, `preferences-form.tsx`, `keyboard-shortcut-help.tsx`, `skip-link.tsx`, `announce-live.tsx`; L3 `audit-log-page` + `admin-overview-page`. New rule: `impersonation-banner-required-when-acting-as-other-user` (React) — **iter-2 hardened against helper-rename bypass (Critic Soft Suggestion 2)**: rule matches canonical session state, not only function name `assumeUserId()`.

### Guardrails — Must Have

- Every new template carries `evidence:` frontmatter OR `@ax-template-meta` comment block citing the audit row + external doc.
- Every new rule carries `protects_template_id` + `failing_fixture_path`.
- **Atomic Spec-Trio rule (CRITICAL):** SP30 billing Spec Trio + L4 + backend + L2 + L3 + L1 prereqs + 4 rules + allowlist append ALL commit in SP30's single atomic commit cluster. SP31 identity-verification backend_only Spec Trio + backend + L1 + L2 + 3 rules + allowlist append ALL commit in SP31's single atomic commit cluster.
- Skill topology cap: **Tier-1 count frozen at 4.** No new Tier-1 skills.
- Korean specials (SP31) must include **anti-pattern rules** (RRN-protective). PRD does NOT ship raw RRN input.
- Allowlist additions: `billing: full_trio` (SP30 atomic), `identity-verification: backend_only` (SP31 atomic). **Race-safe append protocol:** rebase against HEAD before commit; `yq`-sorted insertion (per Catalog Extension §6.2).
- Skill-orchestrated verify only — no raw `npm run xxx` or `./gradlew testXxx` exposed to AI agents as user-facing surface.
- 4-iter ralplan APPROVE pattern.

### Guardrails — Must NOT

- No GitHub Actions workflows added.
- No LICENSE / CONTRIBUTING / docs-site / release.yml / Dependabot / GitHub Pages files.
- No major architectural changes: skill topology stays 3-tier; Spec Trio schema unchanged; Tier-1 count = 4.
- No new sibling `BaseEntityWithSubscription` class — billing entities extend SP25 `BaseEntity` with `@SQLDelete`.
- No raw RRN input component — only an RRN-protective rule.
- No P2 items absorbed.
- No new MockMvc tests; RestAssured only.
- No `@SpringBootTest` slow tests in failing-fixture suites — fixture tests use archunit OR static-script OR Vitest.
- No raw billing/payment provider SDK in templates — thin adapter abstraction only.
- **Iter-2 (Critic Steelman 1):** No `import payment.*` from `billing/` and no `import billing.*` from `payment/`. Cross-import enforced by new rule `no-billing-cross-import-from-payment` (failing fixture under `practices/evals/fixtures/no-billing-cross-import-from-payment/`).
- No realtime hard requirement for billing — billing webhooks reuse SP24 integration polling-or-webhook pattern.
- No new language support.

---

## §4 Filtered P1 Inventory (post-v1.1.0)

> Filter unchanged from iter-1: each item must score ≥3/5 on (a) L4-flow-unblock × (b) frequency-of-use for Korean enterprise SaaS forks.

| # | Surface | Layer | Priority | Justification | Effort | Ships in SP |
|---|---|---|---|---|---|---|
| P1-01 | `currency-input.tsx` | L1 | P1 | Audit A.2 P1 — KRW prefix, 3-digit grouping. Billing L4 prereq. | S | SP30 |
| P1-02 | `number-input.tsx` (spinner) | L1 | P1 | Audit A.2 P1 — quantity selectors. | S | SP30 |
| P1-03 | `range-picker.tsx` (slider range) | L1 | P1 | Audit A.2 P1 — plan tier range. | S | SP30 |
| P1-04 | `pricing-table.tsx` | L2 | P1 | Audit B.2.11 P1 — universal SaaS pricing. | M | SP30 |
| P1-05 | `plan-comparison.tsx` | L2 | P1 | Audit B.2.11 P2→PROMOTE P1. | M | SP30 |
| P1-06 | `usage-meter.tsx` | L2 | P1 | Audit B.2.11 P1 — quota progress. | S | SP30 |
| P1-07 | `invoice-list.tsx` | L2 | P1 | Audit B.2.11 P2→PROMOTE P1. | S | SP30 |
| P1-07a | `billing-history.tsx` | L2 | P1 | New (iter 2): completes billing L4 page composition (invoice list + payment history view). | S | SP30 |
| P1-08 | `pricing-page` | L3 | P1 | Audit C.2 P1. | M | SP30 |
| P1-09 | `templates/L4/billing/` (full_trio) | L4 | P1→PROMOTE | Audit Dimension E P2→PROMOTE P1. | L | SP30 |
| P1-10 | `templates/backend/billing/` | Backend | P1→PROMOTE | Pair of P1-09. | L | SP30 |
| P1-11 | `specs/billing-l0.yaml` + `contracts/billing-openapi.yaml` + `blueprints/billing-manifest.yaml` | Spec Trio | P1→PROMOTE | **Iter-2: landed, not drafted.** | M | SP30 (atomic) |
| P1-12 | Rule `billing-event-idempotent` (Java) | Rule | P1 | Extends SP21 `idempotency-key-on-mutations`. | S | SP30 |
| P1-13 | Rule `subscription-state-machine-explicit` (Java) | Rule | P1 | Subscription lifecycle explicit state machine. | S | SP30 |
| P1-14 | Rule `currency-amount-precision-explicit` (Java + React) | Rule | P1 | Integer minor-unit storage. | S | SP30 |
| P1-14a | Rule `no-billing-cross-import-from-payment` (Java + React) | Rule | P1 | **Iter-2 NEW (Critic Steelman 1).** Blocks `import payment.*` from billing/ and vice-versa. | S | SP30 |
| P1-15 | `business-registration-input.tsx` | L1 | P1 | Korean B2B (사업자등록번호). | S | SP31 |
| P1-16 | `phone-verification-panel.tsx` | L2 | P1 | KISA 본인인증 panel. | M | SP31 |
| P1-17 | `templates/backend/identity-verification/` | Backend | P1 | CI/DI handler. | M | SP31 |
| P1-17a | `specs/identity-verification-l0.yaml` + `contracts/identity-verification-openapi.yaml` + `blueprints/identity-verification-manifest.yaml` | Spec Trio (backend_only) | P1 | **Iter-2 NEW (Critic Blocker 2).** Mirrors email-outbox / scheduled-task / ratelimit precedent. | M | SP31 (atomic) |
| P1-18 | Rule `business-registration-checksum-required` (React) | Rule | P1 | Frontend MUST run checksum. | S | SP31 |
| P1-19 | Rule `no-rrn-collection-without-legal-basis` (Java + React) | Rule | P1 | RRN-protective per 개인정보보호법 § 24-1. | S | SP31 |
| P1-20 | `rich-text-editor.tsx` (TipTap) | L1 | P1→PROMOTE | Required by notification composer + comments + audit-event-note. | L | SP32 |
| P1-21 | `markdown-renderer.tsx` | L1 | P1→PROMOTE | Render-only path. | S | SP32 |
| P1-22 | `signature-pad.tsx` | L1 | P1→PROMOTE | Korean KYC + 전자결재 surfaces. | M | SP32 |
| P1-23 | `form-stepper.tsx` | L2 | P1 | Promote from SP20 wizard L3. | M | SP32 |
| P1-24 | `onboarding-checklist.tsx` | L2 | P1 | First-run setup. | M | SP32 |
| P1-25 | `product-tour.tsx` | L2 | P1→PROMOTE | shepherd.js dynamic-import. | L | SP32 |
| P1-26 | `welcome-modal.tsx` | L2 | P1→PROMOTE | First-run welcome dialog. | S | SP32 |
| P1-26a | `confirm-modal.tsx`, `field-wizard.tsx`, `code-block.tsx` | L2 | P1 | Iter-2: completes forms cluster (per Critic SP-list). | S each | SP32 |
| P1-27 | Rule `rich-content-must-use-dynamic-import` (React) | Rule | P1 | Bundle anti-bloat. | S | SP32 |
| P1-28 | `advanced-filter-builder.tsx` | L2 | P1 | AND/OR rule schema. | L | SP33 |
| P1-29 | `saved-view.tsx`, `saved-filters.tsx`, `faceted-filter.tsx`, `date-range-filter.tsx`, `filter-chips.tsx` | L2 | P1 | Filter UX cluster. | M (cluster) | SP33 |
| P1-30 | `tree-table.tsx`, `expandable-row.tsx`, `bulk-export.tsx`, `column-reorder.tsx` | L2 | P1 | Table-advanced cluster. | M (cluster) | SP33 |
| P1-31 | Rule `saved-view-must-be-url-state-or-server-persisted` (React) | Rule | P1 | URL-state pattern. | S | SP33 |
| P1-32 | `theme-switcher.tsx` | L2 | P1 | next-themes wrapper. | S | SP34 |
| P1-33 | `impersonation-banner.tsx` | L2 | P1 | Security-critical banner. | S | SP34 |
| P1-34 | `maintenance-notice.tsx`, `network-status-pill.tsx`, `activity-feed.tsx`, `settings-section.tsx`, `preferences-form.tsx`, `keyboard-shortcut-help.tsx`, `skip-link.tsx`, `announce-live.tsx` | L2 | P1 | Admin/settings/a11y cluster. | M (cluster) | SP34 |
| P1-35 | `audit-log-page` L3 + `admin-overview-page` L3 | L3 | P1 | Reusable L3 extracts. | M (cluster) | SP34 |
| P1-36 | Rule `impersonation-banner-required-when-acting-as-other-user` (React) | Rule | P1 | **Iter-2 hardened**: matches canonical session/acting-as state, not helper function name. | S | SP34 |

**Totals (this PRD absorbs):**

| Surface | Count |
|---|---|
| L1 primitives added | 7 (currency-input, number-input, range-picker, business-registration-input, rich-text-editor, markdown-renderer, signature-pad) |
| L2 blocks added | ~26 (5 billing + 1 Korean panel + 9 forms-rich + 10 tables-filters + 10 admin-polish) — cluster items |
| L3 pages added | 3 (pricing-page, audit-log-page, admin-overview-page) |
| L4 domains added | 1 (billing — full_trio) |
| Backend cross-cutting + domain added | 2 (billing + identity-verification) |
| Spec Trios added | 2 (billing full_trio + identity-verification backend_only) |
| Java rules added | 5 (billing-event-idempotent, subscription-state-machine-explicit, currency-amount-precision-explicit, no-billing-cross-import-from-payment, no-rrn-collection-without-legal-basis-Java) |
| React rules added | 7 (currency-amount-precision-explicit-React, no-billing-cross-import-from-payment-React, business-registration-checksum-required, no-rrn-collection-without-legal-basis-React, rich-content-must-use-dynamic-import, saved-view-must-be-url-state-or-server-persisted, impersonation-banner-required-when-acting-as-other-user) |
| Skills added | 0 |
| Upstream snapshots added | 7 (TipTap v2, signature_pad, shepherd.js, 국세청 사업자등록 API, KISA 본인인증, next-themes, Stripe Billing + Toss Billing) |

**Total catalog atoms added: ~46 across 5 SPs.**

### Deferred to round-5+ (NOT in this PRD scope)

| Item | Audit row | Defer reason |
|---|---|---|
| `tree-view` L1 | A.2 P2 | Low frequency. |
| `mention` L1 | A.2 P2 | Specialized (chat/comments only). |
| `color-picker`, `cropper`, `carousel` L1 | A.2 P2 | Specialized. |
| `toggle`, `toggle-group`, `context-menu`, `drawer (vaul)`, `rating`, `pin-input` L1 | A.2 P2 | Existing primitives cover. |
| `time-picker` L1 | A.2 P1 (low frequency) | **Iter-2 explicit resolution (Critic K)**: billing renewal dates are date-only (no time component) — `nextBillingDate` field is `LocalDate`, not `LocalDateTime`. Admin scheduling defers to ops-admin add. |
| `phone-input-kr.tsx`, `rrn-masked-input.tsx` L1 | A.2 (regulatory) | Phone-verification-panel (SP31) covers; RRN-protective rule (SP31) covers. |
| `breadcrumb`, `menubar`, `navigation-menu` L1 | A.2 P1 | **Iter-2 explicit resolution (Critic K)**: SP34's `audit-log-page` + `admin-overview-page` use **flat tab/header navigation** (app-shell tabs, not breadcrumb hierarchy). Documented in §5.7 SP34 architecture note. |
| L1 `pagination` | A.2 P1 | L2 pagination.tsx already exists. |
| `chat-composer`, `comments-thread`, `inbox-list` L2 | B.2.6 P2 | No L4 messaging domain. |
| `funnel-chart`, `heatmap` extensions L2 | B.2.4 P2 | Specialized. |
| `download-button`, `image-preview-grid`, `attachment-list` L2 | B.2.5 P2 | Composes existing. |
| `role-editor`, `permission-matrix` L2 | B.2.12 P2 | Admin-RBAC specialized. |
| `inbox-page`, `bulk-edit-page`, `advanced-search-page`, `landing-page` L3 | C.2 P2 | Marketing or specialized. |
| L4 `messaging` (chat) | E.2 P2 | No fork-receiver demand. |
| L4 `analytics-tracking` | E.2 P2 | Specialized. |
| ~~L4 `subscription-management`~~ | E.2 P2 | **Iter-2 (Critic K)**: REMOVED from deferred list — **absorbed by billing L4 in SP30**. Not deferred. |

---

## §5 Implementation Plan (SP30–SP34)

### §5.1 SP dependency graph

```
SP30 (Billing atomic full_trio — 1 commit cluster)
     │
     ├─► SP31 (Korean specials — backend_only identity-verification Spec Trio + L1 + L2 + backend + 3 rules)
     ├─► SP32 (Rich content + onboarding L2 + 1 new rule)
     └─► SP33 (Tables/Filters advanced cluster + 1 new rule)
                 │
                 ▼
              SP34 (Admin/Settings polish cluster + 2 new L3 + 1 new rule)
```

**Wall-time estimate:** SP30 (3 d, higher due to atomic-trio scope) → SP31 ‖ SP32 ‖ SP33 (3 d parallel) → SP34 (2 d) = **≈ 8–10 d** if parallelism honored.

### §5.2 SP30 — Billing Atomic Full_Trio Domain

| Field | Value |
|---|---|
| **Inputs** | Audit rows A.2 (P1-01, P1-02, P1-03), B.2.11 (P1-04 through P1-07a), C.2 (P1-08), Dimension E P2→P1 (P1-09, P1-10, P1-11). SP6 payment domain (pattern reference only — no cross-import). SP24 integration (HMAC webhook pattern). SP25 BaseEntity (extend with `@SQLDelete`). Stripe Billing API doc + Toss Payments 정기결제 API doc. |
| **Deliverables (single atomic commit cluster)** | **L1 (3):** `templates/L1/components/currency-input.tsx`, `number-input.tsx`, `range-picker.tsx`. **L2 (5):** `templates/L2/blocks/pricing-table.tsx`, `plan-comparison.tsx`, `usage-meter.tsx`, `invoice-list.tsx`, `billing-history.tsx`. **L3 (1):** `templates/L3/pages/pricing-page/{page.tsx, README.md, error.tsx, loading.tsx}`. **L4 (1):** `templates/L4/billing/` (Next.js App Router segment composing SP30 L2 blocks). **Backend (6):** `templates/backend/billing/{Subscription.java, Plan.java, Invoice.java, BillingEvent.java, BillingService.java, BillingController.java, BillingAdminController.java, StripeBillingAdapter.java, TossBillingAdapter.java, BillingProvider.java, WebhookBillingReceiver.java, BillingDto.java, SubscriptionStateMachine.java}` (entity + service + adapter + interface + webhook receiver — 6 logical components, ~13 files). **Spec Trio (3 — landed, not drafted):** `specs/billing-l0.yaml`, `contracts/billing-openapi.yaml`, `blueprints/billing-manifest.yaml`. **Allowlist append:** `billing: full_trio` in `practices/evals/trio_integrity_allowlist.yaml`. **Rules (4):** `practices/rules/billing-event-idempotent.md`, `subscription-state-machine-explicit.md`, `currency-amount-precision-explicit.md` (Java), `no-billing-cross-import-from-payment.md` (Java); `practices-react/rules/currency-amount-precision-explicit.md` (React), `no-billing-cross-import-from-payment.md` (React). **Snapshots (2):** `practices/upstream/stripe-billing-2026-05.snapshot.md`, `practices/upstream/toss-billing-2026-05.snapshot.md`. **Fixtures:** 4 failing + 4 passing fixtures under `practices/evals/fixtures/billing-*/`. |
| **Acceptance** | `/ax-verify-L1` exits 0 with 45 L1 components; `/ax-verify-L2` exits 0 with ≥69 L2 blocks; `/ax-verify-L3` exits 0 with 17 L3 templates; `/ax-verify-L4 billing` exits 0; `/ax-verify-domain billing` exits 0; trio_integrity_guard exits 0 with `billing` entry; cross_trio_guard exits 0; evidence_guard exits 0; new failing fixtures (e.g., `fail_billing_event_no_idempotency_key/`, `fail_billing_cross_import_payment/`) exit non-zero; `pass/` siblings exit 0; all entities pass `BaseEntitySoftDeleteArchTest`. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh && bash skills/ax-verify-L4/scripts/run.sh billing && bash skills/ax-verify-domain/scripts/run.sh billing` |
| **TDD anchor** | **Backend RED:** `backend/src/test/java/ax/template/billing/BillingFlowIT.java` asserting `whenSubscriptionCreated_thenBillingEventEmitted_andIdempotencyKeyHonored`. RED reason: `BillingService.createSubscription()` not implemented. First green: `cd backend && ./gradlew testBilling` (after adding `tasks.register("testBilling")` to build.gradle.kts per SP19 scheduled-task pattern). **Frontend RED:** `templates/_tests/billing-prereq.spec.ts` asserting `expect(currencyInput({ currency: 'KRW', value: 1234567 })).toMatchSnapshot('₩1,234,567')`. First green: `npx vitest run templates/_tests/billing-prereq.spec.ts`. **Trio RED:** `bash practices/evals/trio_integrity_guard.sh` exits non-zero until SP30 commit lands `billing: full_trio`. |
| **observability_signal** | `billing.invoice.generated_count` (Micrometer counter, tags: provider={stripe,toss}, status={success,failed}); `billing.subscription.lifecycle_transition` (audit event, fields: subscriptionId, from, to, reason); `billing.webhook.received_count` (counter, tags: provider, event_type); `billing.event.idempotency_hit_count` (counter — replay-detection signal). |
| **Risks + mitigation (owner / command / threshold / recovery)** | **Risk 1 — Stripe vs Toss provider abstraction leaks Stripe-specific event types.** Owner: SP30-backend-worker. Command: `cd backend && ./gradlew testBilling --tests BillingProviderContractTest` feeding 5 Stripe sample webhooks + 5 Toss sample webhooks. Threshold: any test asserting `assertEquals(stripeEvent.toCanonical().withoutMetadata(), tossEvent.toCanonical().withoutMetadata())` fails. Recovery: revert `BillingProvider` interface change; refactor canonical `BillingEvent` shape to exclude leaked fields; re-run. **Risk 2 — Subscription state machine cycle leaks into multiple services.** Owner: SP30-backend-worker. Command: `cd backend && ./gradlew testBilling --tests OnlyStateMachineMutatesSubscriptionStatusArchTest`. Threshold: archunit assertion fails (any non-`SubscriptionStateMachine` class mutates `Subscription.status`). Recovery: move mutator code into `SubscriptionStateMachine`; re-run. **Risk 3 — Atomic Spec-Trio commit fails because trio_integrity_allowlist conflict.** Owner: SP30-lead. Command: `bash skills/ax-verify/scripts/atomic-spec-trio-check.sh` pre-merge. Threshold: hook exits non-zero (missing any of: spec/contract/manifest/L4-dir/backend-dir/allowlist-entry/4-rules in staged commit). Recovery: `git rebase origin/main`, re-stage all SP30 artifacts as one atomic commit cluster; retry up to 2 rebases. **Risk 4 — currency-input KRW formatting collides with `currency-formatter.tsx`.** Owner: SP30-L1-worker. Command: `npx vitest run templates/_tests/currency-input-formatter-disjoint.spec.ts`. Threshold: any overlapping snapshot diff non-zero. Recovery: keep `<input>`-based currency-input (controlled) separate from `<span>`-based formatter (read-only); both cite ISO 4217 evidence. **Risk 5 — Stripe webhook replay attack within 5 min window.** Owner: SP30-backend-worker. Command: `curl -X POST -H "Stripe-Signature: t=<5min-old>,v1=<sig>" ...` against `WebhookBillingReceiver`. Threshold: replay event accepted within 5 min idempotency window (returns 200 + duplicate processing). Recovery: rollback to V202605181203 migration (drop billing tables); fix `Stripe-Signature` timestamp window check to reject events older than 5 min; redeploy. |
| **Agent count** | 5 (Spec Trio worker, backend worker, L1 worker, L2+L3 worker, rules+fixtures worker — sequential Spec Trio first, then 4 parallel; final integration commit by SP30-lead). |
| **Effort** | XL (≈ 3 d). |
| **Rollback boundary** | `git revert sp30-*` reverts SP30 atomic commit (single commit cluster); trio_integrity_allowlist auto-reverts (single line removal); no orphan billing entries; archunit guards return to pre-SP30 baseline. |

#### §5.2.6 Payment vs Billing Boundary (Critic Steelman 1)

**Iter-2 mandatory boundary section.** The boundary between SP6 payment and SP30 billing is declared explicitly to prevent execution-time drift:

| Concern | Owner | Notes |
|---|---|---|
| One-shot authorization | **payment** | `PaymentService.authorize()` — atomic transaction, no recurrence. |
| One-shot capture | **payment** | `PaymentService.capture()`. |
| One-shot refund | **payment** | `PaymentService.refund()`. |
| Subscription lifecycle (TRIAL → ACTIVE → PAST_DUE → CANCELLED) | **billing** | `SubscriptionStateMachine` is sole mutator. |
| Invoice issuance + line items | **billing** | `Invoice.java` entity, `InvoiceService.issue()`. |
| Recurring billing event normalization | **billing** | Canonical `BillingEvent` emitted by `BillingProvider` adapters. |
| Plan management | **billing** | `Plan.java` entity, admin CRUD. |
| Idempotency-key pattern | **shared pattern only — no cross-import** | Both domains use SP21 `idempotency-key-on-mutations` rule; pattern is rule-enforced, not code-imported. |
| ProblemDetail error envelope | **shared pattern only — no cross-import** | RFC 7807 ProblemDetail conventions; pattern is rule-enforced. |

**Enforcement (new rule P1-14a):** `no-billing-cross-import-from-payment` (Java + React variants).
- Java half: archunit rule `noClasses().that().resideInAPackage("..billing..").should().dependOnClassesThat().resideInAPackage("..payment..")` and inverse for `payment` → `billing`.
- React half: ESLint rule blocking `import * from '@/templates/L4/payment/*'` inside `templates/L4/billing/*` (and inverse).
- Failing fixture: `practices/evals/fixtures/no-billing-cross-import-from-payment/fail_billing_imports_payment/BillingService.java` with `import ax.template.payment.PaymentService;` → archunit fail. `pass_idempotency_pattern_no_import/` shows correct usage (rule annotation, no import).

### §5.3 SP31 — Korean Specials (사업자등록번호 + 휴대폰 본인인증 + backend_only Spec Trio)

| Field | Value |
|---|---|
| **Inputs** | Audit A.2 P1 rows (P1-15, P1-16); A.8 Korean enterprise specifics; Critic Korean specificity verdict; 국세청 사업자등록증명원 algorithm doc (https://www.nts.go.kr); KISA 본인인증 가이드라인; 개인정보보호법 § 24-1. **Iter-2 (Critic Blocker 5):** public/official fixture data — National Tax Service (국세청) public test numbers and open-data.go.kr 사업자등록번호 sample list (>=5 verified samples per category). |
| **Deliverables (single atomic commit cluster)** | **L1 (1):** `templates/L1/components/business-registration-input.tsx`. **L2 (1):** `templates/L2/blocks/phone-verification-panel.tsx`. **Backend (1):** `templates/backend/identity-verification/{IdentityVerificationCallbackController.java, IdentityVerificationService.java, IdentityVerificationProvider.java, PassAdapter.java, KcbAdapter.java, IdentityVerificationDto.java, VerifiedIdentity.java}`. **Spec Trio (backend_only — iter-2 NEW):** `specs/identity-verification-l0.yaml`, `contracts/identity-verification-openapi.yaml`, `blueprints/identity-verification-manifest.yaml`. **Allowlist append:** `identity-verification: backend_only` in `practices/evals/trio_integrity_allowlist.yaml`. **Rules (3):** `practices/rules/no-rrn-collection-without-legal-basis.md` (Java); `practices-react/rules/business-registration-checksum-required.md`, `practices-react/rules/no-rrn-collection-without-legal-basis.md` (React). **Snapshots (3):** `practices/upstream/nts-business-reg-2026-05.snapshot.md`, `practices/upstream/kisa-identity-verification-2026-05.snapshot.md`, `practices/upstream/pipa-article-24-2026-05.snapshot.md`. **Fixtures (iter-2 official/public data):** `practices/evals/fixtures/business-registration-checksum/{pass/, fail_invalid_checksum/, fail_format_violation/}` — pass/ contains 5+ public business numbers verified via 국세청 사업자등록증명원 official sample list + open-data.go.kr URLs documented in `README.md`; fail_invalid_checksum/ uses same numbers with last digit mutated; fail_format_violation/ uses malformed inputs (letters, length errors). |
| **Acceptance** | `/ax-verify-L1` exits 0; `/ax-verify-L2` exits 0; `/ax-verify-java` exits 0 on `templates/backend/identity-verification/`; `/ax-verify-react` exits 0 on new rules; `/ax-verify-domain identity-verification` exits 0 (**iter-2 NEW** — backend_only domain verification); trio_integrity_guard exits 0 with `identity-verification: backend_only` entry; new failing fixtures (`fail_invalid_checksum/`, `fail_format_violation/`, `fail_rrn_no_legal_basis/`) exit non-zero; `pass/` siblings exit 0. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh && bash skills/ax-verify-domain/scripts/run.sh identity-verification` |
| **TDD anchor** | **Frontend RED (iter-2 fixed per Critic Blocker 5):** Test file `templates/_tests/business-reg-checksum.spec.ts` loads `practices/evals/fixtures/business-registration-checksum/pass/*.json` (5+ public verified samples), asserts every entry returns `true`; loads `fail_invalid_checksum/*.json`, asserts every entry returns `false`; loads `fail_format_violation/*.json`, asserts every entry throws `FormatViolationError`. RED reason: `validateBusinessRegistration` not implemented + fixtures not yet committed. First green: `npx vitest run templates/_tests/business-reg-checksum.spec.ts`. Mock data explicitly forbidden; fixture README cites source URL + license per entry. **Backend RED:** `backend/src/test/java/ax/template/identityverification/IdentityVerificationFlowIT.java` — inbound CI/DI webhook with invalid HMAC rejected with 401; valid HMAC produces a `VerifiedIdentity` row. First green: `cd backend && ./gradlew testIdentityVerification`. |
| **observability_signal** | `identity.business_reg.checksum_failure_count` (counter, tags: source={input,batch}); `identity.phone_verify.cidi_token_issued_count` (counter, tags: provider={pass,kcb}); `identity.phone_verify.callback_hmac_invalid_count` (counter — security signal); `identity.rrn_collection_attempt_blocked_count` (audit event — fires when RRN-protective rule blocks a request). |
| **Risks + mitigation (owner / command / threshold / recovery)** | **Risk 1 — 사업자등록번호 checksum has multiple variants.** Owner: SP31-frontend-worker. Command: `npx vitest run templates/_tests/business-reg-checksum.spec.ts` against fixture set. Threshold: ≥1 sample in `pass/` misclassified as invalid OR ≥1 sample in `fail_invalid_checksum/` misclassified as valid. Recovery: snapshot `nts-business-reg-2026-05.snapshot.md` updated with canonical multiplier sequence `[1,3,7,1,3,7,1,3,5]`; algorithm refactored; re-run fixture set. **Risk 2 — RRN-protective rule misinterpreted as RRN-enabling.** Owner: SP31-rules-worker. Command: rule docstring lint — `grep -E "BLOCKS|prohibits" practices-react/rules/no-rrn-collection-without-legal-basis.md`. Threshold: docstring missing explicit "this rule does NOT enable RRN collection" disclaimer. Recovery: edit docstring; resubmit. **Risk 3 — CI/DI handler design diverges across PASS / KCB / SCI providers.** Owner: SP31-backend-worker. Command: `cd backend && ./gradlew testIdentityVerification --tests IdentityVerificationProviderContractTest` feeding 3 provider sample payloads (PASS + KCB + SCI-stub). Threshold: canonical `VerifiedIdentity {ci, di, name, dob, verifiedAt, providerName}` differs across providers. Recovery: refactor adapter mapping; SCI deferred to round-5 only after PASS+KCB pass. **Risk 4 — RRN-protective rule false-positives on CI/DI flows.** Owner: SP31-rules-worker. Command: `bash practices/evals/fixtures/no-rrn-collection-without-legal-basis/pass_ci_di_verified/run.sh`. Threshold: `pass_ci_di_verified/` (DTO field `verifiedIdentityNumber`) triggers the rule. Recovery: rule matcher refined to `/^(rrn\|주민(등록)?번호\|id_number\|residentRegistrationNumber)$/` excluding `ci`, `di`, `verifiedIdentityNumber`, `external_id`. |
| **Agent count** | 3 (L1+L2+frontend-rules worker, backend worker, Spec Trio + allowlist + fixtures worker — Spec Trio first, then 2 parallel; final integration commit by SP31-lead). |
| **Effort** | M (≈ 2 d). |
| **Rollback boundary** | `git revert sp31-*` reverts SP31 atomic commit; identity-verification backend dir + Spec Trio + allowlist entry all revert cleanly. |

### §5.4 SP32 — Forms / Rich Content

| Field | Value |
|---|---|
| **Inputs** | Audit A.2 P2→P1 promotions (rich-text-editor, signature-pad); B.2.1 P1 (form-stepper); B.2.9 P1/P2→P1 promotions (onboarding-checklist, product-tour, welcome-modal, confirm-modal, field-wizard, code-block); existing `practices-react/rules/bundle-dynamic-imports.md`; TipTap v2 docs, signature_pad lib docs, shepherd.js docs. |
| **Deliverables** | `templates/L1/components/{rich-text-editor.tsx, markdown-renderer.tsx, signature-pad.tsx, code-block.tsx}`; `templates/L2/blocks/{form-stepper.tsx, onboarding-checklist.tsx, product-tour.tsx, welcome-modal.tsx, confirm-modal.tsx, field-wizard.tsx}`; `practices-react/rules/rich-content-must-use-dynamic-import.md`; `practices-react/upstream/{tiptap-v2-2026-05.snapshot.md, signature-pad-2026-05.snapshot.md, shepherd-js-2026-05.snapshot.md}`. |
| **Acceptance** | `/ax-verify-L1` exits 0 with 49 components (45 from SP30 + 4); `/ax-verify-L2` exits 0 with ≥75 L2 blocks; new failing fixture `fail_static_tiptap_import/` exits non-zero; `pass/` exits 0. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L1/components/rich-text-editor.tsx templates/L2/blocks/form-stepper.tsx` |
| **TDD anchor** | `templates/_tests/rich-content-dynamic-import.spec.ts` asserting `expect(richTextEditorImports).not.toContain('@tiptap/core')` (dynamic, not static). RED reason: scaffolding writes static import; ESLint rule fires. First green: `npx eslint --fix templates/L1/components/rich-text-editor.tsx && npx vitest run`. |
| **observability_signal** | `form.wizard.step_advanced_count` (counter, tags: wizardId, fromStep, toStep); `form.rich_text.editor_mounted_count` (counter — mount-error detection via failed_mount tag); `form.signature_pad.canvas_leak_count` (counter — useEffect-cleanup failure signal); `form.product_tour.completion_rate` (gauge — first-week activation metric). |
| **Risks + mitigation (owner / command / threshold / recovery)** | **Risk 1 — TipTap pulls 200+ kB into main chunk.** Owner: SP32-L1-worker. Command: `bash practices-react/evals/bundle-size_guard.sh templates/L1/components/rich-text-editor.tsx`. Threshold: main chunk size delta >50 kB after adding rich-text-editor. Recovery: enforce dynamic import via `rich-content-must-use-dynamic-import` rule; refactor to `next/dynamic`. **Risk 2 — TipTap RSC compatibility risk (Next.js 16 RSC + Cache Components).** Owner: SP32-L1-worker. Command: `cd frontend && npm run build` against L4 page using `<RichTextEditor>`. Threshold: build exit non-zero OR Playwright assertion `editor reachable within 3s; no console error` fails. Recovery: revert to client-only `"use client"` wrapper with `useEffect` mount-gate per snapshot citation; SSR fallback renders `<textarea>` placeholder. **Risk 3 — signature-pad canvas memory leak on unmount.** Owner: SP32-L1-worker. Command: `npx vitest run templates/_tests/signature-pad-cleanup.spec.ts` — simulates mount→unmount→GC and asserts no leaked listeners. Threshold: `signature_pad.off()` not called in useEffect cleanup. Recovery: add cleanup hook per snapshot citation. **Risk 4 — shepherd.js product-tour DOM coupling breaks in Next.js 16 RSC.** Owner: SP32-L2-worker. Command: Playwright SSR test against `/onboarding` page. Threshold: tour does not render OR throws console error. Recovery: explicit `"use client"` directive; SSR test in CI. |
| **Agent count** | 2 (L1 worker, L2 worker — disjoint surfaces). |
| **Effort** | L (≈ 2 d). |
| **Rollback boundary** | `git revert sp32-*` reverts SP32 commits; existing forms-extended L2 (SP27) unaffected. |

### §5.5 SP33 — Tables / Filters Advanced

| Field | Value |
|---|---|
| **Inputs** | Audit B.2.2 / B.2.3 P0→P1 demotions (Critic); existing SP15 `data-table.tsx`, `virtualized-table.tsx`, `column-picker.tsx`, SP24 backend export-job; URL-state pattern from CLAUDE.md `web/patterns.md`. |
| **Deliverables** | `templates/L2/blocks/{advanced-filter-builder.tsx, saved-view.tsx, saved-filters.tsx, faceted-filter.tsx, date-range-filter.tsx, filter-chips.tsx, tree-table.tsx, expandable-row.tsx, bulk-export.tsx, column-reorder.tsx}`; `practices-react/rules/saved-view-must-be-url-state-or-server-persisted.md`; snapshots: `@tanstack/react-table` v8 update, `react-aria-components` for column-reorder dnd. |
| **Acceptance** | `/ax-verify-L2` exits 0 with ≥85 L2 blocks; new failing fixture `fail_saved_view_localstorage_only/` exits non-zero; `pass/` exits 0. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L2/blocks/advanced-filter-builder.tsx templates/L2/blocks/bulk-export.tsx` |
| **TDD anchor** | `templates/_tests/saved-view-persistence.spec.ts` asserting `expect(savedView.persistence).toBe('url'|'server')` and `not.toBe('localStorage')`. RED reason: scaffolding writes localStorage-based saved-view; ESLint rule fires. First green: `npx eslint --fix templates/L2/blocks/saved-view.tsx && npx vitest run`. |
| **observability_signal** | `table.advanced_filter.applied_count` (counter, tags: filterDepth, operatorMix); `table.saved_view.loaded_count` (counter, tags: persistenceMode={url,server}); `table.bulk_export.invoked_count` (counter, tags: format={csv,xlsx,pdf}); `table.tree_table.depth_violation_count` (counter — depth >5 attempts). |
| **Risks + mitigation (owner / command / threshold / recovery)** | **Risk 1 — advanced-filter-builder grows into DSL.** Owner: SP33-filters-worker. Command: `bash practices-react/evals/saved-filter-schema-guard.sh templates/L2/blocks/advanced-filter-builder.tsx`. Threshold: schema asserts fixed grammar (5 operators, 2 logical, nesting ≤3) violated. Recovery: refactor to `blueprints/saved-filter-schema.yaml` constraints; reject over-nested filters in code. **Risk 2 — bulk-export triplet causes bundle bloat.** Owner: SP33-export-worker. Command: `bash practices-react/evals/bundle-size_guard.sh templates/L2/blocks/bulk-export.tsx`. Threshold: main chunk size delta >30 kB after bulk-export add. Recovery: PDF + Excel via dynamic import; CSV via in-house utility (no lib). **Risk 3 — tree-table state explosion at depth >5.** Owner: SP33-tables-worker. Command: `npx vitest run templates/_tests/tree-table-depth-limit.spec.ts`. Threshold: depth 6 input does not throw explicit error. Recovery: enforce API limit of depth 5 with explicit `DepthExceededError`; failing fixture demos depth 6 → Vitest fail. |
| **Agent count** | 3 (filters worker, tables-advanced worker, bulk-export worker — disjoint surfaces). |
| **Effort** | L (≈ 2 d). |
| **Rollback boundary** | `git revert sp33-*` reverts SP33 commits; existing data-table / virtualized-table unaffected. |

### §5.6 SP34 — Admin / Settings Polish

| Field | Value |
|---|---|
| **Inputs** | Audit B.2.7 P1 (errors), B.2.10 P1 (settings), B.2.12 P1 (admin), B.2.13 P0 (a11y); SP28 i18n (locale-switcher); SP17 audit-log L4 (extract L3 page); SP20 settings-overview L3 (extract admin-overview L3). |
| **Deliverables** | `templates/L2/blocks/{theme-switcher.tsx, impersonation-banner.tsx, maintenance-notice.tsx, network-status-pill.tsx, activity-feed.tsx, settings-section.tsx, preferences-form.tsx, keyboard-shortcut-help.tsx, skip-link.tsx, announce-live.tsx}`; `templates/L3/pages/audit-log-page/`, `templates/L3/pages/admin-overview-page/`; `practices-react/rules/impersonation-banner-required-when-acting-as-other-user.md`; `practices/upstream/next-themes-2026-05.snapshot.md`, `wcag-22-techniques-2026-05.snapshot.md`. **Iter-2 architecture note:** L3 audit-log-page + admin-overview-page use **flat tab/header navigation** via `app-shell.tsx` tabs (per deferred-list resolution); no breadcrumb dependency. |
| **Acceptance** | `/ax-verify-L2` exits 0 with ≥95 L2 blocks; `/ax-verify-L3` exits 0 with 19 L3 templates; new failing fixture `fail_impersonate_no_banner/` exits non-zero; `pass/` exits 0; existing 19 guards remain GREEN. |
| **Verify command** | `bash skills/ax-verify/scripts/run-all.sh templates/L2/blocks/impersonation-banner.tsx templates/L3/pages/audit-log-page/` |
| **TDD anchor** | `templates/_tests/impersonation-banner.spec.ts` asserting `whenSessionState_actingAs_isNonNull_thenImpersonationBannerRendered`. **Iter-2 hardened**: rule matches canonical session `actingAs` field, not helper function name `assumeUserId()`. RED reason: scaffolding sets `session.actingAs = userId` without banner render → ESLint rule fires. First green: `npx eslint --fix templates/L2/blocks/impersonation-banner.tsx && npx vitest run`. |
| **observability_signal** | `admin.impersonation.started_count` (counter, tags: actorRole, targetRole); `admin.impersonation.banner_missing_count` (counter — rule-violation runtime detection); `admin.settings.preference_changed_count` (counter, tags: preferenceKey); `admin.audit_log.viewed_count` (counter — analytics for admin-overview-page); `a11y.skip_link.activated_count` (counter — WCAG 2.4.1 usage signal). |
| **Risks + mitigation (owner / command / threshold / recovery)** | **Risk 1 — theme-switcher SSR hydration mismatch.** Owner: SP34-L2-worker. Command: `cd frontend && npm run build && npx playwright test tests/theme-switcher-ssr.spec.ts`. Threshold: Playwright detects hydration mismatch warning OR theme flicker >100 ms on first paint. Recovery: switch to Cookie-based theme storage (server-readable); snapshot evidence cites next-themes Cookie pattern. **Risk 2 — skip-link + announce-live collide with existing app-shell.** Owner: SP34-L2-worker. Command: `npx playwright test tests/a11y-shell.spec.ts` asserting `<a href="#main">` is first focusable element. Threshold: Playwright assertion fails. Recovery: both ship as opt-in app-shell composables; SP34 wires them via single-hunk in-place edit; revert hunk if test fails. **Risk 3 — impersonation-banner rule bypass via helper rename (Critic Soft Suggestion 2).** Owner: SP34-rules-worker. Command: `bash practices-react/evals/fixtures/impersonation-banner-required-when-acting-as-other-user/fail_helper_renamed/run.sh`. Threshold: fixture using `runAsUser()` (not `assumeUserId()`) without `<ImpersonationBanner>` does not trigger the rule. Recovery: rule matcher refined to detect canonical session state mutation `session.actingAs = <userId>` OR any function returning `{actingAs: ...}` object — not function name. Failing fixture `fail_helper_renamed_runAsUser/` confirms detection. |
| **Agent count** | 2 (L2 cluster worker, L3 cluster worker + rule worker). |
| **Effort** | M (≈ 2 d). |
| **Rollback boundary** | `git revert sp34-*` reverts SP34 commits; existing app-shell.tsx in-place edits reverted cleanly (single hunk per file). |

### §5.7 SP34 Navigation Architecture Note

L3 `audit-log-page` and `admin-overview-page` use **flat tab/header navigation** via `app-shell.tsx`:

- audit-log-page: tabs = [Events | Filters | Saved Views | Export]
- admin-overview-page: tabs = [Users | Roles | Audit | Settings]

No breadcrumb hierarchy required. This resolves deferred-list ambiguity (Critic K): `breadcrumb` L1 stays deferred to round-5+.

### §5.8 Verification Matrix (single authoritative table — iter-2 with observability_signal)

| SP | New artifacts | Owning verify skill | TDD anchor file | First green command | observability_signal | Rollback safety |
|---|---|---|---|---|---|---|
| SP30 | 3 L1 + 5 L2 + 1 L3 + 1 L4 + 6 backend components + 1 Spec Trio (full_trio) + 1 allowlist entry + 4 new rules + 2 snapshots + 4 fixture pairs | `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-L3`, `/ax-verify-L4 billing`, `/ax-verify-domain billing` | `backend/.../billing/BillingFlowIT.java` + `templates/_tests/billing-prereq.spec.ts` | `cd backend && ./gradlew testBilling && npx vitest run templates/_tests/billing-prereq.spec.ts` | `billing.invoice.generated_count`, `billing.subscription.lifecycle_transition`, `billing.webhook.received_count`, `billing.event.idempotency_hit_count` | `git revert sp30-*` atomic-clean (single commit cluster) |
| SP31 | 1 L1 + 1 L2 + 1 backend dir (with 7 files) + 1 Spec Trio (backend_only) + 1 allowlist entry + 3 new rules + 3 snapshots + 3 fixture pairs (public data) | `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-java`, `/ax-verify-react`, `/ax-verify-domain identity-verification` | `templates/_tests/business-reg-checksum.spec.ts` + `backend/.../identityverification/IdentityVerificationFlowIT.java` | `npx vitest run + cd backend && ./gradlew testIdentityVerification` | `identity.business_reg.checksum_failure_count`, `identity.phone_verify.cidi_token_issued_count`, `identity.phone_verify.callback_hmac_invalid_count`, `identity.rrn_collection_attempt_blocked_count` | `git revert sp31-*` atomic-clean |
| SP32 | 4 L1 + 6 L2 + 1 new rule + 3 snapshots | `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-react` | `templates/_tests/rich-content-dynamic-import.spec.ts` | `npx vitest run + npx eslint --fix` | `form.wizard.step_advanced_count`, `form.rich_text.editor_mounted_count`, `form.signature_pad.canvas_leak_count`, `form.product_tour.completion_rate` | `git revert sp32-*` clean |
| SP33 | 10 L2 + 1 new rule + 2 snapshots | `/ax-verify-L2`, `/ax-verify-react` | `templates/_tests/saved-view-persistence.spec.ts` | `npx vitest run + npx eslint --fix` | `table.advanced_filter.applied_count`, `table.saved_view.loaded_count`, `table.bulk_export.invoked_count`, `table.tree_table.depth_violation_count` | `git revert sp33-*` clean |
| SP34 | 10 L2 + 2 L3 + 1 new rule + 2 snapshots + in-place app-shell.tsx edit | `/ax-verify-L2`, `/ax-verify-L3`, `/ax-verify-react` | `templates/_tests/impersonation-banner.spec.ts` | `npx vitest run + npx eslint --fix` | `admin.impersonation.started_count`, `admin.impersonation.banner_missing_count`, `admin.settings.preference_changed_count`, `admin.audit_log.viewed_count`, `a11y.skip_link.activated_count` | `git revert sp34-*` clean |

---

## §6 Autonomous Execution Safety

### §6.1 Rollback boundary per SP

Each SP commits as a single squashed commit at SP close. `git revert <sp-merge-commit>` is the atomic rollback. SP30 (atomic Spec-Trio billing) and SP31 (atomic backend_only Spec-Trio identity-verification) are highest-risk: each commit MUST include Spec Trio YAMLs + allowlist append + backend/L4 dirs + new rules in a single atomic commit. Pre-merge hook (`skills/ax-verify/scripts/atomic-spec-trio-check.sh`, existing from SP26) blocks partial atomic commits.

### §6.2 Shared-artifact ownership matrix (iter-2 expanded per Critic Blocker 4)

| Shared artifact | SP30 | SP31 | SP32 | SP33 | SP34 | Conflict policy |
|---|---|---|---|---|---|---|
| `templates/L1/components/*` | WRITE (3 new) | WRITE (1 new) | WRITE (4 new) | — | — | Disjoint files; parallel-safe. |
| `templates/L2/blocks/*` | WRITE (5 new) | WRITE (1 new) | WRITE (6 new) | WRITE (10 new) | WRITE (10 new + in-place app-shell.tsx) | Disjoint files except app-shell.tsx (sole writer = SP34). |
| `templates/L3/pages/*` | WRITE (1 new pricing-page) | — | — | — | WRITE (2 new: audit-log-page, admin-overview-page) | Disjoint. |
| `templates/L4/*` | WRITE (new billing/) | — | — | — | — | Sole writer. |
| `templates/backend/*` | WRITE (new billing/) | WRITE (new identity-verification/) | — | — | — | Disjoint dirs. |
| `specs/*`, `contracts/*`, `blueprints/*` | WRITE (billing trio — landed) | WRITE (identity-verification trio — landed) | — | — | — | Disjoint files; SP30 and SP31 run sequentially OR SP31 starts after SP30 commit lands. |
| `practices/evals/trio_integrity_allowlist.yaml` | WRITE (append `billing: full_trio`) | WRITE (append `identity-verification: backend_only`) | — | — | — | **Append-only; check HEAD before commit; halt threshold = 3 consecutive same-file conflicts → ESCAPE valve.** |
| `practices/rules/*.md` | WRITE (4 new Java) | WRITE (1 new Java) | — | — | — | Disjoint. |
| `practices-react/rules/*.md` | WRITE (2 new) | WRITE (2 new) | WRITE (1 new) | WRITE (1 new) | WRITE (1 new) | Disjoint files. |
| `practices/upstream/_MANIFEST.yaml` | WRITE (add 2 snapshots: stripe-billing, toss-billing) | WRITE (add 3 snapshots: nts-business-reg, kisa-identity-verification, pipa-article-24) | — | — | WRITE (add 2 snapshots: next-themes, wcag-22-techniques) | **Iter-2 NEW (Critic Blocker 4): append-only; check HEAD before commit; serialize on conflict. SP30 and SP31 run sequentially (SP30 → SP31). SP34 runs after SP31/SP32/SP33 close. Halt threshold = 3 consecutive same-file conflicts → ESCAPE valve at `docs/superpowers/escape/p1-<sp>-manifest-yaml-conflict.yaml`.** |
| `practices-react/upstream/_MANIFEST.yaml` | — | — | WRITE (add 3 snapshots: tiptap, signature-pad, shepherd-js) | — | — | **Iter-2 NEW: sole writer SP32; no race.** |
| `practices/upstream/*.snapshot.md` | WRITE (2 new) | WRITE (3 new) | — | WRITE (2 new — tanstack-table, react-aria-components) | WRITE (2 new) | Disjoint files. |
| `practices-react/upstream/*.snapshot.md` | — | — | WRITE (3 new) | — | — | Sole writer. |
| `practices/AGENTS.md` | WRITE (sha256 re-baseline — rules added) | WRITE (sha256 re-baseline) | — | — | — | **Iter-2 NEW (Critic Blocker 4): sentinel regen race. SP30 and SP31 sequentially update; final sha256 written at each SP's commit. Halt threshold = 3 consecutive same-file conflicts → ESCAPE.** |
| `practices-react/AGENTS.md` | WRITE (sha256 re-baseline) | WRITE (sha256 re-baseline) | WRITE (sha256 re-baseline) | WRITE (sha256 re-baseline) | WRITE (sha256 re-baseline) | **Iter-2 NEW: sentinel regen race. SP30 → SP31 → SP32 ‖ SP33 sequential ordering; SP32 and SP33 must serialize their AGENTS.md write (one of them commits first; the other rebases and re-baselines). Halt threshold = 3 consecutive same-file conflicts → ESCAPE.** |
| `templates/DECISIONS.md` | WRITE (TD-ADRs append) | WRITE (TD-ADRs append) | WRITE (TD-ADRs append) | WRITE (TD-ADRs append) | WRITE (TD-ADRs append) | **Iter-2 NEW: TD-ADR append-only; check HEAD before commit. Each SP rebases against HEAD before committing.** |
| `templates/L2/blocks/app-shell.tsx` | — | — | — | — | WRITE (in-place edit: insert SkipLink + AnnounceLive) | Sole writer; single-hunk in-place edit. |

**Race-safe protocol for shared sentinels (`_MANIFEST.yaml`, `AGENTS.md`, `DECISIONS.md`, `trio_integrity_allowlist.yaml`):**

1. Agent rebases against `origin/main` immediately before commit.
2. Agent uses `yq`-sorted insertion for YAML appends; deterministic sha256 regen for AGENTS.md.
3. Pre-merge hook (existing) verifies file consistency.
4. If first commit attempt conflicts, agent re-rebases and retries up to 2 times.
5. **Halt threshold (Critic Blocker 4):** 3 consecutive same-file conflicts → halt SP, write ESCAPE record to `docs/superpowers/escape/p1-<sp>-<reason>.yaml` documenting blocker inventory + maintainer acknowledgement required.

### §6.3 Stale-state invalidation rule

If any SP fails its `/ax-verify-*` exit-0 acceptance, the SP is reverted and the next SP does NOT begin until the failed SP either lands successfully on retry OR is explicitly skipped with a documented Open Question entry in §9. Parallel SPs (SP31/SP32/SP33) all halt if any one of them fails until resolved.

### §6.4 Halt thresholds (iter-2 expanded)

- **Halt on:** 3 consecutive guard failures in any single SP → halt all SPs, escalate to maintainer.
- **Halt on:** `practices/evals/trio_integrity_allowlist.yaml` diff churn (modified by any agent other than SP30 or SP31's atomic-commit agent) → halt, escalate.
- **Halt on:** new failing fixture passes (`fail_*/` dir exits 0) → halt, escalate (means rule is not actually enforced).
- **Halt on:** Tier-1 count diff (any SP attempts to create `skills/ax-<new-name>/SKILL.md`) → halt, escalate (cap violation).
- **Iter-2 NEW (Critic Blocker 4):** 3 consecutive same-file conflicts on any shared sentinel (`practices/upstream/_MANIFEST.yaml`, `practices-react/upstream/_MANIFEST.yaml`, `practices/AGENTS.md`, `practices-react/AGENTS.md`, `practices/evals/trio_integrity_allowlist.yaml`, `templates/DECISIONS.md`) → halt SP, write ESCAPE record at `docs/superpowers/escape/p1-<sp>-<reason>.yaml`.

### §6.5 ESCAPE valve (iter-2 tightened per Critic Soft Suggestion 5)

If after 2 ralplan iterations the PRD still has unresolved Critic blockers, the maintainer may invoke `/ralplan --escape` to lock the current iter as draft-final and proceed to execution with **explicit residual-risk record**: ESCAPE record at `docs/superpowers/escape/p1-<sp>-<reason>.yaml` MUST include:

- Blocker inventory (full list of unresolved blockers by id).
- Maintainer acknowledgement (named maintainer + ISO-8601 timestamp).
- Rollback plan (which commits to revert if the residual risk materializes).
- Re-review trigger (named follow-up PRD or round in which the residual must be resolved).

ESCAPE valve cannot bypass unresolved critical blockers without all four fields populated.

### §6.6 Cross-stack dependency declaration

SP30 (billing L4) **does not depend on** SP6 payment domain code beyond pattern reuse (enforced by `no-billing-cross-import-from-payment` rule — Critic Steelman 1). SP31 (identity-verification) **does not depend on** SP30 billing. SP32/SP33/SP34 are independent of SP30/SP31. The only hard sequences are:
1. SP30 → SP31 (allowlist + manifest serialization).
2. SP31/SP32/SP33 → SP34 (SP34 wires app-shell.tsx after parallel SPs close).

---

## §7 Pre-mortem (DELIBERATE mode — 5 scenarios)

### Scenario 1 — Billing provider abstraction leaks Stripe-specific event types

**Failure mode:** SP30 ships `BillingProvider` interface but canonical `BillingEvent` accidentally carries Stripe-specific fields. Forks using Toss Payments adapter discover the leak only after first staging webhook. Composition kit invariant violated.

**Detection:** Adapter contract test `BillingProviderContractTest` verifies both Stripe AND Toss sample webhooks produce **identical** canonical BillingEvent shape (no provider-only fields).

**Mitigation (executable):**
1. `BillingEvent.java` is a sealed record with exhaustive fields: `eventId, externalEventId (opaque), type (enum), occurredAt, subscriptionId, invoiceId (nullable), amountInMinorUnits, currencyCode, metadata (Map<String,String>)`.
2. Both adapters MUST translate provider payload to this shape; provider-specific data lives only in `metadata`.
3. Contract test asserts `assertEquals(stripeEvent.toCanonical().withoutMetadata(), tossEvent.toCanonical().withoutMetadata())`.

**Threshold:** Contract test fails → halt SP30; refactor `BillingEvent` shape.

### Scenario 2 — 사업자등록번호 checksum algorithm has multiple valid variants (iter-2 hardened per Critic Blocker 5)

**Failure mode:** SP31 ships `validateBusinessRegistration` based on one cited 국세청 algorithm, but real-world numbers fail validation because the canonical algorithm has corner cases. Korean enterprise fork-receivers report false negatives.

**Detection:** Snapshot `nts-business-reg-2026-05.snapshot.md` includes canonical algorithm AND test data set from **official/public sources only** — no mock data permitted (Critic Blocker 5):

- **National Tax Service (국세청) public test numbers** OR
- **Publicly registered business numbers** from open-data.go.kr 사업자등록번호 sample lists OR
- **KISA-published test vectors**.

TDD fixture path: `practices/evals/fixtures/business-registration-checksum/`:
- `pass/`: 5+ public business numbers with verified algorithm match (each entry includes source URL + license in fixture README).
- `fail_invalid_checksum/`: same numbers with mutated last digit (deterministic generation; mutations are traceable).
- `fail_format_violation/`: malformed inputs (letters, length errors).

**Mitigation (executable):**
1. SP31 deliverable includes `practices/evals/fixtures/business-registration-checksum/README.md` documenting data source URL + license per entry. Mock data explicitly prohibited.
2. Unit test runs all `pass/*` through `validateBusinessRegistration`; expected output = `true`. Tests run `fail_invalid_checksum/*` → `false`. Tests run `fail_format_violation/*` → `FormatViolationError`.
3. If any sample is misclassified, SP31 halts.
4. Snapshot URL pinned to `https://www.nts.go.kr/...` AND `https://www.data.go.kr/...` with quoted multiplier sequence `[1,3,7,1,3,7,1,3,5]`.

**Threshold:** ≥1 known-valid sample misclassified OR ≥1 known-invalid sample mis-passed → halt SP31.

### Scenario 3 — TipTap dynamic import breaks Next.js 16 RSC

**Failure mode:** SP32 ships `rich-text-editor.tsx` with `dynamic import` per the rule, but Next.js 16 App Router with Cache Components has known issues with `next/dynamic` in client-only contexts. Component crashes on first render.

**Detection:** Playwright integration test runs in CI against a Next.js 16 build; navigates to a page containing `<RichTextEditor>` and asserts editor mounts within 3s without console errors. Also: `cd frontend && npm run build` exit 0 against L4 page using `<RichTextEditor>`.

**Mitigation (executable):**
1. Snapshot `tiptap-v2-2026-05.snapshot.md` includes Next.js 16 RSC integration pattern (`"use client"` + `useEffect` + lazy state).
2. Component file uses explicit `"use client"` directive at top.
3. SSR fallback renders `<textarea>` placeholder until client hydrates.
4. Playwright assertion: editor reachable within 3s; no console error.

**Threshold:** Playwright fails OR `npm run build` exits non-zero → halt SP32; revert to client-only `"use client"` wrapper.

### Scenario 4 — RRN-protective rule false-positives on legitimate identity-verification flows

**Failure mode:** SP31 ships `no-rrn-collection-without-legal-basis` rule. Rule triggers on any field named `rrn`, `주민번호`, `id_number`, etc. SP30 billing or SP6 payment legitimately collect identity info via PASS/KCB CI/DI (which produces CI, not raw RRN). Rule incorrectly flags `identity-verification/IdentityVerificationDto.java` because DTO has field named `verifiedIdentityNumber`.

**Detection:** SP31 ships 2 fixtures: (a) `fail_rrn_no_legal_basis/` — raw RRN collection without legal basis → rule fires; (b) `pass_ci_di_verified/` — CI/DI fields with no raw RRN → rule does NOT fire.

**Mitigation (executable):**
1. Rule matcher uses explicit pattern: field name MUST match `/^(rrn|주민(등록)?번호|id_number|residentRegistrationNumber)$/` AND `@LegalBasis` annotation MUST be missing.
2. Field names `ci`, `di`, `verifiedIdentityNumber`, `external_id` are explicitly excluded.
3. Fixture `pass_ci_di_verified/` covers identity-verification DTO; expected: rule does NOT fire.

**Threshold:** `pass_ci_di_verified/` triggers rule → halt SP31; refine pattern.

### Scenario 5 — SP30 / SP31 atomic commit fails because trio_integrity_allowlist or _MANIFEST.yaml conflict

**Failure mode:** SP30 (or SP31) attempts to commit allowlist + manifest append but a concurrent operation creates a conflict. Atomic commit fails; partial state left on disk: billing L4 + backend exist but allowlist entry is missing OR manifest is missing a snapshot entry. trio_integrity_guard exits non-zero on next run.

**Detection (iter-2 expanded per Critic Blocker 4 and pre-mortem 5):** Pre-merge hook `skills/ax-verify/scripts/atomic-spec-trio-check.sh` verifies ALL of: spec YAML + contract YAML + manifest YAML + L4/backend dir + allowlist entry + new rules + `practices/upstream/_MANIFEST.yaml` snapshot list update + `practices/AGENTS.md` sha256 re-baseline are present in staged commit. If any is missing, commit is rejected.

**Mitigation (executable):**
1. SP30/SP31 agent rebases against HEAD before committing (per Catalog Extension §6.2 race-safe protocol).
2. `yq` sorted-insertion ensures deterministic allowlist + manifest position.
3. Pre-merge hook blocks partial atomic commits.
4. If conflict detected, agent halts, retries rebase, reapplies all SP artifacts as one commit cluster.
5. **Halt threshold (Critic Blocker 4):** 3 consecutive same-file conflicts on `trio_integrity_allowlist.yaml`, `practices/upstream/_MANIFEST.yaml`, `practices-react/upstream/_MANIFEST.yaml`, `practices/AGENTS.md`, `practices-react/AGENTS.md`, or `templates/DECISIONS.md` → halt SP and write ESCAPE record at `docs/superpowers/escape/p1-<sp>-<reason>.yaml`.

**Threshold:** 2 rebase-retries fail → halt SP and escalate; 3 same-file conflicts → ESCAPE.

---

## §8 ADR Template — TD-2026-05-19-NNN

| ADR | Topic | Decision (placeholder) |
|---|---|---|
| TD-2026-05-19-001 | Billing as 4th provider-abstracted L4 domain | Adopt Stripe + Toss Payments adapter pattern reusing SP6 payment provider abstraction; canonical BillingEvent shape; payment ↔ billing cross-import BLOCKED. |
| TD-2026-05-19-002 | SubscriptionStateMachine sole mutator | Adopt explicit state machine, archunit-guarded. |
| TD-2026-05-19-003 | currency-amount-precision integer-only | Adopt integer minor-unit storage. |
| TD-2026-05-19-004 | RRN-protective rule (anti-pattern) | Adopt `no-rrn-collection-without-legal-basis` as BLOCK rule with explicit field-name pattern + CI/DI exclusion. |
| TD-2026-05-19-005 | rich-content dynamic-import mandate | Adopt `rich-content-must-use-dynamic-import` ESLint rule. |
| TD-2026-05-19-006 | saved-view URL-state-or-server | Adopt `saved-view-must-be-url-state-or-server-persisted` rule. |
| TD-2026-05-19-007 | impersonation-banner mandate (iter-2 hardened) | Adopt `impersonation-banner-required-when-acting-as-other-user` rule matching canonical session `actingAs` state, not helper function name. |
| TD-2026-05-19-008 | a11y skip-link + announce-live as opt-in app-shell composables | SP34 in-place edit to `app-shell.tsx`; WCAG 2.4.1 + 4.1.3 enforced via Playwright. |
| TD-2026-05-19-009 | identity-verification as backend_only Spec Trio | **Iter-2 NEW (Critic Blocker 2)**: register `identity-verification: backend_only` per email-outbox / scheduled-task / ratelimit precedent. |
| TD-2026-05-19-010 | payment ↔ billing cross-import BLOCKED | **Iter-2 NEW (Critic Steelman 1)**: `no-billing-cross-import-from-payment` rule (archunit Java + ESLint React); failing fixture demonstrates violation; idempotency-key + ProblemDetail are pattern-shared, not code-imported. |

Each ADR follows the SP21/SP27 pattern: Decision, Drivers, Alternatives considered, Why chosen, Consequences, Follow-ups.

---

## §9 Open Questions (iter-2)

1. **Q1 — 본인인증 provider concrete adapter scope.** SP31 ships PASS + KCB stubs. SCI (NICE 신용평가) deferred to round-5. **Recommendation:** ship PASS + KCB only; document the abstraction-proving intent in SP31 README. **Risk:** if Korean banking fork-receivers report SCI is primary provider, round-5 must add it. Track in `.omc/plans/open-questions.md`.

2. **Q2 — Billing recurring task scheduling.** Webhook-triggered (provider-driven, no internal cron); fallback cron only if webhook fails. Decision deferred to SP30 architect review.

3. **Residual ambiguity (iter-2, max 2):**
   - **R1 — Q3 deferred to SP34 architect review.** Theme switcher SSR strategy: Cookie-based (recommended) vs `"use client"` + `useEffect` mount-gate. Iter-2 recommends Cookie-based per next-themes idiomatic pattern.
   - **R2 — SP30 wall-time risk.** Iter-2 collapsed iter-1 SP30+SP31 into single atomic SP30 (~3 d wall-time, 5 workers). If atomic commit cluster cannot be authored in 3 d due to coordination overhead, fallback is to ship SP30 in 2 rebase-iterations within the same SP (not splitting into 2 SPs). Monitored via §6.4 halt thresholds.

---

## §10 Honored Constraints (cross-check vs CLAUDE.md + prior PRDs)

| Constraint | Source | Iter-2 compliance |
|---|---|---|
| Composition-kit framing | CLAUDE.md §Vision Principle 3 | All 5 SPs ship catalog atoms. ✓ |
| Java + React equal partners | CLAUDE.md §Vision Principle 1 | SP30 full-stack; SP31 full-stack; SP32–SP34 frontend-focused. ✓ |
| Catalog expansion is normal | CLAUDE.md §Vision Principle 2 | +46 catalog atoms. ✓ |
| Spec-before-code, evidence-anchored | Functional Ext §1 P2 | SP30 atomic Spec Trio + SP31 atomic backend_only Spec Trio. ✓ |
| Binary verification | PRD-1 §1 P3 | Every SP terminates on `/ax-verify-* exit 0` + observability_signal. ✓ |
| Tier-1 cap = 4 | Functional Ext §1 P4 | Zero new Tier-1 skills. ✓ |
| Atomic Spec-Trio | Functional Ext Critic Blocker 4 | **Iter-2 (Critic Blocker 1): SP30 atomic billing + SP31 atomic identity-verification. No `.draft` loophole.** ✓ |
| No GitHub Actions workflows | Functional Ext §11 | Out of scope. ✓ |
| No LICENSE/CONTRIBUTING/docs-site | Functional Ext §11 | Out of scope. ✓ |
| No major architectural changes | This PRD §3 | No skill-topology overhaul. ✓ |
| No raw RRN input | Audit Critic verdict | SP31 ships RRN-protective rule only. ✓ |
| No new MockMvc tests | Functional Ext Critic Blocker 6 | RestAssured only. ✓ |
| Korean enterprise specificity | Audit Critic verdict | SP31 absorbs 사업자등록번호 + 본인인증 with public/official fixture data. ✓ |
| 4-iter ralplan APPROVE pattern | Functional Ext consensus | Iter-2 in progress. ✓ |
| Race-safe trio_integrity_allowlist | Catalog Ext §6.2 | SP30 + SP31 rebase-before-commit; yq-sorted insertion; halt threshold = 3 conflicts. ✓ |
| No new sibling BaseEntity class | Functional Ext Critic Blocker 3 | Billing entities extend existing BaseEntity. ✓ |
| **Iter-2 NEW: backend_only Spec Trio for new backend domains** | Critic Blocker 2 | SP31 registers `identity-verification: backend_only`. ✓ |
| **Iter-2 NEW: observability_signal column on Verification Matrix** | Critic Blocker 3 | §5.8 has the column for every SP. ✓ |
| **Iter-2 NEW: shared-artifact ownership for sentinels** | Critic Blocker 4 | §6.2 expanded with _MANIFEST.yaml + AGENTS.md + DECISIONS.md rows + halt thresholds. ✓ |
| **Iter-2 NEW: public/official fixture for business-reg checksum** | Critic Blocker 5 | SP31 fixtures use 국세청 + open-data.go.kr public sources. ✓ |
| **Iter-2 NEW: SP-card risks = owner + command + threshold + recovery** | Critic Blocker 6 | All 5 SPs normalized. ✓ |
| **Iter-2 NEW: payment ↔ billing boundary** | Critic Steelman 1 | §5.2.6 explicit boundary; new `no-billing-cross-import-from-payment` rule. ✓ |

---

## §11 Out-of-Scope (explicit)

- GitHub Actions workflows (`/.github/workflows/`)
- LICENSE / CONTRIBUTING.md / CODE_OF_CONDUCT.md / SECURITY.md
- Public docs site / GitHub Pages / README hero rewrite for public release
- Dependabot config / release.yml / npm publish workflow
- v2 architectural changes: skill topology, Spec Trio schema, ax-template-meta shape, Tier-1 cap changes
- P2 catalog items (deferred to round-5+ pending fork-receiver evidence): tree-view, mention, color-picker, cropper, carousel, toggle, context-menu, drawer (vaul), rating, pin-input, time-picker, raw phone-input-kr, raw rrn-masked-input, breadcrumb, menubar, navigation-menu, L1 pagination, chat-composer, comments-thread, inbox-list, funnel-chart, heatmap extensions, download-button, image-preview-grid, attachment-list, role-editor, permission-matrix, inbox-page, bulk-edit-page, advanced-search-page, landing-page, L4 messaging, L4 analytics-tracking
- L4 `subscription-management` as separate domain — **absorbed into billing L4 in SP30**; removed from deferred list (Critic K resolution).
- New language support (Kotlin / Go / Rust)
- /react-best-practices ESLint plugin npm publish (separate workstream)

---

## §12 ADR-ready commit content (for Step 6 if APPROVE)

- **Decision:** Approve P1 Absorption PRD iter-2 as canonical for Round 4 / SP30–SP34. Atomic billing full_trio + atomic identity-verification backend_only + Forms/Rich-Content + Tables/Filters advanced + Admin/Settings polish.
- **Drivers:** Close audit P1 residuals filtered by L4-unblock × frequency-of-use; preserve composition-kit self-discoverability; honor atomic Spec-Trio rule strictly (no `.draft` loophole); preserve Tier-1 cap; honor RRN-protective Critic verdict; bind payment ↔ billing boundary via new `no-billing-cross-import-from-payment` rule; ensure parallel SP execution is sentinel-race-safe.
- **Alternatives considered:** Option A (catalog-wide sweep), Option C (Korean-only), Option D (admin-only polish), Option E (defer-all-P1), **Option F (NEW — strict atomic billing + defer polish)** — all rejected with documented rationale.
- **Why chosen:** Option B+C hybrid produces a fork-visible new L4 domain (billing) atomically + Korean specials cluster (with `backend_only` Spec Trio) + targeted polish without speculative absorption. 5 SPs (iter-2 collapsed from iter-1's 6 SPs per Critic Blocker 1) is the right granularity; atomic Spec-Trio for billing AND identity-verification.
- **Consequences:** +46 catalog atoms (~7 L1, ~26 L2, ~3 L3, ~1 L4, ~2 backend dirs, ~2 Spec Trios — 1 full_trio + 1 backend_only, ~12 rules Java+React); 5 SPs over ≈ 8–10 d wall-time; v1.2.0-p1-absorbed tag at SP34 close; remaining P1 deferred to round-5+ pending fork-receiver evidence; Tier-1 count stays at 4; payment ↔ billing cross-import BLOCKED; sentinel race surfaces (_MANIFEST.yaml, AGENTS.md, DECISIONS.md) explicitly serialized.
- **Follow-ups:** Promote `/ax-verify policy-check` to highlight new rules; refresh `practices/upstream/_MANIFEST.yaml` with new snapshots; re-baseline AGENTS.md sha256; NICE 신용평가 (SCI) adapter to round-5 pending fork-receiver demand.

---

## §13 Iteration log

- **Iter 1 (2026-05-18):** Planner authored, Architect ITERATE, Codex Critic ITERATE (6 hard blockers + 1 independent steelman + 5 soft suggestions). Open Questions: 3.
- **Iter 2 (2026-05-18, this revision):** Applies 6 hard blockers + 1 steelman.
  - Blocker 1 (atomic Spec-Trio): SP30+SP31 collapsed into single atomic SP30 (5 SPs total, down from 6). §1, §5.2, §5.8 revised.
  - Blocker 2 (identity-verification backend_only): Added `specs/identity-verification-l0.yaml` + contract + manifest + allowlist entry + `/ax-verify-domain identity-verification` row. §3 O2, §4 P1-17a, §5.3, §5.8, §10.
  - Blocker 3 (observability_signal column): Added to §5.8 Verification Matrix for every SP with concrete Micrometer counters + audit events.
  - Blocker 4 (shared-artifact ownership + halt thresholds): §6.2 expanded with _MANIFEST.yaml, AGENTS.md, DECISIONS.md rows; §6.4 halt threshold added; ESCAPE valve tightened.
  - Blocker 5 (public/official fixture data): SP31 TDD anchor replaced mock data with `practices/evals/fixtures/business-registration-checksum/{pass,fail_invalid_checksum,fail_format_violation}` from 국세청 + open-data.go.kr public sources. Pre-mortem Scenario 2 expanded.
  - Blocker 6 (SP-card risks → owner + command + threshold + recovery): All 5 SPs normalized risks.
  - Steelman 1 (payment vs billing boundary): §5.2.6 new section + new rule `no-billing-cross-import-from-payment` (P1-14a) + §10 honored constraint + TD-2026-05-19-010 ADR.

---

## §14 End of PRD iter-2

**Line count:** ~810 lines (target +100–200 lines vs iter-1's 610; achieved +200).
**SP count:** 5 (SP30–SP34; iter-1 was 6).
**Branch (when execution starts):** `feat/p1-absorption-sp30-sp34`.
**Tier-1 cap:** 4 (unchanged).
