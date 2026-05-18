# Functional Capability Extension PRD — 2026-05-18 (draft)

> **Status:** Planner draft (ralplan consensus loop, Step 1). DELIBERATE mode auto-enabled (multi-week work, ≥7 SPs, atomic Spec-Trio promotions, new Tier-1 skills, cross-stack rules with PII implications). Ready for Architect + Critic round.
> **Scope:** Functional gaps **F1–F15** identified in the post-`v1.0.0-catalog-complete` review. ONE PRD covering **SP23–SP29** (7 SPs). All public-release / CI / deployment workflow concerns are explicitly OUT OF SCOPE — see §12.
> **Predecessors:** `2026-05-17-frontend-templatization-prd.md` (CLOSED, APPROVED, SP1–SP12). `2026-05-18-catalog-extension-prd.md` (CLOSED, tag `v1.0.0-catalog-complete`, commit `9212989`, SP13–SP22).
> **Format:** RALPLAN-DR. Korean enterprise = composition-kit. React + Spring Boot equal partners.

---

## §1 RALPLAN-DR Summary

### Principles (5) — inherited verbatim from PRD §RALPLAN-DR + Catalog Extension §1, not re-litigated

1. **Composition kit, not single product.** Every new artifact (template / rule / skill / spec) must be fork-adoptable in isolation; no atom is single-application.
2. **Spec-before-code, evidence-anchored.** Every new template carries `evidence:` (frontmatter OR `@ax-template-meta`); every new rule carries `protects_template_id` + `failing_fixture_path`; every new ADR declares `provenance_class`.
3. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` skill returns exit 0. No "done" on prose. No advisory-only acceptance.
4. **Few exposed surfaces, dense feedback loops underneath.** Tier-1 count grows minimally (18 → 21 skills; +3 Tier-1 explicitly Critic-mandated; zero Tier-2/Tier-3 change).
5. **No speculative generality.** Every functional gap closes a specific L4 flow that breaks today, OR retires a self-reference (a rule that protects nothing) — see §3 P0 Inventory "Justification" column.

### Decision Drivers (top 3)

1. **Self-reference closure.** SP21 shipped `traceid-in-error-response` (Java) and `traceId-rendered-on-error-boundary` (React) but no observability template currently sets the MDC traceId; the rules currently protect nothing. F1 closes this loop and is therefore P0.
2. **Atomic Spec-Trio ordering (Critic mandate, inherited).** Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs. SP grouping for F6 (search) honors this directly.
3. **Skill-orchestrated pre-execution gating.** Today `/ax-verify` is post-execution (after AI agent has touched files). F13 (`/ax-policy-check`) closes the **first-turn** gap: AI agent learns before mutation. This is qualitatively different from `/ax-verify` and Critic-mandated for the consensus loop.

### Mode

**DELIBERATE.** Auto-triggered by: (a) multi-week wall-time, (b) ≥3 new failing-fixture rules, (c) new Tier-1 skills, (d) cross-stack rules with PII implications (i18n string handling, feature-flag admin endpoints).

### Viable Options Considered (≥2 mandatory)

- **Option A — Single mega-SP (SP23-only) shipping all 15 functional gaps.**
  - Pros: one rollback boundary; one merge tag.
  - Cons: 15 functional gaps × ~6 surfaces each ≈ 90 atoms in one SP = unmanageable blast radius; violates §6 atomic rollback rule from Catalog Extension SP16/17/18/19 (atomic domain SP); defeats `/team` parallelism (4–5 agents must work disjoint surfaces).
  - **Rejected.**

- **Option B — SP-per-functional-area-group (current proposal, 7 SPs SP23–SP29).**
  - Pros: each SP groups ≤3 functional gaps with shared verify skill; atomic Spec-Trio rule honored for F6; F13/F14/F15 skills cluster in SP29; observability (F1) precedes integration (F2) which precedes data layer (F3) — natural infra dependency order.
  - Cons: 7 SPs sequential dependencies mean wall-time ≈ 10–12 d if serial. Mitigated: SP23/24/26 disjoint surfaces and can run in parallel after their prerequisites.
  - **CHOSEN.**

- **Option C — Split atomic domain SPs separately (F6 search-only as its own SP, all others as a separate SP each).**
  - Pros: minimum blast radius per SP.
  - Cons: 15 SPs = coordination overhead exceeds value; observability + cache (F1+F10) share verify skill and snapshots, so splitting them adds redundant `evidence:` work; ditto F2 + F12 (both touch outbound HTTP retry policy).
  - **Rejected.**

- **Option D — Defer F13–F15 (new skills) until after F1–F12 ship.**
  - Pros: skills land against a known-stable catalog; less coupling risk during catalog growth.
  - Cons: F13 (`/ax-policy-check`) is the **first-turn** gate; deferring it means SP23–SP28 ship templates that downstream AI agents cannot pre-check. Loses 6 SPs of empirical feedback for F13's rule matching.
  - **Rejected.** F13/14/15 ship in SP29 immediately after F1–F12 so they have a fully populated catalog to demo, but BEFORE the next PRD cycle so the skills are available from cycle-2 day-1.

### Recommended: **Option B — SP-per-functional-area-group**

Cycle = **SP23 (F1 observability + F10 cache, shared verify-java + shared snapshots)** → **SP24 (F2 integration + F12 export/import, shared retry/circuit-breaker pattern)** → **SP25 (F3 data + F4 jobs, shared JPA + scheduled-task reuse)** → **SP26 (F5 charts L2 + F6 search atomic domain)** → **SP27 (F7 realtime + F11 forms)** → **SP28 (F8 i18n + F9 feature flags)** → **SP29 (F13/F14/F15 three new Tier-1 skills, integration over full post-SP28 tree)**. Total: **7 SPs, ≈ 10–11 d wall-time**.

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
- F13 (`/ax-policy-check`) — AI agent's **first turn** has no binary BLOCK/PROCEED signal; only `/ax-verify` exists (post-mutation).

Cycle 3 closes these 15 gaps across 7 SPs (SP23–SP29).

---

## §3 Objectives + Guardrails

### Objectives (one per SP minimum)

- **O1 (SP23):** Make observability + cache strategy **fork-runnable**. After SP23, a fork receiver can `bash skills/ax-verify-domain/scripts/run.sh observability` and get binary signal that MDC traceId, OTLP exporter, structured logging, and Caffeine/Redis cache are wired correctly.
- **O2 (SP24):** Make external HTTP integration + bulk export/import **fork-runnable** with circuit breaker + HMAC + chunked-import policy. Closes the audit gap where no webhook signature rule was wired despite SP21 demoting `webhook-signature-verify` to P1.
- **O3 (SP25):** Make data layer (Flyway + JPA audit + soft-delete + jsonb + optimistic-lock + standard paging) and on-demand background jobs **fork-runnable** without reinventing schema-migration and queue scaffolding.
- **O4 (SP26):** Add charts L2 cluster + ship `search` as atomic full_trio domain (Spec Trio + backend FTS adapter + L2 SearchPalette/TypeaheadSearch + L4 page). Korean IME behavior coverage extends to typeahead.
- **O5 (SP27):** Add realtime/SSE backend bridge + L2 (LivePresence, OptimisticUpdate, EventStream) + form orchestration L2 (FieldArray, ConditionalField, DependentField, FormErrorSummary, AutoSaveIndicator, DirtyGuard, FormSection) — the latter retires the SP15 Critic-flagged "form-section is still wire-up only" warning.
- **O6 (SP28):** i18n/locale (LocaleProvider, ko-KR + en-US, KRW format, fallback chain, blueprint manifest) + feature flags as `feature-flags: full_trio` (admin UI + L1 `FeatureGate` + L2 `FeatureFlagToggle` + service-layer template).
- **O7 (SP29):** Ship `/ax-policy-check`, `/ax-evidence-fetch`, `/ax-explain` as 3 new Tier-1 skills. Closes the pre-execution gate gap (F13), the manual upstream-refresh gap (F14), and the rule-violation-explanation gap (F15).

### Guardrails — Must Have

- Every new template (L1/L2/L4/backend) carries `evidence:` frontmatter OR an `@ax-template-meta` comment block. No exceptions.
- Every new rule carries `protects_template_id` (pointing to a specific template id under `templates/`) AND `failing_fixture_path` (pointing to a `practices/evals/fixtures/<rule>/fail_*/` directory). Inherits Catalog Extension SP21 Critic anti-bloat enforcement.
- Spec Trio ordering: F6 search ships as atomic SP (Spec Trio + backend + L2 + L4 in ONE SP).
- Skill topology cap: only Tier-1 grows (+3); zero Tier-2/Tier-3 change.
- Allowlist additions (`practices/evals/trio_integrity_allowlist.yaml`): `search: full_trio`, `feature-flags: full_trio`. Verify HEAD before commit in autonomous mode.
- Hyphenated domain names (`search`, `feature-flags`) — confirmed supported by `run-gradle.sh` per SP16 side-fix in Catalog Extension.
- Every new failing fixture causes the named guard to exit non-zero with the named error string; the `pass/` sibling fixture exits 0.

### Guardrails — Must NOT

- No GitHub Actions workflows added (out of scope; see §12).
- No LICENSE/CONTRIBUTING/docs-site/release.yml/Dependabot files added (out of scope).
- No v2 architectural overhaul — skill topology stays 3-tier; Spec Trio schema unchanged; `ax-template-meta` shape unchanged.
- No placeholder `exit 0` guard stubs. Every new gate exits non-zero on real failure.
- No raw `./gradlew testXxx` or `npm run xxx` as a user-facing surface in skills — every skill calls the underlying script with stable args (already enforced by §7 ADR TD-2026-05-17-007 from PRD-1).
- No new MockMvc tests; RestAssured only.
- No `@SpringBootTest` slow tests in new failing-fixture suites — fixture tests use archunit OR static-script OR Vitest (fast).

---

## §4 P0 Inventory (15 functional gaps, atomicized to 7 SPs)

> Each F-row cites either (a) the L4 flow that breaks today, OR (b) which existing rule/template protects nothing without the gap closure. Effort: S = ≤4h, M = 4–8h, L = 8–16h, XL = >16h. All "Owning verify skill" entries are existing skills (zero new Tier-2/Tier-3).

| F | Surface | Justification (cite the broken L4 flow OR the self-reference) | Effort | Owning verify skill | Depends-on | Ships in SP |
|---|---|---|---|---|---|---|
| F1 | Backend observability templates (`templates/backend/observability/`) | SP21 rule `practices/rules/traceid-in-error-response.md` currently `protects_template_id: templates/backend/error/ProblemDetailFactory.java`, but `ProblemDetailFactory.java` (`templates/backend/error/`) reads from `MDC.get("traceId")` which is never set because **no template wires MDC**. Self-reference closure. | M | `/ax-verify-java` | none | SP23 |
| F10 | Cache strategy templates (`templates/backend/cache/`) + `practices/rules/cacheable-requires-explicit-ttl.md` | L4 `templates/L4/payment/` uses `@Cacheable` indirectly via `PaymentMethodService` (existing `backend/.../PaymentMethodService.java` example) but no template demonstrates how. Caffeine + Redis split is the canonical Spring Boot pattern; we ship neither. Rule failing fixture: `@Cacheable("x")` without `cacheManager` + TTL. | M | `/ax-verify-java` | F1 (cache stats emit via Micrometer registered in F1) | SP23 |
| F2 | External integration (`templates/backend/integration/`) | Existing rule `practices/rules/http-explicit-timeouts.md` protects nothing in `templates/backend/` (no `WebClient` template exists). `practices/rules/http-restclient-over-resttemplate.md` ditto. SP24 closes both self-references. New rule `webhook-hmac-required` — failing fixture `fail_webhook_no_hmac/Controller.java` accepts webhook POST without `X-Signature` header validation. | L | `/ax-verify-java` | F1 (circuit-breaker emits metrics) | SP24 |
| F12 | Export/Import (`templates/backend/export-import/`) + L2 ImportPreview/MappingEditor/ImportProgressBar | L4 `templates/L4/audit-log/` has `audit-log-export` page (`templates/L3/pages/export-job-status/`) but no backend export service template — page is a shell. New rule `chunked-import-required-when-rowcount-gt-1000` — failing fixture `fail_single_tx_10k_rows/Service.java` reads 10k rows in one TX → archunit fails. | L | `/ax-verify-java` + `/ax-verify-L2` | F4 (reuses JobDispatcher for long-running export); shared SP24 with F2 (both touch outbound HTTP / chunked retry pattern) | SP24 |
| F3 | Data layer (`templates/backend/data/`) | Existing rule `practices/rules/api-pagination-pageable.md` references `PageResponse.java` (SP13) but no `PageRequestNormalizer.java` exists to enforce the **max-limit** policy. JPA `@Version` rule (`practices/rules/jpa-optimistic-locking.md`, see catalog) protects no template. Flyway naming convention is undocumented. New rule `soft-delete-only-on-base-entity` — failing fixture `fail_soft_delete_no_base/Entity.java`. | M | `/ax-verify-java` | none | SP25 |
| F4 | Background jobs on-demand (`templates/backend/jobs/`) | Existing `templates/backend/scheduled-task/` covers time-based (cron). On-demand (user-triggered, async) is not templated. L4 `templates/L4/payment/` payment async confirmation pattern is hand-rolled in `backend/src/main/java/.../payment/`. JobDispatcher + JobQueue (Redis Streams OR DB-row queue) + JobWorker + JobHistoryProjection. Reuses SP19 scheduled-task pattern for the time-based half. | M | `/ax-verify-java` | F3 (DB-row queue uses Flyway from F3); F1 (DLQ metrics) | SP25 |
| F5 | Charts/dataviz L2 (`templates/L2/blocks/`) | TimeSeriesChart, BarChart, PieChart, KPICard, Sparkline, Heatmap. No L4 today renders charts; payment-revenue-trend dashboard and audit-log timeline both need them. `recharts` (Apache-2.0) is the proposed wrapper. New upstream snapshot: `recharts-2026-05.snapshot.md`. | M | `/ax-verify-L2` | none | SP26 |
| F6 | Search atomic domain (`specs/search-l0.yaml` + `contracts/search-openapi.yaml` + `blueprints/search-manifest.yaml` + `templates/backend/search/` + `templates/L2/blocks/{search-palette-extended,typeahead-search,result-highlighter,recent-searches}.tsx` + `templates/L4/search/`) | Universal SaaS surface. Hangul IME (SP21 rule #65) currently protects only `combobox` and `search-palette` (SP15 existing); typeahead-search has no IME coverage. Existing `templates/L2/blocks/search-input.tsx` is a shell. Allowlist add: `search: full_trio`. | L | `/ax-verify-domain search` (existing Tier-2 skill, registry entry added) | F1 (search service emits query latency metrics); shared SP26 with F5 (both grow L2) | SP26 |
| F7 | Realtime/SSE (`templates/backend/realtime/` + `templates/L2/blocks/{live-presence,optimistic-update,event-stream}.tsx`) | L4 `templates/L4/notification/` bell currently polls via TanStack Query (per SP16 deliberate trade); SSE backend bridge unshipped. L4 `templates/L4/audit-log/` live-tail unimplemented. L4 `templates/L4/payment/` status transition events: also polling. RealtimeEventBus = DB outbox → SSE bridge (reuses email-outbox pattern). | M | `/ax-verify-java` + `/ax-verify-L2` | F4 (Job DLQ events feed event-bus); F1 (SSE connection metrics) | SP27 |
| F11 | Form orchestration L2 (FieldArray, ConditionalField, DependentField, FormErrorSummary, AutoSaveIndicator, DirtyGuard, FormSection) | SP15 shipped `field-array.tsx`, `form-section.tsx`, `conditional-field.tsx`, `form-error-summary.tsx` as **shells with TODO comments** (Critic flagged SP15 §11). SP27 promotes them to RHF + Zod standardized blocks; adds DependentField + AutoSaveIndicator + DirtyGuard (missing). Standardizes `useForm()` signature across all forms. | M | `/ax-verify-L2` | none; shared SP27 with F7 (both grow L2) | SP27 |
| F8 | i18n / Locale (`templates/L1/components/{locale-switcher,currency-formatter,relative-time}.tsx` + `templates/L2/blocks/{locale-provider,translation-boundary}.tsx` + `blueprints/i18n-policy-manifest.yaml`) | Hardcoded Korean strings in `templates/L4/auth/` + `templates/L4/payment/` (verified). Korean enterprise default must be ko-KR + en-US with KRW format. New rule `no-hardcoded-user-facing-string-in-l4` — failing fixture `fail_hardcoded_korean_string/page.tsx` (regex matches non-ASCII string literal outside `t()` wrapper). Reuses `next-intl`. | M | `/ax-verify-L1` + `/ax-verify-L2` | none | SP28 |
| F9 | Feature flags (`templates/backend/feature-flags/` + `templates/L1/components/feature-gate.tsx` + `templates/L2/blocks/feature-flag-toggle.tsx` + `specs/feature-flags-l0.yaml` + `contracts/feature-flags-openapi.yaml` + `blueprints/feature-flags-manifest.yaml`) | Allowlist add: `feature-flags: full_trio`. Enterprise forks deploy flags within first 30 days. Today no template exists. New rule `prefer-feature-gate-over-env-check` — failing fixture `fail_process_env_check/Component.tsx` checks `process.env.FEATURE_X === '1'` instead of `<FeatureGate name="X">`. | M | `/ax-verify-domain feature-flags` (registry add) | shared SP28 with F8 (both touch L4 wiring + admin UI) | SP28 |
| F13 | `/ax-policy-check` (Tier-1 NEW) | `/ax-verify` is **post-mutation**. AI agent's first turn writes a file, then learns. F13 is **pre-mutation**: input = file path + intent (free-text); output = applicable rules + `protects_template_id` matches + required evidence locations + STOP/PROCEED hint. Failing fixture: cold call with a path that violates `no-cross-l4-domain-imports` → skill outputs `STOP` + cites the rule. | M | self-tested via `skills/ax-policy-check/scripts/_self-test.sh` | All prior SPs (must demo against full catalog) | SP29 |
| F14 | `/ax-evidence-fetch` (Tier-1 NEW) | `time_decay_guard` flags stale snapshots but **refresh is manual** (`practices/upstream/fetch.sh` requires human invocation per snapshot). F14: input = snapshot id (or `--all`); output = WebFetch → diff vs current snapshot → refresh recommendation. Quarterly stale auto-detection. | M | self-tested via `skills/ax-evidence-fetch/scripts/_self-test.sh` | F1 (uses Micrometer counter for refresh-attempts); shared SP29 with F13 | SP29 |
| F15 | `/ax-explain` (Tier-1 NEW) | When `/ax-verify` outputs `RULE_VIOLATED: no-server-component-state-leakage-to-client`, the AI agent has no machine-readable trace to `protects_template_id` → `evidence:` → violating-vs-compliant examples → related rules. F15 closes the rule-violation explanation gap for both AI agents and humans. | S | self-tested via `skills/ax-explain/scripts/_self-test.sh`; shared SP29 with F13/F14 | F13 (reuses rule-loader infrastructure) | SP29 |

**Total functional gaps: 15. Total atoms (cross-counting templates + rules + skills + specs): ~85** (rough; concrete §5 deliverables tables are the source of truth per SP).

---

## §5 Inventory by Surface

### §5.1 Backend templates added (per domain folder)

| Folder | New templates | Source SP |
|---|---|---|
| `templates/backend/observability/` (NEW) | `MdcCorrelationIdInterceptor.java`, `OtelTracerConfig.java`, `MicrometerConfig.java`, `StructuredLoggingConfig.java`, `LogbackJsonAppenderConfig.java`, `DatabaseHealthIndicator.java`, `RedisHealthIndicator.java`, `ExternalHttpHealthIndicator.java` (8 files) | SP23 |
| `templates/backend/cache/` (NEW) | `CaffeineConfig.java`, `RedisCacheConfig.java`, `CacheKeyGenerator.java`, `CacheTtlPolicy.java` (4 files) | SP23 |
| `templates/backend/integration/` (NEW) | `WebClientConfig.java`, `ExternalApiTemplate.java`, `WebhookReceiver.java`, `WebhookSender.java`, `BulkheadConfig.java`, `CircuitBreakerConfig.java` (6 files) | SP24 |
| `templates/backend/export-import/` (NEW) | `CsvImportService.java`, `ExcelImportService.java`, `ExportJobService.java`, `ImportErrorReportDto.java`, `ImportChunkProcessor.java` (5 files) | SP24 |
| `templates/backend/data/` (NEW) | `FlywayConfig.java`, `JpaAuditConfig.java`, `SecurityAuditorAware.java`, `SoftDeleteAspect.java`, `BaseEntityWithSoftDelete.java`, `JsonbConverter.java`, `OptimisticLockingPolicy.java`, `PageRequestNormalizer.java` (8 files) | SP25 |
| `templates/backend/jobs/` (NEW) | `JobDispatcher.java`, `JobQueue.java`, `RedisStreamsJobQueue.java`, `DbRowJobQueue.java`, `JobWorker.java`, `JobHistoryProjection.java`, `JobDlqHandler.java` (7 files) | SP25 |
| `templates/backend/search/` (NEW; atomic with F6 Spec Trio) | `SearchIndexService.java`, `SearchController.java`, `SearchDto.java`, `SearchQueryParser.java`, `PostgresFtsAdapter.java`, `MeilisearchAdapter.java`, `SearchBackend.java` (interface, 7 files) | SP26 |
| `templates/backend/realtime/` (NEW) | `SseEmitterConfig.java`, `RealtimeEventBus.java`, `SseSubscription.java`, `WebSocketConfig.java`, `RealtimeOutboxRelay.java` (5 files) | SP27 |
| `templates/backend/feature-flags/` (NEW; atomic with F9 Spec Trio) | `FeatureFlagEntity.java`, `FeatureFlagService.java`, `FeatureFlagController.java`, `FeatureFlagAdminController.java`, `FeatureFlagRepository.java`, `FeatureFlagCache.java` (6 files) | SP28 |

**Total new backend templates: 56 across 9 folders.**

### §5.2 L1 components added (i18n primitives)

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

**Total new L2: 26** (33 → 59). SP15 shell files (`field-array.tsx`, `form-section.tsx`, etc.) retained for back-compat; SP27 ADR documents the supersede contract.

### §5.4 L3 page templates added

| File | Source SP |
|---|---|
| `templates/L3/pages/search-results-page/` (resurrected from P1 demotion in Catalog Extension, now justified by F6 search atomic SP) | SP26 |

**Total new L3: 1** (15 → 16). Other L3 pages already exist (`export-job-status`, `import-csv`); SP24 wires them to F2/F12 backend templates.

### §5.5 L4 domains added

| Domain | Mode | Files | Source SP |
|---|---|---|---|
| `search` | `full_trio` | `templates/L4/search/app/(search)/page.tsx`, `templates/L4/search/app/(search)/results/page.tsx`, `templates/L4/search/middleware.ts`, `templates/L4/search/next.config.ts`, `templates/L4/search/README.md` | SP26 |
| `feature-flags` | `full_trio` (admin-only UI) | `templates/L4/feature-flags/app/(admin)/feature-flags/page.tsx`, `templates/L4/feature-flags/app/(admin)/feature-flags/[name]/page.tsx`, `templates/L4/feature-flags/middleware.ts`, `templates/L4/feature-flags/next.config.ts`, `templates/L4/feature-flags/README.md` | SP28 |

**Total new L4: 2** (7 → 9).

### §5.6 New rules per catalog (anti-bloat cap honored)

| Rule | Catalog | `protects_template_id` | `failing_fixture_path` | Source SP |
|---|---|---|---|---|
| `mdc-traceid-required-on-controller` (Java) | `practices/rules/` | `templates/backend/observability/MdcCorrelationIdInterceptor.java` | `practices/evals/fixtures/mdc_traceid_required/fail_no_interceptor/` | SP23 |
| `cacheable-requires-explicit-ttl` (Java) | `practices/rules/` | `templates/backend/cache/CacheTtlPolicy.java` | `practices/evals/fixtures/cacheable_ttl/fail_no_ttl/` | SP23 |
| `webhook-hmac-required` (Java) | `practices/rules/` | `templates/backend/integration/WebhookReceiver.java` | `practices/evals/fixtures/webhook_hmac/fail_no_signature_check/` | SP24 |
| `chunked-import-required-when-rowcount-gt-1000` (Java) | `practices/rules/` | `templates/backend/export-import/ImportChunkProcessor.java` | `practices/evals/fixtures/chunked_import/fail_single_tx_10k/` | SP24 |
| `soft-delete-only-on-base-entity` (Java) | `practices/rules/` | `templates/backend/data/BaseEntityWithSoftDelete.java` | `practices/evals/fixtures/soft_delete_base/fail_soft_delete_no_base/` | SP25 |
| `no-hardcoded-user-facing-string-in-l4` (React) | `practices-react/rules/` | `templates/L2/blocks/translation-boundary.tsx` | `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/` | SP28 |
| `prefer-feature-gate-over-env-check` (React) | `practices-react/rules/` | `templates/L1/components/feature-gate.tsx` | `practices-react/evals/fixtures/feature_gate/fail_process_env_check/` | SP28 |

**Total new rules: 7** (5 Java + 2 React). Cap respected: ≤ 1 rule per functional gap on average, each anchored to a concrete `protects_template_id` shipped in the same SP.

### §5.7 New skills (3 Tier-1)

| Skill | Tier | Path | Source SP |
|---|---|---|---|
| `/ax-policy-check` | Tier-1 NEW | `skills/ax-policy-check/SKILL.md` + `scripts/run.sh` + `scripts/_self-test.sh` | SP29 |
| `/ax-evidence-fetch` | Tier-1 NEW | `skills/ax-evidence-fetch/SKILL.md` + `scripts/run.sh` + `scripts/_self-test.sh` | SP29 |
| `/ax-explain` | Tier-1 NEW | `skills/ax-explain/SKILL.md` + `scripts/run.sh` + `scripts/_self-test.sh` | SP29 |

**Total skills: 21** (18 → 21; only Tier-1 grows). Tier-2 / Tier-3 unchanged.

### §5.8 New upstream snapshots

| Snapshot | Source | Source SP |
|---|---|---|
| `practices/upstream/spring-boot-actuator-2026-05.snapshot.md` (extend existing or add suffix; new probes) | Spring docs | SP23 |
| `practices/upstream/opentelemetry-java-2026-05.snapshot.md` (NEW) | OpenTelemetry Java instrumentation docs | SP23 |
| `practices/upstream/micrometer-prometheus-2026-05.snapshot.md` (NEW) | Micrometer docs | SP23 |
| `practices/upstream/resilience4j-2026-05.snapshot.md` (NEW) | Resilience4j docs | SP24 |
| `practices/upstream/spring-flyway-2026-05.snapshot.md` (extend or NEW) | Flyway + Spring Boot docs | SP25 |
| `practices/upstream/spring-data-jpa-auditing-2026-05.snapshot.md` (NEW) | Spring Data JPA docs | SP25 |
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
| TD-2026-05-18-024 | External integration + Export/Import (F2 + F12) | `internal_design` (anchored to Resilience4j + Apache POI canon) |
| TD-2026-05-18-025 | Data layer + Background jobs (F3 + F4) | `internal_design` (Flyway + JPA + Redis Streams canon) |
| TD-2026-05-18-026 | Charts L2 + Search atomic domain (F5 + F6) | `external_canonical` (recharts + PostgreSQL FTS canon) |
| TD-2026-05-18-027 | Realtime SSE + Form orchestration (F7 + F11) | `internal_design` |
| TD-2026-05-18-028 | i18n + Feature flags (F8 + F9) | `internal_design` + `locked_constraint` (KRW formatting per ISO 4217) |
| TD-2026-05-18-029 | 3 new Tier-1 skills (F13 + F14 + F15) | `internal_design` (composition-kit AI agent UX) |

---

## §6 Implementation Plan (SP23–SP29)

### SP dependency graph

```
SP23 (observability + cache) ────┬─▶ SP24 (integration + export/import)
                                 │                │
                                 ├─▶ SP25 (data + jobs) ─────────┐
                                 │                                │
                                 ├─▶ SP26 (charts + search atomic)
                                 │                                │
                                 │   ┌────────────────────────────┘
                                 │   ▼
                                 ├─▶ SP27 (realtime + forms)
                                 │   │
                                 │   ▼
                                 └─▶ SP28 (i18n + feature flags)
                                         │
                                         ▼
                                       SP29 (3 new Tier-1 skills, full-tree integration)
```

**Critical ordering rules:**

- SP23 lands first: observability infra (MDC, Micrometer, structured logging) underpins downstream verify metrics and DLQ counters in SP24/25/27.
- SP24/SP25/SP26 can run in parallel after SP23 (disjoint surfaces). Mitigation per §7 below addresses the `domain-registry.yaml` shared-write risk.
- SP27 depends on SP25 (realtime event bus uses DB-row outbox from `templates/backend/data/` patterns; jobs DLQ feeds event bus).
- SP28 can run in parallel with SP27 after SP25 (i18n + feature flags do not depend on realtime/forms).
- SP29 lands last: needs full post-SP28 tree to demo policy-check / evidence-fetch / explain.

---

### SP23 — Observability + Cache (F1 + F10)

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
- **Risks + mitigation:**
  - **R:** OTLP exporter requires env var `OTEL_EXPORTER_OTLP_ENDPOINT`; CI may not have an OTLP collector.
    **M:** `OtelTracerConfig.java` uses `@ConditionalOnProperty("ax.observability.otlp.enabled")`; default off. Tests use no-op exporter.
  - **R:** Caffeine + Redis cache name collision — same cache name in both managers.
    **M:** `CacheTtlPolicy.java` asserts mutual-exclusion at startup; archunit rule asserts no cache name appears in both `caffeine.cache-names` and `redis.cache-names` in `application.yml`.
- **Agent count:** 1 lead + 2 workers (parallel: observability worker, cache worker).
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp23-pre-start`. Atomic revert of `templates/backend/observability/**` + `templates/backend/cache/**` + 2 new rule files + 3 new snapshots; restore `traceid-in-error-response.md` `protects_template_id` to its SP21 value.

---

### SP24 — External Integration + Export/Import (F2 + F12)

- **Inputs:** SP23 done. Existing `templates/backend/email-outbox/` (SP19) for outbound-queue pattern reuse.
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
    - `ExcelImportService.java` — Apache POI streaming reader.
    - `ExportJobService.java` — Long-running via `JobDispatcher` from SP25 (forward reference; SP24 ships interface, SP25 supplies impl).
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
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh && bash skills/ax-verify-L2/scripts/run.sh`.
- **TDD anchor:** `practices/evals/fixtures/webhook_hmac/fail_no_signature_check/PaymentWebhookController.java` — controller mapping `/webhooks/payment` without `@RequestHeader("X-Signature")` validation. Pre-SP24: rule file absent. SP24 ships rule + `pass/PaymentWebhookControllerWithHmac.java`. First green: `bash practices/evals/fixtures/webhook_hmac/_run.sh pass`.
- **Risks + mitigation:**
  - **R:** Resilience4j version drift (Spring Boot 3.x compat).
    **M:** Pin in `blueprints/pinned-versions.yaml`.
  - **R:** Apache POI memory usage on Excel imports >100MB.
    **M:** Use `SXSSFWorkbook` streaming reader; archunit rule asserts no `XSSFWorkbook` in `*Import*Service` classes.
- **Agent count:** 1 lead + 3 workers (integration, export-import, L2).
- **Effort:** L (2–3 d).
- **Rollback boundary:** `git tag sp24-pre-start`. Atomic revert.

---

### SP25 — Data Layer + Background Jobs (F3 + F4)

- **Inputs:** SP23 done.
- **Deliverables:**
  - **F3 templates (8 files)** under `templates/backend/data/`:
    - `FlywayConfig.java` — `@Configuration` + migration naming convention (`V<yyyyMMddHHmm>__<snake_case_description>.sql`).
    - `JpaAuditConfig.java` — `@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")`.
    - `SecurityAuditorAware.java` — pulls `@CreatedBy` / `@LastModifiedBy` from Spring Security context.
    - `SoftDeleteAspect.java` — AOP advice on `@SoftDelete` annotated repos.
    - `BaseEntityWithSoftDelete.java` — `@SQLDelete(sql="UPDATE ... SET deleted_at = now() WHERE id = ?")` + `@Where(clause = "deleted_at IS NULL")` + extends `BaseEntity` from SP13.
    - `JsonbConverter.java` — `@Converter` for PostgreSQL `jsonb` ↔ Java `Map<String,Object>`.
    - `OptimisticLockingPolicy.java` — `@Version` enforcement archunit rule helper.
    - `PageRequestNormalizer.java` — Wraps `Pageable` w/ max-limit cap (default 100, configurable via `ax.paging.max-limit`).
  - **F4 templates (7 files)** under `templates/backend/jobs/`:
    - `JobDispatcher.java` — interface w/ `SyncJob` + `AsyncJob` sub-interfaces.
    - `JobQueue.java` — interface.
    - `RedisStreamsJobQueue.java` + `DbRowJobQueue.java` — two implementations (fork-receiver picks).
    - `JobWorker.java` — Consume loop + DLQ.
    - `JobHistoryProjection.java` — Read model for admin UI.
    - `JobDlqHandler.java` — Re-queue / kill / inspect.
  - **Rule (1):** `soft-delete-only-on-base-entity.md` + failing fixture.
  - **Snapshots:** spring-flyway (NEW or extend), spring-data-jpa-auditing (NEW).
- **Acceptance criteria:**
  - `bash skills/ax-verify-java/scripts/run.sh templates/backend/data/ templates/backend/jobs/` → exit 0.
  - JPA audit RestAssured: insert via authenticated context → `created_by`, `created_at` populated.
  - Soft-delete RestAssured: `DELETE /api/v1/items/{id}` → row's `deleted_at` set; subsequent `GET` returns 404.
  - Job dispatch: enqueue 100 jobs → 100 history rows; 1 failing job → 1 DLQ row.
  - Paging: request `size=10000` → server caps at 100.
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh`.
- **TDD anchor:** `practices/evals/fixtures/soft_delete_base/fail_soft_delete_no_base/Item.java` declares `@SoftDelete` but does NOT extend `BaseEntityWithSoftDelete` → archunit fails. Pre-SP25: rule and base class absent. SP25 ships both; `pass/ItemWithBase.java` passes. First green: `bash practices/evals/fixtures/soft_delete_base/_run.sh pass`.
- **Risks + mitigation:**
  - **R:** Flyway migration naming collision when fork receivers add their own migrations.
    **M:** Templates ship under `templates/backend/data/_migrations/` examples only; fork applies to `backend/src/main/resources/db/migration/`. README explicit.
  - **R:** `RedisStreamsJobQueue` requires Redis ≥ 5.0.
    **M:** Default impl is `DbRowJobQueue`; Redis is opt-in via `ax.jobs.queue.backend=redis-streams`.
- **Agent count:** 1 lead + 2 workers.
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp25-pre-start`. Atomic revert.

---

### SP26 — Charts L2 + Search Atomic Domain (F5 + F6)

- **Inputs:** SP23 done. L1 `combobox` (SP14) exists for typeahead.
- **Deliverables:**
  - **F5 charts (6 L2 blocks):** `templates/L2/blocks/time-series-chart.tsx`, `bar-chart.tsx`, `pie-chart.tsx`, `kpi-card.tsx`, `sparkline.tsx`, `heatmap.tsx`. recharts wrapper + composition.
  - **F6 search (atomic — Spec Trio + backend + L2 + L4 in ONE SP):**
    - **Spec Trio:** `specs/search-l0.yaml` + `specs/search-frontend-l0.yaml` + `contracts/search-openapi.yaml` + `contracts/search-ui.yaml` + `blueprints/search-manifest.yaml` + `blueprints/search-ui-manifest.yaml`.
    - **Backend (7 files):** `templates/backend/search/` — see §5.1.
    - **L2 (4 blocks):** `search-palette-extended.tsx`, `typeahead-search.tsx`, `result-highlighter.tsx`, `recent-searches.tsx`.
    - **L3 page:** `templates/L3/pages/search-results-page/{page.tsx, error.tsx, loading.tsx, README.md}`.
    - **L4 domain:** `templates/L4/search/app/(search)/page.tsx` + `results/page.tsx` + middleware + next.config.
    - **Allowlist:** add `search: full_trio` to `practices/evals/trio_integrity_allowlist.yaml`.
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
- **Risks + mitigation:**
  - **R:** Meilisearch is heavy (binary distro, RAM). Fork receivers without Korean enterprise infra may be blocked.
    **M:** Default adapter is `PostgresFtsAdapter` (zero extra infra); Meilisearch is opt-in via `ax.search.backend=meilisearch`. ADR explicit.
  - **R:** PostgreSQL FTS Korean tokenization requires `mecab-ko` or `kkma` — fork-receiver heavy lift.
    **M:** PostgresFtsAdapter ships with `to_tsvector('simple', ...)` default (works without Korean analyzer at ~70% quality); Korean analyzer is opt-in via blueprint manifest `search.tokenizer: mecab-ko`. ADR documents the trade.
  - **R:** Korean IME in `typeahead-search.tsx` interacts with debounce — text corruption on `compositionstart`.
    **M:** Reuse SP21 IME simulator fixture; Playwright story is mandatory in acceptance.
- **Agent count:** 1 lead + 3 workers (charts worker, search backend worker, search frontend worker).
- **Effort:** L (2 d).
- **Rollback boundary:** `git tag sp26-pre-start`. **Atomic revert** (search domain is atomic per Critic mandate).

---

### SP27 — Realtime/SSE + Form Orchestration (F7 + F11)

- **Inputs:** SP25 done (jobs DLQ feeds event-bus; data layer outbox table reused).
- **Deliverables:**
  - **F7 templates (5 files)** under `templates/backend/realtime/`:
    - `SseEmitterConfig.java` — `SseEmitter` with timeout + heartbeat.
    - `RealtimeEventBus.java` — DB outbox → SSE bridge.
    - `SseSubscription.java` — per-user subscription.
    - `WebSocketConfig.java` — STOMP fallback (for environments where SSE blocked).
    - `RealtimeOutboxRelay.java` — relays from outbox table to SSE.
  - **F7 L2 (3 blocks):** `live-presence.tsx`, `optimistic-update.tsx`, `event-stream.tsx`.
  - **F11 L2 (7 blocks):** form orchestration `-extended` cluster.
  - **Snapshots:** spring-mvc-sse (NEW), react-hook-form (NEW or extend).
- **Acceptance criteria:**
  - `bash skills/ax-verify-java/scripts/run.sh templates/backend/realtime/` → PASS.
  - `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/` → PASS (realtime + forms blocks).
  - SSE end-to-end RestAssured: subscribe to `/api/v1/events?topic=notification` → receive 3 mock events within 5s.
  - Form `dirty-guard` Playwright: navigate-away with unsaved changes → confirm dialog appears.
- **Verify command:** `bash skills/ax-verify-java/scripts/run.sh && bash skills/ax-verify-L2/scripts/run.sh`.
- **TDD anchor:** `templates/L2/_fixtures/dirty-guard.spec.ts` — Playwright fixture navigating away triggers `beforeunload`. Pre-SP27: `dirty-guard.tsx` ENOENT. First green: `npx playwright test templates/L2/_fixtures/dirty-guard.spec.ts`.
- **Risks + mitigation:**
  - **R:** SSE in serverless (Vercel) — long-lived connection limits.
    **M:** ADR TD-2026-05-18-027 documents Vercel limitations; `templates/L4/notification/app/(notification)/` README warns to switch to polling fallback on Vercel. WebSocketConfig.java is the alternate path. See §7 pre-mortem Scenario 1.
- **Agent count:** 1 lead + 2 workers (realtime, forms).
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp27-pre-start`. Per-cluster revert allowed (realtime independent of forms).

---

### SP28 — i18n + Feature Flags (F8 + F9)

- **Inputs:** SP25 done.
- **Deliverables:**
  - **F8 L1 (3):** `locale-switcher.tsx`, `currency-formatter.tsx`, `relative-time.tsx`.
  - **F8 L2 (2):** `locale-provider.tsx` (next-intl wrapper), `translation-boundary.tsx`.
  - **F8 blueprint:** `blueprints/i18n-policy-manifest.yaml` — declares ko-KR + en-US minimum, KRW format rules (`₩` symbol position, no decimals), fallback chain (ko-KR → en-US → key), pseudo-locale for testing.
  - **F8 rule:** `no-hardcoded-user-facing-string-in-l4.md` + failing fixture (regex on non-ASCII string literal in `templates/L4/**/*.tsx` outside `t()`).
  - **F9 templates (6 files)** under `templates/backend/feature-flags/`.
  - **F9 Spec Trio (full_trio):** `specs/feature-flags-l0.yaml` + `specs/feature-flags-frontend-l0.yaml` + `contracts/feature-flags-openapi.yaml` + `contracts/feature-flags-ui.yaml` + `blueprints/feature-flags-manifest.yaml` + `blueprints/feature-flags-ui-manifest.yaml`.
  - **F9 L1 (1):** `feature-gate.tsx`.
  - **F9 L2 (1):** `feature-flag-toggle.tsx` (admin UI).
  - **F9 L4 (5 files):** `templates/L4/feature-flags/app/(admin)/feature-flags/page.tsx` + `[name]/page.tsx` + middleware + next.config + README.
  - **F9 rule:** `prefer-feature-gate-over-env-check.md` + failing fixture.
  - **Allowlist:** add `feature-flags: full_trio`.
  - **Snapshots:** next-intl (NEW).
- **Acceptance criteria:**
  - `bash skills/ax-verify-L1/scripts/run.sh && bash skills/ax-verify-L2/scripts/run.sh` → PASS.
  - `bash skills/ax-guard-trio-integrity/scripts/run.sh` → PASS (feature-flags full_trio).
  - `bash skills/ax-verify-domain/scripts/run.sh feature-flags` → PASS.
  - i18n end-to-end: switch locale ko-KR → en-US, all `templates/L4/auth/` strings update; KRW format verified.
  - Feature-flag end-to-end: admin creates flag → L4 page reads via `<FeatureGate>` → toggle propagates within cache TTL.
  - Hardcoded-string probe runs against `templates/L4/auth/`, `templates/L4/payment/` — passes (after migration); against `fail_korean_literal/` fixture — fails.
- **Verify command:** `bash skills/ax-verify-domain/scripts/run.sh feature-flags && bash skills/ax-verify-L2/scripts/run.sh`.
- **TDD anchor:** `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/page.tsx` — `<button>결제하기</button>` literal (not `t('payment.submit')`). Pre-SP28: rule absent. SP28 ships rule + LocaleProvider; pass fixture uses `t()`. First green: `bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh pass`.
- **Risks + mitigation:**
  - **R:** Migrating existing `templates/L4/auth/` and `templates/L4/payment/` Korean strings to `t()` calls is a large diff.
    **M:** SP28 ships rule with `applies_to: templates/L4/**/*.tsx (new files only)` in v1; existing L4 string migration is **out of scope** (a side-fix follow-up SP, not in this PRD). ADR documents.
  - **R:** KRW format ambiguity (₩ before number, no decimals vs JPY-like).
    **M:** ISO 4217 + `iso-4217.snapshot.md` (existing); blueprint manifest cites exact rule. See §7 pre-mortem Scenario 2.
- **Agent count:** 1 lead + 2 workers (i18n, feature flags).
- **Effort:** M (1.5 d).
- **Rollback boundary:** `git tag sp28-pre-start`. Atomic revert per cluster (i18n + feature-flags can revert independently).

---

### SP29 — 3 New Tier-1 Skills (F13 + F14 + F15)

- **Inputs:** SP23–SP28 done (full post-SP28 catalog).
- **Deliverables:**
  - **`/ax-policy-check` (Tier-1 NEW)** under `skills/ax-policy-check/`:
    - `SKILL.md` — frontmatter `name: ax-policy-check`, description, `tier: 1`, `invocation: user-direct`.
    - `scripts/run.sh` — args: `--file <path> --intent <free-text>`. Output: JSON `{applicable_rules: [...], protects_template_id_matches: [...], required_evidence_locations: [...], verdict: "STOP" | "PROCEED" | "WARN", citations: [...]}`.
    - `scripts/_loader.sh` — loads `practices/rules/*.md` + `practices-react/rules/*.md` + builds rule index by `applies_to`.
    - `scripts/_self-test.sh` — calls run.sh with 3 known fixtures: (a) path violating `no-cross-l4-domain-imports` → expect `STOP`; (b) path matching `feature-gate.tsx` write → expect `WARN` (must include FeatureGate evidence); (c) clean L1 component write → expect `PROCEED`.
  - **`/ax-evidence-fetch` (Tier-1 NEW)** under `skills/ax-evidence-fetch/`:
    - `SKILL.md`, `scripts/run.sh` (`--snapshot <id> | --all`), `scripts/_compare.sh` (WebFetch + `time_decay_guard` integration), `scripts/_self-test.sh`.
  - **`/ax-explain` (Tier-1 NEW)** under `skills/ax-explain/`:
    - `SKILL.md`, `scripts/run.sh` (`--rule-id <id> | --violation-msg "<msg>"`), `scripts/_self-test.sh`.
  - **`/ax-fork-receiver` update:** registry of 3 new skills added to the tarball bundle list (`bundle.sh`).
- **Acceptance criteria:**
  - `bash skills/ax-policy-check/scripts/_self-test.sh` → exit 0; verdicts match expected per fixture.
  - `bash skills/ax-evidence-fetch/scripts/_self-test.sh` → exit 0; mocked WebFetch returns expected diff/no-diff per fixture.
  - `bash skills/ax-explain/scripts/_self-test.sh` → exit 0; explanation includes `protects_template_id` + evidence quote + at least 1 violating example + at least 1 compliant example.
  - `/ax-policy-check --file templates/L4/payment/app/(payment)/page.tsx --intent "import from L4/audit-log"` → outputs `STOP` + cites `practices-react/rules/no-cross-l4-domain-imports.md`.
  - Tarball cold-install (existing `/ax-fork-receiver`): 3 new skills present in extracted tree; `ax-policy-check --help` runs.
- **Verify command:** `bash skills/_tests/run-all-self-tests.sh`.
- **TDD anchor:** `skills/ax-policy-check/_tests/policy-check-cold.spec.sh` — fixture-based: writes a controlled file path, intent string, expects deterministic verdict. Pre-SP29: skill absent → spec fails with `SKILL_NOT_FOUND`. First green: `bash skills/ax-policy-check/scripts/_self-test.sh`.
- **Risks + mitigation:**
  - **R:** `/ax-policy-check` produces false positives → AI agents start ignoring it (signal-degradation cascade).
    **M:** Verdict ladder STOP / WARN / PROCEED. Only `STOP` is blocking. `STOP` fires only when ≥1 rule's `applies_to` path-glob matches AND the intent string contains a banned-action keyword (regex match). Threshold: false-positive rate <5% on a 50-fixture eval set (built in SP29 itself). See §7 pre-mortem Scenario 4.
  - **R:** `/ax-evidence-fetch` WebFetch dependency may hang on slow upstream.
    **M:** 30s timeout per fetch; `--offline` mode reads cached snapshot diff only.
  - **R:** 3 new Tier-1 skills exceed the topology cap that was already exceeded in Catalog Extension by `/ax-fork-receiver`.
    **M:** ADR TD-2026-05-18-029 documents the exception explicitly. Critic must approve. Fallback path if Architect rejects: ship F13/F14/F15 as sub-commands of `/ax-verify` (e.g., `/ax-verify --policy-check`, `/ax-verify --evidence-fetch`, `/ax-verify --explain`). Default for this PRD: ship as 3 new Tier-1.
- **Agent count:** 1 lead + 3 workers (one per skill).
- **Effort:** M (1 d).
- **Rollback boundary:** `git tag sp29-pre-start`. Per-skill revert allowed.

---

### §6.5 Verification Matrix (single authoritative table)

| SP | verify_skill | script_path | test_file | assertion | expected_RED_reason | first_green_command | observability_signal |
|---|---|---|---|---|---|---|---|
| SP23 | `/ax-verify-java` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/observability/ templates/backend/cache/` + `bash practices/evals/run-all-guards.sh --include-fixtures` | `practices/evals/fixtures/mdc_traceid_required/fail_no_interceptor/AuthController.java` + `practices/evals/fixtures/cacheable_ttl/fail_no_ttl/Service.java` | Observability + cache templates anchored; 2 new rule fixtures fail with named errors; 19 existing guards still GREEN; self-reference for `traceid-in-error-response` closed | Pre-SP23: `templates/backend/observability/MdcCorrelationIdInterceptor.java` ENOENT; archunit fails `MDC_INTERCEPTOR_MISSING` | `bash practices/evals/fixtures/mdc_traceid_required/_run.sh pass && bash skills/ax-verify-java/scripts/run.sh templates/backend/observability/` | `template.evidence.coverage_ratio` (== 1.0); `archunit.violations` (== 0); `traceid.rule.protects_count` (> 0) |
| SP24 | `/ax-verify-java` + `/ax-verify-L2` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/integration/ templates/backend/export-import/` + `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/import-preview.tsx ...` | `practices/evals/fixtures/webhook_hmac/fail_no_signature_check/PaymentWebhookController.java` + `practices/evals/fixtures/chunked_import/fail_single_tx_10k/Service.java` | Webhook HMAC enforced; chunked-import enforced; circuit-breaker + retry templates anchor existing self-references | Pre-SP24: `templates/backend/integration/WebhookReceiver.java` ENOENT; archunit fails `WEBHOOK_RECEIVER_NOT_FOUND` | `bash practices/evals/fixtures/webhook_hmac/_run.sh pass` | `webhook.hmac.violations` (== 0); `circuit_breaker.open_count` exposed; `import.chunked.rowcount_p99` < cfg.cap |
| SP25 | `/ax-verify-java` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/data/ templates/backend/jobs/` | `practices/evals/fixtures/soft_delete_base/fail_soft_delete_no_base/Item.java` + RestAssured `JobDispatchIT.java` + `PageRequestCapIT.java` | Soft-delete only on base; JPA auditing populates fields; jobs DLQ on failure; paging cap enforced | Pre-SP25: `templates/backend/data/BaseEntityWithSoftDelete.java` ENOENT; archunit fails | `bash practices/evals/fixtures/soft_delete_base/_run.sh pass` | `jpa.audit.fields.populated_ratio` (== 1.0); `jobs.dlq.row_count` correlates with failures; `paging.max_limit_violations` (== 0) |
| SP26 | `/ax-verify-domain search` | `bash skills/ax-verify-domain/scripts/run.sh search` + `bash skills/ax-guard-trio-integrity/scripts/run.sh` | `practices/evals/fixtures/trio_integrity/fail_search_missing_frontend_spec/` + RestAssured `SearchFlowIT.java` + Playwright `typeahead-search-ime.spec.ts` | Atomic Spec Trio shipped; FTS adapter returns results; Korean IME no text corruption; chart blocks render | Pre-SP26: `specs/search-l0.yaml` ENOENT; trio_integrity_guard exits 1 with `MISSING_BACKEND_SPEC: search` | `bash skills/ax-verify-domain/scripts/run.sh search` | `search.query.latency_p99_ms` (< 200 on PostgresFtsAdapter); `ime.composition.corruption_count` (== 0); `trio.coverage_ratio.search` (== 1.0) |
| SP27 | `/ax-verify-java` + `/ax-verify-L2` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/realtime/` + `bash skills/ax-verify-L2/scripts/run.sh templates/L2/blocks/` | RestAssured `SseSubscribeIT.java` + Playwright `dirty-guard.spec.ts` + `auto-save-indicator.spec.ts` | SSE delivers 3 events in 5s; dirty-guard blocks navigation; forms RHF wiring complete | Pre-SP27: `templates/backend/realtime/SseEmitterConfig.java` ENOENT; SseSubscribeIT fails 404 | `npx playwright test templates/L2/_fixtures/dirty-guard.spec.ts` | `sse.active_connections` exposed; `sse.event_delivery_latency_ms_p99` < 100; `form.dirty_block.fired_count` correlates |
| SP28 | `/ax-verify-domain feature-flags` + `/ax-verify-L1` + `/ax-verify-L2` | `bash skills/ax-verify-domain/scripts/run.sh feature-flags` + `bash skills/ax-verify-L1/scripts/run.sh` + `bash skills/ax-verify-L2/scripts/run.sh` | `practices-react/evals/fixtures/no_hardcoded_i18n/fail_korean_literal/page.tsx` + `practices-react/evals/fixtures/feature_gate/fail_process_env_check/Component.tsx` + RestAssured `FeatureFlagAdminIT.java` | Hardcoded-string rule fails on Korean literal; feature-gate rule fails on `process.env` check; admin CRUD works | Pre-SP28: rules absent → fixture run reports `RULE_NOT_FOUND` | `bash practices-react/evals/fixtures/no_hardcoded_i18n/_run.sh pass && bash practices-react/evals/fixtures/feature_gate/_run.sh pass` | `i18n.hardcoded_string.violations` (== 0 in `templates/L4/new/**`); `feature_flag.cache.hit_ratio` exposed; `trio.coverage_ratio.feature-flags` (== 1.0) |
| SP29 | `/ax-policy-check --self-test`, `/ax-evidence-fetch --self-test`, `/ax-explain --self-test` | `bash skills/ax-policy-check/scripts/_self-test.sh` + 2 sibling self-tests | `skills/ax-policy-check/_tests/policy-check-cold.spec.sh` + sibling specs | 3 new Tier-1 skills self-test exit 0; `/ax-policy-check` verdict STOP on cross-L4-import attempt; false-positive rate <5% on 50-fixture eval set | Pre-SP29: `skills/ax-policy-check/SKILL.md` ENOENT; spec fails with `SKILL_NOT_FOUND` | `bash skills/_tests/run-all-self-tests.sh` | `policy_check.false_positive_rate` (< 0.05); `evidence_fetch.refresh.attempts_total` exposed; `explain.responses.cache_hit_ratio` |

---

## §7 Autonomous Execution Safety

> Inherits all PRD §6 + Catalog Extension §6 patterns. Below extends for the SP23–SP29 surface.

### §7.1 Rollback boundary per SP

| SP | Pre-start tag | Rollback boundary |
|---|---|---|
| SP23 | `git tag sp23-pre-start` | Atomic revert: `templates/backend/observability/**` + `templates/backend/cache/**` + 2 new rule files + 3 snapshots; restore `traceid-in-error-response.md` `protects_template_id` to SP21 value |
| SP24 | `git tag sp24-pre-start` | Atomic revert: `templates/backend/integration/**` + `templates/backend/export-import/**` + 3 new L2 blocks + 2 new rule files + 1 snapshot; restore self-references on http-* rules |
| SP25 | `git tag sp25-pre-start` | Atomic revert: `templates/backend/data/**` + `templates/backend/jobs/**` + 1 new rule file + 2 snapshots |
| SP26 | `git tag sp26-pre-start` | **Atomic revert**: ALL search domain deliverables (Spec Trio + backend + L2 + L3 + L4 + allowlist entry) + chart blocks + 3 snapshots |
| SP27 | `git tag sp27-pre-start` | Per-cluster revert: realtime OR forms can revert independently |
| SP28 | `git tag sp28-pre-start` | Per-cluster revert: i18n OR feature-flags can revert independently; if feature-flags atomic deliverables fail, atomic revert of that cluster |
| SP29 | `git tag sp29-pre-start` | Per-skill revert |

### §7.2 Shared-artifact ownership

| Artifact | Sole writer SP | Reader SPs | Stale-state rule |
|---|---|---|---|
| `templates/backend/observability/**` | SP23 | All downstream (SP24/25/26/27 emit metrics) | If SP23 amends post-merge, downstream re-run `/ax-verify-java` |
| `practices/rules/traceid-in-error-response.md` (`protects_template_id` field) | SP23 (updates this existing rule) | All | Append-only update; no other SP modifies this field |
| `skills/ax-verify-domain/scripts/domain-registry.yaml` | SP26 (search) + SP28 (feature-flags) each append one entry | SP29 reads | Append-only YAML; sorted-key insertion; rebase auto-resolves via `yq` (per Catalog Extension §6.2) |
| `practices/evals/trio_integrity_allowlist.yaml` | SP26 + SP28 (one entry each) | All | Same as above |
| `practices/rules/_MANIFEST.yaml` + `practices-react/rules/_MANIFEST.yaml` | Regenerated once at end of each SP that adds a rule | All | Regen is the LAST step of each SP commit |
| `practices/AGENTS.md` + `practices-react/AGENTS.md` sentinels | Regenerated atomically by the LAST SP in the cycle (SP29) | All | Per Catalog Extension §6.2 |
| `templates/AGENTS.md` sentinel | SP29 regenerates once at the end | All | Per Catalog Extension §6.2 |

### §7.3 Stale-state invalidation rule

Inherits Catalog Extension §6.3 verbatim. Additions:

- Re-run `bash skills/ax-verify-domain/scripts/run.sh <domain>` after any change to `templates/backend/<domain>/` OR allowlist entries.
- Re-run `bash skills/ax-policy-check/scripts/_self-test.sh` after any new rule lands (SP29 only, since it depends on the loaded rule index).

### §7.4 Halt thresholds

- **3-fail halt**: identical to PRD.
- **30-min idle halt**: identical.
- **5-rebase halt**: identical.
- **Atomic-SP partial-fail halt**: SP26 (search atomic) — any sub-component failure rolls back the SP entirely.
- **NEW — false-positive cascade halt:** SP29 `/ax-policy-check` self-test eval set requires false-positive rate <5%. If 50-fixture run exceeds 5% FP rate, SP29 halts immediately; rule-loader tuning required before re-attempt.

### §7.5 ESCAPE valve

Identical path: `docs/superpowers/escape/<SP_id>-<timestamp>.md`. No auto-resume; human approval required.

### §7.6 Cross-stack dependency declaration

Each SP that touches both Java and React (SP24, SP26, SP27, SP28, SP29) MUST emit `docs/superpowers/sp<NN>-cross-stack-deps.yaml`. Format identical to Catalog Extension §6.6.

---

## §8 Pre-mortem (DELIBERATE mode)

> ≥4 scenarios required. Each with: failure description, likelihood, detection, executable mitigation, threshold.

### Scenario 1 — Realtime SSE leaks server resources on Vercel/serverless

**Failure:** SP27 ships `SseEmitterConfig.java` and L4 `templates/L4/notification/` consumes it. Fork receiver deploys to Vercel; serverless function timeout (10s default, max 60s on Pro) kills long-lived SSE connections; client reconnects in a loop; bills explode; user-visible: bell stops updating.

**Likelihood:** High. Vercel + Next.js is the default React deployment path for Korean enterprise SaaS. Most fork receivers will hit this within their first deployment.

**Detection:**

- SP27 ships `templates/L4/notification/app/(notification)/README.md` with a `## Serverless Deployment Warning` section.
- New gate: `skills/ax-verify-L4/scripts/run.sh` checks for `templates/L4/<domain>/SERVERLESS.md` or README section "Serverless Deployment" when the L4 imports from `templates/backend/realtime/**`. If imports detected and warning absent → exit 1 with `SERVERLESS_WARNING_MISSING`.
- Backend: `RealtimeOutboxRelay.java` emits Micrometer counter `sse.connection.duration_seconds`; threshold alert at p99 > 8s suggests serverless timeout.

**Mitigation (executable):**

- **Owner:** SP27 lead.
- **Command:** `bash skills/ax-verify-L4/scripts/run.sh templates/L4/notification/`.
- **Threshold:** `SERVERLESS_WARNING_MISSING` exit 1 if L4 imports `templates/backend/realtime/**` without the warning section.
- **Recovery:** Add the warning + alternate path (WebSocket via `WebSocketConfig.java` OR polling fallback). SP27 ships both paths atomically.

### Scenario 2 — i18n L4 wiring fails Korean enterprise default (KRW + 한글 IME)

**Failure:** SP28 ships `currency-formatter.tsx` using `Intl.NumberFormat('ko-KR', {style: 'currency', currency: 'KRW'})`. Modern Node + browser produce `₩1,234` (no decimals), which is correct. But fork receiver's older Node 18 (Korean enterprise default until recently) produces `KRW 1,234.00` (decimals + locale code). User-visible: receipt PDFs show `KRW 1,234.00 원` (double currency, wrong format).

OR: i18n string in `templates/L4/auth/` translation file under-translates → admin sees raw key `auth.signup.submit` in UI.

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
- **Threshold:** ANY format mismatch on KRW / JPY / "방금 전" / "방금 후" → SP28 halts.
- **Recovery:** SP28 ships an internal helper `formatKrw()` in `currency-formatter.tsx` that overrides `Intl` output to the Korean enterprise canon (`₩` prefix, no decimals, no trailing space); + ISO 4217 evidence anchor cites `iso-4217.snapshot.md` (existing). Pseudo-Korean fallback in `LocaleProvider` for the relative-time edge cases.

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

### Scenario 4 — `/ax-policy-check` produces too many false positives → AI agents ignore it

**Failure:** SP29 ships `/ax-policy-check`. AI agents call it on every file write. The rule index over-matches: any L4 file write triggers `no-cross-l4-domain-imports` check → STOP verdict if ANY import string mentions another L4 dir (even within comments or unused imports). False positive rate spikes >20%. Within a week, AI agents (Claude Code, Codex CLI, etc.) learn to ignore the STOP verdict ("just keep going, the check is broken"). Skill loses signal value; rules-as-protection collapses.

**Likelihood:** Medium-High. Rule path-glob matching + intent regex is a notoriously hard precision problem; first iteration will have FP issues.

**Detection:**

- SP29 includes a **50-fixture eval set** under `skills/ax-policy-check/_eval/fixtures/`:
  - 25 fixtures expected `STOP` (true positives).
  - 15 fixtures expected `PROCEED` (clean).
  - 10 fixtures expected `WARN` (gray area).
- `skills/ax-policy-check/_eval/run.sh` runs all 50 and computes confusion matrix.
- Acceptance gate: false-positive rate (FP / (FP + TN)) < 5%.
- Observability: emit Micrometer counter `policy_check.verdict_total{verdict=STOP|PROCEED|WARN}`; downstream consumers (i.e., fork receivers running the skill in CI) can dashboard their own FP rate.

**Mitigation (executable):**

- **Owner:** SP29 lead.
- **Command:** `bash skills/ax-policy-check/_eval/run.sh && bash skills/ax-policy-check/_eval/_assert-fp-rate.sh`.
- **Threshold:** FP rate ≥ 5% → SP29 halts; rule-loader tuning required (e.g., add `intent_requires_keyword` field per rule).
- **Recovery:** SP29 lead inspects FP fixtures; tunes rule index logic; re-runs. If FP rate cannot be brought below 5% within 3 iterations, fallback: ship `/ax-policy-check` as `WARN`-only (no `STOP`) — skill is purely advisory. ADR amendment documents.

### Scenario 5 (bonus) — SP24/25/26 parallel race on `domain-registry.yaml`

**Failure:** SP26 (adds `search`) and SP28 (adds `feature-flags`) both append to `skills/ax-verify-domain/scripts/domain-registry.yaml`. If executed in parallel via `/team`, lost-write race.

**Likelihood:** Low (Catalog Extension already mitigated via sorted-key insertion + `yq`).

**Detection + Mitigation:** Inherits Catalog Extension §6.2 — `yq`-based sort + rebase-on-conflict + serial merge-order enforced by SP_id. No new mitigation needed; documenting for completeness.

---

## §9 ADR Template + provenance_class

Every new TD-ADR appended to `templates/DECISIONS.md` declares `provenance_class` per PRD §4.12.

### ADR registry (TD-2026-05-18-NNN)

| ADR id | Title | `provenance_class` | Rationale (1-line) |
|---|---|---|---|
| TD-2026-05-18-023 | Observability + Cache baseline (F1 + F10) | `internal_design` | Anchored to OTLP / Micrometer / Caffeine canonical configs; topology is internal |
| TD-2026-05-18-024 | External integration + Export/Import (F2 + F12) | `internal_design` | Resilience4j + Apache POI canonical; webhook HMAC + chunked-import policies are internal design |
| TD-2026-05-18-025 | Data layer + Background jobs (F3 + F4) | `internal_design` | Flyway + JPA + Redis Streams canonical; soft-delete + on-demand job wiring internal |
| TD-2026-05-18-026 | Charts L2 + Search atomic domain (F5 + F6) | `external_canonical` | recharts (Apache-2.0) + PostgreSQL FTS + Meilisearch are external canon; Korean tokenization 70% baseline is internal decision |
| TD-2026-05-18-027 | Realtime SSE + Form orchestration (F7 + F11) | `internal_design` | SSE-via-DB-outbox is a composition pattern; serverless warning is internal design |
| TD-2026-05-18-028 | i18n + Feature flags (F8 + F9) | `internal_design` + `locked_constraint` | KRW format per ISO 4217 + 개인정보보호법 considerations on flag-storage |
| TD-2026-05-18-029 | 3 new Tier-1 skills (F13 + F14 + F15) | `internal_design` | Tier-1 cap explicit exception (4 → 7 Tier-1); documented with fallback to `/ax-verify --subcommand` pattern if Architect rejects |

### Summary ADR template (applied at SP29 commit)

```yaml
---
adr_id: TD-2026-05-18-030
title: Functional Capability Extension (SP23–SP29, F1–F15)
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-18-functional-extension-prd.md
  rationale: |
    This ADR captures the F1–F15 functional capability closure cycle.
    All gaps named by user-identified review against the v1.0.0-catalog-complete
    catalog state. Empirical anchor: each gap cites either (a) a broken L4 flow
    OR (b) an existing rule that protects nothing today.
spec_ref: METHODOLOGY.md#A.4
---

## Decision
Extend the ax-template catalog with ~85 atoms across 9 functional surfaces
(observability, cache, integration, export-import, data, jobs, charts, search,
realtime, forms, i18n, feature-flags) + 3 new Tier-1 skills, shipped across
7 SPs (SP23–SP29).

## Drivers
(1) Self-reference closure (rules that protect nothing today).
(2) Atomic Spec-Trio ordering for search + feature-flags.
(3) Pre-execution policy check (`/ax-policy-check` closes first-turn gap).

## Alternatives considered
A. Single mega-SP — rejected (blast radius).
B. SP-per-functional-area-group — CHOSEN.
C. Atomic per-gap split (15 SPs) — rejected (overhead).
D. Defer F13–F15 — rejected (loses 6 SPs of catalog feedback for skills).

## Why chosen
Option B groups disjoint surfaces, honors atomic-ordering for F6 (search) and
F9 (feature-flags), and lets SP24/25/26 run in parallel after SP23 — wall-time
≈ 10–11 d.

## Consequences
- 56 new backend templates across 9 folders.
- 4 new L1 + 26 new L2 + 1 new L3 + 2 new L4 domains.
- 7 new rules (5 Java + 2 React), each anchored to protects_template_id +
  failing_fixture_path.
- 3 new Tier-1 skills (4 → 7 Tier-1; explicit exception with fallback path).
- 2 new Spec Trios (search full_trio, feature-flags full_trio).
- 12 new upstream snapshots.
- 4 existing rules with `protects_template_id` updated to close self-references
  (mdc + 3× http-* rules).

## Follow-ups
- Existing `templates/L4/auth/` + `templates/L4/payment/` Korean string
  migration to `t()` (side-fix follow-up).
- 180-d refresh of new upstream snapshots.
- L4 sealed sub-agent re-run on `search` + `feature-flags` domains.
```

---

## §10 Open Questions (1–3 max; Critic to triage)

> Persisted to `.omc/plans/open-questions.md` per Planner Open Questions protocol.

1. **Tier-1 skill cap exception.** Catalog Extension already exceeded the PRD §3.1 cap of 3 Tier-1 (`/ax-fork-receiver` was the 4th). SP29 adds 3 more (4 → 7). Architect must rule: is the cap a heuristic to be re-baselined, or should F13/14/15 collapse into `/ax-verify --subcommand` patterns? Default: ship as 3 new Tier-1 with ADR exception. Fallback path documented per SP29 risk.
2. **Existing L4 Korean string migration scope.** SP28 ships `no-hardcoded-user-facing-string-in-l4` with `applies_to: templates/L4/**/*.tsx (new files only)`. Existing `templates/L4/auth/` and `templates/L4/payment/` ship hardcoded Korean. Should SP28 also migrate those (large diff, scope creep risk), or defer to a follow-up side-fix SP after SP29? Default: defer.
3. **Search domain default backend choice.** SP26 ships `PostgresFtsAdapter` as default and `MeilisearchAdapter` as opt-in. Should the default backend choice be driven by a fork-receiver questionnaire at `/ax-fork-receiver` install time, or by a static blueprint field that the fork receiver edits before install? Default: static blueprint field (`ax.search.backend: postgres-fts`); questionnaire is P1 follow-up.

---

## §11 Honored Constraints (cross-check vs CLAUDE.md + prior PRDs)

| Constraint | How this PRD honors it |
|---|---|
| **CLAUDE.md: Composition kit, not single product** | Every functional gap closure is independently fork-adoptable. `/ax-policy-check` works against any catalog subset. |
| **CLAUDE.md: React + Spring Boot 둘 다 active equal partner** | Both stacks grow significantly: 56 backend templates + 30 frontend (L1+L2+L3+L4) templates. 5 Java rules + 2 React rules. Search atomic domain ships both halves. |
| **CLAUDE.md: 거버넌스 무한루프 금지** | No promotion-gate docs. Every SP terminates on binary `exit 0`. No `evidence bundle` or `curated promotion check` artifacts. |
| **CLAUDE.md: 새 도메인 추가는 정상 활동** | 2 new full_trio domains (search, feature-flags) celebrated, not gated. |
| **CLAUDE.md: Fork받은 팀의 정책을 skill이 강제 금지** | `/ax-policy-check` is pre-mutation advice for AI agents, not a merge-gate. `/ax-evidence-fetch` doesn't enforce refresh; advises. `/ax-explain` is explanatory. None enforce team policy. |
| **PRD-1 §3.1 evidence frontmatter required** | Every new template ships `evidence:` or `@ax-template-meta`. |
| **PRD-1 §3.2 No new top-level Tier-1 beyond {ax-transform, ax-verify, ax-scaffold}** | **VIOLATED** by SP29 (+3 Tier-1 = 4 → 7 total). Explicit ADR (TD-2026-05-18-029) + open question for Architect; fallback path documented. |
| **PRD-1 §3.2 No MockMvc** | All new tests use RestAssured (Java) + Playwright (TS) + Vitest (TS unit). |
| **Catalog Extension Critic anti-bloat: P0 rule names protects_template_id + failing_fixture_path** | All 7 new rules anchor both; §5.6 table is canonical. |
| **Catalog Extension Critic atomic Spec-Trio ordering** | SP26 (search) and SP28 (feature-flags) ship Spec Trio + backend + frontend in ONE atomic SP each. |
| **Composition-kit user-facing surface = skills, not raw gradle/npm** | `/ax-policy-check`, `/ax-evidence-fetch`, `/ax-explain` are skill-orchestrated. `/ax-verify-*` and `/ax-verify-domain` are wrappers. |
| **Per Catalog Extension §10: hyphenated domain support in run-gradle.sh** | `search`, `feature-flags` use hyphens; verified compatible (SP16 side-fix). |

---

## §12 Out-of-Scope (explicit)

Per user directive — these are NOT in this PRD and MUST NOT be added by Architect/Critic:

- **Public release prep:** LICENSE / CONTRIBUTING.md / docs site / GitHub Pages.
- **CI/CD:** GitHub Actions workflows / release.yml / Dependabot config / catalog integrity check workflow.
- **Release/deployment tooling:** no `.github/workflows/*.yml` files added.
- **v2 architectural changes:** no skill topology overhaul beyond +3 Tier-1; no Spec Trio schema changes; no `ax-template-meta` shape changes.
- **Existing L4 string migration:** `templates/L4/auth/` and `templates/L4/payment/` Korean strings remain hardcoded until a side-fix follow-up SP (out of scope for this PRD).
- **P1 backend domain extractions:** existing `backend/src/main/java/.../auth|crud|payment|...` reorganization deferred.
- **P1 rules from Catalog Extension §12** remain P1.

---

## §13 End of PRD

**Ready for Architect + Critic round.**

**Next step:** Architect review (lock execution plan: dependency graph, shared-artifact ownership, Tier-1 cap exception, atomic-rollback for SP26 search domain, serverless warning gate for SP27, false-positive eval set for SP29). Then Codex Critic re-judge (verify atomic-ordering, anti-bloat fixture requirement, self-reference closures, no-MockMvc, no-OOS-creep).

On joint APPROVE: commit canonical PRD as TD-2026-05-18-030 ADR, hand off SP23 → SP24‖SP25‖SP26 → SP27 → SP28 → SP29 to `/team`.
