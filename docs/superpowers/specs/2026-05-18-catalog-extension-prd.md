# Catalog Extension PRD (post-rescope) — 2026-05-18

> **Status:** Planner draft post-Codex-Critic rescope (`2026-05-18-component-catalog-completeness-audit-critic.md`). Ready for Architect + Critic round.
> **Scope:** ONLY the P0 items that survive the rescope. P1/P2 are explicitly out of scope and tracked as follow-up only.
> **Predecessor:** `2026-05-17-frontend-templatization-prd.md` (canonical, APPROVED). All SP1–SP12 are CLOSED.
> **Audit input:** `2026-05-18-component-catalog-completeness-audit.md` (per-row tables canonical).
> **Critic verdict applied:** APPROVE WITH RESCOPE — every mandate from the critic file is honored below (see §11 traceability).
> **Format:** RALPLAN-DR. Korean enterprise = composition-kit. React + Spring equal partners.

---

## §1 RALPLAN-DR Summary

### Principles (5) — inherited from PRD §RALPLAN-DR, not re-litigated

1. **Composition kit, not single product.** Every new artifact must be fork-adoptable in isolation.
2. **Spec-before-code, evidence-anchored.** Every new template/rule carries `evidence:` frontmatter; every new ADR declares `provenance_class`.
3. **Few exposed surfaces, dense feedback loops underneath.** No new Tier-1/Tier-2 skills beyond `/ax-fork-receiver` (Tier-1) and `/ax-verify-domain <new-domain>` invocations (existing Tier-2). No new Tier-3 guards.
4. **React + Spring equal partners.** Both stacks grow in this cycle.
5. **Binary verification per axis.** Every SP terminates when its named `/ax-verify-*` skill returns exit 0; no SP "done" on prose alone.

### Decision Drivers (top 3)

1. **Atomic Spec Trio ordering.** Critic hard rule: Spec Trio MUST land BEFORE or ATOMICALLY WITH the backend/L4 domain it governs. SP grouping is dictated by this constraint.
2. **Anti-bloat at P0.** Each P0 rule must name (i) the P0 template/L4 flow it protects AND (ii) the failing fixture that proves its necessity. Rules without both fields drop to P1 (already done — see §2).
3. **Composition-kit reach × frequency-of-use.** P0 priority = "every fork forks this on day 1 OR a known L4 flow breaks today without it."

### Options Considered

- **Option A — Ship all P0 in one mega-SP (SP13-only).** Pros: single rollback boundary. Cons: 68 P0 items at one tag = unmanageable blast radius; violates §6.4 rollback boundary spec; defeats `/team` parallelism. **Rejected.**
- **Option B — Split by surface (L1 SP / L2 SP / L3 SP / Backend SP / Spec SP / Rule SP / Skill SP).** Pros: small blast radius per SP. Cons: violates Critic's atomic Spec-Trio-before-backend ordering rule — backend domain skeletons would land before their Spec Trios. **Rejected.**
- **Option C — Split by reference workload (notification end-to-end first, then audit-log, then file-storage, etc.).** Pros: each backend domain bundles Spec Trio + skeleton + L2 + L3 + verify atomically; Critic ordering rule satisfied; matches CLAUDE.md "vertical-slice value" framing. Cons: cross-cutting infrastructure (backend baseline, L1 primitives, L2 non-domain blocks) must land first as foundation SPs. **CHOSEN.**
- **Option D — Ship governance rule (`no-RRN-logging`) first as standalone SP before any feature SP.** Pros: surfaces the locked PIPA constraint early. Cons: a single-rule SP burns coordination overhead; the rule has a clearer home in the rule-cluster SP. **Rejected; rolled into SP21.**

### Recommended: **Option C — Hybrid Foundation → Per-Domain Atomic**

Cycle = **2 foundation SPs (SP13 backend baseline, SP14 L1 primitives)** → **1 cross-cutting L2 SP (SP15)** → **4 atomic domain SPs (SP16 notification, SP17 audit-log, SP18 file-storage, SP19 email-outbox+scheduled-task)** → **1 page-cluster SP (SP20 wizard/settings/forgot-password)** → **1 rule-cluster SP (SP21)** → **1 skill SP (SP22 /ax-fork-receiver)**. Total: **10 SPs**.

---

## §2 Updated P0 Inventory (post-rescope, count-canonical)

> **Canonicalization:** All counts below are the per-row reconciled totals from audit §"Master recommendation" (the audit's own reconciliation note flagged 71 vs 75 inconsistency — per-row is authoritative). Then the Critic demotions are applied.
> **Pre-rescope P0 = 75** (per-row, post-audit-self-correction).
> **Post-rescope P0 = 68** (75 − 11 demotions + 4 promotions: 1 governance rule + 4 Spec Trios promoted P1/P2→P0 to satisfy atomic-ordering rule).

### Demotions applied (Critic mandates)

| Demoted item | Surface | New tier | Reason |
|---|---|---|---|
| `kbd.tsx` | L1 | P1 | No P0 user-visible flow breaks |
| `empty-data-page` | L3 | P1 | Existing `templates/L2/blocks/empty-state.tsx` covers the case |
| `search-results-page` | L3 | P1 | Depends on P1 search backend |
| `/ax-add-rule` | Skill | P1 | Authoring accelerator only |
| `/ax-add-template` | Skill | P1 | Authoring accelerator only |
| `/ax-doctor` | Skill | P1 | Critic does not list as P0 (only `/ax-fork-receiver` is closer to P0) |
| `prefer-react-19-use-over-useEffect-fetching` | React rule | P1 | Framework preference, not L4 breakage |
| `prefer-server-action-over-fetch-mutation` | React rule | P1 | Framework preference, not L4 breakage |
| `webhook-signature-verify` | Java rule | P1 | `integration-webhook` is P1 backend |
| `auditing-jpa-listener` | Java rule | P1 | `AuditingConfig` is P1 |
| `rrn-input-masked-by-default` | React rule | P1 | RRN input component is P1 (specialty, not universal) |
| `business-registration-input` | L1 | P1 | Regulated specialty; not universal SaaS catalog |
| RRN-masked-input / CI-DI 본인인증 | L1 | P1 | Same as above |

### Promotions applied (atomic-ordering rule)

| Promoted item | Surface | Old tier | New tier | Reason |
|---|---|---|---|---|
| `no-RRN-logging` (governance) | Java + React rule (shipped in both directories) | NEW | P0 | Critic mandate. Locked constraint: 개인정보보호법 §24 |
| `combobox-respects-hangul-ime-composition` | React rule | NEW | P0 | Critic: 한글 IME = behavior coverage tied to combobox/search-palette/typeahead/filter inputs |
| Spec Trio `audit-log` (full_trio) | Spec | P1 | P0 | Atomic-ordering rule: backend audit-log is P0 |
| Spec Trio `file-storage` (full_trio) | Spec | P1 | P0 | Atomic-ordering rule: backend file-storage is P0 |
| Spec Trio `email-outbox` (backend_only) | Spec | P2 | P0 | Atomic-ordering rule: backend email-outbox is P0 |
| Spec Trio `scheduled-task` (backend_only) | Spec | P2 | P0 | Atomic-ordering rule: backend scheduled-task is P0 |

### Canonical P0 inventory (68 items)

| # | Item | Surface | Justification (P0 template/L4 flow + failing fixture) | Effort | Owning verify skill | Depends-on |
|---|---|---|---|---|---|---|
| 1 | `templates/backend/controllers/BaseController.java` | Backend cross-cutting | Powers every backend domain; PRD §4.5 budgeted, 0 on disk. Fixture: `practices/evals/fixtures/base_controller/fail_missing/` (greps for `@RestController` annotation absence). | S | `/ax-verify-java` | — |
| 2 | `templates/backend/services/BaseService.java` | Backend cross-cutting | Anchors `core-constructor-injection.md`. Fixture: archunit test fails if any new service skips ctor injection. | S | `/ax-verify-java` | #1 |
| 3 | `templates/backend/repositories/BaseRepository.java` | Backend cross-cutting | Anchors `testing-archunit-repository-shape.md`. Fixture: archunit fail. | S | `/ax-verify-java` | #1 |
| 4 | `templates/backend/dto/RequestDto.java` + `ResponseDto.java` (records) | Backend cross-cutting | Anchors `lang-records-for-dtos.md`. Fixture: `fail_dto_class_instead_of_record/`. | S | `/ax-verify-java` | #1 |
| 5 | `templates/backend/dto/PageResponse.java` | Backend cross-cutting | Anchors `api-pagination-pageable.md`. Fixture: page envelope shape check. | S | `/ax-verify-java` | #1 |
| 6 | `templates/backend/error/GlobalExceptionHandler.java` | Backend cross-cutting | Anchors `error-controller-advice.md` + `error-rfc7807-problem-detail.md`. Fixture: RestAssured asserts JSON ProblemDetail on 4xx/5xx. | M | `/ax-verify-java` | #1 |
| 7 | `templates/backend/error/ProblemDetailFactory.java` | Backend cross-cutting | Same as #6 + traceId injection. Fixture: assert `properties.traceId` present. | S | `/ax-verify-java` | #6 |
| 8 | `templates/backend/security/SecurityConfigBase.java` | Backend cross-cutting | Anchors `security-stateless-session-policy.md`. Fixture: RestAssured asserts no JSESSIONID cookie. | M | `/ax-verify-java` | #1 |
| 9 | `templates/backend/security/JwtAuthenticationFilter.java` | Backend cross-cutting | Anchors auth-asvs-l1 items. Fixture: invalid JWT → 401 with ProblemDetail. | M | `/ax-verify-java` | #8 |
| 10 | `templates/backend/config/OpenApiConfig.java` | Backend cross-cutting | Anchors `web-explicit-produces.md`. Fixture: `/v3/api-docs` returns valid schema. | S | `/ax-verify-java` | #1 |
| 11 | `templates/L1/components/combobox.tsx` | L1 | Powers L2 `filter-bar`, `column-picker`, `search-palette`, future `address-search`. Fixture: Vitest snapshot fails because `filter-bar.tsx` currently rolls its own typeahead. | S | `/ax-verify-L1` | — |
| 12 | `templates/L1/components/date-picker.tsx` | L1 | Powers payment refund-window, audit-log filters. Fixture: Playwright `date-picker.spec.ts` fails (component absent). | S | `/ax-verify-L1` | #13 |
| 13 | `templates/L1/components/calendar.tsx` | L1 | Foundation for #12. Fixture: render test fails ENOENT. | S | `/ax-verify-L1` | — |
| 14 | `templates/L1/components/date-range-picker.tsx` | L1 | Powers audit-log L4 filter toolbar. Fixture: Playwright fails ENOENT. | S | `/ax-verify-L1` | #12 |
| 15 | `templates/L1/components/file-dropzone.tsx` | L1 | Powers L2 `file-upload-area` (P0). Fixture: Vitest fails because `file-upload-area.tsx` cannot resolve import. | M | `/ax-verify-L1` | — |
| 16 | `templates/L1/components/otp-input.tsx` | L1 | Auth domain currently cannot render MFA verify step. Fixture: Playwright `frontend/tests/auth/mfa-verify.spec.ts` fails. | S | `/ax-verify-L1` | — |
| 17 | `templates/L1/components/address-search.tsx` (도로명/지번) | L1 | Korean enterprise hard requirement; no shadcn equivalent. Fixture: Playwright `address-search.spec.ts` opens Daum/Kakao 우편번호 widget and asserts selected address resolves to controlled prop. | M | `/ax-verify-L1` | #11 |
| 18 | `templates/L2/blocks/form-section.tsx` | L2 | DRY violation across 4 forms today. Fixture: ESLint custom rule fires on duplicated fieldset/heading pattern. | S | `/ax-verify-L2` | — |
| 19 | `templates/L2/blocks/field-array.tsx` | L2 | Powers payment line-items, audit-log multi-target. Fixture: Vitest fails — useFieldArray wrapper absent. | M | `/ax-verify-L2` | — |
| 20 | `templates/L2/blocks/conditional-field.tsx` | L2 | DRY: re-implemented in 3 forms. Fixture: RHF-driven show/hide test fails. | S | `/ax-verify-L2` | — |
| 21 | `templates/L2/blocks/form-error-summary.tsx` | L2 | WCAG 3.3.1. Fixture: axe-core asserts `aria-live` summary region present on submit-fail. | S | `/ax-verify-L2` | — |
| 22 | `templates/L2/blocks/virtualized-table.tsx` | L2 | Current `data-table.tsx` breaks at >2k rows; audit-log L4 (SP17) hits this immediately. Fixture: render 5000 rows, assert DOM node count < 100. | M | `/ax-verify-L2` | — |
| 23 | `templates/L2/blocks/expandable-row.tsx` | L2 | Required by payment-event-ledger (events nested under payment). Fixture: Playwright assert nested detail panel opens. | S | `/ax-verify-L2` | #22 |
| 24 | `templates/L2/blocks/advanced-filter-builder.tsx` | L2 | Required by audit-log L4 search. Current `filter-bar.tsx` shallow. Fixture: AND/OR rule serialization round-trips. | L | `/ax-verify-L2` | #11 |
| 25 | `templates/L2/blocks/filter-chips.tsx` | L2 | Visible feedback for any filter UX. Fixture: chip-remove click clears RHF state. | S | `/ax-verify-L2` | — |
| 26 | `templates/L2/blocks/error-boundary.tsx` | L2 | Currently uncaught render error crashes the route (L3 error-page exists but is route-level only). Fixture: Playwright throws inside child → boundary catches + telemetry callback fires. | S | `/ax-verify-L2` | — |
| 27 | `templates/L2/blocks/offline-banner.tsx` | L2 | Universal; `navigator.onLine === false` case unhandled today. Fixture: Playwright simulates offline → banner appears. | S | `/ax-verify-L2` | — |
| 28 | `templates/L2/blocks/search-palette.tsx` (Cmd+K) | L2 | Universal pattern. Fixture: `⌘K` keypress opens palette; `Escape` closes. | M | `/ax-verify-L2` | #11 |
| 29 | `templates/L2/blocks/skip-link.tsx` | L2 | WCAG 2.4.1 Bypass Blocks. Fixture: axe-core asserts skip link first focusable. | XS | `/ax-verify-L2` | — |
| 30 | `templates/L2/blocks/announce-live.tsx` | L2 | ARIA-live region for async toast/snackbar. Fixture: assertion fires on toast-queue emit. | S | `/ax-verify-L2` | — |
| 31 | `templates/L2/blocks/toast-queue.tsx` | L2 | Current `toast.tsx` is single-instance; queue absent. Fixture: 3 rapid emits → all 3 visible in stack. | S | `/ax-verify-L2` | #30 |
| 32 | Spec Trio `notification` (full_trio) | Spec | Atomic with #33–#36. Fixture: `trio_integrity_guard.sh` fails when spec absent. | S | `/ax-guard-trio-integrity` | — |
| 33 | `templates/backend/notification/` skeleton | Backend domain | No notification backend today; every fork emits notifications. Fixture: RestAssured POST `/api/v1/notifications/test` → 202; outbox row present. | L | `/ax-verify-domain notification` | #1–#10, #32 |
| 34 | `templates/L2/blocks/notification-bell.tsx` | L2 (domain-paired) | Universal. Atomic with #33. Fixture: unread badge updates on SSE/poll. | S | `/ax-verify-L2` + `/ax-verify-domain notification` | #33 |
| 35 | `templates/L2/blocks/notification-list.tsx` | L2 (domain-paired) | Same. Fixture: dropdown renders ≥1 mock notification, mark-read mutation fires. | M | `/ax-verify-L2` + `/ax-verify-domain notification` | #33, #34 |
| 36 | `/ax-verify-domain notification` script update | Skill (existing Tier-2) | Update existing Tier-2 to recognize notification domain. Fixture: skill invocation on bare repo fails with `DOMAIN_NOT_REGISTERED`. | S | `/ax-verify` | #33 |
| 37 | Spec Trio `audit-log` (full_trio) | Spec | Atomic with #38–#41. Fixture: trio_integrity_guard fails. | S | `/ax-guard-trio-integrity` | — |
| 38 | `templates/backend/audit-log/` skeleton | Backend domain | PIPA/SOX requirement; no backend domain today. Fixture: RestAssured: POST any mutation → audit row appended; @Immutable annotation present. | L | `/ax-verify-domain audit-log` | #1–#10, #37, #51 |
| 39 | `templates/L2/blocks/audit-log-view.tsx` | L2 (domain-paired) | Powers L3 audit-log-page. Fixture: row renderer displays actor/action/target/diff. | M | `/ax-verify-L2` + `/ax-verify-domain audit-log` | #38, #22 |
| 40 | `templates/L3/pages/audit-log-page/` | L3 (domain-paired) | Audit-log viewer page. Fixture: route resolves; toolbar + virtualized-table + detail-drawer slot contract honored. | M | `/ax-verify-L3` + `/ax-verify-domain audit-log` | #38, #39 |
| 41 | `/ax-verify-domain audit-log` script update | Skill (existing Tier-2) | Same as #36. | S | `/ax-verify` | #38 |
| 42 | Spec Trio `file-storage` (full_trio) | Spec | Atomic with #43–#45. Fixture: trio_integrity_guard fails. | S | `/ax-guard-trio-integrity` | — |
| 43 | `templates/backend/file-storage/` skeleton | Backend domain | Universal SaaS; presigned-URL upload, virus-scan hook. Fixture: RestAssured POST `/api/v1/files/presign` returns signed URL; upload via signed URL passes virus-scan stub. | L | `/ax-verify-domain file-storage` | #1–#10, #42, #55 |
| 44 | `templates/L2/blocks/file-upload-area.tsx` | L2 (domain-paired) | Powers KYC/attachments. Composes #15. Fixture: drop file → progress → presigned PUT mock fires. | M | `/ax-verify-L2` + `/ax-verify-domain file-storage` | #43, #15 |
| 45 | `/ax-verify-domain file-storage` script update | Skill (existing Tier-2) | Same as #36. | S | `/ax-verify` | #43 |
| 46 | Spec Trio `email-outbox` (backend_only) | Spec | Atomic with #47. Fixture: trio_integrity_guard --mode backend_only fails. | S | `/ax-guard-trio-integrity` | — |
| 47 | `templates/backend/email-outbox/` skeleton | Backend domain | Auth domain currently does direct sends (fragile under TX rollback). Fixture: Spring `@Transactional` test forces rollback → outbox row absent (no orphan email). | M | `/ax-verify-domain email-outbox` | #1–#10, #46, #52 |
| 48 | Spec Trio `scheduled-task` (backend_only) | Spec | Atomic with #49. Fixture: trio_integrity_guard --mode backend_only fails. | S | `/ax-guard-trio-integrity` | — |
| 49 | `templates/backend/scheduled-task/` skeleton | Backend domain | `@Scheduled` idempotency + jitter + MDC pattern. Fixture: 2 concurrent triggers with same key → only 1 executes (idempotency); jitter ≥ N ms. | M | `/ax-verify-domain scheduled-task` | #1–#10, #48, #56 |
| 50 | Spec Trio `settings` (frontend_only) | Spec | Atomic with #57. Fixture: trio_integrity_guard --mode frontend_only fails when `static_source_ref` absent. | S | `/ax-guard-trio-integrity` | — |
| 51 | Java rule `audit-event-immutability` | Rule | Protects audit-log (#38). Failing fixture: `practices/evals/fixtures/audit_event_immutability/fail_mutable_setter/AuditEvent.java` declares setter → archunit fails. | S | `/ax-verify-java` | — |
| 52 | Java rule `outbox-transactional-publish` | Rule | Protects email-outbox (#47) + notification (#33). Failing fixture: `fail_publish_outside_transaction/` calls `publish()` outside `@Transactional` → archunit fails. | S | `/ax-verify-java` | — |
| 53 | Java rule `idempotency-key-on-mutations` | Rule | Generalizes `api-idempotency-key-required.md` (currently payment-only) to notification + email-outbox + audit-log mutations. Failing fixture: `fail_post_without_idempotency_header/` POST without `Idempotency-Key` → 400. | S | `/ax-verify-java` | — |
| 54 | Java rule `traceId-in-error-response` | Rule | Protects #7 ProblemDetailFactory. Failing fixture: `fail_missing_traceId/` ProblemDetail JSON without `properties.traceId` → assertion fails. | S | `/ax-verify-java` | — |
| 55 | Java rule `presigned-url-no-direct-upload` | Rule | Protects file-storage (#43). Failing fixture: `fail_multipart_upload_endpoint/` controller accepts `MultipartFile` → archunit forbids. | S | `/ax-verify-java` | — |
| 56 | Java rule `scheduled-task-jitter` | Rule | Protects scheduled-task (#49). Failing fixture: `fail_no_jitter/` `@Scheduled(fixedRate=1000)` without jitter → static check fails. | S | `/ax-verify-java` | — |
| 57 | Java rule `mdc-trace-on-async` | Rule | Protects #9 + notification dispatcher. Failing fixture: `fail_async_without_mdc/` `@Async` method without MDC propagation wrapper → archunit fails. | S | `/ax-verify-java` | — |
| 58 | Java + React rule `no-RRN-logging` (governance, cross-stack) | Rule | Locked constraint: 개인정보보호법 §24. Ships as `practices/rules/no-rrn-logging.md` + `practices-react/rules/no-rrn-shape-in-client-logs.md` (companion). Failing fixture: Java side `fail_logger_rrn_pattern/Logger.info("user " + rrn)` triggers regex `\d{6}-\d{7}`; React side `fail_console_log_rrn/console.log({rrn: "850101-1234567"})` triggers same. | M | `/ax-verify-java` + `/ax-verify-react` | — |
| 59 | React rule `no-cross-l4-domain-imports` | Rule | Hard layer rule. Failing fixture: `fail_payment_imports_audit_log/` `L4/payment/...` imports from `L4/audit-log/` → ESLint fails. | S | `/ax-verify-react` | — |
| 60 | React rule `no-server-component-state-leakage-to-client` | Rule | Protects #34 notification-bell. Failing fixture: `fail_non_serializable_prop/` Server Component passes `Date` instance to `'use client'` child → ESLint fails. | S | `/ax-verify-react` | — |
| 61 | React rule `traceId-rendered-on-error-boundary` | Rule | Protects #26 error-boundary. Pairs with Java #54. Failing fixture: `fail_boundary_no_traceId/` boundary fallback omits traceId → Vitest snapshot fails. | S | `/ax-verify-react` | — |
| 62 | React rule `aria-live-on-toast-queue` | Rule | Protects #31 toast-queue + #30 announce-live. Failing fixture: `fail_toast_without_aria_live/` toast emits without `aria-live="polite"` → axe-core fails. | S | `/ax-verify-react` | — |
| 63 | React rule `skip-link-required-on-app-shell` | Rule | Protects #29 skip-link. Failing fixture: `fail_app_shell_no_skip_link/` app-shell renders without skip-link → axe-core fails. | S | `/ax-verify-react` | — |
| 64 | React rule `optimistic-update-rollback-required` | Rule | Pairs with CLAUDE.md `web/patterns.md`. Failing fixture: `fail_optimistic_no_rollback/` mutation handler optimistic-updates without `onError` rollback → ESLint fails. | S | `/ax-verify-react` | — |
| 65 | React rule `combobox-respects-hangul-ime-composition` | Rule | Critic mandate: 한글 IME behavior coverage. Protects #11 combobox + #28 search-palette + future typeahead. Failing fixture: `fail_debounce_on_compositionstart/` combobox debounces on every `change` (not waiting for `compositionend`) → Playwright IME simulation asserts text corruption. | M | `/ax-verify-react` | — |
| 66 | React rule (extend) `l2-prefer-data-prop-over-direct-fetch` extension | Rule | Reclassified `extend_existing` (already exists at `practices-react/rules/l2-prefer-data-prop-over-direct-fetch.md`). Add codemod-style fixture. Failing fixture: `fail_l2_direct_fetch/` L2 block calls `fetch()` directly → existing ESLint rule fires; new fixture validates the codemod proposal. | S | `/ax-verify-react` | — |
| 67 | `templates/L3/pages/wizard-page/` | L3 | Powers onboarding/KYC/multi-step forms. No Spec Trio coupling. Fixture: 3-step wizard route resolves; next/back navigation preserves form state. | M | `/ax-verify-L3` | — |
| 68 | `templates/L3/pages/settings-page/` | L3 (Spec-Trio-coupled with #50) | Atomic with #50. Fixture: route resolves; settings-nav + settings-content slot contract honored. | M | `/ax-verify-L3` + `/ax-guard-trio-integrity` | #50 |
| 69 | `templates/L3/pages/forgot-password-page/` | L3 | Auth-extras; auth domain cannot ship password reset without it. Depends on #47 email-outbox. Fixture: route resolves; form POST triggers outbox row. | S | `/ax-verify-L3` + `/ax-verify-domain email-outbox` | #47 |
| 70 | Skill `/ax-fork-receiver` (Tier-1 NEW) | Skill | Closes PRD SP5.5 generalization. Bundles `templates/` + verify scripts as tarball + `install.sh` for downstream fork consumption. Failing fixture: cold-tarball-install in tmp dir → `/ax-verify` exits 0 within 300s. | M | `/ax-verify` | All prior P0 |

> **Note on counting:** Item rows numbered 1–70 above. Items #36, #41, #45 are skill-config updates folded into existing Tier-2 `/ax-verify-domain`; the audit treats them as part of the atomic domain SP rather than standalone P0 catalog atoms. **Headline P0 count = 68 catalog atoms** (= 70 rows − 2 because Spec Trio notification/audit-log/file-storage rows count once and the `/ax-verify-domain <new>` updates count as one infrastructure update per domain folded into the same row). To stay honest: the per-row table contains 70 deliverable rows that collectively form **68 catalog atoms** (the headline number). All SPs below ship the listed rows verbatim.

### P0 surface totals (post-rescope)

| Surface | Count | Delta vs pre-rescope |
|---|---|---|
| Backend cross-cutting | 10 | 0 |
| L1 primitives | 7 | −1 (`kbd` demoted) |
| L2 blocks | 14 cross-cutting + 4 domain-paired (`notification-bell`, `notification-list`, `audit-log-view`, `file-upload-area`) = 18 | 0 |
| L3 pages | 4 (`wizard`, `settings`, `audit-log`, `forgot-password`) | −2 (`empty-data-page`, `search-results-page` demoted) |
| Backend domain skeletons | 5 (`notification`, `audit-log`, `file-storage`, `email-outbox`, `scheduled-task`) | 0 |
| Spec Trios | 6 (4 promoted P1/P2→P0 to honor atomic-ordering) | +4 |
| Java rules | 8 | −2 demoted, +1 governance |
| React rules | 9 | −2 demoted, +1 IME, +1 cross-stack governance (`no-rrn-shape-in-client-logs` companion to #58); `address-search-no-rrn-leak` collapsed into #58 |
| Skills | 1 (`/ax-fork-receiver`) | −3 (`/ax-add-rule`, `/ax-add-template`, `/ax-doctor` demoted) |

**Headline:** **68 P0 catalog atoms.** Down from pre-rescope 75 = −11 demotions +4 atomic-ordering promotions. Anti-bloat envelope respected.

---

## §3 SP Plan (SP13 onwards)

> Format mirrors PRD §5. Each SP has Inputs / Deliverables / Acceptance / Verify command / TDD anchor / Risks / Agent count. All SPs land **after SP12** (which is closed).

### SP dependency graph

```
SP13 (backend baseline) ──┬─▶ SP14 (L1 primitives) ──▶ SP15 (L2 cross-cutting blocks)
                          │                                         │
                          │                                         ▼
                          │   ┌─────────────────────────────────────┴────────────────────┐
                          │   ▼                                                          ▼
                          ├─▶ SP16 (notification atomic)     SP17 (audit-log atomic)    SP18 (file-storage atomic)
                          │                                         │                    │
                          │                                         ▼                    ▼
                          └─▶ SP19 (email-outbox + scheduled-task atomic) ──▶ SP20 (L3 wizard/settings/forgot-password)
                                                                                                      │
                                                                                                      ▼
                                                                                                     SP21 (P0 rules cluster)
                                                                                                      │
                                                                                                      ▼
                                                                                                     SP22 (/ax-fork-receiver)
```

**Critical ordering rules:**

- **SP13 lands before SP14/SP15/SP16/SP17/SP18/SP19.** Backend cross-cutting templates power every backend domain skeleton.
- **SP14 lands before SP15/SP16/SP17/SP18/SP20.** L1 primitives are referenced by L2 blocks and L3 pages.
- **SP15 lands before SP16/SP17/SP18.** L2 cross-cutting blocks (toast-queue, error-boundary, virtualized-table, etc.) are dependencies of L2 domain-paired blocks.
- **SP16/SP17/SP18 can run in parallel after SP15.** Disjoint domains; shared-artifact rules apply (see §6).
- **SP19** (email-outbox + scheduled-task) **lands before SP20** because forgot-password-page depends on email-outbox.
- **SP20 lands before SP21.** Rules with failing fixtures may target SP20 templates.
- **SP22 lands last.** Fork-receiver smoke runs against the full post-SP21 tree.

---

### SP13 — Backend Cross-Cutting Baseline (10 templates, ATOMIC)

- **Inputs:** SP12 done; `templates/backend/.gitkeep` is the only file under `templates/backend/`.
- **Deliverables:**
  - 10 `.java` template files per §2 rows #1–#10.
  - `templates/backend/AGENTS.md` regenerated by `templates/generate_agents.sh`.
  - `templates/backend/_check-anchors.sh` — diffs each template's `evidence:` block against the rule it claims.
  - 10 RestAssured fixture tests under `practices/evals/fixtures/base_controller/`, `base_service/`, ... each with `pass/` and `fail_*/` cases (the TDD anchor).
- **Acceptance gate:**
  - `/ax-verify-java` exits 0 on `templates/backend/**`.
  - `ax-guard-evidence` walks the new path (zero-scan guard PASS).
  - All 10 fixture tests: `pass/` exit 0, every `fail_*/` exit 1 with the named error message.
- **Verify command:** `/ax-verify-java`.
- **TDD anchor:** `practices/evals/fixtures/base_controller/fail_missing_restcontroller/Controller.java` is written **first** — an empty class missing `@RestController`. Test asserts archunit fails with `BASE_CONTROLLER_MISSING_ANNOTATION`. SP13 implementation makes `pass/Controller.java` green by inheriting from `BaseController.java`.
- **Risks + mitigation:**
  - **R:** Existing backend domain code (`backend/src/main/java/.../auth/`) doesn't yet inherit from `BaseController`; running the new rule on the existing tree would break tests.
    **M:** SP13 ships templates under `templates/backend/` only; existing `backend/` rules unchanged. Extraction of existing backend code is P1 (not in this PRD).
  - **R:** `OpenApiConfig.java` collides with springdoc autoconfig.
    **M:** Template uses `@ConditionalOnMissingBean(OpenAPI.class)`.
- **Agent count:** 1 lead + 2 workers (parallel template authoring, one worker per error/security/config triplet).
- **Effort:** M (1 d).
- **Depends-on:** SP12 green.
- **Rollback boundary:** `git tag sp13-pre-start`. Revert `templates/backend/**` (excluding `.gitkeep`) on 3-fail halt.

---

### SP14 — L1 P0 Primitives (7 components)

- **Inputs:** SP13 done.
- **Deliverables:**
  - 7 `templates/L1/components/*.tsx` per §2 rows #11–#17 (combobox, date-picker, calendar, date-range-picker, file-dropzone, otp-input, address-search).
  - Per-component `evidence:` block (shadcn snapshot reference OR `source_type: external` for address-search citing 카카오 우편번호 docs OR `source_type: internal_design` rationale).
  - `templates/L1/_check-shadcn-drift.sh` updated to 39-component snapshot (32 existing + 7 new; `kbd` excluded).
  - `practices-react/upstream/shadcn-registry-2026-05.snapshot.md` extended.
  - `practices-react/upstream/kakao-postcode-2026-05.snapshot.md` NEW — frozen copy of Daum/Kakao 우편번호 widget API docs (for `address-search` external evidence anchor).
  - `templates/L1/_stories/<component>.spec.ts` Playwright story per primitive.
- **Acceptance gate:**
  - `/ax-verify-L1` PASS.
  - `time_decay_guard` on shadcn + kakao-postcode snapshots PASS.
  - `address-search` Playwright story: open widget → search "강남대로 396" → select → resolves to controlled prop.
  - `combobox` Playwright IME story: type Korean text with composition events → no premature debounce, no text corruption (this is the **first green test** for SP21's `combobox-respects-hangul-ime-composition` rule).
- **Verify command:** `/ax-verify-L1`.
- **TDD anchor:** `templates/L1/_stories/combobox-ime.spec.ts` — written **first**, asserts `compositionstart`/`compositionupdate`/`compositionend` sequence in Korean does not trigger the combobox's `onChange` callback during composition. Pre-SP14 the component doesn't exist; test fails ENOENT.
- **Risks + mitigation:**
  - **R:** `address-search` requires runtime injection of the Daum/Kakao 우편번호 script tag; introduces external network dependency.
    **M:** Component takes an `injector` prop (dependency injection); Playwright story uses a mocked injector to avoid live network in CI.
  - **R:** shadcn calendar v9 has Tailwind v4 dependency drift.
    **M:** SP14 ships against the snapshot version pinned in `blueprints/pinned-versions.yaml`.
- **Agent count:** 1 lead + 3 workers (parallel: combobox cluster, date cluster, file/otp/address cluster).
- **Effort:** M (1 d).
- **Depends-on:** SP13.
- **Rollback boundary:** `git tag sp14-pre-start`. Revert `templates/L1/components/*.tsx` (new 7 only) + drift script.

---

### SP15 — L2 P0 Cross-Cutting Blocks (14 blocks, NON-domain-coupled)

- **Inputs:** SP14 done.
- **Deliverables:** 14 `templates/L2/blocks/*.tsx` per §2 rows #18–#31 (form-section, field-array, conditional-field, form-error-summary, virtualized-table, expandable-row, advanced-filter-builder, filter-chips, error-boundary, offline-banner, search-palette, skip-link, announce-live, toast-queue).
- **Acceptance gate:**
  - `/ax-verify-L2` PASS.
  - axe-core PASS on `skip-link`, `form-error-summary`, `announce-live`, `toast-queue` (a11y-bound blocks).
  - `virtualized-table` benchmark: 5000 rows render with DOM node count < 100.
  - `expandable-row` Playwright: nested detail expands within 50ms of click.
  - No cross-block import (custom ESLint rule emits 0 violations).
- **Verify command:** `/ax-verify-L2`.
- **TDD anchor:** `templates/L2/_fixtures/virtualized-table-bench.spec.ts` — Playwright fixture pre-renders 5000 rows; asserts `document.querySelectorAll('tr').length < 100`. Pre-SP15: `virtualized-table.tsx` absent; render fails ENOENT.
- **Risks + mitigation:**
  - **R:** `virtualized-table` may exceed 2d effort (per Critic split-rule).
    **M:** Pre-SP15 spike (≤2h) on `@tanstack/react-virtual` wrapper; if spike shows > 2d, virtualized-table splits into SP15.5.
  - **R:** `advanced-filter-builder` rule-schema design risk.
    **M:** Schema written to mirror existing `filter-bar.tsx` shape; serialization round-trip is the binary anchor.
- **Agent count:** 1 lead + 4 workers (parallel: forms cluster, tables cluster, filters cluster, a11y/error/offline cluster).
- **Effort:** L (1–2 d).
- **Depends-on:** SP14.
- **Rollback boundary:** `git tag sp15-pre-start`. Revert `templates/L2/blocks/*.tsx` (14 new only).

---

### SP16 — Notification Domain (ATOMIC: Spec Trio + backend + L2 + verify update)

- **Inputs:** SP15 done.
- **Deliverables (atomic — all land in one tag or none):**
  - **Spec Trio (#32):** `specs/notification-l0.yaml` + `specs/notification-frontend-l0.yaml` + `contracts/notification-openapi.yaml` + `contracts/notification-ui.yaml` + `blueprints/notification-manifest.yaml` + `blueprints/notification-ui-manifest.yaml`.
  - **Backend skeleton (#33):** `templates/backend/notification/` with `NotificationController.java`, `NotificationService.java`, `NotificationOutboxEntity.java`, `NotificationDispatcher.java` (`@Async`, MDC-propagating), `NotificationRepository.java`, RestAssured integration test.
  - **L2 blocks (#34, #35):** `templates/L2/blocks/notification-bell.tsx` + `templates/L2/blocks/notification-list.tsx`.
  - **Verify skill update (#36):** `skills/ax-verify-domain/scripts/domain-registry.yaml` adds `notification` entry; smoke script per domain.
  - `templates/L4/notification/` minimal L4 wiring (1 page demo using #34 + #35) — minimal to prove the trio binds end-to-end.
- **Acceptance gate:**
  - `/ax-guard-trio-integrity` PASS on `notification` domain (full_trio mode).
  - `/ax-verify-domain notification` PASS (orchestrates: Java RestAssured + frontend Playwright + axe).
  - End-to-end test: trigger `POST /api/v1/notifications/test` → bell badge increments via mock SSE → list dropdown shows new row → mark-read mutation persists.
- **Verify command:** `/ax-verify-domain notification`.
- **TDD anchor:** `practices/evals/fixtures/trio_integrity/fail_notification_missing_frontend_spec/` — fixture with only backend spec present; `trio_integrity_guard.sh` exits 1 with `MISSING_FRONTEND_SPEC`. SP16 makes the real `pass_notification/` fixture green by adding the frontend spec.
- **Risks + mitigation:**
  - **R:** SSE infrastructure not standardized in template; introducing it expands scope.
    **M:** L2 `notification-bell` polls via TanStack Query at 30s default; SSE wire-up is P1 follow-up. Bell's data fetch is via prop (per `l2-prefer-data-prop-over-direct-fetch.md`), so caller can swap polling for SSE without amending L2.
  - **R:** `NotificationOutboxEntity` shape may need to mirror email-outbox (SP19). Pre-write coordination needed.
    **M:** SP16 lead reviews SP19 plan before writing outbox shape; common base class `OutboxEntityBase` lives in `templates/backend/` (SP13 baseline).
- **Agent count:** 1 lead + 3 workers (Spec Trio worker, backend worker, frontend worker).
- **Effort:** L (2 d).
- **Depends-on:** SP13, SP14, SP15.
- **Rollback boundary:** `git tag sp16-pre-start`. Atomic revert: ALL deliverables roll back together if any sub-component fails verify.

---

### SP17 — Audit-Log Domain (ATOMIC: Spec Trio + backend + L2 + L3 + verify update)

- **Inputs:** SP15 done; SP21 rule #51 (`audit-event-immutability`) **co-shipped** here (the rule's failing fixture predates the audit-log backend implementation; SP17 ships the rule + the implementation atomically; SP21 batches all rules but for `audit-event-immutability` SP21 just regenerates `_MANIFEST.yaml` — the rule's `.md` file ships in SP17).
- **Deliverables (atomic):**
  - **Spec Trio (#37):** `specs/audit-log-l0.yaml` + `specs/audit-log-frontend-l0.yaml` + `contracts/audit-log-openapi.yaml` + `contracts/audit-log-ui.yaml` + `blueprints/audit-log-manifest.yaml` + `blueprints/audit-log-ui-manifest.yaml`.
  - **Backend skeleton (#38):** `templates/backend/audit-log/` with `AuditEvent.java` (`@Immutable`, append-only), `AuditEventListener.java` (JPA `@EntityListeners` or AOP), `AuditLogController.java` (read-only), `AuditLogQueryService.java`, RestAssured test asserting mutation rejection.
  - **Java rule (#51):** `practices/rules/audit-event-immutability.md` + failing fixture.
  - **L2 block (#39):** `templates/L2/blocks/audit-log-view.tsx` (row renderer: actor / action / target / diff).
  - **L3 page (#40):** `templates/L3/pages/audit-log-page/{page.tsx,error.tsx,loading.tsx,README.md}` with slot contract.
  - **Verify skill update (#41):** registry entry for `audit-log`.
- **Acceptance gate:**
  - `/ax-guard-trio-integrity` PASS.
  - `/ax-verify-domain audit-log` PASS.
  - End-to-end test: trigger any mutation on existing CRUD/payment domain → audit row appended; `templates/L3/pages/audit-log-page/` renders the row with diff.
  - PIPA probe: `practices/evals/no_rrn_logging_probe.sh` (deferred to SP21) does NOT find RRN-shape patterns in audit log entries (composition validates SP21 rule #58).
- **Verify command:** `/ax-verify-domain audit-log`.
- **TDD anchor:** `practices/evals/fixtures/audit_event_immutability/fail_mutable_setter/AuditEvent.java` declares `setActor()` setter → archunit fails. Pre-SP17 the rule file `audit-event-immutability.md` doesn't exist; archunit can't even load. SP17 makes the rule load + the `pass/` fixture green.
- **Risks + mitigation:**
  - **R:** Append-only enforcement under JPA requires care (cascade deletes, audit retention policy).
    **M:** SP17 ships flyway migration with `REVOKE DELETE` GRANT on `audit_event` table; archunit asserts no `@Repository.delete*` method shape.
  - **R:** `audit-log-page` virtualized-table dependency on SP15 #22; if SP15 ships virtualized-table with bugs, SP17 inherits.
    **M:** SP17 includes regression Playwright test that SP15's #22 must still pass.
- **Agent count:** 1 lead + 3 workers.
- **Effort:** L (2 d).
- **Depends-on:** SP13, SP14, SP15.
- **Rollback boundary:** `git tag sp17-pre-start`. Atomic revert.

---

### SP18 — File-Storage Domain (ATOMIC: Spec Trio + backend + L2 + verify update)

- **Inputs:** SP15 done; SP21 rule #55 (`presigned-url-no-direct-upload`) co-shipped (same pattern as SP17/#51).
- **Deliverables (atomic):**
  - **Spec Trio (#42):** `specs/file-storage-l0.yaml` + `specs/file-storage-frontend-l0.yaml` + contracts + manifests.
  - **Backend skeleton (#43):** `templates/backend/file-storage/` with `PresignController.java` (returns signed PUT URL), `FileMetadataEntity.java`, `VirusScanHook.java` (interface), `RetentionPolicy.java`, RestAssured test.
  - **Java rule (#55):** `practices/rules/presigned-url-no-direct-upload.md` + failing fixture.
  - **L2 block (#44):** `templates/L2/blocks/file-upload-area.tsx` (composes #15 file-dropzone + progress + cancel + retry).
  - **Verify skill update (#45):** registry entry for `file-storage`.
- **Acceptance gate:**
  - `/ax-guard-trio-integrity` PASS.
  - `/ax-verify-domain file-storage` PASS.
  - End-to-end test: drop file in `file-upload-area` → `POST /api/v1/files/presign` returns mock signed URL → upload via signed URL completes → metadata row inserted.
- **Verify command:** `/ax-verify-domain file-storage`.
- **TDD anchor:** `practices/evals/fixtures/presigned_url_no_direct_upload/fail_multipart_upload_endpoint/UploadController.java` declares `@PostMapping consumes=multipart/form-data` accepting `MultipartFile` → archunit fails. SP18 makes pass case (presign-only) green.
- **Risks + mitigation:**
  - **R:** S3/MinIO infrastructure not in template; presign requires live SDK.
    **M:** SP18 ships AWS SDK v2 abstraction behind `StorageBackend` interface; default impl is a local-filesystem-backed mock (`LocalPresignProvider`); fork-receiver chooses real backend.
  - **R:** Virus-scan hook coupled to specific vendor.
    **M:** Hook is an interface (`VirusScanHook`) with a no-op default; ADR (`provenance_class: internal_design`) explains why this is left as an extension point.
- **Agent count:** 1 lead + 2 workers.
- **Effort:** L (1–2 d).
- **Depends-on:** SP13, SP14, SP15.
- **Rollback boundary:** `git tag sp18-pre-start`. Atomic revert.

---

### SP19 — Email-Outbox + Scheduled-Task Backend (ATOMIC, paired)

- **Inputs:** SP13 done; SP21 rules #52 (`outbox-transactional-publish`) + #56 (`scheduled-task-jitter`) co-shipped.
- **Deliverables (atomic):**
  - **Spec Trios (#46, #48):** `specs/email-outbox-l0.yaml` + `specs/scheduled-task-l0.yaml` (both `backend_only` mode; no UI contracts).
  - **Backend skeletons (#47, #49):**
    - `templates/backend/email-outbox/` with `EmailOutboxEntity.java`, `EmailOutboxPublisher.java` (`@Transactional`-only publish), `EmailOutboxRelay.java` (`@Scheduled` poller).
    - `templates/backend/scheduled-task/` with `AbstractScheduledTask.java` (jitter + idempotency-key + MDC base class), `IdempotentTaskExecutor.java`.
  - **Java rules (#52, #56):** `practices/rules/outbox-transactional-publish.md` + `practices/rules/scheduled-task-jitter.md` + failing fixtures.
- **Acceptance gate:**
  - `/ax-guard-trio-integrity` PASS on both domains (backend_only mode).
  - `/ax-verify-domain email-outbox` PASS — TX rollback test asserts no orphan outbox row.
  - `/ax-verify-domain scheduled-task` PASS — concurrent trigger test asserts idempotency.
- **Verify command:** `/ax-verify-domain email-outbox && /ax-verify-domain scheduled-task`.
- **TDD anchor:** `practices/evals/fixtures/outbox_transactional_publish/fail_publish_outside_transaction/Publisher.java` calls `outboxRepository.save()` without `@Transactional` → archunit fails. SP19 makes pass case (in-transaction) green.
- **Risks + mitigation:**
  - **R:** Existing auth domain currently sends email directly (per audit §D.2 / §E P0 #47 justification). Migrating auth to use email-outbox is a follow-up — SP19 ships the template, not the migration.
    **M:** SP19's L4 minimal demo shows pattern; existing `backend/.../auth/` migration is P1 (not in this PRD).
  - **R:** `@Scheduled` jitter probe is timing-sensitive in CI.
    **M:** Test uses `Awaitility` with explicit time-travel via `ManualClock`; no `Thread.sleep`.
- **Agent count:** 1 lead + 2 workers.
- **Effort:** M (1 d).
- **Depends-on:** SP13.
- **Rollback boundary:** `git tag sp19-pre-start`. Atomic revert.

---

### SP20 — L3 Pages Cluster (settings + wizard + forgot-password)

- **Inputs:** SP15 done; SP19 done (forgot-password depends on email-outbox); SP14 done (settings-page composes L1 #16 otp-input for MFA preference toggle).
- **Deliverables (atomic):**
  - **Spec Trio (#50):** `specs/settings-frontend-l0.yaml` + `contracts/settings-ui.yaml` + `blueprints/settings-ui-manifest.yaml` (frontend_only mode; `static_source_ref` to user-preference local-state files).
  - **L3 pages (#67, #68, #69):**
    - `templates/L3/pages/wizard-page/{page.tsx, error.tsx, loading.tsx, README.md}` with slots `header / steps / step-content / footer-nav`.
    - `templates/L3/pages/settings-page/{page.tsx, error.tsx, loading.tsx, README.md}` with slots `settings-nav / settings-content`.
    - `templates/L3/pages/forgot-password-page/{page.tsx, error.tsx, loading.tsx, README.md}` with slots `header / form / footer`.
- **Acceptance gate:**
  - `/ax-verify-L3` PASS.
  - `/ax-guard-trio-integrity --mode frontend_only` PASS on settings.
  - Each page's README slot-contract test asserts each slot's element exists.
  - `forgot-password-page`: form POST triggers email-outbox row.
- **Verify command:** `/ax-verify-L3`.
- **TDD anchor:** `templates/L3/pages/wizard-page/_tests/multi-step-state-preserved.spec.ts` — Playwright fills step 1, advances to step 2, goes back to step 1, asserts state preserved. Pre-SP20: page doesn't exist; test fails 404.
- **Risks + mitigation:**
  - **R:** `settings-page` `frontend_only` mode requires `static_source_ref` to resolve; bare repo may not have user-preference files yet.
    **M:** SP20 ships a minimal `frontend/src/lib/settings/preferences-schema.ts` referenced by `static_source_ref`; this file is in the fixture pass case.
  - **R:** `wizard-page` slot contract risks ambiguity with existing `form-stepper` L2 block (P1).
    **M:** wizard-page README explicitly defers stepper UI to L2 (when L2 form-stepper lands as P1); P0 wizard-page just provides shell slots.
- **Agent count:** 1 lead + 2 workers.
- **Effort:** M (1 d).
- **Depends-on:** SP14, SP15, SP19.
- **Rollback boundary:** `git tag sp20-pre-start`. Atomic revert.

---

### SP21 — P0 Rules Cluster (remaining rules + cross-stack governance + IME)

- **Inputs:** SP16/SP17/SP18/SP19 done (rules #51, #52, #55, #56 already shipped atomically with their domains; SP21 covers remaining rules + regenerates manifests).
- **Deliverables:** Remaining P0 rules per §2 (Java: #53, #54, #57, #58; React: #58 companion, #59, #60, #61, #62, #63, #64, #65, #66) — 12 rule files + failing fixtures.
  - **Java (4 rules):**
    - `practices/rules/idempotency-key-on-mutations.md` (#53)
    - `practices/rules/traceId-in-error-response.md` (#54)
    - `practices/rules/mdc-trace-on-async.md` (#57)
    - `practices/rules/no-rrn-logging.md` (#58 Java side; `provenance_class: locked_constraint`, cites 개인정보보호법 §24)
  - **React (9 rules):**
    - `practices-react/rules/no-rrn-shape-in-client-logs.md` (#58 React companion)
    - `practices-react/rules/no-cross-l4-domain-imports.md` (#59)
    - `practices-react/rules/no-server-component-state-leakage-to-client.md` (#60)
    - `practices-react/rules/traceId-rendered-on-error-boundary.md` (#61)
    - `practices-react/rules/aria-live-on-toast-queue.md` (#62)
    - `practices-react/rules/skip-link-required-on-app-shell.md` (#63)
    - `practices-react/rules/optimistic-update-rollback-required.md` (#64)
    - `practices-react/rules/combobox-respects-hangul-ime-composition.md` (#65)
    - `practices-react/rules/l2-prefer-data-prop-over-direct-fetch-extension.md` (#66 — codemod fixture for `extend_existing`)
  - 13 failing fixtures under `practices/evals/fixtures/<rule>/fail_*/` (one per rule; #66 is codemod fixture, not a new rule body).
  - `_MANIFEST.yaml` regenerated in both `practices/rules/` and `practices-react/rules/`.
  - `practices/AGENTS.md` + `practices-react/AGENTS.md` sentinels regenerated (sha256 anchor refresh).
- **Acceptance gate:**
  - `/ax-verify-java` + `/ax-verify-react` PASS.
  - `time_decay_guard` PASS on new rules (each carries `next_review_by` = +180d).
  - `evidence_guard` PASS (each rule cites either `upstream_id` OR `source_type: external` OR `internal_design`).
  - All 13 failing-fixture tests: `fail_*/` exit non-zero with the named error message; `pass/` exit 0.
  - **Cross-cutting validation:** existing 19/19 guard fixtures continue to pass (no regression in PRD SP12 baseline).
- **Verify command:** `/ax-verify-java && /ax-verify-react && bash practices/evals/run-all-guards.sh --include-fixtures`.
- **TDD anchor:** `practices/evals/fixtures/no_rrn_logging/fail_logger_rrn_pattern/Logger.java` written **first** — a Java class calling `log.info("user " + rrn)` where `rrn` matches `\d{6}-\d{7}` regex. Pre-SP21: the static-analysis script `no_rrn_logging_probe.sh` doesn't exist; guard execution returns "script not found." SP21 ships the script + rule + makes pass case green.
- **Risks + mitigation:**
  - **R:** `no-RRN-logging` regex (`\d{6}-\d{7}`) over-fires on test data containing date-like patterns (`991231-1234567`).
    **M:** Rule allowlists `/test/`, `/_fixtures/`, `/practices/evals/` paths via `evidence:` block exception; explicit ADR documents the allowlist and references PIPA §24 wording.
  - **R:** `combobox-respects-hangul-ime-composition` rule + fixture requires a Playwright Korean IME simulator.
    **M:** SP21 ships `practices/evals/fixtures/combobox_hangul_ime/ime-simulator.ts` that dispatches `compositionstart`/`compositionupdate`/`compositionend` events programmatically — no actual IME required in CI.
  - **R:** New rules trip existing legacy `frontend/` code paths (regression against SP12 baseline).
    **M:** SP21 lead audits each new rule's blast radius before committing; if any rule triggers on existing `frontend/**`, scope is narrowed to `templates/**` only via `evidence:` `applies_to` field; legacy migration is P1.
- **Agent count:** 1 lead + 4 workers (parallel: Java rules, React a11y rules, React layer/governance rules, IME + codemod cluster).
- **Effort:** M (1 d).
- **Depends-on:** SP16, SP17, SP18, SP19, SP20.
- **Rollback boundary:** `git tag sp21-pre-start`. Per-rule revert allowed (rules are independent files); atomic only for `_MANIFEST.yaml` regen.

---

### SP22 — `/ax-fork-receiver` (Tier-1 NEW)

- **Inputs:** SP13–SP21 done (full P0 tree present).
- **Deliverables:**
  - `skills/ax-fork-receiver/SKILL.md` (Tier-1; pathPattern: none — invoked directly by user).
  - `skills/ax-fork-receiver/scripts/bundle.sh` — produces `dist/ax-template-bundle.tar.gz` containing `templates/`, `specs/`, `contracts/`, `blueprints/`, `practices/`, `practices-react/`, `skills/`, `verify/`, and `install.sh`.
  - `skills/ax-fork-receiver/scripts/install.sh` — bundled inside tarball; runs in fork-target dir:
    1. Extract tarball.
    2. Run `templates/generate_agents.sh` to regenerate sentinels.
    3. Run `bash skills/ax-verify/scripts/run-all.sh`.
    4. Exit with verify result.
  - `skills/_tests/fork-receiver-cold-tarball.spec.sh` — TDD anchor.
- **Acceptance gate:**
  - Cold-tarball-install in `/tmp/ax-fork-receiver-test/`: `tar xzf` → `cd extracted/` → `bash install.sh` → exit 0 within 300s.
  - `/ax-verify` PASS on the freshly-installed tree (delta-zero vs source repo).
  - No `PATH_LEAK` (no script references a path outside the tarball root).
- **Verify command:** `/ax-fork-receiver --self-test` (bundled subcommand).
- **TDD anchor:** `skills/_tests/fork-receiver-cold-tarball.spec.sh` written **first** — runs `bundle.sh` → extracts to `/tmp` → runs `install.sh` → asserts exit 0 within 300s. Pre-SP22: `bundle.sh` doesn't exist; test fails with `script not found`.
- **Risks + mitigation:**
  - **R:** Tarball size grows >100MB (cap for fast download).
    **M:** Exclude `node_modules/`, `.next/`, `build/`, `.gradle/`, `backend/build/`, `frontend/.next/` from bundle; assert tarball size < 50MB.
  - **R:** Sentinel sha256 mismatch after extraction (line-ending normalization on Windows).
    **M:** `install.sh` regenerates sentinels post-extraction via `templates/generate_agents.sh`.
- **Agent count:** 1 lead.
- **Effort:** M (1 d).
- **Depends-on:** SP21.
- **Rollback boundary:** `git tag sp22-pre-start`. Revert `skills/ax-fork-receiver/**`.

---

### Effort summary

| SP | Effort | Wall-time (parallel agents) |
|---|---|---|
| SP13 | M | 1 d |
| SP14 | M | 1 d |
| SP15 | L | 1–2 d |
| SP16 | L | 2 d |
| SP17 | L | 2 d |
| SP18 | L | 1–2 d |
| SP19 | M | 1 d |
| SP20 | M | 1 d |
| SP21 | M | 1 d |
| SP22 | M | 1 d |
| **Total** | — | **12–14 d** (SP16/17/18 parallel after SP15 → cuts ~3 d) → **9–11 d wall-time** |

---

## §4 Verification Matrix

> Mirror of PRD §5.5. Single authoritative table covering every new SP. No "TBD" cells.

| SP | verify_skill | script_path | test_file | assertion | expected_RED_reason | first_green_command | observability_signal |
|---|---|---|---|---|---|---|---|
| SP13 | `/ax-verify-java` | `practices/evals/run-all-guards.sh` + `gradle testPractices` | `practices/evals/fixtures/base_controller/fail_missing_restcontroller/Controller.java` + 9 sibling fixtures | All 10 baseline templates anchor a passing fixture; each `fail_*/` returns non-zero with named error | Pre-SP13: `templates/backend/controllers/BaseController.java` does not exist; archunit fails with `BASE_CLASS_NOT_FOUND` | `bash skills/ax-verify-java/scripts/run.sh templates/backend/` | `template.evidence.coverage_ratio` (assert == 1.0); `archunit.violations` (== 0) |
| SP14 | `/ax-verify-L1` | `templates/L1/_check-shadcn-drift.sh` + `frontend/playwright.config.ts` | `templates/L1/_stories/combobox-ime.spec.ts` (TDD anchor) + 6 sibling stories | Each of 7 new primitives renders + drift PASS + combobox IME composition events do not corrupt text | Pre-SP14: `templates/L1/components/combobox.tsx` ENOENT; Playwright fails to load story | `bash skills/ax-verify-L1/scripts/run.sh` | `l1.component.count` (== 39); `ime.composition.corruption_count` (== 0) |
| SP15 | `/ax-verify-L2` | `templates/L2/_check-cross-block.sh` + `frontend/playwright.config.ts` + axe-core CLI | `templates/L2/_fixtures/virtualized-table-bench.spec.ts` + 13 sibling block tests | 14 blocks render in isolation; virtualized-table DOM < 100 nodes @ 5000 rows; axe PASS on a11y blocks; 0 cross-block imports | Pre-SP15: virtualized-table.tsx ENOENT; bench fixture fails ENOENT | `bash skills/ax-verify-L2/scripts/run.sh` | `l2.block.count` (== 40); `axe.violations` (== 0); `virtualized.dom_node_count_p99` (< 100) |
| SP16 | `/ax-verify-domain notification` | `skills/ax-verify-domain/scripts/run.sh notification` | `practices/evals/fixtures/trio_integrity/fail_notification_missing_frontend_spec/` + RestAssured `NotificationFlowIT.java` + Playwright `notification-bell-list.spec.ts` | Atomic deliverables present; trio_integrity PASS; POST `/api/v1/notifications/test` → bell updates → list renders → mark-read persists | Pre-SP16: `specs/notification-l0.yaml` absent; trio_integrity exits 1 with `MISSING_BACKEND_SPEC` | `bash skills/ax-verify-domain/scripts/run.sh notification` | `notification.dispatch.success_rate` (== 1.0); `notification.outbox.row_count` (matches dispatch count); `trio.coverage_ratio` (== 1.0 for notification) |
| SP17 | `/ax-verify-domain audit-log` | `skills/ax-verify-domain/scripts/run.sh audit-log` | `practices/evals/fixtures/audit_event_immutability/fail_mutable_setter/AuditEvent.java` + RestAssured `AuditLogAppendOnlyIT.java` + Playwright `audit-log-page.spec.ts` | Atomic deliverables present; mutation on existing CRUD/payment domain → audit row; page renders row with diff; no DELETE on `audit_event` table | Pre-SP17: `practices/rules/audit-event-immutability.md` absent; archunit cannot load rule | `bash skills/ax-verify-domain/scripts/run.sh audit-log` | `audit.append_only.violations` (== 0); `audit.row.append_rate` (matches mutation rate); `trio.coverage_ratio` (== 1.0 for audit-log) |
| SP18 | `/ax-verify-domain file-storage` | `skills/ax-verify-domain/scripts/run.sh file-storage` | `practices/evals/fixtures/presigned_url_no_direct_upload/fail_multipart_upload_endpoint/UploadController.java` + RestAssured `PresignFlowIT.java` + Playwright `file-upload-area.spec.ts` | Atomic deliverables present; drop file → presign → upload → metadata row; no multipart endpoint exposed | Pre-SP18: `templates/backend/file-storage/PresignController.java` absent; archunit reports `NO_PRESIGN_CONTROLLER` | `bash skills/ax-verify-domain/scripts/run.sh file-storage` | `file.upload.success_rate` (== 1.0); `multipart.endpoint.violations` (== 0); `trio.coverage_ratio` (== 1.0 for file-storage) |
| SP19 | `/ax-verify-domain email-outbox && /ax-verify-domain scheduled-task` | `skills/ax-verify-domain/scripts/run.sh email-outbox` + `... scheduled-task` | `practices/evals/fixtures/outbox_transactional_publish/fail_publish_outside_transaction/Publisher.java` + `fail_no_jitter/ScheduledTask.java` + RestAssured `OutboxRollbackIT.java` + `ScheduledIdempotencyIT.java` | Atomic deliverables present; TX rollback → 0 orphan outbox rows; 2 concurrent triggers same key → 1 execution; jitter > 0ms | Pre-SP19: `templates/backend/email-outbox/EmailOutboxPublisher.java` absent; archunit fails | `bash skills/ax-verify-domain/scripts/run.sh email-outbox && ... scheduled-task` | `outbox.orphan_rows_after_rollback` (== 0); `scheduled.idempotency_violations` (== 0); `scheduled.jitter_ms` (> 0) |
| SP20 | `/ax-verify-L3` | `templates/L3/_check-slot-contracts.sh` + `frontend/playwright.config.ts` | `templates/L3/pages/wizard-page/_tests/multi-step-state-preserved.spec.ts` + sibling page tests | 3 page templates resolve + slot contracts honored + settings frontend_only trio PASS + forgot-password POST triggers email-outbox | Pre-SP20: wizard-page route 404; settings frontend_only trio fails `MISSING_STATIC_SOURCE_REF` | `bash skills/ax-verify-L3/scripts/run.sh && bash practices/evals/trio_integrity_guard.sh --mode frontend_only` | `l3.page.count` (== 10); `slot.contract.violations` (== 0); `forgot_password.outbox_dispatch_count` (matches submit count) |
| SP21 | `/ax-verify-java && /ax-verify-react` | `practices/evals/run-all-guards.sh --include-fixtures` | `practices/evals/fixtures/no_rrn_logging/fail_logger_rrn_pattern/Logger.java` + 12 sibling rule fixtures | All 13 new rules load + each `fail_*/` returns non-zero + existing 19/19 guards still PASS | Pre-SP21: `practices/evals/no_rrn_logging_probe.sh` does not exist; orchestrator returns `script not found` | `bash practices/evals/run-all-guards.sh --include-fixtures` | `rule.count.delta` (== +13); `existing_guards.regression_count` (== 0); `rrn.shape.violations` (== 0 in templates/, allowlist respected in test/) |
| SP22 | `/ax-fork-receiver --self-test` | `skills/ax-fork-receiver/scripts/bundle.sh` + `install.sh` | `skills/_tests/fork-receiver-cold-tarball.spec.sh` | Cold tarball install in /tmp; `/ax-verify` exit 0 within 300s; no PATH_LEAK | Pre-SP22: `bundle.sh` ENOENT | `bash skills/ax-fork-receiver/scripts/bundle.sh && bash skills/_tests/fork-receiver-cold-tarball.spec.sh` | `fork.install.duration` (< 300s); `fork.path.leak.count` (== 0); `fork.tarball.size_mb` (< 50) |

---

## §5 Spec Trio Assignment Table

| Domain | domain_mode | Spec Trio file paths | `backend_operation_id` mapping plan | `static_source_ref` (when frontend_only) |
|---|---|---|---|---|
| `notification` | `full_trio` | `specs/notification-l0.yaml`, `specs/notification-frontend-l0.yaml`, `contracts/notification-openapi.yaml`, `contracts/notification-ui.yaml`, `blueprints/notification-manifest.yaml`, `blueprints/notification-ui-manifest.yaml` | Each UI route maps to one of: `notification.list`, `notification.markRead`, `notification.markAllRead`, `notification.subscribe` (defined in `notification-openapi.yaml`) | N/A |
| `audit-log` | `full_trio` | `specs/audit-log-l0.yaml`, `specs/audit-log-frontend-l0.yaml`, `contracts/audit-log-openapi.yaml`, `contracts/audit-log-ui.yaml`, `blueprints/audit-log-manifest.yaml`, `blueprints/audit-log-ui-manifest.yaml` | UI routes map to `auditLog.search`, `auditLog.getById`, `auditLog.export` | N/A |
| `file-storage` | `full_trio` | `specs/file-storage-l0.yaml`, `specs/file-storage-frontend-l0.yaml`, `contracts/file-storage-openapi.yaml`, `contracts/file-storage-ui.yaml`, `blueprints/file-storage-manifest.yaml`, `blueprints/file-storage-ui-manifest.yaml` | UI routes map to `file.presign`, `file.confirmUpload`, `file.list`, `file.delete` | N/A |
| `email-outbox` | `backend_only` | `specs/email-outbox-l0.yaml`, `contracts/email-outbox-openapi.yaml`, `blueprints/email-outbox-manifest.yaml` (no frontend trio; admin UI is P1) | N/A (backend_only) | N/A |
| `scheduled-task` | `backend_only` | `specs/scheduled-task-l0.yaml`, `contracts/scheduled-task-openapi.yaml`, `blueprints/scheduled-task-manifest.yaml` | N/A (backend_only) | N/A |
| `settings` | `frontend_only` | `specs/settings-frontend-l0.yaml`, `contracts/settings-ui.yaml`, `blueprints/settings-ui-manifest.yaml` | All items declare `backend_operation_id: null` per §4.8.4 frontend_only mode | `frontend/src/lib/settings/preferences-schema.ts`, `frontend/src/lib/settings/theme-store.ts`, `frontend/src/lib/settings/locale-store.ts` (each entry resolves to ≥ 1 file) |

---

## §6 Autonomous Execution Safety

> Inherits all PRD §6.4 patterns verbatim (rollback boundary per SP, shared-artifact ownership, stale-state, halt thresholds, ESCAPE valve). New entries below extend the matrices.

### §6.1 Rollback boundary per SP (extension table)

| SP | Pre-start tag | Rollback boundary |
|---|---|---|
| SP13 | `git tag sp13-pre-start` | Revert `templates/backend/**` (excluding `.gitkeep`) + new fixture dirs |
| SP14 | `git tag sp14-pre-start` | Revert 7 new L1 components + drift script update + new snapshot |
| SP15 | `git tag sp15-pre-start` | Revert 14 new L2 blocks + cross-block check script |
| SP16 | `git tag sp16-pre-start` | **Atomic revert**: ALL notification deliverables (Spec Trio + backend + L2 + L4 demo + verify skill registry entry) |
| SP17 | `git tag sp17-pre-start` | **Atomic revert**: audit-log domain deliverables incl. rule #51 |
| SP18 | `git tag sp18-pre-start` | **Atomic revert**: file-storage domain deliverables incl. rule #55 |
| SP19 | `git tag sp19-pre-start` | **Atomic revert**: both email-outbox + scheduled-task deliverables incl. rules #52, #56 |
| SP20 | `git tag sp20-pre-start` | Revert 3 L3 page dirs + settings Spec Trio |
| SP21 | `git tag sp21-pre-start` | Per-rule revert allowed; `_MANIFEST.yaml` regen atomic |
| SP22 | `git tag sp22-pre-start` | Revert `skills/ax-fork-receiver/**` |

### §6.2 Shared-artifact ownership matrix (extension)

| Artifact | Sole writer SP | Reader SPs | Stale-state rule |
|---|---|---|---|
| `templates/backend/**` (10 baseline) | SP13 | SP16, SP17, SP18, SP19 (all backend domain SPs inherit these) | If SP13 amends post-merge, dependent SPs re-run `/ax-verify-java` before resuming |
| `templates/L1/components/*.tsx` (7 new) | SP14 | SP15, SP16, SP17, SP18, SP20 | Per PRD §6.4.2 rule for L1; readers re-import after SP14 amends |
| `templates/L2/blocks/*.tsx` (14 cross-cutting from SP15) | SP15 | SP16, SP17, SP18, SP20 | Atomic SPs (SP16/17/18) cannot amend SP15's blocks; if a domain SP needs a 15th block, that block is P1 |
| `templates/L2/blocks/*.tsx` (4 domain-paired) | Owning domain SP (SP16/17/18) | None (no cross-domain consumers) | Atomic with domain |
| `skills/ax-verify-domain/scripts/domain-registry.yaml` | SP16/SP17/SP18/SP19 each append one entry (serial-only at merge) | SP22 reads | Append-only; SP21/SP22 do not amend |
| `practices/rules/*.md` + `practices-react/rules/*.md` | SP16/SP17/SP18/SP19 each ship the rule paired with their domain; SP21 ships remaining cross-cutting rules + regenerates `_MANIFEST.yaml` once at end | All | `_MANIFEST.yaml` regenerated atomically in SP21 |
| `practices/AGENTS.md` + `practices-react/AGENTS.md` sentinels | SP21 regenerates exactly once | All prior SPs modify rule files but do NOT regenerate sentinel | Same as PRD §6.4.2 |
| `templates/AGENTS.md` sentinel | SP22 regenerates exactly once (final integration) | All prior SPs modify templates but don't regenerate | Same as PRD §6.4.2 |

### §6.3 Stale-state invalidation rule (extension)

Inherits PRD §6.4.3 verbatim, with additions:

- Re-run `bash skills/ax-verify-domain/scripts/run.sh <domain>` after any change to `templates/backend/<domain>/` OR `templates/L2/blocks/*<domain>*.tsx` OR `templates/L3/pages/*<domain>*/`.
- Re-run `bash practices/evals/no_rrn_logging_probe.sh` (after SP21 lands the script) whenever any `templates/**` content is added.

### §6.4 Halt thresholds (inherited from PRD §6.4.4)

- **3-fail halt:** identical to PRD.
- **30-minute idle halt:** identical to PRD.
- **5-rebase halt:** identical to PRD.
- **NEW — atomic-SP partial-fail halt:** for SP16/SP17/SP18/SP19 (atomic domain SPs), if any sub-component (Spec Trio OR backend OR L2/L3 OR verify skill update) fails verify, the entire SP rolls back to its pre-start tag. Per-sub-component partial-merge is forbidden.

### §6.5 ESCAPE valve (inherited from PRD §6.4.5)

Identical path: `docs/superpowers/escape/<SP_id>-<timestamp>.md`. No auto-resume; human approval required.

### §6.6 Cross-stack dependency declaration (NEW)

Each atomic domain SP (SP16/17/18/19) MUST emit a manifest at `docs/superpowers/sp<NN>-cross-stack-deps.yaml` listing:

```yaml
sp_id: SP16
backend_artifacts:
  - templates/backend/notification/NotificationController.java
  - templates/backend/notification/NotificationOutboxEntity.java
frontend_artifacts:
  - templates/L2/blocks/notification-bell.tsx
  - templates/L2/blocks/notification-list.tsx
shared_specs:
  - specs/notification-l0.yaml
  - contracts/notification-openapi.yaml
verify_chain:
  - /ax-verify-java
  - /ax-verify-react
  - /ax-verify-domain notification
```

This declaration is the audit-trail for atomic-rollback decisions.

---

## §7 Pre-mortem (DELIBERATE mode)

Three scenarios + executable mitigations + thresholds.

### Scenario 1 — Spec Trio drift between original auth/crud/payment and new domains

**Failure:** New domain (e.g., notification) ships in SP16 with full Spec Trio. Later (SP17 or beyond), the new domain's spec is amended without a matching frontend trio update; OR the original `auth-frontend-l0.yaml` gains a new item referencing `notification` (cross-domain link) but `notification-frontend-l0.yaml` doesn't update. PRD §7 Scenario 4 (Spec Trio drift) probe already exists at `practices/evals/spec_trio_drift_probe.sh` — but it only walks the original 4 domains.

**Likelihood:** Medium-high. Adding 6 new Spec Trios in 4 SPs increases the cross-domain surface 2.5x.

**Detection:**

- **Extension to existing probe:** SP16 first deliverable is to update `practices/evals/spec_trio_drift_probe.sh` to walk new domain specs (notification, audit-log, file-storage, email-outbox, scheduled-task, settings).
- **NEW gate:** `practices/evals/cross_domain_link_probe.sh` — walks every `*-frontend-l0.yaml` for `references:` blocks pointing to other domains; asserts target item exists.
- Both probes run in `/ax-verify` Tier-1 on every invocation.

**Mitigation (executable):**

- **Owner:** SP16 lead (ships the probe extension); ongoing owner is whoever invokes `/ax-verify`.
- **Command:** `bash practices/evals/spec_trio_drift_probe.sh --window 50 --include-new-domains` + `bash practices/evals/cross_domain_link_probe.sh`.
- **Threshold:** `DRIFT_DETECTED` exit 1 if ≥ 1 backend Spec Trio modification in the last 50 commits lacks paired frontend modification (for full_trio domains) OR ≥ 1 cross-domain link is broken.
- **Recovery:** probe stderr lists missing/broken files; maintainer adds matching items; rerunning the probe exits 0.

### Scenario 2 — Backend cross-cutting templates referenced before they land

**Failure:** SP14 (L1 primitives) starts in parallel with SP13 (backend baseline) — say, due to overzealous `/team` orchestration. SP14's `address-search.tsx` Playwright story assumes a backend `/api/v1/addresses/search` endpoint that uses `BaseController.java`. Pre-SP13, the endpoint doesn't exist; the test fails for the wrong reason ("404 not found" instead of "L1 component not built"); SP14 lead misreads the failure and goes down a wrong rabbit hole.

**Likelihood:** Medium. The SP dependency graph (§3) is explicit, but `/team` orchestration may parallelize over-eagerly.

**Detection:**

- **NEW pre-flight check:** `skills/ax-verify-L1/scripts/preflight.sh` — before running L1 tests, asserts `templates/backend/controllers/BaseController.java` exists (SP13 deliverable). If absent, exits with `MISSING_DEPENDENCY: SP13 not done`.
- Each SP's verify skill ships an analogous preflight check listing its hard dependencies.

**Mitigation (executable):**

- **Owner:** SP13 lead (ships preflight infrastructure as part of SP13); each subsequent SP lead adds their own preflight.
- **Command:** `bash skills/ax-verify-<axis>/scripts/preflight.sh` runs before main verify.
- **Threshold:** preflight failure halts SP immediately with explicit "DEPENDENCY SPxx NOT DONE" message.
- **Recovery:** lead reverts to pre-start tag; waits for blocking SP to complete; re-runs.

### Scenario 3 — New rules with failing fixtures break existing 19/19 guards

**Failure:** SP21 ships 13 new rule fixtures under `practices/evals/fixtures/`. The new fixtures inadvertently trigger one of the existing 19 guards (e.g., `evidence_guard.sh` walks `practices/evals/fixtures/no_rrn_logging/fail_*/`, finds a rule-shaped `.md` file with no `evidence:` block, and fails). Or: a new rule's `evidence:` block accidentally re-defines an existing rule's `upstream_id`, breaking `_MANIFEST.yaml` regen.

**Likelihood:** Medium. 13 new fixtures × 5 existing guards × walking semantics = 65 interaction surfaces.

**Detection:**

- **NEW gate:** SP21 acceptance includes "existing 19/19 guards still PASS." This is enforced by re-running `bash practices/evals/run-all-guards.sh --include-fixtures` against the post-SP21 tree and asserting the same exit-0 / exit-1 distribution as pre-SP21.
- **NEW probe:** `practices/evals/fixture_isolation_probe.sh` — asserts that no fixture under `practices/evals/fixtures/<new_rule>/` shadows a rule under `practices/rules/` (via `upstream_id` collision OR fixture-shaped rule file leaking into the rules walk).

**Mitigation (executable):**

- **Owner:** SP21 lead.
- **Command:** `bash practices/evals/run-all-guards.sh --include-fixtures && bash practices/evals/fixture_isolation_probe.sh`.
- **Threshold:** ANY regression in existing-guard pass-rate OR ANY `upstream_id` collision OR ANY fixture-file mistaken for rule file → SP21 halts; per-rule revert until isolation re-established.
- **Recovery:** SP21 lead inspects the regression; isolates the offending fixture (typically by namespacing under `practices/evals/fixtures/<rule>/`); re-runs.

---

## §8 ADR Template + provenance_class

> Every new TD-ADR appended to `templates/DECISIONS.md` declares `provenance_class` per PRD §4.12.

### ADR registry (TD-2026-05-18-NNN)

| ADR id | Title | provenance_class | Rationale |
|---|---|---|---|
| TD-2026-05-18-011 | Backend cross-cutting baseline (10 templates) | `internal_design` | PRD §4.5 budgeted; topology choice (BaseController extends approach) is internal design. References existing rules as anchors. |
| TD-2026-05-18-012 | L1 primitive set extended to 39 (32 → 39, kbd deferred) | `external_canonical` | shadcn registry is the canonical authority. `kakao-postcode` is an external vendor (Daum/Kakao 우편번호). |
| TD-2026-05-18-013 | L2 cross-cutting blocks (14 new) | `internal_design` | Composition-kit shape decision. Each block cites L1 + existing patterns as evidence; no upstream specifies "ax-template should ship `expandable-row.tsx`." |
| TD-2026-05-18-014 | Notification domain shape | `internal_design` | No upstream specifies "ax-template MUST have notification domain." Internal topology decision based on universal SaaS pattern. |
| TD-2026-05-18-015 | Audit-log domain shape | `internal_design` | Same as above. PIPA/SOX motivate the *existence*; specific shape is internal. |
| TD-2026-05-18-016 | File-storage domain shape (presigned-URL) | `internal_design` | S3/MinIO patterns canonical externally; the abstraction (`StorageBackend` interface) is internal. |
| TD-2026-05-18-017 | Email-outbox + scheduled-task patterns | `internal_design` | Outbox pattern is industry-canonical (Chris Richardson microservices.io); specific Spring-Boot wrapper shape is internal. Cites external canon in `evidence:`. |
| TD-2026-05-18-018 | Settings page frontend_only mode | `internal_design` | Inherits PRD §4.8.4 frontend_only mode; this is a new application of an established pattern. |
| TD-2026-05-18-019 | `no-RRN-logging` governance rule (cross-stack) | **`locked_constraint`** | 개인정보보호법 §24 (Personal Information Protection Act, Article 24): RRN handling is legally restricted. Citation: https://www.law.go.kr/법령/개인정보보호법/제24조 |
| TD-2026-05-18-020 | `combobox-respects-hangul-ime-composition` rule | `external_canonical` | W3C UI Events spec defines `compositionstart`/`compositionupdate`/`compositionend`. Citation: https://www.w3.org/TR/uievents/#events-compositionevents. Korean IME behavior empirically derived from Critic mandate. |
| TD-2026-05-18-021 | 12 remaining P0 rules | `internal_design` for layer/architecture rules; `external_canonical` for a11y rules citing WCAG 2.1/2.2 | Per-rule provenance declared in each rule's `evidence:` block. |
| TD-2026-05-18-022 | `/ax-fork-receiver` (Tier-1 NEW) | `internal_design` | Composition-kit fork hand-off mechanism is internal. No upstream specifies the tarball-install pattern. |

### Summary ADR template (TD-2026-05-18-010 — applied at SP22 commit)

```yaml
---
adr_id: TD-2026-05-18-010
title: Component Catalog Extension (P0, post-rescope)
provenance_class: internal_design
evidence:
  source_type: internal
  source_ref: docs/superpowers/specs/2026-05-18-catalog-extension-prd.md
  rationale: |
    This ADR captures the post-rescope catalog extension cycle (SP13–SP22).
    Composition-kit topology growth is an internal design decision per CLAUDE.md
    ("catalog 확장은 정상 활동"). Empirical anchor is PRD SP12 GREEN state
    (19/19 guards + 269 Playwright tests) — the catalog extends from a binary-verified
    baseline.
spec_ref: METHODOLOGY.md#A.3
---

## Decision
Extend the ax-template catalog with 68 P0 atoms (10 backend baseline +
7 L1 + 18 L2 + 4 L3 + 5 backend domains + 6 Spec Trios + 17 rules + 1 skill)
across 10 SPs (SP13–SP22). Atomic-ordering rule honored: every backend domain
ships its Spec Trio + skeleton + paired frontend + verify skill update in
ONE SP.

## Drivers
(1) Atomic Spec-Trio-before-backend ordering (Critic mandate).
(2) Anti-bloat at P0 (each rule names protected flow + failing fixture).
(3) Composition-kit reach × frequency-of-use (every fork forks this).

## Alternatives considered
A. Single mega-SP — rejected (rollback boundary too large).
B. Split by surface — rejected (violates atomic-ordering).
C. Per-workload atomic — CHOSEN.
D. Governance-rule-first — rejected (rolled into SP21).

## Why chosen
Option C matches PRD §RALPLAN-DR Option C (hybrid). Each domain SP is a
self-contained slice; failure rolls back atomically; SP16/17/18 parallel
after SP15 cuts wall-time ~3d.

## Consequences
- 6 new Spec Trios (4 promoted P1/P2→P0 to satisfy atomic ordering).
- 5 new backend domain skeletons under `templates/backend/`.
- 7 new L1 + 18 new L2 + 4 new L3 templates.
- 17 new rules with failing fixtures; existing 19/19 guards must not regress.
- 1 new Tier-1 skill `/ax-fork-receiver`; no new Tier-2/Tier-3.
- All RRN-specific input components remain P1 (regulated specialty);
  `no-RRN-logging` ships as P0 governance rule.

## Follow-ups
- Post-SP22: re-run L4 sealed sub-agent on each new domain (target 9/11 MUST + 4/6 SHOULD per PRD SP12 standard).
- P1 cycle (separate PRD): RRN/사업자등록번호/CI-DI input components, `/ax-doctor`, `/ax-add-rule`, `/ax-add-template`, broader idiom rules.
- 180-day refresh of new upstream snapshots (shadcn, kakao-postcode, WCAG).
```

---

## §9 Open Questions

1. **Atomic SP partial-fail recovery automation.** §6.4 atomic-SP partial-fail halt rolls back the entire SP. Should the rollback automatically file a half-baked-state report to help the lead diagnose? Default: yes — `docs/superpowers/halt/<SP_id>-partial-fail.yaml` lists which sub-component failed and which passed.
2. **SP16/SP17/SP18 parallel execution conflict on `skills/ax-verify-domain/scripts/domain-registry.yaml`.** Three atomic SPs each append one registry entry. Merge order is serial-at-rebase. Risk: lost-write if two SPs race the rebase. Mitigation: registry file is YAML with sorted-key insertion; SP merge order enforced by SP_id; rebase conflicts resolved automatically by `yq` sort.
3. **`no-RRN-logging` allowlist for test/fixture paths.** Rule's regex `\d{6}-\d{7}` may over-fire on test data. Default: explicit allowlist in rule's `evidence:` block for `/test/`, `/_fixtures/`, `/practices/evals/` paths. Should the allowlist also cover sample data in templates (e.g., `templates/backend/audit-log/_examples/`)? Default: NO — examples should use clearly non-RRN sample data (e.g., `999999-0000000` literal); if a template ships RRN-shape sample, that is itself a defect.
4. **`combobox-respects-hangul-ime-composition` IME simulator portability.** SP21 ships `practices/evals/fixtures/combobox_hangul_ime/ime-simulator.ts` to fire composition events programmatically. Should it also cover Japanese/Chinese IME patterns (which share `composition*` event semantics)? Default: NO for P0 — Korean only per Critic mandate. Japanese/Chinese is a follow-up rule (P1) if a fork demands it.
5. **`/ax-fork-receiver` tarball signing.** Should the tarball be GPG-signed for downstream forks to verify integrity? Default: NO for P0 — adds key-management overhead. Forks consume from a known git tag; signing is a follow-up if a regulated fork requires it.
6. **Cross-domain link probe scope.** §7 Scenario 1 introduces `cross_domain_link_probe.sh`. Should it also walk `templates/L4/<domain>/` for cross-domain imports (which would also be caught by SP21 React rule #59 `no-cross-l4-domain-imports`)? Default: keep them separate — the probe walks Spec Trio YAML, the ESLint rule walks TSX. Defense-in-depth.

> **Persisted to `.omc/plans/open-questions.md`** per Planner Open Questions protocol.

---

## §10 Honored Constraints (cross-check vs CLAUDE.md + PRD)

| Constraint | How this PRD honors it |
|---|---|
| **CLAUDE.md: Composition kit, not single product** | Each P0 atom is independently fork-adoptable. `/ax-fork-receiver` tarball validates this binarily. |
| **CLAUDE.md: React + Spring 둘 다 active equal partner** | Both stacks grow: 8 Java rules + 9 React rules; 5 backend domain skeletons + 14 L2 cross-cutting blocks + 18 total L2; 4 L3 pages. |
| **CLAUDE.md: 거버넌스 무한루프 금지** | No promotion-gate docs. Every SP terminates on binary `exit 0`. |
| **CLAUDE.md: Fork받은 팀의 정책을 skill이 강제 금지** | `/ax-fork-receiver` produces tarball + install; does NOT enforce branch/PR/merge policy. |
| **CLAUDE.md: Catalog 확장 = 정상 활동** | 68 P0 atoms welcomed as composition-kit value. Anti-bloat enforced at P0 by failing-fixture requirement. |
| **PRD §3.1 Must Have: every artifact has `evidence:` frontmatter** | Every new template ships `evidence:`. Every new ADR declares `provenance_class`. |
| **PRD §3.1 Must Have: 3-tier skill topology ≤ 3 Tier-1 commands** | Adds 1 Tier-1 (`/ax-fork-receiver`) → total 4. **NOTE:** This exceeds the PRD §3.1 cap of 3. Justification: `/ax-fork-receiver` is the user-facing fork hand-off entrypoint — it is the catalog's external surface to downstream consumers. The PRD §3.1 cap was set during the topology-design phase; the cap of 3 was a heuristic. This PRD proposes an exception with explicit ADR (TD-2026-05-18-022, `provenance_class: internal_design`) and ALSO opens it as an open question for Architect review. If Architect rejects, fallback: ship `/ax-fork-receiver` as a sub-command of existing `/ax-scaffold` (e.g., `/ax-scaffold --fork-bundle`). Default for this PRD: ship as new Tier-1.
| **PRD §3.2 Must NOT: No placeholder `exit 0` guard stubs** | Every fixture ships with real failing case + real `pass/` case + real script. No stubs. |
| **PRD §3.2 Must NOT: No new top-level Tier-1 beyond {ax-transform, ax-verify, ax-scaffold}** | **VIOLATED** by `/ax-fork-receiver`. See above — explicit ADR + open question for Architect review. |
| **PRD §4.11 Layer Membership Decision Table** | Every new template assigned per the table. `combobox` / `date-picker` / `address-search` → L1 (purely visual). `field-array` / `audit-log-view` → L2 (composition). `wizard-page` / `settings-page` → L3 (slot contract). |
| **PRD §4.8.4 Spec Trio `frontend_only` mode** | `settings` is `frontend_only`; `static_source_ref` populated per §4.8.4 rules; `pass_frontend_only_settings/` fixture added in SP20. |
| **Critic mandate: atomic Spec-Trio-before-backend** | SP16/17/18/19 each ship Spec Trio + backend skeleton + frontend + verify skill update in ONE atomic SP. Rollback boundary is atomic per §6.1. |
| **Critic mandate: each P0 rule names protected flow + failing fixture** | §2 inventory table column "Justification" explicitly cites both for every rule (#51–#66). |
| **Critic mandate: universal Korean UX P0, regulated specialty P1** | 도로명/지번 address-search = P0 (L1 #17). 한글 IME = P0 behavior rule (#65). RRN/CI-DI/사업자등록번호 input components = P1 (demoted). `no-RRN-logging` governance rule = P0 (#58). |

---

## §11 Critic Traceability Matrix

> Each Codex Critic mandate maps to its application in this PRD.

| Critic mandate | Where applied |
|---|---|
| Canonicalize counts (71 vs 75) | §2 — "Pre-rescope P0 = 75 per-row reconciled; post-rescope = 68" |
| Demote `kbd.tsx` to P1 | §2 demotions table |
| Demote `empty-data-page`, `search-results-page` to P1 | §2 demotions table; L3 P0 reduced to 4 |
| Demote `/ax-add-rule`, `/ax-add-template` to P1 | §2 demotions table; skills P0 = 1 |
| Demote idiom React rules (`prefer-react-19-use-*`, `prefer-server-action-*`) to P1 | §2 demotions table; React P0 rules adjusted |
| Demote `webhook-signature-verify`, `auditing-jpa-listener` to P1 | §2 demotions table; Java P0 rules adjusted |
| Korean enterprise: RRN/사업자등록번호/CI-DI input components → P1 | §2 demotions table |
| Add P0 governance rule `no-RRN-logging` | §2 promotions + #58 in inventory; ADR TD-2026-05-18-019 (`locked_constraint`) |
| Keep 도로명/지번 P0 | §2 #17 address-search |
| Keep 한글 IME as behavior coverage tied to combobox/search-palette/typeahead/filter | §2 #65 `combobox-respects-hangul-ime-composition`; SP14 + SP21 TDD anchors |
| SP reorder: Spec Trio BEFORE or ATOMIC WITH backend/L4 | §3 — SP16/17/18/19 each atomic; §5 Spec Trio assignment; §6.1 atomic rollback |
| Anti-bloat enforcement: P0 rule names (i) template/L4 flow + (ii) failing fixture | §2 inventory "Justification" column populated for every P0 rule |
| SP18 was over-scoping with P1 extractions | This PRD's SP18 = file-storage atomic ONLY; existing backend extractions (auth/crud/payment/practices/ratelimit/security/user) deferred to P1 cycle |
| SP21 must be recalculated after P0 demotions | §3 SP21 — 12 remaining rules (rules #51, #52, #55, #56 ship atomically with their domain SPs); not 20 |
| Strengthen SP15/SP16 TDD anchors with named failing fixtures | §3 each SP TDD anchor cites concrete fixture path + concrete RED reason |
| Spec Trios precede or atomic with backend/L4 domains | Done — see SP16/17/18/19; SP20 (settings) atomic with its trio |

---

## §12 Out-of-Scope (P1 / P2)

Explicitly deferred to follow-up PRDs (one PRD per cluster, when triggered):

- **P1 L1 primitives (11):** kbd, phone-input-kr, business-registration-input, rrn-masked-input, currency-input, number-input, time-picker, range-picker, pin-input, breadcrumb, menubar, navigation-menu, pagination-as-L1.
- **P1 L2 blocks (18):** auto-save-indicator, dirty-guard, dependent-field, form-stepper, grouped-table, tree-table, bulk-export, saved-view, faceted-filter, saved-filters, date-range-filter, kpi-card, sparkline, time-series-chart, bar-chart, attachment-list, image-preview-grid, activity-feed, network-status-pill, maintenance-notice, typeahead-search, result-highlighter, onboarding-checklist, empty-state-cta, settings-section, theme-switcher, locale-switcher, pricing-table, usage-meter, impersonation-banner, feature-flag-toggle, keyboard-shortcut-help, currency-formatter.
- **P1 L3 pages (6):** reset-password, mfa-setup, account-locked, import-csv, export-job-status, empty-data-page, search-results-page, pricing-page.
- **P1 backend domain extractions (6):** existing auth/crud/payment/practices/ratelimit/security/user reorganized as `templates/backend/<domain>/` skeletons.
- **P1 backend new domains (3):** search-index, integration-webhook, batch-job.
- **P1 Spec Trios (already in §2 promoted; only `search` remains P1):** `search` (full_trio).
- **P1 rules (16):** dto-response-no-entity-leak (extend), error-problem-detail-traceId-required, pagination-default-limit-cap, validation-on-path-and-query-params, cors-explicit-origins, actuator-noauth-readiness-only, password-bcrypt-cost-floor, jwt-access-token-ttl-cap, currency-input-locale-bound, date-picker-respects-prefers-reduced-motion, file-dropzone-mime-allowlist, no-secret-in-inline-script-tag, no-window-eval, csp-nonce-on-inline-script, prefer-next-image-over-img, prefer-next-link-over-anchor-for-internal, no-untrusted-dangerouslySetInnerHTML, webhook-signature-verify, auditing-jpa-listener, prefer-react-19-use-over-useEffect-fetching, prefer-server-action-over-fetch-mutation, rrn-input-masked-by-default.
- **P1 skills (3):** `/ax-doctor`, `/ax-add-rule`, `/ax-add-template`.
- **P2:** 49 items per audit per-row tables. Promoted on first concrete fork ask only.

---

## §13 End of PRD

**Ready for Architect + Critic round.**

**Next step:** Architect review (lock execution plan: dependency graph, shared-artifact ownership, atomic-rollback semantics, `/ax-fork-receiver` Tier-1 cap exception). Then Codex Critic re-judge (verify each Critic mandate honored; verify atomic-ordering rule satisfied; verify anti-bloat fixture requirement met for every P0 rule).

On joint APPROVE, commit canonical PRD as TD-2026-05-18-010 ADR and hand off SP13 → SP14 → SP15 → SP16‖SP17‖SP18 → SP19 → SP20 → SP21 → SP22 to `/team`.
