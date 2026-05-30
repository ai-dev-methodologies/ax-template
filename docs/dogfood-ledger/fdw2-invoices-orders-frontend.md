# FDW2 → FMW4 — invoices/orders admin (frontend dogfood arc)

Companion to `fdw1-crud-admin-frontend.md`. FDW2 (2nd frontend dogfood,
Workflow `wf_c796f8dd-96b`) had personas build an invoices/orders admin console
on the live React catalog. This ledger records the FDW2 frontier findings and
the **FMW4** improvement waves that closed them.

## FDW2 measured outcome (the loop, proven again)

FDW1's "4 seams 3/3 hand-rolled" → FDW2's **5 primitives + crud-form
`fieldErrors` all 3/3 discovered-and-used**. The CRUD completeness axis
**converged**. Maturity re-measured: presentation 85→88, **data-flow 40→72
(+32)**, enforcement 55→58, spec 50→62. The frontier MOVED from "does the
primitive exist" to **"does the AI entry point ROUTE to it, and does enforcement
BLOCK the wrong path"** — i.e. discoverability + enforcement became the two
lowest layers.

## Findings → FMW4 closures

| # | FDW2 finding | classification | FMW4 closure |
|---|---|---|---|
| F1 | FMW1's name-based lint broadening shipped 2 false-positives that turned `npm run lint` RED on a clean tree (`page.fill()` destructure; per-iteration local `src.includes`) | real_bug | **FMW4a** (`9a3deb3`): scope `no-array-mutate-on-state` to useState-derived names; skip loop-local names in `no-array-includes-in-loop`; drop 4 orphan disables. lint binary again. |
| F2 | The sha-anchored AI sentinel `practices-react/AGENTS.md` names the new L0 kit ~0 times (grep=1) — a context-0 AI entering the designed path never learns the kit exists | methodology_gap | **FMW4b**: generator emits a disk-globbed "Fork-receiver kit (L0) + key seam blocks (L2)" index (kit mentions 1→11). |
| F3 | L4 crud list reference still drove page/sort/search with `React.useState`, ignoring the catalog's own `useUrlListState` — the most-emphasized URL-state rule had no worked example | methodology_gap | **FMW4b**: migrated `templates/L4/crud/.../items/page.tsx` to `useUrlListState`; upgraded the saved-view rule from `verification.type:review` to a binary guard `saved_view_url_state_guard.sh` [57]. |
| F4 | Backend IMW4 emits the full optimistic-lock contract (428/412/409 + `current_etag`) but the frontend had ZERO counterpart — pure frontend-asymmetry (same gap-shape FMW2 closed for BulkResult) | real_bug | **FMW4c**: L0 `use-conflict-resolution` (`classifyConflict`/`parseConflict`/`useConflictResolution`) + L2 `conflict-banner` (your-value vs server-value vs validator). |
| F5 | `money.ts` existed but the JSON wire type (number vs bigint vs string) was undocumented, and there was no form-layer money field nor an inverse list-state serializer | methodology_gap | **FMW4d**: `parseMinor`/`serializeMinor`/`fractionDigitsFor` + wire-type doc; `'money'` FieldType in crud-create/edit-form (emits minor units, RangeError→fieldError); `listStateToQuery` inverse serializer. |

## Enforcement lesson (recursive)

F1 is the FDW1 lesson applied to itself: a **name-based rule needs a
shape/scope guard or it false-positives and erodes trust**. FMW4b's new
`saved_view_url_state_guard.sh` was written with that lesson baked in — it
matches a localStorage **method call** (not the bare word, which the
saved-view block only names in a "FORBIDDEN" JSDoc) and asserts URL-state on a
single **pinned** reference path, so it has zero false positives.

## Verification (FMW4b/c/d)

- `frontend`: `npm run lint` exit 0 · `npm run test` 293 passed (incl. new
  `tests/fmw4-primitives.vitest.ts`, 16) · `npm run build` green.
- `lint_own_blocks_guard.sh` [56]: 102 shipped L0/L2 blocks satisfy every ax/* rule.
- `run-all-guards.sh --include-fixtures`: 100 passed / 0 failed, incl. new
  `saved_view_url_state` [57] (live + fixture_fail + fixture_pass).
- `practices-react/generate_agents.sh` idempotent; sentinel sha reflects the
  saved-view rule edit.

## Still open (next ramp)

- Discoverability is now the lowest layer: does the AI sentinel route to the kit
  *at the moment of need*? (AGENTS index is necessary, not yet proven sufficient.)
- `tsc`-on-`templates/*` guard still deferred (needs a `templates/*` path-map);
  out-of-root template modules are currently type-checked only via fork adoption
  + vitest runtime, not a catalog typecheck gate.
