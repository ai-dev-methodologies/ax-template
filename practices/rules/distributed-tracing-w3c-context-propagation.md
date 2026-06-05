---
title: Distributed tracing MUST propagate W3C Trace Context across services — traceparent in, span out, trace_id in logs
impact: MEDIUM
impactDescription: "If a service does not read the inbound traceparent and continue the trace, every hop starts a fresh disconnected trace — a cross-service request can no longer be reconstructed end to end, and a production incident that spans three services shows three unrelated traces instead of one. W3C Trace Context is the wire standard that keeps the trace continuous; a span lifecycle, sampling, semantic conventions, and trace_id-in-logs make it usable."
tags:
  - observability
  - distributed-tracing
  - w3c-trace-context
  - opentelemetry
  - propagation
  - correlation
spec_ref: "specs/distributed-tracing-l0.yaml#TRACE-CONTEXT-001"
verification:
  type: review
  source: "specs/distributed-tracing-l0.yaml#TRACE-CONTEXT-001"
  pattern: "A service MUST propagate W3C Trace Context: read the inbound `traceparent` (and `tracestate`) header, continue that trace (not start a fresh root), and inject `traceparent` on every outbound call so the trace stays continuous across hops (TRACE-CONTEXT-001). Each unit of work opens a span with a parent-child link to the inbound context and records a status (ok/error) and is always ended, even on exception (TRACE-SPAN-001). Cross-cutting values that must travel with the trace (tenant, locale) ride W3C `baggage`, never smuggled in app headers (TRACE-BAGGAGE-001). Sampling is a declared head-based ratio that is parent-based — a child respects the parent's sampled decision so a trace is sampled whole or not at all (TRACE-SAMPLING-001). Span and metric attributes follow OpenTelemetry semantic conventions, not ad-hoc names (TRACE-SEMCONV-001). The active `trace_id` is placed in the logging MDC so every structured log line correlates to its trace (TRACE-CORRELATION-001). Reject a service that ignores the inbound traceparent and starts a new root, that drops the header on outbound calls, that leaves spans unended on the error path, or that logs without the trace_id."
upstream:
  - "https://www.w3.org/TR/trace-context/"
  - "https://opentelemetry.io/docs/specs/semconv/"
evidence:
  - source_type: external
    citation: "W3C Trace Context (W3C Recommendation) — Abstract / purpose"
    url: "https://www.w3.org/TR/trace-context/"
    quote: "This specification defines standard HTTP headers and a value format to propagate context information that enables distributed tracing scenarios."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "W3C Trace Context (W3C Recommendation) — traceparent header field"
    url: "https://www.w3.org/TR/trace-context/"
    quote: "The `traceparent` HTTP header field identifies the incoming request in a tracing system."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "W3C Trace Context (W3C Recommendation) — tracestate header field"
    url: "https://www.w3.org/TR/trace-context/"
    quote: "The main purpose of the `tracestate` HTTP header is to provide additional vendor-specific trace identification information across different distributed tracing systems."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Distributed tracing MUST propagate W3C Trace Context across services — traceparent in, span out, trace_id in logs

**Impact: MEDIUM — A request that crosses three services should produce ONE trace with a span per hop, linked parent-to-child. It only does so if every service reads the inbound trace context and continues it. The moment one service ignores the inbound `traceparent` and starts a fresh root span, the trace breaks: the downstream work shows up as an unrelated trace with no link back to the caller, and an on-call engineer debugging a slow or failing cross-service request sees disconnected fragments instead of one timeline. W3C Trace Context is the wire standard that prevents this — per the spec, it *defines standard HTTP headers and a value format to propagate context information that enables distributed tracing scenarios*, and the `traceparent` header *identifies the incoming request in a tracing system*.**

There are six load-bearing requirements — the items of `specs/distributed-tracing-l0.yaml`, all governed by this rule.

**1. W3C Trace Context propagation (TRACE-CONTEXT-001).** On every inbound request the service reads `traceparent` (and `tracestate`) and *continues that trace* — the new span's parent is the inbound span id, not a fresh root. On every outbound call (HTTP client, message publish) it injects the current `traceparent` so the next hop continues the same trace. A missing inbound `traceparent` starts a new root (legitimate entry point); a present one MUST be honored, never discarded.

**2. Span lifecycle + parent-child + status (TRACE-SPAN-001).** Each unit of work opens a span linked to the active context, sets a status (`OK` / `ERROR`, with the error recorded on the exception path), and is ALWAYS ended — in a `finally` / try-with-resources scope, so an exception never leaks an unended span (which would corrupt span timing and parent-child nesting).

**3. W3C Baggage for cross-cutting values (TRACE-BAGGAGE-001).** Values that must travel with the whole trace (tenant id, locale) ride the W3C `baggage` header, propagated alongside `traceparent` — not smuggled in bespoke application headers that downstream tracing is blind to. `tracestate` carries vendor trace identification (*additional vendor-specific trace identification information across different distributed tracing systems*); `baggage` carries application context.

**4. Parent-based, head-based sampling (TRACE-SAMPLING-001).** The sampling decision is a *declared* head-based ratio (e.g. a `TraceIdRatioBased` sampler) wrapped parent-based: a child span respects the parent's sampled flag so a distributed trace is sampled *as a whole* — all spans or none. A per-service independent decision fragments a trace into a mix of sampled and dropped spans that cannot be assembled.

**5. OpenTelemetry semantic conventions (TRACE-SEMCONV-001).** Span and attribute names follow OpenTelemetry semantic conventions (`http.request.method`, `url.path`, `server.address`, …) rather than ad-hoc per-service names, so traces are queryable and comparable across services and backends.

**6. trace_id in structured logs (TRACE-CORRELATION-001).** The active `trace_id` (from the W3C context) is placed in the logging MDC for the duration of the request, so every structured log line carries the trace_id and a log can be pivoted to its trace and back. (Distinct from a locally-minted request id — this is the propagated W3C trace_id, the same value across all hops.)

**Incorrect — ignores the inbound context and starts a new root; the cross-service trace breaks:**

```java
@GetMapping("/orders/{id}")
public Order get(@PathVariable String id) {
    Span span = tracer.spanBuilder("getOrder").startSpan();   // VIOLATION: no parent from inbound traceparent → new root
    Order o = httpClient.get("/inventory/" + id);              // VIOLATION: traceparent NOT injected → downstream is a 3rd disconnected trace
    return o;                                                  // VIOLATION: span never ended; error path leaks it
}
```

**Correct — continues the inbound trace, injects on outbound, ends the span, trace_id in MDC:**

```java
@GetMapping("/orders/{id}")
public Order get(@PathVariable String id) {
    // Context.extract(inbound traceparent/tracestate) done by the tracing filter;
    // the span's parent IS the inbound span (TRACE-CONTEXT-001 / TRACE-SPAN-001).
    Span span = tracer.spanBuilder("getOrder")
        .setSpanKind(SpanKind.SERVER).startSpan();
    try (Scope scope = span.makeCurrent()) {
        MDC.put("trace_id", span.getSpanContext().getTraceId());  // TRACE-CORRELATION-001
        Order o = httpClient.get("/inventory/" + id);             // traceparent auto-injected (TRACE-CONTEXT-001 outbound)
        span.setStatus(StatusCode.OK);
        return o;
    } catch (RuntimeException e) {
        span.recordException(e); span.setStatus(StatusCode.ERROR);
        throw e;
    } finally {
        MDC.remove("trace_id");
        span.end();                                               // always ended (TRACE-SPAN-001)
    }
}
// Sampler: ParentBased(TraceIdRatioBased(ratio)) — parent-based head sampling (TRACE-SAMPLING-001).
// Attributes follow OTel semantic conventions (TRACE-SEMCONV-001).
```

Verification: review-tier. Trace continuity is a cross-service property with no compile-time signal — a service that drops the inbound context compiles and serves requests fine; the break only shows when you try to assemble a trace across hops. Verify by review against `specs/distributed-tracing-l0.yaml`: inbound `traceparent` is read and continued (not a fresh root); `traceparent` is injected on every outbound call; spans are always ended with a status; `baggage` carries cross-cutting values; sampling is parent-based head ratio; attributes follow OTel semantic conventions; the propagated `trace_id` is in the MDC for every log line. When a fork-receiver wires a real two-service IT asserting the downstream span's trace_id equals the upstream's, this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [W3C Trace Context (standard headers to propagate distributed tracing context)](https://www.w3.org/TR/trace-context/)

Reference: [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/)
