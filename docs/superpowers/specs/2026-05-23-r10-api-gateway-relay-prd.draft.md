# R10 — api-gateway-relay Recipe PRD — 2026-05-23 (Round 10, ralplan iter 1 DRAFT)

> **Status:** DRAFT (Planner iter 1; ready for Architect + Codex Critic review).
> **Date:** 2026-05-23. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Predecessors:**
> - `2026-05-22-r9-webhook-internal-it-prd.md` (CLOSED — `v1.7.0-webhook-internal-it` on `main@97fb625`).
> - `2026-05-21-r8-lms-cms-prd.md` (CLOSED — `v1.6.0-lms-cms`).
> - `2026-05-19-r6-recipes-prd.md` (CLOSED — `v1.4.0-recipes-complete`). Best-style precedent for "recipe-only, no new L4" cycle.
> **Branch (when execution starts):** `feat/r10-api-gateway-relay-sp47-sp48`.
> **Targeted tag:** `v1.8.0-api-gateway-relay` IFF SP48 sealed verdict ≥10/12 MUST + ≥5/8 SHOULD; no tag IFF verdict fails (SP47 reverts; no partial since single recipe).
> **Mandate trigger:** User said "R10 시작해 — api-gateway-relay 자율주행" — invokes R9 TD-027 forward-pointer.

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **R9 closed.** `v1.7.0-webhook-internal-it` shipped webhook L4 + internal-it recipe; `deferred_recipes:` queue CLOSED (Synthesis-A trim of 4 fully realized R7+R8+R9). 12 sealed verdicts all PASS; 22 hard guards GREEN.
- **R10 strategy.** User-approved direction: ship `api-gateway-relay` as **the named TD-027 forward-pointer candidate** from R9. Honors the "plausible R10+ deferred candidate" half of the 2-consumer-signal gate by VALIDATING the gate retroactively: webhook L4 now has 2 shipped consumers (internal-it R9 + api-gateway-relay R10).
- **Recipe-only.** R10 ships ZERO new L4. api-gateway-relay COMPOSES existing 12 L4 (webhook + auth + audit-log + crud + scheduled-task + optional notification + optional feature-flags). R6 atomic-precedent shape: 2 SPs (SP47 atomic + SP48 FINAL).
- **TD-027 2-consumer-signal gate NOT re-triggered.** Shipping a NEW L4 in R10 would self-fulfill the gate (planner proposes both the L4 + the qualifying recipe in same cycle). R10 is the FIRST shipped api-gateway-* recipe — the 2nd consumer for any new gateway primitive would be R11+. If R10 implementation reveals a primitive truly needs separation (rate-limit, api-key, etc.), defer to R11+ deferred-recipes queue (which starts fresh post-R9 closure).
- **Disk discoveries (2026-05-23).** `recipes/api-gateway-relay/` ABSENT; `specs/recipes/api-gateway-relay-recipe-l0.yaml` ABSENT; `templates/L4/webhook/README.md` `applied_recipes:` key currently `[internal-it]` (born R9 SP45b). R10 SP47 appends `api-gateway-relay` alphabetically → `[api-gateway-relay, internal-it]`. `specs/ratelimit-l0.yaml` exists on disk (4 items — `RATELIMIT-1..4`) and is bindable WITHOUT introducing a new L4 (rate-limit is currently spec-only with no L4 directory; R10 recipe binds the SPEC IDs directly via `spec_ref:`, treating rate-limiting as a cross-cutting concern enforced inside auth/webhook/crud layers).
- **Evidence rigor.** 5 logical English attempts (Kong + AWS + Cloudflare + Tyk + Apigee) + 3 logical Korean attempts (KakaoCloud + NHN Cloud + Naver Cloud API Gateway). Verbatim PASS: **5 English** (all 5 EN passed) + **1 Korean** (Toss Payments adjacent-platform fallback after 3 Korean cloud-native 404 cascade). Korean cloud-native hosts ALL host-wide 404/ECONNREFUSED — documented honest downgrade pattern.

### Principles (8 numbered)

1. **Composition kit, not single product.** api-gateway-relay COMPOSES existing 12 L4. Zero new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill / practices rule family.
2. **Spec-before-code, evidence-anchored AT PRD SIGNATURE.** All 5 `spec_ref:` disk-verified at PRD signature against existing specs (webhook + auth + audit-log + scheduled-task + ratelimit). Korean ledger captures 3 cloud-native attempts + 1 adjacent-platform success per R9 precedent.
3. **Binary verification per axis.** `/ax-verify` exits 0; `recipe_governance_guard.sh` exits 0 across 11 active recipes; `recipe_spec_referential_integrity_guard.sh` exits 0 across 11 specs; SP48 sealed verdict ≥10/12 MUST + ≥5/8 SHOULD.
4. **Tier-1 cap = 4. Tier-2 count = 8. L1 = 49, L2 = 92, L3 = 20. L4 = 12. FROZEN.** Recipes 10 → 11. Sealed verdicts 12 → 13.
5. **Atomic Spec Trio rule per SP.** SP47 ships api-gateway-relay quartet + spec + L4 README appends + sealed-verdict scaffold + manifest add + evidence snapshot atomically (R6 SP39 / R8 SP43 precedent).
6. **Recipe does not ship code; AI implements business logic.** Inherited R5/R6/R7/R8/R9.
7. **R9 TD-027 2-consumer-signal gate HONORED — no new L4 this cycle.** Shipping a new L4 + new recipe in same cycle re-triggers the gate self-fulfillingly (Architect H1 concern from R9 iter 1). R10 ships RECIPE ONLY; webhook L4 receives its 2nd consumer (api-gateway-relay) **post-hoc validating** the gate set in R9 TD-027. If R10 implementation reveals genuine primitive need (rate-limit-as-L4, api-key-as-L4), defer to R11+ as NEW deferred-recipe candidate with its own evidence chain.
8. **No new L2 / L3 / practices rule families.** Recipe binds to existing rules + existing spec items only. Rate-limiting bound via `specs/ratelimit-l0.yaml#RATELIMIT-1..4` spec_ref (no L4 directory exists for ratelimit; spec-only binding is the deliberate "cross-cutting" framing).

### Decision Drivers (top 3)

1. **R9 TD-027 forward-pointer discipline.** R9 explicitly named api-gateway-relay as the "plausible R10+ deferred candidate" satisfying condition (c.2) of the 2-consumer-signal gate. R10 acts on this forward-pointer: webhook L4 acquires its 2nd shipped consumer (api-gateway-relay) and the gate is RETROACTIVELY VALIDATED. Without R10, TD-027 would remain a single-consumer-signal precedent at R12 horizon (TD-027 Follow-ups: "If `api-gateway-relay` is never proposed by R12, the forward-pointer is replaced with whichever other R10+ deferred candidate consumes webhook").
2. **Evidence rigor.** 5 English verbatim PASS (Kong + AWS + Cloudflare + Tyk + Apigee — all 5 in-scope English attempts succeeded; strongest English chain in any single recipe this cycle alongside R8 cms) + 1 Korean verbatim PASS (Toss Payments adjacent-platform — Korean cloud-native API gateway hosts ALL 404/blocked, host-wide pattern documented as R8/R9-style honest downgrade). Clears 1-floor with 5x buffer (English).
3. **R6 cadence parity.** R6 SP39 atomic-3 was the canonical "recipe-only, no new L4" precedent. R10 mirrors at n=1: SP47 atomic-1 + SP48 FINAL. R8 SP43 atomic-2 was the closest direct precedent (no new L4, recipes-only, 2 recipes). R10 atomic-1 is the simplest variant of the same shape. No SP split needed (single recipe, no harness novelty, no L4-introduction risk).

### Viable Options Considered (≥2 mandatory)

- **Option (1) — Atomic SP47 (api-gateway-relay only) + SP48 FINAL.** (R6 SP39 / R8 SP43 atomic-precedent style)
  - Pros: Smallest delta; one recipe; no L4 surface mutation beyond `applied_recipes:` plural-list appends (R6 dual-form regex + alphabetical-append already proven across R6/R7/R8/R9); validates R9 TD-027 forward-pointer without re-opening the L4-introduction gate; partial-tag policy trivially simplifies to PASS/FAIL binary (no recipe-axis partial possible at n=1).
  - Cons: Doesn't pre-emptively introduce rate-limit-as-L4 even if fork-receivers might want it — but TD-027 is explicit that 2-consumer-signal is required for L4 split, and rate-limit currently has ZERO shipped consumers.
  - **CHOSEN.**

- **Option (2) — New L4 (api-gateway) + api-gateway-relay recipe in same SP47.** (R7 / R9 "new L4 + new recipe" style)
  - Pros: One unified gateway primitive surface; standard L4 introduction grammar applies.
  - Cons: **Re-triggers R9 Architect H1 self-fulfilling 2-consumer gate concern verbatim** — planner would propose BOTH the L4 AND its qualifying recipe in same cycle, exactly what TD-027 (c) was tightened to prevent. R9 webhook had an organic 2nd consumer signal (api-gateway-relay forward-pointer authored after webhook's own evidence chain was independently complete); R10 has no equivalent forward-pointer for a new L4. **REJECTED** per TD-027 discipline.

- **Option (3) — Spin rate-limit as its own L4 (no new recipe).** (Pre-emptive primitive split)
  - Pros: rate-limit spec exists on disk (4 items), could become a 13th L4 directory.
  - Cons: ZERO shipped consumers today; TD-027 condition (c) demands AT LEAST one shipped active recipe consuming the primitive AND one plausible R10+ deferred candidate. rate-limit-as-L4 fails BOTH halves. Recipe-less L4 also violates the catalog's "L4 ships evidence chain" rule (each L4 needs ≥3 external + ≥1 Korean per R5/R9 precedent). **REJECTED.**

- **Option (4) — Defer api-gateway-relay past R10; wait for fork-receiver demand.**
  - Pros: Lowest-risk no-op.
  - Cons: TD-027 forward-pointer becomes dead-letter at R12 horizon; deferred-queue starts re-accumulating without resolution; R9's careful 2-consumer-signal framing loses its single concrete validation. **REJECTED** — user explicitly invoked R10 with TD-027 framing.

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
| Deferred recipes | **0** (Synthesis-A queue CLOSED at R9) | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Sealed verdicts | **12** (all PASS) | `skills/_tests/sealed-verdict/` |
| Hard guards GREEN | 22 | `practices/evals/` |
| Practices rules | 68 (Tier-1/Tier-2 4/8 frozen) | `practices/rules/` |
| Current tag | `v1.7.0-webhook-internal-it` on `main@97fb625` | `git tag --sort=-creatordate \| head -1` |

### R10 scope

- **Deliverable (api-gateway-relay, SP47 atomic):** `recipes/api-gateway-relay/` quartet (RECIPE.md + L4-composition.md + L2-block-recipe.md + spec-trio-template.yaml) + `specs/recipes/api-gateway-relay-recipe-l0.yaml` (5 business_invariants, ALL spec_ref/rule_ref disk-resolvable at PRD signature). Composition: `webhook + auth + audit-log + crud + scheduled-task` (5 mandatory) + `notification + feature-flags` (2 optional). Plus cross-cutting `specs/ratelimit-l0.yaml` SPEC bindings (no L4 directory — spec-only).
- **Shared SP47 mutations:** `recipes/_MANIFEST.yaml` add (active recipes 10 → 11; `deferred_recipes: []` UNCHANGED — R10 does not re-open the queue); `templates/L4/webhook/README.md` `applied_recipes:` list append (`[internal-it]` → `[api-gateway-relay, internal-it]` alphabetical); 6 other L4 READMEs (`auth, audit-log, crud, scheduled-task, notification, feature-flags`) plural-list append `api-gateway-relay` alphabetically.

NO new L1 / L2 / L3 / L4 / Tier-1 / Tier-2 skill / practices rule. Recipe count 10 → 11. L4 count UNCHANGED (12).

---

## §3 Objectives + Guardrails

### Must Have

- 1 recipe (`recipes/api-gateway-relay/`) full Spec Trio quartet.
- 1 recipe spec (`specs/recipes/api-gateway-relay-recipe-l0.yaml`) — `l2_blocks_used:` strictly disk-resolvable (R8 Critic L blocker precedent: only files at `templates/L2/blocks/*.tsx`).
- 5 business_invariants, ALL `spec_ref:` / `rule_ref:` disk-resolvable at PRD signature.
- 1 sealed verdict (`skills/_tests/sealed-verdict/api-gateway-relay-verdict.md`) ≥10/12 MUST + ≥5/8 SHOULD.
- `recipes/_MANIFEST.yaml` append api-gateway-relay to `recipes:` (active block); `deferred_recipes: []` UNCHANGED (R6 Synthesis-A queue closed at R9; R10 does NOT re-open).
- `templates/L4/webhook/README.md` `applied_recipes:` list-append `api-gateway-relay` alphabetically (current `[internal-it]` → `[api-gateway-relay, internal-it]`).
- 6 L4 READMEs (`auth, audit-log, crud, scheduled-task, notification, feature-flags`) plural-list append `api-gateway-relay` alphabetically.
- 1 × `frontend/tests/recipes/api-gateway-relay-compose.spec.ts`.
- 1 × `practices/upstream/r10-sp47-evidence-snapshot.md` with all 8 logical attempts + redirect/alternate rows + HTTP status + 2026-05-23 timestamp.
- `/ax-verify` exit 0; both recipe guards exit 0 against 11 active recipes/specs.
- Tag `v1.8.0-api-gateway-relay` IFF SP48 sealed verdict PASS.

### Must NOT Have

- NO new L4 / L3 / L2 / L1 / Tier-1 / Tier-2 skill.
- NO new practices rule family (no `practices/rules/*.md` file added; INV-005 if catalog-novel uses R7 co-shipped-rule escape hatch — see §4.1).
- NO `RECIPE_DEVIATION.md` ceremony.
- NO `/ax-scaffold business <pattern> --analyze` free-text NLP.
- NO Korean reference fabrication. Cloud-native Korean 404 cascade is honest evidence per R8/R9 precedent.
- NO change to git workflow / CI policy / release process.
- NO partial deliverable within SP47 (single recipe — atomic ships entire quartet + spec + L4 appends + manifest + scaffold + test + evidence snapshot, or rollback).
- NO opening of `deferred_recipes:` queue (R9 closed it; if R10 implementation surfaces primitive-need, R11+ planner authors NEW candidate with its own evidence chain).
- NO TD-027 re-triggering (no new L4 this cycle — Principle 7).

---

## §4 Recipe Inventory — ALL `spec_ref:` disk-verified

### 4.1 `api-gateway-relay`

- **L4 composition (5 mandatory + 2 optional):** `webhook` (mandatory — outbound fanout to backend services), `auth` (mandatory — gateway-level access control + token introspection), `audit-log` (mandatory — every relayed request emits audit row), `crud` (mandatory — endpoint registry + route config CRUD), `scheduled-task` (mandatory — per-endpoint circuit-breaker state reconciliation + dead-letter replay scheduling). **Optional:** `notification` (alerting on circuit-breaker open / dead-letter accumulation thresholds), `feature-flags` (per-route gating, canary routing).
- **L2 blocks used (disk-verified via `ls templates/L2/blocks/*.tsx` on 2026-05-23 — guard-resolvable only):** `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `confirm-dialog`, `bulk-actions-bar`.
- **L1 primitives consumed (documented in `recipes/api-gateway-relay/L2-block-recipe.md` "L1 primitives consumed" subsection — NOT in `l2_blocks_used:` per R8 Critic L blocker contract):** `code-block` (request/response payload preview), `relative-time` (last-request displays), `badge` (route status indicators: ACTIVE / PAUSED / CIRCUIT-OPEN / DEAD-LETTER).
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **Business invariants (all 5 disk-RESOLVED at PRD signature):**

  - `API-GATEWAY-RELAY-INV-001`: Every relayed request to a backend service is signed via webhook L4's HMAC-SHA256 over `<timestamp>.<body>` AND emits an audit-log row with operator + relay_target_endpoint + delivery_id. `spec_ref: specs/webhook-l0.yaml#WEBHOOK-SIGN-001` (DISK-VERIFIED — `specs/webhook-l0.yaml:38`) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (DISK-VERIFIED — `specs/audit-log-l0.yaml:7`). **PASS.**

  - `API-GATEWAY-RELAY-INV-002`: Relay authorization checks token validity AND scope BEFORE forwarding to upstream; on auth failure the request is rejected with audit-logged denial reason. `spec_ref: specs/auth-asvs-l1.yaml#ASVS-V4.1.1` (DISK-VERIFIED — `specs/auth-asvs-l1.yaml:139`) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (DISK-VERIFIED — `specs/audit-log-l0.yaml:23`). **PASS.**

  - `API-GATEWAY-RELAY-INV-003`: Per-route rate-limiting enforced before relay; over-limit requests rejected with HTTP 429 AND counted via observability counter. `spec_ref: specs/ratelimit-l0.yaml#RATELIMIT-1` (DISK-VERIFIED — `specs/ratelimit-l0.yaml:6`) + `spec_ref: specs/ratelimit-l0.yaml#RATELIMIT-2` (DISK-VERIFIED — `specs/ratelimit-l0.yaml:15`). **PASS.**
    - **Cross-cutting spec binding rationale (R10-novel framing):** `ratelimit-l0.yaml` exists on disk as a 4-item spec WITHOUT a `templates/L4/ratelimit/` directory. R10 treats rate-limiting as a CROSS-CUTTING CONCERN enforced inside existing L4 boundaries (auth + webhook + crud filters), bound via spec_ref ONLY. This is deliberately DIFFERENT from spinning a new L4 — see Principle 7 + TD-027 (c) two-consumer-signal gate. `ratelimit-l0.yaml` becomes a candidate for future L4 promotion in R11+ if a 2nd recipe consumer arrives organically.

  - `API-GATEWAY-RELAY-INV-004`: Circuit breaker per upstream endpoint auto-opens on 90% failure rate over rolling 50 attempts; reconciliation of half-open / closed state runs via scheduled-task lock primitive (no double-reconcile on multi-node deployment). `spec_ref: specs/webhook-l0.yaml#WEBHOOK-CIRCUIT-001` (DISK-VERIFIED — `specs/webhook-l0.yaml:128`) + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` (DISK-VERIFIED — `specs/scheduled-task-l0.yaml:21`). **PASS.**

  - `API-GATEWAY-RELAY-INV-005`: Route registration + config mutations (URL, method, target, signing-secret) follow CRUD validation rules AND emit audit rows with before/after diff. `spec_ref: specs/crud-security.yaml#CRUD-VAL-1` (DISK-VERIFIED — `specs/crud-security.yaml:22`) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (DISK-VERIFIED — `specs/audit-log-l0.yaml:23`) + `rule_ref: practices/rules/idempotency-key-on-mutations.md` (DISK-VERIFIED — file exists in `practices/rules/`). **PASS.**

- **INV disambiguation (R8/R9 precedent):** All 5 INVs bind to EXISTING spec items + EXISTING practices rules. No `co-shipped-rule` invocation needed (contrast R7 community-INV-005 and R9 internal-it-INV-005 which used co-shipped-rule for catalog-novel concerns — server-side XSS sanitization and per-endpoint signing-secret encryption respectively). R10 api-gateway-relay reuses ALL anchors authored across R5-R9 — the strongest expression of composition-kit reuse this cycle. If implementation later reveals a genuine catalog-novel concern (e.g. cross-route request-correlation-id propagation), R11+ planner authors as standalone recipe-level invariant with co-shipped-rule pattern.

- **Override allowance:** inline — skip `notification` for headless gateway deployments; skip `feature-flags` for monolithic single-route relays; skip `scheduled-task` for stateless circuit-breaker (in-memory only — fork-receiver accepts state loss on restart).

- **External evidence (verbatim — 5 English PASS, exceeds 1-floor with 5x buffer; strongest English chain alongside R8 cms):** Kong Gateway + AWS API Gateway + Cloudflare API Shield + Tyk + Apigee. See §4.4 ledger.

- **Korean evidence (verbatim — 1 PASS via adjacent-platform fallback after cloud-native cascade):** Toss Payments API (canonical Korean enterprise REST API platform — adjacent gateway-style integration semantics). See §4.4 cascade documentation.

- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/api-gateway-relay-compose.spec.ts"
    assertion: "recipes/api-gateway-relay/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, auth, crud, scheduled-task, webhook] (alphabetical mandatory; notification + feature-flags documented as override_allowed) AND 5 disk-verified spec_ref/rule_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh AND l2_blocks_used: contains only files present at templates/L2/blocks/*.tsx"
    expected_RED_reason: "recipes/api-gateway-relay/ directory does not exist"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/api-gateway-relay-compose.spec.ts"
    owning_SP: "SP47"
  ```

### 4.2 Cluster claim (R6 SP39 / R8 SP43 precedent — n=1 variant)

api-gateway-relay is a SINGLE-RECIPE cycle — no cross-recipe cluster. Mutation surface is purely append-only across 6 L4 READMEs (alphabetical plural-list append; R6 dual-form regex handles). webhook L4 README transitions `applied_recipes: [internal-it]` → `[api-gateway-relay, internal-it]` (alphabetical insertion, NOT key-birth — R8 TD-024 first-consumer-arrival convention does not apply since webhook is now a 2-consumer L4).

### 4.3 Webhook 2nd-consumer signal — R9 TD-027 retroactive validation (R10-specific)

R9 TD-027 condition (c) required TWO consumer signals for L4 split: (c.1) shipped active recipe (internal-it) + (c.2) plausible R10+ deferred candidate (api-gateway-relay forward-pointer). R10 ships api-gateway-relay as a SHIPPED active recipe — webhook L4 now has 2 SHIPPED consumers. The 2-consumer-signal gate is RETROACTIVELY VALIDATED for the webhook precedent (the "plausible R10+ deferred" half upgrades to "shipped active"). R10 does NOT create a new TD-027 instance — it CLOSES the open one.

### 4.4 Evidence ledger (WebFetched during PRD revision — 2026-05-23)

> **Counter wording (R8 soft Critic precedent):** 8 **logical** WebFetch attempts (1 attempt = 1 host targeted with intent to verbatim-cite). 3 additional rows are bookkeeping for redirect captures + adjacent-platform fallback. Total **8 logical attempts + 3 redirect/alternate rows.**

| Source class | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| EN Kong | `https://docs.konghq.com/gateway/` | 301 redirect → `https://developer.konghq.com/gateway/` (2026-05-23) | — | Followed redirect | (redirect captured) |
| EN Kong | `https://developer.konghq.com/gateway/` | **200 OK — verbatim** (2026-05-23) | `"Kong Gateway is a lightweight, fast, and flexible cloud-native API gateway."` | Verbatim cite | `external` |
| EN AWS | `https://docs.aws.amazon.com/apigateway/` | **200 OK — verbatim** (2026-05-23) | `"Amazon API Gateway enables you to create and deploy your own REST and WebSocket APIs at any scale."` | Verbatim cite | `external` |
| EN Cloudflare | `https://developers.cloudflare.com/api-gateway/` | **HTTP 404** (2026-05-23) | — | (alternate fetched) | — |
| EN Cloudflare | `https://developers.cloudflare.com/api-shield/` | **200 OK — verbatim** (2026-05-23) | `"Identify and address your API vulnerabilities."` | Verbatim cite (alternate host path — API Shield is Cloudflare's API Gateway product) | `external` |
| EN Tyk | `https://tyk.io/docs/` | **200 OK — verbatim** (2026-05-23) | `"The hub for Tyk API management. Whether you're new or experienced, get started with Tyk, explore our product stack and core concepts, access in-depth guides, and actively contribute to our ever-evolving products."` | Verbatim cite | `external` |
| EN Apigee | `https://cloud.google.com/apigee/docs` | 301 redirect → `https://docs.cloud.google.com/apigee/docs` (2026-05-23) | — | Followed redirect | (redirect captured) |
| EN Apigee | `https://docs.cloud.google.com/apigee/docs` | **200 OK — verbatim** (2026-05-23) | `"With Apigee, you can build API proxies—RESTful, HTTP-based APIs that interact with your services."` | Verbatim cite | `external` |
| KO KakaoCloud | `https://docs.kakaocloud.com/service/cloud-edge/apigateway` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` — page does not exist on host |
| KO KakaoCloud (alt) | `https://docs.kakaocloud.com/` | **200 OK — no API Gateway descriptive content** (2026-05-23) | — | Downgrade | `internal_design` — homepage shell only; no API Gateway verbatim |
| KO KakaoCloud (alt 2) | `https://kakaoi.kakaocloud.com/service/cloudEdge/apiGateway` | **ECONNREFUSED** (2026-05-23) | — | Downgrade | `internal_design` — host unreachable |
| KO NHN Cloud | `https://docs.nhncloud.com/ko/Application%20Service/API%20Gateway` | 301 → `http://docs.nhncloud.com/ko/Application%20Service/API%20Gateway/` (2026-05-23) | — | Followed redirect | (redirect captured) |
| KO NHN Cloud | `http://docs.nhncloud.com/ko/Application%20Service/API%20Gateway/` | 302 → error page `http://www.nhncloud.com/error/CAoNCgYFA` (2026-05-23) | — | (alternate fetched) | — |
| KO NHN Cloud (alt) | `https://meetup.nhncloud.com/posts/250` | **HTTP 404** — `"404 존재하지 않는 페이지 입니다."` (2026-05-23) | — | Downgrade | `internal_design` — NHN Cloud meetup post 404 |
| KO Naver Cloud | `https://api.ncloud-docs.com/docs/apigateway-overview` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` — page does not exist |
| KO Naver Cloud (alt) | `https://www.ncloud.com/product/applicationService/apiGateway` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` — product page URL invalid |
| KO Naver Cloud (alt 2) | `https://guide.ncloud-docs.com/docs/apigw-overview` | **HTTP 404** (2026-05-23) | — | Downgrade | `internal_design` — guide page URL invalid |
| KO adjacent fallback | `https://docs.tosspayments.com/reference` | **200 OK — verbatim Korean** (2026-05-23) | `"토스페이먼츠 API 엔드포인트(Endpoint)와 객체 정보, 파라미터, 요청 및 응답 예제를 살펴보세요."` | Verbatim cite (Korean enterprise REST API platform with gateway-style integration semantics — R9 Toss precedent) | `external` |

**Per-recipe evidence density floor:**

- **api-gateway-relay:** 5 English verbatim (Kong + AWS + Cloudflare API Shield + Tyk + Apigee — all 5 in-scope English attempts succeeded; STRONGEST English chain in any single recipe alongside R8 cms's 4 English) + 1 Korean verbatim adjacent-platform (Toss Payments after 3-host Korean cloud-native cascade: KakaoCloud × 3 attempts all 404/ECONNREFUSED + NHN Cloud × 3 attempts all 404/redirect-to-error + Naver Cloud × 3 attempts all 404). PASS — clears 1-floor with 5x buffer (English) + 1x (Korean adjacent).

- **Korean cycle rationale (R8/R9 precedent applied):** Korean cloud-native API gateway hosts (KakaoCloud, NHN Cloud, Naver Cloud) ALL exhibit host-wide URL invalidity on 2026-05-23 (9 host attempts across 3 platforms: all return 404, 301/302-to-error, or ECONNREFUSED). This is the **same host-wide pattern** R9 documented for ServiceNow (4 attempts) + Kakao 알림톡 (6 attempts) — honest evidence that public API-gateway documentation for these three Korean cloud platforms is not currently verbatim-fetchable. R10 falls back to **Toss Payments** (R9-already-cited Korean enterprise platform) as the adjacent verbatim anchor — Toss is canonical Korean enterprise REST API platform offering gateway-style integration semantics. Project vision (CLAUDE.md) framing of "Korean enterprise standard stack" is honored at the **stack level** (React + Spring Boot) + **per-cycle freshness signal** (≥1 Korean verbatim PASS this cycle).

- **Per-source-class breakdown (R9 Codex Critic soft #1 precedent):** Verbatim PASS = **6** (5 EN + 1 KO adjacent). Downgrades = **8** (Cloudflare /api-gateway 404 + KakaoCloud × 3 + NHN Cloud × 3 + Naver Cloud × 3 — total 10 raw host attempts minus 2 redirects-to-error captured as separate redirect rows = 8 downgrades). Redirects/alternates = **3** (Kong 301 → developer.konghq.com + Apigee 301 → docs.cloud.google.com + NHN 301-302 cascade-to-error). 8 logical attempts + 3 redirect/alternate rows = 11 raw table rows excluding Toss adjacent row + Cloudflare 404 captured-as-alternate row.

**Re-attempt at SP execution:** SP47 pre-flight re-runs one Cloudflare canonical URL (`/api-shield/` already PASS; no re-attempt needed) + one host probe per Korean cloud platform (KakaoCloud root + NHN Cloud root + Naver Cloud root) — single-shot, no fabrication. Any newly successful fetch upgrades to `external` in the evidence snapshot file.

---

## §4.5 SP Plan + Verification Matrix (2 SPs)

| SP | Atomic deliverables | TDD anchors | Verification | Observability (advisory) |
|---|---|---|---|---|
| **SP47** (atomic — api-gateway-relay) | (a) `recipes/api-gateway-relay/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; (b) `specs/recipes/api-gateway-relay-recipe-l0.yaml` (5 INVs disk-resolvable per §4.1; `l2_blocks_used:` strictly guard-resolvable); (c) `recipes/_MANIFEST.yaml` append api-gateway-relay to `recipes:` active block (`deferred_recipes: []` UNCHANGED); (d) `templates/L4/webhook/README.md` `applied_recipes:` plural-list append → `[api-gateway-relay, internal-it]` alphabetical; (e) 6 L4 READMEs (`auth, audit-log, crud, scheduled-task, notification, feature-flags`) plural-list append `api-gateway-relay` alphabetically; (f) `skills/_tests/sealed-verdict/api-gateway-relay-verdict.md` scaffold PENDING; (g) `frontend/tests/recipes/api-gateway-relay-compose.spec.ts`; (h) `practices/upstream/r10-sp47-evidence-snapshot.md`. | compose-test RED → GREEN; `recipe_governance_guard.sh` RED → GREEN; `recipe_spec_referential_integrity_guard.sh` RED → GREEN; webhook L4 alphabetical-insert mutation RED → GREEN. | `bash practices/evals/recipe_governance_guard.sh` exit 0 across 11 active recipes; `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exit 0 across 11 specs; `/ax-verify-domain` × 7 touched L4 (`webhook, auth, audit-log, crud, scheduled-task, notification, feature-flags`); webhook README `grep -E "^applied_recipes:" \| grep -c "api-gateway-relay"` exit 0. | `recipe.api_gateway_relay.relay_request_total`, `recipe.api_gateway_relay.relay_rate_limit_rejected_total`, `recipe.api_gateway_relay.circuit_breaker_open_total`. |
| **SP48** (FINAL — verdict exec + tag + PR) | Sealed sub-agent exec × 1 (context-0 input); `recipes/README.md` updated (11 active recipes listed); `/ax-verify` exit 0; tag policy applied. | Sealed verdict harness × 1 PENDING → PASS. | `/ax-verify` exit 0; tag policy enforced (see §6 table — binary PASS/FAIL at n=1). | `recipes.active_total: 11`; `L4.domain_total: 12` (UNCHANGED). |

**SP atomicity:** SP47 ships full quartet + spec + manifest add + 7 L4 README appends + verdict scaffold + compose-test + evidence snapshot together OR rollback.
**SP linearization:** SP47 → SP48. No parallel branches.

---

## §5 Webhook L4 `applied_recipes:` alphabetical-insert mutation (R10-specific)

**Resolution:** R9 SP45b birthed webhook README `applied_recipes: [internal-it]` per TD-024 first-consumer-arrival. R10 SP47 PERFORMS THE FIRST 2-element insertion on this key — alphabetical insertion of `api-gateway-relay` BEFORE `internal-it` → `[api-gateway-relay, internal-it]`. R6 dual-form regex already accepts this shape (proven across R6/R7/R8/R9 fixtures `pass_applied_recipes_plural` + `fail_applied_recipes_empty_list`).

### Disk evidence (2026-05-23)

- `templates/L4/webhook/README.md` `applied_recipes:` key currently `[internal-it]` (born R9 SP45b commit on `main@97fb625`).
- `practices/evals/recipe_governance_guard.sh` dual-form alternation regex (R6 SP39) accepts both singular and plural list forms; `pass_applied_recipes_plural/RECIPE.md` fixture proves ≥2-element list validity.

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

- **Pre-flight gate (before SP47 starts):** §4.4 evidence captured. SP47 pre-flight re-runs one host probe per Korean cloud platform (KakaoCloud root + NHN Cloud root + Naver Cloud root) — single-shot; document HTTP status + timestamp in evidence snapshot. Disk-verify `recipes/api-gateway-relay/` ABSENCE; disk-verify `specs/recipes/api-gateway-relay-recipe-l0.yaml` ABSENCE; disk-verify webhook L4 README current `applied_recipes:` shape (`[internal-it]`); disk-verify 5 INV spec_ref/rule_ref anchors per §4.1 still resolve. Disk-verify L2 inventory by `ls templates/L2/blocks/*.tsx` against §4.1 list. Abort if any prep step fails.
- **Mid-flight gate (between SP47 and SP48):** `git status` clean; `/ax-verify-domain` × 7 touched L4 exit 0; both recipe guards exit 0 across 11 active recipes; `frontend/tests/recipes/api-gateway-relay-compose.spec.ts` passes; commit message references SP47 + 5 INV IDs.
- **Stop conditions:** If `recipe_spec_referential_integrity_guard.sh` cannot reach GREEN within 3 iter cycles, SP47 rolls back; recipe authored as NEW deferred-recipes entry with `blocker:` field (re-opens the queue — explicit ADR required at that point for the queue re-opening decision).
- **Sealed verdict release policy (n=1 binary):** Tag `v1.8.0-api-gateway-relay` IFF SP48 verdict ≥10/12 MUST + ≥5/8 SHOULD. No tag IFF FAIL (SP47 reverted; api-gateway-relay returns to NEW deferred-recipes entry per §10).
- **Rollback:** Each SP is one squash-mergeable commit. Revert SP47 cleanly without disturbing R9 state.
- **No destructive ops:** No `git reset --hard`, no force push. webhook README append-only mutation (alphabetical insert; existing `internal-it` line unchanged).

### Partial-tag policy (R8 small table precedent — degenerate at n=1)

Single recipe; no recipe-axis partial possible.

| SP48 verdict outcome | Tag | `_MANIFEST.yaml` membership | `recipes/api-gateway-relay/RECIPE.md` `status:` | Webhook L4 README `applied_recipes:` |
|---|---|---|---|---|
| 1/1 PASS | `v1.8.0-api-gateway-relay` | api-gateway-relay `active` (11 active, 0 deferred) | `active` | `[api-gateway-relay, internal-it]` (unchanged from SP47 commit) |
| 0/1 FAIL | no tag | SP47 reverted; api-gateway-relay returned to NEW `deferred_recipes:` entry with `blocker: sealed-verdict <10/12 MUST or <5/8 SHOULD` field | n/a (directory removed) | `[internal-it]` (SP47 webhook append reverted) |

**Rationale:** At n=1, partial-tag policy collapses to binary. No inline desync annotation needed (R9 M6 fix applied only because partial-tag webhook README would otherwise diverge from `_MANIFEST.yaml` — at n=1 the two states co-mutate atomically).

---

## §7 Pre-Mortem (4 scenarios — SHORT mode floor)

1. **Korean cloud-native API gateway hosts remain host-wide 404/blocked at SP47 execution.** Likelihood: HIGH (already observed across 9 host attempts in §4.4 spanning 3 platforms). Impact: api-gateway-relay's Korean verbatim count stays at 1 (Toss adjacent fallback) — clears 1-floor with no buffer. Mitigation: §4.4 documents host-wide pattern explicitly across 9 attempts; SP47 pre-flight one-shot per platform root URL does NOT block. Toss adjacent fallback is honest precedent established R9 internal-it + carried forward verbatim. If SP48 sealed verdict flags Korean-evidence-gap, the verdict harness is instructed to weight English chain comfort (5 verbatim) + R8/R9 host-wide-pattern precedent.

2. **`specs/ratelimit-l0.yaml` spec_ref binding misinterpreted as new L4 introduction.** Likelihood: LOW. Impact: TD-027 2-consumer-signal gate falsely re-triggered. Mitigation: §4.1 INV-003 paragraph explicitly frames rate-limiting as CROSS-CUTTING CONCERN (enforced inside auth + webhook + crud filters) bound by spec_ref only — NO `templates/L4/ratelimit/` directory created in SP47. §8 TD-028 ADR documents the cross-cutting framing. If ratelimit L4 promotion arises organically in R11+ (2nd consumer recipe), the existing spec file becomes the L4's Spec Trio anchor — no rework needed.

3. **Sealed verdict for api-gateway-relay scores below threshold because context-0 sub-agent cannot disambiguate api-gateway-relay from webhook L4 itself.** Likelihood: LOW. Impact: SP48 no-tag. Mitigation: `recipes/api-gateway-relay/RECIPE.md` explicitly distinguishes from `templates/L4/webhook/` (recipe is GATEWAY-PATTERN COMPOSER; webhook L4 is OUTBOUND-EMIT-PRIMITIVE). Sub-agent prompt includes both file paths as sealed context. Recipe quartet documents the 5 mandatory L4 + 2 optional + cross-cutting rate-limit spec — composition surface is fully explicit.

4. **`applied_recipes:` alphabetical insertion on webhook L4 README mistakenly REPLACES `[internal-it]` instead of INSERTING `api-gateway-relay` above it.** Likelihood: LOW. Impact: R9 internal-it loses its webhook membership declaration → R9 sealed verdict regression. Mitigation: SP47 mutation is APPEND-ONLY semantic (R6 SP39 / R8 SP43 / R9 SP45b proven pattern); test `frontend/tests/recipes/api-gateway-relay-compose.spec.ts` asserts BOTH `api-gateway-relay` AND `internal-it` appear in the final list; `recipe_governance_guard.sh` exit 0 against ALL 11 active recipes (including internal-it) ensures regression detection.

---

## §8 ADR Template (2 entries — TD-028, TD-029)

- **TD-2026-05-23-028 (NEW)** — Recipe `api-gateway-relay` shipped via composition of existing 12 L4 (5 mandatory + 2 optional + 1 cross-cutting spec binding).
  - **Decision:** `recipes/api-gateway-relay/` deferred→active. Active 10 → 11. Composition: `webhook + auth + audit-log + crud + scheduled-task` (mandatory) + `notification + feature-flags` (optional) + `specs/ratelimit-l0.yaml` (cross-cutting spec binding, NO new L4).
  - **Drivers:** R9 TD-027 named api-gateway-relay as the "plausible R10+ deferred candidate"; R10 acts on that forward-pointer; 5 English verbatim PASS (Kong + AWS + Cloudflare + Tyk + Apigee) — STRONGEST English chain alongside R8 cms; 1 Korean adjacent verbatim PASS (Toss Payments after 9-host cloud-native cascade); 5 INVs all disk-resolvable at PRD signature without `co-shipped-rule` invocation (strongest catalog-reuse expression this cycle).
  - **Alternatives considered:** Option 2 new L4 (api-gateway) + recipe in same SP (rejected — re-triggers R9 TD-027 self-fulfilling gate); Option 3 spin ratelimit-as-L4 (rejected — zero shipped consumers, fails TD-027 (c) both halves); Option 4 defer past R10 (rejected — user-invoked + dead-letters TD-027 forward-pointer).
  - **Why chosen:** Composition-kit reuse at maximum expression; validates R9 TD-027 retroactively; honors recipe-only / no-new-L4 discipline per Principle 7.
  - **Consequences:** 7 L4 READMEs gain `api-gateway-relay`. webhook README `applied_recipes:` shape `[internal-it]` → `[api-gateway-relay, internal-it]` (first 2-element list on webhook L4). R10 closes TD-027 retroactive-validation loop; `api-gateway-relay` becomes a SHIPPED 2nd consumer of webhook L4 (gate condition c.1 + c.2 both fulfilled by SHIPPED recipes post-R10).
  - **Follow-ups:** R11+ refresh re-attempts Korean cloud-native API gateway docs if any platform unblocks; rate-limit L4 promotion if 2nd recipe consumer arrives organically; api-gateway-relay sealed verdict re-execution annually per R5/R6/R7/R8/R9 catalog-quality cadence.

- **TD-2026-05-23-029 (NEW)** — TD-027 2-consumer-signal gate HONORED in R10 by deliberate no-new-L4 decision.
  - **Decision:** R10 ships RECIPE ONLY (api-gateway-relay). NO new L4 introduced. Validates R9 TD-027 (c) by SUPPLYING the 2nd consumer signal for webhook (internal-it shipped R9 + api-gateway-relay shipped R10), rather than spawning a new L4 + new recipe pair in same cycle (which would self-fulfill the gate per Architect H1).
  - **Drivers:** R9 Architect H1 explicitly tightened TD-027 (c) to TWO consumer signals so the test is not self-fulfilling by writing one recipe alongside the L4 proposal. R10 is the FIRST opportunity to TEST this tightening — and the tightening only holds if R10 demonstrates discipline by NOT spawning a new L4. If R10 had spawned api-gateway L4 + api-gateway-relay recipe together, the test would be exactly the self-fulfilling pattern Architect feared.
  - **Alternatives considered:** Option 2 new L4 + new recipe same SP (rejected — re-triggers H1 verbatim); Option 3 ratelimit as standalone L4 (rejected — fails both halves of (c) — zero shipped consumers); pre-emptive primitive split based on speculative future demand (rejected — TD-027 explicitly disallows speculative L4 introduction).
  - **Why chosen:** First operational test of TD-027 (c) tightening; R10 is the canonical "discipline holds" demonstration that R9 Architect explicitly hoped for in the H1 closure rationale.
  - **Consequences:** R10 ships at L4 = 12 (UNCHANGED). Future R11+ L4-introduction proposals MUST satisfy ALL THREE TD-027 conditions independently (not by self-fulfillment). If R10 implementation surfaces genuine primitive need (e.g. rate-limit demands its own L4 for cross-recipe lifecycle), R11+ planner authors with explicit 2-consumer-signal documentation citing this ADR.
  - **Follow-ups:** First R11+ application of TD-027 (next time someone proposes an L4 split) generates a precedent log entry under TD-027 + this ADR — captures whether all three conditions held without self-fulfillment.

---

## §9 Honored Constraints

- **Caps:** Tier-1 = 4 FROZEN · Tier-2 = 8 UNCHANGED · L1/L2/L3 = 49/92/20 UNCHANGED.
- **Deltas:** L4 = 12 (UNCHANGED — no new L4 this cycle per Principle 7 / TD-029) · Recipes 10 → 11 (api-gateway-relay per TD-028) · Deferred 0 → 0 (queue NOT re-opened) · Sealed verdicts 12 → 13.
- **Atomic SP rule per axis** (R6 SP39 / R8 SP43 precedent — SP47 atomic-1 + SP48 FINAL).
- **TD-027 2-consumer-signal gate HONORED** (R10 supplies 2nd consumer for webhook; no new L4 introduced; TD-029 documents discipline).
- **Korean references** — 3 Korean cloud-native logical hosts attempted (KakaoCloud + NHN Cloud + Naver Cloud × 3 host attempts each = 9 raw probes) + 1 adjacent-platform fallback (Toss Payments — R9 precedent). 1 Korean verbatim PASS (Toss adjacent). R7 5-host floor MET (9 host attempts ≥ 5); R8/R9 1-Korean-PASS target MET.
- **Cloud-native Korean cluster downgrade** — host-wide 404/blocked pattern across 9 attempts spanning 3 platforms; documented honest evidence (R8 ServiceNow + R9 Kakao 알림톡 precedent).
- **CRUD spec-path** — `specs/crud-security.yaml`, NOT `crud-l0.yaml` (R9 Codex Critic soft #2 precedent).
- **DECISIONS.md format** — R7+ ADR bullet format honored (TD-028 + TD-029 append-only; opening-note convention from R9 SP45 preserved).
- **No empty applied-recipes array** (R7 H2/M4); webhook README transitions to 2-element list, not empty.
- **Sealed verdict threshold** — ≥10/12 MUST + ≥5/8 SHOULD (R5-R9 precedent).
- **INV anchor preference** — all 5 INVs bind existing rules + spec items; NO `co-shipped-rule` invocation (R8 lms/cms preferred-path precedent honored; R7 community / R9 internal-it escape-hatch preserved for catalog-novel concerns).
- **R6 dual-form regex + alphabetical-append** — webhook README `[internal-it]` → `[api-gateway-relay, internal-it]` alphabetical insertion.
- **`deferred_recipes:` queue stays CLOSED** — R9 closure preserved; R10 does NOT re-open (recipe-only voluntary cycle, NOT deferred-queue trim continuation).

---

## §10 Out-of-scope (R10 explicit) + Deferred Recipes (stays EMPTY post-R10)

### Deferred recipes — QUEUE STAYS CLOSED

`recipes/_MANIFEST.yaml#deferred_recipes:` stays `[]` post-SP47. R10 is a VOLUNTARY recipe addition (TD-027 forward-pointer validation), NOT a deferred-queue trim continuation. R10 closes TD-027 retroactively without re-opening the queue.

| Future R11+ deferred candidate (illustrative — NO entries today) | Trigger to re-open queue |
|---|---|
| `rate-limit-as-L4` (if R10 implementation surfaces a 2nd recipe consumer organically) | Fork-receiver demand + 2nd shipped active recipe consuming rate-limit primitive + ≥3 English verbatim |
| `api-key-as-L4` | Fork-receiver demand + 2nd shipped active recipe + ≥3 English verbatim |
| Other gateway-adjacent primitives surfaced by R10 implementation | TD-027 condition (c) 2-consumer-signal gate satisfied (NO self-fulfillment) |

### Out-of-scope (R10)

- New L1/L2/L3/L4 surface · new Tier-1/Tier-2 skill · new practices rule families.
- New L4 introduction (deliberate — TD-029).
- Frontend code (recipe doc only — R5/R6/R7/R8/R9 recipe-no-code) · backend impls (no skeleton stubs this cycle — recipe binds to existing webhook + auth + audit-log + crud + scheduled-task backends).
- Deployment / CI / release scope · `RECIPE_DEVIATION.md` ceremony · 6-month recipe-retirement review.
- Korean cloud-native API gateway verbatim retry beyond SP47 pre-flight one-shot per platform (host-wide pattern across 9 attempts; further attempts deferred to R11+).
- New recipe candidates for R11+ deferred queue (fork-receiver demand drives; no proactive deferral pipeline opens).
- Rate-limit L4 promotion (deferred to R11+; requires 2-consumer-signal gate).

---

## §11 Branch + path summary

- **Branch:** `feat/r10-api-gateway-relay-sp47-sp48` (cut from `main@97fb625`).
- **PRD path (this draft):** `docs/superpowers/specs/2026-05-23-r10-api-gateway-relay-prd.draft.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — api-gateway-relay appended to `recipes:` active block; `deferred_recipes: []` UNCHANGED.
- **DECISIONS.md target:** `practices/DECISIONS.md` — append TD-2026-05-23-028 (api-gateway-relay recipe) + TD-2026-05-23-029 (TD-027 no-L4-split discipline) per R9 SP45 opening-note convention (R7+ ADRs as bullets).
- **Evidence snapshot path:** `practices/upstream/r10-sp47-evidence-snapshot.md` — 8 logical attempts + 3 redirect/alternate rows + per-source-class arithmetic.
- **Final tag:** `v1.8.0-api-gateway-relay` IFF SP48 sealed verdict PASS. No tag IFF FAIL (SP47 reverted).

---

## §12 Verdict line

R10 iter 1 PRD ships R6-shape "recipe-only, no new L4" with: **api-gateway-relay recipe** COMPOSING existing 12 L4 (5 mandatory + 2 optional + 1 cross-cutting `ratelimit-l0.yaml` spec binding); **5 English verbatim PASS** (Kong + AWS + Cloudflare API Shield + Tyk + Apigee — strongest English chain alongside R8 cms); **1 Korean verbatim PASS** (Toss Payments adjacent fallback after 9-host KakaoCloud + NHN Cloud + Naver Cloud cascade — all host-wide 404/blocked, R8 ServiceNow + R9 Kakao 알림톡 precedent honored); **R9 TD-027 2-consumer-signal gate HONORED via deliberate no-new-L4 decision** (TD-029 documents discipline; R10 supplies webhook's 2nd shipped consumer post-hoc validating R9 (c.2) forward-pointer); **5 INVs all disk-resolvable WITHOUT `co-shipped-rule` invocation** (strongest catalog-reuse expression this cycle); **2 SPs** (SP47 atomic-1 + SP48 FINAL — R6 SP39 / R8 SP43 precedent); **partial-tag policy degenerate at n=1** (binary PASS/FAIL — no inline desync annotation needed); **2 new ADRs** (TD-028 api-gateway-relay recipe + TD-029 TD-027 no-L4-split discipline). 2 SPs, ≈ 2-3 d wall-time. Ready for Architect + Codex Critic iter 1 review.

---

## RALPLAN-DR Summary

**Mode:** SHORT (recipe-only; no L4 introduction; no harness novelty; n=1 partial-tag binary).

**Principles (8):** composition-kit · spec-before-code · binary-verification · Tier-1/2-frozen · atomic-Spec-Trio · recipe-no-code · TD-027-honored-no-new-L4 · no-new-L2/L3/rule-family.

**Decision Drivers (top 3):** R9 TD-027 forward-pointer discipline · evidence rigor (5 EN + 1 KO adjacent) · R6 cadence parity.

**Viable Options:** (1) atomic SP47 + SP48 CHOSEN · (2) new L4 + recipe same SP REJECTED (self-fulfills TD-027) · (3) ratelimit-as-L4 REJECTED (zero consumers) · (4) defer past R10 REJECTED (user-invoked + dead-letters forward-pointer).

**Recommended path:** SP47 atomic (recipe quartet + spec + 7 L4 README appends + manifest add + verdict scaffold + compose-test + evidence snapshot) → SP48 FINAL (verdict exec + tag IFF PASS).

**Wall-time:** 2-3 days.

**Pre-mortem:** 4 scenarios (HIGH Korean cascade · LOW ratelimit-misframing · LOW verdict-disambiguation · LOW alphabetical-insert regression).

**Test plan:** unit (recipe-spec YAML structure) · integration (both recipe guards × 11) · E2E (compose-spec × 1 + `/ax-verify-domain` × 7) · observability (3 advisory counters).

**ADRs:** TD-028 (api-gateway-relay recipe) · TD-029 (TD-027 no-L4-split discipline).

**Tag policy:** v1.8.0-api-gateway-relay IFF SP48 PASS; no tag IFF FAIL (n=1 binary).

**Evidence ledger summary:** **5 English verbatim** (Kong + AWS + Cloudflare API Shield + Tyk + Apigee) + **1 Korean verbatim** (Toss Payments adjacent fallback) + **8 documented downgrades** (Cloudflare /api-gateway 404 + KakaoCloud × 3 + NHN Cloud × 3 + Naver Cloud × 3) + **3 redirect/alternate rows** (Kong 301 → developer.konghq.com + Apigee 301 → docs.cloud.google.com + NHN 301-302 cascade-to-error). Verbatim PASS = 6 (5 EN + 1 KO). 8 logical attempts + 3 redirect/alternate = 11 raw table rows total.

**Catalog deltas:** L4 = 12 UNCHANGED (TD-029 discipline) · Recipes 10 → 11 (TD-028) · Deferred 0 → 0 (queue stays closed) · Sealed verdicts 12 → 13; Tier-1/2 4/8 + practices 68 + L1/L2/L3 49/92/20 UNCHANGED.
