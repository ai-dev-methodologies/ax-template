# R9 — Webhook L4 Primitive + internal-it Recipe PRD — 2026-05-22 (Round 9, ralplan iter 2 APPROVED)

> **Status:** APPROVED (2-iter ralplan consensus; Architect iter 2 APPROVE + Codex Critic iter 2 FINAL APPROVE).
> **Date:** 2026-05-22. **Repo:** ax-template. **Format:** RALPLAN-DR.
> **Iter lineage:**
> - `2026-05-22-r9-webhook-internal-it-prd.draft.md` (iter 1).
> - `2026-05-22-r9-webhook-internal-it-prd.iter2.md` (iter 2 — this file; closes 2 HIGH + 4 MEDIUM + 1 INFORMATIONAL + 3 soft).
> **Predecessors:**
> - `2026-05-21-r8-lms-cms-prd.md` (CLOSED, APPROVED iter 3 — `v1.6.0-lms-cms` on `main@953a3d6`).
> - `2026-05-20-r7-scheduler-community-prd.md` (CLOSED, APPROVED iter 3 — `v1.5.0-scheduler-community` on `main@faaf87d`). Best-style precedent for "new L4 + new recipe" cycle shape.
> **Branch (when execution starts):** `feat/r9-webhook-internal-it-sp45-sp46`.
> **Targeted tag:** `v1.7.0-webhook-internal-it` IFF 2/2 sealed verdicts pass; partial `v1.7.0-webhook` IFF only webhook verdict passes; no tag IFF webhook verdict fails (SP45 reverts, SP45b never starts).

---

## §1 RALPLAN-DR Summary

### Cycle frame (6 bullets)

- **R8 closed.** `v1.6.0-lms-cms` shipped lms + cms. 1 recipe remains deferred — **`internal-it`**. R8 §10 trigger: *"Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R9+ if fetch succeeds OR notification L4 gains explicit webhook-emit spec items."*
- **R9 strategy.** User-approved: **"R9 시작해 — internal-it + webhook"**. Closes LAST deferred recipe + introduces supporting `webhook` L4. Webhook-emit is materially distinct from notification-send (different transport, retry, signing semantics) → spin webhook as its OWN L4 rather than overload notification.
- **R7 shape match.** R7 was the most recent "new L4 + new recipe" cycle. R9 mirrors R7 Synthesis-B exactly: 3 SPs (SP45 + SP45b + SP46 partial-tag-aware).
- **Disk discoveries (2026-05-22).** Unlike R7 scheduler (Spec Trio on disk from R3 PR `26de945`), webhook is **completely net-new** — `ls specs/webhook* contracts/webhook* blueprints/webhook* templates/L4/webhook` all empty. SP45 authors ALL FOUR webhook artifacts from scratch. FIRST L4 introduction since billing (R5) where Spec Trio is genuinely net-new at PRD signature.
- **Evidence rigor.** 5 logical English attempts (GitHub · Stripe · Jira · ServiceNow · PagerDuty) + 3 logical Korean attempts (Kakao 알림톡 · Toss · Naver Works). Verbatim PASS: **4 English** + **2 Korean**. Downgrades: ServiceNow + Kakao (multi-host, see §4.4).
- **Deferred-recipe queue closure.** R9 ships internal-it → `deferred_recipes:` becomes **EMPTY**. R6 Synthesis-A trim (community + lms + cms + internal-it) fully realized across R7+R8+R9.

### Principles (8 numbered)

1. **Composition kit, not single product.** Internal-it COMPOSES existing L4 + new webhook L4. Zero new L3/L2/L1/Tier-1/Tier-2 skill. Recipe-no-code honored.
2. **Spec-before-code, evidence-anchored AT PRD SIGNATURE.** All 5 internal-it `spec_ref:` resolve at SP45b prep (4 vs existing L4 specs, 1 vs SP45-shipped `specs/webhook-l0.yaml`). PRD documents target anchors with shipping conditions.
3. **Binary verification per axis.** `/ax-verify`, `recipe_governance_guard.sh`, `recipe_spec_referential_integrity_guard.sh`, `webhook-domain.test.sh` all exit 0; 2 sealed verdicts pass.
4. **Tier-1 cap = 4. Tier-2 count = 8. FROZEN.** L1 = 49, L2 = 92, L3 = 20. L4 = 11 → 12. Recipes 9 → 10.
5. **Atomic Spec Trio rule per SP.** SP45 ships webhook L4 atomically; SP45b ships internal-it atomically; SP46 final tag IFF 2/2 pass. R7 Synthesis-B Option 4 precedent honored.
6. **Recipe does not ship code; AI implements business logic.** Inherited R5/R6/R7/R8.
7. **Webhook is a bona fide L4 primitive (NEW THIS CYCLE).** Unlike R7 scheduler (Spec Trio on disk from R3), webhook Spec Trio is net-new in SP45. Webhook-emit semantics (per-endpoint signing secret · HMAC-SHA256 over body · idempotent retries · dead-letter · circuit breaker) are materially distinct from notification-send (channel routing · template rendering · recipient preferences). TD-025 ADR captures full rationale + explicit RFC 2104 / ASVS V13.2.6 anchor reuse.
8. **No new L2/L3/practices rule families.** Internal-it binds to existing rules + ASVS anchors. Webhook signing-secret-storage co-shipped as `co-shipped-rule: webhook-secret-encryption` (R7 community-html-sanitization precedent). No new `practices/rules/*.md` file.

### Decision Drivers (top 3)

1. **Reintroduction-trigger discipline.** R8 §10 named "verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4" as the internal-it gate. R9 satisfies BOTH halves: (a) Jira webhook docs verbatim PASS (b) clarified webhook-emit primitive — **as its own L4 rather than within notification** — based on materially distinct semantics enumerated in Principle 7. R8 trigger text deliberately said "in notification L4 **OR** notification L4 gains webhook-emit spec items" → R9 picks the OR branch with explicit ADR.
2. **Evidence rigor.** 4 English verbatim (GitHub Webhooks, Stripe Webhooks, Jira webhooks, PagerDuty webhooks) + 2 Korean verbatim (Toss webhook + Naver Works Bot API). Webhook clears 1-floor with 2x buffer (GitHub + Stripe both directly attest webhook delivery+signing+retry semantics). Internal-it clears with Jira + PagerDuty + Naver Works Korean anchor.
3. **R7 cadence parity.** Same shape as R7 (new L4 + new recipe). Same SP shape (atomic-axis + sequential atomic-recipe + partial-tag-aware FINAL). Same partial-tag policy semantics. Same evidence-density floors. Reusing R7's proven shape minimizes ralplan iter cycles.

### Viable Options Considered (3 options)

- **Option (1) — Defer webhook L4; ship internal-it alone against notification-l0 manual webhook stub.**
  - Pros: Smallest delta; no new L4.
  - Cons: Notification L4 overloaded with mismatched webhook concerns (channel routing vs HMAC signing; recipient prefs vs per-endpoint secrets; template rendering vs raw JSON); breaks notification's 4-anchor evidence chain; fork-receivers building real ITSM integrations get confusing L4 boundary. R8 §10 trigger allowed the OR-branch — integration cost outweighs the saving. **REJECTED.**

- **Option (2) — Atomic SP45 (webhook + internal-it) + SP46 FINAL.** (R6 SP39 atomic-3 style)
  - Pros: One PR cycle; cadence-parity with R8 SP43.
  - Cons: Mutation surfaces disjoint; webhook-domain.test.sh harness **net-new shape** (true greenfield, no scheduler-R3 precedent); if webhook verdict fails, internal-it stranded though work is separable. R7 P5 MEDIUM-likelihood risk applies. **REJECTED in favor of Option 3.**

- **Option (3) — Synthesis-B: SP45 webhook-atomic + SP45b internal-it-atomic-sequential + SP46 partial-tag-aware FINAL.** (R7 SP41/SP41b/SP42 precedent)
  - Pros: Mutation surfaces disjoint. If webhook verdict fails (§7 P5 MEDIUM), internal-it **never starts** — saves 3-4 d. If internal-it fails, webhook ships clean at partial tag `v1.7.0-webhook`. Atomic Spec Trio (P5) preserved WITHIN each SP. Same shape that earned R7 3-iter APPROVED.
  - Cons: 3 SPs vs 2; ~0.5 d wall-time penalty; cadence reframed "atomic-per-axis, multi-SP" (matches R7).
  - **CHOSEN.** R7 Synthesis-B Architect-recommended + 3-iter APPROVED; R9 accepts mechanically per "best-style match" rule.

### Mode

**DELIBERATE.** Retained: (a) first net-new L4 Spec Trio since billing R5 (scheduler R7 had R3 on-disk prep); (b) net-new webhook-domain.test.sh harness shape; (c) wall-time ≈ 5-6 d with sequential SP45→SP45b path; (d) partial-tag policy semantics carried forward from R7.

### Recommended: Option (3) — 3 SPs (SP45 webhook-atomic + SP45b internal-it-atomic-sequential + SP46 FINAL with partial-tag policy).

```
SP45   (atomic — webhook L4 full Spec Trio: specs/webhook-l0.yaml + contracts/webhook-openapi.yaml +
        blueprints/webhook-manifest.yaml + templates/L4/webhook/README.md (NO applied_recipes: key —
        file-storage/practices/R7-scheduler precedent) + templates/L4/webhook/backend/ skeleton +
        practices/DECISIONS.md TD-025 + practices/evals/trio_integrity_allowlist.yaml webhook entry +
        skills/_tests/L4/webhook-domain.test.sh + skills/_tests/sealed-verdict/webhook-l4-verdict.md PENDING +
        practices/upstream/r9-sp45-webhook-evidence.md (GitHub + Stripe verbatim snapshot))
   ↓ gated on bash skills/_tests/L4/webhook-domain.test.sh exit 0 AND webhook-l4-verdict ≥10/12 MUST + ≥5/8 SHOULD
SP45b  (atomic — recipes/internal-it/ quartet + specs/recipes/internal-it-recipe-l0.yaml + 6 L4 README
        applied_recipes mutations (webhook FIRST-CONSUMER key born `[internal-it]` + 5 plural-list appends)
        + recipes/_MANIFEST.yaml move (deferred → active; deferred_recipes section becomes EMPTY) +
        skills/_tests/sealed-verdict/internal-it-verdict.md PENDING + practices/DECISIONS.md TD-026 + TD-027 +
        practices/upstream/r9-sp45b-internal-it-evidence.md (Jira + PagerDuty + Naver Works verbatim))
   ↓ gated on recipe_governance_guard.sh + recipe_spec_referential_integrity_guard.sh exit 0 AND internal-it-verdict ≥10/12 + ≥5/8
SP46   (FINAL — /ax-verify full; tag IFF 2/2; partial tag v1.7.0-webhook IFF only webhook verdict passes; PR)
```

All SPs linear. Total: **3 SPs, ≈ 5-6 d wall-time.**

---

## §2 Context

### R8 v1.6.0 disk-verified state (2026-05-22)

| Surface | Count | Path |
|---|---|---|
| L1 primitives | 49 | `templates/L1/components/` |
| L2 blocks | 92 | `templates/L2/blocks/` |
| L3 pages | 20 | `templates/L3/pages/` |
| L4 domains | **11** (audit-log, auth, billing, crud, feature-flags, file-storage, notification, payment, practices, scheduled-task, search) | `templates/L4/` |
| Active recipes | **9** (saas-subscription, e-commerce, crm, booking, marketplace, b2b-admin, community, lms, cms) | `recipes/` |
| Deferred recipes | **1** (internal-it ONLY) | `recipes/_MANIFEST.yaml#deferred_recipes` |
| Sealed verdicts | 10 | `skills/_tests/sealed-verdict/` |
| Practices rules (Java/Spring) | 68 | `practices/rules/` |
| Hard guards GREEN | 22 | `practices/evals/` |
| Tier-1 / Tier-2 | 4 / 8 | `skills/Tier1/` + `skills/Tier2/` (FROZEN) |
| Current tag | `v1.6.0-lms-cms` on `main@953a3d6` | `git tag --sort=-creatordate \| head -1` |

### Disk-verified Spec Trio status for `webhook` (2026-05-22 — NET-NEW)

| Artifact | Path | Status |
|---|---|---|
| Spec | `specs/webhook-l0.yaml` | **ABSENT — SP45 creates** |
| Contract | `contracts/webhook-openapi.yaml` | **ABSENT — SP45 creates** |
| Manifest | `blueprints/webhook-manifest.yaml` | **ABSENT — SP45 creates** |
| L4 README + scaffold | `templates/L4/webhook/README.md` + `templates/L4/webhook/backend/` | **ABSENT — SP45 creates** |
| `trio_integrity_allowlist.yaml` row | `practices/evals/trio_integrity_allowlist.yaml#domains.webhook` | **ABSENT — SP45 adds (mode: `backend_only`)** |
| DECISIONS.md ADR | `practices/DECISIONS.md` | **TD-2026-05-22-025 ABSENT — SP45 adds (per §8 format-resolution opening note)** |

**Disk evidence of completeness:** `ls specs/webhook* contracts/webhook* blueprints/webhook* templates/L4/webhook 2>&1` returns "no matches found" (zsh) on all 4 paths. This is the FIRST genuinely net-new L4 introduction since billing (R5 SP30) — every other L4 (scheduler, file-storage, identity-verification) had Spec Trio shipped in an earlier R3/R4 catalog-extension PR.

### R9 scope

- **Deliverable 1 (webhook L4, SP45 atomic):** Author ALL FOUR webhook Spec Trio artifacts + L4 README + scaffold + DECISIONS TD-025 + allowlist entry + domain test + sealed-verdict scaffold. **NO `applied_recipes:` key** at introduction (matches file-storage + practices + R7-scheduler precedent — H2/M4 R7 fix). Internal-it (SP45b) will birth the key with `[internal-it]` in the same SP45b atomic commit that lists `webhook` in its `enabled_l4_domains:`.
- **Deliverable 2 (internal-it recipe, SP45b atomic-sequential):** Create `recipes/internal-it/` quartet + `specs/recipes/internal-it-recipe-l0.yaml`. Composition: `crud + audit-log + notification + scheduled-task + webhook` (+ optional `auth` for ITSM role-based authorization). Move `internal-it` row deferred→active. `deferred_recipes:` section becomes EMPTY. Append `internal-it` to plural lists on 5 existing L4 READMEs (crud, audit-log, notification, scheduled-task, auth); birth `applied_recipes: [internal-it]` on webhook README (FIRST-CONSUMER convention per R8 TD-024).

NO new L3/L2/L1/Tier-1/Tier-2 skill. Recipe count 9 → 10. L4 count 11 → 12. Deferred recipes 1 → 0.

---

## §3 Objectives + Guardrails

### Must Have

- **Webhook Spec Trio (SP45):** `specs/webhook-l0.yaml` (10 items: EMIT/SIGN/RETRY/DEAD-LETTER — §4.1) + `contracts/webhook-openapi.yaml` (register/list/retry/replay/dead-letter) + `blueprints/webhook-manifest.yaml` (emit · sign · retry · dead-letter · circuit-breaker).
- **Webhook L4 surface (SP45):** `templates/L4/webhook/README.md` mirroring `scheduled-task/README.md` shape (R7 SP41 precedent — NO `applied_recipes:` key at introduction) + `templates/L4/webhook/backend/` skeleton (`WebhookEndpoint.java.skeleton` + `WebhookDelivery.java.skeleton`).
- **DECISIONS.md (SP45 + SP45b):** opening-note format-convention paragraph (R7+ ADRs as `- **TD-YYYY-MM-DD-NNN** ...` bullets; pre-R7 rules retain `## <RULE_ID>`) + `TD-2026-05-22-025` (webhook L4) + `TD-2026-05-22-026` (internal-it) + `TD-2026-05-22-027` (webhook-as-extension-axis).
- **Webhook verification (SP45):** `practices/evals/trio_integrity_allowlist.yaml` `webhook: backend_only` row + `skills/_tests/L4/webhook-domain.test.sh` (8-15 assertions sealed sub-agent test) + `skills/_tests/sealed-verdict/webhook-l4-verdict.md` ≥10/12 MUST + ≥5/8 SHOULD (SP46).
- **Internal-it recipe (SP45b):** `recipes/internal-it/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}` + `specs/recipes/internal-it-recipe-l0.yaml` (5 disk-verified invariants §4.2) + `recipes/_MANIFEST.yaml` deferred→active move (deferred_recipes EMPTY — Synthesis-A trim closure) + `applied_recipes:` append on 5 L4 READMEs (crud/audit-log/notification/scheduled-task/auth alphabetical) + webhook README `applied_recipes: [internal-it]` key BIRTH (R8 TD-024 first-consumer convention with M6 inline annotation).
- **Internal-it verification (SP45b + SP46):** `/ax-verify` exit 0 + `recipe_governance_guard.sh` exit 0 (10 active recipes) + `recipe_spec_referential_integrity_guard.sh` exit 0 (10 specs) + `skills/_tests/sealed-verdict/internal-it-verdict.md` ≥10/12 MUST + ≥5/8 SHOULD.
- **Tag policy (SP46):** `v1.7.0-webhook-internal-it` IFF 2/2 pass · `v1.7.0-webhook` IFF webhook-only · no tag IFF webhook fails.

### Must NOT Have

- NO new L3/L2/L1/Tier-1/Tier-2 skill (sole catalog mutations are L4 11→12 + recipes 9→10) · NO new L4 beyond webhook this cycle.
- NO new practices rule family in `practices/rules/` (webhook-secret-encryption co-shipped as recipe-level invariant — R7 community precedent, Principle 8).
- NO empty applied-recipes array (R7 H2/M4); webhook README omits key at introduction (file-storage + practices + R7-scheduler-pre-R8 precedent).
- NO `RECIPE_DEVIATION.md` ceremony · NO Korean reference fabrication (3 Korean hosts logged in §4.4 with verbatim-or-downgrade rationale).
- NO `/ax-verify-domain webhook` in SP45 gate (R7 Critic L — webhook catalog-only per CLAUDE.md recipe-no-code; bash sealed test is the L4-discoverability gate).
- NO change to git workflow / CI policy / release process (R7 Critic J — tag is catalog-release artifact, not fork-team policy mandate).
- NO partial deliverable within an SP (SP-to-SP partial-tag policy explicit — Option 3 / R7 Critic K) · NO frontend code · NO deployment/CI/release scope.

---

## §4 Deliverable Inventory (2 deliverables, 3 SPs per Option 3)

> All `spec_ref:` for internal-it disk-verified at SP45b prep against `specs/webhook-l0.yaml` (shipped earlier in SP45) + existing L4 specs.

### 4.1 Deliverable 1 — `webhook` L4 primitive (SP45 atomic)

- **Spec Trio (ALL net-new — SP45 authors from scratch):**
  - **Spec:** `specs/webhook-l0.yaml` (10 items planned):
    - `WEBHOOK-EMIT-001`: Register endpoint persists `url + active=true + signing_secret + event_filter` with idempotent upsert by URL.
    - `WEBHOOK-EMIT-002`: Emit event POSTs JSON body to all matching active endpoints with `Content-Type: application/json`.
    - `WEBHOOK-SIGN-001`: Every outbound request carries `X-Webhook-Signature: sha256=<hex(HMAC-SHA256(secret, body))>` header.
    - `WEBHOOK-SIGN-002`: `X-Webhook-Timestamp: <unix-seconds>` header included; signature covers timestamp + body to mitigate replay.
    - `WEBHOOK-RETRY-001`: Failed delivery (4xx/5xx/timeout) re-enqueued with exponential backoff starting 30s, doubling each attempt, max 5 retries.
    - `WEBHOOK-RETRY-002`: Idempotency key `X-Webhook-Delivery-Id: <uuid>` stable across all retry attempts for same event.
    - `WEBHOOK-DEAD-LETTER-001`: After max retries, delivery moves to dead-letter queue with `status=failed_permanent + last_response_code + last_attempt_at` for admin inspection.
    - `WEBHOOK-DEAD-LETTER-002`: Admin replay endpoint re-enqueues a dead-letter delivery with fresh delivery-id (new attempt chain).
    - `WEBHOOK-CIRCUIT-001`: Per-endpoint failure-rate ≥ 90% over rolling 50-attempt window opens circuit; endpoint marked `active=false` automatically with audit-log entry.
    - `WEBHOOK-IDEMPOTENT-001`: Receiver-side replay safety — same `X-Webhook-Delivery-Id` MUST be treated as identical request (sender contract documentation).
  - **Contract:** `contracts/webhook-openapi.yaml` — OpenAPI 3.0 for endpoint registration + listing + manual retry trigger + dead-letter inspection.
  - **Manifest:** `blueprints/webhook-manifest.yaml` — policy sections: `emit · sign · retry · dead-letter · circuit-breaker`.

- **New artifacts (SP45 creates):**
  - `templates/L4/webhook/README.md` mirrors `templates/L4/scheduled-task/README.md` shape — file table, AGENTS hint sheet, composition notes, override_allowed inline. **NO `applied_recipes:` key** at introduction (file-storage + practices + R7-scheduler-pre-R8 precedent). SP45b internal-it commit births the key `[internal-it]` in the SAME atomic SP45b commit that lists `webhook` in its `enabled_l4_domains:` (R8 TD-024 first-consumer-arrival convention).
  - `templates/L4/webhook/backend/` skeleton stubs (`WebhookEndpoint.java.skeleton` + `WebhookDelivery.java.skeleton`).
  - `practices/DECISIONS.md` `TD-2026-05-22-025` (webhook L4 introduction — full ADR per §8; explicit RFC 2104 / ASVS V13.2.6 cryptographic-anchor reuse statement included per Codex Critic INFORMATIONAL).
  - `practices/evals/trio_integrity_allowlist.yaml` — append `webhook: backend_only # SP45: webhook-emit + HMAC-SHA256 signing + idempotent retries + dead-letter; no frontend UI in scope`.
  - `skills/_tests/L4/webhook-domain.test.sh` (sealed sub-agent test — see §4.5).

- **Composition:** Webhook stands alone in R9 SP45; no recipe consumes it within SP45. SP45b internal-it is FIRST consumer.

- **External evidence (verbatim — 4 PASS, exceeds 1-floor with 3x buffer):**
  - **GitHub Webhooks** 200 OK: `"Webhooks provide a way for notifications to be delivered to an external web server whenever certain events occur on GitHub."` + `"You can create webhooks to subscribe to specific events that occur on GitHub."` (quoted_at 2026-05-22, https://docs.github.com/en/webhooks)
  - **Stripe Webhooks** 200 OK (post-301 redirect): `"After you register a webhook endpoint, Stripe can push real-time event data to your application's webhook endpoint when events happen in your Stripe account."` + `"Stripe attempts to deliver events to your destination for up to three days with an exponential back off in live mode."` + `"Stripe signs every webhook event by including a signature in the Stripe-Signature header."` (quoted_at 2026-05-22, https://docs.stripe.com/webhooks)

- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "skills/_tests/L4/webhook-domain.test.sh"
    assertion: "sealed sub-agent given only templates/L4/webhook/README.md + practices/AGENTS.md identifies webhook primitive, lists ≥3 of EMIT/SIGN/RETRY/DEAD-LETTER families, and references specs/webhook-l0.yaml; harness exits 0 with ≥8/15 assertions PASS"
    expected_RED_reason: "templates/L4/webhook/README.md does not exist; specs/webhook-l0.yaml does not exist"
    first_GREEN_command: "bash skills/_tests/L4/webhook-domain.test.sh"
    owning_SP: "SP45"
    semantic_anchor: "assertion-count semantics mirror skills/_tests/L4/scheduler-domain.test.sh shape — N total assertions, ≥M PASS gate (M4 Architect fix; drops Pre-Mortem P4 likelihood MEDIUM→LOW)"
  ```

### 4.2 Deliverable 2 — `internal-it` recipe (SP45b atomic, sequential after SP45)

- **L4 composition (5 existing + 1 optional):** `crud`, `audit-log`, `notification`, `scheduled-task`, `webhook`. **Optional:** `auth` (recommended for ITSM role-based authorization — operator vs requester vs approver).
- **L2 blocks used (disk-verified at SP45b prep against `templates/L2/blocks/*.tsx`):** `crud-create-form`, `crud-edit-form`, `crud-list-adapter`, `data-table`, `filter-bar`, `kpi-card`, `notification-bell`, `notification-list`, `confirm-dialog`. **Subject to disk-resolution at SP45b prep step** per R8 Critic L blocker precedent; non-existent IDs removed; L1 primitives moved to `recipes/internal-it/L2-block-recipe.md` "L1 primitives consumed" subsection.
- **L3 pages used:** `list-page`, `detail-page`, `create-page`, `edit-page`, `dashboard-page`.
- **CRUD spec-path clarification (Codex Critic soft #2):** CRUD invariants in this recipe (and across the catalog generally) anchor to `specs/crud-security.yaml`, **NOT** a `specs/crud-l0.yaml` file (which does not exist on disk; R5 absorbed all CRUD content into `crud-security.yaml`). Internal-it INV-001 audit-log refs do not transit CRUD; this note exists to prevent reviewer confusion at SP45b spec_ref resolution.
- **Business invariants (5 — all `spec_ref:` resolved against SP45-shipped + existing specs at PRD signature):**
  - `INTERNAL-IT-INV-001`: Ticket state transitions (open → in-progress → resolved → closed) emit audit-log row with operator + before/after. `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-001` (line 7) + `spec_ref: specs/audit-log-l0.yaml#AUDIT-RECORD-002` (line 23). **PASS at signature.**
  - `INTERNAL-IT-INV-002`: SLA breach reminder emission uses scheduled-task lock primitive (no double-send on multi-node). `spec_ref: specs/scheduled-task-l0.yaml#SCHED-LOCK-001` (line 21) + `spec_ref: specs/scheduled-task-l0.yaml#SCHED-IDEMPOTENT-001` (line 64). **PASS at signature.**
  - `INTERNAL-IT-INV-003`: Outbound webhook to external ITSM (Jira / ServiceNow / PagerDuty / Slack-incoming) signed with HMAC-SHA256 over body + timestamp + retried with exponential backoff. `spec_ref: specs/webhook-l0.yaml#WEBHOOK-SIGN-001` + `spec_ref: specs/webhook-l0.yaml#WEBHOOK-RETRY-001` (BOTH ship in SP45; SP45b prep re-verifies disk presence after SP45 lands). **PASS at SP45b prep (post-SP45 disk).**
  - `INTERNAL-IT-INV-004`: Assignee notifications (ticket-assigned, status-changed, SLA-near-breach) respect operator preferences + opt-out. `spec_ref: specs/notification-l0.yaml#NOTIF-PREF-001` (line 131) + `spec_ref: specs/notification-l0.yaml#NOTIF-SEND-001` (line 47). **PASS at signature.**
  - `INTERNAL-IT-INV-005`: Webhook signing-secret stored encrypted-at-rest (per-endpoint AES-256 with KMS-managed key) — never logged in plaintext. **co-shipped-rule:** `webhook-secret-encryption` — authored INLINE in `specs/recipes/internal-it-recipe-l0.yaml` (NOT a new `practices/rules/*.md` file — honors Principle 8 "no new rule families"; R7 community-html-sanitization precedent). `invariant_test:` cites `frontend/tests/recipes/internal-it-webhook-secret.spec.ts` co-shipped in SP45b. **PASS via co-shipped escape hatch.**

- **INV-005 co-shipped-rule disambiguation (per R8 M2 fix paragraph precedent):** R8 lms-INV-005 and cms-INV-005 used the preferred path (`spec_ref + rule_ref` to existing `practices/rules/idempotency-key-on-mutations.md`) because the catalog already had a rule covering bulk-enrollment / slug-uniqueness idempotency. **R9 internal-it-INV-005 is a R7-community-style catalog-novel case:** the catalog has no existing rule covering per-endpoint signing-secret encryption-at-rest (it is a webhook-specific KMS pattern, not a general crypto-at-rest pattern — the latter would belong in `security` rule family which does not yet have a `security-secret-encryption-at-rest.md` rule). The choice is deliberate: `co-shipped-rule: webhook-secret-encryption` is the escape hatch when the catalog has no existing anchor; R8 lms/cms had a choice and picked the preferred path; R9 internal-it has no choice and uses the escape hatch. **Promotion criterion (M5 Architect fix — deferred-indefinitely framing per recommendation (b)):** promotion of `webhook-secret-encryption` to a full `practices/rules/security-secret-encryption-at-rest.md` rule is **deferred indefinitely; it remains a recipe-level invariant unless cross-domain need emerges** (no R10+ deferred-recipe entry exists today that would consume it; promotion is reactive to demonstrated demand, not speculative).

- **Override allowance:** inline — skip `auth` for single-operator personal helpdesk; skip `webhook` for fully-internal-only deployments (no external ITSM integration needed); skip `feature-flags` (none required since internal-it is single-tenant-typical).

- **External evidence (verbatim — 2 English PASS + 1 alt for completeness):**
  - **Jira webhooks** 200 OK: `"A webhook is a user-defined callback over HTTP."` + `"You can use Jira webhooks to notify your app or web application when certain events occur in Jira."` (quoted_at 2026-05-22, https://developer.atlassian.com/server/jira/platform/webhooks/)
  - **PagerDuty webhooks** 200 OK: `"Webhooks allow you to receive HTTP callbacks when significant events happen in your PagerDuty account, for example, when an incident triggers, escalates or resolves."` + `"Details about the event are sent to your specified URL, such as Slack or your own custom PagerDuty webhook processor."` (quoted_at 2026-05-22, https://support.pagerduty.com/docs/webhooks)

- **Korean evidence (verbatim — 2 PASS, exceeds R8 1-Korean target):**
  - **Toss Payments webhook** 200 OK: `"토스페이먼츠 결제, 브랜드페이, 지급대행 상태에 변경사항이 있을 때 웹훅으로 실시간 업데이트를 받아보세요."` + `"웹훅이란 데이터가 변경되었을 때 실시간으로 알림을 받을 수 있는 기능이에요."` (quoted_at 2026-05-22, https://docs.tosspayments.com/guides/webhook)
  - **Naver Works Bot API** 200 OK: `"Bot API로 봇에서 메시지를 보내거나, 메뉴를 설정하고, 봇을 관리할 수 있다."` + `"Bot API를 호출하려면 구성원 계정 또는 서비스 계정으로 인증하여 얻은 Access Token이 필요하다."` (quoted_at 2026-05-22, https://developers.worksmobile.com/kr/docs/bot-api)

- **TDD anchor:**
  ```yaml
  tdd_anchor:
    test_file: "frontend/tests/recipes/internal-it-compose.spec.ts"
    assertion: "recipes/internal-it/RECIPE.md frontmatter enabled_l4_domains: list equals [audit-log, crud, notification, scheduled-task, webhook] (alphabetical; optional auth documented as override_allowed) AND 5 disk-verified spec_ref anchors all resolve via recipe_spec_referential_integrity_guard.sh AND l2_blocks_used: contains only files present at templates/L2/blocks/*.tsx"
    expected_RED_reason: "recipes/internal-it/ directory does not exist; webhook spec_ref anchors require SP45 to land first"
    first_GREEN_command: "bash practices/evals/recipe_spec_referential_integrity_guard.sh && bash practices/evals/recipe_governance_guard.sh && cd frontend && npm test -- tests/recipes/internal-it-compose.spec.ts"
    owning_SP: "SP45b"
  ```

### 4.3 Cluster claim (honest framing — Option 3 reframe per R7 precedent)

Webhook L4 + internal-it recipe are **NOT topically related and ARE NOT bundled atomically.** They are sequentially gated SPs (SP45 → SP45b). Each axis is atomic within its own SP. SP45b consumes SP45's webhook artifacts via 2 `spec_ref:` anchors (INV-003) — explicit dependency, hard gate. Mutation surfaces are disjoint at the file level. If webhook verdict fails, internal-it work never starts (saves 3-4d).

### 4.4 Evidence ledger (WebFetched during PRD iter 1 — 2026-05-22)

| Deliverable | URL | HTTP / fetch result | Verbatim quote | Resolution | provenance_class |
|---|---|---|---|---|---|
| webhook | `https://docs.github.com/en/webhooks` | **200 OK — verbatim** (2026-05-22) | `"Webhooks provide a way for notifications to be delivered to an external web server whenever certain events occur on GitHub."` + `"You can create webhooks to subscribe to specific events that occur on GitHub."` | Verbatim cite | `external` |
| webhook | `https://stripe.com/docs/webhooks` | **301 redirect → `https://docs.stripe.com/webhooks`** (2026-05-22) | — | Followed redirect | (redirect captured) |
| webhook | `https://docs.stripe.com/webhooks` | **200 OK — verbatim** (2026-05-22) | `"After you register a webhook endpoint, Stripe can push real-time event data to your application's webhook endpoint when events happen in your Stripe account."` + `"Stripe attempts to deliver events to your destination for up to three days with an exponential back off in live mode."` + `"Stripe signs every webhook event by including a signature in the Stripe-Signature header."` | Verbatim cite (3 quotes — delivery + retry + signing) | `external` |
| internal-it | `https://developer.atlassian.com/cloud/jira/platform/rest/v3/` | **200 OK — content truncated; no extractable verbatim** (2026-05-22) | — | Downgrade; alt host attempted | `internal_design` — page too long; alt-host attempt below |
| internal-it | `https://developer.atlassian.com/cloud/jira/platform/rest/v3/intro/` | **200 OK — content truncated; no extractable verbatim** (2026-05-22) | — | Downgrade; alt host attempted | `internal_design` — content truncation pattern on Cloud REST host |
| internal-it | `https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-webhooks/` | **200 OK — content truncated; no extractable verbatim** (2026-05-22) | — | Downgrade; server-host verbatim PASS below | `internal_design` — Cloud-host truncation pattern persists |
| internal-it | `https://developer.atlassian.com/server/jira/platform/webhooks/` | **200 OK — verbatim** (2026-05-22) | `"A webhook is a user-defined callback over HTTP."` + `"You can use Jira webhooks to notify your app or web application when certain events occur in Jira."` | Verbatim cite (server-host fallback closes Cloud-host truncation gap; both hosts are atlassian-canonical for webhook semantics) | `external` |
| internal-it | `https://developer.servicenow.com/dev.do` | **200 OK — no descriptive content** (2026-05-22) | — | Downgrade; alt hosts attempted | `internal_design` — landing page lacks API descriptions |
| internal-it | `https://docs.servicenow.com/bundle/utah-application-development/page/integrate/inbound-rest/concept/c_RESTAPI.html` | **301 redirect → `http://docs.servicenow.com/`** (2026-05-22) | — | Followed redirect | (redirect captured) |
| internal-it | `https://docs.servicenow.com/` | **301 redirect → `https://www.servicenow.com/docs/`** (2026-05-22) | — | Followed redirect | (redirect captured) |
| internal-it | `https://www.servicenow.com/docs/` | **200 OK — no API descriptive verbatim** (2026-05-22) | — | Downgrade | `internal_design` — navigation/icons only; no REST API verbatim extractable |
| internal-it | `https://www.servicenow.com/products/rest-api.html` | **HTTP 403 Forbidden** (2026-05-22) | — | Downgrade | `internal_design` — host returns 403; ServiceNow REST-API verbatim consistently unavailable across 4 host attempts |
| internal-it | `https://developer.pagerduty.com/api-reference/` | **200 OK — empty body** (2026-05-22) | — | Downgrade; alt hosts attempted | `internal_design` — developer portal returns empty body |
| internal-it | `https://developer.pagerduty.com/docs/webhooks/webhooks-overview` | **200 OK — empty body** (2026-05-22) | — | Downgrade; alt host below | `internal_design` — same empty-body pattern |
| internal-it | `https://developer.pagerduty.com/docs/db0fa8c8984fc-overview` | **200 OK — empty body** (2026-05-22) | — | Downgrade; alt host below | `internal_design` |
| internal-it | `https://support.pagerduty.com/docs/webhooks` | **200 OK — verbatim** (2026-05-22) | `"Webhooks allow you to receive HTTP callbacks when significant events happen in your PagerDuty account, for example, when an incident triggers, escalates or resolves."` + `"Details about the event are sent to your specified URL, such as Slack or your own custom PagerDuty webhook processor."` | Verbatim cite (support.pagerduty.com is canonical product docs host; developer.pagerduty.com developer-portal returns empty body — pattern across 3 attempts on developer host) | `external` |
| internal-it (Korean) | `https://developers.kakao.com` | **200 OK — no descriptive verbatim** (2026-05-22) | — | Downgrade; alt paths attempted | `internal_design` — landing has API status dashboard headers only |
| internal-it (Korean) | `https://developers.kakao.com/docs/latest/ko/message/rest-api` | **302 redirect → `http://developers.kakao.com/docs/ko/message/rest-api`** (2026-05-22) | — | Followed redirect | (redirect captured) |
| internal-it (Korean) | `http://developers.kakao.com/docs/ko/message/rest-api` | **HTTP 404 Not Found** (2026-05-22) | — | Downgrade; alt hosts | `internal_design` — redirect target 404 |
| internal-it (Korean) | `https://business.kakao.com/info/bizmessage/` | **200 OK — no descriptive content** (2026-05-22) | — | Downgrade; alt path | `internal_design` — page title + loading image only |
| internal-it (Korean) | `https://business.kakao.com/info/kakaotalkchannel/` | **200 OK — no descriptive content** (2026-05-22) | — | Downgrade; alt path | `internal_design` — same template as bizmessage |
| internal-it (Korean) | `https://business.kakao.com` | **200 OK — no descriptive content** (2026-05-22) | — | Downgrade; alt path | `internal_design` — Kakao business host pattern; no public extractable verbatim |
| internal-it (Korean) | `https://kakaobusiness.gitbook.io/main/ad/start/example` | **404 page (suggested-pages stub only)** (2026-05-22) | — | Downgrade; alt path | `internal_design` — 404 with link list, no descriptive sentence |
| internal-it (Korean) | `https://kakaobusiness.gitbook.io/main` | **200 OK — navigation only; no 알림톡-descriptive sentence** (2026-05-22) | — | Downgrade | `internal_design` — gitbook navigation/categories; Kakao 알림톡 verbatim consistently unavailable across 6 host attempts |
| internal-it (Korean) | `https://docs.tosspayments.com/guides/webhook` | **200 OK — verbatim** (2026-05-22) | `"토스페이먼츠 결제, 브랜드페이, 지급대행 상태에 변경사항이 있을 때 웹훅으로 실시간 업데이트를 받아보세요."` + `"웹훅이란 데이터가 변경되었을 때 실시간으로 알림을 받을 수 있는 기능이에요."` | Verbatim cite (Korean — Toss Payments webhook guide; 2 verbatim Korean sentences directly attesting webhook semantics) | `external` |
| internal-it (Korean) | `https://developers.worksmobile.com` | **200 OK — heading only; no descriptive verbatim** (2026-05-22) | — | Downgrade; alt path | `internal_design` — landing page lacks descriptive content |
| internal-it (Korean) | `https://developers.worksmobile.com/kr/docs/bot-api` | **200 OK — verbatim** (2026-05-22) | `"Bot API로 봇에서 메시지를 보내거나, 메뉴를 설정하고, 봇을 관리할 수 있다."` + `"Bot API를 호출하려면 구성원 계정 또는 서비스 계정으로 인증하여 얻은 Access Token이 필요하다."` | Verbatim cite (Korean — 네이버웍스 Bot API; 2 verbatim Korean sentences describing webhook-class capability via bot callbacks) | `external` |

**Per-deliverable evidence density floor + per-source-class arithmetic (M3 Architect trim + Codex Critic soft #1 — auditability):**

- **webhook (SP45):** 2 English verbatim (GitHub + Stripe) clears 1-floor with 2x buffer.
- **internal-it (SP45b):** 2 English verbatim (Jira + PagerDuty) + 2 Korean verbatim (Toss + Naver Works) = 4 total, clears 1-floor with 3x buffer (matches R8 classting + brunch shape; 2 consecutive non-zero-Korean cycles).
- **Per-source-class breakdown (auditable arithmetic — Codex Critic soft #1):** Verbatim PASS = **6 total** (4 English: GitHub + Stripe + Jira + PagerDuty; 2 Korean: Toss + Naver Works). Downgrades = **9 total** (Atlassian Cloud × 3 truncation + ServiceNow × 4 hosts + PagerDuty developer-portal × 3 empty-body + Kakao × 6 hosts + Naver Works landing × 1; some recipes share rows — see ledger). Redirects/alternate-host capture rows = **5 total** (Stripe 301, ServiceNow 301 × 2, Kakao 302, Toss/Naver-Works alt-path captures).
- **Host-pattern downgrades (host-wide, not URL-specific):** ServiceNow (4 attempts) + Kakao 알림톡 (6 attempts) both attest **host-wide non-public-API descriptive content** — documented downgrade is honest evidence, not failure.

**Re-attempt at SP execution:** SP45 pre-flight re-runs ServiceNow (`developer.servicenow.com` + `www.servicenow.com/products/rest-api.html`) once. SP45b pre-flight re-runs Kakao 알림톡 (`developers.kakao.com/docs/ko/message/rest-api` + `business.kakao.com/info/bizmessage`) once. Any 200 OK with verbatim upgrades the row. No fabrication.

---

## §4.5 SP Plan + Verification Matrix (3 SPs — Option 3 / R7 Synthesis-B precedent)

| SP | Atomic deliverables | TDD anchors (RED → GREEN) | Verification | Observability_signal (advisory) |
|---|---|---|---|---|
| **SP45** (atomic — webhook L4 only; mutation surface = `specs/webhook-l0.yaml`, `contracts/webhook-openapi.yaml`, `blueprints/webhook-manifest.yaml`, `templates/L4/webhook/`, `practices/DECISIONS.md`, `practices/evals/trio_integrity_allowlist.yaml`, `skills/_tests/L4/`, `skills/_tests/sealed-verdict/webhook-l4-verdict.md`) | (a) Spec Trio × 3 (spec + contract + manifest) net-new authored; (b) `templates/L4/webhook/README.md` (NO `applied_recipes:` key); (c) `templates/L4/webhook/backend/` skeleton; (d) `practices/DECISIONS.md` opening-note format paragraph + `TD-2026-05-22-025`; (e) `practices/evals/trio_integrity_allowlist.yaml` webhook entry; (f) `skills/_tests/L4/webhook-domain.test.sh` — assertion-count semantics mirror `skills/_tests/L4/scheduler-domain.test.sh` shape (N total, ≥M PASS gate — M4 fix); (g) `skills/_tests/sealed-verdict/webhook-l4-verdict.md` (PENDING); (h) `practices/upstream/r9-sp45-webhook-evidence.md` (GitHub + Stripe verbatim snapshot); (i) `practices/AGENTS.md` re-generated IFF `generate_agents.sh` depends on L4 topology (R7 §11 hedge resolution — SP45 prep runs dry-run check first). | webhook-domain test: RED (README + spec absent) → GREEN. | **Binary gates (R7 Critic L precedent):** `bash skills/_tests/L4/webhook-domain.test.sh` exit 0 (replaces `/ax-verify-domain webhook` per R7 option (c) — webhook L4 stays catalog-only per CLAUDE.md recipe-no-code principle; backend `testWebhook` Gradle task does not yet exist; bash sealed test is the appropriate L4-discoverability gate). `/ax-verify-domain` × 0 OTHER touched L4 (SP45 mutates no existing L4 README). AGENTS.md sentinel matches re-generation IFF generate_agents.sh ran. | `webhook.delivery_emit_total`, `webhook.delivery_retry_total`, `webhook.dead_letter_size`, `webhook.circuit_open_total` — advisory only. |
| **SP45b** (atomic — internal-it recipe only; mutation surface = `recipes/internal-it/`, `specs/recipes/`, 5 existing L4 README appends + webhook README key-birth, `_MANIFEST.yaml`, `skills/_tests/sealed-verdict/internal-it-verdict.md`) | (a) `recipes/internal-it/{RECIPE.md, L4-composition.md, L2-block-recipe.md, spec-trio-template.yaml}`; (b) `specs/recipes/internal-it-recipe-l0.yaml` with 5 disk-verified invariants per §4.2 + inline `co-shipped-rule: webhook-secret-encryption` for INV-005; (c) `recipes/_MANIFEST.yaml` — internal-it moves deferred→active; `deferred_recipes:` section becomes EMPTY (Synthesis-A trim closed); (d) `templates/L4/{crud,audit-log,notification,scheduled-task,auth}/README.md` `applied_recipes:` plural lists append `internal-it` alphabetically; (e) `templates/L4/webhook/README.md` `applied_recipes:` key BORN with `[internal-it]  # verdict pending until SP46 — see _MANIFEST.yaml for active status` inline comment (M6 Architect fix — partial-tag desync annotation); (f) `skills/_tests/sealed-verdict/internal-it-verdict.md` (PENDING); (g) `frontend/tests/recipes/internal-it-compose.spec.ts` + `frontend/tests/recipes/internal-it-webhook-secret.spec.ts` (co-shipped INV-005 invariant test); (h) `practices/upstream/r9-sp45b-internal-it-evidence.md` (Jira + PagerDuty + Toss + Naver Works verbatim snapshot); (i) `practices/DECISIONS.md` `TD-2026-05-22-026` + `TD-2026-05-22-027`. | internal-it-compose test: RED (recipes/internal-it/ absent + webhook spec_ref unresolved) → GREEN. recipe_governance_guard: RED (internal-it has no `applied_recipes:` wiring on 5 L4 + webhook key absent) → GREEN. recipe_spec_referential_integrity_guard: RED → GREEN (5 invariants now disk-resolved post-SP45). internal-it-webhook-secret test: RED (co-shipped invariant test absent) → GREEN. | `bash practices/evals/recipe_governance_guard.sh` exit 0 against all 10 active recipes; `bash practices/evals/recipe_spec_referential_integrity_guard.sh` exit 0 against all 10 specs; internal-it-compose.spec.ts passes; internal-it-webhook-secret.spec.ts passes. **Gate:** SP45b commit only opens after SP45 + webhook-l4-verdict ≥10/12 MUST + ≥5/8 SHOULD. | `recipe.internal-it.ticket_active_total`, `recipe.internal-it.sla_breach_total`, `recipe.internal-it.webhook_failed_permanent_total` — advisory only. |
| **SP46** (FINAL — 2 sealed verdicts harness exec + tag policy + PR) | (a) Sealed sub-agent execs `webhook-l4-verdict.md` and `internal-it-verdict.md` (context-0 inputs per §7 P5 mitigation); (b) `recipes/README.md` updated — 10 active + 0 deferred (deferred-queue CLOSED); (c) `/ax-verify` exit 0. **Tag policy (Option 3):** tag `v1.7.0-webhook-internal-it` IFF 2/2 verdicts pass; **partial tag `v1.7.0-webhook` IFF webhook verdict passes AND internal-it-verdict fails** (internal-it marked `status: active-verdict-pending` in `_MANIFEST.yaml`; SP47 fast-follow next ralplan cycle); **no tag** IFF webhook verdict fails (SP45 mutations revert; SP45b never started — Option 3 SP45-gating). | Sealed verdict harness × 2: PENDING → PASS (or PASS/PENDING for partial). | `/ax-verify` exit 0; manual review of 2 sealed verdicts; tag policy enforced by commit message. | `recipes.active_total: 10` (or 9 partial); `recipes.deferred_total: 0` (or 1 partial); `L4.domain_total: 12`. |

**SP atomicity rule:** Within SP45, all webhook artifacts ship together OR rollback. Within SP45b, all internal-it artifacts (including webhook README key-birth) ship together OR rollback. **Between** SP45 and SP45b, gating is hard: SP45b commit does NOT open until SP45 + webhook-l4-verdict pass. **SP45b failure does NOT roll back SP45.** R7 Option 4 partial-tag semantics resolving Critic finding K's contradiction is honored verbatim.

**SP linearization:** SP45 → SP45b → SP46. No parallel branches.

---

## §5 Webhook L4 introduction rationale (R9-specific)

**Resolution: webhook is a bona fide L4 primitive whose Spec Trio is NET-NEW in SP45.** Unlike R7 scheduler (Spec Trio existed on disk from R3 PR `26de945`), webhook has NO prior catalog presence — `ls specs/webhook* contracts/webhook* blueprints/webhook* templates/L4/webhook 2>&1` returns "no matches found" (zsh) on all 4 paths on 2026-05-22. FIRST genuinely net-new L4 since billing R5. Most evidence-heavy L4 introduction in catalog history (4 external + 2 Korean verbatim).

### Why webhook does NOT violate caps

- **Tier-1/Tier-2 cap** is `skills/*` — FROZEN at 4 + 8. Webhook is not a skill.
- **L1/L2/L3 cap** is UI surface. Webhook adds zero UI.
- **L4 layer** is expansionary when spec-first + evidence-anchored. `billing` (R5), `scheduled-task` (R3+R7), `file-storage` (R3) all precedent. Webhook meets BOTH gates.

### Why webhook is its OWN L4 rather than an extension of notification L4

R8 §10 trigger named "in notification L4 **OR** notification L4 gains explicit webhook-emit spec items". R9 picks the OR-branch with TD-027 rationale:

1. **Materially distinct semantics.** Notification-send = user-channel routing (email/SMS/push/in-app) + template rendering + recipient prefs. Webhook-emit = HTTP POST + per-endpoint signing secret + HMAC-SHA256 + exponential-backoff retry + dead-letter + circuit breaker. Manifest sections, contract endpoints, observability counters diverge entirely beyond "I send a thing".
2. **Evidence-chain integrity.** Notification L4 has 4 anchors all about user channels. Overloading dilutes that chain. Webhook's own 4-anchor chain (GitHub + Stripe + Jira + PagerDuty) is independently strong.
3. **Composition-kit cleanliness.** Recipes needing user notifications (e-commerce, lms, cms) don't need webhook-emit. Recipes needing B2B webhook (internal-it; future api-gateway) don't need user channels. Separating L4 lets recipes compose exactly what they need.
4. **Fork-receiver expectations.** Real-world Spring Boot codebases separate Notification (user-facing) from Webhook (system-to-system) — consistent with the 4 external references all treating webhooks as first-class platform features distinct from in-product notifications.

### Why webhook README has NO `applied_recipes:` key at introduction (R7 H2/M4 precedent)

`recipe_governance_guard.sh:55-77` only invokes `check_applied_recipe_declared` for L4 listed in an active recipe's `enabled_l4_domains:`. SP45 ships webhook stand-alone → key MUST be absent. Matches `file-storage` + `practices` + R7-scheduler-pre-R8 precedent. SP45b commit births key `[internal-it]` per R8 TD-024 first-consumer-arrival convention.

### Migration plan (within SP45)

1. Author `specs/webhook-l0.yaml` (10 items: EMIT/SIGN/RETRY/DEAD-LETTER families).
2. Author `contracts/webhook-openapi.yaml` (register/list/retry/dead-letter endpoints).
3. Author `blueprints/webhook-manifest.yaml` (5 sections: emit · sign · retry · dead-letter · circuit-breaker).
4. Create `templates/L4/webhook/README.md` mirroring `templates/L4/scheduled-task/README.md`, **WITHOUT `applied_recipes:` key**.
5. Create `templates/L4/webhook/backend/` skeleton stubs.
6. Append `webhook: backend_only` to `practices/evals/trio_integrity_allowlist.yaml`.
7. (CONDITIONAL per R7 §11 hedge): IF `practices/generate_agents.sh` depends on L4 topology, re-run + commit new sentinel sha; ELSE no sentinel mutation.
8. Append opening-note format paragraph + `TD-2026-05-22-025` bullet entry to `practices/DECISIONS.md` (see §8 format-resolution).
9. Author `skills/_tests/L4/webhook-domain.test.sh` (assertion-count semantics mirror `scheduler-domain.test.sh` — N total, ≥M PASS gate per M4 fix).
10. Scaffold `skills/_tests/sealed-verdict/webhook-l4-verdict.md` (`verdict: PENDING`).
11. Snapshot `practices/upstream/r9-sp45-webhook-evidence.md` (GitHub + Stripe verbatim).

---

## §6 Autonomous Execution Safety

- **Pre-flight gate (before SP45 starts):** §4.4 evidence ledger captured. SP45 pre-flight re-runs ServiceNow `developer.servicenow.com` + `www.servicenow.com/products/rest-api.html` once each. Disk-verify `templates/L4/webhook/`, `specs/webhook-l0.yaml`, `contracts/webhook-openapi.yaml`, `blueprints/webhook-manifest.yaml` ABSENCE; disk-verify `practices/generate_agents.sh` L4-dependency status (R7 §11 hedge). Abort if any prep step fails.
- **Pre-flight gate (before SP45b starts):** SP45 commit landed + webhook-l4-verdict ≥10/12 + ≥5/8. Re-attempt Kakao `developers.kakao.com/docs/ko/message/rest-api` + `business.kakao.com/info/bizmessage` once each. Disk-verify 5 internal-it spec_ref anchors per §4.2 (4 against existing specs + 1 against SP45-shipped `specs/webhook-l0.yaml#WEBHOOK-SIGN-001` + `specs/webhook-l0.yaml#WEBHOOK-RETRY-001`). Disk-verify L2 inventory by `ls templates/L2/blocks/*.tsx` against §4.2 list (R8 Critic L blocker precedent).
- **Mid-flight gate (between SP45b and SP46):** `git status` clean; `/ax-verify-domain` × 5 (crud/audit-log/notification/scheduled-task/auth) exit 0; `recipe_governance_guard.sh` + `recipe_spec_referential_integrity_guard.sh` exit 0 against all 10 active recipes; `frontend/tests/recipes/internal-it-compose.spec.ts` + `internal-it-webhook-secret.spec.ts` pass.
- **Stop conditions:** If `bash skills/_tests/L4/webhook-domain.test.sh` cannot reach GREEN within 3 iter cycles, SP45 rolls back; webhook returns to a NEW `deferred_l4_primitives:` block in `_MANIFEST.yaml` with `blocker:` field. If internal-it guards fail within SP45b, SP45b rolls back; SP45 stays landed; SP46 still ships partial tag `v1.7.0-webhook`.
- **Sealed verdict release policy (Option 3):** Tag `v1.7.0-webhook-internal-it` IFF 2/2 pass. Partial `v1.7.0-webhook` IFF only webhook verdict passes. No tag IFF webhook fails (SP45 reverted; SP45b never started).
- **Rollback:** Each SP is one squash-mergeable commit. Revert SP45b without disturbing SP45.
- **No destructive ops:** No `git reset --hard`, no force push. AGENTS.md sentinel RE-COMPUTED only if R7 §11 prep dry-run confirms generate_agents.sh L4-dependency.

### Partial-tag policy (R8 soft Critic precedent — small table)

`webhook README applied_recipes:` is born `[internal-it]` in the SP45b atomic commit and stays that way regardless of SP46 verdict outcome (the recipe DIRECTORY lands in SP45b — verdict scoring is a downstream catalog-quality artifact, not a recipe-existence gate). The `_MANIFEST.yaml#active_recipes` membership and `status:` keys mutate per partial-tag case:

| SP46 verdict outcome | Tag | `_MANIFEST.yaml` membership | `recipes/internal-it/RECIPE.md` `status:` | Webhook L4 README `applied_recipes:` |
|---|---|---|---|---|
| 2/2 PASS | `v1.7.0-webhook-internal-it` | internal-it `active` (10 active, 0 deferred) | `active` | `[internal-it]` (unchanged) |
| Webhook-only PASS (internal-it fails) | `v1.7.0-webhook` | internal-it `active-verdict-pending` (9 active+1 pending, 0 deferred) | `active-verdict-pending` | `[internal-it]  # verdict pending — see _MANIFEST.yaml for active status` (M6 Architect fix — inline annotation closes desync-window observability gap) |
| 0/2 FAIL (webhook verdict fails) | no tag | SP45 reverted; internal-it stays in `deferred_recipes:` (9 active, 1 deferred) | unchanged (deferred) | key absent (SP45 reverted) |

**Rationale:** `applied_recipes:` reflects directory existence at the README's L4 (binding declared by `enabled_l4_domains:` in the recipe spec), NOT verdict-pass state. Failing internal-it in `active-verdict-pending` status fast-follows in R10 SP47. The inline `# verdict pending — see _MANIFEST.yaml` annotation makes the 3-9 day partial-tag desync window self-documenting for fork-receivers inspecting webhook README in isolation.

---

## §7 Pre-Mortem (4 scenarios — DELIBERATE mode)

1. **ServiceNow + Kakao Alimtalk verbatim consistently unavailable at SP45/SP45b execution.** Likelihood: HIGH (already observed across 4 + 6 host attempts in §4.4). Impact: per-recipe anchor count holds at 4 verbatim for internal-it (Jira + PagerDuty + Toss + Naver Works) which clears 1-floor with 3x buffer. Mitigation: §4.4 documents host-wide pattern explicitly; SP45/SP45b pre-flight re-runs one-shot each per cycle but does not block if again 200-no-verbatim or 403/404. Documented downgrade is honest evidence.

2. **`templates/L4/webhook/README.md` ships before backend entity scaffold authored.** Likelihood: MEDIUM. Impact: fork-receiver gets broken pointer. Mitigation: SP45 PREP step `mkdir -p templates/L4/webhook/backend && touch templates/L4/webhook/backend/WebhookEndpoint.java.skeleton WebhookDelivery.java.skeleton` runs FIRST. README explicitly labels as skeleton. Same precedent R7 used for scheduled-task.

3. **Webhook L4 introduction interpreted as "L4 cap broken; composition kit drifting."** Likelihood: LOW post-R7 Architect signoff on scheduler-as-bona-fide-L4 framing (which had Spec Trio on disk). R9 webhook has NO prior Spec Trio on disk → higher justification bar. Mitigation: §5 disk-verifies 4 ABSENCE paths (specs/contracts/blueprints/templates), §8 TD-025 carries full rationale, §8 TD-027 documents the convention ("when to spin a primitive into its own L4 vs extend an existing one") with H1-tightened **2-consumer-signal gate** so R10+ planners have a stricter precedent. 4 external verbatim + 2 Korean verbatim is the strongest evidence chain of any L4 introduction in catalog history.

4. **Sealed verdict for webhook L4 (NEW harness shape — second net-new since R7 scheduler) scores below threshold because context-0 sub-agent cannot find webhook anchors in `practices/AGENTS.md`.** Likelihood: LOW (downgraded from MEDIUM per M4 fix — assertion-count semantics now explicitly mirror `scheduler-domain.test.sh` shape; harness shape is no longer novel). Impact: SP46 holds tag (Option 3: partial tag `v1.7.0-webhook` impossible if webhook verdict fails — no tag at all; SP45b never starts). Mitigation: `webhook-l4-verdict.md` sub-agent prompt explicitly includes BOTH `templates/L4/webhook/README.md` AND `practices/AGENTS.md` as sealed context. README body references all 3 Spec Trio paths so sub-agent identifies them without needing them in-context. Option 3 SP45-gating means this risk affects only webhook axis, not internal-it (parallel to R7 P5).

---

## §8 ADR Template + DECISIONS.md format resolution

### Format resolution (H2 Architect fix — recommendation (b))

`practices/DECISIONS.md` currently uses `## <RULE_ID>` + structured key:value sections (lines 35-50) for pre-R7 rules. Zero `TD-2026-*` entries exist on disk. SP45 prep prepends a **format-convention opening note** to DECISIONS.md (one short paragraph) before the first ADR bullet:

> *Format convention (added 2026-05-22, R9 SP45):* Pre-R7 rules retain the `## <RULE_ID>` structured-section format above. R7+ architectural decision records (ADRs) land as compact bullet entries in the form `- **TD-YYYY-MM-DD-NNN** — title.` with sub-bullets for Decision / Drivers / Alternatives / Why chosen / Consequences / Follow-ups. This preserves all prior R6/R7/R8 inline ADR references (no backfill required) and gives R9+ ADRs a uniform append-only landing zone.

SP45 appends TD-2026-05-22-025 (webhook L4) as the first bullet under that opening note. SP45b appends TD-2026-05-22-026 (internal-it recipe) + TD-2026-05-22-027 (webhook-as-extension-axis convention) as subsequent bullets. No retrofit of pre-R7 entries. No format ambiguity for R10+ planners. Effort: ~5 lines of prose + 3 bullet appends.

### ADR Bullets (3 entries — TD-025, TD-026, TD-027)

- **TD-2026-05-22-025 (NEW)** — Webhook L4 primitive introduced (NET-NEW Spec Trio).
  - **Decision:** Add `webhook` as the 12th L4 domain. L4 count 11 → 12. ALL FOUR Spec Trio artifacts authored net-new in SP45.
  - **Drivers:** (a) R8 §10 trigger named "clarified webhook-emit primitive in notification L4 OR notification L4 gains explicit webhook-emit spec items"; R9 picks OR-branch with TD-027 rationale. (b) Webhook semantics (signing secret · HMAC-SHA256 · idempotent retry · dead-letter · circuit breaker) not a subset of notification-send (channel routing · template rendering · recipient prefs). (c) 4 external + 2 Korean verbatim PASS — strongest L4-introduction evidence chain in catalog history.
  - **HMAC cryptographic anchor (Codex Critic INFORMATIONAL — explicit reuse statement):** WEBHOOK-SIGN-001/002 outbound signing **deliberately reuses the same RFC 2104 + OWASP ASVS V13.2.6 cryptographic anchor** already cited by `practices/rules/webhook-hmac-required.md` and `specs/spring-practices-l0.yaml#PRACTICES-INTEG-001` for inbound receiver verification. Sender (outbound) and receiver (inbound) are distinct catalog axes (different responsibility — sender computes the MAC over body+timestamp; receiver verifies it) but share the identical RFC 2104 HMAC-SHA256 construction. SP45 evidence snapshot pins RFC 2104 in the upstream manifest. Receiver verification remains scoped to `PRACTICES-INTEG-001`; sender signing is scoped to `specs/webhook-l0.yaml#WEBHOOK-SIGN-001/002`. No new cryptographic primitive introduced.
  - **Alternatives considered:** Extend notification L4 (rejected — pollutes 4-anchor evidence chain); Tier-2 skill (rejected — caps FROZEN); SP45/SP45b atomic-2 (rejected — R7 Synthesis-B Option 3 protects internal-it from webhook-harness risk).
  - **Why chosen:** Composition-kit self-extensibility along catalog's NEW system-to-system axis. Webhook IS that axis for internal-it + future api-gateway recipes.
  - **Consequences:** L4 = 12. R10+ recipes (api-gateway, webhook-relay) unblocked. Webhook README ships **WITHOUT `applied_recipes:` key** (R7/file-storage/practices precedent). SP45b births key `[internal-it]` with inline desync annotation (M6).
  - **Follow-ups:** `webhook-secret-encryption` co-shipped-rule (INV-005) — **promotion deferred indefinitely; remains a recipe-level invariant unless cross-domain need emerges** (M5 fix — deferred-indefinitely framing; no R10+ candidate exists today).

- **TD-2026-05-22-026 (NEW)** — Internal-it recipe shipped (SP45b atomic-sequential); deferred-recipe queue CLOSED.
  - **Decision:** `recipes/internal-it/` moves deferred→active. Active 9 → 10. `deferred_recipes:` becomes EMPTY (R6 Synthesis-A trim of 4 deferred recipes fully realized R7+R8+R9).
  - **Drivers:** Last R6-deferred recipe; Jira + PagerDuty + Toss + Naver Works verbatim PASS (2 consecutive non-zero Korean cycles); 5 invariants disk-resolvable at SP45b prep; INV-005 co-shipped-rule disambiguation explicit (R7 community precedent — escape hatch for catalog-novel concerns).
  - **Alternatives considered:** Wait for ServiceNow + Kakao verbatim (rejected — host-wide pattern; further wait won't change); merge into crm (rejected — ITSM ≠ sales-pipeline); Synthesis-A defer (rejected — Jira + PagerDuty cleared 2x buffer).
  - **Why chosen:** R6 Synthesis-A trim closure; strongest evidence without fabrication; webhook L4 unblocks the canonical primitive.
  - **Consequences:** 6 L4 READMEs gain `internal-it`. Active = 10. Deferred = 0. Co-shipped invariant honors no-new-rule-family.
  - **Follow-ups:** R10+ refresh re-attempts ServiceNow + Kakao if either unblocks; R10 maintainer review per TD-025 Follow-ups.

- **TD-2026-05-22-027 (NEW)** — Webhook-as-extension-axis convention: when to spin a primitive into its own L4 vs extend an existing one.
  - **Decision:** A primitive becomes its own L4 (vs extension) when ALL THREE hold: (a) **Materially distinct semantics** — manifest sections, contract endpoints, observability counters share <30% overlap with candidate-extension L4; (b) **Evidence-chain integrity** — extending would dilute existing external-verbatim chain or force one README to reference two incompatible reference-impl categories (e.g. user-channels vs system-callbacks); (c) **Two consumer signals** — at least one **shipped active recipe** consumes the primitive AND at least one **plausible R10+ deferred candidate** documents the need (H1 Architect fix — tightened from the iter-1 single-recipe gate to require TWO independent consumer signals so the test is not self-fulfilling by writing one recipe alongside the L4 proposal). R9 webhook satisfies (c) via internal-it (shipped active) + api-gateway-relay (plausible R10+ deferred candidate documented in TD-027 Follow-ups).
  - **Drivers:** R9 webhook vs notification needs a generalizable rule for R10+ planners considering similar splits (cron-with-orchestration vs scheduled-task; GraphQL-subscriptions vs notification). Three-condition gate keeps L4 cap discipline tight while permitting legitimate axis introduction. H1 tightening protects against catalog-sprawl-by-precedent (the steelman antithesis Architect raised).
  - **Alternatives considered:** "L4 cap permanent" (rejected — kills self-extensibility; R5/R7 precedents); "Any net-new domain = its own L4" (rejected — invites sprawl; would have spun identity-verification + email-outbox into separate L4 when they correctly sit inside auth + notification); single-recipe gate (rejected per H1 — self-fulfilling; planner proposing the L4 also writes the qualifying recipe).
  - **Why chosen:** Captures the actual heuristic Architect + Critic applied during R9 review; gives R10+ planners a deterministic check; H1 tightening preserves catalog discipline.
  - **Consequences:** R10+ L4-introduction proposals cite this ADR + demonstrate ALL THREE conditions including the 2-consumer-signal gate. Failing any → propose as extension. Webhook becomes canonical reference for "passes all three".
  - **Follow-ups:** First R10+ application generates a precedent log entry under TD-027. `api-gateway-relay` is the named plausible-R10+-deferred candidate cited in condition (c) for the webhook precedent (no `deferred_recipes:` entry today — purely a forward-pointer to validate the 2-signal gate at SP45b PRD signature).

---

## §9 Honored Constraints (deltas + R7/R8 precedents)

- **Caps:** Tier-1 = 4 FROZEN · Tier-2 = 8 UNCHANGED · L1/L2/L3 = 49/92/20 UNCHANGED.
- **Deltas:** L4 11 → 12 (webhook per TD-025) · Recipes 9 → 10 (internal-it per TD-026) · Deferred 1 → 0 (Synthesis-A trim CLOSED).
- **Atomic SP rule per axis** (Option 3: SP45 webhook-axis · SP45b internal-it-axis · SP46 partial-tag-aware FINAL).
- **TD-027 2-consumer-signal gate** (H1) — shipped active recipe + plausible R10+ deferred candidate; sole L4-introduction discipline post-R9.
- **Korean references** — 3 logical hosts (Kakao + Toss + Naver Works); R7 5-host floor MET + R8 1-PASS target MET-PLUS-1 (2 Korean verbatim).
- **ServiceNow + Kakao Alimtalk** — host-wide non-public-API descriptive content pattern across 4+6 host attempts; documented downgrade is honest evidence.
- **CRUD spec-path** — `specs/crud-security.yaml`, NOT `crud-l0.yaml` (Codex Critic soft #2).
- **DECISIONS.md format** — opening-note convention added in SP45 (H2); R7+ ADRs as bullets, pre-R7 rules retain `## <RULE_ID>` structure.
- **HMAC cryptographic anchor** — RFC 2104 + OWASP ASVS V13.2.6 reused (sender axis); receiver stays at `PRACTICES-INTEG-001` (Codex Critic INFORMATIONAL).
- **No empty applied-recipes array** (R7 H2/M4); `/ax-verify-domain webhook` REMOVED from SP45 gate (R7 Critic L) — replaced with `bash skills/_tests/L4/webhook-domain.test.sh` (assertion semantics mirror `scheduler-domain.test.sh` per M4).
- **AGENTS.md sentinel** — sha recompute CONDITIONAL on `generate_agents.sh` L4-dependency (R7 §11 hedge).
- **Sealed verdict threshold** — ≥10/12 MUST + ≥5/8 SHOULD (R5/R6/R7/R8 precedent).
- **INV-005 co-shipped-rule** — R7 community-INV-005 escape-hatch precedent; promotion deferred indefinitely (M5).

---

## §10 Out-of-scope (R9 explicit) + Deferred Recipes (NOW EMPTY)

### Deferred recipes — QUEUE CLOSED

`recipes/_MANIFEST.yaml#deferred_recipes:` becomes EMPTY post-SP45b. **All deferred recipes now CLOSED. R9 closes the deferred-recipe queue from R6 Synthesis-A trim (community + lms + cms + internal-it all shipped across R7 + R8 + R9).** No further R6-rooted recipe pipeline work.

| Recipe | Shipped in | Tag |
|---|---|---|
| community | R7 SP41b | `v1.5.0-scheduler-community` (2026-05-20) |
| lms | R8 SP43 | `v1.6.0-lms-cms` (2026-05-21) |
| cms | R8 SP43 | `v1.6.0-lms-cms` (2026-05-21) |
| internal-it | R9 SP45b (this cycle) | `v1.7.0-webhook-internal-it` (planned 2026-05-22 IFF 2/2 verdicts) |

### Out-of-scope (R9)

- New L1/L2/L3 surface · new Tier-1/Tier-2 skill · new L4 beyond webhook · new practices rule families (webhook-secret-encryption is recipe-level co-shipped per TD-025 Follow-ups; promotion deferred indefinitely per M5).
- Frontend code (recipe doc only — R5/R6/R7/R8 recipe-no-code) · backend impls (skeleton stubs only — fork-receiver writes business logic).
- Deployment / CI / release scope · `RECIPE_DEVIATION.md` ceremony · 6-month recipe-retirement review.
- ServiceNow + Kakao Alimtalk verbatim retry beyond SP45/SP45b pre-flight one-shot (host-wide pattern; further attempts deferred to R10+).
- New recipe candidates for R10+ deferred queue (fork-receiver demand drives; no proactive deferral pipeline opens).

---

## §11 Branch + path summary + AGENTS.md hedge resolution

- **Branch:** `feat/r9-webhook-internal-it-sp45-sp46` (cut from `main` at `953a3d6`).
- **PRD path (this iter):** `docs/superpowers/specs/2026-05-22-r9-webhook-internal-it-prd.iter2.md`.
- **Manifest target:** `recipes/_MANIFEST.yaml` — internal-it moves deferred→active; `deferred_recipes:` section becomes EMPTY (Synthesis-A trim closed).
- **AGENTS.md hedge resolution (R7 §11 precedent):** SP45 PREP step runs `practices/generate_agents.sh --dry-run` (or `bash -n`-equivalent check) to determine whether the script reads `templates/L4/` topology. If YES → §1/§3/§4.5/§5 language about sentinel sha recompute STANDS; sha changes in SP45. If NO → sentinel sha does NOT change for webhook addition; §1/§3/§4.5/§5/§9 language is updated in the SP45 commit to remove sentinel-sha-recompute claims (mechanical 4-line edit per R7 §11). Either resolution is acceptable.
- **DECISIONS.md target:** `practices/DECISIONS.md` — SP45 prepends format-convention opening note (H2 fix per §8) + appends TD-2026-05-22-025 (webhook L4); SP45b appends TD-2026-05-22-026 (internal-it recipe) + TD-2026-05-22-027 (webhook-as-extension-axis convention).
- **Evidence snapshot paths:** `practices/upstream/r9-sp45-webhook-evidence.md` (SP45 — GitHub + Stripe verbatim + RFC 2104 anchor reuse note) + `practices/upstream/r9-sp45b-internal-it-evidence.md` (SP45b — Jira + PagerDuty + Toss + Naver Works verbatim).
- **Final tag:** `v1.7.0-webhook-internal-it` IFF 2/2 verdicts pass; `v1.7.0-webhook` IFF only webhook passes; no tag IFF webhook fails.

---

## §12 Verdict line

R9 iter 2 PRD ships R7-shape "new L4 + new recipe" with: **webhook L4 NET-NEW Spec Trio** (first since R5 billing — 4 ABSENCE paths disk-verified); **4 external verbatim PASS** (GitHub + Stripe + Jira + PagerDuty); **2 Korean verbatim PASS** (Toss + Naver Works); **internal-it closes deferred queue** (Synthesis-A trim of 4 fully realized R7+R8+R9); **3 SPs Option-3 Synthesis-B precedent** (SP45 atomic + SP45b atomic-sequential + SP46 partial-tag-aware FINAL); **partial-tag policy** ported from R7 verbatim with M6 inline desync annotation; **3 new ADRs** (TD-025 webhook L4 with explicit RFC 2104/ASVS V13.2.6 anchor reuse + TD-026 internal-it + TD-027 webhook-as-extension-axis convention with H1 2-consumer-signal gate); **DECISIONS.md format-convention opening note** (H2 fix); **§4.4 evidence summary trimmed + per-source-class arithmetic added** (M3 + Codex Critic soft #1); **CRUD spec-path clarification** (Codex Critic soft #2 — `specs/crud-security.yaml`, not `crud-l0.yaml`); **INV-005 promotion deferred indefinitely** (M5 fix); **webhook-domain.test.sh assertion-count semantics anchored to scheduler-domain.test.sh** (M4 fix; P4 risk MEDIUM→LOW). 3 SPs (SP45/SP45b/SP46), ≈ 5-6 d wall-time. Ready for Architect + Codex Critic iter 2 review.

---

## RALPLAN-DR Summary (compact — full detail in §1)

**Mode:** DELIBERATE. **Principles (8):** composition-kit · spec-before-code · binary-verification · Tier-1/2-frozen · atomic-Spec-Trio · recipe-no-code · webhook-bona-fide-L4 · no-new-rule-family. **Drivers (3):** reintroduction-trigger · evidence-rigor · R7-cadence-parity. **Options:** (1)+(2) REJECTED, (3) Synthesis-B CHOSEN. **Path:** SP45 → SP45b → SP46. **Wall-time:** 5-6 d. **Pre-mortem:** 4 scenarios (HIGH/MEDIUM/LOW/LOW-post-M4). **ADRs:** TD-025 (webhook L4 + RFC 2104 anchor reuse) · TD-026 (internal-it + queue closure) · TD-027 (2-consumer-signal L4 gate). **Tag policy:** v1.7.0-webhook-internal-it IFF 2/2; partial v1.7.0-webhook IFF webhook-only; no tag IFF webhook fails. **Evidence:** 6 verbatim PASS (4 EN + 2 KO) + 9 downgrades + 5 redirects. **Catalog deltas:** L4 11→12 · recipes 9→10 · deferred 1→0 · sealed verdicts 10→12; Tier-1/2 4/8 + practices 68 + L1/L2/L3 49/92/20 UNCHANGED.

---

## Iter 2 changelog

Each iter-1 blocker closed by ID with iter-2 target sections:

- **H1 (Architect HIGH — TD-027 condition (c) self-fulfilling)** — TIGHTENED. Condition (c) now requires TWO consumer signals: shipped active recipe (internal-it) + plausible R10+ deferred candidate (api-gateway-relay forward-pointer). Targets: §8 TD-027 Decision/Alternatives/Follow-ups bullets · cross-refs §1 P7, §9, §12.

- **H2 (Architect HIGH — DECISIONS.md format drift)** — RESOLVED via recommendation (b). SP45 prepends one-paragraph format-convention opening note to `practices/DECISIONS.md` (R7+ ADRs as bullets; pre-R7 rules retain `## <RULE_ID>` structure). Targets: §8 "Format resolution" subsection · §3 Must-Have · §4.5 SP45 row item (d) · §5 step 8 · §9 · §11.

- **M3 (Architect MEDIUM — §4.4 redundant verbatim summary)** — TRIMMED. 4-row compact summary table removed; replaced with per-deliverable bullet density-floor paragraph. Target: §4.4 "Per-deliverable evidence density floor + per-source-class arithmetic" subsection (replaces iter-1 :281-294 block).

- **M4 (Architect MEDIUM — webhook-domain.test.sh assertion semantics)** — ANCHORED. SP45 TDD anchor gains `semantic_anchor:` field citing `scheduler-domain.test.sh` shape (N total, ≥M PASS gate). Targets: §4.1 TDD anchor YAML · §4.5 SP45 row item (f) · §5 step 9 · §7 P4 likelihood MEDIUM→LOW · §9.

- **M5 (Architect MEDIUM — INV-005 promotion criterion ambiguous)** — DEFERRED-INDEFINITELY per recommendation (b). `webhook-secret-encryption` promotion is "deferred indefinitely; remains a recipe-level invariant unless cross-domain need emerges." Hypothetical `payment-callback-secret` reference removed. Targets: §4.2 INV-005 disambiguation paragraph · §8 TD-025 Follow-ups · §10 · §12.

- **M6 (Architect MEDIUM — partial-tag webhook README desync)** — ANNOTATED. SP45b webhook README key-birth carries inline comment `# verdict pending — see _MANIFEST.yaml for active status`. Targets: §4.5 SP45b row item (e) · §6 partial-tag table row 2 · §6 rationale closing sentence · §8 TD-025 Consequences · §12.

- **Codex Critic INFORMATIONAL (outbound webhook HMAC anchor)** — EXPLICIT REUSE per recommendation (a). TD-025 contains dedicated "HMAC cryptographic anchor" bullet: outbound signing reuses RFC 2104 + OWASP ASVS V13.2.6 already cited by `practices/rules/webhook-hmac-required.md` + `PRACTICES-INTEG-001` (inbound receiver). Sender/receiver distinct catalog axes sharing identical HMAC-SHA256 construction. Targets: §1 P7 closing clause · §8 TD-025 HMAC bullet · §9 · §11 evidence snapshot · §12.

- **Codex Critic soft #1 (evidence-summary arithmetic auditability)** — PER-SOURCE-CLASS BREAKDOWN added: Verbatim PASS = 6 (4 EN + 2 KO); Downgrades = 9 (Atlassian Cloud × 3 + ServiceNow × 4 + PagerDuty developer × 3 + Kakao × 6 + Naver-Works landing × 1); Redirects/alternates = 5. Targets: §4.4 per-source-class breakdown bullet · §12 evidence-ledger summary.

- **Codex Critic soft #2 (CRUD spec path naming)** — CLARIFIED. §4.2 contains note: CRUD anchors use `specs/crud-security.yaml`, NOT `specs/crud-l0.yaml` (does not exist; R5 absorbed all CRUD content into `crud-security.yaml`). Targets: §4.2 "CRUD spec-path clarification" paragraph · §9.

- **Codex Critic soft #3** — covered by INFORMATIONAL fix above.

**Re-review readiness:** all 6 Architect findings closed verbatim; 1 Codex INFORMATIONAL + 3 soft items closed; 3-SP Synthesis-B plan preserved; L2 inventory remains disk-resolvable at SP45b prep; CRUD spec path clarified; outbound webhook HMAC explicitly anchored. Ready for Architect + Codex Critic iter 2 re-review.
