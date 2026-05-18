# L4 Composition — api-gateway-relay

> Which L4 domains to enable and how they wire together. api-gateway-relay is a
> **gateway-pattern COMPOSER** over existing 12 L4 primitives — NOT itself a
> primitive. See `RECIPE.md` preamble.

## Domain Wiring

```
auth (gateway-level access control — request entry filter)
 └── ASVS V4.1.1 — token validity + scope check BEFORE relay forward
      ↓ (auth failure → reject + audit-log denial reason; INV-002)
      ↓
[CROSS-CUTTING] specs/ratelimit-l0.yaml#RATELIMIT-1/2/3/4
 └── Per-route rate-limit check; over-limit → HTTP 429 with Retry-After header
      (RFC 6585 §4 — cross-cutting enforced inside auth filter; INV-003)
      ↓ (no L4 directory; spec-only binding — R10 framing per TD-2026-05-23-029)
      ↓
crud (endpoint registry + route config)
 └── Routes: URL + method + target + signing-secret + active flag
      ↓ CRUD-VAL-1 validation on create / update (INV-005)
      ↓ AUDIT-RECORD-002 immutable before/after diff on mutations
      ↓ idempotency-key required on route mutations (rule_ref:
        practices/rules/idempotency-key-on-mutations.md)
      ↓
audit-log (every relayed request + every route config mutation)
 └── @Audited("api_gateway_relay.request.relayed") on inbound relay
      records operator + relay_target_endpoint + delivery_id (INV-001)
      ↓
webhook (outbound signed relay to upstream backend services)
 └── WebhookDispatcher.emit("api_gateway_relay.<route>.forward", body) →
      per-route HMAC-SHA256 over <timestamp>.<body> (WEBHOOK-SIGN-001/002 —
      RFC 2104 + OWASP ASVS V13.2.6 anchor shared with the inbound
      practices/rules/webhook-hmac-required.md receiver rule) →
      exponential backoff 30s × 2 up to 5 attempts (WEBHOOK-RETRY-001) →
      X-Webhook-Delivery-Id stable across retries (WEBHOOK-RETRY-002) →
      FAILED_PERMANENT terminal status + admin replay (WEBHOOK-DEAD-LETTER-001/002) →
      circuit-breaker auto-open on 90% rolling 50-attempt failure rate
      (WEBHOOK-CIRCUIT-001; INV-004)
      ↓
scheduled-task (circuit-breaker reconciliation + dead-letter replay scheduling)
 └── CircuitBreakerReconcileTask — distributed lock (SCHED-LOCK-001) +
      half-open → closed transition probe; idempotent per-endpoint key
      (INV-004; no double-reconcile on multi-node)
```

## Domain Configuration Notes

### `auth` (gateway-level access control)
- Token validity + scope check BEFORE relay forward (ASVS V4.1.1) — implemented
  in a Spring Security `OncePerRequestFilter` placed BEFORE the relay
  controller.
- On auth failure: short-circuit response WITH audit-logged denial reason
  (INV-002 INV-005 binding to AUDIT-RECORD-002).
- Reference: `templates/L4/auth/`

### `crud` (endpoint registry + route config)
- Three CRUD entities: `RouteRegistration`, `UpstreamEndpoint`, `RoutePolicy`.
- `RouteRegistration` columns: `id` / `url_pattern` / `method` /
  `upstream_target` / `signing_secret_id` / `active` / `created_at` / `updated_at`.
- Idempotency key required on route mutations per
  `practices/rules/idempotency-key-on-mutations.md` (INV-005 — stable
  `X-Idempotency-Key` on `POST /api/admin/routes`).
- CRUD-VAL-1 validation on create / update: URL pattern syntax, method in {GET,
  POST, PUT, PATCH, DELETE}, target URI scheme HTTPS only.
- Reference: `templates/L4/crud/`

### `audit-log` (every relayed request + every route config mutation)
- Annotate `RelayService.forward()` with
  `@Audited(action = "api_gateway_relay.request.relayed")`. Records:
  `operator_id`, `target_id` (route UUID), `relay_target_endpoint`,
  `delivery_id`, `at` (AUDIT-RECORD-001; INV-001).
- Annotate `RouteRegistrationService.update()` /  `.create()` with
  `@Audited(action = "api_gateway_relay.route.config.changed")` carrying
  before/after diff (AUDIT-RECORD-002 immutability; INV-005).
- Retention ≥ 90 days per `specs/audit-log-l0.yaml#AUDIT-RETENTION-001`.
- Reference: `templates/L4/audit-log/`

### `scheduled-task` (circuit-breaker reconciliation)
- Register `CircuitBreakerReconcileTask` with cron (e.g.
  `0 */5 * * * *` every 5 minutes).
- `LockingPolicy.tryAcquire("api-gateway-relay-circuit-reconcile", node-id)`
  before each tick (SCHED-LOCK-001 — multi-node-safe; INV-004).
- JobHistory row appended per run (SCHED-EXECUTE-001).
- Probe each OPEN endpoint with a low-cost GET; on success, transition
  half-open → closed; emit audit-log row on either transition.
- Reference: `templates/L4/scheduled-task/`

### `webhook` (outbound signed relay)
- **2nd CONSUMER of the webhook L4 primitive shipped in R9 SP45** (NET-NEW
  Spec Trio per TD-2026-05-22-025; 1st consumer was R9 SP45b internal-it).
- Per-route HMAC-SHA256 over `<X-Webhook-Timestamp>.<body>` per WEBHOOK-SIGN-001
  + WEBHOOK-SIGN-002. The cryptographic anchor (RFC 2104 + OWASP ASVS V13.2.6)
  is the SAME anchor reused by `practices/rules/webhook-hmac-required.md`
  for the inbound axis — sender + receiver are distinct catalog axes sharing
  identical construction.
- Retry shape: 30s × 2 up to 5 attempts (WEBHOOK-RETRY-001); stable
  `X-Webhook-Delivery-Id` across retries (WEBHOOK-RETRY-002).
- Dead-letter: terminal `FAILED_PERMANENT` row retained for admin inspection
  (WEBHOOK-DEAD-LETTER-001); admin replay creates a fresh delivery_id chain
  (WEBHOOK-DEAD-LETTER-002).
- Circuit-breaker: 90% rolling failure rate over 50 attempts auto-deactivates
  the endpoint and emits an audit-log row (WEBHOOK-CIRCUIT-001; INV-004).
- Reference: `templates/L4/webhook/`

### Cross-cutting `specs/ratelimit-l0.yaml` binding
- 4 spec items `RATELIMIT-1..4` enforced INSIDE existing L4 boundaries (auth
  filter + webhook dispatcher + crud route handler) WITHOUT introducing a new
  L4 directory.
- INV-003 binds `RATELIMIT-1` (HTTP 429 rejection) + `RATELIMIT-2` (Retry-After
  header per RFC 6585 §4); RATELIMIT-3 (client-key isolation) + RATELIMIT-4
  (window reset) are documented but not invariant-anchored this cycle.
- Implementation hint: `RateLimitFilter` placed AFTER auth filter (so the
  client-key is resolved first), per-route Caffeine LoadingCache keyed by
  `<route_id, client_id>`; over-limit returns 429 + `Retry-After: <window>`.
- **Cross-cutting NOT a new L4** — see `RECIPE.md` "Cross-cutting binding"
  section + `templates/DECISIONS.md` TD-2026-05-23-029 no-L4-split discipline.

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - api-gateway-relay
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6
dual-form guard.)

The 5 affected L4 READMEs (alphabetical: `audit-log`, `auth`, `crud`,
`scheduled-task`, `webhook`) carry `api-gateway-relay` in their
`applied_recipes:` plural lists.

- 4 READMEs (`audit-log`, `auth`, `crud`, `scheduled-task`) receive an
  **alphabetical insertion** of `api-gateway-relay` at the head of their
  existing plural lists (the new alphabetically-smallest entry).
- 1 README (`webhook`) carries the **first 2-element plural-list insertion** —
  the existing single-entry list `[internal-it]` (R9 SP45b first-consumer-
  arrival key birth) becomes `[api-gateway-relay, internal-it]` (alphabetical
  insertion BEFORE `internal-it`). R6 dual-form regex + alphabetical-append
  proven shape — no new fixture needed (TD-2026-05-23-028 Consequences).
- 2 optional READMEs (`notification`, `feature-flags`) ALSO receive
  alphabetical insertion of `api-gateway-relay` because the recipe documents
  them as `override_allowed: add:` opt-in (gateway alerting + per-route
  feature-flag gating). The R6 dual-form guard accepts optional-L4 plural-list
  membership when the recipe's `override_allowed:` block names them.
- `file-storage` + `practices` L4 READMEs remain `applied_recipes:`-key-less
  (no api-gateway-relay consumer arrives; H2/M4 unused-L4 precedent
  preserved).
