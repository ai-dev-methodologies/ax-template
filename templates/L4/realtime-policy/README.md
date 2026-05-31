# L4 / realtime-policy — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). Channel/topic names are tenant-scoped: the reference workload resolves every authenticated caller to one shared tenant scope, and a subscribe whose scope segment does not match is rejected 403 (RT-CHANNEL-AUTH-002). A multi-tenant fork resolves the scope from `TenantContext` via the existing tenancy runtime + the `realtime_connection_tenant_scope_guard` — this domain composes with that contract by reference and does NOT add a tenant guard.

> **⚠️ MULTI-TENANT FORK — YOU MUST REPLACE THE STATIC SCOPE.**
> `RealtimeController.RESOLVED_TENANT_SCOPE = "default"` is a **single-tenant placeholder**.
> It is correct for the reference workload (a single shared scope is what lets multiple
> callers fan out on one topic; a per-user scope would break fan-out), but a fork that
> enables `tenant_model: multi` **without replacing it** gets ONE shared registry key
> across **all tenants** — a cross-tenant data leak. **Replace this static scope with
> `TenantContext` extraction** (multi-tenant-l0 `MULTI-TENANT-PROPAGATION-001`, via the
> existing tenancy runtime + the `realtime_connection_tenant_scope_guard`) so each tenant
> gets a **distinct registry key**. The TenantContext runtime is a multi-tenant follow-up;
> it is not wired into this single-tenant reference.

**Status**: backend-only (promoted future_add → selectable, recipe_orphan). The Spring realtime runtime ships in this commit (`backend/src/main/java/com/ax/template/authblueprint/realtime/`); verified by `./gradlew testRealtime` (8 tests GREEN — 6 spec items + 2 hardening regressions: unknown-topic-404 cardinality bound + disconnect-recorded-once race). This is a cross-cutting policy domain — no entity, no state machine, no user-facing UI — so it follows the `i18n-policy` / `multi-tenant` backend-only L4 convention, not the entity-domain (CRUD) convention.

## Domain summary

Server-pushed realtime channel policy for fork-receivers that need live UX (notification dashboards, activity-feed live tail, multi-user collaboration). The spec ([`specs/realtime-policy-l0.yaml`](../../../specs/realtime-policy-l0.yaml)) defines the contract; the runtime here defines the canonical Spring wiring, realized **SSE-FIRST**.

Seven items / five families (six mechanically tested; RT-PROTOCOL-001 is review-only):

- `RT-CHANNEL-AUTH-001` — subscribe goes through the SAME auth pipeline as HTTP. SSE subscribe is a plain chunked HTTP `GET`, so the existing Spring Security chain authenticates it; an unauthenticated subscribe is rejected 401 BEFORE the controller. There is no "WebSocket bypass" because SSE performs no protocol switch.
- `RT-CHANNEL-AUTH-002` — topic names are tenant-scoped; a cross-scope subscribe → 403. Composes with multi-tenant-l0 `MULTI-TENANT-PROPAGATION-001` (not reimplemented).
- `RT-FANOUT-001` — audience resolved BEFORE fan-out; a subscriber outside the audience never receives the frame at the transport layer (mirrors `ActivityService.publish`).
- `RT-BACKPRESSURE-001` — bounded per-subscriber write queue (manifest `queueThreshold`); a slow consumer that overflows the queue is completed-with-error + disconnected rather than buffered unboundedly. The publisher only ever does a non-blocking enqueue, so one slow consumer cannot back-pressure peers (Project Reactor bounded-buffer / drop concept).
- `RT-RECONNECT-001` — reconnect with `Last-Event-ID` (W3C SSE §9.2) replays gap-free from a bounded per-topic retention ring buffer (manifest `retentionEvents`).
- `RT-PROTOCOL-001` (verification_type: review) — the recipe declares `realtime_protocol: sse | websocket` in RECIPE.md frontmatter (convention below). A mechanical guard is explicitly deferred by the spec (a future `recipe_realtime_protocol_guard.sh`).
- `RT-OBSERVABILITY-001` — exactly three Micrometer meters: `active_subscribers_count{channel,tenant}` (Gauge), `messages_sent_total{channel,tenant}` (Counter), `disconnect_rate{channel,tenant,reason}` (Counter). Bounded labels only (channel = bare topic name, a fixed tenant value, a fixed reason enum {timeout, backpressure, client, complete}); no subscriber id / PII. The `channel` label is bound by a **topic allowlist** (`ax.realtime.topics`, default `notification, audit, payment, system` from the manifest): a subscribe/publish to a topic NOT in the list is rejected **404** BEFORE any registry entry or metric series is created, so an attacker cannot explode the metric cardinality with infinite unique topics (memory / Prometheus DoS). The disconnect path is atomic — a one-shot CAS elects a single removal winner so `disconnect_rate{reason}` is never misattributed or double-counted under the concurrent backpressure/drain-error race.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/realtime/` — cross-cutting, NOT per-domain
  - `RealtimeChannelService` — in-memory subscriber registry per `tenant/topic`; audience-filter-before-emit; bounded per-subscriber write queue with backpressure-disconnect; retention ring buffer for `Last-Event-ID` replay
  - `RealtimeController` — `GET /api/realtime/topics/{tenantScope}/{topic}` returns the `SseEmitter` (`text/event-stream`, honors `Last-Event-ID`); `POST .../publish` is the demo fan-out hook
  - `RealtimeMetrics` — the exactly-three canonical meters, bounded non-PII labels
  - `RealtimeProperties` — manifest-backed `ax.realtime.topics` (bounded allowlist) / `ax.realtime.queueThreshold` / `ax.realtime.retentionEvents`
  - `RealtimeProblemAdvice` + `CrossTenantSubscriptionException` (403) + `UnknownTopicException` (404) — package-scoped RFC 9457 mappings (additive; never claims another domain's exceptions)
- Spec: [`specs/realtime-policy-l0.yaml`](../../../specs/realtime-policy-l0.yaml) — `domain_mode: backend_only`
- Blueprint: [`blueprints/realtime-policy-manifest.yaml`](../../../blueprints/realtime-policy-manifest.yaml) — transport/queue/retention policy anchors
- Tests: `./gradlew testRealtime` — 8 tests = 6 testable items (CHANNEL-AUTH ×2, FANOUT, BACKPRESSURE, RECONNECT, OBSERVABILITY) + 2 hardening regressions (unknown-topic → 404 with no metric series; disconnect recorded exactly once under the backpressure/drain-error race); `RT-PROTOCOL-001` is review-only

### SSE realized · WebSocket documented as the parallel path

The catalog ships canonical wiring for SSE as the reference because it is the simpler, MVC-compatible, RestAssured-testable protocol: SSE subscribe is a plain HTTP `GET` that goes through the existing Servlet/MVC Spring Security chain — additive, no reactive stack mixed into the Servlet app.

A fork that needs **bidirectional** flows (chat, multi-user editing) picks WebSocket instead. The WebSocket path is the parallel option a fork wires as a follow-up (mirrors how multi-tenant documents its runtime primitives):

- Add `spring-boot-starter-websocket`; register a `WebSocketHandler` (or STOMP `@MessageMapping`) endpoint at `/ws`.
- **WS-upgrade auth equivalent of RT-CHANNEL-AUTH-001**: authenticate the upgrade handshake — RFC 6455 §4 allows the HTTP `Authorization` header on the upgrade request; verify the bearer token in a `HandshakeInterceptor` and reject (do not switch protocols) on failure. This is the WebSocket analogue of "SSE goes through the existing chain".
- **Subprotocol negotiation** (RFC 6455 §1.9): pin `realtime_subprotocol: <name>` (e.g. `v12.stomp`) in RECIPE.md; the server echoes the chosen subprotocol and rejects an unimplemented one.
- RT-RECONNECT-001 equivalent: STOMP receipt + server-side replay log, or a custom `resume-from` header on the upgrade request (SSE gets `Last-Event-ID` for free).

Do NOT add Spring WebFlux or a WebSocket server to this reference workload — that mixes reactive into the Servlet/MVC app and is the #1 cross-domain risk. The SSE realization here is intentionally additive.

## Frontend

realtime-policy has **no first-class UI**. It is server-push CONFIG + policy consumed by a fork's live surfaces (the client side is an `EventSource` for SSE or a `WebSocket` for the WS path). The reference workloads ship with polling (`TanStack Query refetchInterval` per R82); a recipe needing server-push adopts these items before production. Registered as `backend_only` in `practices/evals/trio_integrity_allowlist.yaml`.

## Composition contract

Adopt the spec items IN ORDER (each is foundational for the next):

1. `RT-CHANNEL-AUTH-001` — authenticate the subscribe first (skipping it makes every other item irrelevant).
2. `RT-CHANNEL-AUTH-002` — tenant-scope the topic names.
3. `RT-FANOUT-001` — resolve audience before fan-out.
4. `RT-BACKPRESSURE-001` — bound the per-subscriber write queue.
5. `RT-RECONNECT-001` — resume gap-free from `Last-Event-ID`.
6. `RT-PROTOCOL-001` — declare the protocol in RECIPE.md (below).
7. `RT-OBSERVABILITY-001` — emit the three canonical metrics before production.

### RECIPE.md `realtime_protocol` / `retention_window` convention (RT-PROTOCOL-001)

A recipe that needs server-push declares the protocol + retention window in its RECIPE.md frontmatter (exactly one of `sse` / `websocket` — mixing both is not catalog-supported):

```yaml
---
recipe: notification-livetail
tenant_model: multi
realtime_protocol: sse              # OR: websocket
realtime_retention_window_seconds: 300
# realtime_subprotocol: v12.stomp   # WebSocket only (RFC 6455 §1.9)
---
```

- `realtime_protocol` — `sse` for server-push-only (notification feed, dashboard tiles); `websocket` for bidirectional (chat, multi-user editing).
- `realtime_retention_window_seconds` — the RT-RECONNECT-001 replay window the fork maps onto `ax.realtime.retentionEvents`.

A mechanical `recipe_realtime_protocol_guard.sh` is deferred by the spec until adoption pressure is real.

## Next steps

- Promote `recipe_orphan: true` → wired into a recipe when a fork-receiver flips a live surface from polling to server-push (e.g. a `notification-livetail` recipe).
- Add a dogfood ledger entry (`docs/dogfood-ledger/realtime-policy-iter1.yaml`) once a recipe composes it — the 2-persona protocol applies to cross-cutting primitives the same as to domain verticals.
- A distributed fan-out (Redis pub/sub or an outbox → SSE bridge) for multi-node deployments is a fork-receiver concern; the reference ships the single-node in-memory registry.
