---
title: A generated statement's identity MUST be a deterministic content hash of (subject, period, basis) — never a client-supplied idempotency header — so an identical regeneration returns the SAME row and a changed basis appends a new version, and the statement's columns stay immutable once written
impact: HIGH
impactDescription: "A statement-generation endpoint that relies on a client-supplied idempotency key (or nothing at all) to prevent duplicates produces duplicate authoritative artifacts under retry/at-least-once delivery — a client double-charged, double-notified, or shown two conflicting statements for the same period. Updating an existing statement's content in place on regeneration destroys the record of what the subject was actually told at generation time"
tags:
  - idempotency
  - audit
  - conservation
spec_ref: "specs/derived-statement-l0.yaml#STMT-DERIVE-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/derivedstatement/DerivedStatementService.java + backend/src/main/java/com/ax/template/authblueprint/derivedstatement/DerivedStatement.java"
  pattern: "basisHash = SHA-256 of the canonicalized basis line items; a lookup by (subject, period, basisHash) returns the EXISTING row verbatim (no insert) when found; a changed basis appends a NEW version, the prior row's columns untouched; a DB UNIQUE(subject, period, basis_hash) backstop catches a benign concurrent-identical-submit race (DataIntegrityViolationException → re-fetch the winner, never a 500); every statement column is @Column(updatable=false), no public setter exists on the entity"
upstream:
  - "https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2"
evidence:
  - source_type: external
    citation: "RFC 9110: HTTP Semantics, §9.2.2 Idempotent Methods — IETF (the general idempotency contract POST is not automatically granted by the HTTP method itself)"
    url: "https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2"
    quote: "the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request"
    quoted_at: "2026-07-14"
---

## A generated statement's identity is its content, not a client's idempotency header

**Impact: HIGH — relying on a client-supplied idempotency key (or nothing) to prevent duplicate statement generation produces duplicate authoritative artifacts under retry; mutating a generated statement in place on regeneration destroys the record of what was actually issued.**

RFC 9110 §9.2.2 defines idempotency as a property of certain HTTP methods (PUT, DELETE, safe methods): *"the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request."* POST — the natural verb for "generate a statement" — is not among them. A generation endpoint that wants retry-safety cannot borrow it from the HTTP method; it has to build it in.

The catalog already established the pattern for a COMPUTATION run's identity: `external-reconciliation-l0`'s `UNIQUE(source_key, feed_snapshot_hash)` and `remeasurement-trueup-l0`'s `TUP-RUNVERSION-001` basis-hash both key a run by the content that produced it, not a client header. This rule generalizes the identical discipline to a GENERATED DOCUMENT: the statement itself is the hash-identified row.

**Incorrect — a client-supplied idempotency key (or nothing) guards against duplicate generation:**

```java
// <!-- catalog-example-ok: StatementService — illustrative anti-pattern, not a shipped symbol -->
@PostMapping("/api/statements")
public Statement generate(@RequestHeader(required = false) String idempotencyKey, @RequestBody GenerateReq req) {
    if (idempotencyKey != null && seen.contains(idempotencyKey)) {
        return repo.findByIdempotencyKey(idempotencyKey);   // ❌ trusts a CLIENT-supplied token
    }
    // ❌ no idempotency key at all, or a client that forgets to send one → duplicate statement rows
    return repo.save(new Statement(req.subject(), req.period(), computeTotal(req.basis())));
}
```

**Correct — the statement's own content hash is its identity; no client header involved:**

```java
@Transactional
public DerivedStatement generate(String subject, String period, List<LineItem> basis) {
    String basisHash = sha256(canonicalize(basis));
    var existing = statements.findBySubjectAndPeriodAndBasisHash(subject, period, basisHash);
    if (existing.isPresent()) {
        return existing.get();                              // STMT-DERIVE-001 — identical inputs, same row
    }
    int nextVersion = statements.findTopBySubjectAndPeriodOrderByVersionDesc(subject, period)
        .map(s -> s.getVersion() + 1).orElse(1);
    try {
        return statements.save(new DerivedStatement(UUID.randomUUID(), subject, period,
            basisHash, canonicalize(basis), total(basis), nextVersion, Instant.now(clock)));
    } catch (DataIntegrityViolationException raced) {
        // STMT-RETRY-002 — a concurrent identical submit won the uq(subject, period, basis_hash) race
        return statements.findBySubjectAndPeriodAndBasisHash(subject, period, basisHash)
            .orElseThrow(DerivedStatementException::notFound);
    }
}
```

**1. Derived identity (STMT-DERIVE-001).** The statement's key is a hash of what it was generated FROM, not a value either the client or the server invents separately — identical (subject, period, basis) can only ever resolve to one statement.

**2. Retry safety by construction (STMT-RETRY-002).** No idempotency header is read or required. A double-submit of the identical request is caught by the SAME hash-lookup path a first-time generation uses — there is no separate "dedup" code path to forget to call.

**3. Immutability (STMT-IMMUTABLE-003).** Every column is `updatable=false`; there is no update path. A changed basis produces a NEW version row; the prior version's content is untouched and permanently fetchable.

Verification: review-tier — confirm the lookup-before-insert happens on the (subject, period, basisHash) tuple with no client-supplied token in the path, the DB unique constraint backstops the identity, and the entity carries no update path.

Reference: [RFC 9110 §9.2.2 — Idempotent Methods](https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2)
