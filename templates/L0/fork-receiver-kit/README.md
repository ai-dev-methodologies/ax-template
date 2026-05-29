# L0 / fork-receiver-kit — Shared client primitives

**Purpose**: the kit hosts the cross-cutting TS helpers that every L4 frontend
trio needs but the catalog refuses to duplicate. R53 (2026-05-26) extracted the
first three from inline copies that had drifted across 7 L4 verticals; FMW2
(2026-05-30) added four more after the FDW1 frontend dogfood found all 3 personas
hand-rolling the same data-flow seams.

| File | Purpose | Origin |
|---|---|---|
| `use-caller-id.ts` | `useCallerId()` / `useCallerRole()` / `normalizeUserId()` / `sameUser()` — caller identity hooks with prod hard-stop + dev warn. Anchors R47 `rbac-stub-default-fail-closed`. | R53 (7 L4s) |
| `parse-error.ts` | `parseError()` (RFC 9457 ProblemDetail unwrap with Korean PII deny-list + 120-char cap), `CodedError` class (preserves `body.code` for actionable advice), `sanitizeStoredError()` (render-layer scrub for audit/outbox `lastError` columns). | R53 (7 L4s) |
| `entity-key.ts` | `assertSafeEntityRef(entityType, entityId)` — path-segment defense-in-depth guard. Anchors R46 iter2 F6. | R53 (favorites canonical) |
| `parse-field-errors.ts` | `parseFieldErrors(res, codeToField?)` / `extractFieldErrors(body, codeToField?)` — map a ProblemDetail's per-field validation array (`violations` / `errors` / RFC 9457 `invalid-params`) **and** a single-field `code` into `Record<field,message>` for `setError` / `crud-*-form fieldErrors`. The seam parse-error stops short of. | FMW2 (FDW1 r3 #1) |
| `use-url-list-state.ts` | `useUrlListState({ defaultPageSize?, filterKeys? })` — typed page/sort/search/filter ↔ query string with one immutable patcher and reset-page-on-filter. Makes the URL-as-state architecture rule the easy path. | FMW2 (FDW1 r3) |
| `use-idempotency-key.ts` | `useIdempotencyKey()` → `{ key, regenerate }` — domain-neutral `Idempotency-Key` lifecycle (stable across retries; regenerate after success). Pairs with backend `common/IdempotencyKeyStore`. | FMW2 (FDW1 r3) |
| `money.ts` | `toMinorUnits(major)` / `toMajorUnits(minor)` — string-based, round-half-up, bigint-safe; integer minor units, **no float**. Pairs with `currency-amount-precision-explicit` (which mandated this but shipped no helper). | FMW2 (FDW1 r3) |

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
import { parseFieldErrors, extractFieldErrors } from 'templates/L0/fork-receiver-kit/parse-field-errors'
import { useUrlListState } from 'templates/L0/fork-receiver-kit/use-url-list-state'
import { useIdempotencyKey } from 'templates/L0/fork-receiver-kit/use-idempotency-key'
import { toMinorUnits, toMajorUnits } from 'templates/L0/fork-receiver-kit/money'
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

Originally the kit had no test suite of its own (exercised transitively by each
L4's tests). FMW2 grew it past the ~5-helper threshold, so the two PURE helpers
now carry kit-local unit tests: `frontend/tests/fmw2-primitives.vitest.ts`
covers `money` (rounding + round-trip) and `parse-field-errors` (shape
tolerance across `violations`/`errors`/`invalid-params`/`code`). The hook
helpers (`use-caller-id`, `use-url-list-state`, `use-idempotency-key`) remain
exercised by consuming-component tests. Every shipped block is additionally
gated by `lint_own_blocks_guard.sh` (run-all-guards [56]) so the kit satisfies
every `ax/*` rule it asks fork-receivers to follow.
