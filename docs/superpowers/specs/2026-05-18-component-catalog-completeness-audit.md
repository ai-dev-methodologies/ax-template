# Component Catalog Completeness Audit — 2026-05-18

> Source-of-truth audit: enumerate **every component** that can plausibly live
> inside the ax-template composition kit (Next.js 16 App Router + Spring Boot
> 3.2 + Java 21), grounded in the actual repo state as of 2026-05-18.
> Reviewed against PRD `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.md`
> (especially §4.1–§4.4 and §4.11 Layer Membership Decision Table).
> Output is consumed by Codex Critic before any new SP launches.

## Preamble — Assumptions

A1. **"Complete catalog" is a moving target.** This audit defines a v1 ceiling.
    Korean enterprise SaaS / B2B admin / consumer apps for the next ~12 months
    are the addressable market; specialized verticals (IoT dashboards, 3D
    canvases, in-browser DAW, etc.) are out-of-scope.

A2. **Composition-kit framing wins over single-product framing.** Every
    addition below MUST defend itself against the CLAUDE.md vision:
    catalog growth is normal but speculative generality is forbidden.
    Each P0 item is justified by either (i) a current/imminent reference
    workload (SP10/SP11/SP12 deliverable), (ii) a hard gap that breaks
    an L4 user-visible flow today, or (iii) a Korean enterprise hard
    requirement.

A3. **Layer assignment follows PRD §4.11 Layer Membership Decision Table.**
    When ambiguous, the audit cites the closest existing row and explains
    the analogy.

A4. **Backend `templates/backend/**` is currently empty (`.gitkeep` only).**
    PRD §4.5 reserves 10 cross-cutting slots; SP3 carved the directory,
    SP-TBD must actually populate it. The audit treats this as a P0 gap
    inherited from PRD (not a new ask).

A5. **Counts in the brief vs. counts on disk.** The team-lead brief states
    "64 Java rules"; the repo currently has **68** Java rules under
    `practices/rules/` and **70** React rules under
    `practices-react/rules/`. The audit defers to disk reality.

A6. **`/ax-` skills (3 Tier-1 + 8 Tier-2 + 6 Tier-3) are stable and
    quality-graded.** Skill gaps below are limited to **workflow** holes
    (scaffolding, fork-handoff, doctor, regression), not new verify axes.

A7. **Anti-template policy (CLAUDE.md `web/design-quality.md`) is binding.**
    Recommendations avoid generic shadcn dashboard primitives that
    already exist; they target high-leverage **patterns** the catalog
    can't cleanly produce today.

A8. **Korean enterprise specifics matter.** 도로명/지번 address search, RRN
    masking (주민번호), 사업자등록번호 입력+검증, 휴대폰 본인인증 (CI/DI),
    원화 currency formatting, 한글 IME composition handling are all
    first-class for the target market.

A9. **MUST/SHOULD/MAY uses RFC 2119 + Spec Trio §4.8 semantics.**
    - P0 = MUST (catalog cannot be called "complete" without this)
    - P1 = SHOULD (production-grade fork blocked without this)
    - P2 = MAY (specialized; add on first concrete fork ask)

A10. **Effort estimates** (S/M/L) map to the PRD §5.bonus revised effort
    table: **S** = ≤4 h, **M** = ½–1 d, **L** = 1–3 d, **XL** = ≥3 d.

---

## Executive summary

**Current catalog (grounded — 2026-05-18 disk state)**

| Layer / Surface | Path | Current count |
|---|---|---|
| L1 UI primitives | `templates/L1/components/*.tsx` | 32 |
| L2 feature blocks | `templates/L2/blocks/*.tsx` | 26 |
| L3 page templates | `templates/L3/pages/*/` | 7 |
| L4 domain workloads | `templates/L4/*` | 1 sealed (`auth`) + 1 in flight (`crud`) |
| Backend cross-cutting | `templates/backend/**` | **0** (PRD §4.5 budgets 10) |
| Backend domain code | `backend/src/main/java/com/ax/template/authblueprint/*` | 7 packages (auth, crud, payment, practices, ratelimit, security, user) |
| Java practices rules | `practices/rules/*.md` | 68 |
| React practices rules | `practices-react/rules/*.md` | 70 |
| `/ax-` skills | `skills/ax-*` | 17 (3 + 8 + 6) |
| Frontend Spec Trios shipped | `specs/*-frontend-l0.yaml` | 2 (auth, crud) |

**After this audit's recommendations**

| Surface | Add P0 | Add P1 | Add P2 | New total ceiling |
|---|---|---|---|---|
| L1 primitives | 8 | 11 | 9 | 60 |
| L2 feature blocks | 14 | 18 | 13 | 71 |
| L3 page templates | 6 | 6 | 4 | 23 |
| L4 domain workloads | 2 (payment, practices) — already pending in SP10/SP11 | 4 (notification, audit-log, settings, search) | 3 (billing, admin-impersonation, file-storage admin) | 9 |
| Backend cross-cutting | 10 (PRD §4.5 budget, currently 0) | 6 | 5 | 21 |
| Backend domain templates | 5 (notification, audit-log, file-storage, email-outbox, scheduled-task) | 3 (search-index, integration-webhook, batch-job) | 2 (event-sourcing skeleton, saga skeleton) | 17 |
| Java rule additions | 9 | 8 | 5 | 90 (cap-respected ≤30) |
| React rule additions | 11 | 9 | 6 | 96 (cap-respected ≤30) |
| Skill additions | 4 | 3 | 1 | 25 |
| Spec Trios | 2 (notification, settings frontend_only) | 3 (search, audit-log, file-storage) | 2 | 9 |

**Totals — P0/P1/P2 counts**

- **P0 (must add before catalog is "complete")**: **71 items**
- **P1 (production-grade fork)**: **74 items**
- **P2 (specialized)**: **49 items**

**Top 10 P0 items (ranked by L4 unblock × frequency-of-use)**

1. **Backend cross-cutting templates** — populate `templates/backend/**` per PRD §4.5 (10 files). Currently empty; blocks every SP that promises "fork → working backend".
2. **L1 `combobox.tsx`** — required by FilterBar, ColumnPicker, AdvancedFilterBuilder, plus every domain that ships a typeahead. shadcn ships a canonical version.
3. **L1 `date-picker.tsx`** + **`date-range-picker.tsx`** — required by any payment / audit-log / refund-window UI. PRD §4.11 ambiguity: lives at L1 (purely visual), domain logic goes to L2.
4. **L1 `file-upload.tsx` (Dropzone)** — required by audit attachments, KYC docs, payment receipt upload, profile avatar. Hard to wrap retroactively in L2.
5. **L1 `address-search.tsx` (도로명/지번 picker)** — Korean enterprise hard requirement; no off-the-shelf shadcn equivalent. Wraps Daum/Kakao 우편번호 widget behind a controlled-component interface.
6. **L2 `notification-bell.tsx` + `notification-list.tsx` + `toast-queue.tsx`** — every domain emits notifications; not modeled today. Backend-domain pairing required (see backend P0 #2).
7. **L2 `error-boundary.tsx` + `offline-banner.tsx`** — production-grade hardening; `error-page` L3 exists but no reusable runtime boundary.
8. **L2 `virtualized-table.tsx` + advanced sort/group plumbing for `data-table`** — current `data-table.tsx` is non-virtualized; breaks at >2k rows. Audit-log / payment-event-ledger L4 will hit this immediately.
9. **L4 `payment` + L4 `practices`** — already pending in SP10/SP11. Listed to make the catalog completeness count honest.
10. **Backend domain skeletons** — `templates/backend/notification/`, `audit-log/`, `file-storage/`, `email-outbox/`, `scheduled-task/`. PRD §4.5 only enumerated cross-cutting; per-domain skeletons (mirror of `practices-react/` for frontend) are absent.

**Suggested SP plan (SP13 onward)**

Grouped to honor the existing SP9 → SP10/SP11 → SP12 fan-in. New SPs slot in
**after SP12 only** (per CLAUDE.md anti-pattern guardrails — no parallel
chaos). Detail in §"Suggested SP plan" below.

**Audit file path**: this file (`docs/superpowers/specs/2026-05-18-component-catalog-completeness-audit.md`)
**Expected line count**: ~1700 lines.

---

## Dimension A — L1 primitives gap (current 32 → recommended 60)

### A.0 Methodology
- Walked `templates/L1/components/*.tsx` (32 files, all shadcn-derived with
  `evidence:` blocks per `practices-react/upstream/shadcn-ui-2026-05.snapshot.md`).
- Cross-referenced canonical shadcn registry (`practices-react/upstream/shadcn-registry-2026-05.snapshot.md`).
- Cross-referenced canonical Radix UI primitive list.
- Filtered against (i) Korean enterprise hard requirements, (ii) L2 blocks
  that currently compose missing primitives (e.g., `filter-bar.tsx` re-implements
  a `<Combobox>` inline because L1 lacks one).

### A.1 Current 32 — for the record (cite)
`accordion`, `alert`, `alert-dialog`, `aspect-ratio`, `avatar`, `badge`,
`button`, `card`, `checkbox`, `collapsible`, `command`, `dialog`,
`dropdown-menu`, `form`, `hover-card`, `input`, `label`, `popover`, `progress`,
`radio-group`, `resizable`, `scroll-area`, `select`, `separator`, `sheet`,
`skeleton`, `slider`, `sonner`, `switch`, `tabs`, `textarea`, `tooltip`.
Files at `templates/L1/components/{accordion.tsx … tooltip.tsx}`.

### A.2 Recommended additions

| Component | Priority | Justification | Backing evidence | Effort |
|---|---|---|---|---|
| `combobox.tsx` | **P0** | shadcn ships canonical Combobox (Popover + Command + Input). Required by FilterBar, ColumnPicker, AddressSearch, every typeahead. `templates/L2/blocks/search-input.tsx` currently rolls a poor-man's version. | shadcn `combobox`; Radix Popover + cmdk | S |
| `date-picker.tsx` | **P0** | Required by every domain emitting dates. shadcn ships `Calendar` (already in 32 via `calendar`? — actually NOT in current 32; see gap line below) + DatePicker recipe. | shadcn `date-picker`; react-day-picker v9 | S |
| `calendar.tsx` | **P0** | Building block for DatePicker / DateRangePicker. Missing from current 32 — a real omission (PRD §4.1 assumed it). | shadcn `calendar`; react-day-picker v9 | S |
| `date-range-picker.tsx` | **P0** | Required by audit-log filters, payment refund-window pickers, report runners. | shadcn `date-range-picker` recipe | S |
| `file-dropzone.tsx` | **P0** | Drag-and-drop file upload primitive. Required by KYC, attachments, avatar, payment receipt upload. shadcn has `file-upload` block; react-dropzone is the canonical hook layer. | react-dropzone v14; shadcn `file-upload` block | M |
| `otp-input.tsx` | **P0** | 6-digit OTP input for 2FA / email verification / phone verification. shadcn ships `input-otp`. Auth domain currently can't render `MFA` step without this. | shadcn `input-otp`; input-otp lib | S |
| `kbd.tsx` | **P0** | Keyboard hint chip (`<kbd>⌘K</kbd>`). Required by SearchPalette and any shortcut surface. Trivial but visible everywhere; absence is a design tell. | shadcn `kbd` (community); MDN `<kbd>` | XS |
| `address-search.tsx` | **P0** | Korean enterprise hard req — 도로명/지번 주소 검색. Wraps Daum/Kakao 우편번호 widget behind a controlled-component interface. No shadcn equivalent. | 카카오 우편번호 서비스 docs (https://postcode.map.daum.net/guide); internal_design | M |
| `phone-input-kr.tsx` | **P1** | Korean 휴대폰번호 입력 마스크 (010-XXXX-XXXX), with intl-mode toggle. Wraps shadcn `input` + libphonenumber-js. | libphonenumber-js; KISA 휴대폰번호 표준 | S |
| `business-registration-input.tsx` | **P1** | 사업자등록번호 입력 마스크 + checksum 검증 (XXX-XX-XXXXX). B2B SaaS hard requirement. | 국세청 사업자등록번호 검증 알고리즘 (https://www.nts.go.kr) | S |
| `rrn-masked-input.tsx` | **P1** | 주민등록번호 입력 — front 6 digits + last digit only; rest masked. PIPA compliance pattern. Comes with explicit `redact_on_blur` prop. | 개인정보보호법 § 24 (RRN 마스킹 가이드라인) | M |
| `currency-input.tsx` | **P1** | 원화 입력 (3-digit grouping, ₩ prefix, no decimal by default — extensible to JPY/USD). Wraps `react-number-format`. | ISO 4217 + `react-number-format` v5 | S |
| `number-input.tsx` (spinner) | **P1** | Numeric stepper with +/- buttons, min/max/step. Used by quantity selectors, pagination size, retry budgets. shadcn has no canonical version but `react-aria-components` does. | react-aria-components `NumberField`; shadcn community recipe | S |
| `time-picker.tsx` | **P1** | HH:mm:ss picker. Required by scheduled-task forms, batch-job windows. Composes Select × 3. | shadcn `time-picker` recipe | S |
| `range-picker.tsx` (slider range) | **P1** | Two-thumb range slider — Radix has it; shadcn ships `slider` single-thumb only. Required by payment-amount-range filter. | Radix `Slider`; shadcn `slider` extend | S |
| `pin-input.tsx` | **P1** | 4-digit PIN distinct from OTP (no resend/timer). Used by quick-lock / app-pin patterns. | input-otp (variant) | S |
| `breadcrumb.tsx` | **P1** | Used by every L3 page header. shadcn ships canonical `breadcrumb`. Currently missing — `app-header.tsx` re-implements a primitive crumb inline. | shadcn `breadcrumb` | S |
| `menubar.tsx` | **P1** | Application menu bar (File / Edit / View) — relevant for admin viewers, internal tools. shadcn ships canonical. | shadcn `menubar`; Radix Menubar | S |
| `navigation-menu.tsx` | **P1** | Mega-menu / multi-column nav. shadcn canonical. Used by marketing / settings IA. | shadcn `navigation-menu`; Radix NavigationMenu | S |
| `pagination.tsx` (L1 primitive) | **P1** | Note: `templates/L2/blocks/pagination.tsx` exists at L2 (correct — domain-aware). shadcn also ships `pagination` as L1 chrome (just the controls, no fetch). Add as L1 primitive; L2 wraps it. | shadcn `pagination` | S |
| `chart.tsx` (recharts wrapper) | **P2** | shadcn ships canonical `chart` wrapper. Foundation for charts at L2 (TimeSeriesChart, BarChart). | shadcn `chart`; recharts v3 | M |
| `carousel.tsx` | **P2** | embla-based carousel. Used by onboarding tours, image galleries. shadcn canonical. | shadcn `carousel`; embla-carousel-react | S |
| `toggle.tsx` + `toggle-group.tsx` | **P2** | Pressable toggle button (distinct from Switch which is form-binary). Radix has it. | shadcn `toggle`, `toggle-group`; Radix Toggle | S |
| `context-menu.tsx` | **P2** | Right-click menu. shadcn canonical. Used by data-table row actions. | shadcn `context-menu`; Radix ContextMenu | S |
| `drawer.tsx` (vaul) | **P2** | Mobile-friendly bottom drawer. shadcn canonical. Differentiated from `sheet`. | shadcn `drawer`; vaul | S |
| `rating.tsx` (star) | **P2** | Star rating for reviews. No shadcn canonical — community recipe. | a11y-rating recipes; ARIA APG | S |
| `signature-pad.tsx` | **P2** | Canvas signature capture for KYC / 전자결재 surfaces. | signature_pad lib | M |
| `rich-text-editor.tsx` (TipTap thin) | **P2** | TipTap headless wrapper; surface in L2 as content composer. Caveat: heavy; ship behind dynamic import per `practices-react/rules/bundle-dynamic-imports.md`. | TipTap v2 docs | L |

**Anti-bloat defenses**

- **No `tree-view`, `mention`, `color-picker`, `cropper`** at any priority.
  These are speculative for the target market (no current reference workload
  demands them). Add only on first concrete fork ask.
- **No second `tabs` variant** (e.g., "scrollable tabs") — extend props on
  existing `tabs.tsx` instead. Per CLAUDE.md "200 lines could be 50, rewrite it."
- **`chart.tsx` deferred to P2** despite being shadcn-canonical because
  `practices/rules/` has no observability/reporting rule yet and SP10–SP12
  don't ship charts. Promote to P1 the moment SP-audit-log starts.

**Totals A:** 8 P0 + 11 P1 + 9 P2 = **28 additions**. Catalog → 60.

---

## Dimension B — L2 feature blocks gap (current 26 → recommended 71)

### B.0 Methodology
- Walked all 26 blocks at `templates/L2/blocks/*.tsx`. Inspected `evidence:`
  blocks and `imports_from` / `imports_forbidden` headers to validate layer.
- For each candidate block, mapped it to (i) the L4 domain that needs it,
  (ii) the L1 primitive(s) it composes, (iii) the closest existing block
  it would extend, (iv) whether it deserves L2 or belongs in L3 per
  PRD §4.11.

### B.1 Current 26 — for the record (cite)
- **Auth (5)**: `login-form.tsx`, `signup-form.tsx`, `oauth-callback-panel.tsx`,
  `email-verify-panel.tsx`, `protected-route.tsx`
- **Layout (3)**: `app-shell.tsx`, `app-header.tsx`, `sidebar.tsx`
- **Data (7)**: `data-table.tsx`, `pagination.tsx`, `filter-bar.tsx`,
  `search-input.tsx`, `bulk-actions-bar.tsx`, `column-picker.tsx`,
  `empty-state.tsx`
- **CRUD (4)**: `crud-list-adapter.tsx`, `crud-create-form.tsx`,
  `crud-edit-form.tsx`, `crud-delete-confirm.tsx`
- **Payment (4)**: `payment-checkout-form.tsx`, `payment-method-picker.tsx`,
  `idempotency-key-handler.tsx`, `slow-provider-warning.tsx`
- **Common (3)**: `confirm-dialog.tsx`, `loading-boundary.tsx`, `toast.tsx`

### B.2 Recommended additions

#### B.2.1 Forms layer (currently missing as a category)

| Block | Priority | Justification | Layer rationale (per PRD §4.11) | Effort |
|---|---|---|---|---|
| `form-section.tsx` | **P0** | Captures `<fieldset>` + heading + description pattern. Repeated in `signup-form`, `crud-create-form`, `crud-edit-form`. DRY violation today. | Pure composition of `Label`/`Card`; no domain coupling → **L2** | S |
| `field-array.tsx` | **P0** | Dynamic repeating fields (e.g., line items in invoices, beneficiaries in payment). Reusable across CRUD/payment. | Wraps `react-hook-form` `useFieldArray`; props-only → **L2** | M |
| `conditional-field.tsx` | **P0** | Show/hide field based on watched RHF value. Currently re-implemented in each form. | Headless utility composing L1 → **L2** | S |
| `form-error-summary.tsx` | **P0** | A11y pattern: surface all field errors at top, focusable on submit-fail. WCAG 3.3.1. Required by accessibility audit. | A11y-bound presentation; no domain coupling → **L2** | S |
| `auto-save-indicator.tsx` | **P1** | "Saved 3s ago" indicator paired with debounced auto-save mutation. Useful in long forms. | Pure visual; data callback via props → **L2** | S |
| `dirty-guard.tsx` | **P1** | "You have unsaved changes" guard on route exit. Uses Next.js 16 `unstable_useUnsavedChangesWarning` (or fallback). | Hook + dialog; cross-cutting → **L2** | S |
| `dependent-field.tsx` | **P1** | Cascading selects (시 → 구 → 동). Korean address subdivisions, org-tree drilldowns. | Headless utility → **L2** | S |
| `form-stepper.tsx` | **P1** | Multi-step wizard chrome. Used by onboarding, KYC, complex CRUD. | Composes `Progress` + `Button` → **L2** (extracted from L3 multi-step-form page) | M |

#### B.2.2 Tables advanced

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `virtualized-table.tsx` | **P0** | Current `data-table.tsx` is non-virtualized; breaks at >2k rows. Audit-log L4 hits this immediately. Wraps `@tanstack/react-virtual`. | Same shape as `data-table`; orthogonal capability → **L2** | M |
| `expandable-row.tsx` | **P0** | Row-level disclosure (master/detail inline). Required by payment-event-ledger (events nested under payment). | Pure presentation; selection state via props → **L2** | S |
| `grouped-table.tsx` | **P1** | Group-by header rows. Used by report viewers. | Composes `data-table` + group reducer → **L2** | M |
| `tree-table.tsx` | **P1** | Hierarchical row tree (org chart, file tree). | Composes `data-table` + expand state → **L2** | M |
| `bulk-export.tsx` | **P1** | CSV/Excel/PDF export button cluster with progress. Pairs with backend `audit-log` job. | Composes `dropdown-menu` + progress; callback-only → **L2** | S |
| `saved-view.tsx` | **P1** | Save & restore table column/filter state to URL or backend. URL state pattern from CLAUDE.md `patterns.md`. | Hook + select; cross-cutting → **L2** | M |
| `column-reorder.tsx` | **P2** | Drag-to-reorder columns. Extends `column-picker.tsx`. | Same lane as existing column-picker → **L2** | M |
| `row-drag-handle.tsx` | **P2** | Drag-to-reorder rows. Used by ordering / priority lists. | dnd-kit thin wrapper → **L2** | M |

#### B.2.3 Filters advanced

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `advanced-filter-builder.tsx` | **P0** | AND/OR rule builder (field × operator × value). Required by audit-log L4 search. Current `filter-bar.tsx` is shallow. | Headless; rule schema via props → **L2** | L |
| `filter-chips.tsx` | **P0** | Active-filter chip row with remove ✕. Visible feedback layer for any filter UX. | Pure presentation → **L2** | S |
| `faceted-filter.tsx` | **P1** | Multi-select facet with counts (like e-commerce). Useful in CRUD-list facets. | Composes `popover` + `command` → **L2** | M |
| `saved-filters.tsx` | **P1** | Per-user saved filter presets. URL-state-aware. | Same pattern as `saved-view.tsx`; UI surface only → **L2** | S |
| `date-range-filter.tsx` | **P1** | Toolbar-style date range with quick presets ("Last 7d / 30d / Custom"). | Composes `date-range-picker` (L1) + presets → **L2** | S |

#### B.2.4 Charts / Viz

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `kpi-card.tsx` | **P1** | Big-number + delta + sparkline card. Used by every dashboard. Composes recharts via L1 `chart`. | Pure presentation; data via props → **L2** | S |
| `sparkline.tsx` | **P1** | Inline mini-chart inside table rows. Composes recharts. | Pure presentation → **L2** | S |
| `time-series-chart.tsx` | **P1** | Line chart over time. Used by payment-volume / audit-event-rate dashboards. Composes recharts. | Composes L1 `chart` → **L2** | M |
| `bar-chart.tsx` | **P1** | Bar chart. Same lane. | → **L2** | S |
| `pie-chart.tsx` | **P2** | Pie / donut. Less universally useful. | → **L2** | S |
| `funnel-chart.tsx` | **P2** | Funnel for conversion. | → **L2** | M |
| `heatmap.tsx` | **P2** | Calendar / matrix heatmap. | → **L2** | M |

#### B.2.5 File / Media

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `file-upload-area.tsx` | **P0** | Composes L1 `file-dropzone` + progress + cancel + retry. Used by KYC / attachments / receipts. | Composes L1; no backend coupling → **L2** | M |
| `attachment-list.tsx` | **P1** | List of uploaded files with download / remove. | → **L2** | S |
| `image-preview-grid.tsx` | **P1** | Grid of image previews with lightbox. | → **L2** | M |
| `download-button.tsx` | **P2** | Download with progress and abort. | → **L2** | S |

#### B.2.6 Communication / Notifications

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `notification-bell.tsx` | **P0** | Unread-count chip in app-header. Universal SaaS pattern. Pairs with backend `notification` domain (see Dimension E P0). | Cross-cutting; data callback only → **L2** | S |
| `notification-list.tsx` | **P0** | Notification dropdown / drawer body. | → **L2** | M |
| `toast-queue.tsx` | **P0** | App-wide toast manager wrapping sonner. Current `toast.tsx` is a single-instance wrapper; queue layer absent. | Cross-cutting → **L2** | S |
| `activity-feed.tsx` | **P1** | Reverse-chrono activity timeline (audit-log surface). | → **L2** | M |
| `comments-thread.tsx` | **P2** | Threaded comments with author chips. | → **L2** | L |
| `inbox-list.tsx` | **P2** | Email-style inbox list with unread state. | → **L2** | M |
| `chat-composer.tsx` | **P2** | Multi-line composer with toolbar. | → **L2** | M |

#### B.2.7 Errors / Diagnostics

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `error-boundary.tsx` | **P0** | React `ErrorBoundary` with fallback UI + reset + telemetry callback. L3 has `error-page` but no runtime block. | Cross-cutting; pure → **L2** | S |
| `offline-banner.tsx` | **P0** | Top banner on `navigator.onLine === false`. Universal. | Cross-cutting → **L2** | S |
| `maintenance-notice.tsx` | **P1** | Scheduled-maintenance banner with countdown. | → **L2** | S |
| `network-status-pill.tsx` | **P1** | Discreet pill in app-header showing online/offline/slow. | → **L2** | S |

#### B.2.8 Search

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `search-palette.tsx` (Cmd+K) | **P0** | App-wide command palette. Universal pattern; shadcn `command` is the L1 building block, but the palette shell (open trigger, recent searches, scoping) is L2. | Cross-cutting; result schema via props → **L2** | M |
| `typeahead-search.tsx` | **P1** | Header search box with debounced typeahead results. Extends `search-input.tsx`. | → **L2** | S |
| `result-highlighter.tsx` | **P1** | `<mark>` query terms in result text. | Pure presentation → **L2** | S |
| `recent-searches.tsx` | **P2** | Persist + render last N queries. | → **L2** | S |

#### B.2.9 Onboarding

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `onboarding-checklist.tsx` | **P1** | "Complete your profile (3/5)" checklist drawer. | Cross-cutting → **L2** | M |
| `empty-state-cta.tsx` | **P1** | Enhanced empty-state with primary action and visual. Extends `empty-state.tsx`. | Same lane → **L2** | S |
| `product-tour.tsx` | **P2** | Step-through tooltip tour. Wraps shepherd.js or react-joyride. | → **L2** | L |
| `welcome-modal.tsx` | **P2** | First-run welcome dialog. | → **L2** | S |

#### B.2.10 Settings / Preferences

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `settings-section.tsx` | **P1** | Heading + description + form-pair pattern (e.g., GitHub settings). Repeated across every settings page. | Pure composition → **L2** | S |
| `theme-switcher.tsx` | **P1** | Light/dark/system selector. Pairs with `next-themes`. | Cross-cutting → **L2** | S |
| `locale-switcher.tsx` | **P1** | ko-KR / en-US selector (Korean enterprise apps often serve both). | → **L2** | S |
| `preferences-form.tsx` | **P2** | Generic preferences form wrapper. | → **L2** | S |

#### B.2.11 Billing / Subscription

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `pricing-table.tsx` | **P1** | Plan comparison with feature rows. Used by upgrade flows; tightly paired with payment domain. | Pure presentation → **L2** | M |
| `usage-meter.tsx` | **P1** | Progress bar with quota label (e.g., "12k / 50k API calls"). | Pure presentation → **L2** | S |
| `invoice-list.tsx` | **P2** | Table of past invoices with download. | Composes `data-table` → **L2** | S |
| `plan-comparison.tsx` | **P2** | Feature × plan matrix. | → **L2** | M |

#### B.2.12 Admin

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `audit-log-view.tsx` | **P0** | Read-only audit-event row renderer with actor / action / target / diff. Paired with backend `audit-log` domain. | Cross-cutting; consumes `audit-log` API → **L2** | M |
| `impersonation-banner.tsx` | **P1** | "You are viewing as <user>" red banner. Security-critical UX. | Cross-cutting → **L2** | S |
| `feature-flag-toggle.tsx` | **P1** | Admin-only flag toggle. Pairs with backend `feature-flag` (currently absent — out of scope here). | → **L2** | S |
| `role-editor.tsx` | **P2** | RBAC role editor. | → **L2** | L |
| `permission-matrix.tsx` | **P2** | Role × permission matrix viewer. | → **L2** | M |

#### B.2.13 i18n / a11y

| Block | Priority | Justification | Layer rationale | Effort |
|---|---|---|---|---|
| `skip-link.tsx` | **P0** | WCAG 2.4.1 (Bypass Blocks). Required. Missing today. | a11y-bound → **L2** | XS |
| `announce-live.tsx` | **P0** | ARIA-live region for async announcements. Required for any toast/snackbar flow per WCAG. | a11y-bound → **L2** | S |
| `keyboard-shortcut-help.tsx` | **P1** | Dialog listing shortcuts (`?` keypress). Pairs with `kbd.tsx`. | → **L2** | S |
| `currency-formatter.tsx` | **P1** | `<CurrencyFormatter value={amount} currency="KRW" />`. Pairs with L1 `currency-input`. | Pure presentation → **L2** | XS |
| `number-formatter.tsx` | **P2** | Locale-aware number display. | → **L2** | XS |

**Totals B:** 14 P0 + 18 P1 + 13 P2 = **45 additions**. Catalog → 71.

**Anti-bloat defenses**

- **No CMS-style block builder, no whiteboard canvas, no MIDI/DAW**.
- **`chat-composer` / `comments-thread` deferred to P2** — only relevant if
  the target app has comms. Not in any current SP.
- **`product-tour` P2** — useful but pulls in a heavy library and a
  bespoke UX pattern. Add only on first concrete fork ask.

---

## Dimension C — L3 page templates gap (current 7 → recommended 23)

### C.0 Methodology
- Walked `templates/L3/pages/` (7 page templates with `README.md` slot
  contracts + `page.tsx` skeletons + `error.tsx` / `loading.tsx` /
  `not-found.tsx`).
- For each candidate, validated via PRD §4.11: L3 is a **page layout**
  with slot contract; the page is wired domain-side at L4.

### C.1 Current 7 — for the record (cite)
`auth-callback-page`, `create-page`, `dashboard-page`, `detail-page`,
`edit-page`, `error-page`, `list-page`. Files at
`templates/L3/pages/{auth-callback-page,create-page,…}/page.tsx`.

### C.2 Recommended additions

| Page template | Priority | Justification | Slot contract sketch | Effort |
|---|---|---|---|---|
| `wizard-page` | **P0** | Multi-step form flow shell. Used by onboarding, KYC, complex CRUD. Composes `form-stepper` L2. | `header / steps / step-content / footer-nav` | M |
| `settings-page` | **P0** | Two-column settings layout: nav left, sections right. | `settings-nav / settings-content` | M |
| `audit-log-page` | **P0** | Audit-log viewer: filter toolbar + virtualized table + drawer detail. | `toolbar / table / detail-drawer` | M |
| `search-results-page` | **P0** | Generic search page: query + facets + results. | `search-header / facets / results / pagination` | M |
| `empty-data-page` | **P0** | "Nothing here yet" — full-page empty state for first-run. | `header / illustration / cta` | S |
| `forgot-password-page` | **P0** | Auth-extras page family. Currently missing — auth domain can't ship password reset without it. | `header / form / footer` | S |
| `reset-password-page` | **P1** | Pair of forgot-password (carries token from email). | `header / form / footer` | S |
| `mfa-setup-page` | **P1** | TOTP enroll page. | `header / qr-code / verify-form / recovery-codes` | M |
| `account-locked-page` | **P1** | Locked / suspended account notice. | `header / message / contact-cta` | S |
| `import-csv-page` | **P1** | CSV import wizard page (file upload → mapping → preview → submit). | `header / steps / step-content` | L |
| `export-job-page` | **P1** | "Your export is being prepared" status page. | `header / status / download-link-when-ready` | S |
| `pricing-page` | **P1** | Marketing-style pricing page. SSR-friendly. | `hero / plans / faq / cta` | M |
| `landing-page` | **P2** | Generic marketing landing (hero / features / cta). Optional. | `hero / features / cta` | M |
| `advanced-search-page` | **P2** | Advanced filter builder full page (extends search-results-page). | `filter-builder / saved-filters / results` | M |
| `inbox-page` | **P2** | Three-pane mail-like layout (list / thread / detail). | `list / thread / detail` | L |
| `bulk-edit-page` | **P2** | Multi-select bulk-edit shell. | `selection-summary / form / submit-status` | M |

**Totals C:** 6 P0 + 6 P1 + 4 P2 = **16 additions**. Catalog → 23.

**Anti-bloat defense**

- **No `analytics-explorer-page`, `kanban-board-page`, `gantt-page`** —
  these are vertical-specialized.
- **`landing-page` P2** despite being common: ax-template's target is
  app-internal pages, not marketing surfaces; marketing should fork from
  Vercel templates if needed.

---

## Dimension D — Backend cross-cutting gap (current 0 → recommended 21)

### D.0 Methodology
- Walked `templates/backend/` — currently only `.gitkeep`. PRD §4.5 budgets
  10 cross-cutting templates but SP3/SP8/SP9 have not populated.
- Walked `backend/src/main/java/com/ax/template/authblueprint/**` (130+
  Java files across 7 packages) to identify patterns repeated 2+ times
  that warrant extraction.
- Cross-referenced against `practices/rules/*.md` (68 rules) — only
  templates that anchor to a rule (per PRD §4.10 evidence_guard) are
  admitted.

### D.1 PRD §4.5 baseline (10 slots — currently empty)

PRD §4.5: `templates/backend/controllers/`, `services/`, `repositories/`,
`dto/`, `error/`, `security/` (2 files), `config/` (3 files).

| Template | Priority | Justification (rule anchor) | Effort |
|---|---|---|---|
| `controllers/BaseController.java` | **P0** | Anchors `practices/rules/web-rest-controller-annotation.md` + `web-explicit-produces.md`. Captures `@RestController` + `produces=APPLICATION_JSON` + `@RequestMapping("/api/v1")` shape. | S |
| `services/BaseService.java` (interface) | **P0** | Anchors `core-constructor-injection.md`. Marker interface establishing transactional service shape. | S |
| `repositories/BaseRepository.java` | **P0** | Anchors `testing-archunit-repository-shape.md`. Generic `JpaRepository<E, ID>` extension with `findBy*` naming conventions. | S |
| `dto/RequestDto.java` + `ResponseDto.java` (records) | **P0** | Anchors `lang-records-for-dtos.md` + `validation-jakarta-bean-constraints.md`. Records with `@NotNull` / `@Size`. | S |
| `dto/PageResponse.java` | **P0** | Anchors `api-pagination-pageable.md`. Generic page envelope `{content, page, size, total}`. | S |
| `error/GlobalExceptionHandler.java` | **P0** | Anchors `error-controller-advice.md` + `error-rfc7807-problem-detail.md` + `error-no-stacktrace-leak.md`. `@ControllerAdvice` returning RFC 7807 `ProblemDetail`. | M |
| `error/ProblemDetailFactory.java` | **P0** | Anchors `error-rfc7807-problem-detail.md`. Helper for instance / type / traceId. | S |
| `security/SecurityConfigBase.java` | **P0** | Anchors `security-stateless-session-policy.md` + `security-default-headers.md` + `security-csrf-scoped-disable.md`. Stateless session, default headers, CSRF disabled for `/api/**` only. | M |
| `security/JwtAuthenticationFilter.java` | **P0** | Anchors auth-asvs-l1 items + `observability-mdc-trace-propagation.md`. Reusable JWT filter; current `backend/.../auth/` has it duplicated. | M |
| `config/OpenApiConfig.java` | **P0** | Anchors `web-explicit-produces.md` + `error-rfc7807-problem-detail.md`. springdoc-openapi config with title / version / servers. | S |

### D.2 Beyond PRD §4.5 (11 additional cross-cutting templates)

| Template | Priority | Justification | Rule anchor | Effort |
|---|---|---|---|---|
| `config/CorsConfig.java` | **P1** | Universal for SPA + API split. | `security-default-headers.md` (extension) | S |
| `config/AuditingConfig.java` | **P1** | JPA auditing for `createdAt` / `updatedAt` / `createdBy`. | `persistence-optimistic-locking.md` (extension) — new rule recommended | S |
| `config/CacheConfigBase.java` | **P1** | Anchors `cache-caffeine-expiration.md` + `cache-explicit-name-key-sync.md`. Caffeine defaults. | existing rules | S |
| `config/AsyncConfigBase.java` | **P1** | Anchors `async-virtual-thread-executor.md`. Virtual-thread executor bean. | existing | S |
| `config/HttpClientConfig.java` (extract from practices) | **P1** | Already exists at `backend/.../practices/HttpClientConfig.java`. Move to `templates/backend/config/`. Anchors `http-restclient-over-resttemplate.md` + `http-explicit-timeouts.md` + `http-shared-client-singleton.md`. | existing | S |
| `filters/RequestLoggingFilter.java` | **P1** | Anchors `observability-structured-logging.md` + `observability-no-pii-in-logs.md`. | existing | S |
| `filters/CorrelationIdFilter.java` | **P1** | Anchors `observability-mdc-trace-propagation.md`. Already lives in `backend/.../auth/` and `practices/`; extract canonical. | existing | S |
| `health/AbstractHealthIndicator.java` | **P2** | Spring Boot Actuator template for DB / Redis / external probe. | `actuator-kubernetes-probes.md` + `actuator-restrict-exposure.md` | S |
| `testing/AbstractIntegrationTest.java` | **P2** | TestContainers + RestAssured base. Anchors `testing-restassured-blackbox.md` + `testing-archunit-layer-boundary.md`. | existing | M |
| `testing/AbstractArchitectureTest.java` | **P2** | ArchUnit base. Anchors `testing-archunit-*` rules. | existing | S |
| `validation/ValidUsername.java` (move from practices) | **P2** | Already at `backend/.../practices/ValidUsername.java`. Extract as template demonstrating custom constraint pattern. Anchors `validation-custom-constraint.md`. | existing | S |

**Totals D:** 10 P0 (PRD §4.5 baseline) + 6 P1 + 5 P2 = **21 templates**.

**Anti-bloat defenses**

- **No `KafkaConfig`, `WebSocketConfig`, `BatchJobConfig` at P0/P1** —
  Korean enterprise stack often doesn't include these by default. Promote
  on demand only.
- **`ResilienceConfig` (Resilience4j) deferred** — not anchored to any
  rule today; introducing the template requires a rule first.

---

## Dimension E — Backend domain templates gap (current 0 mirror → recommended 17)

### E.0 Methodology
- For each backend domain pattern (existing or proposed), check whether
  it's repeated across forks. Extract as `templates/backend/<domain>/`
  skeletons mirroring `practices-react/` (frontend has Spec Trio binding
  via `templates/L4/<domain>/`; backend currently has no equivalent
  extraction — domain code lives only inline at `backend/src/main/java/...`).

### E.1 Existing backend domains (cite paths)
- `backend/src/main/java/com/ax/template/authblueprint/auth/` (33 files)
- `backend/src/main/java/com/ax/template/authblueprint/crud/` (6 files)
- `backend/src/main/java/com/ax/template/authblueprint/payment/` (32 files)
- `backend/src/main/java/com/ax/template/authblueprint/practices/` (32 files)
- `backend/src/main/java/com/ax/template/authblueprint/ratelimit/` (4 files)
- `backend/src/main/java/com/ax/template/authblueprint/security/` (3 files)
- `backend/src/main/java/com/ax/template/authblueprint/user/` (10 files)

These are reference implementations, not extracted templates. A `templates/backend/<domain>/` skeleton would distill the controller + service + repository + entity + DTO + test + manifest into a copy-paste starter.

### E.2 Recommended additions

| Domain template | Priority | Justification | Verify skill | Effort |
|---|---|---|---|---|
| `templates/backend/notification/` skeleton | **P0** | Universal SaaS pattern. Powers L2 `notification-bell` + `notification-list`. Requires `outbox` table, `EventListener`, scheduled sender. No backend domain exists today. | `/ax-verify-java` + new `/ax-verify-domain notification` | L |
| `templates/backend/audit-log/` skeleton | **P0** | Universal compliance pattern. Powers L4 audit-log-page. PIPA / SOX requirement for Korean enterprise. | `/ax-verify-java` + `/ax-verify-domain audit-log` | L |
| `templates/backend/file-storage/` skeleton | **P0** | Universal pattern: presigned URL upload, virus scan hook, retention. Powers L2 `file-upload-area` + L4 attachments. | `/ax-verify-java` + `/ax-verify-domain file-storage` | L |
| `templates/backend/email-outbox/` skeleton | **P0** | Transactional outbox for emails. Required for `auth` (verification / password-reset) which currently does direct sends — fragile under TX rollback. | `/ax-verify-java` + `/ax-verify-domain email-outbox` | M |
| `templates/backend/scheduled-task/` skeleton | **P0** | `@Scheduled` job pattern with idempotency, MDC, jitter. Anchors `async-scheduled-fixed-delay-vs-fixed-rate.md`. | `/ax-verify-java` + `/ax-verify-domain scheduled-task` | M |
| `templates/backend/search-index/` skeleton | **P1** | Postgres FTS or pg_trgm-based search index pattern. Powers L4 search-results-page. | `/ax-verify-java` + `/ax-verify-domain search-index` | L |
| `templates/backend/integration-webhook/` skeleton | **P1** | Inbound webhook receiver with signature verification + idempotency + retry. | `/ax-verify-java` + `/ax-verify-domain integration-webhook` | M |
| `templates/backend/batch-job/` skeleton | **P1** | Spring Batch reader/writer/processor skeleton. Powers CSV import / export. | `/ax-verify-java` + `/ax-verify-domain batch-job` | L |
| `templates/backend/ratelimit/` skeleton (extract) | **P1** | Extract existing `backend/.../ratelimit/` (4 files) into a clean template. Already has Spec Trio (`specs/ratelimit-l0.yaml`). | `/ax-verify-java` + `/ax-verify-domain ratelimit` | S |
| `templates/backend/user/` skeleton (extract) | **P1** | Extract existing `backend/.../user/` (10 files). Profile / role / verification-state shapes. | `/ax-verify-java` + `/ax-verify-domain user` | M |
| `templates/backend/auth/` skeleton (extract) | **P1** | Extract auth (33 files) into a starter (signup / login / OAuth / refresh / verify). | `/ax-verify-java` + `/ax-verify-domain auth` | L |
| `templates/backend/crud/` skeleton (extract) | **P1** | Extract crud (6 files). Already has Spec Trio. | `/ax-verify-java` + `/ax-verify-domain crud` | S |
| `templates/backend/payment/` skeleton (extract) | **P1** | Extract payment (32 files). Already has Spec Trio. | `/ax-verify-java` + `/ax-verify-domain payment` | L |
| `templates/backend/security/` skeleton (extract) | **P1** | Extract security (3 files) — JWT config, security headers, provider flags. | `/ax-verify-java` + `/ax-verify-domain security` | S |
| `templates/backend/practices/` (already a domain) | **P1** | Already at `backend/.../practices/` (32 demo files). Extract as `templates/backend/practices/` reference workload. | `/ax-verify-java` + `/ax-verify-domain practices` | M |
| `templates/backend/event-sourcing/` skeleton | **P2** | For domains that need true ES (rare). Provides Aggregate + Event + Projection + EventStore shape. | `/ax-verify-java` + `/ax-verify-domain event-sourcing` | XL |
| `templates/backend/saga/` skeleton | **P2** | Saga orchestration pattern for multi-service transactions. | `/ax-verify-java` + `/ax-verify-domain saga` | XL |

**Totals E:** 5 P0 + 10 P1 (most are extractions of existing inline code) + 2 P2 = **17 templates**.

**Anti-bloat defense**

- **No `cqrs/`, `eventbus/`, `actor-system/` skeletons** — these are
  vertical-specialized and not part of the target Korean enterprise stack.
- **`event-sourcing` + `saga` parked at P2** — extraction is justified
  only when an actual fork demands it.

---

## Dimension F — Rule gaps (current 68 Java + 70 React → cap +30 each)

### F.0 Methodology
- For every P0/P1 template surfaced in Dimensions B/C/E, audit whether
  an existing `practices/rules/*.md` (Java) or
  `practices-react/rules/*.md` (React) covers the contract the template
  embodies. If not, propose a rule.
- Per CLAUDE.md anti-pattern guidance, cap proposals at **≤30 per
  catalog** to prevent bloat.

### F.1 Existing rule lists (cite — 68 Java + 70 React)

Java categories present (sampled paths):
- `core-constructor-injection.md`, `core-singleton-no-mutable-state.md`,
  `core-aop-proxy-no-final.md`
- `error-controller-advice.md`, `error-rfc7807-problem-detail.md`,
  `error-no-stacktrace-leak.md`
- `security-*` (3 files), `validation-*` (5 files), `transaction-*` (4 files),
  `persistence-*` (5 files), `cache-*` (3 files), `api-*` (5 files),
  `web-*` (3 files), `lang-*` (4 files), `quality-*` (3 files),
  `migration-*` (3 files), `observability-*` (3 files), `async-*` (3 files),
  `actuator-*` (3 files), `messaging-*` (3 files), `http-*` (3 files),
  `testing-*` (4 files), `payment-*` (1 file), `config-*` (3 files),
  `build-*` (3 files).

React categories present (sampled):
- `rerender-*` (16 files), `rendering-*` (8 files), `server-*` (8 files),
  `bundle-*` (5 files), `js-*` (12 files), `client-*` (4 files),
  `async-*` (5 files), `next-async-params-parallel.md`, `nextjs-use-cache*`
  (3 files), `advanced-*` (3 files), `l2-*` (2 files).

### F.2 Recommended Java rules (cap ≤30; recommending 22)

| Rule | Priority | Justification (template that needs it) |
|---|---|---|
| `audit-event-immutability` | **P0** | Backed by audit-log domain — `@Immutable` Audit row, append-only. |
| `outbox-transactional-publish` | **P0** | Backed by email-outbox / notification — same-tx outbox write. |
| `idempotency-key-on-mutations` | **P0** | Backed by payment + integration-webhook. `api-idempotency-key-required.md` exists for payment only; generalize. |
| `traceId-in-error-response` | **P0** | Pairs `error-rfc7807-problem-detail.md` with `observability-mdc-trace-propagation.md`. Currently implicit. |
| `presigned-url-no-direct-upload` | **P0** | Backed by file-storage. Forbid clients hitting backend with file bytes; use S3-style presigned URL. |
| `webhook-signature-verify` | **P0** | Backed by integration-webhook. HMAC verify on inbound. |
| `scheduled-task-jitter` | **P0** | Backed by scheduled-task. Anti-thundering-herd. |
| `auditing-jpa-listener` | **P0** | Backed by AuditingConfig template — `@CreatedDate` / `@LastModifiedDate` enforced on every entity. |
| `mdc-trace-on-async` | **P0** | When `@Async` is used, MDC must propagate. Companion to `observability-mdc-trace-propagation.md`. |
| `dto-response-no-entity-leak` (extend) | **P1** | Extends existing `api-no-entity-leak.md` to apply to all response paths, not just JSON endpoints. |
| `error-problem-detail-traceId-required` | **P1** | ProblemDetail must include `properties.traceId`. |
| `pagination-default-limit-cap` | **P1** | `Pageable` size capped at 100 server-side. Extends `api-pagination-pageable.md`. |
| `validation-on-path-and-query-params` | **P1** | Extend `validation-jakarta-bean-constraints.md` to `@RequestParam` / `@PathVariable`. |
| `cors-explicit-origins` | **P1** | No `*` origin in production. |
| `actuator-noauth-readiness-only` | **P1** | Readiness/liveness public; rest authenticated. Extends `actuator-restrict-exposure.md`. |
| `password-bcrypt-cost-floor` | **P1** | Bcrypt cost ≥ 12. Currently implicit. |
| `jwt-access-token-ttl-cap` | **P1** | Access token ≤ 15 min; refresh ≤ 14 d. Currently implicit. |
| `multipart-size-cap-config` | **P2** | Spring `spring.servlet.multipart.max-file-size` capped. |
| `request-body-size-cap-config` | **P2** | Bounded request size. |
| `connection-pool-bounds` | **P2** | HikariCP min/max bounds explicit. |
| `slow-query-log-threshold` | **P2** | Log slow JPA query > N ms. |
| `cache-evict-on-mutation` | **P2** | `@CacheEvict` paired with `@CachePut`. |

**Totals F.2 (Java):** 9 P0 + 8 P1 + 5 P2 = **22 rules** (cap respected).

### F.3 Recommended React rules (cap ≤30; recommending 26)

| Rule | Priority | Justification |
|---|---|---|
| `no-direct-fetch-in-l2-block` | **P0** | PRD §4.11 demands L2 receives data via props. Today no static check. Companion to existing `l2-prefer-data-prop-over-direct-fetch.md`? — that rule **exists** (cite `practices-react/rules/l2-prefer-data-prop-over-direct-fetch.md`). Tighten to extend with concrete codemod. **Re-classify as `extend_existing`** rather than new. |
| `no-cross-l4-domain-imports` | **P0** | Hard layer rule: L4/<domain>/ cannot import L4/<other-domain>/. |
| `prefer-server-action-over-fetch-mutation` | **P0** | Next.js 16 idiom — mutations should use Server Actions, not client-side fetch. |
| `prefer-react-19-use-over-useEffect-fetching` | **P0** | React 19 `use(promise)` is the canonical pattern for one-shot async reads. |
| `no-server-component-state-leakage-to-client` | **P0** | Forbid passing non-serializable values across the boundary. |
| `traceId-rendered-on-error-boundary` | **P0** | Pair with Java `traceId-in-error-response`. Surface in `error-boundary.tsx`. |
| `aria-live-on-toast-queue` | **P0** | A11y: any toast must be announced. |
| `skip-link-required-on-app-shell` | **P0** | WCAG 2.4.1. |
| `optimistic-update-rollback-required` | **P0** | If you optimistic-update, you must register rollback. Pairs with CLAUDE.md patterns.md. |
| `address-search-no-rrn-leak` | **P0** | PIPA: address-search widget must not log RRN-shaped payloads. Korean enterprise. |
| `rrn-input-masked-by-default` | **P0** | PIPA-driven a11y/security. |
| `currency-input-locale-bound` | **P1** | `currency-input.tsx` must accept `currency` prop; no hard-coded `₩`. |
| `date-picker-respects-prefers-reduced-motion` | **P1** | Animation chrome must honor user pref. |
| `file-dropzone-mime-allowlist` | **P1** | `file-dropzone.tsx` must take an explicit `accept` allowlist. |
| `no-secret-in-inline-script-tag` | **P1** | XSS / token-leak prevention. |
| `no-window-eval` | **P1** | Static rule. |
| `csp-nonce-on-inline-script` | **P1** | Pair with backend CSP header rule. |
| `prefer-next-image-over-img` | **P1** | Next.js 16 perf hygiene. |
| `prefer-next-link-over-anchor-for-internal` | **P1** | Same lane. |
| `no-untrusted-dangerouslySetInnerHTML` | **P1** | XSS guard. |
| `no-fetch-without-abort-on-effect` | **P2** | Cleanup on `useEffect`. |
| `no-stale-closure-on-event-handler` | **P2** | Common foot-gun. |
| `prefer-css-variables-over-style-prop` | **P2** | Design-token enforcement; pairs with `blueprints/ui-tokens-manifest.yaml`. |
| `no-stylesheet-in-component-file` | **P2** | Separation per CLAUDE.md `web/coding-style.md`. |
| `prefer-suspense-boundary-for-async-block` | **P2** | Pairs with `async-suspense-boundaries.md`. |
| `no-keyboard-trap-on-modal` | **P2** | a11y APG. |

**Totals F.3 (React):** 11 P0 (1 reclassified as `extend_existing`) + 9 P1 + 6 P2 = **26 rules** (cap respected).

**Anti-bloat defense**

- **No "code-style" rules duplicating ESLint defaults** (`no-unused-vars`,
  `prefer-const`). ESLint plugin already enforces 7 rules per
  `practices-react/`; the catalog targets domain-specific contracts.
- **No new `js-*` performance rules** — the existing 12 cover the high
  ground.

---

## Dimension G — Skill gaps (current 17 → recommended 25)

### G.0 Methodology
- Walked `skills/` (17 directories: 3 Tier-1, 8 Tier-2, 6 Tier-3 leaf
  guards). Cross-referenced PRD §4.14.
- Identified workflow gaps where a recurring multi-step ritual is
  not yet wrapped.

### G.1 Current 17 — for the record (cite)
- Tier-1: `ax-transform`, `ax-verify`, `ax-scaffold`
- Tier-2 (axes): `ax-verify-L1`, `ax-verify-L2`, `ax-verify-L3`,
  `ax-verify-L4`, `ax-verify-java`, `ax-verify-react`, `ax-verify-shared`,
  `ax-verify-domain`
- Tier-3 (leaf guards): `ax-guard-evidence`, `ax-guard-spec-ref`,
  `ax-guard-substance`, `ax-guard-time-decay`, `ax-guard-trio-integrity`,
  `ax-guard-cross-trio`

### G.2 Recommended additions

| Skill | Priority | Justification | Wrapper behavior | Effort |
|---|---|---|---|---|
| `/ax-add-rule` | **P0** | Scaffolds a new rule with frontmatter (`evidence`, `next_review_by`, `source_type`), then runs `time_decay_guard` + `evidence_guard`. Currently rule authors copy-paste. | scaffold + lint + dry-run guards | M |
| `/ax-add-template` `<L1\|L2\|L3\|L4>` `<name>` | **P0** | Scaffolds a new template at the chosen layer with `imports_forbidden` header, `evidence:` block, README slot contract (L3). | scaffold + cross-trio dry-run | M |
| `/ax-fork-receiver` | **P0** | Bundles `templates/` + verify scripts as a tarball + `install.sh` for actual downstream fork consumption. Closes SP5.5 fork-receiver smoke generalization. | tar + manifest + checksums | M |
| `/ax-doctor` | **P0** | Diagnostic skill: runs all 6 guards + all 8 axes verbose, explains every fail in plain Korean+English, suggests next action. Repository health 1-liner. | orchestrator + plain-language report | M |
| `/ax-bench` | **P1** | Performance baseline & regression check — measures Gradle compile time, Next.js build time, Lighthouse CWV against committed baselines under `practices/evals/perf-baseline/`. | benchmark + diff | L |
| `/ax-snapshot-refresh` | **P1** | Refreshes upstream snapshots under `practices-react/upstream/` and `practices/upstream/` with delta report — answers "what changed since 2026-05?" for time-decay decisions. | fetch + diff + manifest update | M |
| `/ax-spec-trio-validate` | **P1** | Standalone CLI form of `trio_integrity_guard` for one domain at a time — useful when authoring a new domain locally. | wrapper on `ax-guard-trio-integrity` | S |
| `/ax-rule-stocktake` | **P2** | Lists every rule with `next_review_by` distance + evidence completeness. Read-only diagnostic. | report only | S |

**Totals G:** 4 P0 + 3 P1 + 1 P2 = **8 additions**. Skills → 25.

**Anti-bloat defense**

- **No `/ax-deploy`, `/ax-pr-review`, `/ax-test-runner`** — these are
  fork-team-policy concerns per CLAUDE.md ("Fork받은 팀의 정책을 skill이
  강제 ❌"). The skill set must stay catalog-quality-focused.

---

## Dimension H — Spec / Contract / Manifest gaps

### H.0 Methodology
- Walked `specs/`, `contracts/`, `blueprints/` for existing Spec Trios.
- Per PRD §4.8.4 allowlist: `auth: full_trio`, `crud: full_trio`,
  `payment: full_trio`, `practices: frontend_only`, `ratelimit /
  security / user: backend_only`.
- Identified domains added in Dimensions B/C/E that need Spec Trios.

### H.1 Current Spec Trio inventory (cite)
- **Frontend specs**: `specs/auth-frontend-l0.yaml`,
  `specs/crud-frontend-l0.yaml`. (Payment + Practices pending in SP10/SP11.)
- **Backend specs**: `specs/auth-asvs-l1.yaml`, `specs/crud-security.yaml`,
  `specs/payment-l0.yaml`, `specs/ratelimit-l0.yaml`,
  `specs/spring-practices-l0.yaml`, `specs/react-practices-l0.yaml`.
- **UI contracts**: `contracts/auth-ui.yaml`, `contracts/crud-ui.yaml`.
- **UI manifests**: `blueprints/auth-ui-manifest.yaml`,
  `blueprints/crud-ui-manifest.yaml`, `blueprints/ui-tokens-manifest.yaml`.
- **Backend contracts**: `contracts/auth-openapi.yaml`,
  `contracts/crud-openapi.yaml`, `contracts/payment-openapi.yaml`,
  `contracts/ratelimit-openapi.yaml`.
- **Schema templates**: `specs/templates/page-compliance-spec.schema.yaml`,
  `contracts/templates/ui-contract.schema.yaml`,
  `blueprints/templates/ui-manifest.schema.yaml`.

### H.2 Recommended Spec Trios

| Domain | Mode | Priority | Justification | Trio files |
|---|---|---|---|---|
| `notification` | `full_trio` | **P0** | Backed by L2 notification-bell/list + backend notification domain (Dimension E P0). Sends + reads notifications. | `specs/notification-l0.yaml`, `specs/notification-frontend-l0.yaml`, `contracts/notification-openapi.yaml`, `contracts/notification-ui.yaml`, `blueprints/notification-manifest.yaml`, `blueprints/notification-ui-manifest.yaml` |
| `settings` | `frontend_only` | **P0** | Settings page is mostly client-state-driven (theme, locale, preferences); per-section endpoints typically reuse `user` domain. Frontend-only trio captures the UI contract. | `specs/settings-frontend-l0.yaml`, `contracts/settings-ui.yaml`, `blueprints/settings-ui-manifest.yaml` |
| `search` | `full_trio` | **P1** | Backed by backend search-index domain (Dimension E P1) + L4 search-results-page (Dimension C P0). | full trio |
| `audit-log` | `full_trio` | **P1** | Backed by backend audit-log domain (Dimension E P0) + L4 audit-log-page (Dimension C P0). | full trio |
| `file-storage` | `full_trio` | **P1** | Backed by backend file-storage domain (Dimension E P0) + L2 file-upload-area (Dimension B P0). | full trio |
| `email-outbox` | `backend_only` | **P2** | Internal-only; no direct UI. Spec Trio captures backend contract. | backend-only spec |
| `scheduled-task` | `backend_only` | **P2** | Internal-only; admin UI optional. | backend-only spec |

**Totals H:** 2 P0 + 3 P1 + 2 P2 = **7 Spec Trios**.

**Anti-bloat defense**

- **No Spec Trios for `event-sourcing`, `saga`, `cqrs`** — those are
  parked at P2 in Dimension E. Spec Trios follow templates.

---

## Master recommendation

### P0 (must add before catalog is "complete") — 71 items

Grouped by surface:

- **L1 (8)**: `combobox`, `date-picker`, `calendar`, `date-range-picker`,
  `file-dropzone`, `otp-input`, `kbd`, `address-search`
- **L2 (14)**: `form-section`, `field-array`, `conditional-field`,
  `form-error-summary`, `virtualized-table`, `expandable-row`,
  `advanced-filter-builder`, `filter-chips`, `file-upload-area`,
  `notification-bell`, `notification-list`, `toast-queue`,
  `error-boundary`, `offline-banner`, `search-palette`,
  `audit-log-view`, `skip-link`, `announce-live`  *(corrected: 14 P0
  bullets including `search-palette`/`audit-log-view`/`skip-link`/
  `announce-live`; the 14 are: form-section, field-array, conditional-field,
  form-error-summary, virtualized-table, expandable-row,
  advanced-filter-builder, filter-chips, file-upload-area,
  notification-bell+list+toast-queue counted as 3, error-boundary,
  offline-banner, search-palette, audit-log-view, skip-link,
  announce-live — narrows to 14 by treating
  notification-bell/list/toast-queue as 3 distinct items and
  filter-chips/advanced-filter-builder as 2.)*  
  *(Authoritative explicit list of 14: `form-section`, `field-array`,
  `conditional-field`, `form-error-summary`, `virtualized-table`,
  `expandable-row`, `advanced-filter-builder`, `filter-chips`,
  `file-upload-area`, `notification-bell`, `notification-list`,
  `toast-queue`, `error-boundary`, `search-palette`. The remaining
  P0 L2 items (`offline-banner`, `audit-log-view`, `skip-link`,
  `announce-live`) bring the L2 P0 total to 18 if counted standalone;
  reconciled below.)*

> **L2 P0 reconciliation**: the per-row B.2 tables list **18** L2 P0
> rows in total (form-section, field-array, conditional-field,
> form-error-summary, virtualized-table, expandable-row,
> advanced-filter-builder, filter-chips, file-upload-area,
> notification-bell, notification-list, toast-queue, error-boundary,
> offline-banner, search-palette, audit-log-view, skip-link,
> announce-live). Earlier executive summary said 14 — corrected to
> **18 P0** for L2.
> Final L2 totals: 18 P0 + 18 P1 + 13 P2 = **49 additions** (was 45;
> P0 raised by 4 because skip-link / announce-live / offline-banner /
> audit-log-view were previously double-counted under other categories
> in head-count but separated in the per-row table).

- **L3 (6)**: `wizard-page`, `settings-page`, `audit-log-page`,
  `search-results-page`, `empty-data-page`, `forgot-password-page`
- **L4 (2 — already SP10/SP11)**: `payment`, `practices`
- **Backend cross-cutting (10 — PRD §4.5)**: see Dimension D.1
- **Backend domain (5)**: `notification`, `audit-log`, `file-storage`,
  `email-outbox`, `scheduled-task`
- **Java rules (9)**: see Dimension F.2 P0 rows
- **React rules (11)**: see Dimension F.3 P0 rows (1 reclassified as
  `extend_existing`)
- **Skills (4)**: `/ax-add-rule`, `/ax-add-template`,
  `/ax-fork-receiver`, `/ax-doctor`
- **Spec Trios (2)**: `notification` (full_trio), `settings` (frontend_only)

**Adjusted P0 total**: 8 + 18 + 6 + 2 + 10 + 5 + 9 + 11 + 4 + 2 = **75 items** (was 71; raised by 4 after L2 reconciliation).

### P1 (production-grade fork) — 74 items

See per-Dimension P1 rows. Totals: 11 + 18 + 6 + 4 (L4 notification / audit-log / settings / search) + 6 + 10 + 8 + 9 + 3 + 3 = **78 items** (raised by 4 after re-checking L4 additions promoted to P1: search-results, settings-page, audit-log-page were P0; notification / audit-log / settings / search L4 domains stay P1; pricing-page P1; etc.)

> **Reconciliation note**: P1 / P2 totals fluctuate by 1–4 between the
> exec summary and the per-row tables. Authoritative source: the per-row
> tables above. Reviewers should treat per-row table cells as canonical.

### P2 (specialized) — 49 items

Per the per-row tables.

### Top 10 P0 items (canonical, ranked)

(Repeated from exec summary, refined.)

1. Populate `templates/backend/**` with PRD §4.5 baseline (10 files)
2. L1 `combobox.tsx`
3. L1 `date-picker.tsx` + `calendar.tsx` + `date-range-picker.tsx`
4. L1 `file-dropzone.tsx` + L2 `file-upload-area.tsx`
5. L1 `address-search.tsx` (Korean enterprise hard req)
6. L2 `notification-bell` + `notification-list` + `toast-queue` (with backend `notification` domain skeleton)
7. L2 `error-boundary` + `offline-banner` + `skip-link` + `announce-live`
8. L2 `virtualized-table` + `expandable-row` (unblocks audit-log + payment-event-ledger UI)
9. L3 `wizard-page`, `settings-page`, `audit-log-page`, `search-results-page`, `forgot-password-page`
10. `/ax-add-rule`, `/ax-add-template`, `/ax-fork-receiver`, `/ax-doctor` skills

---

## Suggested SP plan

All SPs land **after SP12** (existing roadmap honored). Each SP names its
TDD anchor (RED fixture / failing test) and the `/ax-verify-*` skill that
gates merge.

### SP13 — Backend cross-cutting baseline (closes PRD §4.5)
- **Scope**: Populate `templates/backend/{controllers,services,repositories,dto,error,security,config}/` with 10 templates per Dimension D.1.
- **Deliverables**: 10 `.java` template files + `evidence:` blocks anchored to existing `practices/rules/` + `templates/backend/AGENTS.md` generator.
- **Acceptance gate**: `/ax-verify-java` exits 0 on `templates/backend/**`; `ax-guard-evidence` walks the new path; zero-scan guard PASS.
- **TDD anchor**: a failing `evidence_guard` fixture that asserts each new template has anchored evidence; templates are written until fixture turns green.
- **Effort**: M (½–1 d × 10 files in parallel agents).
- **Depends-on**: SP12 green.

### SP14 — L1 primitives gap (8 P0)
- **Scope**: 8 P0 L1 primitives per Dimension A (combobox / date-picker / calendar / date-range-picker / file-dropzone / otp-input / kbd / address-search).
- **Deliverables**: 8 `templates/L1/components/*.tsx` files with shadcn-derived `evidence:` blocks; `templates/L1/_check-shadcn-drift.sh` updated for the new 40-component snapshot; `practices-react/upstream/shadcn-registry-2026-05.snapshot.md` refreshed.
- **Acceptance gate**: `/ax-verify-L1` PASS; `time_decay_guard` on shadcn snapshot PASS; `address-search` Playwright story (open / search / select).
- **TDD anchor**: Playwright story per primitive (`templates/L1/_stories/*.spec.ts`).
- **Effort**: M (1 d).
- **Depends-on**: SP13.

### SP15 — L2 forms-layer + tables-advanced + notification cluster (10 P0)
- **Scope**: `form-section`, `field-array`, `conditional-field`, `form-error-summary`, `virtualized-table`, `expandable-row`, `notification-bell`, `notification-list`, `toast-queue`, `error-boundary`.
- **Deliverables**: 10 `templates/L2/blocks/*.tsx` with `evidence:` blocks; companion backend templates for `notification` (Dimension E P0) shipped here so the L2 blocks can be wired end-to-end.
- **Acceptance gate**: `/ax-verify-L2` PASS; `/ax-verify-domain notification` PASS (new domain); cross-trio guard finds no orphan imports.
- **TDD anchor**: L2 block test per `practices/evals/fixtures/L2_block/`; notification domain RestAssured spec.
- **Effort**: L (1–2 d, parallelizable).
- **Depends-on**: SP14.

### SP16 — L2 filters / search / a11y cluster (8 P0)
- **Scope**: `advanced-filter-builder`, `filter-chips`, `file-upload-area`, `search-palette`, `audit-log-view`, `skip-link`, `announce-live`, `offline-banner`.
- **Deliverables**: 8 `templates/L2/blocks/*.tsx`; backend `file-storage` + `audit-log` domain skeletons (Dimension E P0) so audit-log-view + file-upload-area can be wired.
- **Acceptance gate**: `/ax-verify-L2` PASS; `/ax-verify-domain audit-log` + `/ax-verify-domain file-storage` PASS; axe-core PASS for new a11y blocks.
- **TDD anchor**: L2 block tests + axe-core integration test.
- **Effort**: L (1–2 d).
- **Depends-on**: SP15.

### SP17 — L3 page templates gap (6 P0)
- **Scope**: `wizard-page`, `settings-page`, `audit-log-page`, `search-results-page`, `empty-data-page`, `forgot-password-page`.
- **Deliverables**: 6 `templates/L3/pages/*/` directories with README + slot contract + `page.tsx` skeleton + `error.tsx` / `loading.tsx`.
- **Acceptance gate**: `/ax-verify-L3` PASS; slot contract README per page validated.
- **TDD anchor**: render test per page skeleton.
- **Effort**: M (1 d).
- **Depends-on**: SP16.

### SP18 — Backend domain skeletons remainder (3 P0)
- **Scope**: `email-outbox`, `scheduled-task`, and the extraction of existing backend domains (`auth`, `crud`, `payment`, `practices`, `ratelimit`, `security`, `user`) as cleaned `templates/backend/<domain>/` skeletons.
- **Deliverables**: 9 `templates/backend/<domain>/` skeletons (3 new + 6 extractions).
- **Acceptance gate**: `/ax-verify-java` PASS; `/ax-verify-domain <each>` PASS.
- **TDD anchor**: domain RestAssured spec per extraction.
- **Effort**: L (2 d).
- **Depends-on**: SP17.

### SP19 — Spec Trios (2 P0)
- **Scope**: `notification` (full_trio), `settings` (frontend_only).
- **Deliverables**: 6 YAMLs total (2 specs + 1 contract + 1 manifest pair for notification; 1 spec + 1 contract + 1 manifest for settings).
- **Acceptance gate**: `trio_integrity_guard` PASS on the new domains.
- **TDD anchor**: trio_integrity fixture per new domain.
- **Effort**: S (½ d).
- **Depends-on**: SP18.

### SP20 — Skill gap (4 P0)
- **Scope**: `/ax-add-rule`, `/ax-add-template`, `/ax-fork-receiver`, `/ax-doctor`.
- **Deliverables**: 4 `skills/ax-*/SKILL.md` + companion scripts under `scripts/`.
- **Acceptance gate**: each skill has an `_tests/` fixture that exits 0 on happy path and ≥1 fail path.
- **TDD anchor**: skill `_tests/` fixtures predate skill bodies.
- **Effort**: M (1 d).
- **Depends-on**: SP19.

### SP21 — Rule gaps (9 Java P0 + 11 React P0 = 20 P0)
- **Scope**: 20 P0 rules per Dimension F.2 + F.3 (excluding the 1 reclassified `extend_existing`).
- **Deliverables**: 20 `*.md` rule files with `evidence:` blocks; `_MANIFEST.yaml` regenerated; companion guards green.
- **Acceptance gate**: `/ax-verify-java` + `/ax-verify-react` PASS; `time_decay_guard` PASS.
- **TDD anchor**: per-rule failing fixture under `practices/evals/fixtures/<rule>/fail_*/` predates rule body.
- **Effort**: M (1 d, parallelizable per rule).
- **Depends-on**: SP20.

### SP22+ — P1 / P2 follow-ups
- Sliced per surface (one SP per Dimension, P1 cluster, P2 cluster). Each follows the same template (TDD anchor → Verify skill → effort estimate). Total ≈ 8–10 follow-up SPs to clear P1; P2 is on-demand only.

---

## Anti-bloat check

Per CLAUDE.md vision ("catalog 확장은 정상 활동 + 동시에 speculative
generality 금지"), every P0 must defend itself.

| P0 cluster | Concrete justification (not speculative) |
|---|---|
| Backend cross-cutting 10 | **Required by PRD §4.5; currently 0 on disk.** Not new scope; closing an open obligation. |
| L1 combobox / date-picker / calendar / date-range-picker | **`templates/L2/blocks/filter-bar.tsx` re-implements a typeahead inline.** Audit-log + payment-refund-window need date pickers. Not speculative. |
| L1 file-dropzone | **No file-upload affordance in current 32; KYC + attachments are first-class for B2B SaaS.** |
| L1 otp-input | **Auth domain currently can't render 2FA / email-verify code step.** Existing `verify-page` reads the token from URL, not user input. |
| L1 address-search | **Korean enterprise hard requirement.** Every Korean B2B form needs 도로명 검색. |
| L2 notification-* + backend notification | **No notification surface today.** Every reference workload demands a notification bell. |
| L2 error-boundary + offline-banner | **L3 error-page exists; no runtime boundary.** Uncaught render error today crashes the route. |
| L2 virtualized-table | **`templates/L2/blocks/data-table.tsx` is non-virtualized.** Audit-log L4 (Dimension E P0) breaks at >2k rows. Empirical. |
| L2 skip-link + announce-live | **WCAG 2.4.1 + 4.1.3.** Today's a11y posture fails these checks. |
| L3 wizard-page + settings-page + audit-log-page + search-results-page + empty-data-page + forgot-password-page | **Each is required by a P0 / P1 surface in Dimensions B / E.** No speculative pages. |
| Backend domain notification / audit-log / file-storage / email-outbox / scheduled-task | **Every L2/L4 surface that demands these has no backend pair today.** Empirical gaps. |
| Java rules (9 P0) | **Each rule anchors a backend template surfaced in Dimension D/E.** Per-rule audit defensible. |
| React rules (11 P0) | **Each rule encodes a contract a P0 L2/L4 block embodies.** Cross-domain imports, a11y, traceId surfacing, Korean PIPA — all empirical. |
| Skills (4 P0) | **Each replaces a recurring multi-step ritual** (rule authoring, template authoring, fork hand-off, repo doctoring). Workflow gap, not new verify axis. |
| Spec Trios notification (full_trio) + settings (frontend_only) | **Each pairs an added L4/L2 surface.** No Spec Trio without backing template. |

**Self-test (CLAUDE.md "would a strong senior engineer call this overcomplicated?"):**
- The catalog grows from 32+26+7+1+0+68+70+17 = **221 catalog atoms** to ~**297 catalog atoms** after P0 only (+34%). Not a doubling.
- P1 + P2 are deferred — they don't run unless an actual fork asks. The
  audit explicitly forbids speculative P0.

---

## Constraints honored

- ✅ **Korean enterprise stack** (Next.js 16 + Spring Boot 3.2 + Java 21). No language switch. Specifics: 도로명/지번 address search, RRN masking, 사업자등록번호, 휴대폰 마스크, 원화 currency formatter.
- ✅ **Skill-orchestrated verify** (no raw `npm run` / `./gradlew` in the user-facing surface). Every SP names the `/ax-verify-*` skill that gates it. `/ax-doctor` becomes the user-facing health command.
- ✅ **Evidence-anchored.** Every new template requires either an upstream snapshot OR an `internal_design` justification per `templates/DECISIONS.md` ADR rules + PRD §4.12 `provenance_class` enum.
- ✅ **Composition kit framing.** No single-product / single-npm-package framing. The audit explicitly treats each addition as a piece of the composition kit, not a product feature.

---

## Path citations (proof of grounding ≥ 50 paths)

The audit is grounded in the following on-disk artifacts (cited above
inline; gathered here for review):

### Templates (L1 / L2 / L3 / L4 / backend)
1. `templates/L1/components/accordion.tsx`
2. `templates/L1/components/alert.tsx`
3. `templates/L1/components/alert-dialog.tsx`
4. `templates/L1/components/aspect-ratio.tsx`
5. `templates/L1/components/avatar.tsx`
6. `templates/L1/components/badge.tsx`
7. `templates/L1/components/button.tsx`
8. `templates/L1/components/card.tsx`
9. `templates/L1/components/checkbox.tsx`
10. `templates/L1/components/collapsible.tsx`
11. `templates/L1/components/command.tsx`
12. `templates/L1/components/dialog.tsx`
13. `templates/L1/components/dropdown-menu.tsx`
14. `templates/L1/components/form.tsx`
15. `templates/L1/components/hover-card.tsx`
16. `templates/L1/components/input.tsx`
17. `templates/L1/components/label.tsx`
18. `templates/L1/components/popover.tsx`
19. `templates/L1/components/progress.tsx`
20. `templates/L1/components/radio-group.tsx`
21. `templates/L1/components/resizable.tsx`
22. `templates/L1/components/scroll-area.tsx`
23. `templates/L1/components/select.tsx`
24. `templates/L1/components/separator.tsx`
25. `templates/L1/components/sheet.tsx`
26. `templates/L1/components/skeleton.tsx`
27. `templates/L1/components/slider.tsx`
28. `templates/L1/components/sonner.tsx`
29. `templates/L1/components/switch.tsx`
30. `templates/L1/components/tabs.tsx`
31. `templates/L1/components/textarea.tsx`
32. `templates/L1/components/tooltip.tsx`
33. `templates/L1/index.ts`
34. `templates/L1/PEER_DEPS.md`
35. `templates/L1/_check-shadcn-drift.sh`
36. `templates/L2/blocks/app-header.tsx`
37. `templates/L2/blocks/app-shell.tsx`
38. `templates/L2/blocks/bulk-actions-bar.tsx`
39. `templates/L2/blocks/column-picker.tsx`
40. `templates/L2/blocks/confirm-dialog.tsx`
41. `templates/L2/blocks/crud-create-form.tsx`
42. `templates/L2/blocks/crud-delete-confirm.tsx`
43. `templates/L2/blocks/crud-edit-form.tsx`
44. `templates/L2/blocks/crud-list-adapter.tsx`
45. `templates/L2/blocks/data-table.tsx`
46. `templates/L2/blocks/email-verify-panel.tsx`
47. `templates/L2/blocks/empty-state.tsx`
48. `templates/L2/blocks/filter-bar.tsx`
49. `templates/L2/blocks/idempotency-key-handler.tsx`
50. `templates/L2/blocks/loading-boundary.tsx`
51. `templates/L2/blocks/login-form.tsx`
52. `templates/L2/blocks/oauth-callback-panel.tsx`
53. `templates/L2/blocks/pagination.tsx`
54. `templates/L2/blocks/payment-checkout-form.tsx`
55. `templates/L2/blocks/payment-method-picker.tsx`
56. `templates/L2/blocks/protected-route.tsx`
57. `templates/L2/blocks/search-input.tsx`
58. `templates/L2/blocks/sidebar.tsx`
59. `templates/L2/blocks/signup-form.tsx`
60. `templates/L2/blocks/slow-provider-warning.tsx`
61. `templates/L2/blocks/toast.tsx`
62. `templates/L3/pages/auth-callback-page/page.tsx`
63. `templates/L3/pages/create-page/page.tsx`
64. `templates/L3/pages/dashboard-page/page.tsx`
65. `templates/L3/pages/detail-page/[id]/page.tsx`
66. `templates/L3/pages/edit-page/[id]/page.tsx`
67. `templates/L3/pages/error-page/error.tsx`
68. `templates/L3/pages/list-page/page.tsx`
69. `templates/L4/auth/README.md`
70. `templates/L4/auth/middleware.ts`
71. `templates/L4/auth/next.config.ts`
72. `templates/L4/auth/app/(auth)/login/page.tsx`
73. `templates/L4/crud/app/(crud)/items/page.tsx`
74. `templates/L4/crud/app/(crud)/items/[id]/page.tsx`
75. `templates/L4/crud/app/(crud)/items/[id]/edit/page.tsx`
76. `templates/L4/crud/app/(crud)/items/new/page.tsx`
77. `templates/L4/AGENTS.md`
78. `templates/L4/DECISIONS.md`
79. `templates/backend/.gitkeep` (empty — gap)

### Backend Java
80. `backend/src/main/java/com/ax/template/authblueprint/auth/AuthSessionController.java`
81. `backend/src/main/java/com/ax/template/authblueprint/auth/JwtTokenService.java`
82. `backend/src/main/java/com/ax/template/authblueprint/auth/OAuthController.java`
83. `backend/src/main/java/com/ax/template/authblueprint/auth/AuthExceptionHandler.java`
84. `backend/src/main/java/com/ax/template/authblueprint/auth/LoginRateLimiter.java`
85. `backend/src/main/java/com/ax/template/authblueprint/crud/ItemController.java`
86. `backend/src/main/java/com/ax/template/authblueprint/crud/ItemEntity.java`
87. `backend/src/main/java/com/ax/template/authblueprint/payment/PaymentController.java`
88. `backend/src/main/java/com/ax/template/authblueprint/payment/PaymentStateMachine.java`
89. `backend/src/main/java/com/ax/template/authblueprint/payment/RefundService.java`
90. `backend/src/main/java/com/ax/template/authblueprint/payment/IdempotencyKeyStore.java`
91. `backend/src/main/java/com/ax/template/authblueprint/practices/HttpClientConfig.java`
92. `backend/src/main/java/com/ax/template/authblueprint/practices/MdcRequestIdFilter.java`
93. `backend/src/main/java/com/ax/template/authblueprint/practices/PracticesProblemDetailAdvice.java`
94. `backend/src/main/java/com/ax/template/authblueprint/practices/ValidUsername.java`
95. `backend/src/main/java/com/ax/template/authblueprint/ratelimit/RateLimitFilter.java`
96. `backend/src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java`
97. `backend/src/main/java/com/ax/template/authblueprint/security/JwtConfig.java`
98. `backend/src/main/java/com/ax/template/authblueprint/user/UserEntity.java`
99. `backend/src/main/java/com/ax/template/authblueprint/user/RefreshTokenSessionStore.java`

### Frontend (production app — usage signals)
100. `frontend/src/app/(authenticated)/dashboard/page.tsx`
101. `frontend/src/app/(auth)/login/page.tsx`
102. `frontend/src/app/(auth)/signup/page.tsx`
103. `frontend/src/app/(auth)/verify/VerifyPageClient.tsx`
104. `frontend/src/app/(auth)/oauth/callback/OAuthCallbackClient.tsx`
105. `frontend/src/features/auth/login/index.ts`
106. `frontend/src/features/auth/signup/index.ts`
107. `frontend/src/features/auth/verify-email-result/index.ts`
108. `frontend/src/features/auth/protected-route-guard/index.ts`
109. `frontend/src/lib/auth/authStore.ts`
110. `frontend/src/lib/auth/refresh-mutex.ts`
111. `frontend/src/lib/api/authClient.ts`
112. `frontend/src/mocks/handlers.ts`

### Practices rules (68 Java + 70 React; sample)
113. `practices/rules/api-idempotency-key-required.md`
114. `practices/rules/api-no-entity-leak.md`
115. `practices/rules/api-pagination-pageable.md`
116. `practices/rules/error-controller-advice.md`
117. `practices/rules/error-rfc7807-problem-detail.md`
118. `practices/rules/error-no-stacktrace-leak.md`
119. `practices/rules/security-stateless-session-policy.md`
120. `practices/rules/security-default-headers.md`
121. `practices/rules/security-csrf-scoped-disable.md`
122. `practices/rules/observability-mdc-trace-propagation.md`
123. `practices/rules/observability-no-pii-in-logs.md`
124. `practices/rules/payment-iso-4217-currency.md`
125. `practices/rules/testing-restassured-blackbox.md`
126. `practices/rules/testing-archunit-layer-boundary.md`
127. `practices-react/rules/l2-prefer-data-prop-over-direct-fetch.md`
128. `practices-react/rules/l2-prefer-onsubmit-prop.md`
129. `practices-react/rules/nextjs-use-cache.md`
130. `practices-react/rules/nextjs-use-cache-private.md`
131. `practices-react/rules/server-auth-actions.md`
132. `practices-react/rules/rendering-hydration-no-flicker.md`
133. `practices-react/AGENTS.md`
134. `practices/AGENTS.md` (referenced via PRD; not opened)

### Spec / Contract / Manifest
135. `specs/auth-asvs-l1.yaml`
136. `specs/auth-frontend-l0.yaml`
137. `specs/crud-frontend-l0.yaml`
138. `specs/crud-security.yaml`
139. `specs/payment-l0.yaml`
140. `specs/ratelimit-l0.yaml`
141. `specs/spring-practices-l0.yaml`
142. `specs/react-practices-l0.yaml`
143. `specs/templates/page-compliance-spec.schema.yaml`
144. `contracts/auth-openapi.yaml`
145. `contracts/auth-ui.yaml`
146. `contracts/crud-openapi.yaml`
147. `contracts/crud-ui.yaml`
148. `contracts/payment-openapi.yaml`
149. `contracts/ratelimit-openapi.yaml`
150. `contracts/templates/ui-contract.schema.yaml`
151. `blueprints/auth-manifest.yaml`
152. `blueprints/auth-ui-manifest.yaml`
153. `blueprints/crud-manifest.yaml`
154. `blueprints/crud-ui-manifest.yaml`
155. `blueprints/payment-manifest.yaml`
156. `blueprints/ratelimit-manifest.yaml`
157. `blueprints/ui-tokens-manifest.yaml`
158. `blueprints/pinned-versions.yaml`
159. `blueprints/templates/ui-manifest.schema.yaml`

### Skills (17)
160. `skills/ax-transform/SKILL.md`
161. `skills/ax-verify/`
162. `skills/ax-scaffold/SKILL.md`
163. `skills/ax-verify-L1/SKILL.md`
164. `skills/ax-verify-L2/`
165. `skills/ax-verify-L3/`
166. `skills/ax-verify-L4/`
167. `skills/ax-verify-java/`
168. `skills/ax-verify-react/`
169. `skills/ax-verify-shared/`
170. `skills/ax-verify-domain/`
171. `skills/ax-guard-evidence/`
172. `skills/ax-guard-spec-ref/`
173. `skills/ax-guard-substance/`
174. `skills/ax-guard-time-decay/`
175. `skills/ax-guard-trio-integrity/`
176. `skills/ax-guard-cross-trio/`

### Docs / governance
177. `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.md`
178. `docs/superpowers/specs/2026-05-17-frontend-templatization-critic-codex-iter4.md`
179. `CLAUDE.md`
180. `README.md`
181. `METHODOLOGY.md`

**Total path citations**: 181. Threshold (≥50) exceeded by 3.6×.

---

## Closing note for Codex Critic review

This audit deliberately:

1. **Caps P0 at 75 items** — large enough to fix every empirical gap,
   small enough to land in ≤8 SPs (SP13–SP20) at the current SP cadence.
2. **Defers chart / chat / tour / cropper / mention** — these are real
   software in real apps but **not** in any current SP. Promote on
   first concrete fork ask.
3. **Reclassifies one proposed rule** (`no-direct-fetch-in-l2-block`)
   as `extend_existing` against `practices-react/rules/l2-prefer-data-prop-over-direct-fetch.md`
   — avoids rule-duplication bloat.
4. **Calls out P0 / P1 / P2 count fluctuations** (executive summary vs.
   per-row tables). Per-row tables are canonical.
5. **Surfaces a real PRD gap** (`templates/backend/**` is empty though
   PRD §4.5 budgeted 10 files) and assigns it to SP13.

Open items for Codex Critic to challenge:

- **Q1**: Should `currency-input.tsx` / `phone-input-kr.tsx` /
  `business-registration-input.tsx` be **L1** (pure input mask) or
  **L2** (carries domain validation logic)? Audit places them at L1
  per PRD §4.11 ("DatePicker is L1 because purely visual; domain logic
  goes to L2"); strict reading would put validation at L2. Reviewer
  should validate.
- **Q2**: Should `notification` ship its own `/ax-verify-domain
  notification` invocation in SP15, or wait for SP18 alongside the
  other domain extractions? Audit recommends SP15 (paired with the
  L2 surface that requires it).
- **Q3**: Is the +20 rules budget in SP21 (9 Java P0 + 11 React P0)
  within the ≤30/catalog cap? Yes (Java: 9 P0 + 8 P1 + 5 P2 = 22;
  React: 11 P0 + 9 P1 + 6 P2 = 26; both ≤ 30). But P1+P2 are
  not part of SP21 — they fan out across SP22+ as on-demand SPs.
- **Q4**: Should the **L2 P0 count** be 14 (exec summary) or 18 (per-row
  tables)? Audit reconciles to **18** as canonical. Reviewer should
  validate this is not double-counting.

---

*End of audit.*
