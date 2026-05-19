# R10 — api-gateway-relay Recipe PRD — 2026-05-23 (Round 10, ralplan iter 2)

> **Status:** ITER 2 (Planner iter 2; revised per Architect APPROVE-WITH-CHANGES + Codex Critic iter 1 ITERATE).
> **Date:** 2026-05-23. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-23-r10-api-gateway-relay-prd.draft.md` (iter 1 — 393 lines; Architect 2 HIGH + 4 MEDIUM; Codex iter 1 1 NEW BLOCKING + soft).
> - `2026-05-22-r9-webhook-internal-it-prd.md` (CLOSED — `v1.7.0-webhook-internal-it` on `main@97fb625`).
> - `2026-05-19-r6-recipes-prd.md` (CLOSED — `v1.4.0-recipes-complete`). Best-style precedent for "recipe-only, no new L4" cycle.
> **Branch (when execution starts):** `feat/r10-api-gateway-relay-sp47-sp48`.
> **Targeted tag:** `v1.8.0-api-gateway-relay` IFF SP48 sealed verdict ≥10/12 MUST + ≥5/8 SHOULD; no tag IFF verdict fails (SP47 reverts CLEAN — api-gateway-relay ABSENT from both active AND deferred per Codex L Option (a)).
> **Mandate trigger:** User said "R10 시작해 — api-gateway-relay 자율주행" — invokes R9 TD-027 forward-pointer.

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **R9 closed.** `v1.7.0-webhook-internal-it` shipped webhook L4 + internal-it recipe; `deferred_recipes:` queue CLOSED (Synthesis-A trim of 4 fully realized R7+R8+R9). 12 sealed verdicts all PASS; 22 hard guards GREEN.
- **R10 strategy.** User-approved direction: ship `api-gateway-relay` as **the named TD-027 forward-pointer candidate** from R9. Honors the "plausible R10+ deferred candidate" half of the 2-consumer-signal gate by VALIDATING the gate retroactively: webhook L4 now has 2 shipped consumers (internal-it R9 + api-gateway-relay R10).
- **Recipe-only.** R10 ships ZERO new L4. api-gateway-relay COMPOSES existing 12 L4 (webhook + auth + audit-log + crud + scheduled-task + optional notification + optional feature-flags). R6 atomic-precedent shape: 2 SPs (SP47 atomic + SP48 FINAL).
- **TD-027 2-consumer-signal gate NOT re-triggered.** Shipping a NEW L4 in R10 would self-fulfill the gate (planner proposes both the L4 + the qualifying recipe in same cycle). R10 is the FIRST shipped api-gateway-* recipe — the 2nd consumer for any new gateway primitive would be R11+. If R10 implementation reveals a primitive truly needs separation (rate-limit, api-key, etc.), defer to R11+ with FRESH evidence + explicit trigger (NOT via deferred-queue addition; queue stays closed per Codex L Option (a)).
- **Disk discoveries (2026-05-23).** `recipes/api-gateway-relay/` ABSENT; `specs/recipes/api-gateway-relay-recipe-l0.yaml` ABSENT; `templates/L4/webhook/README.md` `applied_recipes:` key currently `[internal-it]` (born R9 SP45b). R10 SP47 appends `api-gateway-relay` alphabetically → `[api-gateway-relay, internal-it]`. `specs/ratelimit-l0.yaml` exists on disk (4 items — `RATELIMIT-1..4`) and is bindable WITHOUT introducing a new L4 (rate-limit is currently spec-only with no L4 directory; R10 recipe binds the SPEC IDs directly via `spec_ref:`, treating rate-limiting as a cross-cutting concern enforced inside auth/webhook/crud layers).
- **Evidence rigor.** 5 logical English attempts (Kong + AWS + Cloudflare + Tyk + Apigee) + 4 logical Korean attempts (KakaoCloud + NHN Cloud + Naver Cloud + NAVER Cloud Platform service catalog fresh-vendor adjacent). Verbatim PASS: **5 English** (all 5 EN passed) + **2 Korean** (Toss Payments adjacent fallback after 3 Korean cloud-native 404 cascade + NAVER Cloud Platform service catalog fresh-vendor 200 OK iter 2 add). Architect H1 CLOSED via fresh-vendor add.

### Principles (8 numbered)

1. **Composition kit, not single product.** api-gateway-relay COMPOSES existing 12 L4. Zero new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill / practices rule family.
2. **Spec-before-code, evidence-anchored AT PRD SIGNATURE.** All 5 `spec_ref:` disk-verified at PRD signature against existing specs (webhook + auth + audit-log + scheduled-task + ratelimit). Korean ledger captures 3 cloud-native attempts + 2 adjacent-platform successes (Toss + NAVER Cloud Platform fresh-vendor per Architect H1).
3. **Binary verification per axis.** `/ax-verify` exits 0; `recipe_governance_guard.sh` exits 0 across 11 active recipes; `recipe_spec_referential_integrity_guard.sh` exits 0 across 11 specs; SP48 sealed verdict ≥10/12 MUST + ≥5/8 SHOULD.
4. **Tier-1 cap = 4. Tier-2 count = 8. L1 = 49, L2 = 92, L3 = 20. L4 = 12. FROZEN.** Recipes 10 → 11. Sealed verdicts 12 → 13.
5. **Atomic Spec Trio rule per SP.** SP47 ships api-gateway-relay quartet + spec + L4 README appends + sealed-verdict scaffold + manifest add + evidence snapshot atomically (R6 SP39 / R8 SP43 precedent).
6. **Recipe does not ship code; AI implements business logic.** Inherited R5/R6/R7/R8/R9.
7. **R9 TD-027 2-consumer-signal gate HONORED — no new L4 this cycle.** Shipping a new L4 + new recipe in same cycle re-triggers the gate self-fulfillingly (Architect H1 concern from R9 iter 1). R10 ships RECIPE ONLY; webhook L4 receives its 2nd consumer (api-gateway-relay) **post-hoc validating** the gate set in R9 TD-027. If R10 implementation reveals genuine primitive need (rate-limit-as-L4, api-key-as-L4), R11+ planner authors with fresh evidence chain — NOT via deferred-queue addition (Codex L Option (a)).
8. **No new L2 / L3 / practices rule families.** Recipe binds to existing rules + existing spec items only. Rate-limiting bound via `specs/ratelimit-l0.yaml#RATELIMIT-1..4` spec_ref (no L4 directory exists for ratelimit; spec-only binding is the deliberate "cross-cutting" framing).

### Decision Drivers (top 3)

1. **R9 TD-027 forward-pointer discipline.** R9 explicitly named api-gateway-relay as the "plausible R10+ deferred candidate" satisfying condition (c.2) of the 2-consumer-signal gate. R10 acts on this forward-pointer: webhook L4 acquires its 2nd shipped consumer (api-gateway-relay) and the gate is RETROACTIVELY VALIDATED.
2. **Evidence rigor.** 5 English verbatim PASS (Kong + AWS + Cloudflare + Tyk + Apigee — strongest English chain alongside R8 cms) + 2 Korean verbatim PASS (Toss Payments adjacent + NAVER Cloud Platform service catalog fresh-vendor per Architect H1 closure). Clears 1-floor with 5x EN buffer + 2x KO.
3. **R6 cadence parity.** R6 SP39 atomic-3 was the canonical "recipe-only, no new L4" precedent. R10 mirrors at n=1: SP47 atomic-1 + SP48 FINAL.

### Viable Options Considered (≥2 mandatory)

- **Option (1) — Atomic SP47 (api-gateway-relay only) + SP48 FINAL.** (R6 SP39 / R8 SP43 atomic-precedent style)
  - Pros: Smallest delta; one recipe; no L4 surface mutation beyond `applied_recipes:` plural-list appends (R6 dual-form regex + alphabetical-append already proven across R6/R7/R8/R9); validates R9 TD-027 forward-pointer without re-opening the L4-introduction gate; partial-tag policy trivially simplifies to PASS/FAIL binary (no recipe-axis partial possible at n=1); fail-state CLEAN REVERT per Codex L Option (a) — api-gateway-relay ABSENT from both active AND deferred, future R11+ can reintroduce with fresh evidence; preserves R9's hard-won `deferred_recipes: []` closure invariant under all outcomes.
  - Cons: Doesn't pre-emptively introduce rate-limit-as-L4 even if fork-receivers might want it — but TD-027 is explicit that 2-consumer-signal is required for L4 split, and rate-limit currently has ZERO shipped consumers; doesn't preserve api-gateway-relay state on a hypothetical sealed-verdict near-miss (10/12 MUST + 4/8 SHOULD = FAIL with no second-chance ledger) — but n=1 partial-tag binary mandates this rigor.
  - **CHOSEN.**

- **Option (2) — New L4 (api-gateway) + api-gateway-relay recipe in same SP47.** (R7 / R9 "new L4 + new recipe" style)
  - Pros: One unified gateway primitive surface; standard L4 introduction grammar applies; would establish an explicit gateway-primitive surface separate from webhook (which is currently an outbound-emit primitive being reused as a gateway emit channel).
  - Cons: **Re-triggers R9 Architect H1 self-fulfilling 2-consumer gate concern verbatim** — planner would propose BOTH the L4 AND its qualifying recipe in same cycle, exactly what TD-027 (c) was tightened to prevent. R9 Architect's H1 closure rationale explicitly relied on R10 demonstrating the discipline holds (no new L4 alongside the qualifying recipe). **REJECTED** per TD-027 discipline.

- **Option (3) — Spin rate-limit as its own L4 (no new recipe).** (Pre-emptive primitive split)
  - Pros: rate-limit spec exists on disk (4 items: `RATELIMIT-1..4`), could become a 13th L4 directory; would give cross-cutting rate-limit a first-class spec home rather than the current "cross-cutting concern" framing.
  - Cons: ZERO shipped consumers today; TD-027 condition (c) demands AT LEAST one shipped active recipe consuming the primitive AND one plausible R10+ deferred candidate. rate-limit-as-L4 fails BOTH halves. Also fails the "L4 ships its own ≥3 EN + ≥1 KO evidence chain" rule (R5/R9 precedent). **REJECTED.**

- **Option (4) — Defer api-gateway-relay past R10; wait for fork-receiver demand.**
  - Pros: Lowest-risk no-op; no execution surface introduced; preserves all R9 invariants without any new state.
  - Cons: TD-027 forward-pointer becomes dead-letter at R12 horizon; R9's careful 2-consumer-signal framing loses its single concrete validation; user explicitly invoked R10 with TD-027 framing ("R10 시작해 — api-gateway-relay 자율주행"). **REJECTED** — user-invoked + dead-letters forward-pointer.

### Mode

**SHORT.** Cycle is recipe-only (no new L4); no harness novelty; no Spec Trio net-new authoring; partial-tag policy trivial at n=1. R6 SP39 atomic precedent applies mechanically. Pre-mortem ≥3 scenarios + standard test plan sufficient. Wall-time ≈ 2-3 d.

### Recommended: Option (1) — 2 SPs (SP47 atomic-1 + SP48 FINAL).

```
SP47   (atomic — api-gateway-relay: recipe quartet + recipe spec + 6 L4 README applied_recipes
        appends (webhook plural-list append → [api-gateway-relay, internal-it] alphabetical;
        + auth/audit-log/crud/scheduled-task/notification/feature-flags appends) + manifest
        add + sealed-verdict scaffold PENDING + compose-spec test + evidence snapshot)
   ↓ gated on recipe_governance_guard.sh + recipe_spec_referential_integrity_guard.sh exit 0
SP48   (FINAL — sealed verdict exec + /ax-verify full + tag v1.8.0-api-gateway-relay IFF PASS + PR)
```

Total: **2 SPs, ≈ 2-3 d wall-time.**

---

## §2 Context

### R9 v1.7.0 disk-verified state (2026-05-23)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 49 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | **12** (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, scheduled-task, search, webhook) | `templates/L4/` |
| Active recipes | **10** (saas-subscription, e-commerce, crm, booking, marketplace, b2b-admin, community, lms, cms, internal-it) | `recipes/` |
| Deferred recipes | **0** (Synthesis-A queue CLOSED at R9; R10 KEEPS CLOSED — Codex L Option (a)) | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Sealed verdicts | **12** (all PASS) | `skills/_tests/sealed-verdict/` |
| Hard guards GREEN | 22 | `practices/evals/` |
| Practices rules | 68 (Tier-1/Tier-2 4/8 frozen) | `practices/rules/` |
| Current tag | `v1.7.0-webhook-internal-it` on `main@97fb625` | `git tag --sort=-creatordate \| head -1` |

### R10 scope

- **Deliverable (api-gateway-relay, SP47 atomic):** `recipes/api-gateway-relay/` quartet (RECIPE.md + L4-composition.md + L2-block-recipe.md + spec-trio-template.yaml) + `specs/recipes/api-gateway-relay-recipe-l0.yaml` (5 business_invariants, ALL spec_ref/rule_ref disk-resolvable at PRD signature). Composition: `webhook + auth + audit-log + crud + scheduled-task` (5 mandatory) + `notification + feature-flags` (2 optional). Plus cross-cutting `specs/ratelimit-l0.yaml` SPEC bindings (no L4 directory — spec-only).
- **Shared SP47 mutations:** `recipes/_MANIFEST.yaml` add (active recipes 10 → 11; `deferred_recipes: []` UNCHANGED — R10 does not re-open the queue and on FAIL Path stays UNCHANGED per Codex L Option (a)); `templates/L4/webhook/README.md` `applied_recipes:` list append (`[internal-it]` → `[api-gateway-relay, internal-it]` alphabetical); 6 other L4 READMEs (`auth, audit-log, crud, scheduled-task, notification, feature-flags`) plural-list append `api-gateway-relay` alphabetically.

NO new L1 / L2 / L3 / L4 / Tier-1 / Tier-2 skill / practices rule. Recipe count 10 → 11. L4 count UNCHANGED (12).

---

## §3 Objectives + Guardrails

### Must Have

- 1 recipe (`recipes/api-gateway-relay/`) full Spec Trio quartet.
- 1 recipe spec (`specs/recipes/api-gateway-relay-recipe-l0.yaml`) — `l2_blocks_used:` strictly disk-resolvable (R8 Critic L blocker precedent: only files at `templates/L2/blocks/*.tsx`).
- **5 INVs, each with ≥1 anchor; all anchors disk-resolvable.** *(M4 closure — wording per Architect M4 + Codex M4)*
- 1 sealed verdict (`skills/_tests/sealed-verdict/api-gateway-relay-verdict.md`) ≥10/12 MUST + ≥5/8 SHOULD.
- `recipes/_MANIFEST.yaml` append api-gateway-relay to `recipes:` (active block); `deferred_recipes: []` UNCHANGED — INCLUDING ON FAIL PATH (R6 Synthesis-A queue closed at R9; R10 does NOT re-open under any outcome per Codex L Option (a)).
- `templates/L4/webhook/README.md` `applied_recipes:` list-append `api-gateway-relay` alphabetically (current `[internal-it]` → `[api-gateway-relay, internal-it]`).
- 6 L4 READMEs (`auth, audit-log, crud, scheduled-task, notification, feature-flags`) plural-list append `api-gateway-relay` alphabetically.
- 1 × `frontend/tests/recipes/api-gateway-relay-compose.spec.ts`.
- 1 × `practices/upstream/r10-sp47-evidence-snapshot.md` with all 9 logical attempts + redirect/alternate rows + HTTP status + 2026-05-23 timestamp.
- `/ax-verify` exit 0; both recipe guards exit 0 against 11 active recipes/specs.
- Tag `v1.8.0-api-gateway-relay` IFF SP48 sealed verdict PASS.

### Must NOT Have

- NO new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill.
- NO new practices rule family.
- NO `RECIPE_DEVIATION.md` ceremony.
- NO `/ax-scaffold business <pattern> --analyze` free-text NLP.
- NO Korean reference fabrication. Cloud-native Korean 404 cascade is honest evidence per R8/R9 precedent.
- NO change to git workflow / CI policy / release process.
- NO partial deliverable within SP47.
- NO opening of `deferred_recipes:` queue UNDER ANY OUTCOME (PASS keeps `[]`; FAIL also keeps `[]` via SP47 clean revert per Codex L Option (a)).
- NO TD-027 re-triggering (no new L4 this cycle — Principle 7).

---

## §4 Recipe Inventory — ALL `spec_ref:` disk-verified

### 4.1 `api-gateway-relay`

> **Pre-commit RECIPE.md preamble (verbatim — per Architect M3 + Codex M3):** "api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4's outbound-emit primitive; NOT itself a primitive." This sentence MUST appear verbatim at the top of `recipes/api-gateway-relay/RECIPE.md` so the context-0 sealed sub-agent reads it as a disambiguation anchor (closes §7 P3).

- **L4 composition (5 mandatory + 2 optional):** `webhook` (mandatory — outbound fanout to backend services), `auth` (mandatory — gateway-level access control + token introspection), `audit-log` (mandatory — every relayed request emits audit row), `crud` (mandatory — endpoint registry + route config CRUD), `scheduled-task` (mandatory — per-endpoint circuit-breaker state reconciliation + dead-letter replay scheduling). **Optional:** `notification` (alerting on circuit-breaker open / dead-letter accumulation thresholds), `feature-flags` (per-route gating, canary routing).
- **L2 blocks used (disk-verified via `ls templates/L2/blocks/*.tsx` on 2026-05-23 — guard-resolvable only):** `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `confirm-dialog`, `bulk-actions-bar`.
- **L1 primitives consumed (documented in `recipes/api-gateway-relay/L2-block-recipe.md` "L1 primitives consumed" subsection — NOT in `l2_blocks_used:` per R8 Critic L blocker contract):** `code-block` (request/response payload preview), `relative-time` (last-request displays), `badge` (route status indicators: ACTIVE / PAUSED / CIRCUIT-OPEN / DEAD-LETTER).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants (all 5 disk-RESOLVED at PRD signature; each with ≥1 anchor per M4 wording):**

  - `API-GATEWAY-RELAY-INV-001`: Every relayed request to a backend service is signed via webhook L4's HMAC-SHA256 over `<timestamp>.<body>` AND emits an audit-log row with operator + relay_target_endpoint + delivery_id. `spec_ref: specs/webhook-l0.yaml#WEBHOOK-SIGN-001` (DISK-VERIFIED) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (DISK-VERIFIED). **PASS.**

  - `API-GATEWAY-RELAY-INV-002`: Relay authorization checks token validity AND scope BEFORE forwarding to upstream; on auth failure the request is rejected with audit-logged denial reason. `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.1.1` (DISK-VERIFIED) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (DISK-VERIFIED). **PASS.**

  - `API-GATEWAY-RELAY-INV-003`: Per-route rate-limiting enforced before relay; over-limit requests rejected with HTTP 429 AND counted via observability counter. `spec_ref: specs/ratelimit-l0.yaml#RATELIMIT-1` (DISK-VERIFIED) + `spec_ref: specs/ratelimit-l0.yaml#RATELIMIT-2` (DISK-VERIFIED). **PASS.**
    - **Cross-cutting spec binding rationale (R10-novel framing):** `ratelimit-l0.yaml` exists on disk as a 4-item spec WITHOUT a `templates/L4/ratelimit/` directory. R10 treats rate-limiting as a CROSS-CUTTING CONCERN enforced inside existing L4 boundaries (auth + webhook + crud filters), bound via spec_ref ONLY. This is deliberately DIFFERENT from spinning a new L4 — see Principle 7 + TD-027 (c) two-consumer-signal gate.

  - `API-GATEWAY-RELAY-INV-004`: Circuit breaker per upstream endpoint auto-opens on 90% failure rate over rolling 50 attempts; reconciliation of half-open / closed state runs via scheduled-task lock primitive (no double-reconcile on multi-node deployment). `spec_ref: specs/webhook-l0.yaml#WEBHOOK-CIRCUIT-001` (DISK-VERIFIED) + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` (DISK-VERIFIED). **PASS.**

  - `API-GATEWAY-RELAY-INV-005`: Route registration + config mutations (URL, method, target, signing-secret) follow CRUD validation rules AND emit audit rows with before/after diff. `spec_ref: specs/crud-security.yaml#CRUD-VAL-1` (DISK-VERIFIED) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (DISK-VERIFIED) + `rule_ref: practices/rules/idempotency-key-on-mutations.md` (DISK-VERIFIED). **PASS — 3 anchors, ≥1 floor met per M4 wording.**

- **INV disambiguation:** All 5 INVs bind to EXISTING spec items + EXISTING practices rules. No `co-shipped-rule` invocation needed.

- **Override allowance:** inline — skip `notification` for headless gateway deployments; skip `feature-flags` for monolithic single-route relays; skip `scheduled-task` for stateless circuit-breaker.

- **External evidence (verbatim — 5 English PASS):** Kong Gateway + AWS API Gateway + Cloudflare API Shield + Tyk + Apigee. See §4.4 ledger.

- **Korean evidence (verbatim — 2 PASS: Toss Payments adjacent + NAVER Cloud Platform service catalog fresh-vendor):** See §4.4 cascade documentation.

- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/api-gateway-relay-compose.spec.ts"
    assertion: "recipes/api-gateway-relay/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, auth, crud, scheduled-task, webhook] (alphabetical mandatory; notification + feature-flags documented as override_allowed) AND 5 disk-verified spec_ref/rule_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh AND l2_blocks_used: contains only files present at templates/L2/blocks/*.tsx AND RECIPE.md preamble contains the gateway-pattern-composer disambiguation sentence verbatim"
    expected_RED_reason: "recipes/api-gateway-relay/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/api-gateway-relay-compose.spec.ts"
    owning_SP: "SP47"
  ```

### 4.2 Cluster claim (R6 SP39 / R8 SP43 precedent — n=1 variant)

api-gateway-relay is a SINGLE-RECIPE cycle — no cross-recipe cluster. Mutation surface is purely append-only across 6 L4 READMEs (alphabetical plural-list append; R6 dual-form regex handles).

### 4.3 Webhook 2nd-consumer signal — R9 TD-027 retroactive validation (R10-specific)

R9 TD-027 condition (c) required TWO consumer signals for L4 split: (c.1) shipped active recipe (internal-it) + (c.2) plausible R10+ deferred candidate (api-gateway-relay forward-pointer). R10 ships api-gateway-relay as a SHIPPED active recipe — webhook L4 now has 2 SHIPPED consumers. The 2-consumer-signal gate is RETROACTIVELY VALIDATED for the webhook precedent. R10 does NOT create a new TD-027 instance — it CLOSES the open one.

### 4.4 Evidence ledger (WebFetched during PRD revision — 2026-05-23)

> **Counter wording:** 9 **logical** WebFetch attempts (1 attempt = 1 host targeted with intent to verbatim-cite). Additional rows are bookkeeping for redirect captures + adjacent-platform fallbacks. **Iter 2 add:** NAVER Cloud Platform service catalog fresh-vendor adjacent fetch per Architect H1.

| Source class | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| EN Kong | `https://docs.konghq.com/gateway/` | 301 redirect → `https://developer.konghq.com/gateway/` (2026-05-23) | — | Followed redirect | (redirect captured) |
| EN Kong | `https://developer.konghq.com/gateway/` | **200 OK — verbatim** (2026-05-23) | `"Kong Gateway is a lightweight, fast, and flexible cloud-native API gateway."` | Verbatim cite | `external` |
| EN AWS | `https://docs.aws.amazon.com/apigateway/` | **200 OK — verbatim** (2026-05-23) | `"Amazon API Gateway enables you to create and deploy your own REST and WebSocket APIs at any scale."` | Verbatim cite | `external` |
| EN Cloudflare | `https://developers.cloudflare.com/api-gateway/` | **HTTP 404** (2026-05-23) | — | alternate-fetched-as-bridge | — |
| EN Cloudflare | `https://developers.cloudflare.com/api-shield/` | **200 OK — verbatim** (2026-05-23) | `"Identify and address your API vulnerabilities."` | final-verbatim-via-alternate | `external` |
| EN Tyk | `https://tyk.io/docs/` | **200 OK — verbatim** (2026-05-23) | `"The hub for Tyk API management. Whether you're new or experienced, get started with Tyk, explore our product stack and core concepts, access in-depth guides, and actively contribute to our ever-evolving products."` | Verbatim cite | `external` |
| EN Apigee | `https://cloud.google.com/apigee/docs` | 301 redirect → `https://docs.cloud.google.com/apigee/docs` (2026-05-23) | — | Followed redirect | (redirect captured) |
| EN Apigee | `https://docs.cloud.google.com/apigee/docs` | **200 OK — verbatim** (2026-05-23) | `"With Apigee, you can build API proxies—RESTful, HTTP-based APIs that interact with your services."` | Verbatim cite | `external` |
| KO KakaoCloud | `https://docs.kakaocloud.com/service/cloud-edge/apigateway` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` — page does not exist |
| KO KakaoCloud (alt) | `https://docs.kakaocloud.com/` | **200 OK — no API Gateway descriptive content** (2026-05-23) | — | Downgrade | `internal_design` — homepage shell only |
| KO KakaoCloud (alt 2) | `https://kakaoi.kakaocloud.com/service/cloudEdge/apiGateway` | **ECONNREFUSED** (2026-05-23) | — | Downgrade | `internal_design` — host unreachable |
| KO NHN Cloud | `https://docs.nhncloud.com/ko/Application%20Service/API%20Gateway` | 301 → `http://docs.nhncloud.com/...` (2026-05-23) | — | Followed redirect | (redirect captured) |
| KO NHN Cloud | `http://docs.nhncloud.com/ko/Application%20Service/API%20Gateway/` | 302 → error page (2026-05-23) | — | Downgrade | `internal_design` — redirect-to-error |
| KO NHN Cloud (alt) | `https://meetup.nhncloud.com/posts/250` | **HTTP 404** — `"404 존재하지 않는 페이지 입니다."` (2026-05-23) | — | Downgrade | `internal_design` |
| KO Naver Cloud | `https://api.ncloud-docs.com/docs/apigateway-overview` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` |
| KO Naver Cloud (alt) | `https://www.ncloud.com/product/applicationService/apiGateway` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` |
| KO Naver Cloud (alt 2) | `https://guide.ncloud-docs.com/docs/apigw-overview` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` |
| KO adjacent fallback (Toss) | `https://docs.tosspayments.com/reference` | **200 OK — verbatim Korean** (2026-05-23) | `"토스페이먼츠 API 엔드포인트(Endpoint)와 객체 정보, 파라미터, 요청 및 응답 예제를 살펴보세요."` | Verbatim cite (adjacent platform — R9 Toss precedent) | `external` |
| **KO fresh-vendor (NAVER Cloud Platform) — ITER 2 ADD per Architect H1** | `https://www.ncloud.com/product` | **200 OK — verbatim Korean** (2026-05-23) | `"API 호출, 관리, 모니터링 등 API와 관련된 모든 작업을 실행할 수 있는 서비스"` | **Verbatim cite (fresh-vendor adjacent — NAVER Cloud Platform service catalog page; different vendor than R9 Toss; verbatim describes API gateway operating semantics: invocation/management/monitoring — closes Architect H1 + Codex H1)** | `external` |

**Per-recipe evidence density floor:**

- **api-gateway-relay:** 5 English verbatim + 2 Korean verbatim (Toss adjacent + NAVER Cloud Platform fresh-vendor iter 2 add). PASS — clears 1-floor with 5x EN buffer + 2x KO. Architect H1 + Codex H1 CLOSED.

- **Korean cycle rationale:** Korean cloud-native API gateway deep-doc hosts (KakaoCloud docs, NHN Cloud docs, Naver Cloud docs) ALL exhibit host-wide URL invalidity (9 host attempts: 404 / 301-302-to-error / ECONNREFUSED). Iter 2 surfaces NAVER Cloud Platform's product catalog page itself returns 200 OK with verbatim Korean describing API gateway service semantics — closes Architect H1 fresh-vendor demand without dropping Toss adjacent (which remains as documented R9-precedent fallback).

- **Per-source-class arithmetic (M1 closure per Architect + Codex):** "Verbatim cite rows = 6; Downgrade rows = 7; Followed-redirect rows = 3; alternate-fetched-as-bridge rows = 1; final-verbatim-via-alternate rows = 1; Toss adjacent row = 1; total 19 rows incl. header+separator." *(Iter 2 add: +1 fresh-vendor verbatim row from NAVER Cloud Platform → verbatim cite rows = 7; updated total = 20 incl. header+separator.)*
  - Reading guide for the 7 verbatim cite rows: Kong (developer.konghq.com 200 OK after 301 from docs.konghq.com); AWS (docs.aws.amazon.com/apigateway/ 200 OK direct); Cloudflare API Shield (developers.cloudflare.com/api-shield/ 200 OK after /api-gateway/ 404 bridge); Tyk (tyk.io/docs/ 200 OK direct); Apigee (docs.cloud.google.com/apigee/docs 200 OK after 301 from cloud.google.com); Toss Payments (docs.tosspayments.com/reference 200 OK adjacent fallback — R9 precedent); NAVER Cloud Platform (www.ncloud.com/product 200 OK fresh-vendor adjacent — iter 2 add per Architect H1).
  - Reading guide for the 7 Downgrade rows: KakaoCloud × 3 (page 404 + homepage shell-only + kakaoi host ECONNREFUSED); NHN Cloud × 2 (302-to-error after 301 chain + meetup posts/250 404); Naver Cloud × 2 deep-doc URLs (api.ncloud-docs.com 404 + guide.ncloud-docs.com 404). Note: ncloud.com `/product/applicationService/apiGateway` deep page returns 404 separately and is one of the Naver Cloud downgrade rows — distinct from the iter 2 fresh-vendor verbatim hit on `/product` catalog root.

**Re-attempt at SP execution:** SP47 pre-flight one host probe per Korean cloud platform (KakaoCloud root + NHN Cloud root + Naver Cloud root) — single-shot, no fabrication.

---

## §4.5 SP Plan + Verification Matrix (2 SPs)

| SP | Atomic deliverables | TDD anchors | Verification | Observability (advisory) |
|---|---|---|---|---|
| **SP47** (atomic — api-gateway-relay) | (a) `recipes/api-gateway-relay/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}` including gateway-composer-vs-webhook-primitive preamble verbatim; (b) `specs/recipes/api-gateway-relay-recipe-l0.yaml` (5 INVs disk-resolvable per §4.1); (c) `recipes/_MANIFEST.yaml` append api-gateway-relay (`deferred_recipes: []` UNCHANGED); (d) `templates/L4/webhook/README.md` plural-list append; (e) 6 L4 READMEs plural-list append; (f) `skills/_tests/sealed-verdict/api-gateway-relay-verdict.md` scaffold PENDING; (g) `frontend/tests/recipes/api-gateway-relay-compose.spec.ts`; (h) `practices/upstream/r10-sp47-evidence-snapshot.md`. | compose-test RED → GREEN; `recipe_governance_guard.sh` RED → GREEN; `recipe_spec_referential_integrity_guard.sh` RED → GREEN; webhook L4 alphabetical-insert mutation RED → GREEN. | `bash practices/evals/recipe_governance_guard.sh` exit 0 across 11 active recipes; `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exit 0 across 11 specs; `/ax-verify-domain` × 7 touched L4. | `recipe.api_gateway_relay.relay_request_total`, `recipe.api_gateway_relay.relay_rate_limit_rejected_total`, `recipe.api_gateway_relay.circuit_breaker_open_total`. |
| **SP48** (FINAL — verdict exec + tag + PR) | Sealed sub-agent exec × 1 (context-0 input); `recipes/README.md` updated (11 active recipes listed); `/ax-verify` exit 0; tag policy applied. | Sealed verdict harness × 1 PENDING → PASS. | `/ax-verify` exit 0; tag policy enforced (see §6 table — binary PASS/FAIL at n=1; FAIL = CLEAN REVERT, no deferred-queue addition per Codex L Option (a)). | `recipes.active_total: 11`; `L4.domain_total: 12` (UNCHANGED). |

**SP atomicity:** SP47 ships full quartet + spec + manifest add + 7 L4 README appends + verdict scaffold + compose-test + evidence snapshot together OR rollback CLEAN (Codex L Option (a)).
**SP linearization:** SP47 → SP48. No parallel branches.

---

## §5 Webhook L4 `applied_recipes:` alphabetical-insert mutation (R10-specific)

R9 SP45b birthed webhook README `applied_recipes: [internal-it]` per TD-024 first-consumer-arrival. R10 SP47 PERFORMS THE FIRST 2-element insertion — alphabetical insertion of `api-gateway-relay` BEFORE `internal-it` → `[api-gateway-relay, internal-it]`. R6 dual-form regex already accepts this shape.

### Migration plan (within SP47)

1. **Webhook README mutation:** Edit `templates/L4/webhook/README.md` `applied_recipes:` block — insert `- api-gateway-relay` line ABOVE the existing `- internal-it` line. Final block reads:
   ```yaml
   applied_recipes:
     - api-gateway-relay
     - internal-it
   ```
2. **6 other L4 READMEs append:** `auth, audit-log, crud, scheduled-task, notification, feature-flags` each gain `- api-gateway-relay` line in their respective `applied_recipes:` lists alphabetically (R6 SP39 / R8 SP43 / R9 SP45b proven shape).
3. **No new fixtures needed.** R6 dual-form regex + alphabetical-append already handles. R8 TD-024 first-consumer-arrival convention DOES NOT APPLY to webhook (it became a 2-consumer L4 at R10 — past first arrival). `file-storage` + `practices` L4 READMEs remain `applied_recipes:`-key-less (no R10 consumer arrives; H2/M4 unused-L4 precedent preserved).
4. **Disk-verify via `recipe_governance_guard.sh`** — exit 0 across all 11 active recipes (including api-gateway-relay).

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP47 starts):** §4.4 evidence captured. SP47 pre-flight re-runs one host probe per Korean cloud platform — single-shot; document HTTP status + timestamp. Disk-verify `recipes/api-gateway-relay/` ABSENCE; disk-verify `specs/recipes/api-gateway-relay-recipe-l0.yaml` ABSENCE; disk-verify webhook L4 README current `applied_recipes:` shape (`[internal-it]`); disk-verify 5 INV spec_ref/rule_ref anchors per §4.1 still resolve. Disk-verify L2 inventory by `ls templates/L2/blocks/*.tsx`. **Audit-confirmed ratelimit guard tolerance (per Codex soft + Architect H2 downgrade):** Codex iter 1 audited `practices/evals/recipe_spec_referential_integrity_guard.sh` and confirmed `spec_ref` resolution checks file existence + ID presence only (NOT L4 directory presence) — `specs/ratelimit-l0.yaml#RATELIMIT-1/2` cross-cutting binding is GUARD-COMPATIBLE without `templates/L4/ratelimit/`. SP47 pre-flight grep validates this audit finding holds at execution time. Abort if any prep step fails.
- **Mid-flight gate (between SP47 and SP48):** `git status` clean; `/ax-verify-domain` × 7 touched L4 exit 0; both recipe guards exit 0 across 11 active recipes; `frontend/tests/recipes/api-gateway-relay-compose.spec.ts` passes; commit message references SP47 + 5 INV IDs.
- **Stop conditions:** If `recipe_spec_referential_integrity_guard.sh` cannot reach GREEN within 3 iter cycles, SP47 rolls back CLEAN (per Codex L Option (a)) — api-gateway-relay ABSENT from both `recipes/` active block AND `deferred_recipes:`. `deferred_recipes:` stays `[]`. Future R11+ may reintroduce api-gateway-relay only with FRESH evidence chain + explicit trigger.
- **Sealed verdict release policy (n=1 binary):** Tag `v1.8.0-api-gateway-relay` IFF SP48 verdict ≥10/12 MUST + ≥5/8 SHOULD. No tag IFF FAIL — SP47 reverted CLEAN; api-gateway-relay ABSENT from both active AND deferred per Codex L Option (a). Future R11+ proposal must furnish fresh evidence chain + explicit trigger (not via deferred-queue resurrection).
- **Rollback:** Each SP is one squash-mergeable commit. Revert SP47 cleanly without disturbing R9 state AND without adding any deferred-queue entry.
- **No destructive ops:** No `git reset --hard`, no force push. webhook README append-only mutation.

### Partial-tag policy (R8 small table precedent — degenerate at n=1)

Single recipe; no recipe-axis partial possible.

| SP48 verdict outcome | Tag | `_MANIFEST.yaml` membership | `recipes/api-gateway-relay/RECIPE.md` `status:` | Webhook L4 README `applied_recipes:` |
|---|---|---|---|---|
| 1/1 PASS | `v1.8.0-api-gateway-relay` | api-gateway-relay `active` (11 active, **0 deferred** — `deferred_recipes: []` UNCHANGED) | `active` | `[api-gateway-relay, internal-it]` |
| 0/1 FAIL | no tag | **SP47 reverted CLEAN; api-gateway-relay ABSENT from BOTH active AND `deferred_recipes:` (queue stays `[]`)** — *(per Codex L Option (a) — voluntary recipe failure does NOT create a new deferred-queue entry; R11+ may reintroduce only with fresh evidence + explicit trigger)* | n/a (directory removed) | `[internal-it]` (SP47 webhook append reverted) |

**Rationale:** At n=1, partial-tag policy collapses to binary. Codex L Option (a) chosen: clean revert preserves `deferred_recipes: []` invariant under all outcomes (PASS keeps `[]`; FAIL also keeps `[]`). No new deferred category created; R9 closure preserved. R11+ reintroduction requires fresh evidence chain + explicit user/forward-pointer trigger.

---

## §7 Pre-Mortem (4 scenarios — SHORT mode floor)

1. **Korean cloud-native API gateway deep-doc hosts remain host-wide 404/blocked at SP47 execution.** Likelihood: HIGH. Impact: Korean verbatim count = 2 (Toss adjacent + NAVER Cloud Platform fresh-vendor). Mitigation: §4.4 documents host-wide pattern + iter 2 fresh-vendor add closes H1. SP47 pre-flight one-shot per platform root does NOT block.

2. **`specs/ratelimit-l0.yaml` spec_ref binding misinterpreted as new L4 introduction.** Likelihood: LOW. Impact: TD-027 falsely re-triggered. Mitigation: §4.1 INV-003 framing + §6 audit-confirmed guard tolerance sentence + §8 TD-028 ADR.

3. **Sealed verdict for api-gateway-relay scores below threshold because context-0 sub-agent cannot disambiguate api-gateway-relay from webhook L4 itself.** Likelihood: LOW. Impact: SP48 no-tag → CLEAN REVERT (no deferred-queue addition per Codex L Option (a)). Mitigation: `recipes/api-gateway-relay/RECIPE.md` preamble verbatim sentence "api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4's outbound-emit primitive; NOT itself a primitive." (M3 closure). Sub-agent reads as anchor.

4. **`applied_recipes:` alphabetical insertion on webhook L4 README mistakenly REPLACES `[internal-it]` instead of INSERTING.** Likelihood: LOW. Impact: R9 internal-it loses its webhook membership declaration. Mitigation: SP47 mutation is APPEND-ONLY semantic; compose-test asserts BOTH names present; `recipe_governance_guard.sh` exit 0 across ALL 11 active recipes (regression detection).

---

## §8 ADR Template (2 entries — TD-028, TD-029)

- **TD-2026-05-23-028 (NEW)** — Recipe `api-gateway-relay` shipped via composition of existing 12 L4.
  - **Decision:** `recipes/api-gateway-relay/` deferred→active. Active 10 → 11. Composition: `webhook + auth + audit-log + crud + scheduled-task` (mandatory) + `notification + feature-flags` (optional) + `specs/ratelimit-l0.yaml` (cross-cutting, NO new L4).
  - **Pre-commit disambiguation sentence (M3 — verbatim in RECIPE.md):** "api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4's outbound-emit primitive; NOT itself a primitive."
  - **Drivers:** R9 TD-027 named api-gateway-relay as forward-pointer; 5 English verbatim PASS; 2 Korean verbatim PASS (Toss adjacent + NAVER Cloud Platform fresh-vendor iter 2 add); 5 INVs all disk-resolvable.
  - **Alternatives considered:** Option 2 new L4 + recipe same SP (rejected — self-fulfills TD-027); Option 3 ratelimit-as-L4 (rejected — zero shipped consumers); Option 4 defer past R10 (rejected — user-invoked).
  - **Why chosen:** Composition-kit reuse at maximum expression; validates R9 TD-027 retroactively; recipe-only / no-new-L4 discipline.
  - **Consequences:** 7 L4 READMEs gain `api-gateway-relay`. webhook README shape `[internal-it]` → `[api-gateway-relay, internal-it]`. `deferred_recipes: []` UNCHANGED under all outcomes (Codex L Option (a)).
  - **Follow-ups:**
    - R11+ refresh re-attempts Korean cloud-native API gateway deep-doc hosts if any platform unblocks.
    - Rate-limit L4 promotion if 2nd recipe consumer arrives organically.
    - api-gateway-relay sealed verdict re-execution annually per R5-R9 catalog-quality cadence.
    - **Korean adjacent fallback rotation precedent (M2 closure):** R10 iter 2 establishes that fresh-vendor Korean adjacent verbatim from a DIFFERENT vendor than the previous cycle's anchor is the rotation precedent (R9 Toss → R10 NAVER Cloud Platform fresh-vendor + Toss preserved). If R11 + R12 both fall back to Toss again, R12 planner MUST escalate to dedicated Korean-vendor-diversity guard OR accept Toss-as-permanent-adjacent precedent via explicit ADR.

- **TD-2026-05-23-029 (NEW)** — TD-027 2-consumer-signal gate HONORED in R10 by deliberate no-new-L4 decision.
  - **Decision:** R10 ships RECIPE ONLY. NO new L4. Validates R9 TD-027 (c) by supplying the 2nd consumer signal for webhook.
  - **Drivers:** R9 Architect H1 tightened TD-027 (c) to TWO consumer signals — R10 is the FIRST operational test of that tightening.
  - **Alternatives considered:** Option 2 same-SP L4+recipe (rejected); Option 3 ratelimit-as-L4 (rejected); pre-emptive primitive split (rejected).
  - **Why chosen:** First operational test of TD-027 (c) tightening; R10 is the "discipline holds" demonstration.
  - **Consequences:** R10 ships at L4 = 12. Future R11+ L4-introduction proposals MUST satisfy ALL THREE TD-027 conditions independently. **`deferred_recipes: []` invariant strengthened (Codex L Option (a)):** voluntary recipe failure no longer creates new deferred-queue entries.
  - **Follow-ups:** First R11+ application of TD-027 generates a precedent log entry; voluntary-recipe-failure precedent established (CLEAN REVERT, no deferred resurrection).

---

## §9 Honored Constraints

- **Caps:** Tier-1 = 4 FROZEN · Tier-2 = 8 UNCHANGED · L1/L2/L3 = 49/92/20 UNCHANGED.
- **Deltas:** L4 = 12 (UNCHANGED) · Recipes 10 → 11 · **Deferred 0 → 0 (queue NOT re-opened under any outcome — Codex L Option (a))** · Sealed verdicts 12 → 13.
- **Atomic SP rule per axis** (R6 SP39 / R8 SP43 precedent).
- **TD-027 2-consumer-signal gate HONORED** (R10 supplies 2nd consumer for webhook).
- **Korean references** — 3 Korean cloud-native logical hosts attempted + 2 adjacent-platform fallbacks (Toss R9-precedent + NAVER Cloud Platform fresh-vendor iter 2 add).
- **Cloud-native Korean cluster downgrade** — host-wide 404/blocked pattern across 9 deep-doc attempts.
- **CRUD spec-path** — `specs/crud-security.yaml` (R9 precedent).
- **DECISIONS.md format** — R7+ ADR bullet format honored.
- **No empty applied-recipes array** (R7 H2/M4).
- **Sealed verdict threshold** — ≥10/12 MUST + ≥5/8 SHOULD.
- **INV anchor preference** — all 5 INVs bind existing rules + spec items; NO `co-shipped-rule` invocation.
- **R6 dual-form regex + alphabetical-append** — webhook README transitions to 2-element list.
- **`deferred_recipes:` queue stays CLOSED UNDER ALL OUTCOMES** — R9 closure preserved; PASS keeps `[]`; FAIL also keeps `[]` via clean revert (Codex L Option (a)).

---

## §10 Out-of-scope (R10 explicit) + Deferred Recipes (stays EMPTY post-R10)

### Deferred recipes — QUEUE STAYS CLOSED (under all outcomes)

`recipes/_MANIFEST.yaml#deferred_recipes:` stays `[]` post-SP47 regardless of SP48 verdict outcome. R10 is a VOLUNTARY recipe addition. PASS keeps queue empty; FAIL also keeps queue empty (SP47 clean revert per Codex L Option (a)).

| Future R11+ deferred candidate (illustrative — NO entries today) | Trigger to re-open queue |
|---|---|
| `rate-limit-as-L4` (if R10 implementation surfaces a 2nd recipe consumer organically) | Fork-receiver demand + 2nd shipped active recipe consuming rate-limit primitive + ≥3 English verbatim + explicit ADR re-opening the queue |
| `api-key-as-L4` | Same gate as above |
| Other gateway-adjacent primitives | TD-027 condition (c) satisfied (NO self-fulfillment) + explicit queue re-open ADR |

### Out-of-scope (R10)

- New L1/L2/L3/L4 surface · new Tier-1/Tier-2 skill · new practices rule families.
- New L4 introduction (deliberate — TD-029).
- Frontend code · backend impls (recipe binds to existing backends).
- Deployment / CI / release scope · `RECIPE_DEVIATION.md` ceremony.
- Korean cloud-native API gateway deep-doc verbatim retry beyond SP47 pre-flight one-shot.
- New recipe candidates for R11+ deferred queue.
- Rate-limit L4 promotion (deferred to R11+).

---

## §11 Branch + path summary

- **Branch:** `feat/r10-api-gateway-relay-sp47-sp48` (cut from `main@97fb625`).
- **PRD path (this iter):** `docs/superpowers/specs/2026-05-23-r10-api-gateway-relay-prd.iter2.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — `deferred_recipes: []` UNCHANGED under all outcomes.
- **DECISIONS.md target:** `practices/DECISIONS.md` — append TD-2026-05-23-028 + TD-2026-05-23-029.
- **Evidence snapshot path:** `practices/upstream/r10-sp47-evidence-snapshot.md` — 9 logical attempts + redirect/alternate rows + per-source-class arithmetic.
- **Final tag:** `v1.8.0-api-gateway-relay` IFF SP48 PASS. No tag IFF FAIL (SP47 clean revert; no deferred entry).

---

## §12 Verdict line

R10 iter 2 PRD closes Architect 2 HIGH + 4 MEDIUM + Codex L NEW BLOCKING + 3 soft: **api-gateway-relay recipe** COMPOSING existing 12 L4 (5 mandatory + 2 optional + 1 cross-cutting `ratelimit-l0.yaml` spec binding); **5 English verbatim PASS** (Kong + AWS + Cloudflare API Shield + Tyk + Apigee); **2 Korean verbatim PASS** (Toss Payments adjacent + NAVER Cloud Platform fresh-vendor iter 2 add — closes Architect H1 + Codex H1); **R9 TD-027 honored via deliberate no-new-L4 decision** (TD-029); **ratelimit cross-cutting binding audit-confirmed compatible** (Codex H2 downgrade path + soft closure); **5 INVs each with ≥1 anchor; all disk-resolvable** (M4 wording); **per-source-class arithmetic restated** (M1); **RECIPE.md disambiguation preamble pre-committed verbatim** (M3); **Korean vendor rotation precedent ADR Follow-up added** (M2); **deferred-queue invariant strengthened via Codex L Option (a) — clean revert, no fail-state queue addition** (L closure); **2 SPs** (SP47 atomic-1 + SP48 FINAL — R6 SP39 / R8 SP43 precedent); **partial-tag policy degenerate at n=1**; **2 new ADRs** (TD-028 + TD-029). 2 SPs, ≈ 2-3 d wall-time. Ready for Architect + Codex Critic iter 2 review.

---

## RALPLAN-DR Summary (iter 2)

**Mode:** SHORT (recipe-only; no L4 introduction; no harness novelty; n=1 partial-tag binary).

**Principles (8):** composition-kit · spec-before-code · binary-verification · Tier-1/2-frozen · atomic-Spec-Trio · recipe-no-code · TD-027-honored-no-new-L4 · no-new-L2/L3/rule-family.

**Decision Drivers (top 3):** R9 TD-027 forward-pointer discipline · evidence rigor (5 EN + 2 KO with iter 2 fresh-vendor add) · R6 cadence parity.

**Viable Options:** (1) atomic SP47 + SP48 CHOSEN with Codex L Option (a) clean-revert fail-state · (2) new L4 + recipe same SP REJECTED · (3) ratelimit-as-L4 REJECTED · (4) defer past R10 REJECTED.

**Recommended path:** SP47 atomic (recipe quartet + spec + 7 L4 README appends + manifest add + verdict scaffold + compose-test + evidence snapshot including fresh-vendor NAVER Cloud Platform row) → SP48 FINAL (verdict exec + tag IFF PASS; FAIL = clean revert, no deferred entry).

**Wall-time:** 2-3 days.

**Pre-mortem:** 4 scenarios (HIGH Korean cascade — partly mitigated by iter 2 fresh-vendor add · LOW ratelimit-misframing · LOW verdict-disambiguation · LOW alphabetical-insert regression).

**Test plan:** unit (recipe-spec YAML structure) · integration (both recipe guards × 11 + audit-confirmed ratelimit cross-cutting binding) · E2E (compose-spec × 1 + `/ax-verify-domain` × 7) · observability (3 advisory counters).

**ADRs:** TD-028 (api-gateway-relay recipe + Korean vendor rotation Follow-up) · TD-029 (TD-027 no-L4-split discipline + clean-revert voluntary-failure precedent).

**Tag policy:** v1.8.0-api-gateway-relay IFF SP48 PASS; no tag IFF FAIL (n=1 binary; FAIL = SP47 clean revert, `deferred_recipes: []` UNCHANGED).

**Evidence ledger summary:** **5 English verbatim** + **2 Korean verbatim** (Toss adjacent + NAVER Cloud Platform fresh-vendor iter 2) + **7 documented downgrades** + **3 redirect/alternate rows**. Verbatim PASS = 7 (5 EN + 2 KO). 9 logical attempts + 4 redirect/alternate/bridge rows = 20 raw table rows incl. header+separator.

**Catalog deltas:** L4 = 12 UNCHANGED · Recipes 10 → 11 · Deferred 0 → 0 (queue stays closed under all outcomes — Codex L Option (a)) · Sealed verdicts 12 → 13; Tier-1/2 4/8 + practices 68 + L1/L2/L3 49/92/20 UNCHANGED.

---

## Iter 2 changelog

Each closure mapped to its blocker ID + target line(s):

- **H1 (Architect HIGH) — Fresh-vendor Korean adjacent attempt added.** §4.4 ledger row added: NAVER Cloud Platform service catalog (`https://www.ncloud.com/product`) — 200 OK verbatim `"API 호출, 관리, 모니터링 등 API와 관련된 모든 작업을 실행할 수 있는 서비스"`. Different vendor than R9 Toss. **Lines: §4.4 last table row + §4.4 per-recipe density floor + §4.4 per-source-class arithmetic + §1 cycle frame 6th bullet (5 EN + 2 KO updated) + §1 Decision Driver 2 + §12 verdict line.** WebFetch attempted in priority order (d2.naver.com blocked → toss.tech 404/no-verbatim → banksalad.engineering homepage 200 no-verbatim → ncloud.com /product 200 VERBATIM PASS).
- **H2 (Architect HIGH → DOWNGRADED soft) — Ratelimit guard tolerance audit-confirmed sentence.** §6 pre-flight gate adds: "Codex iter 1 audited `practices/evals/recipe_spec_referential_integrity_guard.sh` and confirmed `spec_ref` resolution checks file existence + ID presence only (NOT L4 directory presence) — `specs/ratelimit-l0.yaml#RATELIMIT-1/2` cross-cutting binding is GUARD-COMPATIBLE without `templates/L4/ratelimit/`." **Lines: §6 pre-flight gate paragraph.**
- **M1 (Architect MEDIUM) — Ledger arithmetic restated.** §4.4 per-source-class breakdown updated verbatim: "Verbatim cite rows = 6; Downgrade rows = 7; Followed-redirect rows = 3; alternate-fetched-as-bridge rows = 1; final-verbatim-via-alternate rows = 1; Toss adjacent row = 1; total 19 rows incl. header+separator." Plus iter 2 increment for NAVER Cloud Platform fresh-vendor row (verbatim cite rows = 7; total = 20). **Lines: §4.4 per-source-class arithmetic paragraph.**
- **M2 (Architect MEDIUM) — Korean vendor rotation precedent.** TD-028 Follow-ups bullet added: "Korean adjacent fallback rotation precedent (M2 closure): R10 iter 2 establishes that fresh-vendor Korean adjacent verbatim from a DIFFERENT vendor than the previous cycle's anchor is the rotation precedent (R9 Toss → R10 NAVER Cloud Platform fresh-vendor + Toss preserved). If R11 + R12 both fall back to Toss again, R12 planner MUST escalate to dedicated Korean-vendor-diversity guard OR accept Toss-as-permanent-adjacent precedent via explicit ADR." **Lines: §8 TD-028 Follow-ups bullets.**
- **M3 (Architect MEDIUM + Codex soft) — RECIPE.md disambiguation preamble pre-committed verbatim.** "api-gateway-relay is a GATEWAY-PATTERN COMPOSER that registers and routes inbound traffic to multiple backend services via webhook L4's outbound-emit primitive; NOT itself a primitive." Added to §4.1 (top), §8 TD-028 (Pre-commit disambiguation sentence bullet), §7 P3 mitigation, and tdd_anchor assertion clause. **Lines: §4.1 top preamble quote + §4.1 tdd_anchor assertion + §7 P3 mitigation + §8 TD-028 disambiguation bullet.**
- **M4 (Architect MEDIUM + Codex M4) — Must-Have wording fix.** §3 Must Have row replaces "5 business_invariants, ALL spec_ref/rule_ref disk-resolvable" with "5 INVs, each with ≥1 anchor; all anchors disk-resolvable." Plus §4.1 INV list intro echoes the new wording. **Lines: §3 Must Have 3rd bullet + §4.1 INV section intro.**
- **L NEW BLOCKING (Codex Critic — clean REVERT Option (a) chosen).** Fail-state policy unified across §3 Must Have + §6 pre-flight + §6 stop conditions + §6 sealed-verdict release policy + §6 rollback + §6 partial-tag table FAIL row + §8 TD-028 Consequences + §8 TD-029 Consequences + §9 Honored Constraints + §10 deferred section + §11 final-tag bullet. Verbatim policy: voluntary recipe failure → SP47 CLEAN REVERT; `deferred_recipes: []` UNCHANGED; api-gateway-relay ABSENT from BOTH active AND deferred; R11+ reintroduction requires fresh evidence + explicit trigger. NO deferred-queue addition under any outcome. **Lines: §3 Must Have 5th + 11th bullets + §6 stop conditions + §6 sealed verdict release policy + §6 partial-tag table FAIL row + §8 TD-028 Consequences + §8 TD-029 Consequences + §9 Honored Constraints "Deferred 0 → 0" line + §10 deferred-recipes section paragraph + §11 final-tag bullet.**
- **Codex soft (guard audit sentence) — Closed under H2 above.** §6 pre-flight gate sentence.
- **Codex soft (M3 disambiguation verbatim in §4.1 + TD-028) — Closed under M3 above.**
- **Codex soft (§4.4 arithmetic separation) — Closed under M1 above.**
