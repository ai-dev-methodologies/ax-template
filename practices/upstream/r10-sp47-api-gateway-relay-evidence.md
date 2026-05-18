# R10 SP47 — api-gateway-relay recipe external evidence snapshot

**Fetched:** 2026-05-23
**Purpose:** anchor `templates/DECISIONS.md` TD-2026-05-23-028 (api-gateway-relay
recipe shipped) + TD-2026-05-23-029 (no-L4-split discipline / Codex L Option
(a) clean-revert) with verbatim external evidence. **5 English** hosts cleared
with verbatim (Kong + AWS + Cloudflare API Shield + Tyk + Apigee) + **2 Korean**
hosts cleared with verbatim (Toss Payments adjacent fallback + NAVER Cloud
Platform service catalog fresh-vendor adjacent per Architect H1 iter-2 add).
8 hosts documented as downgrades (KakaoCloud × 3 + NHN Cloud × 2 + Naver Cloud
× 3 deep-doc — host-wide 404 / ECONNREFUSED / redirect-to-error patterns).

All verbatim URLs returned HTTP 200 OK on 2026-05-23 and the quoted substrings
appear verbatim in the rendered page text per the PRD §4.4 evidence ledger.

These quotes anchor `recipes/api-gateway-relay/RECIPE.md`,
`specs/recipes/api-gateway-relay-recipe-l0.yaml`, and the two ADRs; they are
NOT registered in `practices/upstream/_MANIFEST.yaml` (this file is a per-ADR
evidence ledger, not a `.snapshot.md` time-decay-guarded snapshot).

---

## Quote 1 — Kong Gateway (English)

- **URL:** https://developer.konghq.com/gateway/
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK (after 301 redirect from `docs.konghq.com/gateway/`)
- **Verbatim quote:**

> Kong Gateway is a lightweight, fast, and flexible cloud-native API gateway.

- **Relevance:** Kong is the canonical open-source API gateway; the verbatim
  attests the cloud-native gateway pattern api-gateway-relay encodes via the
  webhook L4 outbound-emit + auth + audit-log composition. `docs.konghq.com`
  301-redirected to `developer.konghq.com` on 2026-05-23 — followed redirect;
  final host carries verbatim.

---

## Quote 2 — Amazon API Gateway (English)

- **URL:** https://docs.aws.amazon.com/apigateway/
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK (direct)
- **Verbatim quote:**

> Amazon API Gateway enables you to create and deploy your own REST and WebSocket APIs at any scale.

- **Relevance:** AWS API Gateway is the canonical hyperscaler gateway product;
  the verbatim attests the create-deploy-API gateway pattern. Direct fetch — no
  redirect chain.

---

## Quote 3 — Cloudflare API Shield (English)

- **URL:** https://developers.cloudflare.com/api-shield/
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK (alternate-fetched bridge after `/api-gateway/` 404)
- **Verbatim quote:**

> Identify and address your API vulnerabilities.

- **Relevance:** Cloudflare API Shield is the canonical CDN-vendor API security
  surface; the verbatim attests the API-vulnerability-mitigation framing that
  api-gateway-relay's per-route authz + rate-limit + audit composition encodes.
  `/api-gateway/` returned HTTP 404 on 2026-05-23 — bridged to `/api-shield/`
  (final-verbatim-via-alternate path per PRD §4.4).

---

## Quote 4 — Tyk API gateway (English)

- **URL:** https://tyk.io/docs/
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK (direct)
- **Verbatim quote:**

> The hub for Tyk API management. Whether you're new or experienced, get started with Tyk, explore our product stack and core concepts, access in-depth guides, and actively contribute to our ever-evolving products.

- **Relevance:** Tyk is a canonical open-source / commercial API gateway; the
  verbatim attests the API-management framing the recipe composes. Direct
  fetch — no redirect chain.

---

## Quote 5 — Apigee (English)

- **URL:** https://docs.cloud.google.com/apigee/docs
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK (after 301 redirect from `cloud.google.com/apigee/docs`)
- **Verbatim quote:**

> With Apigee, you can build API proxies—RESTful, HTTP-based APIs that interact with your services.

- **Relevance:** Apigee is the canonical enterprise API gateway product on
  Google Cloud; the verbatim attests the API-proxy / backend-relay pattern.
  `cloud.google.com/apigee/docs` 301-redirected to `docs.cloud.google.com/apigee/docs`
  on 2026-05-23 — followed redirect; final host carries verbatim.

---

## Quote 6 — Toss Payments API reference (Korean — adjacent fallback)

- **URL:** https://docs.tosspayments.com/reference
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK
- **Verbatim quote (Korean):**

> 토스페이먼츠 API 엔드포인트(Endpoint)와 객체 정보, 파라미터, 요청 및 응답 예제를 살펴보세요.

- **Relevance:** Korean adjacent-platform fallback (R9 Toss-as-adjacent
  precedent). After Korean cloud-native API gateway hosts (KakaoCloud × 3 +
  NHN Cloud × 2 + Naver Cloud × 3 deep-doc) ALL returned 404 / ECONNREFUSED /
  redirect-to-error on 2026-05-23, Toss Payments API reference page describes
  the API endpoint / parameter / request-response shape api-gateway-relay
  encodes. Korean verbatim preserves the non-zero-Korean cycle (R8 lms-cms +
  R9 internal-it + R10 api-gateway-relay = **3 consecutive non-zero-Korean
  cycles**).

---

## Quote 7 — NAVER Cloud Platform service catalog (Korean — fresh-vendor adjacent — iter 2 add per Architect H1)

- **URL:** https://www.ncloud.com/product
- **Fetched at:** 2026-05-23
- **HTTP status:** 200 OK
- **Verbatim quote (Korean):**

> API 호출, 관리, 모니터링 등 API와 관련된 모든 작업을 실행할 수 있는 서비스

- **Relevance:** **NAVER Cloud Platform service catalog page** — DIFFERENT
  vendor than R9 Toss anchor, establishing the **Korean vendor rotation
  precedent** per TD-2026-05-23-028 Follow-ups (M2 closure). Verbatim describes
  API gateway operating semantics: invocation (호출) / management (관리) /
  monitoring (모니터링) — the exact triad api-gateway-relay's composition
  (webhook outbound + audit-log audit-trail + scheduled-task circuit-reconcile)
  materializes.
  Iter-2 add per Architect H1 to close fresh-vendor-Korean-diversity demand
  while preserving Toss adjacent (documented R9-precedent fallback). Different
  vendor than R9 Toss = vendor-rotation; if R11 + R12 both fall back to Toss
  again, R12 planner MUST escalate to a dedicated Korean-vendor-diversity guard
  OR accept Toss-as-permanent-adjacent precedent via explicit ADR.

---

## Downgrade pattern summary (host-wide — honest evidence per PRD §4.4)

### KakaoCloud (3 host attempts)
- `https://docs.kakaocloud.com/service/cloud-edge/apigateway` → HTTP 404
- `https://docs.kakaocloud.com/` → 200 OK but homepage shell only (no API
  gateway descriptive content)
- `https://kakaoi.kakaocloud.com/service/cloudEdge/apiGateway` → ECONNREFUSED

### NHN Cloud (2 host attempts)
- `https://docs.nhncloud.com/ko/Application%20Service/API%20Gateway` → 301 →
  `http://docs.nhncloud.com/...` → 302 → error page
- `https://meetup.nhncloud.com/posts/250` → HTTP 404
  (`404 존재하지 않는 페이지 입니다.`)

### Naver Cloud (3 deep-doc host attempts — distinct from /product catalog-root iter 2 hit)
- `https://api.ncloud-docs.com/docs/apigateway-overview` → HTTP 404
- `https://www.ncloud.com/product/applicationService/apiGateway` → HTTP 404
  (deep-doc path; distinct from `/product` catalog-root which IS the iter-2
  verbatim hit captured above)
- `https://guide.ncloud-docs.com/docs/apigw-overview` → HTTP 404

---

## Per-source-class arithmetic (PRD §4.4 iter-3 closure — Codex iter 2 narrow M1)

| Source class | Count |
|---|---|
| Verbatim-bearing rows (English) | 5 (Kong + AWS + Cloudflare API Shield + Tyk + Apigee) |
| Verbatim-bearing rows (Korean) | 2 (Toss adjacent + NAVER Cloud Platform fresh-vendor) |
| Downgrade rows (KakaoCloud × 3 + NHN Cloud × 2 + Naver Cloud × 3 deep-doc) | 8 |
| Followed-redirect rows | 3 (Kong 301 + Apigee 301 + NHN 301-302 cascade) |
| Alternate-fetched-as-bridge rows | 1 (Cloudflare /api-gateway 404 → /api-shield) |
| **Total data rows** | **19** (5 + 2 + 8 + 3 + 1 = 19 ✓) |

Of the 7 verbatim-bearing rows, 6 are tagged `Verbatim cite` and 1 is tagged
`final-verbatim-via-alternate` (Cloudflare API Shield bridge after /api-gateway/
404). Reading guide for these 7 verbatim-bearing rows: Kong
(`developer.konghq.com` 200 OK after 301 from `docs.konghq.com`); AWS
(`docs.aws.amazon.com/apigateway/` 200 OK direct); Cloudflare API Shield
(`developers.cloudflare.com/api-shield/` 200 OK after `/api-gateway/` 404
bridge); Tyk (`tyk.io/docs/` 200 OK direct); Apigee
(`docs.cloud.google.com/apigee/docs` 200 OK after 301 from
`cloud.google.com/apigee/docs`); Toss Payments
(`docs.tosspayments.com/reference` 200 OK adjacent fallback — R9 precedent);
NAVER Cloud Platform (`www.ncloud.com/product` 200 OK fresh-vendor adjacent —
iter 2 add per Architect H1).

---

## Notes

- These 7 verbatim quotes satisfy the R10 SP47 evidence-density floor
  (≥1 external verbatim per deliverable; 7 captured for 5x EN + 2x KO buffer
  including 2 Korean for 3-consecutive-cycle preservation).
- The PRD §4.4 evidence ledger documents the full fetch result table including
  8 downgrades + 3 followed-redirect + 1 alternate-fetched-as-bridge = 19 data
  rows total.
- R9 SP45 webhook L4 carries its own evidence snapshot at
  `practices/upstream/r9-sp45-webhook-evidence.md` (GitHub + Stripe + RFC 2104
  anchor — 3 quotes).
- R9 SP45b internal-it recipe carries its own evidence snapshot at
  `practices/upstream/r9-sp45b-internal-it-evidence.md` (Jira × 2 + PagerDuty
  × 2 + Toss × 2 + Naver Works × 2 — 8 quotes).
- KakaoCloud + NHN Cloud + Naver Cloud deep-doc verbatim are deferred to R11+
  per PRD §10 — host-wide pattern across 9 host attempts; further retries
  beyond SP47 pre-flight one-shot are out-of-scope this cycle.
