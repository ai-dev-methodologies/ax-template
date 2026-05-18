---
recipe: webhook-l4
verdict_version: "1"
recorded_at: "2026-05-22"
agent_context: "context-0 — given only templates/L4/webhook/README.md + practices/AGENTS.md"
result:
  must_score: null
  must_total: 12
  should_score: null
  should_total: 8
  verdict: PENDING
  threshold: "≥10/12 MUST + ≥5/8 SHOULD"
---

# Sealed Verdict — webhook L4 (SP46 will execute)

## Sealed Context (sub-agent input)

The sub-agent will receive **only** these two files at spawn time:

1. `templates/L4/webhook/README.md`
2. `practices/AGENTS.md`

No other codebase context. No `specs/webhook-l0.yaml` body, no contract, no
manifest. The README must be self-describing enough that a context-0 sub-agent
can re-derive the webhook L4 primitive's shape from it alone (cross-referencing
catalog rules from `practices/AGENTS.md` only).

## Sub-Agent Prompt (SP46 will execute)

```
You are given two files:
  1. templates/L4/webhook/README.md — the webhook L4 reference workload
  2. practices/AGENTS.md — the ax-template practices catalog

Using ONLY these two files, describe the webhook L4 primitive. Your answer
must cover:

a) The domain mode (full_trio | backend_only | frontend_only)
b) The 3 Spec Trio file paths anchoring this domain
c) At least 3 of the 6 spec families this primitive defines
d) The outbound signing algorithm + the cryptographic anchor (RFC + ASVS clause)
   reused from the inbound webhook rule
e) The retry policy shape (initial delay, multiplier, max attempts)
f) Why the README has no applied_recipes: key today
g) At least one external service or framework that documents this primitive

Do not use any information outside the two provided files. If you cannot
answer a sub-question from those two files, say "not derivable from sealed
context".
```

## MUST Rubric (12 items — to be executed by SP46)

| # | Criterion | Expected Source in README | Pass? |
|---|-----------|---------------------------|-------|
| M1 | Names domain mode `backend_only` | §"Domain Mode" header states `backend_only` explicitly | PENDING |
| M2 | Lists `specs/webhook-l0.yaml` as the backend Spec | §"Spec Trio anchors" + §"Spec Trio (backend_only)" table | PENDING |
| M3 | Lists `contracts/webhook-openapi.yaml` as the contract | §"Spec Trio" anchors block | PENDING |
| M4 | Lists `blueprints/webhook-manifest.yaml` as the policy manifest | §"Spec Trio" anchors block | PENDING |
| M5 | Names ≥3 of EMIT / SIGN / RETRY / DEAD-LETTER / CIRCUIT-BREAKER / IDEMPOTENCY families | §"Compliance items" table names all 6 | PENDING |
| M6 | Identifies HMAC-SHA256 as the outbound signing algorithm | §"How to fork" step 2 + §"Compliance items" WEBHOOK-SIGN-001 row | PENDING |
| M7 | Identifies the cryptographic anchor reuse (RFC 2104 / OWASP ASVS V13.2.6 — receiver rule shares anchor) | §"How to fork" step 2 paragraph + §"Compliance items" SIGN-001 row notes anchor reuse | PENDING |
| M8 | Identifies retry policy shape (30s base × 2 multiplier, max 5 attempts) | §"Compliance items" WEBHOOK-RETRY-001 + §"Configuration knobs" | PENDING |
| M9 | Identifies dead-letter terminal status `FAILED_PERMANENT` with admin replay | §"Compliance items" WEBHOOK-DEAD-LETTER-001/002 rows | PENDING |
| M10 | Explains why the README carries no `applied_recipes:` key today | §"Composition" explicit (file-storage + practices + scheduler-pre-R8 precedent named) | PENDING |
| M11 | Names GitHub OR Stripe webhooks as the underlying external primitive | §"External evidence" verbatim block names both | PENDING |
| M12 | Identifies stable `X-Webhook-Delivery-Id` across retry attempts | §"Compliance items" WEBHOOK-RETRY-002 row | PENDING |

## SHOULD Rubric (8 items — to be executed by SP46)

| # | Criterion | Expected Source in README | Pass? |
|---|-----------|---------------------------|-------|
| S1 | Names `X-Webhook-Signature: sha256=<hex>` header format | §"Compliance items" WEBHOOK-SIGN-001 + §"Domain-specific spec requirements" | PENDING |
| S2 | Names `X-Webhook-Timestamp` header + signed input shape `timestamp + "." + body` | §"How to fork" step 2 snippet + WEBHOOK-SIGN-002 | PENDING |
| S3 | Names the per-endpoint signing-secret storage as encrypted-at-rest (KMS-managed) | §"How to fork" + composition (recipe-level invariant cross-ref) | PENDING |
| S4 | Names the 90% failure rate over rolling 50 attempts as the circuit-breaker threshold | §"Domain-specific spec requirements" WEBHOOK-CIRCUIT-001 row | PENDING |
| S5 | Names ≥3 configuration knobs (`ax.webhook.retry.*`, `ax.webhook.dead-letter.*`, `ax.webhook.circuit.*`) | §"Configuration knobs" block | PENDING |
| S6 | Identifies SP45b internal-it as the first downstream consumer | §"Composition" + §"Backend templates" paragraph | PENDING |
| S7 | Identifies admin replay endpoint (`POST /webhook-deliveries/{id}/replay`) preserves the original FAILED_PERMANENT row | §"Compliance items" WEBHOOK-DEAD-LETTER-002 row | PENDING |
| S8 | Identifies receiver-side contract obligation (treat repeated `X-Webhook-Delivery-Id` as same event) | §"Compliance items" WEBHOOK-IDEMPOTENT-001 row | PENDING |

## Verdict

```
MUST:   PENDING / 12  (threshold: ≥10)
SHOULD: PENDING / 8   (threshold: ≥5)
VERDICT: PENDING (SP46 will execute the sealed sub-agent simulation and update this row)
```

**§7 Pre-Mortem 4 mitigation honored:** README explicitly references all 3 Spec
Trio paths (spec + contract + manifest) so the sub-agent identifies them
without needing them in sealed context. README §"Compliance items" table names
all 6 spec families inline so the sub-agent can list ≥3 without depending on
external lookup. README §"External evidence" carries 2 verbatim quotes (GitHub
+ Stripe) anchoring M11. README §"Composition" carries the file-storage +
practices + scheduler-pre-R8 precedent inline so M10 is answerable. README
§"How to fork" step 2 explicitly references RFC 2104 + OWASP ASVS V13.2.6
anchor reuse so M7 is answerable.

**Evidence density note:** 2 external English verbatim anchors (GitHub
Webhooks + Stripe Webhooks). 1 anchor (Stripe) is post-redirect from the
legacy host `https://stripe.com/docs/webhooks → https://docs.stripe.com/webhooks`
(PRD §4.4 evidence ledger row).
