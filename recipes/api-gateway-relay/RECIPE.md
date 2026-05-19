---
pattern: api-gateway-relay
display_name: "API Gateway Relay (Endpoint Registry + Signed Outbound Relay + Route Authz + Rate-Limit + Circuit Breaker)"
schema_version: 1
compatible_with_catalog_version: "v1.8.0-api-gateway-relay"
last_verified_at: "2026-05-23"
enabled_l4_domains:
  - audit-log
  - auth
  - crud
  - scheduled-task
  - webhook
l2_blocks_used:
  - bulk-actions-bar
  - confirm-dialog
  - crud-create-form
  - crud-edit-form
  - crud-list-adapter
  - data-table
  - filter-bar
  - kpi-card
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
  #   add: ["notification"]
  #   rationale: "Alerting on circuit-breaker open / dead-letter accumulation thresholds."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   add: ["feature-flags"]
  #   rationale: "Per-route gating; canary routing across upstream variants."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["scheduled-task"]
  #   rationale: "Stateless circuit-breaker — no reconciliation cron needed for in-memory deployments."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["notification"]
  #   rationale: "Headless gateway deployment — no operator-facing alert surface."
  #   citation: "<internal ticket / PR url>"
  #
  # enabled_l4_domains:
  #   skip: ["feature-flags"]
  #   rationale: "Monolithic single-route relay — no per-route gating needed."
  #   citation: "<internal ticket / PR url>"
---

## Backend Implementation Status

> See [`docs/IMPLEMENTATION-STATUS.md`](../../docs/IMPLEMENTATION-STATUS.md) for the full 12-L4 status taxonomy and fork-receiver expectation alignment (R15+ mandatory section).

| L4 domain | Status | Effort if not impl |
|---|---|---|
| `audit-log` | **spec-only** 📋 | ~5-10 eng-days (implement backend) |
| `auth` | **impl** ✅ | — (ready) |
| `crud` | **impl** ✅ | — (ready) |
| `scheduled-task` | **skeleton** ⚠️ | ~3-7 eng-days (flesh out .skeleton) |
| `webhook` | **skeleton** ⚠️ | ~3-7 eng-days (flesh out .skeleton) |

**Summary**: 2 impl ready · 1 spec-only (implement) · 2 skeleton (flesh out) · est. ~15-22 engineering days for the gap.

**Reading guide**: `impl` = backend Java reference workload ready in `backend/src/main/java/com/ax/template/authblueprint/<domain>/`. `spec-only` = Spec Trio + Next.js stub only; backend NOT included. `skeleton` = `.skeleton` file present; flesh out controller/service yourself. Sealed verdict PASS validates catalog self-discoverability, NOT runnable backend code.


# Recipe: api-gateway-relay

> **Disambiguation preamble (per PRD M3 + Codex M3 — must appear verbatim in this RECIPE.md so the context-0 sealed sub-agent reads it as the gateway-vs-primitive anchor):**
>
> **api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4's outbound-emit primitive; NOT itself a primitive.**

**Business context:** Inbound API traffic registry + per-route authz + per-route
rate-limit + signed outbound relay to multiple backend services + circuit-breaker
per upstream endpoint + dead-letter inspection / replay. Targets the API-gateway
pattern (Kong / AWS API Gateway / Cloudflare API Shield / Tyk / Apigee shape)
**re-expressed as a COMPOSITION over existing 12 L4 primitives** — webhook L4
provides the outbound-emit substrate (HMAC-SHA256 over `<timestamp>.<body>` +
exponential backoff + circuit breaker), auth L4 provides gateway-level access
control + token introspection, audit-log L4 captures every relayed request, crud
L4 owns endpoint registry + route configuration, scheduled-task L4 reconciles
circuit-breaker half-open / closed state across multi-node deployments. Plus a
cross-cutting binding to `specs/ratelimit-l0.yaml` (4 items — `RATELIMIT-1..4`)
enforced INSIDE existing L4 boundaries (auth + webhook + crud filters) WITHOUT
introducing a new L4 directory.

This recipe is the **second downstream consumer of the webhook L4 primitive**
(R9 SP45 NET-NEW Spec Trio; TD-2026-05-22-025) — the first being `internal-it`
(R9 SP45b; TD-2026-05-22-026). It RETROACTIVELY VALIDATES R9's TD-2026-05-22-027
(c) two-consumer-signal gate by supplying the named-forward-pointer 2nd
consumer; the gate set in R9 thus achieves operational proof in R10. See
`templates/DECISIONS.md` TD-2026-05-23-028 + TD-2026-05-23-029.

## Enabled L4 Domains

| L4 Domain | Role in this recipe |
|---|---|
| `webhook` | Outbound signed relay to backend services — HMAC-SHA256 over `<timestamp>.<body>` per WEBHOOK-SIGN-001/002, exponential backoff per WEBHOOK-RETRY-001, dead-letter per WEBHOOK-DEAD-LETTER-001, circuit-breaker per WEBHOOK-CIRCUIT-001 |
| `auth` | Gateway-level access control — token validity + scope checks BEFORE relay forward (ASVS V4.1.1) |
| `audit-log` | Every relayed request emits AUDIT-RECORD-001 row with operator + relay_target_endpoint + delivery_id; route config mutations emit before/after diff (AUDIT-RECORD-002 immutability) |
| `crud` | Endpoint registry + route config CRUD (URL, method, target, signing-secret) — CRUD-VAL-1 validation on create / update |
| `scheduled-task` | Per-endpoint circuit-breaker state reconciliation + dead-letter replay scheduling — SCHED-LOCK-001 multi-node-safe |

**Optional (per `override_allowed:`):** `notification` (alerting on circuit-breaker
open / dead-letter accumulation thresholds), `feature-flags` (per-route gating,
canary routing across upstream variants). `scheduled-task` is OPTIONAL-SKIP for
stateless circuit-breaker deployments per the override block.

## Cross-cutting `specs/ratelimit-l0.yaml` binding (R10-novel framing)

`ratelimit-l0.yaml` exists on disk as a 4-item spec (`RATELIMIT-1..4`) WITHOUT a
`templates/L4/ratelimit/` directory. R10 treats rate-limiting as a CROSS-CUTTING
CONCERN enforced INSIDE existing L4 boundaries (auth + webhook + crud filters),
bound via `spec_ref:` ONLY. This is deliberately DIFFERENT from spinning a new
L4 — see TD-2026-05-22-027 (c) two-consumer-signal gate + TD-2026-05-23-029
no-L4-split discipline. `recipe_spec_referential_integrity_guard.sh` resolves
`spec_ref:` against file existence + ID presence only (NOT L4 directory
presence), so the cross-cutting binding is guard-compatible without
materializing a new L4 directory (Codex iter 1 guard audit confirmed at PRD
signature).

## Business Invariants

| ID | Statement | Binding |
|---|---|---|
| API-GATEWAY-RELAY-INV-001 | Every relayed request to a backend service is signed via webhook L4's HMAC-SHA256 over `<timestamp>.<body>` AND emits an audit-log row with operator + relay_target_endpoint + delivery_id | `spec_ref: specs/webhook-l0.yaml#WEBHOOK-SIGN-001` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` |
| API-GATEWAY-RELAY-INV-002 | Relay authorization checks token validity AND scope BEFORE forwarding to upstream; on auth failure the request is rejected with audit-logged denial reason | `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.1.1` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` |
| API-GATEWAY-RELAY-INV-003 | Per-route rate-limiting enforced before relay; over-limit requests rejected with HTTP 429 AND counted via observability counter | `spec_ref: specs/ratelimit-l0.yaml#RATELIMIT-1` + `spec_ref: specs/ratelimit-l0.yaml#RATELIMIT-2` |
| API-GATEWAY-RELAY-INV-004 | Circuit breaker per upstream endpoint auto-opens on 90% failure rate over rolling 50 attempts; reconciliation of half-open / closed state runs via scheduled-task lock primitive (no double-reconcile on multi-node deployment) | `spec_ref: specs/webhook-l0.yaml#WEBHOOK-CIRCUIT-001` + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` |
| API-GATEWAY-RELAY-INV-005 | Route registration + config mutations (URL, method, target, signing-secret) follow CRUD validation rules AND emit audit rows with before/after diff | `spec_ref: specs/crud-security.yaml#CRUD-VAL-1` + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` + `rule_ref: practices/rules/idempotency-key-on-mutations.md` |

### INV anchor disambiguation (deliberate framing — disambiguated from R7 community)

All 5 INVs bind to **EXISTING** spec items + **EXISTING** practices rules. **No
`co-shipped-rule` invocation is needed this cycle** (deliberately disambiguated
from the R7 community / R9 internal-it `co-shipped-rule + invariant_test` escape
hatch). The catalog already provides every anchor INV-001 through INV-005
requires:

- INV-001 / INV-004: webhook L4 spec items shipped in R9 SP45 (WEBHOOK-SIGN-001,
  WEBHOOK-CIRCUIT-001).
- INV-002: ASVS V4.1.1 + AUDIT-RECORD-002 already in `specs/auth-asvs-l1.yaml` +
  `specs/audit-log-l0.yaml`.
- INV-003: `specs/ratelimit-l0.yaml#RATELIMIT-1/2` — cross-cutting binding (no
  new L4 directory; spec-only resolution via the `recipe_spec_referential_integrity_guard.sh`
  file/ID-only check).
- INV-005: `specs/crud-security.yaml#CRUD-VAL-1` + AUDIT-RECORD-002 +
  `practices/rules/idempotency-key-on-mutations.md` (3 anchors; clears the
  ≥1-anchor floor with 2x buffer per M4 wording).

## Business Observability (advisory — no emitter test enforced this cycle)

| Signal | Type | Notes |
|---|---|---|
| `recipe.api_gateway_relay.relay_request_total` | Counter | Inbound relays accepted + signed + dispatched to upstream |
| `recipe.api_gateway_relay.relay_rate_limit_rejected_total` | Counter | Inbound relays rejected with HTTP 429 (over-limit) |
| `recipe.api_gateway_relay.circuit_breaker_open_total` | Counter | Per-endpoint circuit transitions to OPEN state |

## Evidence

```yaml
evidence:
  # ─── English verbatim PASS (5) ──────────────────────────────────────────
  - provenance_class: external
    source: "Kong Gateway — official developer docs"
    url: "https://developer.konghq.com/gateway/"
    citation: "Kong Gateway is a lightweight, fast, and flexible cloud-native API gateway."
    quoted_at: "2026-05-23"
    fidelity_note: "Kong is the canonical open-source API gateway; the verbatim attests the gateway-pattern composition api-gateway-relay encodes. docs.konghq.com 301-redirected to developer.konghq.com on 2026-05-23 — followed redirect, final host carries verbatim."

  - provenance_class: external
    source: "Amazon API Gateway — AWS docs"
    url: "https://docs.aws.amazon.com/apigateway/"
    citation: "Amazon API Gateway enables you to create and deploy your own REST and WebSocket APIs at any scale."
    quoted_at: "2026-05-23"
    fidelity_note: "AWS API Gateway is the canonical hyperscaler gateway product; the verbatim attests the create-deploy-API gateway pattern."

  - provenance_class: external
    source: "Cloudflare API Shield"
    url: "https://developers.cloudflare.com/api-shield/"
    citation: "Identify and address your API vulnerabilities."
    quoted_at: "2026-05-23"
    fidelity_note: "Cloudflare API Shield (alternate-fetched bridge after /api-gateway/ 404 on 2026-05-23) — final-verbatim-via-alternate path documented in PRD §4.4 ledger; attests the API-vulnerability-mitigation framing that api-gateway-relay's per-route authz + rate-limit + audit composition encodes."

  - provenance_class: external
    source: "Tyk API gateway — official docs"
    url: "https://tyk.io/docs/"
    citation: "The hub for Tyk API management. Whether you're new or experienced, get started with Tyk, explore our product stack and core concepts, access in-depth guides, and actively contribute to our ever-evolving products."
    quoted_at: "2026-05-23"
    fidelity_note: "Tyk is a canonical open-source / commercial API gateway; the verbatim attests the API-management framing the recipe composes."

  - provenance_class: external
    source: "Apigee — Google Cloud docs"
    url: "https://docs.cloud.google.com/apigee/docs"
    citation: "With Apigee, you can build API proxies—RESTful, HTTP-based APIs that interact with your services."
    quoted_at: "2026-05-23"
    fidelity_note: "Apigee is the canonical enterprise API gateway product on Google Cloud; the verbatim attests the API-proxy / backend-relay pattern. cloud.google.com/apigee/docs 301-redirected to docs.cloud.google.com on 2026-05-23 — followed redirect, final host carries verbatim."

  # ─── Korean verbatim PASS (2) ──────────────────────────────────────────
  - provenance_class: external
    source: "토스페이먼츠 API 레퍼런스 (Toss Payments — Korean adjacent fallback)"
    url: "https://docs.tosspayments.com/reference"
    citation: "토스페이먼츠 API 엔드포인트(Endpoint)와 객체 정보, 파라미터, 요청 및 응답 예제를 살펴보세요."
    quoted_at: "2026-05-23"
    fidelity_note: "Korean verbatim — Toss Payments adjacent-platform fallback (R9 Toss-as-adjacent precedent). After cloud-native Korean API gateway hosts (KakaoCloud × 3 / NHN Cloud × 2 / Naver Cloud × 3) all returned 404 / ECONNREFUSED / redirect-to-error per PRD §4.4 ledger, Toss API reference page describes the API endpoint / parameter / request-response shape api-gateway-relay encodes."

  - provenance_class: external
    source: "NAVER Cloud Platform — service catalog (Korean fresh-vendor adjacent — iter 2 add per Architect H1)"
    url: "https://www.ncloud.com/product"
    citation: "API 호출, 관리, 모니터링 등 API와 관련된 모든 작업을 실행할 수 있는 서비스"
    quoted_at: "2026-05-23"
    fidelity_note: "Korean verbatim — NAVER Cloud Platform service catalog page (200 OK on 2026-05-23). DIFFERENT vendor than R9 Toss anchor — establishes the Korean vendor rotation precedent per TD-2026-05-23-028 Follow-ups M2 closure. Verbatim describes API gateway operating semantics: invocation (호출) / management (관리) / monitoring (모니터링) — the exact triad api-gateway-relay's composition (webhook outbound + audit-log + scheduled-task reconciliation) materializes."

  # ─── Documented downgrades (honest evidence per PRD §4.4) ──────────────
  - provenance_class: internal_design
    source: "KakaoCloud API Gateway docs (3 host attempts on 2026-05-23)"
    url: "https://docs.kakaocloud.com/service/cloud-edge/apigateway"
    rationale: "Host-wide pattern: docs.kakaocloud.com/service/cloud-edge/apigateway → HTTP 404; docs.kakaocloud.com/ root → 200 OK but homepage shell only with no API gateway descriptive content; kakaoi.kakaocloud.com/service/cloudEdge/apiGateway → ECONNREFUSED. KakaoCloud API gateway verbatim consistently unavailable across 3 host attempts (PRD §4.4 host-wide downgrade — documented honest evidence; further retry deferred per PRD §10)."

  - provenance_class: internal_design
    source: "NHN Cloud API Gateway docs (2 host attempts on 2026-05-23)"
    url: "https://docs.nhncloud.com/ko/Application%20Service/API%20Gateway"
    rationale: "Host-wide pattern: docs.nhncloud.com 301 → http://docs.nhncloud.com 302 → error page; meetup.nhncloud.com/posts/250 → HTTP 404 '존재하지 않는 페이지 입니다'. NHN Cloud API gateway verbatim consistently unavailable across 2 host attempts (PRD §4.4 host-wide downgrade — documented honest evidence; further retry deferred per PRD §10)."

  - provenance_class: internal_design
    source: "Naver Cloud API Gateway deep-doc hosts (3 host attempts on 2026-05-23)"
    url: "https://api.ncloud-docs.com/docs/apigateway-overview"
    rationale: "Host-wide pattern: api.ncloud-docs.com/docs/apigateway-overview → 404; ncloud.com/product/applicationService/apiGateway → 404 (deep-doc path, distinct from /product catalog-root verbatim hit); guide.ncloud-docs.com/docs/apigw-overview → 404. Naver Cloud deep-doc verbatim consistently unavailable across 3 host attempts. The /product catalog-root page itself (different URL) carries verbatim Korean and is captured above (PRD §4.4 iter-2 fresh-vendor add per Architect H1)."

  - provenance_class: internal_design
    derives_from:
      - "SP15 crud"
      - "SP17 audit-log"
      - "SP41 scheduled-task"
      - "SP45 webhook (R9 NET-NEW Spec Trio; TD-2026-05-22-025)"
      - "auth ASVS L1"
      - "specs/ratelimit-l0.yaml (cross-cutting; no L4 directory)"
    rationale: "Composition derives from existing L4 specs (5 mandatory: webhook + auth + audit-log + crud + scheduled-task) + cross-cutting ratelimit spec binding. NO new L4 introduced by api-gateway-relay recipe — recipe is GATEWAY-PATTERN COMPOSER over existing primitives (RECIPE.md disambiguation preamble verbatim). Validates R9 TD-2026-05-22-027 (c) two-consumer-signal gate retroactively as the 2nd shipped consumer of webhook L4 (after R9 internal-it as the 1st shipped consumer)."
```

## Scaffold Usage

```bash
/ax-scaffold business api-gateway-relay my-gateway-app
```

This will scaffold all 5 enabled L4 domains into `my-gateway-app/` and run
`/ax-verify-domain` for each. Optional L4 (`notification`, `feature-flags`) and
optional-skip (`scheduled-task` for stateless deployments) follow the inline
`override_allowed:` block.
