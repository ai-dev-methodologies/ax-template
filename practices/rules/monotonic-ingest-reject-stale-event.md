---
title: A late / out-of-order external event MUST NOT clobber a fresher current-state row — reject at-or-behind the watermark
impact: HIGH
impactDescription: "On an at-least-once event stream, a 'delivered' webhook can physically arrive before the earlier 'out-for-delivery' webhook; a naive last-write-wins handler rolls the authoritative shipment/position/status row backwards to a stale value, corrupting the record every downstream consumer trusts"
tags:
  - event-ingest
  - out-of-order
  - watermark
  - idempotency
  - at-least-once
  - state-projection
spec_ref: "specs/monotonic-event-ingest-l0.yaml#INGEST-REJECT-STALE-001"
verification:
  type: review
  source: "specs/monotonic-event-ingest-l0.yaml#INGEST-REJECT-STALE-001"
  pattern: "the event apply path compares the inbound event-time/sequence against the persisted monotonic watermark; an event with event-time/sequence <= watermark is dropped (counted, ack'd) and never mutates the row"
upstream:
  - "https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/concepts/time/"
  - "https://microservices.io/patterns/communication-style/idempotent-consumer.html"
evidence:
  - source_type: external
    citation: "Apache Flink 1.18 — Timely Stream Processing: Event Time and Watermarks"
    url: "https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/concepts/time/"
    quote: "A Watermark(t) declares that event time has reached time t in that stream, meaning that there should be no more elements from the stream with a timestamp t' <= t (i.e. events with timestamps older or equal to the watermark)."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "microservices.io — Idempotent Consumer pattern (at-least-once delivery)"
    url: "https://microservices.io/patterns/communication-style/idempotent-consumer.html"
    quote: "Make a consumer idempotent by having it record the IDs of processed messages in the database. When processing a message, a consumer can detect and discard duplicates by querying the database."
    quoted_at: "2026-06-01"
---

## A late / out-of-order external event MUST NOT clobber a fresher current-state row — reject at-or-behind the watermark

**Impact: HIGH — on an at-least-once stream, arrival order is not event order; last-write-wins silently corrupts the authoritative row**

When an authoritative current-state row is a projection of an external event stream — a shipment's lifecycle status folded from carrier webhooks, a vehicle's last-known position folded from a telemetry feed, an order's status folded from a payment-provider callback — the events do not arrive in the order they were emitted. At-least-once transports retry, reorder, and redeliver. A `delivered` webhook stamped at 14:05 can physically POST to your endpoint *before* the `out_for_delivery` webhook stamped at 13:50 that logically precedes it. A handler that blindly writes whatever it last received rolls the row backward from `delivered` to `out_for_delivery`, and every dashboard, SLA timer, and customer notification that reads the row now trusts a stale value.

The fix is the stream-processing watermark applied to a single row. The row persists a monotonic `last_applied` watermark — the maximum event-time (or provider sequence number) it has ever applied. On each inbound event the handler compares the event's own event-time against the watermark. An event strictly ahead advances the row and the watermark together. An event at or behind the watermark is **rejected**: it does not mutate the row, it does not roll any field backward, it is counted (`stale_event_dropped_total{reason=behind_watermark}`), and it is acknowledged to the transport so the provider stops redelivering. Out-of-order delivery is normal, not an error — a behind-the-watermark event is a no-op, never a 5xx.

This is distinct from optimistic locking (which guards a human read-modify-write over HTTP with ETag/If-Match) and from using the server clock for decisions (PRACTICES-TIME-001): here the ordering authority is the *event's own* event-time, and the row carries the watermark.

**Incorrect — last-write-wins; a late `out_for_delivery` rolls `delivered` backward:**

```java
@PostMapping("/webhooks/carrier")
public ResponseEntity<Void> onCarrierEvent(@RequestBody CarrierEvent ev) {
    Shipment s = shipments.findByTrackingNo(ev.trackingNo()).orElseThrow();
    s.setStatus(ev.status());          // ❌ whatever arrived last wins
    s.setStatusAt(ev.occurredAt());    // ❌ a 13:50 event clobbers the 14:05 'delivered'
    shipments.save(s);                 // row now reads a stale, earlier state
    return ResponseEntity.ok().build();
}
```

**Correct — compare event-time to the monotonic watermark; reject at-or-behind:**

```java
@PostMapping("/webhooks/carrier")
public ResponseEntity<Void> onCarrierEvent(@RequestBody CarrierEvent ev) {
    Shipment s = shipments.findByTrackingNo(ev.trackingNo()).orElseThrow();

    // event-time of THIS event, stamped by the carrier at emission — not local receive time
    Instant eventTime = ev.occurredAt();

    // at or behind the watermark => late / out-of-order on an at-least-once stream: drop, count, ack
    if (!eventTime.isAfter(s.getLastAppliedAt())) {
        staleDropped.increment("carrier", "behind_watermark");
        return ResponseEntity.ok().build();   // ✅ no mutation, no rollback, no redelivery storm
    }

    s.setStatus(ev.status());
    s.setStatusAt(eventTime);
    s.setLastAppliedAt(eventTime);            // ✅ watermark advances monotonically, same txn
    shipments.save(s);
    return ResponseEntity.ok().build();
}
```

Pair this with idempotent dedup on `(stream, event_id)` (INGEST-IDEMPOTENT-APPLY-001) so an exact replay of the current event is also a no-op: the watermark check catches *older* events, dedup catches *identical* ones. Both converge on the invariant — a fresher persisted value is never clobbered, and each distinct event takes effect exactly once.

Verification: review the apply path against `specs/monotonic-event-ingest-l0.yaml#INGEST-REJECT-STALE-001` — confirm the inbound event-time/sequence is compared to a persisted monotonic watermark and that an at-or-behind event mutates nothing.

Reference: [Apache Flink — Event Time and Watermarks](https://nightlies.apache.org/flink/flink-docs-release-1.18/docs/concepts/time/)

Reference: [microservices.io — Idempotent Consumer](https://microservices.io/patterns/communication-style/idempotent-consumer.html)
