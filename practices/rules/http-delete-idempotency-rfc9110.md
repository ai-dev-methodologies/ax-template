---
title: DELETE endpoints MUST be idempotent — second call on absent target returns 204, not 404
impact: MEDIUM
impactDescription: "Non-idempotent DELETE causes client retry loops on network failures + breaks RFC 9110 contract"
tags:
  - api
  - http
  - idempotency
  - retry-safety
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-CRUD-002"
verification:
  gradle_task: testFavorites
  tag: FAV-CRUD-002
upstream:
  - "https://www.rfc-editor.org/rfc/rfc9110.html#name-delete"
evidence:
  - source_type: external
    citation: "RFC 9110 §9.3.5 — HTTP DELETE method idempotency"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-delete"
    quote: "The DELETE method requests deletion of the resource identified by the request target."
    quoted_at: "2026-05-22"
  - source_type: external
    citation: "RFC 9110 §9.2.2 — Idempotent Methods"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-idempotent-methods"
    quote: "A request method is considered idempotent if the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request."
    quoted_at: "2026-05-22"
---

## DELETE endpoints MUST be idempotent — second call on absent target returns 204, not 404

**Impact: MEDIUM — Non-idempotent DELETE causes client retry loops and breaks the HTTP contract**

RFC 9110 §9.2.2 specifies DELETE as one of three idempotent methods. The contract: "the intended effect on the server of multiple identical requests with that method is the same as the effect for a single such request." A DELETE that returns 404 on a second call has *observably different* server effects between calls — the client sees success then failure — which is the literal definition of non-idempotency.

Practically: every production network retries on connection reset, 502, gateway timeout. If the first DELETE succeeded but the response was lost, the client retries. If the server returns 404 on the retry, the client thinks the operation failed and either errors loudly or surfaces a confusing "already gone" state. The catalog pattern (tag-categorization R32, favorites R34, session-management R33 revoke, comment-thread R36 soft-delete) returns 204 unconditionally — the resource is gone, whether the first call did the work or not.

**Incorrect — DELETE returns 404 on second call:**

```java
@DeleteMapping("/api/favorites/{id}")
public ResponseEntity<Void> remove(@PathVariable UUID id) {
    Favorite f = repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));   // ← 404 on retry
    repo.delete(f);
    return ResponseEntity.noContent().build();
}
```

A client whose first response was lost will retry, get 404, and think the operation failed.

**Correct — DELETE returns 204 whether the row existed or not:**

```java
@DeleteMapping("/api/favorites/{entityType}/{entityId}")
public ResponseEntity<Void> remove(Authentication auth,
                                    @PathVariable String entityType,
                                    @PathVariable String entityId) {
    service.remove(auth.getName(), entityType, entityId);  // matches 0 or 1 rows; both → 204
    return ResponseEntity.noContent().build();
}
```

The service issues a `DELETE WHERE …` and discards the row-count. RFC 9110's idempotency contract is satisfied: client retries do not change the observable result.

**Exception — when 404 is semantically required**: hard-delete on a resource the caller is expected to own (e.g. revoke API key — the caller is acting on a specific id they presumably know exists). In these cases the second call STILL returns 204 if you treat the deletion as idempotent. If the resource was never the caller's, return 404 once (IDOR-safe). The principle: *idempotency is about the server effect*, not about whether the caller is allowed to know the row's history.

**Soft-delete corollary**: when DELETE is implemented as status-flip (e.g. comment-thread soft-delete), the second call observes status already DELETED, leaves `deletedAt` unchanged, and returns 204. The state is identical to the post-first-call state — the definition of idempotent.

Reference: [RFC 9110 §9.3.5 — HTTP DELETE](https://www.rfc-editor.org/rfc/rfc9110.html#name-delete)

Reference: [RFC 9110 §9.2.2 — Idempotent Methods](https://www.rfc-editor.org/rfc/rfc9110.html#name-idempotent-methods)
