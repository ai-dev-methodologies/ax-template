---
title: A producer that mutates state AND emits a message MUST use a transactional outbox — never a dual write
impact: HIGH
impactDescription: "Writing the domain row and publishing to the broker as two separate operations (a dual write) is not atomic: a crash between them either commits the state change with no message published (a lost event — downstream never learns) or publishes a message for a transaction that rolled back (a phantom event). The transactional outbox closes the gap: the message is inserted into an outbox table inside the SAME local transaction as the state change, and a separate relay publishes it at-least-once."
tags:
  - messaging
  - transactional-outbox
  - dual-write
  - at-least-once
  - reliability
  - eventual-consistency
spec_ref: "specs/transactional-outbox-l0.yaml#OUTBOX-WRITE-001"
verification:
  type: review
  source: "specs/transactional-outbox-l0.yaml#OUTBOX-WRITE-001"
  pattern: "A command that mutates domain state AND must emit a message MUST insert the message into an outbox table in the SAME local @Transactional unit as the entity write (OUTBOX-WRITE-001 — never a broker publish call inside or after the transaction, which is a dual write). A separate relay publishes PENDING rows to the broker and marks them SENT, delivering at-least-once (OUTBOX-RELAY-001). Every outbox row carries a stable immutable message_id assigned at write time so a duplicate delivery is detectable and the consumer can dedup (OUTBOX-IDEMPOTENT-001). Rows for the same aggregate_id are published in committed order (OUTBOX-ORDERING-001). SENT rows are purged/archived past a retention window so the table does not grow unbounded (OUTBOX-CLEANUP-001). A failed publish leaves the row PENDING for retry with exponential backoff + jitter, and a poison row that exhausts retries is routed to a DLQ rather than blocking the relay (OUTBOX-FAILURE-001). Reject any producer that calls the broker directly alongside the DB write, that publishes from inside the transaction, or that regenerates the message_id on relay."
upstream:
  - "https://microservices.io/patterns/data/transactional-outbox.html"
  - "https://microservices.io/patterns/data/polling-publisher.html"
evidence:
  - source_type: external
    citation: "Chris Richardson — Transactional outbox pattern (microservices.io, Solution)"
    url: "https://microservices.io/patterns/data/transactional-outbox.html"
    quote: "The solution is for the service that sends the message to first store the message in the database as part of the transaction that updates the business entities. A separate process then sends the messages to the message broker."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Chris Richardson — Transactional outbox pattern (microservices.io, message outbox)"
    url: "https://microservices.io/patterns/data/transactional-outbox.html"
    quote: "Message outbox - if it's a relational database, this is a table that stores the messages to be sent."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Chris Richardson — Polling publisher pattern (microservices.io, Solution)"
    url: "https://microservices.io/patterns/data/polling-publisher.html"
    quote: "Publish messages by polling the database's outbox table."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A producer that mutates state AND emits a message MUST use a transactional outbox — never a dual write

**Impact: HIGH — A producer command often does two things: it mutates domain state (save the order) and it emits a message (publish `OrderPlaced`). Doing those as two independent operations — write the row, then call the broker — is a *dual write*, and it is not atomic. If the process crashes (or the broker is briefly unreachable) between the two, you get one of two silent corruptions: the state change commits but no message is published (a *lost event* — every downstream consumer stays permanently out of sync), or the message is published for a transaction that then rolls back (a *phantom event* — consumers act on something that never happened). Neither is caught by retries, idempotency keys, or optimistic locking, because the two stores were never coordinated. Chris Richardson's transactional outbox states the fix directly — *first store the message in the database as part of the transaction that updates the business entities. A separate process then sends the messages to the message broker.***

The outbox makes the message *part of the same local transaction* as the state change, so they commit or roll back together; a relay then moves committed messages to the broker. There are six load-bearing requirements — they are the items of `specs/transactional-outbox-l0.yaml`, and this rule governs all six.

**1. Atomic write, no dual write (OUTBOX-WRITE-001).** The entity write and the outbox-row insert happen in ONE `@Transactional` unit. The broker is NOT called inside the transaction (its side effect cannot be rolled back) and NOT called right after (the crash gap reappears). The only durable artifact of the command is rows in the database — the domain row and its outbox row, committed atomically. The outbox is, per microservices.io, *a table that stores the messages to be sent*.

**2. A separate relay publishes at-least-once (OUTBOX-RELAY-001).** A distinct relay — a polling publisher that periodically reads `status=PENDING` rows in committed order and publishes them, or a transaction-log tailer — moves messages to the broker and marks each `SENT`. Per the polling-publisher pattern: *publish messages by polling the database's outbox table.* Because the relay can crash after publishing but before marking the row `SENT`, delivery is **at-least-once**: a message may be published more than once, and that is acceptable *only because* requirement 3 makes duplicates detectable.

**3. Stable message id → consumer dedup (OUTBOX-IDEMPOTENT-001).** Every outbox row carries a stable, immutable `message_id` (an RFC 4122 UUID) assigned at WRITE time and **never regenerated** on relay or retry. The same logical message thus carries the same id across every redelivery, so a consumer can dedup on it (composing `idempotency-l0`). Regenerating the id on relay defeats at-least-once safety — each duplicate would look like a new message.

**4. Per-aggregate ordering (OUTBOX-ORDERING-001).** Messages for the same `aggregate_id` MUST be published in the order they were committed — the relay dispatches them ordered by a monotonic per-row sequence (the outbox PK / a sequence column). Global total order is not required; *per-aggregate* order is, or a consumer sees `Updated` before `Created`.

**5. Bounded retention (OUTBOX-CLEANUP-001).** `SENT` rows MUST NOT accumulate forever — they are purged or archived once confirmed published and older than a retention window. An unbounded outbox table eventually starves the relay's `PENDING` scan and the database. Cleanup runs off the hot path (a scheduled sweep), never inline with publishing.

**6. Retry, backoff, poison handling (OUTBOX-FAILURE-001).** When a broker publish fails, the row STAYS `PENDING` (never dropped) and is retried by the next relay pass with exponential backoff plus jitter. A poison row that exhausts its retry budget is routed to a DLQ (composing the job-queue DLQ pattern) so one undeliverable message does not wedge the relay behind it.

**Incorrect — dual write: the broker call and the DB write are not atomic; a crash between them loses or phantoms the event:**

```java
@Transactional
public Order place(PlaceOrder cmd) {
    Order order = orderRepository.save(Order.from(cmd));   // (1) DB write
    broker.publish("orders", new OrderPlaced(order.id())); // (2) VIOLATION: broker call
    // If (2) is inside the txn and the txn later rolls back → phantom event.
    // If (2) is moved after the txn and the process crashes first → lost event.
    return order;
}
```

**Correct — outbox row written in the same transaction; a separate relay publishes at-least-once with a stable id:**

```java
@Transactional
public Order place(PlaceOrder cmd) {
    Order order = orderRepository.save(Order.from(cmd));
    outbox.save(OutboxRow.pending(
        UUID.randomUUID(),            // stable message_id, assigned ONCE at write (OUTBOX-IDEMPOTENT-001)
        order.id(),                   // aggregate_id for per-aggregate ordering (OUTBOX-ORDERING-001)
        "orders", new OrderPlaced(order.id())));
    return order;                     // entity + outbox row commit atomically (OUTBOX-WRITE-001)
}

// Separate relay (OUTBOX-RELAY-001) — polling publisher:
@Scheduled(fixedDelay = 1000)
@Transactional
public void relay() {
    for (OutboxRow row : outbox.findPendingOrderedByAggregateThenSeq(BATCH)) {
        try {
            broker.publish(row.topic(), row.payload());   // at-least-once
            row.markSent();                               // crash before this → republished (dedup on message_id)
        } catch (BrokerException e) {
            row.scheduleRetryWithBackoffJitter();         // stays PENDING (OUTBOX-FAILURE-001)
            if (row.retriesExhausted()) row.routeToDlq(); // poison → DLQ
        }
    }
}
// Retention sweep (OUTBOX-CLEANUP-001) purges SENT rows past the window, off the hot path.
```

Verification: review-tier. Atomicity-of-two-stores is a structural property with no compile-time signal — a dual write compiles and usually works, failing only on the crash/outage window. Verify by review against `specs/transactional-outbox-l0.yaml`: the message insert shares the entity's `@Transactional` unit; no broker call sits inside or immediately after that transaction; a separate relay publishes `PENDING`→`SENT` at-least-once; `message_id` is assigned once at write and reused on every retry; same-`aggregate_id` rows publish in committed order; `SENT` rows are purged on a retention window; failed publishes stay `PENDING` with backoff+jitter and poison rows go to a DLQ. When a fork-receiver wires a real `@Tag("OUTBOX-WRITE-001")` IT (kill the broker; assert the row is still in the outbox and republished on recovery), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Chris Richardson — Transactional outbox pattern (store the message in the same transaction)](https://microservices.io/patterns/data/transactional-outbox.html)

Reference: [Chris Richardson — Polling publisher pattern (publish by polling the outbox table)](https://microservices.io/patterns/data/polling-publisher.html)
