---
title: Large file uploads MUST be resumable by byte offset (tus-style) with per-chunk integrity, size/type allowlist, and session expiry cleanup
impact: MEDIUM
impactDescription: "A non-resumable upload restarts from byte 0 on any network drop — on a flaky mobile connection a large file may never complete. An upload with no offset validation can interleave or gap chunks into a corrupt file; with no checksum a flipped byte is silently persisted; with no size/type allowlist it is an unbounded-write and malware vector; with no session expiry, abandoned partials accumulate forever."
tags:
  - upload
  - resumable
  - tus
  - integrity
  - file-storage
  - cleanup
spec_ref: "specs/multipart-upload-l0.yaml#UPLOAD-CHUNK-001"
verification:
  type: review
  source: "specs/multipart-upload-l0.yaml#UPLOAD-CHUNK-001"
  pattern: "A large/resumable upload MUST append chunks at a server-validated byte offset: each chunk declares its Upload-Offset and the server accepts it only if it equals the current persisted offset — a mismatched offset is rejected (409), never blindly appended (no gaps, no overlap) (UPLOAD-CHUNK-001). An upload session is created up front declaring the total length (UPLOAD-INIT-001). A HEAD request returns the current offset so a client can resume after an interruption (UPLOAD-RESUME-001). Each chunk passes a per-chunk integrity gate (checksum) before it is committed (UPLOAD-CHECKSUM-001). The server enforces a maximum size and a content-type allowlist (UPLOAD-LIMIT-001). Sessions have a TTL and abandoned partials are cleaned up (UPLOAD-EXPIRY-001, composes storage-reconciliation). Reject an upload that restarts from zero on resume, that appends a chunk without offset validation, that skips the checksum, or that has no size/type bound."
upstream:
  - "https://tus.io/protocols/resumable-upload"
  - "https://www.rfc-editor.org/rfc/rfc9110"
evidence:
  - source_type: external
    citation: "tus resumable upload protocol 1.0.0 — mechanism"
    url: "https://tus.io/protocols/resumable-upload"
    quote: "The protocol provides a mechanism for resumable file uploads via HTTP (RFC 9110)."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "tus resumable upload protocol 1.0.0 — HEAD returns resume offset"
    url: "https://tus.io/protocols/resumable-upload"
    quote: "A `HEAD` request is used to determine the offset at which the upload should be continued."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "tus resumable upload protocol 1.0.0 — Upload-Offset header"
    url: "https://tus.io/protocols/resumable-upload"
    quote: "The `Upload-Offset` request and response header indicates a byte offset within a resource. The value MUST be a non-negative integer."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## Large file uploads MUST be resumable by byte offset with per-chunk integrity, size/type allowlist, and expiry cleanup

**Impact: MEDIUM — A large upload over a real-world (mobile, lossy) connection will be interrupted, and a non-resumable upload restarts from byte 0 every time — a big file may never finish. The tus protocol solves this: it *provides a mechanism for resumable file uploads via HTTP (RFC 9110)*, where *a HEAD request is used to determine the offset at which the upload should be continued* and *the Upload-Offset ... header indicates a byte offset within a resource*. The byte offset is also what keeps the assembled file correct: validate it and chunks append contiguously; ignore it and chunks gap or overlap into a corrupt file. Add a per-chunk checksum, a size/type allowlist, and session expiry and the upload is both resumable and safe.**

There are six load-bearing requirements — the items of `specs/multipart-upload-l0.yaml`, all governed by this rule.

**1. Offset-validated chunk append (UPLOAD-CHUNK-001).** Each chunk carries its `Upload-Offset`; the server appends it ONLY if that offset equals the current persisted offset, else rejects with 409 Conflict. This guarantees contiguity — no gaps, no overlap — regardless of client retries or reordering.

**2. Session init with total length (UPLOAD-INIT-001).** An upload session is created up front, declaring the total length, returning a session id/URL the client uploads chunks against.

**3. HEAD returns offset for resume (UPLOAD-RESUME-001).** After an interruption, a `HEAD` on the session returns the current `Upload-Offset` so the client resumes from exactly where it stopped — never from zero.

**4. Per-chunk integrity gate (UPLOAD-CHECKSUM-001).** Each chunk is checksummed and verified before commit, so a corrupted chunk is rejected (and re-sent) rather than silently assembled into the file.

**5. Size + content-type allowlist (UPLOAD-LIMIT-001).** The server enforces a maximum total size and a content-type allowlist — an upload is otherwise an unbounded-write DoS and a malware-ingress vector.

**6. Session TTL + cleanup (UPLOAD-EXPIRY-001).** Sessions expire after a TTL and abandoned partial uploads are reclaimed, so incomplete blobs do not accumulate forever. Composes `storage-reconciliation`.

**Incorrect — appends blindly with no offset validation and no resume; a dropped connection corrupts or restarts:**

```java
@PostMapping("/upload/{id}")
public void chunk(@PathVariable String id, InputStream chunk) {
    blob.append(id, chunk.readAllBytes());   // VIOLATION: no Upload-Offset check → retried/reordered chunk gaps or overlaps
    // VIOLATION: no HEAD-offset resume → client that dropped must restart from 0
    // VIOLATION: no checksum, no size/type limit
}
```

**Correct — offset-validated append, HEAD resume, checksum, size/type limit, TTL cleanup:**

```java
@PostMapping("/upload/{id}")                  // tus-style PATCH/append
public ResponseEntity<Void> chunk(@PathVariable String id,
        @RequestHeader("Upload-Offset") long offset,
        @RequestHeader("Upload-Checksum") String checksum, byte[] body) {
    UploadSession s = sessions.get(id);
    if (offset != s.currentOffset())          // offset must match (UPLOAD-CHUNK-001)
        return ResponseEntity.status(409).build();
    if (!integrity.matches(body, checksum))   // per-chunk integrity (UPLOAD-CHECKSUM-001)
        return ResponseEntity.status(460).build();
    if (s.currentOffset() + body.length > MAX_SIZE) // size bound (UPLOAD-LIMIT-001)
        return ResponseEntity.status(413).build();
    s.append(body);
    return ResponseEntity.noContent().header("Upload-Offset", String.valueOf(s.currentOffset())).build();
}
@RequestMapping(method = HEAD, value = "/upload/{id}")  // resume (UPLOAD-RESUME-001)
public ResponseEntity<Void> head(@PathVariable String id) {
    return ResponseEntity.ok().header("Upload-Offset", String.valueOf(sessions.get(id).currentOffset())).build();
}
// init declares total length (UPLOAD-INIT-001); a scheduled sweep expires abandoned sessions (UPLOAD-EXPIRY-001).
```

Verification: review-tier. Resumability and offset-correctness are runtime properties — a blind-append upload works for a single clean connection and corrupts only under retry/reorder/interruption. Verify by review against `specs/multipart-upload-l0.yaml`: chunks append only at a matching validated offset (409 on mismatch); a session declares total length; HEAD returns the resume offset; each chunk is checksum-gated; size and content-type are bounded by an allowlist; sessions expire and partials are cleaned up. When a fork-receiver wires a real IT (interrupt mid-upload, resume via HEAD offset, assert the assembled file matches; a wrong-offset chunk → 409), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [tus — resumable upload protocol](https://tus.io/protocols/resumable-upload)
