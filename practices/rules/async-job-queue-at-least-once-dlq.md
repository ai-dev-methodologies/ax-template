---
title: An async job queue MUST be at-least-once with explicit ack + visibility timeout, bounded jittered retry, and a dead-letter queue — idempotent enqueue and workers
impact: HIGH
impactDescription: "A worker that acks a message before it finishes loses the job on a crash (at-most-once data loss); one with no visibility timeout lets two workers grab the same job; one that retries forever wedges the queue behind a poison message; one with no idempotency key double-charges on the inevitable redelivery. At-least-once delivery only works when ack, visibility timeout, bounded retry, a DLQ, and idempotency are all present."
tags:
  - job-queue
  - at-least-once
  - dead-letter-queue
  - visibility-timeout
  - idempotency
  - reliability
spec_ref: "specs/job-queue-l0.yaml#JOB-WORKER-001"
verification:
  type: review
  source: "specs/job-queue-l0.yaml#JOB-WORKER-001"
  pattern: "An async job queue MUST deliver at-least-once with an explicit ack/nack and a visibility timeout: a received message is invisible to other consumers while in flight and is only removed when the worker acks AFTER successful processing — ack-before-work is forbidden (it loses the job on crash) (JOB-WORKER-001). Enqueue returns a job id and accepts a client idempotency key so a duplicate submit does not create a duplicate job (JOB-ENQUEUE-001). A failed/nacked job is retried with exponential backoff + jitter up to a max-attempts cap — never immediately, never unbounded (JOB-RETRY-001). A job that exhausts retries is routed to a dead-letter queue for inspection/replay, never silently dropped and never left to recirculate forever (JOB-DLQ-001). Priority lanes are served with a fairness guarantee so low-priority jobs cannot starve (JOB-PRIORITY-001). Job status + a result handle are queryable by job id (JOB-STATUS-001). Because delivery is at-least-once, every worker MUST be idempotent on the job id. Reject ack-before-processing, an unbounded retry with no DLQ, and a non-idempotent worker."
upstream:
  - "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html"
  - "https://www.rabbitmq.com/docs/confirms"
evidence:
  - source_type: external
    citation: "Amazon SQS Developer Guide — Visibility timeout (message invisible while in flight)"
    url: "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html"
    quote: "When you receive a message from an Amazon SQS queue, it remains in the queue but becomes temporarily invisible to other consumers."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Amazon SQS Developer Guide — Visibility timeout (redelivery on no-delete)"
    url: "https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html"
    quote: "If you don't delete it before the timeout expires, the message becomes visible again in the queue and can be retrieved by another consumer."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## An async job queue MUST be at-least-once with ack + visibility timeout, bounded jittered retry, and a DLQ

**Impact: HIGH — A durable job queue gives at-least-once delivery, and every one of its safety properties exists to make that workable. Per the SQS model, *when you receive a message from an Amazon SQS queue, it remains in the queue but becomes temporarily invisible to other consumers* — the visibility timeout — and *if you don't delete it before the timeout expires, the message becomes visible again in the queue and can be retrieved by another consumer*. That redelivery-on-failure is exactly what prevents lost jobs, but it means a worker that acks before it finishes loses the job on a crash, two workers can grab the same job without a visibility timeout, a job retried forever wedges the queue, and any non-idempotent worker double-applies on the inevitable redelivery.**

There are six load-bearing requirements — the items of `specs/job-queue-l0.yaml`, all governed by this rule.

**1. At-least-once with ack + visibility timeout (JOB-WORKER-001).** A received message is invisible to other consumers for the visibility timeout. The worker acks (deletes) ONLY after successful processing. Ack-before-work is forbidden — a crash mid-processing would lose the job. A nack (or timeout) returns the message for redelivery.

**2. Idempotent enqueue + job id (JOB-ENQUEUE-001).** Enqueue returns a job id and accepts a client idempotency key, so a retried submit (network blip) does not create a second job. Composes `idempotency-l0`.

**3. Bounded jittered retry (JOB-RETRY-001).** A failed job is retried with exponential backoff + jitter up to a max-attempts cap — never an immediate loop, never unbounded.

**4. Dead-letter queue (JOB-DLQ-001).** A job that exhausts its retries goes to a DLQ for inspection/replay — never silently dropped, never left recirculating in the main queue forever blocking other jobs.

**5. Priority + fairness (JOB-PRIORITY-001).** Priority lanes are served with a fairness guarantee (aging / weighted) so low-priority jobs cannot starve indefinitely behind a flood of high-priority ones.

**6. Status + result handle (JOB-STATUS-001).** Job status (`queued`/`running`/`succeeded`/`failed`/`dead-lettered`) and a result handle are queryable by job id.

**Incorrect — acks before processing and retries forever; a crash loses the job, a poison job wedges the queue:**

```java
void consume(Message m) {
    queue.ack(m);                          // VIOLATION: ack BEFORE work → crash below loses the job (JOB-WORKER-001)
    while (true) {                         // VIOLATION: unbounded retry, no DLQ (JOB-RETRY-001/JOB-DLQ-001)
        try { process(m); return; }
        catch (Exception e) { /* retry immediately, forever */ }
    }
}
```

**Correct — ack after success; bounded jittered retry; DLQ on exhaustion; idempotent worker:**

```java
void consume(Message m) {                  // delivered within its visibility timeout (JOB-WORKER-001)
    if (processed.contains(m.jobId())) { queue.ack(m); return; } // idempotent on job id
    try {
        process(m);                        // do the work first
        processed.add(m.jobId());
        queue.ack(m);                      // ack ONLY after success
    } catch (RetryableException e) {
        if (m.attempts() >= MAX_ATTEMPTS)  // bounded (JOB-RETRY-001)
            queue.deadLetter(m);           // → DLQ for inspection/replay (JOB-DLQ-001)
        else
            queue.nackWithBackoff(m, expoJitter(m.attempts())); // backoff + jitter
    }
}
```

Verification: review-tier. Delivery semantics are a runtime-failure property with no compile-time signal — an ack-before-work worker processes the happy path fine and only loses jobs on crashes. Verify by review against `specs/job-queue-l0.yaml`: at-least-once with a visibility timeout and ack-after-success; enqueue is idempotent on a client key and returns a job id; retries are bounded with backoff+jitter; exhausted jobs go to a DLQ; priority lanes guarantee fairness; status is queryable; workers are idempotent on the job id. When a fork-receiver wires a real IT (kill a worker mid-job; assert redelivery + single net effect; poison job → DLQ), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Amazon SQS — Visibility timeout](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html)
