# L4 / webhook — Outbound Webhook Emission Domain

**ax-template R9 SP45** — Webhook L4 reference workload (catalog-only L4 primitive
introduction; Spec Trio NET-NEW at SP45 — first genuinely net-new L4 since
billing R5). 12th L4 domain.

## Domain Mode

`backend_only` — outbound system-to-system event delivery with HMAC-SHA256
signing, idempotent retry, dead-letter queue, and per-endpoint circuit breaker;
no frontend UI in scope. SP45 introduces the L4 README + scaffold. SP45b
internal-it recipe will be the **first downstream consumer** of this primitive.

## Overview

A backend-only outbound-webhook domain: register endpoints (URL + event filter +
per-endpoint signing secret), sign every emit with HMAC-SHA256 over
`<timestamp>.<body>`, retry with exponential backoff (30s × 2 up to 5 attempts),
move terminal failures to a dead-letter row for admin inspection / replay, and
automatically open a circuit breaker per endpoint whose rolling 50-attempt
failure rate hits 90%.

Spec Trio anchors:
- `specs/webhook-l0.yaml` (10 backend items: EMIT + SIGN + RETRY + DEAD-LETTER + CIRCUIT-BREAKER + IDEMPOTENCY families)
- `contracts/webhook-openapi.yaml`
- `blueprints/webhook-manifest.yaml`

## Spec Trio (backend_only)

| File | Purpose |
|------|---------|
| `specs/webhook-l0.yaml` | 10 compliance items across EMIT / SIGN / RETRY / DEAD-LETTER / CIRCUIT-BREAKER / IDEMPOTENCY families |
| `contracts/webhook-openapi.yaml` | OpenAPI 3.0 contract for admin endpoints (register / list / delete / delivery list / replay) |
| `blueprints/webhook-manifest.yaml` | Policy manifest (emit · sign · retry · dead-letter · circuit-breaker · idempotency sections) |

## Compliance items (spec_ref summary)

| Spec ID | Chapter | Requirement (excerpt) |
|---|---|---|
| `WEBHOOK-EMIT-001` | EMIT | `register()` is an idempotent upsert keyed by URL |
| `WEBHOOK-EMIT-002` | EMIT | `emit()` POSTs JSON to every active endpoint whose filter matches |
| `WEBHOOK-SIGN-001` | SIGN | Outbound `X-Webhook-Signature: sha256=<hex(HMAC-SHA256(secret, body))>` |
| `WEBHOOK-SIGN-002` | SIGN | `X-Webhook-Timestamp` header; signature covers timestamp + body (replay mitigation) |
| `WEBHOOK-RETRY-001` | RETRY | Exponential backoff 30s × 2 up to 5 attempts on failure |
| `WEBHOOK-RETRY-002` | RETRY | `X-Webhook-Delivery-Id` stable across all retries for same event |
| `WEBHOOK-DEAD-LETTER-001` | DEAD-LETTER | Exhausted deliveries flip to `FAILED_PERMANENT` for admin inspection |
| `WEBHOOK-DEAD-LETTER-002` | DEAD-LETTER | Admin replay creates a fresh `delivery_id` chain |
| `WEBHOOK-CIRCUIT-001` | CIRCUIT-BREAKER | Per-endpoint 90% failure rate over rolling 50 attempts auto-opens circuit |
| `WEBHOOK-IDEMPOTENT-001` | IDEMPOTENCY | Receivers MUST treat repeated `X-Webhook-Delivery-Id` as same event (sender-side contract documentation) |

(All 10 items in `specs/webhook-l0.yaml`; see the file for full notes + tests.)

## How to fork this template

1. **Copy the backend skeleton** (or roll your own entities):
   ```bash
   cp -r templates/L4/webhook/backend/* backend/src/main/java/com/<org>/webhook/
   ```
   The shipped `WebhookEndpoint.java.skeleton` + `WebhookDelivery.java.skeleton`
   are minimal JPA entity stubs — rename to `.java`, set the package, and wire
   them into your Spring Boot module.

2. **HMAC-SHA256 signing primitive** — implement a small `HmacSigner` with
   `Mac.getInstance("HmacSHA256")`:
   ```java
   String signed = "sha256=" + HexFormat.of().formatHex(
       mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
   ```
   The cryptographic anchor (RFC 2104 HMAC-SHA256 + OWASP ASVS V13.2.6) is the
   SAME anchor reused by the **inbound** verification rule
   `practices/rules/webhook-hmac-required.md` (sender / receiver are distinct
   catalog axes sharing the identical construction — see TD-2026-05-22-025
   HMAC anchor reuse bullet in `templates/DECISIONS.md`).

3. **Pick a retry mechanism** (any pattern below is spec-compliant):
   - **DB-row polling** — simplest; a `@Scheduled(fixedDelay=10_000)` worker
     selects `WHERE status='PENDING_RETRY' AND next_attempt_at <= now()`.
     Recommended for ≤ 5 nodes (combine with the `scheduled-task` lock primitive
     if multi-node).
   - **Resilience4j Retry + RetryAsync** — battle-tested library, more declarative.
     See `blueprints/webhook-manifest.yaml#retry` for the exact 30s × 2 backoff policy.

4. **Wire delivery history** — every emit allocates one `WebhookDelivery` row at
   enqueue time with a freshly minted UUID (`WEBHOOK-RETRY-002`); the same row
   is mutated across retry attempts; terminal transitions update
   `status` + `last_response_code` + `last_attempt_at`
   (`WEBHOOK-DEAD-LETTER-001`).

5. **Circuit-breaker hook** — after every terminal transition (success OR
   permanent failure), call `CircuitBreakerPolicy.evaluate(endpointId)`. The
   policy runs a window query (last 50 deliveries for the endpoint) and flips
   `active=false` plus emits an audit-log row when the failure rate hits 90%.

6. **Configuration knobs**:
   ```properties
   ax.webhook.retry.initial-delay-seconds=30
   ax.webhook.retry.max-attempts=5
   ax.webhook.retry.multiplier=2.0
   ax.webhook.dead-letter.retention-days=30
   ax.webhook.circuit.window-size=50
   ax.webhook.circuit.failure-threshold=0.90
   ```

## Domain-specific spec requirements

| Spec ID | Requirement | Implementation hint |
|---|---|---|
| WEBHOOK-EMIT-001 | Idempotent upsert by URL | `WebhookEndpointService.register()` selects then INSERTs-or-UPDATEs |
| WEBHOOK-EMIT-002 | Fan-out to matching active endpoints | `WebhookDispatcher.emit(type, body)` iterates filtered endpoints |
| WEBHOOK-SIGN-001 | `X-Webhook-Signature: sha256=<hex>` | `HmacSigner.sign(secret, payload)` using `Mac.getInstance("HmacSHA256")` |
| WEBHOOK-SIGN-002 | `X-Webhook-Timestamp` + signature covers timestamp | Compose `timestamp + "." + body` as MAC input (Stripe-style) |
| WEBHOOK-RETRY-001 | 30s × 2 backoff, 5 attempts | `RetryPolicy.nextAttemptAt(attemptCount)` |
| WEBHOOK-RETRY-002 | Stable `delivery_id` | `WebhookDelivery.id` minted at enqueue; never re-generated for the same event |
| WEBHOOK-DEAD-LETTER-001 | `FAILED_PERMANENT` terminal status | Set on the row when `attempt_count == max_attempts` |
| WEBHOOK-DEAD-LETTER-002 | Admin replay → fresh `delivery_id` | New row with `attempt_count=1`; original row preserved |
| WEBHOOK-CIRCUIT-001 | 90% failure rate over 50 → open | `CircuitBreakerPolicy.evaluate()` after every terminal transition |
| WEBHOOK-IDEMPOTENT-001 | Receiver contract documented | This README section; receiver code lives in fork-receiver repos |

## Composition

Webhook is introduced standalone in **R9 SP45**. SP45b internal-it recipe is the
**first downstream consumer**; it will birth the `applied_recipes:` key on this
README per the first-consumer-arrival convention codified in TD-2026-05-21-024
(R8 SP43) — same precedent the `scheduled-task` L4 README relied on through R7.
Until SP45b commits, this README intentionally carries **no** `applied_recipes:`
key (matches `file-storage` and `practices` L4 precedent for catalog rows not
yet consumed by an active recipe).

## External evidence (verbatim, fetched 2026-05-22)

Two verbatim external quotes anchor this L4 domain — see
`practices/upstream/r9-sp45-webhook-evidence.md` for the full snapshot:

- **GitHub Webhooks** (`https://docs.github.com/en/webhooks`):
  > "Webhooks provide a way for notifications to be delivered to an external
  > web server whenever certain events occur on GitHub."

- **Stripe Webhooks** (`https://docs.stripe.com/webhooks`):
  > "After you register a webhook endpoint, Stripe can push real-time event
  > data to your application's webhook endpoint when events happen in your
  > Stripe account."
  >
  > "Stripe attempts to deliver events to your destination for up to three days
  > with an exponential back off in live mode."
  >
  > "Stripe signs every webhook event by including a signature in the
  > Stripe-Signature header."

Both quotes attest that outbound webhook delivery (sign + retry + emit) is a
first-class, externally-documented primitive — the same shape this L4 catalog
row encodes.

## Verification

```bash
# L4 catalog-discoverability gate (sealed sub-agent test)
bash skills/_tests/L4/webhook-domain.test.sh

# Full guard suite (22+ guards, including trio_integrity)
bash practices/evals/run-all-guards.sh

# Domain-specific trio integrity (when backend lands)
bash practices/evals/trio_integrity_guard.sh --domain webhook
```

## Backend templates (skeleton)

`templates/L4/webhook/backend/` ships two stubs at SP45:

- `WebhookEndpoint.java.skeleton` — minimal JPA entity for the registered
  endpoint (id / url / active / signing_secret / event_filter / created_at).
- `WebhookDelivery.java.skeleton` — minimal JPA entity for a single delivery
  attempt-chain (id stable across retries / status / attempt_count /
  next_attempt_at / last_response_code).

A fuller skeleton (HmacSigner, RetryPolicy, CircuitBreakerPolicy, Service,
Controller) is deferred to a future webhook backend-expansion cycle, triggered
by fork-receiver demand or recipe needs (independent of any specific recipe).
SP45b internal-it recipe consumes these stubs for INV-003 (signed + retried
outbound to ITSM systems).
