---
recipe: api-gateway-relay
verdict_version: "1"
recorded_at: "2026-05-23"
agent_context: "context-0 — given only recipes/api-gateway-relay/RECIPE.md + practices/AGENTS.md"
result:
  must_score: 0
  must_total: 12
  should_score: 0
  should_total: 8
  verdict: PENDING
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — api-gateway-relay recipe (SP48 to execute)

## Sealed Context (sub-agent input)

The sub-agent will receive **only** these two files at spawn time:

1. `recipes/api-gateway-relay/RECIPE.md`
2. `practices/AGENTS.md`

No other codebase context. No `specs/recipes/api-gateway-relay-recipe-l0.yaml`
body, no L4-composition, no L2-block-recipe. The RECIPE.md must be
self-describing enough that a context-0 sub-agent can re-derive the
api-gateway-relay recipe's composition + 5 invariants from it alone
(cross-referencing catalog rules from `practices/AGENTS.md` only).

## Sub-Agent Prompt (SP48 will execute)

```
You are given two files:
  1. recipes/api-gateway-relay/RECIPE.md — the api-gateway-relay recipe manifest
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, describe the api-gateway-relay recipe. Your answer
must cover:

a) The enabled L4 domains (alphabetical) wired by the recipe
b) The 5 business invariants and what each binds to (spec_ref / rule_ref)
c) Why INV-003 uses a cross-cutting specs/ratelimit-l0.yaml binding rather
   than spinning ratelimit as a new L4 (TD-2026-05-23-029 framing)
d) Why INV-005 uses (spec_ref + rule_ref) rather than the R7 community / R9
   internal-it co-shipped-rule escape hatch (deliberate framing)
e) The "GATEWAY-PATTERN COMPOSER, NOT itself a primitive" disambiguation
   sentence (preamble — M3 anchor)
f) The 2nd-consumer relationship with the webhook L4 (R9 SP45 → R10 SP47
   retroactive 2-consumer-signal gate validation)
g) At least one English API-gateway product documented as evidence (5
   available: Kong / AWS / Cloudflare API Shield / Tyk / Apigee)
h) At least one Korean platform documented as evidence (Toss adjacent / NAVER
   Cloud Platform fresh-vendor)
i) The override_allowed scenarios (notification add / feature-flags add /
   scheduled-task skip / notification skip / feature-flags skip)

Do not use any information outside the two provided files. If you cannot
answer a sub-question from those two files, say "not derivable from sealed
context".
```

## Sub-Agent Derived Answer (context-0 simulation — SP48 to execute)

PENDING SP48 EXECUTION.

## MUST Rubric (12 items — SP48 to execute)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| M1 | Lists all 5 enabled L4 domains (audit-log, auth, crud, scheduled-task, webhook) | _pending SP48_ | ⏳ |
| M2 | Identifies api-gateway-relay as the 2nd shipped consumer of webhook L4 (R9 internal-it = 1st; R10 api-gateway-relay = 2nd) | _pending SP48_ | ⏳ |
| M3 | Names INV-001 webhook HMAC-SHA256 binding + audit-log binding for outbound relay | _pending SP48_ | ⏳ |
| M4 | Names INV-002 ASVS V4.1.1 gateway-level access control binding | _pending SP48_ | ⏳ |
| M5 | Names INV-003 cross-cutting specs/ratelimit-l0.yaml#RATELIMIT-1/2 binding | _pending SP48_ | ⏳ |
| M6 | Names INV-004 WEBHOOK-CIRCUIT-001 + SCHED-LOCK-001 binding | _pending SP48_ | ⏳ |
| M7 | Names INV-005 (spec_ref + rule_ref): CRUD-VAL-1 + AUDIT-RECORD-002 + idempotency-key-on-mutations.md | _pending SP48_ | ⏳ |
| M8 | Quotes (or paraphrases verbatim) the "GATEWAY-PATTERN COMPOSER, NOT itself a primitive" disambiguation preamble | _pending SP48_ | ⏳ |
| M9 | Identifies api-gateway-relay as the 2nd shipped consumer validating TD-2026-05-22-027 (c) two-consumer-signal gate retroactively | _pending SP48_ | ⏳ |
| M10 | Names ≥1 English API-gateway product as evidence (Kong / AWS / Cloudflare / Tyk / Apigee) | _pending SP48_ | ⏳ |
| M11 | Names ≥1 Korean platform as evidence (Toss adjacent / NAVER Cloud Platform fresh-vendor) | _pending SP48_ | ⏳ |
| M12 | Identifies the override_allowed scenarios (notification / feature-flags / scheduled-task) | _pending SP48_ | ⏳ |

**MUST: pending / 12**

## SHOULD Rubric (8 items — SP48 to execute)

| # | Criterion | Agent Answer | Pass? |
|---|-----------|-------------|-------|
| S1 | Names the cross-cutting framing: ratelimit-l0.yaml exists WITHOUT templates/L4/ratelimit/ directory (R10-novel) | _pending SP48_ | ⏳ |
| S2 | Names ≥3 API-gateway products as composition references | _pending SP48_ | ⏳ |
| S3 | Names the RFC 2104 + OWASP ASVS V13.2.6 HMAC anchor reuse with the receiver rule webhook-hmac-required.md | _pending SP48_ | ⏳ |
| S4 | Names TD-2026-05-23-029 no-L4-split discipline (R10 ships RECIPE ONLY) | _pending SP48_ | ⏳ |
| S5 | Names the Korean vendor rotation precedent (R9 Toss → R10 NAVER Cloud Platform fresh-vendor) | _pending SP48_ | ⏳ |
| S6 | Identifies SP45 as the cycle that shipped the webhook L4 NET-NEW Spec Trio | _pending SP48_ | ⏳ |
| S7 | Names TD-2026-05-23-028 (api-gateway-relay recipe + Korean vendor rotation Follow-up) | _pending SP48_ | ⏳ |
| S8 | Identifies the partial-tag policy: degenerate at n=1 binary (PASS → tag; FAIL → clean revert; queue stays []) | _pending SP48_ | ⏳ |

**SHOULD: pending / 8**

## Verdict

```
MUST:   pending / 12  (threshold: ≥10)
SHOULD: pending /  8  (threshold: ≥5)
VERDICT: PENDING — SP48 to execute
```

Per PRD §6 partial-tag table (n=1 binary):

- **1/1 PASS** → tag `v1.8.0-api-gateway-relay`; api-gateway-relay `active` in
  `_MANIFEST.yaml` (11 active, 0 deferred — `deferred_recipes: []` UNCHANGED);
  RECIPE.md `status: active`; webhook L4 `applied_recipes: [api-gateway-relay, internal-it]`.
- **0/1 FAIL** → no tag; **SP47 reverted CLEAN; api-gateway-relay ABSENT from
  BOTH active AND `deferred_recipes:` (queue stays `[]`)** per Codex L Option
  (a). NO deferred-queue addition under any outcome. R11+ reintroduction
  requires fresh evidence chain + explicit trigger.
