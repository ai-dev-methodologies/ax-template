---
title: A bulk endpoint MUST report per-item partial success (207-style), declare its atomicity mode, cap batch size, and pre-validate before mutating
impact: HIGH
impactDescription: "A bulk endpoint that returns one 200/500 for the whole batch hides which items succeeded and which failed — the client cannot tell what to retry, and a retry of the whole batch re-applies the items that already succeeded. Returning a per-item status array, declaring whether the batch is all-or-nothing or best-effort, and pre-validating before any mutation makes a partial failure recoverable instead of corrupting."
tags:
  - bulk-operation
  - partial-success
  - multi-status
  - rfc-9457
  - batch
  - atomicity
spec_ref: "specs/bulk-operation-l0.yaml#BULK-PARTIAL-001"
verification:
  type: review
  source: "specs/bulk-operation-l0.yaml#BULK-PARTIAL-001"
  pattern: "A batch endpoint MUST return per-item results — a status array carrying each item's outcome (success/failure + an RFC 9457 problem object per failed item), the 207-Multi-Status posture — never a single batch-wide 200/500 that hides which items failed (BULK-PARTIAL-001). The batch envelope MUST enforce a max-batch-size limit (BULK-SUBMIT-001). The endpoint MUST DECLARE its atomicity mode — transactional (all-or-nothing: any item failure rolls back the whole batch) OR best-effort (each item independent) — and behave accordingly; an undeclared/ambiguous mode is forbidden (BULK-ATOMICITY-001). Accepted input formats are declared (JSON array / JSONL / CSV RFC 4180) (BULK-FORMAT-001). A large batch is handed off to an async job returning a job handle rather than blocking the request (composes job-queue) (BULK-ASYNC-001). A pre-validation pass reports ALL item errors BEFORE any mutation, so a best-effort batch does not partially apply then fail on a malformed later item (BULK-VALIDATION-001). Reject a batch-wide single status that masks per-item outcomes, an unbounded batch size, and a best-effort batch that mutates before validating."
upstream:
  - "https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/207"
  - "https://www.rfc-editor.org/rfc/rfc9457"
evidence:
  - source_type: external
    citation: "MDN Web Docs — 207 Multi-Status (mixture of responses)"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/207"
    quote: "The HTTP 207 Multi-Status successful response status code indicates a mixture of responses."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "MDN Web Docs — 207 Multi-Status (body lists individual response codes)"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/207"
    quote: "The response body is a `text/xml` or `application/xml` HTTP entity with a `multistatus` root element that lists individual response codes."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A bulk endpoint MUST report per-item partial success, declare atomicity, cap size, and pre-validate

**Impact: HIGH — A bulk endpoint processes N items, and N items can have N different outcomes. The 207 Multi-Status code exists precisely for this — per MDN, *the HTTP 207 Multi-Status successful response status code indicates a mixture of responses*, and the body *lists individual response codes*. An endpoint that instead collapses the batch into one 200 (or one 500) hides which items succeeded: the client cannot tell what to retry, and a blind retry of the whole batch re-applies every item that already succeeded — duplicating exactly the work the partial failure should have let it skip. Per-item results, a declared atomicity mode, a size cap, and pre-validation make bulk operations recoverable.**

There are six load-bearing requirements — the items of `specs/bulk-operation-l0.yaml`, all governed by this rule.

**1. Per-item partial-success array (BULK-PARTIAL-001).** The response carries a per-item status array: each item reports success or failure, and each failure carries an RFC 9457 problem object (type/title/detail) identifying what went wrong for THAT item — the 207 posture. A single batch-wide status is forbidden when items can independently fail.

**2. Envelope + max batch size (BULK-SUBMIT-001).** The request is a declared envelope with a maximum batch size; an over-limit batch is rejected (413/422) rather than accepted and OOM-ing the server.

**3. Declared atomicity mode (BULK-ATOMICITY-001).** The endpoint declares whether it is transactional (all-or-nothing — any failure rolls back the whole batch) or best-effort (each item independent), and behaves exactly so. An ambiguous mode leaves the client unable to reason about the result of a partial failure.

**4. Declared input formats (BULK-FORMAT-001).** Accepted formats are explicit — JSON array, JSONL, or CSV (RFC 4180) — validated on the way in.

**5. Async hand-off for large batches (BULK-ASYNC-001).** A large batch is handed to an async job returning a job handle (status pollable), not processed inline holding the request thread open. Composes `job-queue`.

**6. Pre-validation before mutation (BULK-VALIDATION-001).** A validation pass reports ALL item errors BEFORE any mutation runs. Without it, a best-effort batch can mutate items 1..k, then hit a malformed item k+1, leaving a half-applied batch whose successful mutations were avoidable.

**Incorrect — one batch-wide status; mutates as it goes; a late bad item leaves a half-applied batch:**

```java
@PostMapping("/users/bulk")
public ResponseEntity<Void> bulk(@RequestBody List<CreateUser> items) {
    for (CreateUser u : items) userService.create(u);  // VIOLATION: mutates before validating all (BULK-VALIDATION-001)
    return ResponseEntity.ok().build();                // VIOLATION: one status hides per-item outcomes (BULK-PARTIAL-001)
    // a malformed item halfway throws 500 — items before it are already committed, client cannot tell which
}
```

**Correct — size-capped envelope, pre-validate all, per-item 207-style result, declared best-effort mode:**

```java
@PostMapping("/users/bulk")
public ResponseEntity<BulkResult> bulk(@RequestBody @Valid BulkEnvelope<CreateUser> req) {
    if (req.items().size() > MAX_BATCH) return ResponseEntity.status(413).build(); // size cap (BULK-SUBMIT-001)
    List<ItemError> errors = validateAll(req.items());   // pre-validate BEFORE mutating (BULK-VALIDATION-001)
    List<ItemResult> results = new ArrayList<>();
    for (int i = 0; i < req.items().size(); i++) {       // best-effort: each item independent (BULK-ATOMICITY-001 declared)
        try { results.add(ItemResult.ok(i, userService.create(req.items().get(i)))); }
        catch (DomainException e) { results.add(ItemResult.failed(i, ProblemDetail.from(e))); } // RFC 9457 per item
    }
    return ResponseEntity.status(207).body(new BulkResult(results)); // per-item outcomes (BULK-PARTIAL-001)
}
```

Verification: review-tier. Partial-success handling is an API-contract property — a batch-wide-status endpoint compiles and works when every item succeeds, hiding the gap only when one fails. Verify by review against `specs/bulk-operation-l0.yaml`: per-item status array with RFC 9457 per failure; max batch size enforced; atomicity mode declared and honored; input formats declared; large batches async with a job handle; all items validated before any mutation. When a fork-receiver wires a real IT (a batch with one bad item → 207 with that item failed and the rest applied, or full rollback in transactional mode), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [MDN — 207 Multi-Status](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/207)

Reference: [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
