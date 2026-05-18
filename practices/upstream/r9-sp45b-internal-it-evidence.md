# R9 SP45b — internal-it recipe external evidence snapshot

**Fetched:** 2026-05-22
**Purpose:** anchor `templates/DECISIONS.md` TD-2026-05-22-026 (internal-it
recipe + R6 Synthesis-A deferred-queue closure) with verbatim external
evidence. Four hosts cleared with verbatim (Jira + PagerDuty + Toss + Naver
Works); five hosts downgraded (Atlassian Cloud × 3 truncation, ServiceNow × 4
attempts, PagerDuty developer-portal × 3 empty-body, Kakao × 6 attempts, Naver
Works landing). All verbatim URLs returned HTTP 200 OK on 2026-05-22 and the
quoted substrings appear verbatim in the rendered page text per the PRD §4.4
evidence ledger.

These quotes anchor `recipes/internal-it/RECIPE.md`,
`specs/recipes/internal-it-recipe-l0.yaml`, and the ADR; they are not
registered in `practices/upstream/_MANIFEST.yaml` (this file is a per-ADR
evidence ledger, not a `.snapshot.md` time-decay-guarded snapshot).

---

## Quote 1 — Jira webhooks (Atlassian Server REST API)

- **URL:** https://developer.atlassian.com/server/jira/platform/webhooks/
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK
- **Verbatim quotes:**

> A webhook is a user-defined callback over HTTP.

> You can use Jira webhooks to notify your app or web application when certain
> events occur in Jira.

- **Relevance:** Jira is the canonical issue-tracker / ITSM platform; the
  Atlassian Server-host REST docs describe the exact callback-on-event
  semantic INV-003 anchors. Atlassian Cloud REST host
  (`developer.atlassian.com/cloud/jira/platform/rest/v3/`,
  `.../intro/`, `.../api-group-webhooks/`) returned content truncation across
  3 attempts on 2026-05-22 (PRD §4.4 evidence ledger); the Server-host
  fallback closes the gap — both atlassian-canonical for webhook semantics.

---

## Quote 2 — PagerDuty webhooks (support.pagerduty.com)

- **URL:** https://support.pagerduty.com/docs/webhooks
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK
- **Verbatim quotes:**

> Webhooks allow you to receive HTTP callbacks when significant events happen
> in your PagerDuty account, for example, when an incident triggers, escalates
> or resolves.

> Details about the event are sent to your specified URL, such as Slack or
> your own custom PagerDuty webhook processor.

- **Relevance:** PagerDuty is the canonical incident-management / ITSM
  platform; the support-host product docs describe the exact incident-
  lifecycle webhook pattern INV-003 anchors. The developer-portal host
  (`developer.pagerduty.com`) returned empty body across 3 attempts
  (`/api-reference/`, `/docs/webhooks/webhooks-overview`,
  `/docs/db0fa8c8984fc-overview`) on 2026-05-22 — the support host is the
  canonical product-docs host; both pagerduty-canonical.

---

## Quote 3 — Toss Payments webhook (Korean — 토스페이먼츠)

- **URL:** https://docs.tosspayments.com/guides/webhook
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK
- **Verbatim quotes (Korean):**

> 토스페이먼츠 결제, 브랜드페이, 지급대행 상태에 변경사항이 있을 때 웹훅으로 실시간 업데이트를 받아보세요.

> 웹훅이란 데이터가 변경되었을 때 실시간으로 알림을 받을 수 있는 기능이에요.

- **Relevance:** Toss Payments is a canonical Korean payment platform that
  ships a webhook product for state-change notifications. The first verbatim
  directly attests the state-change webhook fanout pattern (payments /
  brand-pay / payout); the second verbatim defines 웹훅 (the Korean term)
  semantically — concept anchoring in the Korean locale. PRD §4.4 logs this
  as a verbatim Korean anchor — preserves the R8 1-Korean target with
  buffer.

---

## Quote 4 — Naver Works Bot API (Korean — 네이버웍스)

- **URL:** https://developers.worksmobile.com/kr/docs/bot-api
- **Fetched at:** 2026-05-22
- **HTTP status:** 200 OK
- **Verbatim quotes (Korean):**

> Bot API로 봇에서 메시지를 보내거나, 메뉴를 설정하고, 봇을 관리할 수 있다.

> Bot API를 호출하려면 구성원 계정 또는 서비스 계정으로 인증하여 얻은 Access Token이 필요하다.

- **Relevance:** Naver Works (네이버웍스) is a canonical Korean enterprise
  messaging platform whose Bot API surface attests the per-account access-
  token authentication shape that parallels the per-endpoint signing-secret
  model encoded by `WebhookEndpoint` (INV-005 KMS-managed AES-256 envelope
  storage). 2 Korean verbatim across 2 separate Korean hosts (Toss + Naver
  Works) = **2 consecutive non-zero-Korean-verbatim cycles** (R8 lms-cms +
  R9 internal-it).

---

## Downgrade pattern summary (host-wide — honest evidence per PRD §4.4)

- **Atlassian Cloud REST host (3 attempts):** Cloud REST landing /
  `intro/` / `api-group-webhooks/` all 200 OK but content truncation pattern;
  Server-host fallback closes the verbatim gap.
- **ServiceNow REST API (4 host attempts):** `developer.servicenow.com/dev.do`
  landing has no API descriptive content; `docs.servicenow.com/bundle/utah-
  application-development/page/integrate/inbound-rest/concept/c_RESTAPI.html`
  301 → `http://docs.servicenow.com/` 301 → `https://www.servicenow.com/docs/`
  200 OK but navigation-only; `www.servicenow.com/products/rest-api.html` 403.
  Host-wide non-public-API descriptive content pattern.
- **PagerDuty developer-portal (3 attempts):** `developer.pagerduty.com/api-
  reference/`, `/docs/webhooks/webhooks-overview`, `/docs/db0fa8c8984fc-
  overview` all 200 OK but empty body. Support-host fallback closes the gap.
- **Kakao 알림톡 (6 attempts):** `developers.kakao.com` landing,
  `/docs/latest/ko/message/rest-api` → 302 → 404, `business.kakao.com` root
  + `/info/bizmessage/` + `/info/kakaotalkchannel/`, `kakaobusiness.gitbook.io/main`
  + `/main/ad/start/example` 404. Host-wide non-public-API pattern across 6
  attempts.
- **Naver Works landing (1 attempt):** `developers.worksmobile.com` heading
  only; alt path `/kr/docs/bot-api` cleared with verbatim.

---

## Notes

- These four verbatim quotes satisfy the R9 SP45b evidence-density floor
  (≥1 external verbatim per deliverable; 4 captured for 4x buffer including
  2 Korean for 2-consecutive-cycle preservation).
- The PRD §4.4 evidence ledger documents the full fetch result table
  including 9 downgrades + 5 redirect / alt-host captures.
- SP45 webhook L4 carries its own evidence snapshot at
  `practices/upstream/r9-sp45-webhook-evidence.md` (GitHub + Stripe + RFC
  2104 anchor — 3 quotes).
- ServiceNow + Kakao Alimtalk verbatim are deferred to R10+ per PRD §10 —
  host-wide pattern; further retries beyond SP45b pre-flight one-shot are
  out-of-scope this cycle.
