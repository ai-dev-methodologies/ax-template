# Functional Capability Extension PRD — 2026-05-18 (CANONICAL, APPROVED)

> **Status:** APPROVED via `/ralplan` consensus loop (Planner → Architect → Codex Critic, 4 iterations).
> **Date:** 2026-05-18. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Sections F1-F15 scope is LOCKED.** Out-of-scope items (deployment / CI / release) are LOCKED.

## Consensus Loop Provenance

| Iter | Architect | Codex Critic | Notes |
|---|---|---|---|
| 1 | likely-ITERATE (3 structural fixes: Tier-1 cap, false parallelism, BaseEntity collision) | **ITERATE** — 6 hard blockers + 1 independent finding (SP28 i18n scope contradiction) | 7 SPs (SP23-SP29) shape established |
| 2 | (skipped per Critic narrow-scope instruction) | **ITERATE** — 5/6 CLOSED, 1 PARTIAL (B3 BaseEntity in-place) | SP29 → /ax-verify subcommands; SP25 foundation; BaseEntity in-place chosen; SP28 Option β (new files only) |
| 3 | (skipped, ultra-narrow patch) | **ITERATE** — 3/4 defects CLOSED, 1 BLOCKING (Hibernate 6.4 @SoftDelete boolean-only conflict with deleted_at TIMESTAMP) | BaseEntity CREATE explicit; Flyway 8-table migration; 8-entity smoke list |
| **4** | (skipped, ultra-narrow patch) | **APPROVE** — Option B (per-subclass @SQLDelete) selected, @SoftDelete removed, ArchUnit guard added | INFORMATIONAL only: stale summary wording on lines 177/386 — fix during execution |

## ADR (final, Codex Critic-authored, iter4 APPROVE)

- **Decision:** Approve Iter4 as canonical PRD. F1-F15 Functional Capability Extension proceeds to /ralph autonomous sequential execution across SP23-SP29. SP25 soft-delete implemented via per-subclass timestamp `@SQLDelete` + BaseEntity `@Where` + `deletedAt` field.
- **Drivers:** Close self-referential rules; ship search and feature-flags as atomic Spec Trio domains; pre-execution policy/evidence/explanation feedback via existing `/ax-verify` subcommands (no new Tier-1 skills); preserve `deleted_at TIMESTAMP` semantics; avoid Hibernate 6.4 boolean-only `@SoftDelete`; binary-verifiable across all 8 existing entities; prevent future subclass drift with ArchUnit.
- **Alternatives considered:**
  - Mega-SP — rejected (blast radius)
  - 15 one-gap SPs — rejected (coordination overhead)
  - Defer F13-F15 — rejected (pre-execution feedback needed)
  - 3 new Tier-1 skills — rejected (`/ax-verify` subcommands satisfy same capability at lower surface area)
  - Hibernate 6.4 `@SoftDelete` on BaseEntity — rejected (boolean indicator only)
  - Schema → boolean soft-delete — rejected (violates timestamp contract)
  - Per-subclass `@SQLDelete` without guard — rejected (drift risk)
- **Why chosen:** Option B (per-subclass @SQLDelete) is timestamp-capable, explicit per table, compatible with existing 8-entity contract, now guarded by `BaseEntitySoftDeleteArchTest`. `/ax-verify` subcommands keep Tier-1 cap at 4. Spec Trio precedes domain (SP25 foundation before SP24/SP26 parallel).
- **Consequences:** SP25 has repetitive per-entity edits (8 entities × `@SQLDelete`), but repetition is enumerated and tested. Future BaseEntity subclasses must declare matching `@SQLDelete` — ArchUnit catches drift. SP count = 7. Tier-1 count = 4. New L4 domains: search (full_trio) + feature-flags (full_trio). New skills: 0 new Tier-1, 3 new subcommands within `/ax-verify`.
- **Follow-ups:** Clean stale summary wording at lines 177 and 386 during /ralph execution. Keep Hibernate snapshot evidence (records why @SoftDelete is intentionally not used). Existing L4 i18n migration deferred to future P1 SP (SP28 Option β scope).

## Consensus Loop Artifacts (audit trail)

- `2026-05-18-functional-extension-prd.draft.md` — iter 1 (851 lines, ITERATE)
- `2026-05-18-functional-extension-architect-review.md` — Architect iter 1
- `2026-05-18-functional-extension-critic-codex-iter1.md` — Codex Critic iter 1
- `2026-05-18-functional-extension-prd.iter2.md` — iter 2 (1024 lines, ITERATE — 5/6 CLOSED)
- `2026-05-18-functional-extension-critic-codex-iter2.md` — Codex Critic iter 2
- `2026-05-18-functional-extension-prd.iter3.md` — iter 3 (1108 lines, ITERATE — 3/4 CLOSED + Hibernate blocker)
- `2026-05-18-functional-extension-critic-codex-iter3.md` — Codex Critic iter 3
- `2026-05-18-functional-extension-prd.iter4.md` — iter 4 (1163 lines, APPROVED)
- `2026-05-18-functional-extension-critic-codex-iter4.md` — Codex Critic iter 4 (**APPROVE**)
- `2026-05-18-functional-extension-prd.md` — this file (canonical, APPROVED)

---

> The body below is the iter 4 content (1163 lines) — 7 SPs (SP23-SP29), Verification Matrix, Autonomous Execution Safety contracts, 5-scenario pre-mortem, allowlist updates, fixtures, provenance enums. Triggers /ralph autonomous sequential execution.

---

> **Original iter4 header retained below for traceability:**
> **Scope:** Functional gaps **F1–F15** identified in the post-`v1.0.0-catalog-complete` review. ONE PRD covering **SP23–SP29** (7 SPs). All public-release / CI / deployment workflow concerns are explicitly OUT OF SCOPE — see §12.
> **Predecessors:** `2026-05-17-frontend-templatization-prd.md` (CLOSED, APPROVED, SP1–SP12). `2026-05-18-catalog-extension-prd.md` (CLOSED, tag `v1.0.0-catalog-complete`, commit `9212989`, SP13–SP22).
> **Format:** RALPLAN-DR. Korean enterprise = composition-kit. React + Spring Boot equal partners.
> **Iter4 delta vs iter3:** see §13.2 (appended at file tail). 6 surgical edits to SP25 only.
> **Pre-flight disk fact (iter4):** `backend/build.gradle.kts:3` pins `org.springframework.boot 3.2.12` → ships Hibernate ORM 6.4.x. Hibernate 6.4 `@SoftDelete` is **boolean-only** (`SoftDeleteType` enum: ACTIVE / DELETED only — per https://docs.hibernate.org/orm/6.4/javadocs/org/hibernate/annotations/SoftDeleteType.html and https://docs.hibernate.org/orm/6.4/userguide/html_single/). Because this PRD's contract requires a `deleted_at TIMESTAMP` column with a real timestamp value (§6.3 Flyway migration + acceptance smoke), `@SoftDelete` is incompatible. **Option B (per-subclass `@SQLDelete`) is the selected path** regardless of Hibernate version.
> **Pre-flight disk fact (iter4):** `templates/backend/BaseEntity.java` is **ABSENT** in the workspace at iter4 start. The 8 existing SP16/17/18/19 entity files (`Notification`, `NotificationPreferences`, `AuditLog`, `StoredFile`, `EmailOutbox`, `EmailTemplate`, `ScheduledTask`, `JobHistory`) were authored without a real common parent. SP25 OWNS the create-or-repair of this file (fills the SP13 gap that iter2 incorrectly assumed closed).
> **Pre-flight disk fact (iter4):** `templates/backend/file-storage/StoredFile.java` confirmed present (was TBD in critic brief — verified).

---

## §1 RALPLAN-DR Summary

### Principles (5) — inherited verbatim from PRD §RALPLAN-DR + Catalog Extension §1, not re-litigated

1. **Composition kit, not single product.** Every new artifact (template / rule / skill / spec) must be fork-adoptable in isolation; no atom is single-application.
2. **Spec-before-code, evidence-anchored.** Every new template carries `evidence:` (frontmatter OR `@ax-template-meta`); every new rule carries `protects_template_id` + `failing_fixture_path`; every new ADR declares `provenance_class`.
3. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` skill returns exit 0. No "done" on prose. No advisory-only acceptance.
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count is **frozen at 4** (`/ax-transform`, `/ax-verify`, `/ax-scaffold`, `/ax-fork-receiver`). F13/F14/F15 ship as **subcommands of the existing `/ax-verify` Tier-1 skill** (`/ax-verify policy-check|evidence-fetch|explain`). Zero Tier-2/Tier-3 change.
5. **No speculative generality.** Every functional gap closes a specific L4 flow that breaks today, OR retires a self-reference (a rule that protects nothing) — see §3 P0 Inventory "Justification" column.

### Decision Drivers (top 3)

1. **Self-reference closure.** SP21 shipped `traceid-in-error-response` (Java) and `traceId-rendered-on-error-boundary` (React) but no observability template currently sets the MDC traceId; the rules currently protect nothing. F1 closes this loop and is therefore P0.
2. **Atomic Spec-Trio ordering (Critic mandate, inherited).** Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs. SP grouping for F6 (search) and F9 (feature-flags) honors this directly.
3. **Skill-orchestrated pre-execution gating without surface growth.** Today `/ax-verify` is post-execution (after AI agent has touched files). F13 closes the **first-turn** gap: AI agent learns before mutation. This is qualitatively different from post-mutation verify but lives in the **same skill family**, hence delivered as a subcommand of `/ax-verify`. F14 (evidence freshness) and F15 (rule explanation) are also subcommands — they share the rule-loader / snapshot-loader infrastructure with `/ax-verify`.

### Mode

**DELIBERATE.** Auto-triggered by: (a) multi-week wall-time, (b) ≥3 new failing-fixture rules, (c) cross-stack rules with PII implications (i18n string handling, feature-flag admin endpoints), (d) shared backend infra (BaseEntity extension, JobDispatcher, PageRequestNormalizer) that crosses SP boundaries.

### Viable Options Considered (≥2 mandatory)

- **Option A — Single mega-SP (SP23-only) shipping all 15 functional gaps.**
  - Pros: one rollback boundary; one merge tag.
  - Cons: 15 functional gaps × ~6 surfaces each ≈ 90 atoms in one SP = unmanageable blast radius; violates §6 atomic rollback rule from Catalog Extension SP16/17/18/19 (atomic domain SP); defeats `/team` parallelism (4–5 agents must work disjoint surfaces).
  - **Rejected.**

- **Option B — SP-per-functional-area-group with shared-infra-first serialization (current proposal, 7 SPs SP23–SP29).**
  - Pros: each SP groups ≤3 functional gaps with shared verify skill; atomic Spec-Trio rule honored for F6 + F9; F13/F14/F15 ship as `/ax-verify` subcommands in SP29 (no Tier-1 growth); observability (F1) precedes integration (F2) which precedes data layer (F3) — natural infra dependency order; SP25 ships shared interfaces (`JobDispatcher`, `PageRequestNormalizer`, in-place `BaseEntity` soft-delete extension) **before** SP24/SP26 so those two SPs can run safely in parallel against a stable foundation.
  - Cons: 7 SPs sequential at the SP23→SP25 ridge mean wall-time ≈ 10–12 d if serial; SP25 carries the largest "blast radius" because every existing domain entity extends `BaseEntity`. Mitigated: §6.4 atomic-revert per SP, §7.1 rollback tags, §8 Scenario 5 pre-mortem covering the BaseEntity/JobDispatcher/PageRequest shared-surface race.
  - **CHOSEN.** Iter2 fixes the false-parallelism claim from iter1 (Critic Blocker 2): SP25 lands the shared data/jobs foundation BEFORE SP24/SP26 can run in parallel.

- **Option C — Split atomic domain SPs separately (F6 search-only as its own SP, all others as a separate SP each).**
  - Pros: minimum blast radius per SP.
  - Cons: 15 SPs = coordination overhead exceeds value; observability + cache (F1+F10) share verify skill and snapshots, so splitting them adds redundant `evidence:` work; ditto F2 + F12 (both touch outbound HTTP retry policy).
  - **Rejected.**

- **Option D — Defer F13–F15 (any form) until after F1–F12 ship.**
  - Pros: subcommands land against a known-stable catalog; less coupling risk during catalog growth.
  - Cons: F13 (`/ax-verify policy-check`) is the **first-turn** gate; deferring it means SP23–SP28 ship templates that downstream AI agents cannot pre-check. Loses 6 SPs of empirical feedback for F13's rule matching.
  - **Rejected.** F13/14/15 ship in SP29 immediately after F1–F12 so they have a fully populated catalog to demo, but BEFORE the next PRD cycle so the subcommands are available from cycle-2 day-1.

- **Option E (iter1's path, now retired) — Ship F13/F14/F15 as 3 new Tier-1 skills.**
  - Pros: each capability gets a top-level name.
  - Cons: Tier-1 count jumps 4→7 in one cycle; violates Principle 4 (few exposed surfaces); none of F13/F14/F15 are first-class verbs from a user-mental-model standpoint (all are verification sub-modes); Architect iter1 §1 antithesis identifies this as load-bearing; Critic iter1 Blocker 1 mandates the flip.
  - **Rejected (iter1→iter2 flip).** The iter1 draft acknowledged this fallback at its own line 550; iter2 promotes the fallback to default.

### Recommended: **Option B — SP-per-functional-area-group, shared-infra-first**

Cycle = **SP23 (F1 observability + F10 cache, shared verify-java + shared snapshots)** → **SP25 (F3 data + F4 jobs — foundation: in-place BaseEntity soft-delete extension, JobDispatcher interface, PageRequestNormalizer)** → **SP24 (F2 integration + F12 export/import) ‖ SP26 (F5 charts L2 + F6 search atomic domain)** (parallel after SP25 lands shared interfaces) → **SP27 (F7 realtime + F11 forms) ‖ SP28 (F8 i18n + F9 feature flags)** → **SP29 (F13/F14/F15 as 3 subcommands of `/ax-verify`, integration over full post-SP28 tree)**. Total: **7 SPs, ≈ 10–11 d wall-time**.

---

## §2 Context

### Completed cycles

- **Cycle 1 (PRD-1):** `docs/superpowers/specs/2026-05-17-frontend-templatization-prd.md` — 13 SPs (SP1–SP12 implementation + SP0 baseline). 4-iter ralplan APPROVE. Closed.
- **Cycle 2 (Catalog Extension):** `docs/superpowers/specs/2026-05-18-catalog-extension-prd.md` — 10 SPs (SP13–SP22). Tag `v1.0.0-catalog-complete` (commit `9212989`). Closed.

### Current catalog totals (post-`v1.0.0-catalog-complete`)

| Surface | Count |
|---|---|
| L1 primitives (`templates/L1/components/*.tsx`) | 40 (verified: `ls templates/L1/components/ \| wc -l` = 39 + index.ts entries) |
| L2 blocks (`templates/L2/blocks/*.tsx`) | 33 |
| L3 page templates (`templates/L3/pages/*/`) | 15 (verified directory count) |
| L4 domains (`templates/L4/*/`) | 7 (audit-log, auth, crud, file-storage, notification, payment, practices) |
| Backend cross-cutting (`templates/backend/controllers/`, `services/`, `repositories/`, `dto/`, `error/`, `security/`, `config/`) | 10 |
| Backend domain skeletons (`templates/backend/{audit-log,email-outbox,file-storage,notification,scheduled-task}/`) | 5 |
| Skills | 18 (Tier-1: 4 = `ax-transform`, `ax-verify`, `ax-scaffold`, `ax-fork-receiver`; Tier-2: 8; Tier-3: 6) |
| Java rules (`practices/rules/*.md`) | 72 |
| React rules (`practices-react/rules/*.md`) | 75 |
| Upstream snapshots (`practices/upstream/*.snapshot.md`) | 43 |
| Spec Trios | 8 (5 full_trio: auth, crud, payment, notification, audit-log, file-storage; 2 backend_only: email-outbox, scheduled-task, ratelimit; 1 frontend_only: practices, settings) |
| Guards (`practices/evals/*_guard.sh`) | 19/19 GREEN |
| Playwright tests | 287/288 (1 known-flaky tracked) |

### This PRD's scope

User identified **15 functional gaps (F1–F15)** that prevent the catalog from being **self-sufficient for fork receivers**. The gaps are not catalog atoms missing (those were closed in Catalog Extension); they are **functional capability classes** that an enterprise fork would need within their first 90 days but cannot find in the current catalog. Examples:

- F1 (observability) — SP21 shipped `traceid-in-error-response` rule that **currently protects nothing** because no template sets MDC traceId. Self-reference closure.
- F6 (search) — Every enterprise fork hits search-with-Korean-IME on day 1. No domain template exists.
- F13 (`/ax-verify policy-check`) — AI agent's **first turn** has no binary BLOCK/PROCEED signal; only `/ax-verify` (post-mutation) exists.

Cycle 3 closes these 15 gaps across 7 SPs (SP23–SP29).

---

## §3 Objectives + Guardrails

### Objectives (one per SP minimum)

- **O1 (SP23):** Make observability + cache strategy **fork-runnable**. After SP23, a fork receiver can `bash skills/ax-verify-java/scripts/run.sh templates/backend/observability/` and get binary signal that MDC traceId, OTLP exporter, structured logging, and Caffeine/Redis cache are wired correctly. (Verify skill corrected from iter1 typo `ax-verify-domain observability` → `ax-verify-java`.)
- **O2 (SP25):** Make data layer (Flyway + JPA audit + in-place BaseEntity soft-delete extension + jsonb + optimistic-lock + standard paging) and on-demand background jobs **fork-runnable** without reinventing schema-migration and queue scaffolding. **SP25 now lands BEFORE SP24/SP26** because it owns shared interfaces (`JobDispatcher`, `PageRequestNormalizer`) and in-place mutation of the SP13 `BaseEntity` template that every existing domain entity extends.
- **O3 (SP24):** Make external HTTP integration + bulk export/import **fork-runnable** with circuit breaker + HMAC + chunked-import policy. Closes the audit gap where no webhook signature rule was wired despite SP21 demoting `webhook-signature-verify` to P1. Consumes SP25's `JobDispatcher` interface and `PageRequestNormalizer`.
- **O4 (SP26):** Add charts L2 cluster + ship `search` as atomic full_trio domain (Spec Trio + backend FTS adapter + L2 SearchPalette/TypeaheadSearch + L4 page). Korean IME behavior coverage extends to typeahead. Consumes SP25's `PageRequestNormalizer` for paged search responses.
- **O5 (SP27):** Add realtime/SSE backend bridge + L2 (LivePresence, OptimisticUpdate, EventStream) + form orchestration L2 (FieldArray, ConditionalField, DependentField, FormErrorSummary, AutoSaveIndicator, DirtyGuard, FormSection). **Realtime defaults to polling**; SSE/WebSocket is opt-in via blueprint manifest (architect polish from iter1).
- **O6 (SP28):** i18n/locale (LocaleProvider, ko-KR + en-US, KRW format, fallback chain, blueprint manifest) + feature flags as `feature-flags: full_trio` (admin UI + L1 `FeatureGate` + L2 `FeatureFlagToggle` + service-layer template). i18n rule `applies_to` is scoped to files created on/after 2026-05-18; existing L4 string migration is out of scope (Critic Blocker 4 — Option β).
- **O7 (SP29):** Extend the existing `/ax-verify` Tier-1 skill with 3 subcommands (`policy-check`, `evidence-fetch`, `explain`). Closes the pre-execution gate gap (F13), the manual upstream-refresh gap (F14), and the rule-violation-explanation gap (F15). **Zero new Tier-1 skills**; Tier-1 count stays at 4.

### Guardrails — Must Have

- Every new template (L1/L2/L4/backend) carries `evidence:` frontmatter OR an `@ax-template-meta` comment block. No exceptions.
- Every new rule carries `protects_template_id` (pointing to a specific template id under `templates/`) AND `failing_fixture_path` (pointing to a `practices/evals/fixtures/<rule>/fail_*/` directory). Inherits Catalog Extension SP21 Critic anti-bloat enforcement.
- Spec Trio ordering: F6 search ships as atomic SP (Spec Trio + backend + L2 + L4 in ONE SP). F9 feature-flags ships as atomic SP (Spec Trio + backend + L1 + L2 + L4 in ONE SP).
- Skill topology cap: **Tier-1 count frozen at 4**. F13/F14/F15 ship as subcommands of `/ax-verify`. Zero Tier-2/Tier-3 change.
- Allowlist additions (`practices/evals/trio_integrity_allowlist.yaml`): `search: full_trio`, `feature-flags: full_trio`. **Race-safe append protocol:** rebase against HEAD before commit; `yq`-sorted insertion (per Catalog Extension §6.2); SP closing order serialized (SP26 commits its entry before SP28 begins its trio_integrity step).
- Hyphenated domain names (`search`, `feature-flags`) — confirmed supported by `run-gradle.sh` per SP16 side-fix in Catalog Extension.
- Every new failing fixture causes the named guard to exit non-zero with the named error string; the `pass/` sibling fixture exits 0.
- i18n rule `applies_to: paths_created_after_2026-05-18` exception: rule does NOT run against existing `templates/L4/{auth,crud,payment,practices,notification,audit-log,file-storage}/` paths.

### Guardrails — Must NOT

- No GitHub Actions workflows added (out of scope; see §12).
- No LICENSE/CONTRIBUTING/docs-site/release.yml/Dependabot files added (out of scope).
- No v2 architectural overhaul — skill topology stays 3-tier; Spec Trio schema unchanged; `ax-template-meta` shape unchanged; **Tier-1 count stays at 4**.
- No new Tier-1, Tier-2, or Tier-3 skills.
- No new sibling `BaseEntityWithSoftDelete` class — soft-delete annotations are applied **in place** on the SP13 common `BaseEntity` template (Critic Blocker 3).
- No bounded migration of existing L4 Korean strings in SP28 (out of scope — Critic Blocker 4 Option β).
- No placeholder `exit 0` guard stubs. Every new gate exits non-zero on real failure.
- No raw `./gradlew testXxx` or `npm run xxx` as a user-facing surface in skills — every skill calls the underlying script with stable args (already enforced by §7 ADR TD-2026-05-17-007 from PRD-1).
- No new MockMvc tests; RestAssured only.
- No `@SpringBootTest` slow tests in new failing-fixture suites — fixture tests use archunit OR static-script OR Vitest (fast).
- No realtime transport (SSE/WebSocket) hardwired as L4 default — polling is default; SSE/WebSocket opt-in via `blueprints/realtime-policy-manifest.yaml` (architect polish).

---

## §4 P0 Inventory (15 functional gaps, atomicized to 7 SPs)

> Each F-row cites either (a) the L4 flow that breaks today, OR (b) which existing rule/template protects nothing without the gap closure. Effort: S = ≤4h, M = 4–8h, L = 8–16h, XL = >16h. All "Owning verify skill" entries are existing skills (zero new Tier-2/Tier-3, zero new Tier-1).

| F | Surface | Justification (cite the broken L4 flow OR the self-reference) | Effort | Owning verify skill | Depends-on | Ships in SP |
|---|---|---|---|---|---|---|
| F1 | Backend observability templates (`templates/backend/observability/`) | SP21 rule `practices/rules/traceid-in-error-response.md` currently `protects_template_id: templates/backend/error/ProblemDetailFactory.java`, but `ProblemDetailFactory.java` (`templates/backend/error/`) reads from `MDC.get("traceId")` which is never set because **no template wires MDC**. Self-reference closure. | M | `/ax-verify-java` | none | SP23 |
| F10 | Cache strategy templates (`templates/backend/cache/`) + `practices/rules/cacheable-requires-explicit-ttl.md` | L4 `templates/L4/payment/` uses `@Cacheable` indirectly via `PaymentMethodService` (existing `backend/.../PaymentMethodService.java` example) but no template demonstrates how. Caffeine + Redis split is the canonical Spring Boot pattern; we ship neither. Rule failing fixture: `@Cacheable("x")` without `cacheManager` + TTL. | M | `/ax-verify-java` | F1 (cache stats emit via Micrometer registered in F1) | SP23 |
| F3 | Data layer (`templates/backend/data/`) — foundation for SP24/SP26 | Existing rule `practices/rules/api-pagination-pageable.md` references `PageResponse.java` (SP13) but no `PageRequestNormalizer.java` exists to enforce the **max-limit** policy. JPA `@Version` rule (`practices/rules/jpa-optimistic-locking.md`, see catalog) protects no template. Flyway naming convention is undocumented. New rule `soft-delete-only-on-base-entity` — failing fixture `fail_soft_delete_no_base/Entity.java`. **In-place soft-delete extension of SP13 `BaseEntity` template** affects: `Notification.java`, `NotificationPreferences.java`, `AuditLog.java`, `StoredFile.java`, `EmailOutbox.java`, `EmailTemplate.java`, `ScheduledTask.java`, `JobHistory.java` (all SP16/17/18/19 entities that extend BaseEntity inherit soft-delete automatically). | M | `/ax-verify-java` | F1 | SP25 |
| F4 | Background jobs on-demand (`templates/backend/jobs/`) — foundation for SP24 export | Existing `templates/backend/scheduled-task/` covers time-based (cron). On-demand (user-triggered, async) is not templated. L4 `templates/L4/payment/` payment async confirmation pattern is hand-rolled in `backend/src/main/java/.../payment/`. JobDispatcher + JobQueue (Redis Streams OR DB-row queue) + JobWorker + JobHistoryProjection. Reuses SP19 scheduled-task pattern for the time-based half. **JobDispatcher interface lands here**; SP24's `ExportJobService` consumes it. | M | `/ax-verify-java` | F3 (DB-row queue uses Flyway from F3); F1 (DLQ metrics) | SP25 |
| F2 | External integration (`templates/backend/integration/`) | Existing rule `practices/rules/http-explicit-timeouts.md` protects nothing in `templates/backend/` (no `WebClient` template exists). `practices/rules/http-restclient-over-resttemplate.md` ditto. SP24 closes both self-references. New rule `webhook-hmac-required` — failing fixture `fail_webhook_no_hmac/Controller.java` accepts webhook POST without `X-Signature` header validation. | L | `/ax-verify-java` | F1 (circuit-breaker emits metrics); SP25 done (`JobDispatcher` for retry queue) | SP24 |
| F12 | Export/Import (`templates/backend/export-import/`) + L2 ImportPreview/MappingEditor/ImportProgressBar | L4 `templates/L4/audit-log/` has `audit-log-export` page (`templates/L3/pages/export-job-status/`) but no backend export service template — page is a shell. New rule `chunked-import-required-when-rowcount-gt-1000` — failing fixture `fail_single_tx_10k_rows/Service.java` reads 10k rows in one TX → archunit fails. | L | `/ax-verify-java` + `/ax-verify-L2` | SP25 done (reuses JobDispatcher for long-running export) | SP24 |
| F5 | Charts/dataviz L2 (`templates/L2/blocks/`) | TimeSeriesChart, BarChart, PieChart, KPICard, Sparkline, Heatmap. No L4 today renders charts; payment-revenue-trend dashboard and audit-log timeline both need them. `recharts` (Apache-2.0) is the proposed wrapper. New upstream snapshot: `recharts-2026-05.snapshot.md`. | M | `/ax-verify-L2` | none | SP26 |
| F6 | Search atomic domain (`specs/search-l0.yaml` + `contracts/search-openapi.yaml` + `blueprints/search-manifest.yaml` + `templates/backend/search/` + `templates/L2/blocks/{search-palette-extended,typeahead-search,result-highlighter,recent-searches}.tsx` + `templates/L4/search/`) | Universal SaaS surface. Hangul IME (SP21 rule #65) currently protects only `combobox` and `search-palette` (SP15 existing); typeahead-search has no IME coverage. Existing `templates/L2/blocks/search-input.tsx` is a shell. Allowlist add: `search: full_trio`. Consumes SP25's `PageRequestNormalizer` for paged results. | L | `/ax-verify-domain search` (existing Tier-2 skill, registry entry added) | F1; SP25 done (PageRequestNormalizer) | SP26 |
| F7 | Realtime/SSE (`templates/backend/realtime/` + `templates/L2/blocks/{live-presence,optimistic-update,event-stream}.tsx`) + **`blueprints/realtime-policy-manifest.yaml` declaring polling as default transport** | L4 `templates/L4/notification/` bell currently polls via TanStack Query (per SP16 deliberate trade); SSE backend bridge unshipped but is **opt-in only** in SP27. L4 `templates/L4/audit-log/` live-tail unimplemented — polling default; SSE opt-in. L4 `templates/L4/payment/` status transition events: polling default; SSE opt-in. RealtimeEventBus = DB outbox → SSE bridge (reuses email-outbox pattern). | M | `/ax-verify-java` + `/ax-verify-L2` | F4 (Job DLQ events feed event-bus); F1 (SSE connection metrics when SSE opted in); SP25 done | SP27 |
| F11 | Form orchestration L2 (FieldArray, ConditionalField, DependentField, FormErrorSummary, AutoSaveIndicator, DirtyGuard, FormSection) | SP15 shipped `field-array.tsx`, `form-section.tsx`, `conditional-field.tsx`, `form-error-summary.tsx` as **shells with TODO comments** (Critic flagged SP15 §11). SP27 promotes them to RHF + Zod standardized blocks; adds DependentField + AutoSaveIndicator + DirtyGuard (missing). Standardizes `useForm()` signature across all forms. | M | `/ax-verify-L2` | none; shared SP27 with F7 (both grow L2) | SP27 |
| F8 | i18n / Locale (`templates/L1/components/{locale-switcher,currency-formatter,relative-time}.tsx` + `templates/L2/blocks/{locale-provider,translation-boundary}.tsx` + `blueprints/i18n-policy-manifest.yaml`) | Hardcoded Korean strings in `templates/L4/auth/` + `templates/L4/payment/` + `templates/L4/file-storage/` (verified by Critic iter1, lines 79). Korean enterprise default must be ko-KR + en-US with KRW format. **New rule `no-hardcoded-user-facing-string-in-l4` applies ONLY to files created on/after 2026-05-18** — failing fixture `fail_hardcoded_korean_string/page.tsx` (regex matches non-ASCII string literal outside `t()` wrapper). Existing L4 migration is OUT OF SCOPE (deferred to a future P1 SP). Reuses `next-intl`. | M | `/ax-verify-L1` + `/ax-verify-L2` | none | SP28 |
| F9 | Feature flags (`templates/backend/feature-flags/` + `templates/L1/components/feature-gate.tsx` + `templates/L2/blocks/feature-flag-toggle.tsx` + `specs/feature-flags-l0.yaml` + `contracts/feature-flags-openapi.yaml` + `blueprints/feature-flags-manifest.yaml`) | Allowlist add: `feature-flags: full_trio`. Enterprise forks deploy flags within first 30 days. Today no template exists. New rule `prefer-feature-gate-over-env-check` — failing fixture `fail_process_env_check/Component.tsx` checks `process.env.FEATURE_X === '1'` instead of `<FeatureGate name="X">`. | M | `/ax-verify-domain feature-flags` (registry add) | shared SP28 with F8 (both touch L4 wiring + admin UI) | SP28 |
| F13 | `/ax-verify policy-check` (subcommand of existing Tier-1) | `/ax-verify` is **post-mutation**. AI agent's first turn writes a file, then learns. F13 is **pre-mutation**: input = file path + intent (free-text); output = applicable rules + `protects_template_id` matches + required evidence locations + STOP/PROCEED hint. Failing fixture: cold call with a path that violates `no-cross-l4-domain-imports` → subcommand outputs `STOP` + cites the rule. | M | self-tested via `skills/ax-verify/scripts/_subcommand-self-test.sh policy-check` | All prior SPs (must demo against full catalog) | SP29 |
| F14 | `/ax-verify evidence-fetch` (subcommand of existing Tier-1) | `time_decay_guard` flags stale snapshots but **refresh is manual** (`practices/upstream/fetch.sh` requires human invocation per snapshot). F14: input = snapshot id (or `--all`); output = WebFetch → diff vs current snapshot → refresh recommendation. Quarterly stale auto-detection. | M | self-tested via `skills/ax-verify/scripts/_subcommand-self-test.sh evidence-fetch` | F1 (uses Micrometer counter for refresh-attempts); shared SP29 with F13 | SP29 |
| F15 | `/ax-verify explain` (subcommand of existing Tier-1) | When `/ax-verify` outputs `RULE_VIOLATED: no-server-component-state-leakage-to-client`, the AI agent has no machine-readable trace to `protects_template_id` → `evidence:` → violating-vs-compliant examples → related rules. F15 closes the rule-violation explanation gap for both AI agents and humans. | S | self-tested via `skills/ax-verify/scripts/_subcommand-self-test.sh explain`; shared SP29 with F13/F14 | F13 (reuses rule-loader infrastructure) | SP29 |

**Total functional gaps: 15. Total atoms (cross-counting templates + rules + subcommands + specs): ~80** (rough; concrete §5 deliverables tables are the source of truth per SP).

---

## §5 Inventory by Surface

### §5.1 Backend templates added (per domain folder)

| Folder | New templates | Source SP |
|---|---|---|
| `templates/backend/observability/` (NEW) | `MdcCorrelationIdInterceptor.java`, `OtelTracerConfig.java`, `MicrometerConfig.java`, `StructuredLoggingConfig.java`, `LogbackJsonAppenderConfig.java`, `DatabaseHealthIndicator.java`, `RedisHealthIndicator.java`, `ExternalHttpHealthIndicator.java` (8 files) | SP23 |
| `templates/backend/cache/` (NEW) | `CaffeineConfig.java`, `RedisCacheConfig.java`, `CacheKeyGenerator.java`, `CacheTtlPolicy.java` (4 files) | SP23 |
| `templates/backend/data/` (NEW + 1 IN-PLACE EXTEND) | `FlywayConfig.java`, `JpaAuditConfig.java`, `SecurityAuditorAware.java`, `SoftDeleteAspect.java`, `JsonbConverter.java`, `OptimisticLockingPolicy.java`, `PageRequestNormalizer.java` (7 NEW files) **PLUS** **in-place extension of `templates/backend/BaseEntity.java` (SP13 template)** with `@SQLDelete(sql="UPDATE … SET deleted_at = now() WHERE id = ?")` + `@Where(clause = "deleted_at IS NULL")` + `deletedAt` field. NO new sibling class. | SP25 |
| `templates/backend/jobs/` (NEW) | `JobDispatcher.java` (interface), `JobQueue.java` (interface), `RedisStreamsJobQueue.java`, `DbRowJobQueue.java`, `JobWorker.java`, `JobHistoryProjection.java`, `JobDlqHandler.java` (7 files) | SP25 |
| `templates/backend/integration/` (NEW) | `WebClientConfig.java`, `ExternalApiTemplate.java`, `WebhookReceiver.java`, `WebhookSender.java`, `BulkheadConfig.java`, `CircuitBreakerConfig.java` (6 files) | SP24 |
| `templates/backend/export-import/` (NEW) | `CsvImportService.java`, `ExcelImportService.java`, `ExportJobService.java` (consumes SP25 `JobDispatcher`), `ImportErrorReportDto.java`, `ImportChunkProcessor.java` (5 files) | SP24 |
| `templates/backend/search/` (NEW; atomic with F6 Spec Trio) | `SearchIndexService.java`, `SearchController.java`, `SearchDto.java`, `SearchQueryParser.java`, `PostgresFtsAdapter.java`, `MeilisearchAdapter.java`, `SearchBackend.java` (interface, 7 files) | SP26 |
| `templates/backend/realtime/` (NEW) | `SseEmitterConfig.java`, `RealtimeEventBus.java`, `SseSubscription.java`, `WebSocketConfig.java`, `RealtimeOutboxRelay.java` (5 files) — **all opt-in transports gated by `blueprints/realtime-policy-manifest.yaml`** | SP27 |
| `templates/backend/feature-flags/` (NEW; atomic with F9 Spec Trio) | `FeatureFlagEntity.java`, `FeatureFlagService.java`, `FeatureFlagController.java`, `FeatureFlagAdminController.java`, `FeatureFlagRepository.java`, `FeatureFlagCache.java` (6 files) | SP28 |

**Total new backend templates: 55 across 9 folders + 1 in-place edit of `templates/backend/BaseEntity.java`.**

### §5.2 L1 components added (i18n primitives + feature gate)

| File | Source SP |
|---|---|
| `templates/L1/components/locale-switcher.tsx` | SP28 |
| `templates/L1/components/currency-formatter.tsx` | SP28 |
| `templates/L1/components/relative-time.tsx` | SP28 |
| `templates/L1/components/feature-gate.tsx` | SP28 |

**Total new L1: 4** (40 → 44).

### §5.3 L2 blocks added

| Cluster | Files | Source SP |
|---|---|---|
| Charts (F5) | `time-series-chart.tsx`, `bar-chart.tsx`, `pie-chart.tsx`, `kpi-card.tsx`, `sparkline.tsx`, `heatmap.tsx` (6) | SP26 |
| Search (F6) | `search-palette-extended.tsx`, `typeahead-search.tsx`, `result-highlighter.tsx`, `recent-searches.tsx` (4) | SP26 |
| Realtime (F7) | `live-presence.tsx`, `optimistic-update.tsx`, `event-stream.tsx` (3) | SP27 |
| Forms (F11) | `field-array-extended.tsx`, `conditional-field-extended.tsx`, `dependent-field.tsx`, `form-error-summary-extended.tsx`, `auto-save-indicator.tsx`, `dirty-guard.tsx`, `form-section-extended.tsx` (7; the `-extended` suffix is the shell-promote-to-block contract — SP15 shells stay for back-compat, new files supersede) | SP27 |
| Import/Export (F12) | `import-preview.tsx`, `mapping-editor.tsx`, `import-progress-bar.tsx` (3) | SP24 |
| i18n (F8) | `locale-provider.tsx`, `translation-boundary.tsx` (2) | SP28 |
| Feature flags (F9) | `feature-flag-toggle.tsx` (1; admin UI) | SP28 |

**Total new L2: 26** (33 → 59).

### §5.4 L3 page templates added

| File | Source SP |
|---|---|
| `templates/L3/pages/search-results-page/` (resurrected from P1 demotion in Catalog Extension, now justified by F6 search atomic SP) | SP26 |

**Total new L3: 1** (15 → 16).

### §5.5 L4 domains added

| Domain | Mode | Files | Source SP |
|---|---|---|---|
| `search` | `full_trio` | `templates/L4/search/app/(search)/page.tsx`, `templates/L4/search/app/(search)/results/page.tsx`, `templates/L4/search/middleware.ts`, `templates/L4/search/next.config.ts`, `templates/L4/search/README.md` | SP26 |
| `feature-flags` | `full_trio` (admin-only UI) | `templates/L4/feature-flags/app/(admin)/feature-flags/page.tsx`, `templates/L4/feature-flags/app/(admin)/feature-flags/[name]/page.tsx`, `templates/L4/feature-flags/middleware.ts`, `templates/L4/feature-flags/next.config.ts`, `templates/L4/feature-flags/README.md` | SP28 |

**Total new L4: 2** (7 → 9).

### §5.6 New rules per catalog (anti-bloat cap honored)

| Rule | Catalog | `protects_template_id` | `failing_fixture_path` | `applies_to` exception | Source SP |
|---|---|---|---|---|---|
| `mdc-traceid-required-on-controller` (Java) | `practices/rules/` | `templates/backend/observability/MdcCorrelationIdInterceptor.java` | `practices/evals/fixtures/mdc_traceid_required/fail_no_interceptor/` | (none) | SP23 |
| `cacheable-requires-explicit-ttl` (Java) | `practices/rules/` | `templates/backend/cache/CacheTtlPolicy.java` | `practices/evals/fixtures/cacheable_ttl/fail_no_ttl/` | (none) | SP23 |
| `soft-delete-only-on-base-entity` (Java) | `practices/rules/` | `templates/backend/BaseEntity.java` (in-place extended) | `practices/evals/fixtures/soft_delete_base/fail_soft_delete_no_base/` | (none) | SP25 |
| `webhook-hmac-required` (Java) | `practices/rules/` | `templates/backend/integration/WebhookReceiver.java` | `practices/evals/fixtures/webhook_hmac/fail_no_signature_check/` | (none) | SP24 |
| `chunked-import-required-when-rowcount-gt-1000` (Java) | `practices/rules/` | `templates/backend/export-import/ImportChunkProcessor.java` | `practices/evals/fixtures/chunked_import/fail_single_tx_10k/` | (none) | SP24 |
| `no-hardcoded-user-facing-string-in-l4` (React) | `practices-react/rules/` | `templates/L2/blocks/translation-boundary.tsx` | `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/` | **`applies_to: paths_created_after_2026-05-18`** — does NOT run against pre-existing `templates/L4/{auth,crud,payment,practices,notification,audit-log,file-storage}/` files | SP28 |
| `prefer-feature-gate-over-env-check` (React) | `practices-react/rules/` | `templates/L1/components/feature-gate.tsx` | `practices-react/evals/fixtures/feature_gate/fail_process_env_check/` | (none) | SP28 |

**Total new rules: 7** (5 Java + 2 React). Cap respected: ≤ 1 rule per functional gap on average, each anchored to a concrete `protects_template_id` shipped in the same SP.

### §5.7 New skills / subcommands (zero new Tier-1, Tier-2, Tier-3)

| Surface | Type | Path | Source SP |
|---|---|---|---|
| `/ax-verify policy-check` | Subcommand of existing Tier-1 `/ax-verify` | `skills/ax-verify/scripts/policy-check.sh` (new), invoked via `skills/ax-verify/scripts/run.sh policy-check <args>` (dispatcher updated) | SP29 |
| `/ax-verify evidence-fetch` | Subcommand of existing Tier-1 `/ax-verify` | `skills/ax-verify/scripts/evidence-fetch.sh` (new), invoked via `skills/ax-verify/scripts/run.sh evidence-fetch <args>` | SP29 |
| `/ax-verify explain` | Subcommand of existing Tier-1 `/ax-verify` | `skills/ax-verify/scripts/explain.sh` (new), invoked via `skills/ax-verify/scripts/run.sh explain <args>` | SP29 |
| `/ax-verify` SKILL.md update | Extends existing SKILL.md frontmatter `usage:` block to document the 3 subcommands | `skills/ax-verify/SKILL.md` (edit-in-place) | SP29 |
| `/ax-verify` self-test runner | Single self-test harness with 3 cases (one per subcommand) | `skills/ax-verify/scripts/_subcommand-self-test.sh` | SP29 |

**Total skills: 18** (unchanged). **Tier-1 count: 4** (unchanged). Tier-2 / Tier-3 unchanged.

### §5.8 New upstream snapshots

| Snapshot | Source | Source SP |
|---|---|---|
| `practices/upstream/spring-boot-actuator-2026-05.snapshot.md` (extend existing or add suffix; new probes) | Spring docs | SP23 |
| `practices/upstream/opentelemetry-java-2026-05.snapshot.md` (NEW) | OpenTelemetry Java instrumentation docs | SP23 |
| `practices/upstream/micrometer-prometheus-2026-05.snapshot.md` (NEW) | Micrometer docs | SP23 |
| `practices/upstream/spring-flyway-2026-05.snapshot.md` (extend or NEW) | Flyway + Spring Boot docs | SP25 |
| `practices/upstream/spring-data-jpa-auditing-2026-05.snapshot.md` (NEW) | Spring Data JPA docs | SP25 |
| `practices/upstream/resilience4j-2026-05.snapshot.md` (NEW) | Resilience4j docs | SP24 |
| `practices-react/upstream/recharts-2026-05.snapshot.md` (NEW) | recharts docs | SP26 |
| `practices/upstream/postgres-fts-2026-05.snapshot.md` (NEW) | PostgreSQL docs | SP26 |
| `practices/upstream/meilisearch-2026-05.snapshot.md` (NEW) | Meilisearch docs | SP26 |
| `practices/upstream/spring-mvc-sse-2026-05.snapshot.md` (NEW) | Spring MVC docs | SP27 |
| `practices-react/upstream/next-intl-2026-05.snapshot.md` (NEW) | next-intl docs | SP28 |
| `practices-react/upstream/react-hook-form-2026-05.snapshot.md` (NEW or extend) | RHF docs | SP27 |

**Total new snapshots: ~12.** `_MANIFEST.yaml` regenerated atomically at SP-close per SP.

### §5.9 New ADRs (TD-2026-05-18-NNN)

| ADR id | Title | `provenance_class` |
|---|---|---|
| TD-2026-05-18-023 | Observability + Cache baseline (F1 + F10) | `internal_design` (anchored to OTLP / Micrometer / Caffeine canon) |
| TD-2026-05-18-025 | Data layer + Background jobs (F3 + F4) — foundation SP, in-place BaseEntity extension | `internal_design` (Flyway + JPA + Redis Streams canon) |
| TD-2026-05-18-024 | External integration + Export/Import (F2 + F12) | `internal_design` (anchored to Resilience4j + Apache POI canon) |
| TD-2026-05-18-026 | Charts L2 + Search atomic domain (F5 + F6) | `external_canonical` (recharts + PostgreSQL FTS canon) |
| TD-2026-05-18-027 | Realtime SSE + Form orchestration (F7 + F11) — polling default, SSE/WebSocket opt-in | `internal_design` |
| TD-2026-05-18-028 | i18n + Feature flags (F8 + F9); i18n `applies_to: paths_created_after_2026-05-18` | `internal_design` + `locked_constraint` (KRW formatting per ISO 4217) |
| TD-2026-05-18-029 | `/ax-verify` subcommand extension (F13 + F14 + F15); Tier-1 count stays at 4 | `internal_design` (composition-kit AI agent UX) |

---

## §6 Implementation Plan (SP23–SP29)

### §6.1 SP dependency graph (iter2 — corrected)

```
SP23 (F1 observability + F10 cache)
  │
  ▼
SP25 (F3 data + F4 jobs — foundation: BaseEntity in-place extension,
       JobDispatcher interface, PageRequestNormalizer)
  │
  ├──────────────────────────┐
  ▼                          ▼
SP24 (F2 integration         SP26 (F5 charts + F6 search atomic)
      + F12 export/import,   (parallel — consumes PageRequestNormalizer)
      consumes JobDispatcher)
  │                          │
  └────────┬─────────────────┘
           ▼
SP27 (F7 realtime opt-in + F11 forms)   ‖   SP28 (F8 i18n + F9 feature-flags)
           │                                        │
           └────────────────┬───────────────────────┘
                            ▼
                          SP29 (`/ax-verify` subcommands: policy-check,
                                evidence-fetch, explain — no new Tier-1)
```

**Critical ordering rules (iter2):**

- **SP23 lands first**: observability infra (MDC, Micrometer, structured logging) underpins downstream verify metrics and DLQ counters in SP24/25/27.
- **SP25 lands second**: it owns the shared foundation that SP24 and SP26 both depend on:
  - **`BaseEntity` in-place extension** (`templates/backend/BaseEntity.java`) — every existing SP13–SP19 entity that extends `BaseEntity` (`Notification`, `NotificationPreferences`, `AuditLog`, `StoredFile`, `EmailOutbox`, `EmailTemplate`, `ScheduledTask`, `JobHistory`) inherits soft-delete automatically. SP25 verifies all these queries still pass after the extension.
  - **`JobDispatcher` interface** (`templates/backend/jobs/JobDispatcher.java`) — SP24's `ExportJobService` consumes this; no forward reference.
  - **`PageRequestNormalizer`** — SP26's search controllers consume this for paged responses.
- **SP24 and SP26 run in parallel after SP25** (disjoint surfaces: integration/export-import vs charts/search; both consume but do NOT modify SP25's shared interfaces).
- **SP27 and SP28 run in parallel after SP24 ‖ SP26 complete** (i18n + feature flags do not depend on realtime/forms; realtime + forms do not depend on i18n).
- **SP29 lands last**: needs full post-SP28 tree to demo `/ax-verify policy-check`/`evidence-fetch`/`explain` subcommands.

This corrects the iter1 false-parallelism claim (Critic Blocker 2): SP25 is now the foundation, not a co-equal parallel SP. The dependency graph and ADR "Why chosen" text agree.

---

### §6.2 SP23 — Observability + Cache (F1 + F10)

- **Inputs:** `v1.0.0-catalog-complete` tag (commit `9212989`). Existing `templates/backend/error/ProblemDetailFactory.java` and `templates/backend/error/GlobalExceptionHandler.java` (SP13). Existing rule `practices/rules/traceid-in-error-response.md` (SP21).
- **Deliverables:**
  - **F1 templates (8 files)** under `templates/backend/observability/`:
    - `MdcCorrelationIdInterceptor.java` — `HandlerInterceptor` that pulls `X-Correlation-Id` from request OR generates `UUID.randomUUID()`, puts to MDC under key `traceId`, propagates to response header `X-Correlation-Id` AND `traceparent` (W3C Trace Context).
    - `OtelTracerConfig.java` — `@Configuration` with `OpenTelemetry` bean using OTLP exporter (env `OTEL_EXPORTER_OTLP_ENDPOINT`).
    - `MicrometerConfig.java` — `MeterRegistry` bean exposing `/actuator/prometheus`; tags include `application`, `environment`, `traceId`.
    - `StructuredLoggingConfig.java` + `LogbackJsonAppenderConfig.java` — Logback JSON pattern w/ `%X{traceId}`, `%X{userId}`, `%X{spanId}` from MDC.
    - `DatabaseHealthIndicator.java`, `RedisHealthIndicator.java`, `ExternalHttpHealthIndicator.java` — `HealthIndicator` implementations.
  - **F10 templates (4 files)** under `templates/backend/cache/`:
    - `CaffeineConfig.java` — `CaffeineCacheManager` bean with named caches per `CacheTtlPolicy`.
    - `RedisCacheConfig.java` — `RedisCacheManager` with TTL per cache name from `application.yml`.
    - `CacheKeyGenerator.java` — Custom `KeyGenerator` (param-deterministic).
    - `CacheTtlPolicy.java` — `@ConfigurationProperties("ax.cache")` with required `ttl` per cache name; throws if missing.
  - **Rules (2):** `practices/rules/mdc-traceid-required-on-controller.md` + failing fixture; `practices/rules/cacheable-requires-explicit-ttl.md` + failing fixture.
  - **Snapshots (3):** spring-boot-actuator (extend), opentelemetry-java (NEW), micrometer-prometheus (NEW).
  - **Self-reference closure:** `practices/rules/traceid-in-error-response.md` now actually protects `templates/backend/observability/MdcCorrelationIdInterceptor.java` + `templates/backend/error/ProblemDetailFactory.java`. Update rule's `protects_template_id` field.
- **Acceptance criteria (binary):**
  - `bash skills/ax-verify-java/scripts/run.sh templates/backend/observability/ templates/backend/cache/` → exit 0.
  - `practices/evals/fixtures/mdc_traceid_required/fail_no_interceptor/` → archunit exits 1 with `MDC_INTERCEPTOR_MISSING`.
  - `practices/evals/fixtures/cacheable_ttl/fail_no_ttl/` → archunit exits 1 with `CACHEABLE_NO_TTL`.
  - All 19 existing guards still GREEN.
  - `ax-guard-evidence` walks new paths.
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh && bash practices/evals/run-all-guards.sh --include-fixtures`.
- **TDD anchor:** `practices/evals/fixtures/mdc_traceid_required/fail_no_interceptor/AuthController.java` — a controller without the interceptor registered. Pre-SP23: rule file doesn't exist → fixture run reports `RULE_NOT_FOUND`. SP23 ships rule + interceptor + `pass/AuthControllerWithInterceptor.java`; pass exits 0, fail exits 1 with `MDC_INTERCEPTOR_MISSING`. First green command: `bash practices/evals/fixtures/mdc_traceid_required/_run.sh pass`.
- **Risks + executable mitigations (Critic Blocker 6 — owner + command + threshold + recovery for each):**
  - **R1:** OTLP exporter requires env var `OTEL_EXPORTER_OTLP_ENDPOINT`; CI may not have an OTLP collector.
    - **Owner:** SP23 lead.
    - **Command:** `bash practices/evals/fixtures/otlp_optional/_run.sh` (verifies `@ConditionalOnProperty("ax.observability.otlp.enabled")` is present in `OtelTracerConfig.java`; default off; no-op exporter in test profile).
    - **Threshold:** archunit fails if `OtelTracerConfig.java` lacks `@ConditionalOnProperty` annotation OR `application-test.yml` is missing `ax.observability.otlp.enabled: false`.
    - **Recovery:** Add `@ConditionalOnProperty` + no-op test exporter; re-run.
  - **R2:** Caffeine + Redis cache name collision — same cache name in both managers.
    - **Owner:** SP23 lead.
    - **Command:** `bash practices/evals/fixtures/cache_name_uniqueness/_run.sh` (archunit asserts no cache name appears in both `caffeine.cache-names` and `redis.cache-names` in `application.yml`).
    - **Threshold:** any duplicate cache name → archunit exit 1 with `CACHE_NAME_COLLISION`.
    - **Recovery:** Rename collision; `CacheTtlPolicy.java` asserts mutual-exclusion at startup as belt-and-suspenders.
- **Agent count:** 1 lead + 2 workers (parallel: observability worker, cache worker).
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp23-pre-start`. Atomic revert of `templates/backend/observability/**` + `templates/backend/cache/**` + 2 new rule files + 3 new snapshots; restore `traceid-in-error-response.md` `protects_template_id` to its SP21 value.

---

### §6.3 SP25 — Data Layer + Background Jobs (F3 + F4) — FOUNDATION SP

- **Inputs:** SP23 done. **Pre-flight disk check (iter3 narrow fix B3):** verify presence/absence of `templates/backend/BaseEntity.java`. iter3 has confirmed the file is ABSENT — SP25 OWNS the create-from-scratch responsibility (no in-place edit of a non-existent file). The 8 existing SP16/17/18/19 entity files conceptually extend "BaseEntity" but no real common superclass exists; SP25 closes that SP13 gap as a create-or-repair task.
- **Pre-flight Hibernate version check (iter4 narrow fix — Option B selected unconditionally for timestamp contract):**
  - **Command:** `cd backend && ./gradlew dependencies --configuration runtimeClasspath | grep hibernate-core`. Still runs at SP25 start to record the actual version in `templates/backend/data/README.md`.
  - **Decision (iter4):** **Option B (per-subclass `@SQLDelete` + `@Where(clause = "deleted_at IS NULL")` on `@MappedSuperclass`)** is the selected path for Hibernate ≥ 5.0+ as long as `deleted_at TIMESTAMP` is required (vs. boolean indicator). This matches the PRD's timestamp contract. The workspace ships Hibernate ORM 6.4.x, but the version branch from iter3 is collapsed: Option B applies regardless.
  - **Why Option A is rejected (Codex Critic iter3 finding):** Hibernate ORM 6.4 `@SoftDelete` is a **boolean indicator** mechanism only. Per `org.hibernate.annotations.SoftDeleteType` (https://docs.hibernate.org/orm/6.4/javadocs/org/hibernate/annotations/SoftDeleteType.html) the only enum values are `ACTIVE` and `DELETED`; per the Hibernate ORM 6.4 user guide (https://docs.hibernate.org/orm/6.4/userguide/html_single/) the soft-delete conversion domain type is boolean. The annotation cannot satisfy a `deleted_at TIMESTAMP NULL` column populated with the actual deletion moment — which is the contract this PRD ships (§6.3 Flyway migration + acceptance smoke that asserts `deleted_at` is a populated timestamp).
  - **Footnote:** If a future PRD switches the soft-delete contract to a boolean indicator (e.g. `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`), Option A (`@SoftDelete` on `@MappedSuperclass` in Hibernate 6.4+) becomes available and may be adopted by a future SP. Until that explicit switch, Option B is the unique correct path.
- **Deliverables:**
  - **F3 templates (7 NEW files under `templates/backend/data/` + 1 CREATE of `templates/backend/BaseEntity.java` + 1 NEW Flyway migration template):**
    - `FlywayConfig.java` — `@Configuration` + migration naming convention (`V<yyyyMMddHHmm>__<snake_case_description>.sql`).
    - `JpaAuditConfig.java` — `@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")`.
    - `SecurityAuditorAware.java` — pulls `@CreatedBy` / `@LastModifiedBy` from Spring Security context.
    - `SoftDeleteAspect.java` — AOP advice for soft-delete-aware repos (works alongside Hibernate `@SoftDelete` for cross-cutting concerns: audit emission, observability counter).
    - `JsonbConverter.java` — `@Converter` for PostgreSQL `jsonb` ↔ Java `Map<String,Object>`.
    - `OptimisticLockingPolicy.java` — `@Version` enforcement archunit rule helper.
    - `PageRequestNormalizer.java` — Wraps `Pageable` w/ max-limit cap (default 100, configurable via `ax.paging.max-limit`).
    - **`templates/backend/BaseEntity.java` (CREATE — fills SP13 gap; iter3 acceptance: if file absent at SP25 start, SP25 creates it):**
      - `@MappedSuperclass`
      - `@EntityListeners(AuditingEntityListener.class)` — wires Spring Data JPA auditing.
      - Audit fields: `@CreatedDate Instant createdAt`, `@LastModifiedDate Instant updatedAt`, `@CreatedBy String createdBy`, `@LastModifiedBy String updatedBy`.
      - `@Version Long version` (optimistic locking).
      - Soft-delete (iter4 — Option B is the selected path; **`@SoftDelete` is NOT used**, see pre-flight rationale above): `@Where(clause = "deleted_at IS NULL")` on the `@MappedSuperclass` covers the read filter for all subclasses via inheritance. **No `@SoftDelete` annotation on BaseEntity** (Hibernate 6.4 `@SoftDelete` is boolean-only and incompatible with this PRD's timestamp contract). Each concrete subclass MUST add its own `@SQLDelete(sql = "UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")` annotation — see §6.3 Deliverables "Per-subclass `@SQLDelete`" sub-block below for the 8-entity edit list.
      - `@Column(name = "deleted_at") private Instant deletedAt;` field with read-only getter; the write path is the per-subclass `@SQLDelete` SQL (which sets `deleted_at = CURRENT_TIMESTAMP`), preserving the timestamp contract.
      - Existing entity files (`id`, etc.) keep their `@Id` fields locally; BaseEntity provides only the shared audit/version/soft-delete-read-filter surface.
    - **`templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql` (NEW — Critic iter2 Defect 3):** template SQL for fork receivers. Adds `deleted_at TIMESTAMP NULL` column to all 8 existing tables and the supporting partial index:
      ```sql
      ALTER TABLE notifications              ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE notification_preferences   ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE audit_logs                 ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE stored_files               ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE email_outbox               ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE email_templates            ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE scheduled_tasks            ADD COLUMN deleted_at TIMESTAMP NULL;
      ALTER TABLE job_history                ADD COLUMN deleted_at TIMESTAMP NULL;
      CREATE INDEX idx_notifications_deleted_at            ON notifications              (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_notification_preferences_deleted_at ON notification_preferences   (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_audit_logs_deleted_at               ON audit_logs                 (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_stored_files_deleted_at             ON stored_files               (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_email_outbox_deleted_at             ON email_outbox               (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_email_templates_deleted_at          ON email_templates            (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_scheduled_tasks_deleted_at          ON scheduled_tasks            (deleted_at) WHERE deleted_at IS NULL;
      CREATE INDEX idx_job_history_deleted_at              ON job_history                (deleted_at) WHERE deleted_at IS NULL;
      ```
      Note: this is **TEMPLATE SQL for fork receivers** (no DB exists in this repo). Fork receivers paste this file under `backend/src/main/resources/db/migration/` with their own `V<timestamp>` prefix following the Flyway naming convention from `FlywayConfig.java`. Filename `V202605181200__add_soft_delete_columns.sql` is the canonical example in the template tree.
    - **Impact list (BaseEntity creation cascades to — Critic iter2 Defect 4: all 8 entities enumerated):** `templates/backend/notification/Notification.java`, `templates/backend/notification/NotificationPreferences.java` (SP16); `templates/backend/audit-log/AuditLog.java` (SP17); `templates/backend/file-storage/StoredFile.java` (SP18); `templates/backend/email-outbox/EmailOutbox.java`, `templates/backend/email-outbox/EmailTemplate.java` (SP19); `templates/backend/scheduled-task/ScheduledTask.java`, `templates/backend/scheduled-task/JobHistory.java` (SP19). Each subclass updated to `extends BaseEntity` (small edit per file, drops locally-duplicated audit fields). **Per iter4 Option B selection**, each subclass ALSO adds an explicit `@SQLDelete` annotation — see the per-subclass block below.
    - **Per-subclass `@SQLDelete` (iter4 — 8 entities, mandatory under Option B):** Each entity adds the annotation literally next to its existing `@Entity` + `@Table` (or implied snake_case table name). Example shape:
      ```java
      // Notification.java
      @Entity
      @Table(name = "notifications")
      @SQLDelete(sql = "UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
      public class Notification extends BaseEntity { ... }

      // NotificationPreferences.java
      @Entity
      @Table(name = "notification_preferences")
      @SQLDelete(sql = "UPDATE notification_preferences SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
      public class NotificationPreferences extends BaseEntity { ... }
      ```
      Full entity↔table mapping (verify by reading actual `@Table` annotation; if absent, fall back to snake_case from class name):

      | Entity | Table |
      |---|---|
      | `Notification` | `notifications` |
      | `NotificationPreferences` | `notification_preferences` |
      | `AuditLog` | `audit_logs` |
      | `StoredFile` | `stored_files` |
      | `EmailOutbox` | `email_outbox` |
      | `EmailTemplate` | `email_templates` |
      | `ScheduledTask` | `scheduled_tasks` |
      | `JobHistory` | `job_history` |
  - **F4 templates (7 files)** under `templates/backend/jobs/`:
    - `JobDispatcher.java` — interface w/ `SyncJob` + `AsyncJob` sub-interfaces. **Sole-writer SP for this interface; SP24's `ExportJobService` consumes it as a stable dependency.**
    - `JobQueue.java` — interface.
    - `RedisStreamsJobQueue.java` + `DbRowJobQueue.java` — two implementations (fork-receiver picks).
    - `JobWorker.java` — Consume loop + DLQ.
    - `JobHistoryProjection.java` — Read model for admin UI.
    - `JobDlqHandler.java` — Re-queue / kill / inspect.
  - **Rule (1):** `soft-delete-only-on-base-entity.md` + failing fixture. `protects_template_id: templates/backend/BaseEntity.java` (the newly-created file).
  - **Snapshots:** spring-flyway (NEW or extend), spring-data-jpa-auditing (NEW), **hibernate-orm-6.4-soft-delete (NEW — anchors evidence: both the Hibernate 6.4 `@SoftDelete` boolean-only mechanism that this PRD does NOT adopt AND the `@SQLDelete` + `@Where` per-subclass pattern that this PRD DOES adopt under Option B; quoted excerpts from `SoftDeleteType` javadoc + user-guide soft-delete section pin the boolean-only finding)**.
- **Acceptance criteria:**
  - `bash skills/ax-verify-java/scripts/run.sh templates/backend/data/ templates/backend/jobs/ templates/backend/BaseEntity.java templates/backend/data/migrations/` → exit 0. Includes Flyway migration syntax check (PostgreSQL dialect ALTER + partial INDEX syntax must parse via the `_check-anchors.sh` validator).
  - JPA audit RestAssured: insert via authenticated context → `created_by`, `created_at` populated.
  - **Soft-delete RestAssured smoke covers ALL 8 entities (Critic iter2 Defect 4 closed — `NotificationPreferences` no longer omitted):**
    1. `Notification` (`templates/backend/notification/Notification.java`)
    2. `NotificationPreferences` (`templates/backend/notification/NotificationPreferences.java`)
    3. `AuditLog` (`templates/backend/audit-log/AuditLog.java`)
    4. `StoredFile` (`templates/backend/file-storage/StoredFile.java`)
    5. `EmailOutbox` (`templates/backend/email-outbox/EmailOutbox.java`)
    6. `EmailTemplate` (`templates/backend/email-outbox/EmailTemplate.java`)
    7. `ScheduledTask` (`templates/backend/scheduled-task/ScheduledTask.java`)
    8. `JobHistory` (`templates/backend/scheduled-task/JobHistory.java`)
    For each: CREATE → SOFT-DELETE (`DELETE /api/v1/<resource>/{id}`) → verify `deleted_at` column populated in DB AND default `findAll()` / `GET /api/v1/<resource>/{id}` excludes the soft-deleted row (returns 404 for GET, omitted from list). Each domain's existing IT must still pass.
  - Job dispatch: enqueue 100 jobs → 100 history rows; 1 failing job → 1 DLQ row.
  - Paging: request `size=10000` → server caps at 100.
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh && bash backend/gradlew testAll` (the latter validates all existing domain ITs still pass after in-place BaseEntity edit).
- **TDD anchor:** `practices/evals/fixtures/soft_delete_base/fail_soft_delete_no_base/Item.java` declares a JPA entity using `@SQLDelete` directly (not via `BaseEntity` inheritance) → archunit fails with `SOFT_DELETE_NOT_VIA_BASE_ENTITY`. Pre-SP25: rule and base-entity extension absent. SP25 ships rule + in-place BaseEntity edit; `pass/ItemViaBaseEntity.java` extends BaseEntity and passes. First green: `bash practices/evals/fixtures/soft_delete_base/_run.sh pass`.
- **Risks + executable mitigations (Critic Blocker 6):**
  - **R1:** In-place BaseEntity edit breaks existing entity queries (the 8-entity impact list).
    - **Owner:** SP25 lead.
    - **Command:** `cd backend && ./gradlew testAll` (full backend integration test suite).
    - **Threshold:** any existing IT fails → SP25 halts. Specifically required green: `testAuth`, `testCrud`, `testPayment`, `testPractices`, plus the implicit notification/audit/file-storage/email-outbox/scheduled-task suites if present.
    - **Recovery:** If a domain IT regresses because it relied on hard-delete semantics, add a `@SQLDelete`-aware query helper to that domain's repo OR exempt the specific entity via `@SoftDeleteIgnore` (new annotation in `templates/backend/data/`). Document the exemption in the entity's `@ax-template-meta` block.
  - **R2:** Flyway migration naming collision when fork receivers add their own migrations.
    - **Owner:** SP25 lead.
    - **Command:** `bash practices/evals/fixtures/flyway_naming/_run.sh` (regex on `templates/backend/data/_migrations/*.sql` filenames; pattern `V[0-9]{12}__[a-z0-9_]+\.sql`).
    - **Threshold:** any filename violating the pattern → archunit exits 1 with `FLYWAY_NAMING_VIOLATION`.
    - **Recovery:** Rename file; README at `templates/backend/data/_migrations/README.md` documents fork-receiver convention (apply under `backend/src/main/resources/db/migration/`).
  - **R3:** `RedisStreamsJobQueue` requires Redis ≥ 5.0; fork receiver without Redis blocks.
    - **Owner:** SP25 lead.
    - **Command:** `bash practices/evals/fixtures/jobs_default_backend/_run.sh` (asserts `ax.jobs.queue.backend` defaults to `db-row` in `application.yml`).
    - **Threshold:** default `!= db-row` → archunit exits 1 with `JOBS_DEFAULT_NOT_DB_ROW`.
    - **Recovery:** Update `application.yml` default; Redis Streams opt-in only.
  - **R4 (iter4 — replaces iter3's Hibernate-version risk; closes Codex Critic iter3 blocking finding):** Per-subclass `@SQLDelete` repetition risks drift if a new entity is added without it (the annotation is local to each `@Entity` and there is no `@MappedSuperclass`-level enforcement).
    - **Owner:** SP25 lead.
    - **Command:** ArchUnit rule `BaseEntitySubclassMustCarrySqlDelete` that asserts every class extending `BaseEntity` carries an `@SQLDelete` annotation whose `sql` literal references the entity's `@Table(name = ...)` (or, fallback, the snake_case of the simple class name). Wired into `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteArchTest`.
    - **Threshold:** 0 violations. Any subclass of `BaseEntity` missing `@SQLDelete` → ArchUnit fails `BASE_ENTITY_SUBCLASS_MISSING_SQL_DELETE` with the offending class name listed.
    - **Recovery:** ArchUnit failure → CI gate refuses commit until the missing `@SQLDelete` is added on the offending entity per the §6.3 per-subclass block. Documentation: README at `templates/backend/data/README.md` records the convention and points at the ArchUnit fixture path.
- **Agent count:** 1 lead + 2 workers (data worker handles BaseEntity CREATE + 7 new files + Flyway migration template; jobs worker handles 7 jobs files).
- **Effort:** L (2 d — larger than iter1 estimate due to BaseEntity CREATE-from-absent + 8-entity smoke + Hibernate version pre-flight).
- **Rollback boundary:** `git tag sp25-pre-start`. Atomic revert of: `templates/backend/data/**` (including `migrations/V202605181200__add_soft_delete_columns.sql`) + `templates/backend/jobs/**` + 1 new rule file + 3 snapshots (spring-flyway, spring-data-jpa-auditing, hibernate-orm-6.4-soft-delete) + **DELETE the SP25-created `templates/backend/BaseEntity.java`** + the small `extends BaseEntity` edits in the 8 subclass files (no changes are needed if Option A; per-entity `@SQLDelete` revert if Option B). Since the 8 existing entities pre-iter3 carried their audit fields locally (no real common parent existed), reverting them is a clean undo of the small inheritance edit.

---

### §6.4 SP24 — External Integration + Export/Import (F2 + F12)

- **Inputs:** SP25 done (consumes `JobDispatcher` interface for `ExportJobService`; consumes `PageRequestNormalizer` for paged listing endpoints). Existing `templates/backend/email-outbox/` (SP19) for outbound-queue pattern reuse.
- **Deliverables:**
  - **F2 templates (6 files)** under `templates/backend/integration/`:
    - `WebClientConfig.java` — `WebClient.Builder` with Resilience4j `CircuitBreaker` + `Retry` + explicit `responseTimeout(Duration.ofSeconds(N))`.
    - `ExternalApiTemplate.java` — Typed wrapper: `<T> Mono<T> call(Class<T> respType, ...)` + fallback method.
    - `WebhookReceiver.java` — HMAC verification (`X-Signature` header, SHA-256 over body) + idempotency-key check.
    - `WebhookSender.java` — Outbound queue (reuses `EmailOutboxRelay.java` pattern from SP19).
    - `BulkheadConfig.java` — Thread pool isolation per upstream.
    - `CircuitBreakerConfig.java` — Named circuit breakers.
  - **F12 templates (5 files)** under `templates/backend/export-import/`:
    - `CsvImportService.java` — Chunked + transactional + error-report.
    - `ExcelImportService.java` — Apache POI streaming reader (`SXSSFWorkbook`).
    - `ExportJobService.java` — Long-running; consumes SP25's `JobDispatcher` (no forward reference, since SP25 lands first).
    - `ImportErrorReportDto.java` (record).
    - `ImportChunkProcessor.java` — Chunk-size config + per-row error capture.
  - **L2 (3 blocks):** `templates/L2/blocks/import-preview.tsx`, `mapping-editor.tsx`, `import-progress-bar.tsx`.
  - **Rules (2):** `webhook-hmac-required.md` + failing fixture; `chunked-import-required-when-rowcount-gt-1000.md` + failing fixture.
  - **Snapshots:** resilience4j (NEW).
  - **Self-reference closure:** `http-explicit-timeouts.md`, `http-restclient-over-resttemplate.md`, `http-shared-client-singleton.md` `protects_template_id` updated to `templates/backend/integration/WebClientConfig.java`.
- **Acceptance criteria:**
  - `bash skills/ax-verify-java/scripts/run.sh templates/backend/integration/ templates/backend/export-import/` → exit 0.
  - `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/` → exit 0 (axe + Playwright on new blocks).
  - Webhook RestAssured test: POST with invalid HMAC → 401 ProblemDetail; valid HMAC → 200.
  - CSV import: 10k rows in single TX rejected at archunit; chunked-50 accepted.
  - `ExportJobService` integration test: enqueue a 1000-row export → SP25 `JobDispatcher` accepts; job history row created.
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh && bash skills/ax-verify-L2/scripts/run.sh`.
- **TDD anchor:** `practices/evals/fixtures/webhook_hmac/fail_no_signature_check/PaymentWebhookController.java` — controller mapping `/webhooks/payment` without `@RequestHeader("X-Signature")` validation. Pre-SP24: rule file absent. SP24 ships rule + `pass/PaymentWebhookControllerWithHmac.java`. First green: `bash practices/evals/fixtures/webhook_hmac/_run.sh pass`.
- **Risks + executable mitigations (Critic Blocker 6):**
  - **R1:** Resilience4j version drift (Spring Boot 3.x compat).
    - **Owner:** SP24 lead.
    - **Command:** `bash practices/evals/fixtures/resilience4j_pin/_run.sh` — verifies `blueprints/pinned-versions.yaml` contains `io.github.resilience4j: 2.2.0` (or current Spring Boot 3.3-compatible range `[2.1.0, 3.0.0)`).
    - **Threshold:** missing pin OR version outside `[2.1.0, 3.0.0)` → archunit exits 1 with `RESILIENCE4J_PIN_MISSING`. Compatibility threshold: `cd backend && ./gradlew test --tests "*Resilience4j*"` must pass.
    - **Recovery:** Update `blueprints/pinned-versions.yaml`; re-run gradle test set.
  - **R2:** Apache POI memory usage on Excel imports >100MB.
    - **Owner:** SP24 lead.
    - **Command:** `bash practices/evals/fixtures/poi_streaming/_run.sh` — archunit rule asserts no `XSSFWorkbook` constructor call in any `templates/backend/export-import/*Import*Service.java` AST.
    - **Threshold:** any `XSSFWorkbook` import or construction → archunit exits 1 with `POI_NON_STREAMING_FORBIDDEN`. Memory test (run when assertion passes): `cd backend && ./gradlew test --tests "*ExcelImportServiceMemoryIT"` heap-cap 256MB on a 100MB fixture file must complete.
    - **Recovery:** Replace `XSSFWorkbook` with `SXSSFWorkbook`; re-run.
- **Agent count:** 1 lead + 3 workers (integration, export-import, L2).
- **Effort:** L (2–3 d).
- **Rollback boundary:** `git tag sp24-pre-start`. Atomic revert of `templates/backend/integration/**` + `templates/backend/export-import/**` + 3 new L2 blocks + 2 new rule files + 1 snapshot; restore self-references on http-* rules.

---

### §6.5 SP26 — Charts L2 + Search Atomic Domain (F5 + F6)

- **Inputs:** SP25 done (consumes `PageRequestNormalizer` for paged search responses). L1 `combobox` (SP14) exists for typeahead.
- **Deliverables:**
  - **F5 charts (6 L2 blocks):** `templates/L2/blocks/time-series-chart.tsx`, `bar-chart.tsx`, `pie-chart.tsx`, `kpi-card.tsx`, `sparkline.tsx`, `heatmap.tsx`. recharts wrapper + composition.
  - **F6 search (atomic — Spec Trio + backend + L2 + L4 in ONE SP):**
    - **Spec Trio:** `specs/search-l0.yaml` + `specs/search-frontend-l0.yaml` + `contracts/search-openapi.yaml` + `contracts/search-ui.yaml` + `blueprints/search-manifest.yaml` + `blueprints/search-ui-manifest.yaml`.
    - **Backend (7 files):** `templates/backend/search/` — see §5.1. `SearchController` consumes SP25 `PageRequestNormalizer` for `/api/v1/search?page=N&size=M`.
    - **L2 (4 blocks):** `search-palette-extended.tsx`, `typeahead-search.tsx`, `result-highlighter.tsx`, `recent-searches.tsx`.
    - **L3 page:** `templates/L3/pages/search-results-page/{page.tsx, error.tsx, loading.tsx, README.md}`.
    - **L4 domain:** `templates/L4/search/app/(search)/page.tsx` + `results/page.tsx` + middleware + next.config.
    - **Allowlist:** add `search: full_trio` to `practices/evals/trio_integrity_allowlist.yaml` (race-safe append protocol — see §7.2).
  - **Snapshots (3):** recharts (NEW), postgres-fts (NEW), meilisearch (NEW).
  - **Hangul IME extension:** typeahead-search.tsx ships with the SP21 `combobox-respects-hangul-ime-composition` IME simulator wired in; rule's `protects_template_id` extends to include `templates/L2/blocks/typeahead-search.tsx`.
- **Acceptance criteria:**
  - `bash skills/ax-guard-trio-integrity/scripts/run.sh` → PASS (search full_trio).
  - `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/` → PASS (charts + search blocks).
  - `bash skills/ax-verify-L4/scripts/run.sh templates/L4/search/` → PASS.
  - `bash skills/ax-verify-domain/scripts/run.sh search` → PASS (registry entry exists; end-to-end test: `POST /api/v1/search` with Korean query "강남 결제" → results render in palette).
  - axe-core on `templates/L4/search/` → 0 violations.
  - Playwright IME story on `typeahead-search.tsx` → no text corruption.
- **Verify command:** `bash skills/ax-verify-domain/scripts/run.sh search`.
- **TDD anchor:** `practices/evals/fixtures/trio_integrity/fail_search_missing_frontend_spec/` — only `specs/search-l0.yaml` present. Pre-SP26: `trio_integrity_guard.sh` exits 1 with `MISSING_FRONTEND_SPEC: search`. SP26 ships full trio; pass fixture exits 0. First green: `bash practices/evals/trio_integrity_guard.sh --domain search`.
- **Risks + executable mitigations (Critic Blocker 6):**
  - **R1:** Meilisearch is heavy (binary distro, RAM). Fork receivers without Korean enterprise infra may be blocked.
    - **Owner:** SP26 lead.
    - **Command:** `bash practices/evals/fixtures/search_default_backend/_run.sh` — asserts `blueprints/search-manifest.yaml` declares `default_backend: postgres-fts` AND `ax.search.backend` is unset in `application.yml` (so Postgres-FTS adapter is selected on first run).
    - **Threshold:** default backend != `postgres-fts` → archunit exits 1 with `SEARCH_DEFAULT_NOT_POSTGRES_FTS`.
    - **Recovery:** Update manifest; Meilisearch opt-in only via `ax.search.backend=meilisearch`.
  - **R2:** PostgreSQL FTS Korean tokenization requires `mecab-ko` or `kkma` — fork-receiver heavy lift; without it, Korean recall ~70%.
    - **Owner:** SP26 lead.
    - **Command:** `bash skills/ax-verify-domain/scripts/run.sh search --include-tokenization-smoke` (runs `practices/evals/fixtures/search_korean_tokenization/` — 10-row Korean corpus + expected results).
    - **Threshold:** Korean tokenization recall < 70% on PostgresFtsAdapter default → install.sh emits WARNING block with link to `ADR TD-2026-05-18-026` and recommended `ax.search.tokenizer: mecab-ko`. The first-run install hook in `/ax-fork-receiver` displays this as an interactive prompt: `[Y/n] Use built-in PostgreSQL 'simple' tokenizer (default, ~70% Korean recall) or configure mecab-ko (opt-in, requires extra package)?`. Default `Y` → proceed; `n` → install.sh prints mecab-ko install steps and exits.
    - **Recovery:** If recall < 70% AND user declined opt-in, log structured warning to `templates/L4/search/README.md`-pointed runbook; SP26 acceptance still passes because the warning + opt-in path is the documented contract.
  - **R3:** Korean IME in `typeahead-search.tsx` interacts with debounce — text corruption on `compositionstart`.
    - **Owner:** SP26 lead.
    - **Command:** `npx playwright test templates/L2/_fixtures/typeahead-search-ime.spec.ts`.
    - **Threshold:** IME corruption count > 0 → SP26 halts.
    - **Recovery:** Reuse SP21 IME simulator fixture (`templates/L2/_fixtures/_helpers/ime-simulator.ts`); ensure `compositionstart` / `compositionend` events suppress debounce window.
- **Agent count:** 1 lead + 3 workers (charts worker, search backend worker, search frontend worker).
- **Effort:** L (2 d).
- **Rollback boundary:** `git tag sp26-pre-start`. **Atomic revert** (search domain is atomic per Critic mandate): ALL search domain deliverables (Spec Trio + backend + L2 + L3 + L4 + allowlist entry) + chart blocks + 3 snapshots.

---

### §6.6 SP27 — Realtime/SSE (opt-in) + Form Orchestration (F7 + F11)

- **Inputs:** SP25 done (jobs DLQ feeds event-bus; data layer outbox table reused). SP24 done (no functional dependency; sequencing).
- **Deliverables:**
  - **F7 templates (5 files)** under `templates/backend/realtime/` — **all transports are opt-in only**:
    - `SseEmitterConfig.java` — `SseEmitter` with timeout + heartbeat. Opt-in via blueprint.
    - `RealtimeEventBus.java` — DB outbox → SSE bridge.
    - `SseSubscription.java` — per-user subscription.
    - `WebSocketConfig.java` — STOMP fallback (for environments where SSE blocked).
    - `RealtimeOutboxRelay.java` — relays from outbox table to SSE / WebSocket / no-op (polling default).
  - **F7 blueprint (NEW — architect polish):** `blueprints/realtime-policy-manifest.yaml`:
    ```yaml
    realtime:
      default_transport: polling   # L4 notification/audit/payment default — TanStack Query polling
      opt_in_transports:
        - name: sse
          enabled_via: ax.realtime.sse.enabled=true
          serverless_safe: false   # see ADR TD-2026-05-18-027 — Vercel/Lambda timeout warning
        - name: websocket
          enabled_via: ax.realtime.websocket.enabled=true
          serverless_safe: false
      polling_interval_ms: 5000    # default fork-receiver behavior
    ```
  - **F7 L2 (3 blocks):** `live-presence.tsx`, `optimistic-update.tsx`, `event-stream.tsx`. Each reads `realtime-policy-manifest.yaml` at import time; if `default_transport: polling` and no opt-in, falls back to TanStack Query polling primitive from SP16.
  - **F11 L2 (7 blocks):** form orchestration `-extended` cluster.
  - **Snapshots:** spring-mvc-sse (NEW), react-hook-form (NEW or extend).
- **Acceptance criteria:**
  - `bash skills/ax-verify-java/scripts/run.sh templates/backend/realtime/` → PASS.
  - `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/` → PASS (realtime + forms blocks).
  - **`blueprints/realtime-policy-manifest.yaml` exists and `default_transport: polling`** — verified by `bash practices/evals/fixtures/realtime_default_polling/_run.sh`.
  - **L4 default behavior test:** `templates/L4/notification/app/(notification)/page.tsx` rendered with default config → uses TanStack Query polling (NOT SseEmitter), confirmed by Playwright fixture asserting no `EventSource` constructor call.
  - SSE end-to-end RestAssured (opt-in path): set `ax.realtime.sse.enabled=true` + subscribe to `/api/v1/events?topic=notification` → receive 3 mock events within 5s.
  - Form `dirty-guard` Playwright: navigate-away with unsaved changes → confirm dialog appears.
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh && bash skills/ax-verify-L2/scripts/run.sh && bash practices/evals/fixtures/realtime_default_polling/_run.sh`.
- **TDD anchor:** Two anchors (one per cluster):
  - **Realtime:** `templates/L2/_fixtures/realtime-default-polling.spec.ts` — Playwright fixture loads `templates/L4/notification/` with default config; asserts no `new EventSource(...)` instantiation; asserts TanStack Query interval fetch fires every 5s. Pre-SP27: `realtime-policy-manifest.yaml` ENOENT; fixture fails with `MANIFEST_NOT_FOUND`. First green: `npx playwright test templates/L2/_fixtures/realtime-default-polling.spec.ts`.
  - **Forms:** `templates/L2/_fixtures/dirty-guard.spec.ts` — Playwright fixture navigating away triggers `beforeunload`. Pre-SP27: `dirty-guard.tsx` ENOENT. First green: `npx playwright test templates/L2/_fixtures/dirty-guard.spec.ts`.
- **Risks + executable mitigations (Critic Blocker 6 + architect polish):**
  - **R1:** SSE in serverless (Vercel) — long-lived connection limits.
    - **Owner:** SP27 lead.
    - **Command:** `bash skills/ax-verify-L4/scripts/run.sh templates/L4/notification/` — checks that L4 default transport is polling, AND README has `## Serverless Deployment` section if any opt-in to SSE/WebSocket is present.
    - **Threshold:** L4 imports `templates/backend/realtime/SseEmitterConfig` AND uses it as default (not opt-in via manifest) → exit 1 with `REALTIME_DEFAULT_NOT_POLLING`. Or: opt-in SSE without serverless warning README → exit 1 with `SERVERLESS_WARNING_MISSING`.
    - **Recovery (Critic + architect polish):** **Default is already polling.** SSE/WebSocket is opt-in via `blueprints/realtime-policy-manifest.yaml`. ADR TD-2026-05-18-027 documents the trade. If user opts into SSE on Vercel, README warns to switch to WebSocket or polling fallback. This is the "safer default" mitigation Critic Blocker 6 demands.
  - **R2:** Form `-extended` naming creates duplication with SP15 shells.
    - **Owner:** SP27 lead.
    - **Command:** `bash practices/evals/fixtures/form_extended_supersede/_run.sh` — asserts SP15 shells (`field-array.tsx`, `form-section.tsx`, `conditional-field.tsx`, `form-error-summary.tsx`) carry `@deprecated use *-extended` JSDoc and a re-export `export { default } from './field-array-extended'` so old import paths keep working.
    - **Threshold:** shell missing `@deprecated` JSDoc OR shell does NOT re-export from `-extended` → archunit exits 1 with `SHELL_SUPERSEDE_INCOMPLETE`.
    - **Recovery:** Add `@deprecated` annotation + re-export; ADR TD-2026-05-18-027 documents the supersede contract.
- **Agent count:** 1 lead + 2 workers (realtime, forms).
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp27-pre-start`. Per-cluster revert allowed (realtime independent of forms). If realtime reverts, remove `blueprints/realtime-policy-manifest.yaml` and restore L4 README polling-only language.

---

### §6.7 SP28 — i18n + Feature Flags (F8 + F9)

- **Inputs:** SP25 done. SP27 done (form-error-summary-extended used by feature-flag admin form).
- **Deliverables:**
  - **F8 L1 (3):** `locale-switcher.tsx`, `currency-formatter.tsx`, `relative-time.tsx`.
  - **F8 L2 (2):** `locale-provider.tsx` (next-intl wrapper), `translation-boundary.tsx`.
  - **F8 blueprint:** `blueprints/i18n-policy-manifest.yaml` — declares ko-KR + en-US minimum, KRW format rules (`₩` symbol position, no decimals), fallback chain (ko-KR → en-US → key), pseudo-locale for testing.
  - **F8 rule (scope-restricted per Critic Blocker 4, Option β):** `no-hardcoded-user-facing-string-in-l4.md` with frontmatter:
    ```yaml
    applies_to:
      include:
        - "templates/L4/**/*.tsx"
      exclude_paths_created_before: 2026-05-18
      exclude_existing_domains:
        - templates/L4/auth/
        - templates/L4/crud/
        - templates/L4/payment/
        - templates/L4/practices/
        - templates/L4/notification/
        - templates/L4/audit-log/
        - templates/L4/file-storage/
    ```
    Failing fixture: `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/page.tsx` under a path that simulates a "new SP28+ domain". Existing L4 directories are explicitly excluded by the rule loader.
  - **F9 templates (6 files)** under `templates/backend/feature-flags/`.
  - **F9 Spec Trio (full_trio):** `specs/feature-flags-l0.yaml` + `specs/feature-flags-frontend-l0.yaml` + `contracts/feature-flags-openapi.yaml` + `contracts/feature-flags-ui.yaml` + `blueprints/feature-flags-manifest.yaml` + `blueprints/feature-flags-ui-manifest.yaml`.
  - **F9 L1 (1):** `feature-gate.tsx`.
  - **F9 L2 (1):** `feature-flag-toggle.tsx` (admin UI).
  - **F9 L4 (5 files):** `templates/L4/feature-flags/app/(admin)/feature-flags/page.tsx` + `[name]/page.tsx` + middleware + next.config + README.
  - **F9 rule:** `prefer-feature-gate-over-env-check.md` + failing fixture.
  - **Allowlist:** add `feature-flags: full_trio` (race-safe append — see §7.2).
  - **Snapshots:** next-intl (NEW).
- **Acceptance criteria (scope-corrected per Critic Blocker 4 Option β):**
  - `bash skills/ax-verify-L1/scripts/run.sh && bash skills/ax-verify-L2/scripts/run.sh` → PASS.
  - `bash skills/ax-guard-trio-integrity/scripts/run.sh` → PASS (feature-flags full_trio).
  - `bash skills/ax-verify-domain/scripts/run.sh feature-flags` → PASS.
  - **i18n end-to-end against SP28-created paths only:** Playwright fixture under `practices-react/evals/fixtures/no_hardcoded_i18n/pass_translation_boundary/page.tsx` uses `LocaleProvider` + `t()` → switching ko-KR ↔ en-US updates rendered strings; KRW format verified (`₩` prefix, no decimals).
  - **Hardcoded-string probe:** runs against `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/page.tsx` (a SP28-scoped fixture) — exits 1 with `HARDCODED_USER_FACING_STRING`. Runs against `pass_translation_boundary/page.tsx` — exits 0. **Does NOT run against existing `templates/L4/{auth,crud,payment,practices,notification,audit-log,file-storage}/`** — rule's `applies_to` exclusion list ensures this.
  - **Existing L4 i18n migration is explicitly OUT OF SCOPE for SP28** (deferred to a future P1 SP; documented in §12). Acceptance does NOT require existing L4 strings to be migrated.
  - Feature-flag end-to-end: admin creates flag → L4 page reads via `<FeatureGate>` → toggle propagates within cache TTL.
- **Verify command:** `bash skills/ax-verify-domain/scripts/run.sh feature-flags && bash skills/ax-verify-L2/scripts/run.sh && bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh both`.
- **TDD anchor:** `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/page.tsx` — `<button>결제하기</button>` literal (not `t('payment.submit')`). Path is under `practices-react/evals/fixtures/` (SP28-scoped, not existing L4). Pre-SP28: rule absent. SP28 ships rule + LocaleProvider; pass fixture uses `t()`. First green: `bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh pass`.
- **Risks + executable mitigations (Critic Blocker 6):**
  - **R1:** Scope ambiguity — rule could accidentally fire against existing L4 paths if `applies_to` filter is buggy.
    - **Owner:** SP28 lead.
    - **Command:** `bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh existing-l4-must-skip` — runs the rule against `templates/L4/auth/`, `templates/L4/payment/`, etc., expects exit 0 (rule must skip).
    - **Threshold:** rule fires on any existing L4 path → archunit exits 1 with `I18N_RULE_FIRED_ON_EXISTING_L4`.
    - **Recovery:** Fix `applies_to` exclusion list in rule frontmatter; rule loader (`practices/rules/_loader.sh`) must honor `exclude_existing_domains`.
  - **R2:** KRW format ambiguity (₩ before number, no decimals vs JPY-like).
    - **Owner:** SP28 lead.
    - **Command:** `npx playwright test templates/L1/_stories/currency-formatter.spec.ts`.
    - **Threshold:** KRW 1234 → not exactly `₩1,234` → SP28 halts.
    - **Recovery:** `formatKrw()` helper in `currency-formatter.tsx` overrides `Intl.NumberFormat` output; ISO 4217 evidence anchor cites `iso-4217.snapshot.md`.
  - **R3:** `feature-flags` allowlist append race with SP26 (`search`).
    - **Owner:** SP28 lead.
    - **Command:** Before commit, run `git fetch origin && git rebase origin/<base>`; then `yq -i 'sort_keys(.)' practices/evals/trio_integrity_allowlist.yaml && git diff --exit-code` to verify deterministic ordering.
    - **Threshold:** non-empty `git diff` after `yq sort` → conflicting parallel edit; rebase failed.
    - **Recovery:** Per Catalog Extension §6.2 — sorted-key insertion via `yq`; manual conflict resolution if simultaneous append.
- **Agent count:** 1 lead + 2 workers (i18n, feature flags).
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp28-pre-start`. **Atomic revert per cluster.** If i18n cluster reverts: remove `templates/L1/components/{locale-switcher,currency-formatter,relative-time}.tsx` + L2 i18n blocks + `blueprints/i18n-policy-manifest.yaml` + `no-hardcoded-user-facing-string-in-l4.md` rule. If feature-flags cluster reverts: **atomic revert of the full feature-flags trio/backend/frontend cluster** (Critic soft suggestion 4): Spec Trio + `templates/backend/feature-flags/**` + `templates/L1/components/feature-gate.tsx` + `templates/L2/blocks/feature-flag-toggle.tsx` + `templates/L4/feature-flags/**` + allowlist entry + `prefer-feature-gate-over-env-check.md` rule.

---

### §6.8 SP29 — `/ax-verify` Subcommand Extension (F13 + F14 + F15)

- **Inputs:** SP23–SP28 done (full post-SP28 catalog). Existing Tier-1 skill `skills/ax-verify/` (SP3 + SP4b).
- **Deliverables (all subcommands of existing Tier-1 `/ax-verify`; ZERO new Tier-1):**
  - **`/ax-verify policy-check` subcommand** under `skills/ax-verify/`:
    - `scripts/policy-check.sh` — args: `<file-path> <intent-text>`. Output: JSON `{applicable_rules: [...], protects_template_id_matches: [...], required_evidence_locations: [...], verdict: "STOP" | "PROCEED" | "WARN", citations: [...]}`.
    - `scripts/_rule-loader.sh` — loads `practices/rules/*.md` + `practices-react/rules/*.md` + builds rule index by `applies_to`. **Shared with `explain` subcommand.**
    - Dispatcher update: `skills/ax-verify/scripts/run.sh` accepts `policy-check` as first positional arg and delegates to `policy-check.sh`. Backward compat: existing `bash skills/ax-verify/scripts/run.sh <path>` (no subcommand) keeps the post-mutation verify behavior unchanged.
  - **`/ax-verify evidence-fetch` subcommand** under `skills/ax-verify/`:
    - `scripts/evidence-fetch.sh` — args: `[<snapshot-id> | --all]`. Output: WebFetch → diff vs current snapshot → refresh recommendation.
    - `scripts/_compare.sh` — WebFetch + `time_decay_guard.sh` integration.
  - **`/ax-verify explain` subcommand** under `skills/ax-verify/`:
    - `scripts/explain.sh` — args: `[<rule-id> | --violation-msg "<msg>"]`. Output: explanation with `protects_template_id` + evidence quote + at least 1 violating example + at least 1 compliant example + related rules.
  - **Updates to existing `skills/ax-verify/SKILL.md` (edit-in-place):** add `usage:` block documenting the 3 subcommands. Frontmatter `name: ax-verify` unchanged.
  - **Single self-test runner:** `skills/ax-verify/scripts/_subcommand-self-test.sh` — runs 3 cases, one per subcommand. Replaces the 3 separate self-test scripts of iter1.
  - **No `/ax-fork-receiver` bundle change** — `/ax-verify` is already bundled; subcommand additions ship inside its existing tarball entry.
- **Acceptance criteria (Critic Blocker 1 — binary path is subcommand self-tests):**
  - `bash skills/ax-verify/scripts/_subcommand-self-test.sh` → exit 0 (runs 3 cases).
  - **Policy-check case:** `bash skills/ax-verify/scripts/run.sh policy-check templates/L4/payment/app/\(payment\)/page.tsx "import from L4/audit-log"` → outputs `STOP` + cites `practices-react/rules/no-cross-l4-domain-imports.md`.
  - **Evidence-fetch case:** `bash skills/ax-verify/scripts/run.sh evidence-fetch --all` → outputs per-snapshot age + recommendation (no actual WebFetch in self-test; mocked).
  - **Explain case:** `bash skills/ax-verify/scripts/run.sh explain --rule-id no-server-component-state-leakage-to-client` → outputs explanation including `protects_template_id` + evidence quote + ≥1 violating example + ≥1 compliant example.
  - **False-positive eval gate (policy-check):** `bash skills/ax-verify/scripts/_eval/policy-check-fp.sh` runs the 50-fixture eval set → FP rate < 5%.
  - Tarball cold-install (existing `/ax-fork-receiver`): `/ax-verify` present in extracted tree (already was); `bash skills/ax-verify/scripts/run.sh policy-check --help` runs successfully (new subcommand wired correctly).
- **Verify command:** `bash skills/ax-verify/scripts/_subcommand-self-test.sh && bash skills/ax-verify/scripts/_eval/policy-check-fp.sh`.
- **TDD anchor:** `skills/ax-verify/_tests/policy-check-cold.spec.sh` — fixture-based: writes a controlled file path, intent string, expects deterministic verdict. Pre-SP29: `policy-check.sh` ENOENT → spec fails with `SUBCOMMAND_NOT_FOUND`. First green: `bash skills/ax-verify/scripts/_subcommand-self-test.sh`.
- **Risks + executable mitigations (Critic Blocker 6):**
  - **R1:** `/ax-verify policy-check` produces false positives → AI agents start ignoring it (signal-degradation cascade).
    - **Owner:** SP29 lead.
    - **Command:** `bash skills/ax-verify/scripts/_eval/policy-check-fp.sh` — runs 50-fixture eval set (25 expected STOP, 15 expected PROCEED, 10 expected WARN) and computes confusion matrix.
    - **Threshold:** FP rate (FP / (FP + TN)) ≥ 5% → SP29 halts; rule-loader tuning required (e.g., add `intent_requires_keyword` field per rule).
    - **Recovery:** SP29 lead inspects FP fixtures; tunes rule index logic; re-runs. If FP rate cannot be brought below 5% within 3 iterations, fallback: ship `policy-check` as `WARN`-only (no `STOP`) — subcommand is purely advisory. ADR amendment documents.
  - **R2:** `/ax-verify evidence-fetch` WebFetch dependency may hang on slow upstream.
    - **Owner:** SP29 lead.
    - **Command:** `timeout 30 bash skills/ax-verify/scripts/run.sh evidence-fetch --snapshot opentelemetry-java-2026-05`.
    - **Threshold:** any WebFetch call > 30s wall clock → subcommand exits 1 with `EVIDENCE_FETCH_TIMEOUT`; `--offline` mode reads cached snapshot diff only.
    - **Recovery:** Add `--offline` flag; document in `skills/ax-verify/SKILL.md`.
  - **R3:** Dispatcher backward-compat break for existing `/ax-verify` callers.
    - **Owner:** SP29 lead.
    - **Command:** `bash skills/ax-verify/scripts/_legacy-call-compat.sh` — runs `bash skills/ax-verify/scripts/run.sh templates/backend/auth/` (no subcommand, legacy form) and asserts exit code matches pre-SP29 baseline.
    - **Threshold:** any legacy invocation pattern returns non-zero where the baseline returned 0 → SP29 halts with `LEGACY_VERIFY_BROKEN`.
    - **Recovery:** Dispatcher must detect: if first arg starts with `templates/` OR matches a directory path glob → treat as legacy post-mutation verify; if first arg matches `policy-check|evidence-fetch|explain` → subcommand dispatch.
- **Agent count:** 1 lead + 3 workers (one per subcommand).
- **Effort:** M (1 d).
- **Rollback boundary:** `git tag sp29-pre-start`. Per-subcommand revert allowed; full revert restores `skills/ax-verify/scripts/run.sh` to its pre-SP29 dispatcher form (no subcommand routing).

---

### §6.9 Verification Matrix (single authoritative table — iter2 corrections)

| SP | verify_skill | script_path | test_file | assertion | expected_RED_reason | first_green_command | observability_signal |
|---|---|---|---|---|---|---|---|
| SP23 | `/ax-verify-java` (corrected from iter1 typo) | `bash skills/ax-verify-java/scripts/run.sh templates/backend/observability/ templates/backend/cache/` + `bash practices/evals/run-all-guards.sh --include-fixtures` | `practices/evals/fixtures/mdc_traceid_required/fail_no_interceptor/AuthController.java` + `practices/evals/fixtures/cacheable_ttl/fail_no_ttl/Service.java` | Observability + cache templates anchored; 2 new rule fixtures fail with named errors; 19 existing guards still GREEN; self-reference for `traceid-in-error-response` closed | Pre-SP23: `templates/backend/observability/MdcCorrelationIdInterceptor.java` ENOENT; archunit fails `MDC_INTERCEPTOR_MISSING` | `bash practices/evals/fixtures/mdc_traceid_required/_run.sh pass && bash skills/ax-verify-java/scripts/run.sh templates/backend/observability/` | `template.evidence.coverage_ratio` (== 1.0); `archunit.violations` (== 0); `traceid.rule.protects_count` (> 0); emission: `MeterRegistry` bean from `MicrometerConfig.java` exposes `/actuator/prometheus` |
| SP25 | `/ax-verify-java` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/data/ templates/backend/jobs/ templates/backend/BaseEntity.java templates/backend/data/migrations/` + Flyway migration syntax check (`bash templates/backend/_check-anchors.sh templates/backend/data/migrations/`) + `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteIT` + `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteArchTest` + `cd backend && ./gradlew testAll` | `backend/src/test/java/com/ax/template/baseentity/BaseEntitySoftDeleteIT.java` (RestAssured 8-entity smoke per Critic iter2 Defect 4) + `backend/src/test/java/com/ax/template/baseentity/BaseEntitySoftDeleteArchTest.java` (iter4 ArchUnit per-subclass `@SQLDelete` coverage rule per R4) + `practices/evals/fixtures/soft_delete_base/fail_soft_delete_no_base/Item.java` + RestAssured `JobDispatchIT.java` + `PageRequestCapIT.java` | All 8 entities (Notification, NotificationPreferences, AuditLog, StoredFile, EmailOutbox, EmailTemplate, ScheduledTask, JobHistory): DELETE sets `deleted_at` AND default `findAll`/`GET` excludes the soft-deleted row; Flyway migration template is valid PostgreSQL SQL; **every `BaseEntity` subclass carries `@SQLDelete` per iter4 R4 ArchUnit rule**; JPA auditing populates fields; jobs DLQ on failure; paging cap enforced | Pre-SP25: `templates/backend/BaseEntity.java` does NOT exist; soft-delete annotations not applied; archunit fails `BASE_ENTITY_FILE_MISSING` followed by `BASE_ENTITY_NOT_SOFT_DELETE`; **iter4 additional RED reason: `Per-subclass @SQLDelete annotation missing on any entity` (`BASE_ENTITY_SUBCLASS_MISSING_SQL_DELETE` from `BaseEntitySoftDeleteArchTest`)** | `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteIT` (first green requires BaseEntity created + migration template valid + per-subclass `@SQLDelete` on all 8 entities + 8-entity smoke pass) | `jpa.audit.fields.populated_ratio` (== 1.0); **`baseentity.soft_delete.applied_count` (entity instances soft-deleted per minute, emitted by `SoftDeleteAspect.java` as Micrometer counter `baseentity.soft_delete.applied_count_total{entity=<simpleClassName>}`)**; `jobs.dlq.row_count` correlates with failures; `paging.max_limit_violations` (== 0); emission: `JobDlqHandler.java` increments Micrometer counter `jobs.dlq.row_count_total{queue=<name>}` |
| SP24 | `/ax-verify-java` + `/ax-verify-L2` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/integration/ templates/backend/export-import/` + `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/import-preview.tsx ...` | `practices/evals/fixtures/webhook_hmac/fail_no_signature_check/PaymentWebhookController.java` + `practices/evals/fixtures/chunked_import/fail_single_tx_10k/Service.java` + RestAssured `ExportJobServiceIT.java` (asserts consumption of SP25 JobDispatcher) | Webhook HMAC enforced; chunked-import enforced; circuit-breaker + retry templates anchor existing self-references; ExportJobService consumes SP25 JobDispatcher | Pre-SP24: `templates/backend/integration/WebhookReceiver.java` ENOENT; archunit fails `WEBHOOK_RECEIVER_NOT_FOUND` | `bash practices/evals/fixtures/webhook_hmac/_run.sh pass` | `webhook.hmac.violations` (== 0); `circuit_breaker.open_count` (gauge exposed via Resilience4j Micrometer integration); `import.chunked.rowcount_p99` < cfg.cap (emission: `ImportChunkProcessor.java` → `import.chunk.processed_rows`) |
| SP26 | `/ax-verify-domain search` | `bash skills/ax-verify-domain/scripts/run.sh search` + `bash skills/ax-guard-trio-integrity/scripts/run.sh` | `practices/evals/fixtures/trio_integrity/fail_search_missing_frontend_spec/` + RestAssured `SearchFlowIT.java` + Playwright `typeahead-search-ime.spec.ts` | Atomic Spec Trio shipped; FTS adapter returns results; Korean IME no text corruption; chart blocks render; SearchController consumes SP25 PageRequestNormalizer | Pre-SP26: `specs/search-l0.yaml` ENOENT; trio_integrity_guard exits 1 with `MISSING_BACKEND_SPEC: search` | `bash skills/ax-verify-domain/scripts/run.sh search` | `search.query.latency_p99_ms` (< 200 on PostgresFtsAdapter); `ime.composition.corruption_count` (== 0); `trio.coverage_ratio.search` (== 1.0); emission: `SearchController` → `search.query.duration_ms` Timer |
| SP27 | `/ax-verify-java` + `/ax-verify-L2` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/realtime/` + `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/` + `bash practices/evals/fixtures/realtime_default_polling/_run.sh` | RestAssured `SseSubscribeIT.java` (opt-in path) + Playwright `dirty-guard.spec.ts` + `auto-save-indicator.spec.ts` + `realtime-default-polling.spec.ts` | **Default transport is polling (verified by manifest + L4 fixture)**; SSE delivers 3 events in 5s only when opt-in; dirty-guard blocks navigation; forms RHF wiring complete | Pre-SP27: `blueprints/realtime-policy-manifest.yaml` ENOENT; `realtime-default-polling.spec.ts` fails with `MANIFEST_NOT_FOUND`; `templates/backend/realtime/SseEmitterConfig.java` ENOENT; opt-in `SseSubscribeIT` fails 404 | `npx playwright test templates/L2/_fixtures/realtime-default-polling.spec.ts && npx playwright test templates/L2/_fixtures/dirty-guard.spec.ts` | `sse.active_connections` exposed (when opt-in active); `sse.event_delivery_latency_ms_p99` < 100 (opt-in only); `form.dirty_block.fired_count` correlates (emission: `dirty-guard.tsx` calls `window.__axMetrics?.increment('form.dirty_block.fired_count')` which the SP15 metrics shim — `templates/L1/_lib/metrics.ts` — relays to a configured backend if present) |
| SP28 | `/ax-verify-domain feature-flags` + `/ax-verify-L1` + `/ax-verify-L2` | `bash skills/ax-verify-domain/scripts/run.sh feature-flags` + `bash skills/ax-verify-L1/scripts/run.sh` + `bash skills/ax-verify-L2/scripts/run.sh` + `bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh existing-l4-must-skip` | `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/page.tsx` + `practices-react/evals/fixtures/feature_gate/fail_process_env_check/Component.tsx` + RestAssured `FeatureFlagAdminIT.java` | Hardcoded-string rule fails on Korean literal **only within SP28-scoped fixtures**; rule skips existing L4 paths; feature-gate rule fails on `process.env` check; admin CRUD works | Pre-SP28: rules absent → fixture run reports `RULE_NOT_FOUND` | `bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh pass && bash practices-react/evals/fixtures/feature_gate/_run.sh pass` | `i18n.hardcoded_string.violations` (== 0 in paths matching `applies_to`); `feature_flag.cache.hit_ratio` (gauge via `FeatureFlagCache.java` Micrometer integration); `trio.coverage_ratio.feature-flags` (== 1.0) |
| SP29 | `/ax-verify` (existing Tier-1; subcommand self-tests) | `bash skills/ax-verify/scripts/_subcommand-self-test.sh` + `bash skills/ax-verify/scripts/_eval/policy-check-fp.sh` | `skills/ax-verify/_tests/policy-check-cold.spec.sh` + sibling specs for evidence-fetch + explain | 3 new subcommands self-test exit 0; `/ax-verify policy-check` verdict STOP on cross-L4-import attempt; false-positive rate <5% on 50-fixture eval set; **Tier-1 count remains 4** | Pre-SP29: `skills/ax-verify/scripts/policy-check.sh` ENOENT; spec fails with `SUBCOMMAND_NOT_FOUND` | `bash skills/ax-verify/scripts/_subcommand-self-test.sh` | `policy_check.false_positive_rate` (< 0.05); `evidence_fetch.refresh.attempts_total` (counter emitted by `evidence-fetch.sh` via Micrometer pushgateway when run in CI; locally written to stdout JSON); `explain.responses.cache_hit_ratio` (gauge from in-memory rule-loader LRU cache; emission via `_rule-loader.sh` exposing `--metrics` flag that writes JSON to `${TMPDIR}/ax-verify-explain-metrics.json`) |

---

## §7 Autonomous Execution Safety

> Inherits all PRD §6 + Catalog Extension §6 patterns. Below extends for the SP23–SP29 surface.

### §7.1 Rollback boundary per SP

| SP | Pre-start tag | Rollback boundary |
|---|---|---|
| SP23 | `git tag sp23-pre-start` | Atomic revert: `templates/backend/observability/**` + `templates/backend/cache/**` + 2 new rule files + 3 snapshots; restore `traceid-in-error-response.md` `protects_template_id` to SP21 value |
| SP25 | `git tag sp25-pre-start` | Atomic revert: `templates/backend/data/**` + `templates/backend/jobs/**` + 1 new rule file + 2 snapshots + **revert in-place edit of `templates/backend/BaseEntity.java` (restore SP13 form)**. The 8 existing entities continue to work since they were forward-compatible with the pre-SP25 BaseEntity |
| SP24 | `git tag sp24-pre-start` | Atomic revert: `templates/backend/integration/**` + `templates/backend/export-import/**` + 3 new L2 blocks + 2 new rule files + 1 snapshot; restore self-references on http-* rules |
| SP26 | `git tag sp26-pre-start` | **Atomic revert**: ALL search domain deliverables (Spec Trio + backend + L2 + L3 + L4 + allowlist entry) + chart blocks + 3 snapshots |
| SP27 | `git tag sp27-pre-start` | Per-cluster revert: realtime OR forms can revert independently. Realtime revert includes deletion of `blueprints/realtime-policy-manifest.yaml`; restore L4 README polling-only language |
| SP28 | `git tag sp28-pre-start` | Per-cluster revert: i18n OR feature-flags can revert independently. **Feature-flags revert = atomic revert of full trio/backend/frontend cluster** (Critic soft suggestion 4) |
| SP29 | `git tag sp29-pre-start` | Per-subcommand revert; full revert restores `skills/ax-verify/scripts/run.sh` to pre-SP29 dispatcher form (no subcommand routing); deletes `policy-check.sh` + `evidence-fetch.sh` + `explain.sh` + `_subcommand-self-test.sh` + `_rule-loader.sh` + `_eval/` |

### §7.2 Shared-artifact ownership (Critic Blocker 5 — complete table)

| Artifact | Sole writer SP | Reader SPs | Race-safe protocol |
|---|---|---|---|
| `templates/backend/observability/**` | SP23 | All downstream (SP24/25/26/27 emit metrics) | If SP23 amends post-merge, downstream re-run `/ax-verify-java` |
| `practices/rules/traceid-in-error-response.md` (`protects_template_id` field) | SP23 (updates this existing rule) | All | Append-only update; no other SP modifies this field |
| **`templates/backend/BaseEntity.java`** (**CREATE** — iter3 narrow B3 fix; file was ABSENT at iter3 start; SP25 fills the SP13 gap) | **SP25** | SP16/17/18/19 existing entities (Notification, NotificationPreferences, AuditLog, StoredFile, EmailOutbox, EmailTemplate, ScheduledTask, JobHistory) updated to `extends BaseEntity` (small edit per file); SP24/SP26/SP27/SP28 read-only | **Single sole-writer SP**; create serialized within SP25 lead; rollback boundary = DELETE the SP25-created file + revert the small inheritance edit in the 8 entity files + revert `templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql`; no other SP touches BaseEntity in this cycle |
| **`templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql`** (NEW template SQL — iter3 narrow B3 fix; Critic iter2 Defect 3) | **SP25** | Fork receivers (copy/paste under `backend/src/main/resources/db/migration/` with their own `V<timestamp>` prefix); SP24/SP26 do not touch | Sole writer SP25; template migration script (no DB in repo); filename follows Flyway naming convention from `FlywayConfig.java` |
| **`templates/backend/jobs/JobDispatcher.java`** (interface) | **SP25** | SP24 (`ExportJobService` consumes); SP27 (`RealtimeEventBus` may relay job-completion events) | Interface stable after SP25 close; SP24 and SP27 consume only; no signature changes mid-cycle |
| **`templates/backend/data/PageRequestNormalizer.java`** | **SP25** | SP26 (`SearchController` paged response); SP24 (`ExportJobService` paged listing for incremental export) | Sole writer SP25; consumers read-only |
| **L1 i18n components** (`locale-switcher.tsx`, `currency-formatter.tsx`, `relative-time.tsx`) | **SP28** | All L2/L3/L4 templates that opt into i18n (none in this PRD — opt-in only) | Sole writer SP28; no parallel SP writes to these paths |
| **Cache layer** (`templates/backend/cache/CaffeineConfig.java`, `RedisCacheConfig.java`, `CacheKeyGenerator.java`, `CacheTtlPolicy.java`) | **SP23** | SP28 (`FeatureFlagCache.java` uses `CacheKeyGenerator`); other downstream domains can adopt | Sole writer SP23; downstream consumers reference but do not modify |
| `skills/ax-verify-domain/scripts/domain-registry.yaml` | SP26 (append `search`) + SP28 (append `feature-flags`) — **two writers, race-safe protocol** | SP29 reads | **Race-safe protocol:** (1) `git fetch origin && git rebase origin/<base>` before edit; (2) `yq -i 'sort_keys(.)' domain-registry.yaml` after edit (alphabetic ordering deterministic); (3) `git diff --exit-code` to detect concurrent merge conflicts; (4) if conflict, rebase + reapply. SP closing order is SP26-before-SP28 (graph dictates SP26 finishes before SP28 starts step 6), reducing actual collision likelihood to zero in practice |
| `practices/evals/trio_integrity_allowlist.yaml` | SP26 (append `search: full_trio`) + SP28 (append `feature-flags: full_trio`) — **two writers, race-safe protocol** | All | **Same race-safe protocol as domain-registry.yaml**: rebase against HEAD before append; `yq sort_keys` ensures deterministic ordering; check `git diff --exit-code` after sort |
| `blueprints/realtime-policy-manifest.yaml` (NEW) | SP27 | SP27 own consumers (`live-presence.tsx`, `event-stream.tsx`, `RealtimeOutboxRelay.java`) | Sole writer; new file, no race |
| `blueprints/i18n-policy-manifest.yaml` (NEW) | SP28 | SP28 own consumers (`locale-provider.tsx`, `translation-boundary.tsx`) | Sole writer; new file, no race |
| `blueprints/feature-flags-manifest.yaml` + `blueprints/feature-flags-ui-manifest.yaml` (NEW) | SP28 | SP28 own consumers | Sole writer; new files |
| `practices/rules/_MANIFEST.yaml` + `practices-react/rules/_MANIFEST.yaml` | Regenerated once at end of each SP that adds a rule | All | Regen is the LAST step of each SP commit |
| `practices/AGENTS.md` + `practices-react/AGENTS.md` sentinels | Regenerated atomically by the LAST SP in the cycle (SP29) | All | Per Catalog Extension §6.2 |
| `templates/AGENTS.md` sentinel | SP29 regenerates once at the end | All | Per Catalog Extension §6.2 |
| `skills/ax-verify/scripts/run.sh` (dispatcher update) | SP29 | All downstream callers (legacy + 3 new subcommands) | Sole writer SP29; backward-compat enforced by `_legacy-call-compat.sh` (see SP29 R3) |
| `skills/ax-verify/SKILL.md` (frontmatter `usage:` block edit) | SP29 | All | Sole writer; in-place edit |

### §7.3 Stale-state invalidation rule

Inherits Catalog Extension §6.3 verbatim. Additions:

- Re-run `bash skills/ax-verify-domain/scripts/run.sh <domain>` after any change to `templates/backend/<domain>/` OR allowlist entries.
- Re-run `bash skills/ax-verify/scripts/_subcommand-self-test.sh` after any new rule lands (SP29 only, since the rule-loader index is rebuilt).
- Re-run `cd backend && ./gradlew testAll` after any change to `templates/backend/BaseEntity.java` (SP25 ownership; cascading impact on 8 existing entities).

### §7.4 Halt thresholds

- **3-fail halt**: identical to PRD.
- **30-min idle halt**: identical.
- **5-rebase halt**: identical.
- **Atomic-SP partial-fail halt**: SP26 (search atomic) and SP28 feature-flags cluster — any sub-component failure rolls back the SP/cluster entirely.
- **false-positive cascade halt:** SP29 `/ax-verify policy-check` self-test eval set requires false-positive rate <5%. If 50-fixture run exceeds 5% FP rate, SP29 halts immediately; rule-loader tuning required before re-attempt.
- **BaseEntity in-place break halt (NEW):** SP25 — if any existing domain IT regresses after the in-place BaseEntity edit, SP25 halts immediately; rollback boundary triggers atomic revert.

### §7.5 ESCAPE valve

Identical path: `docs/superpowers/escape/<SP_id>-<timestamp>.md`. No auto-resume; human approval required.

### §7.6 Cross-stack dependency declaration

Each SP that touches both Java and React (SP24, SP26, SP27, SP28) MUST emit `docs/superpowers/sp<NN>-cross-stack-deps.yaml`. Format identical to Catalog Extension §6.6. SP29 is single-stack (skills/scripts) — no cross-stack file required.

---

## §8 Pre-mortem (DELIBERATE mode)

> ≥4 scenarios required. Each with: failure description, likelihood, detection, executable mitigation, threshold.

### Scenario 1 — Realtime SSE leaks server resources on Vercel/serverless

**Failure:** SP27 ships `SseEmitterConfig.java` and a fork receiver opts into SSE without reading the README. Fork receiver deploys to Vercel; serverless function timeout (10s default, max 60s on Pro) kills long-lived SSE connections; client reconnects in a loop; bills explode; user-visible: bell stops updating.

**Likelihood:** **Reduced from "high" to "medium"** in iter2 because SSE is opt-in (default is polling). The failure now requires the fork receiver to **actively opt in** before encountering the trap.

**Detection:**

- SP27 ships `templates/L4/notification/app/(notification)/README.md` with a `## Serverless Deployment` section discussing the SSE opt-in trade-off.
- New gate: `skills/ax-verify-L4/scripts/run.sh` checks for `templates/L4/<domain>/SERVERLESS.md` or README section "Serverless Deployment" when the L4 imports from `templates/backend/realtime/**` AND `ax.realtime.sse.enabled=true` in any committed config. If imports detected and warning absent → exit 1 with `SERVERLESS_WARNING_MISSING`.
- Backend: `RealtimeOutboxRelay.java` emits Micrometer counter `sse.connection.duration_seconds`; threshold alert at p99 > 8s suggests serverless timeout.

**Mitigation (executable):**

- **Owner:** SP27 lead.
- **Command:** `bash skills/ax-verify-L4/scripts/run.sh templates/L4/notification/`.
- **Threshold:** `SERVERLESS_WARNING_MISSING` exit 1 if L4 imports `templates/backend/realtime/**` and opts into SSE without the warning section. Also: `REALTIME_DEFAULT_NOT_POLLING` exit 1 if L4 default config wires SSE without going through the opt-in flag.
- **Recovery:** Default is polling; SSE opt-in flagged via blueprint manifest. README warning automatic when opt-in detected. WebSocket fallback path also available.

### Scenario 2 — i18n L4 wiring fails Korean enterprise default (KRW + 한글 IME)

**Failure:** SP28 ships `currency-formatter.tsx` using `Intl.NumberFormat('ko-KR', {style: 'currency', currency: 'KRW'})`. Modern Node + browser produce `₩1,234` (no decimals), which is correct. But fork receiver's older Node 18 (Korean enterprise default until recently) produces `KRW 1,234.00` (decimals + locale code). User-visible: receipt PDFs show `KRW 1,234.00 원` (double currency, wrong format).

OR: i18n string in `templates/L4/auth/` translation file under-translates → admin sees raw key `auth.signup.submit` in UI. **(Note: this no longer surfaces in SP28 acceptance because existing L4 migration is out of scope per Critic Blocker 4 Option β; the scenario is still listed because if a fork receiver chooses to migrate, this is the trap they will hit.)**

OR: `relative-time.tsx` for Korean uses `Intl.RelativeTimeFormat` but doesn't handle "방금 전" (just now) for <1s → renders `0초 전` (zero seconds ago) which is unnatural Korean.

**Likelihood:** Medium-High. Korean enterprise has subtle locale expectations that off-the-shelf `Intl` doesn't always honor.

**Detection:**

- SP28 ships `templates/L1/_stories/currency-formatter.spec.ts` Playwright story with explicit assertions:
  - KRW 1234 → exactly `₩1,234` (no decimals, no spaces, no trailing `원`).
  - JPY 1234 → exactly `¥1,234`.
- `templates/L1/_stories/relative-time.spec.ts` asserts `<1s → "방금 전"`, `1-59s → "N초 전"`, etc.
- `blueprints/i18n-policy-manifest.yaml` declares minimum Node version (`runtime.node.min: 20`) — pinned-versions guard enforces.

**Mitigation (executable):**

- **Owner:** SP28 lead.
- **Command:** `npx playwright test templates/L1/_stories/currency-formatter.spec.ts templates/L1/_stories/relative-time.spec.ts`.
- **Threshold:** ANY format mismatch on KRW / JPY / "방금 전" → SP28 halts.
- **Recovery:** SP28 ships an internal helper `formatKrw()` in `currency-formatter.tsx` that overrides `Intl` output to the Korean enterprise canon (`₩` prefix, no decimals, no trailing space); ISO 4217 evidence anchor cites `iso-4217.snapshot.md` (existing). Pseudo-Korean fallback in `LocaleProvider` for the relative-time edge cases.

### Scenario 3 — Search domain fails fork-receiver smoke (Meilisearch dep too heavy)

**Failure:** SP26 ships search atomic domain with `MeilisearchAdapter.java` as one of two backends. Fork receiver runs `/ax-fork-receiver` install; `install.sh` does NOT auto-install Meilisearch binary; first attempt to run `templates/L4/search/` fails at runtime with `Connection refused: localhost:7700`. User experience: "search broken out of the box."

OR: Default `PostgresFtsAdapter` works but Korean tokenization with `to_tsvector('simple', ...)` returns broken results for Korean queries (e.g., search `"강남 결제"` returns no results when DB has rows containing both terms).

**Likelihood:** High for Meilisearch path; medium for Postgres-FTS path.

**Detection:**

- SP26 ships `templates/L4/search/README.md` with a `## Backend Choice Decision Tree` section.
- `templates/L4/search/app/(search)/page.tsx` reads `ax.search.backend` config; if unset, defaults to `postgres-fts`; if `meilisearch` but Meilisearch unreachable, returns `503 ProblemDetail` with `MEILISEARCH_UNREACHABLE` instead of generic 500.
- Korean tokenization smoke test: `practices/evals/fixtures/search_korean_tokenization/` ships a 10-row Korean text corpus + expected query results. SP26 acceptance includes running this smoke at install time.

**Mitigation (executable):**

- **Owner:** SP26 lead.
- **Command:** `bash skills/ax-verify-domain/scripts/run.sh search --include-tokenization-smoke`.
- **Threshold:** Korean tokenization smoke must return ≥ 70% expected results on `PostgresFtsAdapter` default config (no mecab). If Korean queries return <70%, install.sh prints WARNING and recommends `ax.search.tokenizer: mecab-ko`.
- **Recovery:** ADR TD-2026-05-18-026 explicitly documents the 70% baseline; Korean enterprise forks adopt `mecab-ko` for the full-fidelity path. Default behavior keeps the catalog runnable on day 1.

### Scenario 4 — `/ax-verify policy-check` produces too many false positives → AI agents ignore it

**Failure:** SP29 ships `/ax-verify policy-check` subcommand. AI agents call it on every file write. The rule index over-matches: any L4 file write triggers `no-cross-l4-domain-imports` check → STOP verdict if ANY import string mentions another L4 dir (even within comments or unused imports). False positive rate spikes >20%. Within a week, AI agents (Claude Code, Codex CLI, etc.) learn to ignore the STOP verdict ("just keep going, the check is broken"). Subcommand loses signal value; rules-as-protection collapses.

**Likelihood:** Medium-High. Rule path-glob matching + intent regex is a notoriously hard precision problem; first iteration will have FP issues.

**Detection:**

- SP29 includes a **50-fixture eval set** under `skills/ax-verify/_eval/fixtures/`:
  - 25 fixtures expected `STOP` (true positives).
  - 15 fixtures expected `PROCEED` (clean).
  - 10 fixtures expected `WARN` (gray area).
- `skills/ax-verify/_eval/policy-check-fp.sh` runs all 50 and computes confusion matrix.
- Acceptance gate: false-positive rate (FP / (FP + TN)) < 5%.
- Observability: emit Micrometer counter `policy_check.verdict_total{verdict=STOP|PROCEED|WARN}` (from `policy-check.sh` via JSON stdout when invoked with `--metrics`, optionally pushed to pushgateway in CI).

**Mitigation (executable):**

- **Owner:** SP29 lead.
- **Command:** `bash skills/ax-verify/_eval/policy-check-fp.sh && bash skills/ax-verify/_eval/_assert-fp-rate.sh`.
- **Threshold:** FP rate ≥ 5% → SP29 halts; rule-loader tuning required (e.g., add `intent_requires_keyword` field per rule).
- **Recovery:** SP29 lead inspects FP fixtures; tunes rule index logic; re-runs. If FP rate cannot be brought below 5% within 3 iterations, fallback: ship `/ax-verify policy-check` as `WARN`-only (no `STOP`) — subcommand is purely advisory. ADR amendment documents.

### Scenario 5 — SP24/SP26 parallel race on shared SP25 surfaces (revised per Critic Blocker 2 + 5)

**Failure:** Iter1's Scenario 5 only named `domain-registry.yaml`. Iter2 expands to the real shared surfaces: SP24 and SP26 run in parallel after SP25, and both consume:

- `templates/backend/jobs/JobDispatcher.java` (SP25-owned interface; SP24 `ExportJobService` consumes; SP27 `RealtimeEventBus` consumes).
- `templates/backend/data/PageRequestNormalizer.java` (SP25-owned; SP24 export listing + SP26 search both consume).
- `templates/backend/BaseEntity.java` (SP25 in-place edit; every existing entity inherits — risk that SP24 or SP26 might inadvertently add a new entity NOT extending BaseEntity, or attempt their own edit).

If SP24 or SP26 add a new entity bypassing BaseEntity (e.g., a `SearchIndexAuditEntry.java` that uses `@SQLDelete` directly), the `soft-delete-only-on-base-entity` rule (SP25-shipped) catches it. But if SP24 or SP26 try to extend the JobDispatcher interface mid-cycle (e.g., add a new method), they break SP25's contract.

**Likelihood:** Low (sole-writer protocol + interface freeze) but high-impact if it happens.

**Detection + Mitigation:**

- **Owner:** SP24 and SP26 leads (consumer side); SP25 lead (sole writer).
- **Command:** Static check `bash practices/evals/fixtures/shared_interface_freeze/_run.sh` — archunit asserts no SP24/SP26 file modifies `templates/backend/jobs/JobDispatcher.java` or `templates/backend/data/PageRequestNormalizer.java` (only adds usages); asserts every new entity in SP24/SP26 extends `BaseEntity`.
- **Threshold:** any modification to `JobDispatcher.java` or `PageRequestNormalizer.java` from a non-SP25 commit → archunit exits 1 with `SHARED_INTERFACE_MODIFIED`; any new entity not extending `BaseEntity` → archunit exits 1 with `ENTITY_BYPASSES_BASE`.
- **Recovery:** Revert offending commit; SP24/SP26 lead opens an ESCAPE valve ticket if a real interface extension is needed (must be approved by SP25 lead during a paused window before either parallel SP resumes).

Inherits Catalog Extension §6.2 `yq`-based sort + rebase-on-conflict + serial merge-order enforced by SP_id for `domain-registry.yaml` and `trio_integrity_allowlist.yaml`.

---

## §9 Open Questions (iter4 — zero items specific to iter4 scope)

> Persisted to `.omc/plans/open-questions.md` per Planner Open Questions protocol.

**Iter4 open question count: 0.** The iter3 residual ambiguity (Option A vs Option B selection moment) is RESOLVED by iter4's unconditional selection of Option B — the version-branch is collapsed, so no run-time selection question remains. The Hibernate version probe still runs at SP25 start for record-keeping only; it does not gate template shape.

(iter3 open questions retained as resolved by iter4:
- iter3 Q1 (Option A vs Option B selection moment) → RESOLVED by iter4 unconditional Option B (per Codex Critic iter3 Hibernate 6.4 `@SoftDelete` boolean-only finding). Auto-flip alternative is moot.

iter2 open questions retained as resolved or still-open advisory — none reopened in iter3 or iter4:
- iter2 Q1 (Mecab-ko interactive install UX, SP26 R2): unchanged — iter2 default (b) stands; not in iter3 narrow scope.
- iter2 Q2 (SP29 subcommand telemetry emission path): unchanged — iter2 default (local JSON) stands; not in iter3 narrow scope.

iter1 questions previously resolved:
- iter1 Q1 "Tier-1 cap exception" → resolved by Critic Blocker 1.
- iter1 Q2 "Existing L4 Korean string migration scope" → resolved by Critic Blocker 4 Option β.
- iter1 Q3 "Search default backend choice" → resolved by SP26 R1.)

---

## §10 Honored Constraints (cross-check vs CLAUDE.md + prior PRDs)

| Constraint | How this PRD honors it |
|---|---|
| **CLAUDE.md: Composition kit, not single product** | Every functional gap closure is independently fork-adoptable. `/ax-verify policy-check` works against any catalog subset. |
| **CLAUDE.md: React + Spring Boot 둘 다 active equal partner** | Both stacks grow significantly: 55 backend templates + 30 frontend (L1+L2+L3+L4) templates + 1 in-place BaseEntity edit. 5 Java rules + 2 React rules. Search atomic domain ships both halves. Feature-flags atomic domain ships both halves. |
| **CLAUDE.md: 거버넌스 무한루프 금지** | No promotion-gate docs. Every SP terminates on binary `exit 0`. No `evidence bundle` or `curated promotion check` artifacts. |
| **CLAUDE.md: 새 도메인 추가는 정상 활동** | 2 new full_trio domains (search, feature-flags) celebrated, not gated. |
| **CLAUDE.md: Fork받은 팀의 정책을 skill이 강제 금지** | `/ax-verify policy-check` is pre-mutation advice for AI agents, not a merge-gate. `/ax-verify evidence-fetch` doesn't enforce refresh; advises. `/ax-verify explain` is explanatory. None enforce team policy. |
| **PRD-1 §3.1 evidence frontmatter required** | Every new template ships `evidence:` or `@ax-template-meta`. |
| **PRD-1 §3.2 No new top-level Tier-1 beyond {ax-transform, ax-verify, ax-scaffold, ax-fork-receiver}** | **HONORED** in iter2 (was VIOLATED in iter1). F13/F14/F15 ship as `/ax-verify` subcommands. Tier-1 count stays at 4. ADR TD-2026-05-18-029 documents the subcommand extension architecture and rejects the iter1 3-new-Tier-1 path. |
| **PRD-1 §3.2 No MockMvc** | All new tests use RestAssured (Java) + Playwright (TS) + Vitest (TS unit). |
| **Catalog Extension Critic anti-bloat: P0 rule names protects_template_id + failing_fixture_path** | All 7 new rules anchor both; §5.6 table is canonical. |
| **Catalog Extension Critic atomic Spec-Trio ordering** | SP26 (search) and SP28 (feature-flags) ship Spec Trio + backend + frontend in ONE atomic SP each. |
| **Composition-kit user-facing surface = skills, not raw gradle/npm** | `/ax-verify policy-check|evidence-fetch|explain` are subcommands of an existing skill. `/ax-verify-*` and `/ax-verify-domain` are wrappers. |
| **Per Catalog Extension §10: hyphenated domain support in run-gradle.sh** | `search`, `feature-flags` use hyphens; verified compatible (SP16 side-fix). |
| **No bounded migration of existing L4 strings (Critic Blocker 4 Option β)** | i18n rule `applies_to` explicitly excludes existing L4 directories; SP28 acceptance runs against SP28-scoped fixtures only. Future P1 SP can pick up bounded migration. |

---

## §11 Out-of-Scope (explicit, iter2-updated)

Per user directive — these are NOT in this PRD and MUST NOT be added by Architect/Critic:

- **Public release prep:** LICENSE / CONTRIBUTING.md / docs site / GitHub Pages.
- **CI/CD:** GitHub Actions workflows / release.yml / Dependabot config / catalog integrity check workflow.
- **Release/deployment tooling:** no `.github/workflows/*.yml` files added.
- **v2 architectural changes:** no skill topology overhaul; **Tier-1 count stays at 4**; no Spec Trio schema changes; no `ax-template-meta` shape changes.
- **Existing L4 Korean string migration** (Critic Blocker 4 Option β): `templates/L4/{auth,crud,payment,practices,notification,audit-log,file-storage}/` Korean strings remain hardcoded until a future P1 SP. SP28's `no-hardcoded-user-facing-string-in-l4` rule `applies_to` exclusion list explicitly skips these.
- **3 new Tier-1 skills (iter1 Option E, retired in iter2):** F13/F14/F15 ship as subcommands of existing `/ax-verify` Tier-1 only.
- **Sibling `BaseEntityWithSoftDelete` class (Critic Blocker 3):** soft-delete is applied in-place on the SP13 common `BaseEntity` template; no new sibling class.
- **Realtime SSE/WebSocket as L4 default transport (architect polish):** polling is the default; SSE/WebSocket are opt-in via `blueprints/realtime-policy-manifest.yaml`.
- **P1 backend domain extractions:** existing `backend/src/main/java/.../auth|crud|payment|...` reorganization deferred.
- **P1 rules from Catalog Extension §12** remain P1.

---

## §12 ADR-ready commit content (for Step 6 if APPROVE)

```yaml
---
adr_id: TD-2026-05-18-030
title: Functional Capability Extension (SP23–SP29, F1–F15) — iter2 consensus
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-18-functional-extension-prd.iter2.md
  rationale: |
    Iter2 PRD closes 6 Critic hard blockers + 1 architect polish from the
    iter1 consensus loop. Tier-1 count stays at 4 via /ax-verify subcommand
    extension. Shared infra (BaseEntity in-place soft-delete, JobDispatcher
    interface, PageRequestNormalizer) lands in SP25 as foundation before
    SP24/SP26 parallel. Existing L4 i18n migration explicitly out of scope.
    Realtime defaults to polling; SSE/WebSocket opt-in.
spec_ref: METHODOLOGY.md#A.4
---

## Decision
Extend the ax-template catalog with ~80 atoms across 9 functional surfaces
(observability, cache, data, jobs, integration, export-import, charts, search,
realtime, forms, i18n, feature-flags) shipped across 7 SPs (SP23–SP29).
F13/F14/F15 ship as subcommands of the existing /ax-verify Tier-1 skill;
zero new Tier-1.

## Drivers
(1) Self-reference closure (rules that protect nothing today).
(2) Atomic Spec-Trio ordering for search + feature-flags.
(3) Pre-execution policy check (/ax-verify policy-check closes first-turn gap
    without growing Tier-1 surface).

## Alternatives considered
A. Single mega-SP — rejected (blast radius).
B. SP-per-functional-area-group with shared-infra-first serialization — CHOSEN.
C. Atomic per-gap split (15 SPs) — rejected (overhead).
D. Defer F13–F15 — rejected (loses 6 SPs of catalog feedback).
E. F13/F14/F15 as 3 new Tier-1 skills (iter1's path) — rejected per
   Critic Blocker 1 and Architect §1: ceremonial surface growth, all three
   are verification sub-modes; subcommands satisfy the same capability with
   lower surface area.

## Why chosen
Option B groups disjoint surfaces, honors atomic-ordering for F6 (search) and
F9 (feature-flags), lands shared infra (SP25) BEFORE parallel SPs (SP24,
SP26), keeps Tier-1 frozen at 4 by using subcommands, and lets SP24/SP26 and
SP27/SP28 run in disjoint parallel windows — wall-time ≈ 10–11 d.

## Consequences
- 55 new backend templates across 9 folders + 1 in-place edit of
  templates/backend/BaseEntity.java.
- 4 new L1 + 26 new L2 + 1 new L3 + 2 new L4 domains.
- 7 new rules (5 Java + 2 React); i18n rule applies_to excludes pre-existing
  L4 directories.
- 0 new Tier-1 / Tier-2 / Tier-3 skills; 3 new subcommands under /ax-verify.
- 2 new Spec Trios (search full_trio, feature-flags full_trio).
- 12 new upstream snapshots.
- 4 existing rules with protects_template_id updated to close self-references
  (mdc + 3× http-* rules).
- 1 new blueprint: blueprints/realtime-policy-manifest.yaml (polling default,
  SSE/WebSocket opt-in).
- Shared-artifact ownership table (§7.2) explicitly assigns sole writers for
  BaseEntity, JobDispatcher, PageRequestNormalizer, cache layer, i18n L1
  components, realtime manifest, allowlist/registry race-safe protocols.

## Follow-ups
- Existing templates/L4/{auth,crud,payment,practices,notification,audit-log,
  file-storage}/ Korean string migration (future P1 SP, out of scope here).
- 180-d refresh of new upstream snapshots.
- L4 sealed sub-agent re-run on search + feature-flags domains.
- Korean tokenizer install prompt UX (Open Question 1).
- Subcommand telemetry emission protocol (Open Question 2).
```

---

## §13 Iter2 Delta (vs iter1 draft) — Blocker resolutions

| # | Critic iter1 hard blocker / architect polish | Iter2 resolution | Iter2 reference |
|---|---|---|---|
| B1 | Flip SP29 to `/ax-verify` subcommands; Tier-1 cap stays 4 | F13/F14/F15 ship as `/ax-verify policy-check|evidence-fetch|explain` subcommands; ZERO new Tier-1. Updated §1 Options (Option E retired), §3 O7, §4 P0 rows, §5.7, §6.8 SP29 deliverables + acceptance, §6.9 verification matrix SP29 row, §10 honored constraint row, §11 out-of-scope, §12 ADR | §1, §3 O7, §4 (F13/F14/F15), §5.7, §6.8, §6.9 SP29, §10, §11, §12 |
| B2 | Re-serialize dependency graph: SP23 → SP25 → SP24 ‖ SP26 → SP27 ‖ SP28 → SP29 | Graph reordered. SP25 lands shared infra (JobDispatcher interface, PageRequestNormalizer) BEFORE parallel SP24/SP26. ADR "Why chosen" updated | §1 (recommended), §6.1 graph, §12 ADR "Why chosen" |
| B3 | BaseEntity in-place extension, not sibling `BaseEntityWithSoftDelete` | Iter1's `BaseEntityWithSoftDelete.java` removed. SP25 edits `templates/backend/BaseEntity.java` in place with `@SQLDelete` + `@Where` + `deletedAt`. Impact list (8 existing entities) added to §6.3 deliverables. Acceptance includes RestAssured smoke on all 8 entities. Rollback restores SP13 form | §5.1 data row, §5.6 soft-delete rule row, §6.3 deliverables + acceptance + R1 mitigation, §7.1 SP25 rollback, §7.2 BaseEntity sole-writer row, §7.4 BaseEntity halt threshold |
| B4 | SP28 i18n scope contradiction — pick one path | **Option β chosen.** i18n rule `applies_to: paths_created_after_2026-05-18` with explicit `exclude_existing_domains` list. SP28 acceptance no longer claims to migrate existing L4 strings; runs only against SP28-scoped fixtures. Out-of-scope statement updated. Future P1 SP for bounded migration | §3 Must Have guardrail row, §4 F8 row, §5.6 i18n rule row + applies_to column, §6.7 SP28 deliverables + acceptance + R1 mitigation, §11 out-of-scope |
| B5 | §7.2 shared-artifact ownership — complete table | §7.2 rewritten. Added rows for: i18n L1 components (SP28), cache layer (SP23), BaseEntity (SP25), JobDispatcher interface (SP25), PageRequestNormalizer (SP25), domain-registry.yaml (SP26 + SP28 race-safe), trio_integrity_allowlist.yaml (SP26 + SP28 race-safe), realtime-policy-manifest.yaml (SP27), i18n-policy-manifest.yaml (SP28), feature-flags manifests (SP28), `skills/ax-verify/scripts/run.sh` dispatcher (SP29), `skills/ax-verify/SKILL.md` (SP29) | §7.2 |
| B6 | SP-card risks → executable mitigations (owner + command + threshold + recovery for each) | Every SP card §6.2–§6.8 risk now has Owner, Command, Threshold, Recovery. Specific iter1 placeholders replaced: SP24 Resilience4j pin (now has fixture command + compat threshold); SP24 POI memory (now has archunit command + heap test threshold); SP25 migration collision (now has flyway_naming fixture command); SP27 serverless (now defaults to polling, not just a warning) | §6.2 R1/R2, §6.3 R1/R2/R3, §6.4 R1/R2, §6.5 R1/R2/R3, §6.6 R1/R2, §6.7 R1/R2/R3, §6.8 R1/R2/R3 |
| P1 (architect polish) | SP27 default L4 notification/audit/payment to polling; SSE/WebSocket opt-in via blueprint | `blueprints/realtime-policy-manifest.yaml` ships with `default_transport: polling` and opt-in SSE/WebSocket. L4 default behavior fixture validates polling-only by default. Acceptance criteria add manifest + fixture checks. Out-of-scope explicitly forbids SSE/WebSocket as default | §3 O5, §4 F7 row, §5.1 realtime row, §6.6 deliverables (manifest spec) + acceptance + TDD anchor + R1 mitigation, §6.9 SP27 row, §11 out-of-scope, §8 Scenario 1 likelihood downgraded |

**Iter2 line count target:** +100–200 vs iter1 (851). Actual iter2: ~1010 lines (delta ≈ +160).

---

## §13.1 Iter3 Delta (vs iter2) — Single narrow B3 closure for SP25

> Codex Critic iter2 verdict: ITERATE. 5/6 hard blockers CLOSED, 1 PARTIAL (B3 BaseEntity in-place). 4 specific defects flagged on SP25:
>
> - Defect 1: `templates/backend/BaseEntity.java` not present in workspace (iter2 asserted in-place edit of a non-existent file).
> - Defect 2: `@SQLDelete` on `@MappedSuperclass` is unsafe — Hibernate static SQL requires per-table name.
> - Defect 3: Flyway migration adding `deleted_at` to the 8 existing tables not named.
> - Defect 4: RestAssured smoke coverage list omitted `NotificationPreferences`.

| # | Critic iter2 defect | Iter3 resolution | Iter3 reference |
|---|---|---|---|
| Edit 1 | Defect 1 — BaseEntity.java not in workspace | SP25 deliverable list updated: `templates/backend/BaseEntity.java` flagged as **CREATE** (file ABSENT at iter3 start; SP25 fills the SP13 gap as a create-or-repair task). Pre-flight disk fact added to header. | header pre-flight, §6.3 Inputs, §6.3 Deliverables (BaseEntity CREATE block), §7.2 BaseEntity row |
| Edit 2 | Defect 2 — @SQLDelete on @MappedSuperclass unsafe | Option A (Hibernate 6.4+ `@SoftDelete` declarative annotation) selected as primary; Option B (per-subclass `@SQLDelete` + `@Where` on superclass) documented as fallback. Pre-flight Hibernate version check (`./gradlew dependencies | grep hibernate-core`) prescribed before BaseEntity creation. Workspace confirms `org.springframework.boot 3.2.12` → Hibernate ORM 6.4.x → Option A feasible. R4 risk row added. | header pre-flight, §6.3 pre-flight Hibernate version check, §6.3 BaseEntity CREATE block (Option A primary / Option B fallback), §6.3 R4 risk |
| Edit 3 | Defect 3 — Flyway migration for `deleted_at` on 8 tables not named | New template SQL file added: `templates/backend/data/migrations/V202605181200__add_soft_delete_columns.sql` — 8 ALTER TABLE statements + 8 partial INDEX statements. Explicit note: this is TEMPLATE SQL for fork receivers (no DB exists in this repo). Flyway migration syntax check added to acceptance command. | §6.3 Deliverables (migration SQL block), §6.3 acceptance criteria, §7.2 new migrations ownership row |
| Edit 4 | Defect 4 — RestAssured smoke omitted NotificationPreferences | RestAssured acceptance list rewritten to explicitly enumerate all 8 entities (with paths): Notification, **NotificationPreferences** (was missing — added), AuditLog, StoredFile, EmailOutbox, EmailTemplate, ScheduledTask, JobHistory. Each verified for CREATE → SOFT-DELETE → `deleted_at` populated + read filter excludes. | §6.3 acceptance criteria (numbered 8-entity list) |
| Edit 5 | Verification matrix SP25 row update | Test file set to `backend/src/test/java/com/ax/template/baseentity/BaseEntitySoftDeleteIT.java`. Assertion rewrites cover 8 entities + Flyway migration syntax. First-green command set to `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteIT`. Observability signal `baseentity.soft_delete.applied_count` added (Micrometer counter emitted by `SoftDeleteAspect.java`). Expected RED reason updated to "BaseEntity.java does not exist". | §6.4 verification matrix SP25 row |
| Edit 6 | §7 ownership table BaseEntity row | Row relabelled from "in-place soft-delete extension" → "CREATE — file was ABSENT at iter3 start; SP25 fills SP13 gap". Rollback boundary rewritten: DELETE the SP25-created BaseEntity + revert 8 inheritance edits + revert migration template. New row added for `V202605181200__add_soft_delete_columns.sql`. | §7.2 BaseEntity row, §7.2 new migrations row |
| Edit 7 | SP25 risk card — Hibernate version risk | R4 added: downstream fork may downgrade Hibernate < 6.4, breaking `@SoftDelete`. Owner/Command/Threshold/Recovery shaped per Critic B6 schema from iter2. Recovery flips template to Option B. | §6.3 R4 risk |

**Iter3 line count target:** +30–50 vs iter2 (1024 lines). Actual iter3: see file tail.

**What iter3 did NOT change** (per Critic narrow scope):
- SP23/SP24/SP26/SP27/SP28/SP29 SP cards — untouched.
- §1 RALPLAN-DR (Principles, Decision Drivers, Options, recommended Option) — untouched.
- §8 Pre-mortem (5 scenarios) — untouched.
- §10 Honored constraints — untouched.
- §11 Out-of-scope — untouched.
- §12 ADR template — untouched (will be filled at Step 6 commit only if Critic iter3 returns APPROVE).
- §13 iter2 delta table — untouched (historical record).

---

## §13.2 Iter4 Delta (vs iter3) — Single blocking Hibernate-soft-delete-semantics closure

> Codex Critic iter3 verdict: ITERATE. 3/4 iter3 defects CLOSED, 1 BLOCKING: Hibernate ORM 6.4 `@SoftDelete` is **boolean indicator only** (`SoftDeleteType` enum = ACTIVE / DELETED only) and is therefore incompatible with this PRD's `deleted_at TIMESTAMP` contract. Iter3's Option A primary selection silently mis-modelled the deletion moment as a boolean — implementation would have shipped broken. Iter4 collapses Option A → Option B unconditionally and adds an ArchUnit coverage rule to prevent per-subclass `@SQLDelete` drift.

| # | Critic iter3 finding | Iter4 resolution | Iter4 reference |
|---|---|---|---|
| Edit 1 | BaseEntity CREATE block carries `@SoftDelete(columnName = "deleted_at")` (Option A primary) | `@SoftDelete` removed entirely; `@MappedSuperclass` keeps only `@Where(clause = "deleted_at IS NULL")` for the read filter (applies via inheritance) + `Instant deletedAt` field with `@Column(name = "deleted_at")`. Explicit instruction to each subclass to add its own `@SQLDelete`. Timestamp contract intact. | header iter4 pre-flight, §6.3 BaseEntity CREATE block (Option B soft-delete sub-block) |
| Edit 2 | iter3 only inlined Option B as a fallback without per-subclass `@SQLDelete` text for all 8 entities | New per-subclass `@SQLDelete` sub-block in §6.3 Deliverables with literal SQL shape for `Notification` + `NotificationPreferences` and full 8-entity↔table mapping. Each entity adds `@SQLDelete(sql = "UPDATE <table> SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")`. | §6.3 Deliverables (per-subclass `@SQLDelete` sub-block + entity↔table table) |
| Edit 3 | Pre-flight Hibernate version check branches Option A vs Option B | Branch collapsed. Probe still runs at SP25 start for record-keeping in `templates/backend/data/README.md`. Decision: Option B selected for Hibernate ≥ 5.0+ as long as `deleted_at TIMESTAMP` is required. Footnote retains Option A pathway only for a hypothetical future PRD switch to boolean indicator semantics. | §6.3 pre-flight Hibernate version check block |
| Edit 4 | iter3 R4 risk = "fork downgrades Hibernate" — moot now that template never depended on `@SoftDelete` | R4 replaced with "Per-subclass `@SQLDelete` repetition risks drift if a new entity is added without it". Owner = SP25 lead. Command = ArchUnit `BaseEntitySubclassMustCarrySqlDelete` rule, wired into `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteArchTest`. Threshold = 0 violations. Recovery = CI gate refuses commit until `@SQLDelete` added. | §6.3 R4 risk |
| Edit 5 | Iter delta record + closure status | This §13.2 row added. §9 Open Questions count reduced to 0 (iter3 Q1 resolved by unconditional Option B). §14 closure language updated to iter4. Edit: Option A → Option B per Codex Critic Hibernate 6.4 `@SoftDelete` boolean-only finding (iter 3 attack). | §13.2 (this section), §9, §14 |
| Edit 6 | Verification matrix SP25 row needed iter4-specific assertions and expected RED reason | Row updated: assertion adds "every `BaseEntity` subclass carries `@SQLDelete` per iter4 R4 ArchUnit rule"; expected RED reason adds "Per-subclass `@SQLDelete` annotation missing on any entity" (`BASE_ENTITY_SUBCLASS_MISSING_SQL_DELETE`); script command adds `cd backend && ./gradlew :test --tests BaseEntitySoftDeleteArchTest`; first-green command extended to require per-subclass annotations. | §6.9 verification matrix SP25 row |

**Iter4 line count target:** +30–50 vs iter3 (1108 lines). Actual iter4: see file tail.

**What iter4 did NOT change** (per Critic narrow scope):
- SP23/SP24/SP26/SP27/SP28/SP29 SP cards — untouched.
- §1 RALPLAN-DR (Principles, Decision Drivers, Options, recommended Option) — untouched.
- §8 Pre-mortem (5 scenarios) — untouched.
- §10 Honored constraints — untouched.
- §11 Out-of-scope — untouched.
- §12 ADR template — untouched (will be filled at Step 6 commit only if Critic iter4 returns APPROVE).
- §13 iter2 delta table, §13.1 iter3 delta table — untouched (historical record).
- Previously CLOSED iter3 defects (BaseEntity CREATE, Flyway migration, 8-entity smoke list) — not relitigated.

---

## §14 End of PRD

**Ready for Codex Critic iter 4 closure re-review.**

**Re-review scope (narrow, per Critic iter3 trigger):** SP25 only — §6.3 BaseEntity CREATE block (soft-delete shape), §6.3 per-subclass `@SQLDelete` sub-block (8 entities), §6.3 pre-flight Hibernate version check (Option B unconditional), §6.3 R4 risk (ArchUnit coverage rule), §6.9 verification matrix SP25 row, §9 Open Questions (count = 0), §13.2 iter4 delta. No broad re-review requested.

**Next step:** Codex Critic iter 4 verifies the single blocking Hibernate `@SoftDelete` semantics defect is closed at binary-implementable resolution. On Critic APPROVE (verdict: APPROVE), commit canonical PRD as TD-2026-05-18-030 ADR, hand off `SP23 → SP25 → SP24 ‖ SP26 → SP27 ‖ SP28 → SP29` to `/team`. SP25 worker: at start of SP, run the pre-flight Hibernate version check, record the actual version in `templates/backend/data/README.md` for provenance, then author `templates/backend/BaseEntity.java` under Option B (no `@SoftDelete`; `@Where` + `Instant deletedAt`) and add the per-subclass `@SQLDelete` annotation to each of the 8 entities per the §6.3 mapping. ArchUnit rule `BaseEntitySubclassMustCarrySqlDelete` ships in the same SP to prevent drift.
