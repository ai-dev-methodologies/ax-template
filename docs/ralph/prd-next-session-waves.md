# PRD — Next Session R-Catalog Backlog (Wave A/B/C/D/E)

**Author**: 2026-05-26 session end (R50→R89 + deslop pushed; HEAD = `9abd90c`).
**Baseline state**:
- 108 rules · 42 guards · 20 L4 + 1 backend-only (identity-verification)
- L0 fork-receiver-kit + backend `common` package + L2 rate-limit-banner
- 8 backend modules with PII discipline
- 1 dogfood ledger entry (email-outbox-iter1)

**Execution mode**: `/oh-my-claudecode:ralph --critic codex` per wave.

**Lessons preempted day-one (from this session's `feedback_*` memories)**:
1. Don't edit catalog files (`practices/`, `specs/`, `templates/L4/*/app/`, `backend/src/main/java/com/ax/template/authblueprint/`) while a background `verify-completion.sh` is running — the verify scans working tree, not just HEAD; mid-edit catches spurious FAIL and blocks recency.
2. Spec.domain_mode discipline (R58/R59 guard) — never create `templates/L4/<domain>/app/` for `backend_only` or domain_mode-absent specs. R59 guard refuses it mechanically; respect the refusal.
3. Rule of three+ → lift in same commit (R80). When a helper reaches 3 adopters, the third commit lifts it (or records explicit deferral with expiry trigger).
4. ralplan master plans can be wrong — always cross-reference each L4 in scope against `specs/<domain>-l0.yaml#domain_mode` before creating frontend.

---

## Wave A — Catalog hardening (4 commits, rule→guard pattern continued)

### A1: R81 stored-error column auto-detection guard

**Subject**: Mechanical companion to R61 `server-side-stored-error-sanitize`. Scans `backend/src/main/java` for entities with `last_error` / `error_message` / `failure_reason` columns and verifies every write site is wrapped in `AuditPiiHelper.sanitizeReason(...)`.

**Files**:
- `practices/evals/stored_error_column_sanitize_guard.sh` (new — 43rd hard guard)
- `practices/evals/run-all-guards.sh` register as step [40]

**Acceptance**:
- Lists every entity field annotated `@Column(name = "last_error" | "error_message" | ...)`.
- For each, scans for setter / mutator method that assigns to the field.
- Verifies the assignment expression contains `AuditPiiHelper.sanitizeReason(` or the field is marked `@PiiSanitized` (introduce annotation as escape hatch).
- 43/43 PASS at HEAD (R63 closed all 5 known sites; new entity should also be sanitized).
- Synthetic regression: temporarily add `this.lastError = ex.getMessage();` without sanitize → guard exits 1.

### A2: R82 background-poll mutation aria-busy + dataUpdatedAt rule

**Subject**: Expands R50 `incident-dashboard-background-poll-plus-refresh`. Any TanStack Query `useQuery` with `refetchInterval` MUST also expose `dataUpdatedAt` to the UI via a visible timestamp + a Refresh button, AND any mutation triggered from a background-polled page MUST set `aria-busy` on the button until settled. R51 email-outbox page already does this; R55 favorites adds notes form that doesn't.

**Files**:
- `practices/rules/background-poll-must-show-refresh-state.md` (new — 109th rule, HIGH impact)

**Acceptance**:
- Rule with 2 external evidence entries (TanStack Query docs + WCAG SC 4.1.3 status messages).
- Incorrect/Correct sections with substantive TSX code (>= 2 lines each).
- AGENTS.md regenerates idempotently (108 → 109 rules).
- 42/42 guards PASS (no new mechanical guard yet — text rule first per R58→R59 cadence; mechanical guard is A3).

### A3: R82b mechanical guard for R82 (44th hard guard)

**Subject**: Grep-based companion to R82 — scans `templates/L4/**/*.tsx` for `useQuery({...refetchInterval: ...})` calls without sibling `dataUpdatedAt` reference in the same function. Bash 3.2 compat (matches existing 42 guard styles).

**Files**:
- `practices/evals/background_poll_refresh_state_guard.sh` (new — 44th hard guard)
- `practices/evals/run-all-guards.sh` register as step [41]

**Acceptance**:
- 44/44 PASS at HEAD (R51 email-outbox page already shows dataUpdatedAt; R55 favorites doesn't have refetchInterval).
- Synthetic regression: insert `refetchInterval: 10000` into any useQuery without dataUpdatedAt nearby → guard exits 1.

### A4: R83b — extend R83 to catch dynamic role assignment

**Subject**: R83 catches `const isAdmin = true` (static). It misses `const r = computeRole(); const isAdmin = r === undefined`. Add a second pass that detects "isAdmin / hasXRole computed from a non-useCallerRole source".

**Files**:
- `practices/evals/l4_role_default_failclosed_guard.sh` (extend) OR new `l4_role_dynamic_failclosed_guard.sh`

**Acceptance**:
- Detects `const has*Role = <something> === <something>` where the LHS of `===` is NOT `useCallerRole()` or `callerRole`.
- Live PASS at HEAD (R75 closure used the correct pattern).
- Synthetic regression: insert `const isAdmin = userType === 'admin'` → guard exits 1.

---

## Wave B — Dogfood iter2+ (3 commits, catalog quality validation)

### B1: R77 email-outbox dogfood iter2

**Subject**: 2-persona protocol per CLAUDE.md (P1 ops 정민영 + P2 security 이주형). iter1 closed 8 findings (7 real_bug + 4 scope_deferral). iter2 looks for residuals after R63 PII sweep + R67 helper lift + R71 ledger.

**Files**:
- `docs/dogfood-ledger/email-outbox-iter2.yaml` (new) — classified findings per ledger guard schema
- Per-finding code/spec changes (estimated 3-5 findings; mostly low/medium)

**Acceptance**:
- iter2 ledger entry with `iteration: 2` + `findings:` block
- All HIGH/MEDIUM findings closed in the same wave
- LOW findings classified as `scope_deferral` with expiry triggers
- testEmailOutbox + 42/42 guards remain GREEN
- dogfood_ledger_guard PASS (2-consecutive-all-deferral check ok because iter1 had real_bug closures)

### B2: R78 favorites-bookmarks dogfood iter1 (formal)

**Subject**: R55 was a refresh + R56 adoption, not formal iter1 dogfood. R78 runs the proper protocol — 2 personas review favorites end-to-end (page + toggle + count + quota + note form + L2 confirm-dialog).

**Files**:
- `docs/dogfood-ledger/favorites-bookmarks-iter1.yaml`
- Per-finding fixes

**Acceptance**:
- Inventory: estimated 4-7 findings (HIGH 1-2 / MEDIUM 2-3 / LOW 2)
- All HIGH/MEDIUM closed; LOW deferred with expiry
- testFavorites GREEN; 42+/42+ guards PASS

### B3: R79 activity-feed dogfood iter1

**Subject**: ActivityService surface (publish / read / mark-read / mark-all-read / audience visibility / BULK_MARK_READ audit). R62 closed the audit-pii lesson. R79 broader audit posture review.

**Files**:
- `docs/dogfood-ledger/activity-feed-iter1.yaml`
- Per-finding fixes

**Acceptance**:
- Inventory: 4-6 findings expected
- All HIGH/MEDIUM closed
- testActivityFeed GREEN; guards remain PASS

---

## Wave C — Fork-receiver experience (3 commits)

### C1: R90 @ax/eslint-plugin-ax extension for R47/R50/R58/R61

**Subject**: 7 current ESLint rules cover ~6 patterns. New rules to enforce R47 fail-closed defaults, R50 destructive-action confirms, R58 spec.domain_mode discipline, R61 audit-pii-hash discipline at build time (not just runtime / git-hook).

**Files**:
- `practices-react/eslint-plugin-ax/src/rules/no-fail-open-role-default.ts`
- `practices-react/eslint-plugin-ax/src/rules/destructive-action-requires-confirm.ts`
- `practices-react/eslint-plugin-ax/src/rules/spec-domain-mode-required.ts`
- `practices-react/eslint-plugin-ax/src/rules/audit-log-pii-hash-required.ts`
- `practices-react/eslint-plugin-ax/src/index.ts` (register 4 new rules)
- 4 RuleTester suites

**Acceptance**:
- 7 → 11 ESLint rules
- All 4 new rules pass RuleTester (valid + invalid cases)
- Existing 7 RuleTester suites still GREEN
- README updated

### C2: R91 /ax-fork-receiver bundle smoke test for L0 + backend common

**Subject**: R53 added L0; R67 added backend common. Verify the `/ax-fork-receiver` bundle includes both layers so a fork-receiver who runs `bash skills/ax-fork-receiver/scripts/bundle.sh` then `bash skills/ax-fork-receiver/scripts/smoke.sh` against the tarball confirms L0 + common are present.

**Files**:
- `skills/ax-fork-receiver/scripts/smoke.sh` extended with L0 + common verification
- Sample fork-receiver smoke output captured in `docs/fork-receiver-smoke-sample.txt`

**Acceptance**:
- Smoke script checks `templates/L0/fork-receiver-kit/` + `backend/src/main/java/com/ax/template/authblueprint/common/` exist in the bundle
- Smoke exit 0 against current bundle
- Catalog ships an explicit "L0 + common are NOT optional for fork-receivers who use any post-R53 L4" note in MAINTAINER.md

### C3: R92 sample fork-receiver tutorial

**Subject**: `docs/fork-receiver-tutorial.md` walks through: fork ax-template → run `/ax-transform` → add one new domain via 5-step METHODOLOGY playbook → integrate L0 + common → wire CI with 44 guards. End-to-end onboarding.

**Files**:
- `docs/fork-receiver-tutorial.md` (new, ~300 lines)

**Acceptance**:
- Tutorial covers all 6 steps of fork-receiver onboarding
- References R87 METHODOLOGY cross-cutting layers section
- Sample commands work end-to-end (test against a scratch fork)
- Links to specific rule files where decisions need user input

---

## Wave D — Deferred F-items (2 commits)

### D1: F5 frontend next-retry countdown UX

**Subject**: R60 dogfood iter1 LOW finding. email-outbox admin page shows `nextAttemptAt` as absolute timestamp ("next retry at 18:42:13") — UX would benefit from countdown ("retries in 3m 12s") with auto-tick.

**Files**:
- `templates/L4/email-outbox/app/(admin)/email-outbox/page.tsx` — add countdown helper component for nextAttemptAt rows

**Acceptance**:
- Countdown updates every second on visible RETRY rows
- Stops ticking when row leaves RETRY status
- Aria-live=polite on the countdown text (WCAG SC 4.1.3)
- 44/44 guards PASS

### D2: F10 EmailTemplate versioning

**Subject**: R60 dogfood iter1 LOW finding. EmailTemplate has no version column → updates silently change every future render. Add `version: int` column + immutable history.

**Files**:
- `backend/src/main/java/com/ax/template/authblueprint/emailoutbox/EmailTemplate.java` (add version field)
- `backend/src/main/java/com/ax/template/authblueprint/emailoutbox/EmailTemplateHistory.java` (new, immutable snapshot)
- `backend/src/main/resources/db/migration/V027__add_email_template_version_history.sql`
- EmailTemplateService writes a history row on every template update
- Tests for version increment + history immutability

**Acceptance**:
- testEmailOutbox + new EmailTemplateHistory tests GREEN
- 44/44 guards PASS

---

## Wave E — future_add L4 promotion (4 sub-ralplans, NOT executed in one session)

Each future_add L4 needs its own sub-ralplan because each requires:
- Spec Trio creation (specs/<domain>-l0.yaml + contracts/<domain>-openapi.yaml + blueprints/<domain>-manifest.yaml)
- Backend impl (entity + repository + service + controller + tests)
- Frontend trio (per L4 pattern)
- Classification YAML promotion (future_add → selectable)
- generate_agents.sh L4 count assertion bump
- Recipe wiring (which recipes include this L4)

### E1: ratelimit L4 — lightest (backend already exists)

**Subject**: `backend/src/main/java/com/ax/template/authblueprint/ratelimit/` already has the RateLimitFilter + counter store. testRateLimit GREEN. Missing: spec.domain_mode declaration + templates/L4/ratelimit/ frontend admin page + classification YAML promotion.

**Scope**: 1 sub-ralplan, ~5 commits (spec + frontend + classification + AGENTS regen + adoption).

### E2: i18n-policy L4 — medium

**Subject**: Korean enterprise default + multi-locale support. Spec items: locale detection (Accept-Language + URL prefix), currency formatting (KRW / USD per market), RTL policy enforcement (Arabic / Hebrew support gate).

**Scope**: 1 sub-ralplan, ~8 commits (spec Trio + backend + frontend + tests + classification).

### E3: multi-tenant L4 — heavy

**Subject**: Runtime tenant isolation. Currently `specs/multi-tenant-l0.yaml` exists with policy items but no L4 disk implementation. Each existing L4 declares `tenant_model: single` in README; multi-tenant L4 would ship the Hibernate filter / schema-per-tenant runtime so b2b-admin recipe can flip to `tenant_model: multi`.

**Scope**: 1 sub-ralplan, ~10-15 commits + significant test surface (tenant isolation tests across every L4).

### E4: realtime-policy L4 — medium

**Subject**: WebSocket / SSE channel auth + back-pressure. Spec items: channel auth (JWT subprotocol), reconnection backoff, message ordering guarantees.

**Scope**: 1 sub-ralplan, ~8 commits.

---

## Suggested execution order for next session

**Day 1** (4-6 hours):
- Wave A all (A1 → A2 → A3 → A4) — 4 commits, low-risk mechanical work
- Wave B B1 (email-outbox iter2) — 1 commit (ledger + findings; assume light)
- Stopping point: 5 commits, 42 → 44 guards, 108 → 109 rules

**Day 2** (4-6 hours):
- Wave B B2 + B3 — 2 commits
- Wave C C1 (ESLint plugin extension) — 1 commit
- Stopping point: 3 commits, 7 → 11 ESLint rules

**Day 3+** (per session):
- Wave C C2 + C3 — 2 commits
- Wave D D1 + D2 — 2 commits

**Future sessions**:
- Wave E (each L4 a separate sub-ralplan, NOT batched)

---

## Out of scope for next session (defer further)

- Catalog rule for "fork-receiver-kit must ship from /ax-fork-receiver bundle" (depends on R91 first)
- Backend `common` package promotion of more utilities (no current 3+ adoption candidate beyond AuditPiiHelper)
- Performance benchmark suite for L4 backends (separate project)
- i18n/multi-tenant L4 promotion as a single ralplan (each is a sub-ralplan; bundling risks scope explosion)

---

## Pre-mortem (high-risk scenarios for next session)

1. **A1 stored-error guard false positive on non-PII columns** — mitigation: opt-in via `@PiiSanitized` annotation; new fields must annotate or sanitize.
2. **B2 favorites dogfood surfaces a HIGH security finding** — mitigation: pause wave, address finding, then resume.
3. **C1 ESLint plugin adds rules that already-shipped templates violate** — mitigation: run plugin against all templates/L4/* before commit; fix violations in same commit.
4. **E1 ratelimit L4 promotion conflicts with existing backend ratelimit module** — mitigation: backend module stays; new templates/L4/ratelimit/ adds frontend trio only + classification promotion.

---

## State expected at next session start

- `origin/main = 9abd90c` (or further if interim work landed)
- 108 rules / 42 guards / 20 L4
- Memory files updated with R50-R75 arc + R76-R89 ralph wave learnings
- `.omc/prd.json` from this session marked all 9 stories `passes: true`
- This PRD at `docs/ralph/prd-next-session-waves.md` ready as ralph input
