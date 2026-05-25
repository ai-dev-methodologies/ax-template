---
pattern: internal-it
display_name: "Internal-IT (Ticketing + SLA + Audit + ITSM Webhook Relay)"
tenant_model: single  # iter-2: explicit declaration per specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001. Recipe ships single-tenant; fork-receivers adopting multi-tenant MUST switch to `tenant_model: multi` AND adopt ISOLATION-001/002/003 + PROPAGATION-001/002.
schema_version: 1
compatible_with_catalog_version: "v1.7.0-webhook-internal-it"
last_verified_at: "2026-05-22"
enabled_l4_domains:
  - audit-log
  - auth
  - crud
  - notification
  - scheduled-task
  - webhook
l2_blocks_used:
  - confirm-dialog
  - crud-create-form
  - crud-edit-form
  - crud-list-adapter
  - data-table
  - filter-bar
  - kpi-card
  - notification-bell
  - notification-list
l3_pages_used:
  - create-page
  - dashboard-page
  - detail-page
  - edit-page
  - list-page
override_allowed:
  # Inline override block — no separate RECIPE_DEVIATION.md file.
  #
  # enabled_l4_domains:
  #   skip: ["auth"]
  #   rationale: "Single-operator personal helpdesk; no role differentiation needed."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["webhook"]
  #   rationale: "Fully-internal-only deployment; no external ITSM integration required."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["feature-flags"]
  #   rationale: "Single-tenant typical; no toggle gating needed."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `auth` | **impl** ✅ | — (ready) |
| `crud` | **impl** ✅ | — (ready) |
| `notification` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `scheduled-task` | **impl** ✅ | — (R49: backend GREEN + admin Next.js surface — task list w/ enable·disable·trigger + per-task history) |
| `webhook` | **impl** ✅ | — (R48: backend GREEN + admin Next.js surface — register w/ one-time secret reveal + delivery monitor + replay) |

**Summary**: 2 impl ready · 2 spec-only (implement) · 2 skeleton (flesh out) · est. ~22-29 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: internal-it

**Business context:** Internal IT-Service-Management — open / triage / assign /
resolve / close ticket lifecycle, role-aware operator vs requester vs approver
authorization, SLA breach reminders via distributed-lock scheduled-task,
assignee notifications respecting opt-out preferences, and **outbound signed
webhook relay to external ITSM systems** (Jira / ServiceNow / PagerDuty /
Slack-incoming / 네이버웍스 / Toss-style state callbacks). Targets internal
IT-helpdesk surfaces (small/mid-org corporate IT, MSP fronts, Kakao Korean
enterprise helpdesk shape).

This recipe is the **first downstream consumer of the webhook L4 primitive**
(R9 SP45 NET-NEW Spec Trio; TD-2026-05-22-025) and closes the R6 Synthesis-A
deferred-recipe queue (community → R7, lms + cms → R8, internal-it → R9; all 4
deferred recipes now shipped). See `templates/DECISIONS.md` TD-2026-05-22-026.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `crud` | Ticket + SLA-policy + Comment CRUD; open / in-progress / resolved / closed state machine |
| `auth` | Role-aware authorization (operator / requester / approver) per ASVS V4.1 |
| `audit-log` | Ticket state transitions emit row with operator + before/after |
| `scheduled-task` | Distributed-lock cron for SLA-breach reminders + idempotent fanout |
| `notification` | Assignee notifications (ticket-assigned / status-changed / SLA-near-breach) respecting opt-out |
| `webhook` | Outbound signed (HMAC-SHA256) webhook relay to external ITSM (Jira / ServiceNow / PagerDuty / Slack) with exponential-backoff retry + dead-letter |

**Optional:** `auth` (single-operator personal helpdesk skip), `webhook`
(fully-internal-only deployment skip), `feature-flags` (single-tenant skip).

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| INTERNAL-IT-INV-001 | Ticket state transitions (open → in-progress → resolved → closed) emit audit-log row with operator + before/after | `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` |
| INTERNAL-IT-INV-002 | SLA breach reminder emission uses scheduled-task lock primitive (no double-send on multi-node) AND idempotent on breach-event key | `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` |
| INTERNAL-IT-INV-003 | Outbound webhook to external ITSM signed HMAC-SHA256 over `<timestamp>.<body>` + retried with exponential backoff (30s × 2 up to 5 attempts) until dead-letter | `spec_ref: specs/webhook-l0.yaml#WEBHOOK-SIGN-001` + `spec_ref: specs/webhook-l0.yaml#WEBHOOK-RETRY-001` (both SP45-shipped) |
| INTERNAL-IT-INV-004 | Assignee notifications (ticket-assigned, status-changed, SLA-near-breach) respect operator preferences + opt-out + at-least-once outbox delivery | `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` + `spec_ref: specs/notification-l0.yaml#NOTIF-SEND-001` |
| INTERNAL-IT-INV-005 | Per-endpoint webhook signing-secret stored encrypted-at-rest (KMS-managed AES-256 envelope) and never logged in plaintext | `co-shipped-rule: webhook-secret-encryption` + `invariant_test: frontend/tests/recipes/internal-it-webhook-secret.spec.ts` |

### INV-005 disambiguation (deliberate framing — R7-community escape hatch reused)

R8 `LMS-INV-005` and `CMS-INV-005` bound to the preferred path
(`spec_ref + rule_ref` against the existing
`practices/rules/idempotency-key-on-mutations.md` rule) because the catalog
already had a rule covering bulk-enrollment / slug-uniqueness idempotency.

R9 **INTERNAL-IT-INV-005 differs:** the catalog has **no existing rule** covering
per-endpoint signing-secret encryption-at-rest. It is a webhook-specific KMS
pattern, not a general crypto-at-rest pattern — the latter would belong in a
`security-secret-encryption-at-rest.md` rule which **does not exist** in the
catalog today. The choice is determined by catalog state at SP execution: when
the catalog has no existing anchor, use the `co-shipped-rule + invariant_test`
escape hatch (R7 `COMMUNITY-INV-005` precedent). When the catalog has an
existing anchor, use the preferred `spec_ref + rule_ref` path (R8 LMS / CMS
precedent).

**Promotion criterion (M5 framing — deferred indefinitely):** promotion of
`webhook-secret-encryption` to a standalone
`practices/rules/security-secret-encryption-at-rest.md` rule is **deferred
indefinitely; it remains a recipe-level invariant unless cross-domain need
emerges.** No R10+ deferred-recipe entry exists today that would consume it;
promotion is reactive to demonstrated demand, not speculative. See
`templates/DECISIONS.md` TD-2026-05-22-025 Follow-ups.

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.internal-it.ticket_active_total` | Counter | Active (non-closed) tickets count |
| `recipe.internal-it.sla_breach_total` | Counter | Tickets that crossed SLA threshold |
| `recipe.internal-it.webhook_failed_permanent_total` | Counter | Outbound ITSM webhook hits `WEBHOOK-DEAD-LETTER-001` |

## Evidence

```yaml
evidence:
  - provenance_class: external
    source: "Jira webhooks — Atlassian Server REST API docs"
    url: "https://developer.atlassian.com/server/jira/platform/webhooks/"
    citation: "A webhook is a user-defined callback over HTTP."
    quoted_at: "2026-05-22"
    fidelity_note: "Jira is the canonical issue-tracker / ITSM platform; Atlassian Server REST docs attest the webhook-callback pattern internal-it recipe encodes. Atlassian Cloud REST host returned content truncation across 3 attempts; Server host fallback closes the gap (both atlassian-canonical for webhook semantics)."

  - provenance_class: external
    source: "Jira webhooks — Atlassian Server REST API docs (event subscription)"
    url: "https://developer.atlassian.com/server/jira/platform/webhooks/"
    citation: "You can use Jira webhooks to notify your app or web application when certain events occur in Jira."
    quoted_at: "2026-05-22"
    fidelity_note: "Second Jira verbatim — directly attests the event-driven outbound semantics INTERNAL-IT-INV-003 binds."

  - provenance_class: external
    source: "PagerDuty webhooks support docs"
    url: "https://support.pagerduty.com/docs/webhooks"
    citation: "Webhooks allow you to receive HTTP callbacks when significant events happen in your PagerDuty account, for example, when an incident triggers, escalates or resolves."
    quoted_at: "2026-05-22"
    fidelity_note: "PagerDuty is the canonical incident-management ITSM platform. developer.pagerduty.com developer-portal returned empty body across 3 attempts; support host fallback closes the gap."

  - provenance_class: external
    source: "PagerDuty webhooks support docs (destination examples)"
    url: "https://support.pagerduty.com/docs/webhooks"
    citation: "Details about the event are sent to your specified URL, such as Slack or your own custom PagerDuty webhook processor."
    quoted_at: "2026-05-22"
    fidelity_note: "Second PagerDuty verbatim — names the integration shapes (Slack inbound + custom processor) the internal-it webhook fanout typically targets."

  - provenance_class: external
    source: "Toss Payments webhook (Korean) — 토스페이먼츠 웹훅"
    url: "https://docs.tosspayments.com/guides/webhook"
    citation: "토스페이먼츠 결제, 브랜드페이, 지급대행 상태에 변경사항이 있을 때 웹훅으로 실시간 업데이트를 받아보세요."
    quoted_at: "2026-05-22"
    fidelity_note: "Korean webhook verbatim. Toss Payments is a canonical Korean payment platform; the verbatim directly attests state-change webhook semantics in Korean enterprise practice."

  - provenance_class: external
    source: "Toss Payments webhook (Korean) — concept definition"
    url: "https://docs.tosspayments.com/guides/webhook"
    citation: "웹훅이란 데이터가 변경되었을 때 실시간으로 알림을 받을 수 있는 기능이에요."
    quoted_at: "2026-05-22"
    fidelity_note: "Second Toss verbatim defining 웹훅 (webhook) primitive in Korean — anchors the L4 webhook concept locale-coverage."

  - provenance_class: external
    source: "Naver Works Bot API (Korean) — 네이버웍스 봇 API"
    url: "https://developers.worksmobile.com/kr/docs/bot-api"
    citation: "Bot API로 봇에서 메시지를 보내거나, 메뉴를 설정하고, 봇을 관리할 수 있다."
    quoted_at: "2026-05-22"
    fidelity_note: "Korean enterprise-messaging API anchor (2 consecutive non-zero-Korean cycles). Naver Works Bot API is the canonical enterprise-messenger callback surface."

  - provenance_class: external
    source: "Naver Works Bot API (Korean) — authentication"
    url: "https://developers.worksmobile.com/kr/docs/bot-api"
    citation: "Bot API를 호출하려면 구성원 계정 또는 서비스 계정으로 인증하여 얻은 Access Token이 필요하다."
    quoted_at: "2026-05-22"
    fidelity_note: "Second Naver Works verbatim — names per-account token authentication shape compatible with per-endpoint signing-secret model in INTERNAL-IT-INV-005."

  - provenance_class: internal_design
    source: "Atlassian Cloud REST API webhooks docs (alt host)"
    url: "https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-webhooks/"
    rationale: "200 OK on 2026-05-22 but content truncated to navigation skeleton across 3 Cloud-host attempts; Server-host fallback verbatim closes the gap (PRD §4.4)."

  - provenance_class: internal_design
    source: "ServiceNow REST API documentation hosts (4 attempts)"
    url: "https://developer.servicenow.com/dev.do"
    rationale: "Host-wide pattern: developer portal landing lacks API descriptive content; docs.servicenow.com utah-application-development 301-redirected; root has no API verbatim; www.servicenow.com/products/rest-api.html returned HTTP 403. ServiceNow verbatim consistently unavailable across 4 host attempts."

  - provenance_class: internal_design
    source: "Kakao 알림톡 / 카카오비즈니스 (Korean) — 6 host attempts"
    url: "https://developers.kakao.com/docs/latest/ko/message/rest-api"
    rationale: "Host-wide pattern across developers.kakao.com landing + 404 redirect + business.kakao.com page-shell + kakaobusiness.gitbook.io 404 navigation. Kakao 알림톡 verbatim consistently unavailable across 6 host attempts (PRD §4.4 host-wide downgrade)."

  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP17 audit-log"
      - "SP26 notification"
      - "SP41 scheduled-task"
      - "SP45 webhook (R9 NET-NEW)"
      - "auth ASVS L1"
    rationale: "Composition derives from existing crud, audit-log, notification, scheduled-task, auth Spec Trios + SP45-shipped webhook L4 Spec Trio. No new L4 introduced by internal-it recipe."
```

## Scaffold Usage

```bash
/ax-scaffold business internal-it my-helpdesk-app
```

This will scaffold all 6 enabled L4 domains into `my-helpdesk-app/` and run
`/ax-verify-domain` for each.
