---
title: Object store and DB MUST be reconciled by scheduled idempotent sweeps — reverse-purge orphan blobs, forward-quarantine dangling rows, never a raw NoSuchKey 500
impact: MEDIUM
impactDescription: "DB rows and object-store blobs drift apart: a purge that deleted the row but not the blob leaves an orphan blob billing forever; a blob lost/never-written leaves a dangling row whose read path 500s on a raw NoSuchKey. Without scheduled reconciliation sweeps the drift accumulates silently — storage cost climbs and reads fail unpredictably — and without sweep idempotency two nodes double-process or a partial run corrupts."
tags:
  - storage
  - reconciliation
  - object-store
  - scheduled-task
  - idempotency
  - garbage-collection
spec_ref: "specs/storage-reconciliation-l0.yaml#RECON-ORPHAN-001"
verification:
  type: review
  source: "specs/storage-reconciliation-l0.yaml#RECON-ORPHAN-001"
  pattern: "Object-store/DB drift MUST be reconciled by scheduled sweeps composing the reclaim-on-purge rule (RECON-DELETE-001). A REVERSE sweep enumerates object-store keys under the domain prefix and purges any blob with NO live DB referent whose last-modified is older than a bounded grace window — never purging a blob inside the grace window (a just-written blob whose row is mid-commit) (RECON-ORPHAN-001). A FORWARD sweep enumerates live DB rows that own a blob and probes the store; a row whose blob is missing is quarantined and its read path returns a controlled RFC 9457 404/422 — NEVER a raw NoSuchKey 500 (RECON-MISSING-001). Both sweeps MUST be idempotent and concurrency-safe by composing two existing primitives: the scheduled-task distributed lock (one runner) and the soft-delete grace window (no premature purge); a no-drift re-run mutates nothing, and an object-store delete of an already-absent key is treated as success (RECON-IDEMPOTENT-001). Reject a reclaim that depends on the inline blob-delete succeeding, a reverse sweep with no grace window, a read path that surfaces a raw NoSuchKey 500, and a sweep that is not lock-guarded."
upstream:
  - "https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html"
  - "https://microservices.io/patterns/data/transactional-outbox.html"
evidence:
  - source_type: external
    citation: "Amazon S3 API Reference — DeleteObject (removes an object)"
    url: "https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html"
    quote: "Removes an object from a bucket."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "Amazon S3 API Reference — DeleteObject (success response)"
    url: "https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html"
    quote: "If the action is successful, the service sends back an HTTP 204 response."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Object store and DB MUST be reconciled by scheduled idempotent sweeps — reverse-purge orphans, forward-quarantine dangling rows

**Impact: MEDIUM — A DB row and its object-store blob are written and deleted by two systems that cannot share one transaction, so they drift. A hard-delete that removed the row but whose blob delete failed leaves an ORPHAN blob — S3's DeleteObject simply *removes an object from a bucket* and *if the action is successful, the service sends back an HTTP 204 response*, but if that call never lands the blob lingers, billing forever. The mirror drift — a row whose blob was never written or was lost — leaves a DANGLING row whose read path throws a raw `NoSuchKey` 500 at users. Neither self-heals; both need scheduled reconciliation sweeps, and the sweeps must be idempotent or two nodes double-process.**

This rule governs the sweep items of `specs/storage-reconciliation-l0.yaml`, composing the reclaim-on-purge rule (`RECON-DELETE-001`, the existing `storage-reclaim-must-be-reconciled` rule), the scheduled-task distributed lock, and the soft-delete grace window.

**1. Reverse sweep — purge orphan blobs past a grace window (RECON-ORPHAN-001).** A scheduled sweep enumerates object-store keys under the domain prefix and purges any blob that has NO live DB referent AND whose last-modified is older than a bounded grace window. The grace window is essential: a blob written seconds ago whose owning row is mid-commit has no referent *yet* and must NOT be purged — only blobs orphaned beyond the window are reclaimed.

**2. Forward sweep — quarantine dangling rows, controlled error (RECON-MISSING-001).** A scheduled sweep enumerates live DB rows that own a blob and probes the store for each key. A row whose blob is missing is quarantined, and the read path for it returns a controlled RFC 9457 `404`/`422` — NEVER a raw `NoSuchKey` 500 leaking the storage internal to the caller.

**3. Sweep idempotency via lock + grace (RECON-IDEMPOTENT-001).** Both sweeps are idempotent and concurrency-safe by COMPOSING two existing primitives: (1) the scheduled-task distributed lock so only one runner executes at a time across nodes, and (2) the soft-delete grace window so nothing is purged prematurely. A no-drift re-run mutates nothing; a delete of an already-absent key is treated as success (idempotent), not an error.

**Incorrect — relies on the inline blob delete; read path 500s on a missing blob; no sweep:**

```java
@Transactional
public void purge(Long id) {
    Doc d = repo.findById(id).orElseThrow();
    s3.deleteObject(bucket, d.key());   // VIOLATION: if this throws, the txn rolls back OR the blob orphans (RECON-DELETE/ORPHAN)
    repo.delete(d);
}
public byte[] read(Long id) {
    return s3.getObject(bucket, repo.findById(id).orElseThrow().key());  // VIOLATION: raw NoSuchKey 500 on a dangling row (RECON-MISSING)
}
// no reverse/forward sweep → orphans and dangles accumulate forever
```

**Correct — reconcile by lock-guarded scheduled sweeps with a grace window; read path returns a controlled 404:**

```java
@Scheduled(fixedDelay = ONE_HOUR)
public void reverseSweep() {                                   // RECON-ORPHAN-001
    schedLock.runExclusively("storage-recon-reverse", () -> {  // distributed lock (RECON-IDEMPOTENT-001)
        for (String key : store.listUnder(PREFIX)) {
            if (!repo.existsByKey(key) && store.lastModified(key).isBefore(now().minus(GRACE)))
                store.deleteIdempotent(key);                   // absent-key delete == success
        }
    });
}
@Scheduled(fixedDelay = ONE_HOUR)
public void forwardSweep() {                                   // RECON-MISSING-001
    schedLock.runExclusively("storage-recon-forward", () ->
        repo.findAllOwningBlob().forEach(d -> { if (!store.exists(d.key())) repo.quarantine(d); }));
}
public byte[] read(Long id) {
    Doc d = repo.findActiveById(id).orElseThrow(NotFound::new); // quarantined → controlled 404/422, not a raw 500
    return store.getObject(d.key());
}
```

Verification: review-tier. Reconciliation is a drift-repair property with no compile-time signal — an inline-delete + raw-read implementation compiles and works until a blob delete fails or a blob goes missing. Verify by review against `specs/storage-reconciliation-l0.yaml`: a reverse sweep purges no-referent blobs only past a grace window; a forward sweep quarantines dangling rows and the read path returns a controlled 404/422 (never a raw NoSuchKey 500); both sweeps run under the scheduled-task distributed lock with the soft-delete grace window and are re-run-safe. When a fork-receiver wires a real IT (orphan blob reclaimed after grace; missing-blob read → 404 not 500), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [Amazon S3 — DeleteObject](https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html)
