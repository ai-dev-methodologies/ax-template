# Implementation Status — 20 L4 Domains (R63 snapshot)

> **Fork-receiver expectation alignment.** This doc closes the gap between catalog promises and runnable code. Persona simulation (R15 옵션A) revealed that fork-receivers consistently confuse `templates/L4/<domain>/` (catalog reference template + Next.js stub) with `backend/src/main/java/com/ax/template/authblueprint/<domain>/` (actual Java reference workload). The two layers are different by design — this table makes the boundary explicit.

## Status taxonomy (refreshed for R47+ stub-promotion sequence)

- **full-trio** — Backend Java reference workload at `backend/src/main/java/com/ax/template/authblueprint/<domain>/` + Next.js frontend trio at `templates/L4/<domain>/app/` + Spec Trio. Fork-receiver runs `./gradlew test{Domain}` on day 1 and ships the page directly. The dominant state after R40-R45 + R51 stub promotion.
- **backend-only** — `specs/<domain>-l0.yaml` declares `domain_mode: backend_only`. Backend exists; **no** `templates/L4/<domain>/` directory by design. The catalog refuses to ship a frontend for server-to-server domains (identity-verification: CI/DI callback). See `practices/rules/spec-domain-mode-gates-frontend-trio.md` (R58) + the `l4_frontend_domain_mode_guard.sh` mechanical guard (R59).
- **rules-as-code** — Special INFRA case. The `practices` directory ships as an L4 template for fork-receiver visibility but is not recipe-selectable — it IS the catalog enforcement system.

## 20 L4 status (disk-verified 2026-05-26 — R63)

| L4 domain | Backend Java | Frontend Next.js trio | `./gradlew test{Domain}` | Status |
|---|---|---|---|---|
| activity-feed | ✅ R44 closure | ✅ trio | 18/18 GREEN | **full-trio** |
| api-key | ✅ R30 closure | ✅ trio (R40) | 16/16 GREEN | **full-trio** |
| approval-workflow | ✅ R31 closure | ✅ trio (R43) | 26/26 GREEN | **full-trio** |
| audit-log | ✅ R20 backend | ✅ trio | 11/11 GREEN | **full-trio** |
| auth | ✅ reference workload | ✅ trio | 26 ASVS items | **full-trio** |
| billing | ✅ R21 backend | ✅ trio | 17/17 GREEN | **full-trio** |
| comment-thread | ✅ R36 closure | ✅ trio (R42) | 18/18 GREEN | **full-trio** |
| crud | ✅ reference workload | ✅ trio | 7/7 GREEN | **full-trio** |
| email-outbox | ✅ R51 closure | ✅ trio (R51) | 24/24 GREEN incl PII helper | **full-trio** |
| favorites-bookmarks | ✅ R34 closure | ✅ trio (R46+R55) | 17/17 GREEN | **full-trio** |
| feature-flags | ✅ R20 backend | ✅ trio | 11/11 GREEN | **full-trio** |
| file-storage | ✅ R20 backend | ✅ trio | 12/12 GREEN | **full-trio** |
| notification | ✅ R20 backend | ✅ trio | testNotification GREEN | **full-trio** |
| payment | ✅ reference workload | ✅ trio | 29 items GREEN | **full-trio** |
| practices | ✅ rules-as-code | ✅ trio | 107 rules GREEN | **rules-as-code** |
| scheduled-task | ✅ R20 closure | ✅ trio | 5/5 GREEN | **full-trio** |
| search | ✅ R20 backend | ✅ trio | 8/8 GREEN | **full-trio** |
| session-management | ✅ R33 closure | ✅ trio (R41) | 23/23 GREEN | **full-trio** |
| tag-categorization | ✅ R32 closure | ✅ trio (R45) | 27/27 GREEN | **full-trio** |
| webhook | ✅ R20 closure | ✅ trio | 13/13 GREEN | **full-trio** |

Plus the spec-anchored backend-only domain (NOT on disk under `templates/L4/`):

| Domain | Backend Java | `templates/L4/` | `./gradlew test{Domain}` | Status |
|---|---|---|---|---|
| identity-verification | ✅ R54 closure | ❌ by design | 19/19 GREEN | **backend-only** (spec `domain_mode: backend_only`) |

**Totals:** 20 disk L4 (19 full-trio + 1 rules-as-code) + 1 backend-only spec-anchored domain.

## Shared client primitives (cross-cutting layers)

- **L0 fork-receiver-kit** (`templates/L0/fork-receiver-kit/`) — R53 lift. `use-caller-id.ts` / `parse-error.ts` / `entity-key.ts` shared by every L4 frontend that needs caller identity, RFC 9457 ProblemDetail unwrap with PII deny-list, or polymorphic-entity-ref path-segment validation.
- **L2 blocks** (`templates/L2/blocks/`) — 30+ composable widgets. Notable additions this session: `rate-limit-banner.tsx` (R56 — RFC 6585 §4 / WCAG 2.2 SC 4.1.3) and the existing `confirm-dialog.tsx` (now broadly adopted via R50 destructive-action-confirm pattern).

## What this means for fork-receivers (R63 refresh)

1. **Every L4 ships with backend + frontend on day 1.** The R40-R45 + R51 stub-promotion sequence (April-May 2026) closed the original "spec-only" gap. A recipe like `b2b-admin` activates 13 L4 (auth, crud, audit-log, api-key, approval-workflow, session-management, activity-feed, comment-thread, tag-categorization, favorites-bookmarks, plus feature-flags / search) — all 13 have backend Java + frontend trio + `./gradlew test{Domain}` GREEN.

2. **Sealed verdict PASS validates AI-agent self-discoverability**, not production readiness. Sealed verdicts confirm the catalog can be picked up by a context-0 AI agent. Per-domain `./gradlew test{Domain}` GREEN is the additional binary signal of catalog-level correctness; fork-receivers still own production hardening (load testing, ops runbooks, RBAC integration with their identity provider, etc.).

3. **METHODOLOGY.md 5-step is the playbook for ADDING new domains.** Existing 20 L4 are already through the playbook. For a fork-receiver introducing a 21st domain not in the catalog, follow Spec Trio + TDD + `./gradlew test{Domain}` cycle.

4. **Estimated effort per L4 customization** (composition kit assumes you fork, not greenfield):
   - Wiring identity provider into existing auth module: ~1-2 days
   - Replacing the LoggingEmailSenderService stub (R60 catalog default) with a real SMTP/SES adapter: ~0.5-1 day
   - Domain-specific business rules layered on existing spec items: ~2-5 days per domain
   - **Bias toward customizing existing L4 first**, then adding new domains via the playbook.

## Recipe-by-recipe completeness

See each `recipes/<pattern>/RECIPE.md` for an inline "Backend Implementation Status" table showing exactly which L4 in that recipe are `impl` vs `spec-only` vs `skeleton`.

## Roadmap

- **Done (R20-R63 sequence)**: audit-log / billing / feature-flags / file-storage / notification / search / scheduled-task / webhook backend impl shipped; R29-R36 closed 8 backend-only L4 stubs; R40-R45 + R51 promoted them to full-trio; R47-R63 hardened with dogfood + PII discipline + L0 / L2 layers.
- **Next candidates** (NOT shipped):
  - Additional L4 domains for niche enterprise patterns (i18n-policy, multi-tenant, realtime-policy, ratelimit) reserved in the schema enum via `future_add` tier of `specs/l4-domain-classification.yaml`. Each requires sub-ralplan + Spec Trio + TDD before any disk presence.
  - 2-persona dogfood iter2 on R51 email-outbox (iter1 closed 8 findings via R60; iter2 looks for residuals after the multi-module PII adoption).
  - Generalize EmailPiiHelper into a backend `common` package when a 6th+ module needs it (current adoption: emailoutbox / activityfeed / scheduledtask / reportexport / webhook / auditlog / notification = 7 already). The promotion trigger is now satisfied; deferred for a focused refactor commit.
