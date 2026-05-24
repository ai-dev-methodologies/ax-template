---
title: Polymorphic (entityType, entityId) refs MUST be path-segment guarded client-side
impact: MEDIUM
impactDescription: "encodeURIComponent masks injection on the wire but Spring re-decodes before PathVariable matching — a client guard refuses the request before it leaves the browser"
tags:
  - bola
  - defense-in-depth
  - path-injection
  - polymorphic-entity
spec_ref: "specs/favorites-bookmarks-l0.yaml#FAV-VALID-001"
verification:
  source: "templates/L4/favorites-bookmarks/app/entity-key.ts"
  pattern: "assertSafeEntityRef(entityType, entityId) rejects values containing '/', '?', '#', '\\0', '\\', or a leading '.' — called by every fetch that emits the pair as a path segment"
upstream:
  - "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
  - "https://cwe.mitre.org/data/definitions/22.html"
evidence:
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 Broken Object Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
    quote: "Object level authorization is an access control mechanism that is usually implemented at the code level to validate that one user can only access objects that they should have access to."
    quoted_at: "2026-05-25"
  - source_type: external
    citation: "CWE-22 — Improper Limitation of a Pathname to a Restricted Directory ('Path Traversal')"
    url: "https://cwe.mitre.org/data/definitions/22.html"
    quote: "The product uses external input to construct a pathname that is intended to identify a file or directory that is located underneath a restricted parent directory, but the product does not properly neutralize special elements within the pathname that can cause the pathname to resolve to a location that is outside of the restricted directory."
    quoted_at: "2026-05-25"
---

## Polymorphic (entityType, entityId) refs MUST be path-segment guarded client-side

**Impact: MEDIUM — encodeURIComponent is not enough on its own**

When a catalog domain models a polymorphic relationship via an `(entityType, entityId)` pair (favorites-bookmarks, tag-categorization attachments, comment-thread, activity-feed objects), those two strings often end up encoded as path segments in REST URLs:

```
DELETE /api/favorites/{entityType}/{entityId}
GET    /api/comments/by-entity/{entityType}/{entityId}
GET    /api/tags/by-entity/{entityType}/{entityId}
```

`encodeURIComponent(entityType)` correctly percent-encodes `/`, `?`, `#`, and other path-separators on the wire. But Spring MVC (and most web frameworks) decode the URL-encoded path **before** `PathVariable` matching — that's the whole point of percent-encoding. A `entityType` of `'../admin/users'` arrives at the controller as the raw string `../admin/users`. If the controller's spec yaml only enforces `@Size(max = 64)` without a charset constraint (which is the default in the catalog as shipped), the backend has no first-line defense.

The cleanest defense-in-depth pattern is to validate the pair client-side **before** the fetch leaves the browser. The validator is a one-shot helper that throws on any character likely to confuse path resolution:

- `/` (forward slash) — direct path-segment break
- `?` (query) — bumps the value into the query string
- `#` (fragment) — strips the value at the URL parser
- `\0` (NUL) — historic terminator-truncation hazard
- `\` (backslash) — Windows-style separator some frameworks treat as `/`
- leading `.` — combined with `.` makes `..`, the traversal prefix

This is *purely defense-in-depth*. The backend SHOULD constrain charset on the spec yaml field via a regex pattern (`@Pattern(regexp = "[a-zA-Z0-9_-]+")`), and the catalog tracks that as a deferred backend-contract item. But the client guard is free to ship today and closes the attack surface from the only side of the contract a fork-receiver controls.

**Incorrect — only encodeURIComponent, no client-side charset guard:**

```ts
async function removeFavorite(entityType: string, entityId: string) {
  // ❌ encodeURIComponent encodes the path-injection characters on the wire,
  // but Spring decodes them before @PathVariable matching.
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  if (!res.ok) throw new Error('Failed to remove favorite')
}
```

**Correct — client-side assertSafeEntityRef gates the fetch:**

```ts
// app/entity-key.ts
export function assertSafeEntityRef(entityType: string, entityId: string): void {
  for (const [name, value, max] of [
    ['entityType', entityType, 64],
    ['entityId', entityId, 255],
  ] as const) {
    if (!value || value.length === 0) throw new Error(`Invalid ${name}: empty`)
    if (value.length > max) throw new Error(`Invalid ${name}: longer than ${max} characters`)
    if (/[\\/?#\0]/.test(value)) throw new Error(`Invalid ${name}: contains forbidden characters`)
    if (value.startsWith('.')) throw new Error(`Invalid ${name}: cannot start with '.'`)
  }
}

// fetch site:
async function removeFavorite(entityType: string, entityId: string) {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  if (!res.ok) throw await parseError(res, 'Failed to remove favorite')
}
```

The guard belongs in a shared module (one per catalog domain or one shared across the polymorphic-entity-using L4 set) so a fork-receiver replacing a single fetch helper inherits the validation by import, not by copy-paste.

Reference: [OWASP API Security Top 10 (2023) — API1:2023 BOLA](https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/)

Reference: [CWE-22 — Path Traversal](https://cwe.mitre.org/data/definitions/22.html)
