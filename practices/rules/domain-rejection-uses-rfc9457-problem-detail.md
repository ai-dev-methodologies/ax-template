---
title: A refused domain operation MUST return its declared RFC 9457 problem type with the correct status, no partial side effect, and no misleading Retry-After
impact: MEDIUM
impactDescription: "A domain rejection (capacity exhausted, reorder conflict, quota exceeded) returned as a bare 400/500 or an ad-hoc JSON blob gives the client nothing machine-readable to branch on — it cannot distinguish a retryable conflict from a permanent refusal. Worse, a rejection that has already applied a partial side effect leaves the system in a half-mutated state, and a Retry-After on a non-time-resetting refusal tells the client to retry something that will never succeed."
tags:
  - error-handling
  - rfc-9457
  - problem-details
  - rejection
  - http-status
spec_ref: "specs/bounded-capacity-claim-l0.yaml#CLAIM-REJECT-001"
verification:
  type: review
  source: "specs/bounded-capacity-claim-l0.yaml#CLAIM-REJECT-001"
  pattern: "When a domain operation is REFUSED by a business rule (capacity exhausted, reorder conflict, quota exceeded, ...), the response MUST be an RFC 9457 problem detail carrying the operation's DECLARED problem `type` (a stable urn), `code`, and the correct HTTP status for the refusal class — 409 Conflict for a contended/conflict refusal (CLAIM-REJECT-001 capacity-exhausted; ORDER-CONFLICT-001 reorder-conflict), 429 for a rate/quota refusal (QUOTA-REJECT-001 quota-exceeded, with the diagnostic members resource/limit/current_usage/requested). The refusal MUST NOT have applied a partial side effect — it is all-or-nothing, no half-mutation, no last-write-wins auto-merge (ORDER-CONFLICT-001), no partial consume (QUOTA-REJECT-001). A `Retry-After` header MUST NOT be sent on a refusal that is not time-resetting (a capacity/conflict refusal that retrying immediately will not clear); it is permitted only for a genuinely period-resetting limit. Where a conflict response must let the client recover, it carries the authoritative current state (e.g. the current ordering/version) and the client retries within a bounded budget. Reject a bare 400/500 for a domain refusal, an ad-hoc non-RFC-9457 error body, a refusal that partially mutated, and a Retry-After on a non-resetting refusal."
upstream:
  - "https://www.rfc-editor.org/rfc/rfc9457"
evidence:
  - source_type: external
    citation: "RFC 9457 — Problem Details for HTTP APIs (Abstract)"
    url: "https://www.rfc-editor.org/rfc/rfc9457"
    quote: "This document defines a 'problem detail' to carry machine-readable details of errors in HTTP response content to avoid the need to define new error response formats for HTTP APIs."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A refused domain operation MUST return its declared RFC 9457 problem type with the correct status, no partial side effect, no misleading Retry-After

**Impact: MEDIUM — Across the catalog, many operations can be REFUSED by a business rule: a bounded-capacity claim when the counter is full, a reorder when a concurrent reorder won the race, a quota claim when the limit is hit. RFC 9457 exists precisely so these refusals are machine-readable — it *defines a 'problem detail' to carry machine-readable details of errors in HTTP response content to avoid the need to define new error response formats for HTTP APIs*. A refusal returned as a bare `400`/`500` or an ad-hoc JSON blob forces the client to string-match prose to decide whether to retry. And two subtler bugs travel with bad refusals: a refusal that already applied a partial side effect (decremented one counter before failing the next) corrupts state, and a `Retry-After` on a refusal that retrying will never clear sends the client into a futile loop.**

This rule governs the rejection-response contract that several spec items share — `CLAIM-REJECT-001` (bounded-capacity-claim), `ORDER-CONFLICT-001` (ordered-collection), `QUOTA-REJECT-001` (per-tenant-resource-quota). Each declares its own `type`/`code`/status; this rule mandates the shared discipline they instantiate.

**1. Declared RFC 9457 problem type + code (the envelope).** The refusal body is an RFC 9457 problem detail with the operation's stable declared `type` urn (`urn:problem:capacity-exhausted`, `urn:problem:reorder-conflict`, `urn:problem:quota-exceeded`) and `code`. The client branches on `type`, never on a prose `detail` string.

**2. Correct status for the refusal class.** A contended/conflict refusal is `409 Conflict` (capacity exhausted, reorder lost the race); a rate/quota refusal is `429 Too Many Requests` (quota exceeded — a `402` opt-in is allowed for a billing-gated quota). The status reflects the *kind* of refusal so generic client middleware reacts correctly.

**3. Diagnostic members.** The problem carries the members that let the client act: a quota refusal includes `resource`, `limit`, `current_usage`, `requested`; a reorder conflict includes the parent id and the authoritative current ordering/version so the client can rebase and retry.

**4. No partial side effect.** A refusal is all-or-nothing — it MUST NOT have committed a partial mutation, applied a last-write-wins auto-merge (reorder), or partially consumed (quota). The operation either fully succeeds or fully refuses with the system unchanged.

**5. No misleading Retry-After.** `Retry-After` is sent ONLY for a genuinely time-resetting limit (a per-window quota that refills). A capacity/conflict refusal — which retrying immediately will not clear — MUST NOT carry `Retry-After`. A conflict instead returns the current state and the client retries within a bounded budget.

**Incorrect — bare 500, partial mutation, misleading Retry-After:**

```java
public void claim(String resource) {
    counter.increment(resource);                 // VIOLATION: mutates BEFORE the capacity check (partial side effect)
    if (counter.get(resource) > capacity)
        throw new RuntimeException("full");        // VIOLATION: bare 500, not RFC 9457; no type/code (CLAIM-REJECT-001)
    // a client sees an opaque 500 and a half-applied increment
}
```

**Correct — atomic check, RFC 9457 problem with declared type + 409, no Retry-After on a non-resetting refusal:**

```java
public ClaimResult claim(String resource) {
    int taken = counter.tryClaim(resource, capacity);   // atomic; no mutation if it would exceed (no partial side effect)
    if (taken < 0) {
        throw new CapacityExhaustedException(resource);  // → @ExceptionHandler maps to:
        //   409 Conflict, ProblemDetail{ type=urn:problem:capacity-exhausted, code=CAPACITY_EXHAUSTED, ... }
        //   NO Retry-After (retrying now will not free capacity)  (CLAIM-REJECT-001)
    }
    return ClaimResult.granted(resource);
}
// ORDER-CONFLICT-001 → 409 urn:problem:reorder-conflict + current ordering for rebase; no auto-merge.
// QUOTA-REJECT-001  → 429 urn:problem:quota-exceeded + {resource,limit,current_usage,requested};
//                     Retry-After ONLY if the quota window resets.
```

Verification: review-tier. Refusal-response correctness is an API-contract property with no compile-time signal — a bare-500 refusal compiles and "works" while being unbranchable and sometimes half-applied. Verify by review against the reject items of `bounded-capacity-claim`, `ordered-collection`, and `per-tenant-resource-quota`: refusals return an RFC 9457 problem with the declared type/code; the status matches the refusal class (409 conflict / 429 rate); diagnostic members are present; no partial side effect occurred; Retry-After appears only on a time-resetting limit. When a fork-receiver wires a real IT (claim at capacity → 409 with the type and no row mutated; quota hit → 429 with the members), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
