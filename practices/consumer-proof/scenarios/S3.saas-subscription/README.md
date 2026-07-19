# S3.saas-subscription — consumer-proof scenario

DOGFOOD cell: **SaaS SUBSCRIPTION dashboard UI vertical slice** (plan cards,
usage widget, upgrade CTA) as a Next.js App-Router client route. Composed
from catalog L2 blocks — `PricingTable` (plan cards + the upgrade CTA button
built into each card), `UsageMeter` (usage widget), `BillingHistory` +
`Pagination` (paginated billing-event list) — plus the L0
`use-url-list-state` hook for URL-backed page state, assembled into a thin
`SubscriptionPage` route delegating to a `@/features/subscription`
container.

## What this proves

Four realistic AI-generated rule violations, each with a BLOCKED violating
fixture and a scanned+PASS clean fixture:

| # | Violation | Guard | Asset |
|---|-----------|-------|-------|
| 1 | 150+-line `"use client"` `src/app/subscription/page.tsx` absorbing plan-change/usage/history logic inline | `ax/no-god-route` (`@ax/eslint-plugin-ax`) | **catalog-reused** |
| 2 | `useState(useSWR('/api/subscription/plan', ...).data)` mirroring the current-plan query into local state | `ax/no-server-state-in-local-state` | **catalog-reused** |
| 3 | `.push` on a `useState`-derived tracked-features array, then `setState` with the same reference | `ax/no-array-mutate-on-state` | **catalog-reused** |
| 4 | FE pagination parser guesses a legacy `{items,total,hasNextPage}` shape instead of the REAL BE `PageEnvelope.java` canonical envelope | `scenario-guards/pagination_envelope_contract_parity.sh` | **hand-rolled** |

Violation #4 is the dogfood brief's ADDITIONAL REQUIREMENT: "a cross-boundary
test validating FE pagination-state parsing against the BE PageEnvelope
contract schema in one integrated run."

## Capability-gap signal (assets_handrolled)

**`pagination_envelope_contract_parity.sh`** — confirmed absent from the
catalog, not merely "not found by a quick grep":

- `grep -ril "PageEnvelope\|totalElements\|hasMore" frontend/src` returns
  **nothing** — no FE code in the reference workload consumes the BE's
  canonical `PAGE-OFFSET-001` envelope shape at all.
- `templates/L2/blocks/pagination.tsx` takes flat `{page, pageSize, total}`
  **props** — it is a display component, not a response-envelope parser.
- `templates/L0/fork-receiver-kit/use-url-list-state.ts` owns URL-backed
  page/sort/filter **state** — it never touches a server response body.
- `practices/evals/*.sh` guards never read `frontend/src` at all — there is
  no cross-boundary (BE-schema ↔ FE-parser) check anywhere in the catalog.

So this scenario hand-rolls the check, isolated to this scenario dir,
modeled on the catalog's own text-scanning shell-guard style
(`admin_preauthorize_guard.sh`'s python3-heredoc structural parsing, from the
sibling `S3.b2b-admin` scenario). It is genuinely cross-boundary: it reads
the REAL `backend/src/main/java/.../common/PageEnvelope.java` to derive the
canonical field names **live** (never a hardcoded copy of the shape — if the
BE record ever renames a field, this check's expectations move with it), then
checks that the FE parser under test actually dereferences every one of
those fields (`.data`, `.pagination.page`, `.pagination.pageSize`,
`.pagination.totalElements`, `.pagination.totalPages`,
`.pagination.hasMore`). A parser that instead guesses a flat/legacy shape
dereferences none of them — exactly the drift `PageEnvelope.java`'s own doc
comment warns against ("every domain re-typed the page response and the
shapes DIVERGED").

The other three violations (god-route, server-state, array-mutate) are
**catalog-reused**: `@ax/eslint-plugin-ax`'s AST-shape rules fire on
arbitrary React/TSX regardless of directory (per
`practices/consumer-proof/README.md`'s Lane A description), so this
scenario's fixtures needed no new guard — only a themed, realistic rewrite of
the existing rule shapes for the subscription domain.

## Isolation

Everything lives under this scenario dir (`react/`, `scenario-guards/`) plus
READ-ONLY use of the sibling `practices/consumer-proof/react/node_modules`
(the `@ax/eslint-plugin-ax` install shared with the main harness) and a
READ-ONLY read of the real `backend/src/.../common/PageEnvelope.java` for
violation #4's cross-boundary check. Nothing here edits `backend/src` or
`frontend/src`, and this scenario is NOT wired into `run-all-guards.sh` or
R25 — it is a standalone probe, run manually.

### A note on ESLint's flat-config base-path check

This scenario's React fixtures live outside `practices/consumer-proof/react`
(the scenario is isolated in its own dir), and ESLint 9's flat config
silently ignores (`exit 0`, "File ignored because outside of base path.") a
file outside its config's base path when invoked the way the main harness
invokes it (`cd $REACT_DIR && npx eslint <relpath>`). This scenario's
`run-scenario-proof.sh` instead calls the `$REACT_DIR`-installed eslint
binary directly with an explicit `--config "$REACT_DIR/eslint.config.mjs"`,
which lints the out-of-tree fixture correctly while still reusing the exact
same shared plugin install/config as the main harness — no fixture was
copied into `$REACT_DIR` to work around this.

## Run it

```bash
bash practices/consumer-proof/scenarios/S3.saas-subscription/run-scenario-proof.sh
```

Exit 0 = proof holds (every violating fixture BLOCKED by its intended
signature, every clean fixture scanned + PASS, cardinality gate satisfied).
Exit 1 = proof falsified or a case could not run.
