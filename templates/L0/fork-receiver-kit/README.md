# L0 / fork-receiver-kit — Shared client primitives

**Purpose**: the kit hosts the three TS helpers that every L4 frontend trio
needs but the catalog refuses to duplicate. R53 (2026-05-26) extracted these
from inline copies that had drifted across 7 L4 verticals.

| File | Purpose | Replaces inline copies in |
|---|---|---|
| `use-caller-id.ts` | `useCallerId()` / `useCallerRole()` / `normalizeUserId()` / `sameUser()` — caller identity hooks with prod hard-stop + dev warn. Anchors R47 `rbac-stub-default-fail-closed`. | activity-feed, approval-workflow, email-outbox, favorites-bookmarks, scheduled-task, tag-categorization, webhook |
| `parse-error.ts` | `parseError()` (RFC 9457 ProblemDetail unwrap with Korean PII deny-list + 120-char cap), `CodedError` class (preserves `body.code` for actionable advice), `sanitizeStoredError()` (render-layer scrub for audit/outbox `lastError` columns). | same 7 L4s above |
| `entity-key.ts` | `assertSafeEntityRef(entityType, entityId)` — path-segment defense-in-depth guard. Anchors R46 iter2 F6. | favorites-bookmarks (canonical source); reusable by any L4 with polymorphic URLs |

## Why L0?

The existing layer stack (L1 primitives → L2 blocks → L3 page templates → L4
domain verticals) covers UI composition. The fork-receiver-kit sits BELOW L1:
no rendering, no JSX, pure TS helpers that L1+ may use. The {@code L0/} layer
is reserved for cross-cutting client utilities that L4 needs without dragging
L1/L2/L3 in.

## Import convention

L4 templates import via the absolute-style path:

```ts
import { useCallerId, useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError, CodedError, sanitizeStoredError } from 'templates/L0/fork-receiver-kit/parse-error'
import { assertSafeEntityRef } from 'templates/L0/fork-receiver-kit/entity-key'
```

This mirrors the L2 blocks convention (`templates/L2/blocks/confirm-dialog`)
and survives the fork verbatim — fork-receivers replicate the path under
their app's source tree (e.g. `src/templates/L0/...`) and the imports keep
working without rewrites.

## Trade-offs the catalog accepted

1. **L4 self-containment is partially relaxed.** Pre-R53 a fork-receiver
   could copy a single L4 directory and go; post-R53 they ALSO need the kit
   sibling. The kit is small (3 files) and stable — the catalog deems this
   worth the DRY win because divergent fixes across 7+ L4 copies had become a
   maintenance burden (R55 vs R51 vs R44 each carrying slightly different
   PII deny-lists).
2. **FavoritesError is gone — replaced by CodedError.** Favorites-specific
   error code surfaces (`FAVORITES_QUOTA_EXCEEDED`) still narrow the same
   way: `err instanceof CodedError && err.code === 'FAVORITES_QUOTA_EXCEEDED'`.
   The catalog refuses to claim ownership of any one L4's code namespace.
3. **The kit pins NO domain.** Frontmatter `provenance_class: internal_design`,
   no `domain:` field — these helpers are domain-agnostic by construction.

## Sentinel posture

The kit has no `@Tag` test suite of its own; it's exercised transitively by
each L4's existing tests. A regression in `parseError` (e.g. a missing PII
pattern) surfaces when the consuming L4's `errorResponse_doesNotContainRrnField`-
style test fails. Adding kit-local unit tests is deferred until the kit grows
beyond ~5 helpers.
