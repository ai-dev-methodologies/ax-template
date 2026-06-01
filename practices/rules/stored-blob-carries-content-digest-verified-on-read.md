---
title: A stored blob MUST carry a write-time content digest (SHA-256) that the read path re-verifies — a mismatch is a fail-closed error, never a silent serve
impact: HIGH
impactDescription: "An object store can return bit-rotted, partially-written, tampered, or simply the WRONG object's bytes, and without a digest captured at write time and re-checked at read time the application streams the corrupt bytes to the user as if they were authentic — a silent integrity failure that no presence/orphan reconciliation sweep can catch"
tags:
  - storage
  - integrity
  - checksum
  - sha-256
  - object-store
  - fail-closed
spec_ref: "specs/file-storage-l0.yaml#FILE-INTEGRITY-001"
verification:
  type: review
  source: "specs/file-storage-l0.yaml#FILE-INTEGRITY-001"
  pattern: "At write time the SHA-256 of the FINAL persisted byte stream is computed and stored in an immutable contentSha256 column in the SAME transaction as the storage key; the read/download path streams the served bytes through a DigestInputStream (or equivalent) and compares the recomputed SHA-256 against the stored digest BEFORE the bytes reach the client, aborting with an integrity error (no partial/corrupt body flushed) on mismatch. Confirm by inspection that (a) the digest column exists and is non-null + non-updatable, (b) the digest is computed over the bytes that are actually persisted (post re-encode/strip, not the raw upload), and (c) no read path can serve bytes without the comparison succeeding."
upstream:
  - "https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html"
  - "https://cwe.mitre.org/data/definitions/353.html"
evidence:
  - source_type: external
    citation: "AWS — Checking object integrity in Amazon S3 (Amazon S3 User Guide), overview section on checksum validation"
    url: "https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html"
    quote: "With Amazon S3, you can use checksum values to verify the integrity of the data that you upload or download."
    quoted_at: "2026-06-01"
  - source_type: external
    citation: "MITRE — CWE-353: Missing Support for Integrity Check, Extended Description (first paragraph)"
    url: "https://cwe.mitre.org/data/definitions/353.html"
    quote: "If integrity check values or 'checksums' are omitted from a protocol, there is no way of determining if data has been corrupted in transmission."
    quoted_at: "2026-06-01"
---

## A stored blob MUST carry a write-time content digest (SHA-256) that the read path re-verifies — a mismatch is a fail-closed error, never a silent serve

**Impact: HIGH — without a digest captured at write time and re-checked at read time, an object store that returns bit-rotted, truncated, tampered, or simply the wrong object's bytes is indistinguishable from one returning the authentic file, and the application streams the corruption to the user as genuine.**

Applications routinely persist a binary artifact in an external object store (S3 / MinIO / GCS / Azure Blob) and a referencing row in the relational DB: an uploaded document, a generated report export, a payslip or receipt PDF, an avatar image. The presence/orphan reconciliation sweep (`storage-reconciliation-l0.yaml`, `RECON-*`) answers *does the blob still exist and is it still referenced* — but it says nothing about whether the bytes are the **right, unaltered** bytes. Object storage can silently corrupt data in ways that leave the key perfectly present: undetected bit-rot on the underlying media, a partial or truncated write that completed without surfacing an error, a multi-tenant key collision or pipeline bug that returns a *different* object under this key, or deliberate at-rest tampering. CWE-353 (Missing Support for Integrity Check) names this class directly: with no checksum, there is no way to determine whether the data has been altered between write and read. A reconciliation sweep will report everything healthy while the read path streams the corrupt bytes to the user as authentic.

The fix is a content digest bound to the bytes themselves. At write time, compute the SHA-256 of the **final persisted byte stream** — after any re-encode / EXIF-strip transform (`FILE-UPLOAD-004`), so the digest matches what is actually served, not the pre-transform input — and store it in an immutable `contentSha256` column written in the *same* transaction as the storage key. On every read/download, stream the bytes through a `DigestInputStream` and compare the recomputed SHA-256 against the stored digest **before** the body reaches the client. A mismatch is fail-closed: abort with an integrity error (no partial or corrupt body flushed), flag the file for review, and never serve the bytes. The stored-bytes-are-served-iff-their-SHA-256-still-equals-the-write-time-digest invariant is what makes silent corruption *loud*.

**Incorrect — the blob is stored and served with no integrity binding; corruption, truncation, or a wrong-object swap is streamed to the client as authentic:**

```java
@Transactional
public StoredFile store(MultipartFile upload, UUID ownerId) {
    byte[] bytes = reEncodeAndStripExif(upload);   // FILE-UPLOAD-004
    String key = storageKeyFactory.newKey(ownerId);
    objectStore.put(bucket, key, bytes);
    return repo.save(new StoredFile(ownerId, key, bytes.length));  // ❌ no digest recorded
}

public ResponseEntity<StreamingResponseBody> download(UUID fileId, UUID callerId) {
    StoredFile f = repo.findByIdAndOwner(fileId, callerId).orElseThrow();
    InputStream in = objectStore.open(bucket, f.getStorageKey());
    // ❌ whatever the store returns — rotted, truncated, the wrong object — is
    //    streamed straight to the user; the app has no way to know it is wrong
    return ResponseEntity.ok().body(out -> in.transferTo(out));
}
```

**Correct — SHA-256 captured at write time in the same transaction; the read path re-verifies before flushing and fails closed on mismatch:**

```java
@Transactional
public StoredFile store(MultipartFile upload, UUID ownerId) {
    byte[] bytes = reEncodeAndStripExif(upload);            // final persisted bytes
    String sha256 = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(bytes));  // (1) digest of FINAL bytes
    String key = storageKeyFactory.newKey(ownerId);
    objectStore.put(bucket, key, bytes);
    // (2) digest persisted in the SAME tx as the key, immutable column
    return repo.save(new StoredFile(ownerId, key, bytes.length, sha256));
}

public ResponseEntity<StreamingResponseBody> download(UUID fileId, UUID callerId) {
    StoredFile f = repo.findByIdAndOwner(fileId, callerId).orElseThrow();
    byte[] served = objectStore.getBytes(bucket, f.getStorageKey());
    String actual = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(served));   // (3) re-hash served bytes
    if (!MessageDigest.isEqual(actual.getBytes(UTF_8),
                               f.getContentSha256().getBytes(UTF_8))) {
        f.markIntegrityFailed();                                   // (4) fail-closed:
        throw new BlobIntegrityException(f.getId());               //     500 ProblemDetail,
        //     corrupt bytes are NEVER streamed to the caller
    }
    return ResponseEntity.ok().body(out -> out.write(served));     // verified → serve
}
```

The `contentSha256` column is `@Column(updatable=false, nullable=false)` so the recorded digest can never be rewritten to match drifted bytes after the fact. The digest is computed over the bytes that are actually persisted (post re-encode/strip), not the raw upload, so a verified read is an honest statement about the served representation. This composes *under* reconciliation rather than replacing it: `RECON-MISSING-001` catches a row whose blob has vanished, while `FILE-INTEGRITY-001` catches a blob that is present but wrong — the two together cover both existence drift and content drift.

Verification (review-tier): confirm by inspection that (a) a non-null, non-updatable `contentSha256` (or equivalent) column is written in the **same** `@Transactional` boundary as the storage key; (b) the digest is computed over the FINAL persisted byte stream, after any re-encode/strip transform; (c) every read/download path re-hashes the bytes it is about to serve and compares against the stored digest before any body is flushed; and (d) a mismatch aborts with an integrity error and never streams a partial or corrupt body. This is a structural write-then-verify-on-read property spanning the persistence and streaming seams, not a single assertable runtime scalar — hence `type: review`, bound per `rule_verification_binding_guard.sh`.

Reference: [AWS — Checking object integrity in Amazon S3 (SHA-256 checksum validation)](https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html)

Reference: [MITRE — CWE-353: Missing Support for Integrity Check](https://cwe.mitre.org/data/definitions/353.html)
