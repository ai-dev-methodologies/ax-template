---
title: A recipe MUST declare a single realtime protocol — SSE OR WebSocket — never both in one recipe
impact: MEDIUM
impactDescription: "Mixing SSE and WebSocket in one recipe doubles the realtime surface a fork-receiver must build, secure, scale, and reconnect: two server endpoints, two client SDKs, two auth integrations, two backpressure stories. Declaring exactly one protocol — SSE for server-push-only flows, WebSocket for bidirectional flows — keeps the realtime layer coherent and the fork-receiver's client one code path."
tags:
  - realtime
  - sse
  - websocket
  - protocol
  - configuration
spec_ref: "specs/realtime-policy-l0.yaml#RT-PROTOCOL-001"
verification:
  type: review
  source: "specs/realtime-policy-l0.yaml#RT-PROTOCOL-001"
  pattern: "A recipe with a realtime surface MUST declare in RECIPE.md frontmatter which protocol it adopts — `realtime_protocol: sse` OR `realtime_protocol: websocket` — and MUST NOT mix both in the same recipe (a 2x maintenance/SDK surface). SSE (Server-Sent Events) is the choice for server-push-only flows (notification feeds, live status, progress) — it is a one-way server→client connection over plain HTTP. WebSocket is the choice for bidirectional flows (chat, multi-user editing, collaborative cursors). The declared protocol matches the flow's directionality: a server-push-only flow MUST NOT pull in a full WebSocket, and a bidirectional flow MUST NOT be forced onto SSE + a side-channel. Reject a recipe that declares both protocols, and a protocol choice mismatched to the flow's directionality."
upstream:
  - "https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events"
  - "https://html.spec.whatwg.org/multipage/server-sent-events.html"
evidence:
  - source_type: external
    citation: "MDN Web Docs — Using server-sent events (one-way connection)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events"
    quote: "This is a one-way connection, so you can't send events from a client to a server."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A recipe MUST declare a single realtime protocol — SSE OR WebSocket — never both

**Impact: MEDIUM — A realtime feature needs a transport, and the two mainstream choices solve different shapes. SSE is server-push-only: per MDN, *this is a one-way connection, so you can't send events from a client to a server* — ideal for notification feeds, live status, and progress where the client only consumes. WebSocket is full-duplex — needed for chat, multi-user editing, and collaborative flows where the client also sends. A recipe that ships BOTH doubles everything a fork-receiver must build and operate: two server endpoints, two client SDKs, two auth paths, two reconnect/backpressure strategies. The catalog default (the `realtime-policy` spec) is SSE-first; this rule pins the single-protocol declaration so a recipe's realtime layer stays one coherent code path.**

There is one load-bearing requirement for `RT-PROTOCOL-001`.

**1. Declare exactly one realtime protocol.** A recipe with a realtime surface declares `realtime_protocol: sse` OR `realtime_protocol: websocket` in RECIPE.md frontmatter, and never both. The choice matches the flow's directionality:
- **SSE** — server-push-only flows (notification feed, live status, job progress). One-way, plain HTTP, auto-reconnect built in.
- **WebSocket** — bidirectional flows (chat, multi-user editing, presence with client input).

A server-push-only flow must NOT pull in a full WebSocket (unnecessary bidirectional surface); a bidirectional flow must NOT be bolted onto SSE plus a separate POST side-channel (that IS mixing, one direction each).

**Incorrect — declares both; uses a WebSocket for a one-way notification feed:**

```yaml
# RECIPE.md frontmatter
recipe: notifications
realtime_protocol: [sse, websocket]   # VIOLATION: both → 2x surface (RT-PROTOCOL-001)
# the feed is server-push-only, yet a full WebSocket is stood up — wrong directionality
```

**Correct — one declared protocol matching the flow's directionality:**

```yaml
# RECIPE.md frontmatter — a server-push-only notification feed
recipe: notifications
realtime_protocol: sse                 # one-way server→client; SSE fits (RT-PROTOCOL-001)
---
# a bidirectional chat recipe would instead declare:
# realtime_protocol: websocket
```

Verification: review-tier. Protocol coherence is a configuration property — a recipe that mixes transports compiles and runs while doubling the maintenance surface. Verify by review against `specs/realtime-policy-l0.yaml#RT-PROTOCOL-001`: the recipe declares exactly one `realtime_protocol` (sse or websocket), not both, and the choice matches the flow's directionality (SSE for server-push-only, WebSocket for bidirectional). When a fork-receiver wires a guard that parses RECIPE.md frontmatter and rejects a dual/absent `realtime_protocol`, this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [MDN — Using server-sent events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)

Reference: [WHATWG HTML — Server-sent events](https://html.spec.whatwg.org/multipage/server-sent-events.html)
