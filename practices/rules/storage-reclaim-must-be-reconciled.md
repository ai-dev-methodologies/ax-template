---
title: Blob reclaim on row delete MUST be reconciled — never a fire-and-forget inline storage delete
impact: HIGH
impactDescription: "A DB row delete and an object-store blob delete cannot be made atomic without 2PC; an inline storage-delete that fails after the row is gone leaks an orphan blob, and one that throws BEFORE the row commits either rolls back the delete (500) or, if swallowed, leaves the bytes forever"
tags:
  - storage
  - reconciliation
  - dual-write
  - object-store
  - idempotency
spec_ref: "specs/storage-reconciliation-l0.yaml#RECON-DELETE-001"
verification:
  type: review
  source: "specs/storage-reconciliation-l0.yaml#RECON-DELETE-001"
  pattern: "On row purge, the blob key is enqueued for reclaim in the SAME local DB transaction that removes the row (reclaim-queue / transactional-outbox row); the inline object-store delete is a swallowed-then-enqueued fast path, never a 500 that rolls back the row removal; an async reclaim worker (running under the scheduled-task LockingPolicy) drains the queue and treats an already-absent key as success."
upstream:
  - "https://microservices.io/patterns/data/transactional-outbox.html"
  - "https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeletingObjects.html"
evidence:
  - source_type: external
    citation: "Chris Richardson — Pattern: Transactional outbox (microservices.io), Context section (dual-write problem)"
    url: "https://microservices.io/patterns/data/transactional-outbox.html"
    quote: "But without using 2PC, sending a message in the middle of a transaction is not reliable. There's no guarantee that the transaction will commit. Similarly, if a service sends a message after committing the transaction there's no guarantee that it won't crash before sending the message."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "AWS — Deleting Amazon S3 objects (Amazon S3 User Guide), Best practices section"
    url: "https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeletingObjects.html"
    quote: "If you want to delete a large number of objects, or for programmatically deleting objects based on object creation date, set a S3 Lifecycle configuration on your bucket."
    quoted_at: "2026-06-01"
---

## Blob reclaim on row delete MUST be reconciled — never a fire-and-forget inline storage delete

**Impact: HIGH — a DB row delete and an object-store blob delete cannot be committed atomically without 2PC; the gap between them leaks orphan blobs or rolls back the delete as a 500**

A huge number of domains store a blob in an external object store (S3 / MinIO / GCS / Azure Blob) and a referencing row in the relational DB: file-storage `StoredFile.storageKey`, report-export output keys, avatar images, generated PDFs. Deleting the entity is a **dual write** — remove the DB row *and* delete the blob — and these two stores live behind two independent transaction managers. There is no single atomic commit across them without a distributed (2PC) transaction, which almost nobody runs. So whatever order you choose, a crash or a network blip in the middle leaves drift: delete-the-blob-first then the row-delete rolls back and you have a live row pointing at bytes that are gone (a *missing* blob); delete-the-row-first then the storage call fails and you have bytes nobody references (an *orphan* blob) accumulating storage cost forever. The naive fix — call `s3.deleteObject(...)` inline inside the `@Transactional` service method — is the worst of both: if it throws, it either propagates as a 500 that rolls back the row removal (now the user can't delete their file at all) or it is silently swallowed and the orphan leaks anyway.

The reconciled pattern removes the atomicity requirement entirely. On row purge, enqueue the blob key for reclaim **in the same local DB transaction that removes the row** — a reclaim-queue row (a transactional-outbox entry). That insert and the row delete are one atomic local commit. The actual object-store delete becomes an *at-least-once async step* drained by a reclaim worker that runs under the existing scheduled-task distributed lock (`SCHED-LOCK-001`, `blueprints/scheduled-task-manifest.yaml#lock`), and an object-store delete of an already-absent key is treated as success (idempotent). An inline delete MAY still be attempted as a fast path, but its failure is *swallowed-then-enqueued*, never surfaced as a 500.

**Incorrect — fire-and-forget inline storage delete inside the transaction; a storage failure either 500s the row delete or silently leaks the blob:**

```java
@Transactional
public void deleteFile(UUID fileId, UUID callerId) {
    StoredFile f = repo.findByIdAndOwner(fileId, callerId)
            .orElseThrow(() -> new EntityNotFoundException(fileId));
    repo.delete(f);                       // DB row gone
    s3.deleteObject(bucket, f.getStorageKey());  // ❌ dual write:
    //  - if this throws → 500, the whole tx rolls back, user can never delete
    //  - if you wrap it in try/catch and swallow → orphan blob leaks forever
    //  - no record that the key still needs reclaiming
}
```

**Correct — enqueue the reclaim key in the same local transaction; an async worker drains it idempotently under the scheduled-task lock:**

```java
@Transactional
public void deleteFile(UUID fileId, UUID callerId) {
    StoredFile f = repo.findByIdAndOwner(fileId, callerId)
            .orElseThrow(() -> new EntityNotFoundException(fileId));
    repo.delete(f);                                   // (1) row removed
    reclaimQueue.save(BlobReclaim.of(f.getStorageKey()));  // (2) SAME local tx — atomic with (1)
    // inline fast path is optional and its failure is harmless:
    try { s3.deleteObject(bucket, f.getStorageKey()); reclaimQueue.markDone(f.getStorageKey()); }
    catch (RuntimeException swallow) { /* left PENDING — the worker will reconcile */ }
}

// Async reclaim worker — runs under the scheduled-task distributed lock (SCHED-LOCK-001),
// idempotent: an already-absent key is a no-op success.
@Scheduled(cron = "${ax.recon.reclaim-cron:0 */5 * * * ?}")
public void drainReclaimQueue() {
    lockingPolicy.executeWithLock("blob-reclaim", () -> {
        for (BlobReclaim r : reclaimQueue.findPending(BATCH)) {
            s3.deleteObjectIfPresent(bucket, r.getKey());  // absent key → success (idempotent)
            reclaimQueue.markDone(r.getKey());
            metrics.counter("storage.recon.reclaimed.total", "domain", domain).increment();
        }
    });
}
```

The reclaim worker is one half of reconciliation; the spec's reverse sweep (`RECON-ORPHAN-001`, purge no-referent blobs past a bounded grace window) and forward sweep (`RECON-MISSING-001`, quarantine rows whose blob is gone — never a raw `NoSuchKey` 500) close the remaining drift, all composing the same scheduled-task lock + soft-delete grace window for idempotency (`RECON-IDEMPOTENT-001`). The orphan/missing/reclaimed metric triple (`RECON-OBSERVABILITY-001`) makes residual drift measurable.

Verification (review-tier): confirm by inspection that (a) the blob key is persisted to a reclaim queue in the **same** `@Transactional` boundary as `repo.delete(...)`; (b) no inline `s3.deleteObject` can propagate an exception that rolls back the row removal; (c) the reclaim worker runs under the scheduled-task `LockingPolicy` and treats an absent key as success. This is a structural/composition property of the persistence + scheduling seam, not a single assertable runtime value — hence `type: review`, bound per `rule_verification_binding_guard.sh`.

Reference: [Chris Richardson — Pattern: Transactional outbox (the dual-write problem)](https://microservices.io/patterns/data/transactional-outbox.html)

Reference: [AWS — Deleting Amazon S3 objects (S3 Lifecycle reclaim + idempotent delete)](https://docs.aws.amazon.com/AmazonS3/latest/userguide/DeletingObjects.html)
