# FDW1 — React CRUD-admin-console (frontend) 3-persona dogfood — FIRST FRONTEND WAVE

**Date:** 2026-05-30 · **Workflow:** wf_63becb2f-f58 · 4 agents, 674K tokens, 868s
**Method:** 3 personas (박지영 Junior / 정도윤 Senior / 황태완 특급시니어) built the SAME
CRUD admin console (list+filter+pagination+sort / create+edit form / detail / bulk actions)
in isolated worktrees (node_modules symlinked from main) on the live frontend catalog:
practices-react 86 rules + eslint-plugin-ax 7 rules + 93 L2 blocks + 3 L0 primitives +
crud-frontend-l0 spec. All 3 `complete`. Self-verified via npm run lint + tsc --noEmit + vitest.

## Headline: the frontend is the MIRROR IMAGE of the backend
The backend had real domains + spec-only cross-cutting. The frontend has a rich, demonstrably
composable component kit sitting on an under-built data-flow + enforcement substrate. Measured
4-layer maturity:

| Layer | Maturity | Evidence |
|---|---|---|
| **Presentation (L2 blocks)** | ~85% MATURE | data-table / pagination / search-input / empty-state / bulk-actions-bar / confirm-dialog / column-picker / announce-live / skip-link composed by all 3 with ~zero structural edits. a11y baked in (aria-sort, role=alertdialog, announce-live). |
| **Data-flow / seam** | ~40% IMMATURE | all 3 hand-rolled the SAME 4 seams: server-error→field mapping, URL-state, server-state hooks, bulk partial-success. |
| **Enforcement** | ~55% PARTIAL | 3 error-level ax rules genuinely block, but the flagship `no-array-mutate-on-state` LIED about arr[i]=v coverage + missed .push; the catalog never linted its own blocks. |
| **Spec / verification** | ~50% SHALLOW | crud-frontend-l0 is component-backed but composition.spec only string-matches (no typecheck); the L4 reference has drifted props that pass the contract test yet would not typecheck. |

## 🔴 Most dangerous finding (the analog of IDW4's audit-on-read): a rule that LIES
`ax/no-array-mutate-on-state` JSDoc line 4 claimed it flags `arr[i] = v`, but
`MUTATING_METHODS = {sort,reverse,splice}` with NO AssignmentExpression visitor — so the two
most common React state-mutation bugs (`.push`, `arr[i]=v`) shipped silently at ERROR level
with the doc actively asserting coverage. False assurance is worse than a known gap. (3/3 personas.)
Second: the catalog never lints its OWN blocks (eslint globs only src/**+tests/**), so
column-picker shipped an `ax/no-array-includes-in-loop` violation invisible until copied to src/.

## L0 primitive verdict (2-of-3 held)
- **parse-error.ts — UNANIMOUS WIN.** All 3 routed every client failure through it (RFC9457 unwrap + CodedError + PII scrub).
- **entity-key.ts — WIN.** All 3 used assertSafeEntityRef for BOLA path defense; minor friction: polymorphic (type,id) signature awkward for a single-entity domain.
- **use-caller-id.ts — DUD for vanilla CRUD.** All 3 copied it, NONE found a load-bearing call site; it is a PARALLEL identity source with no bridge to the lib/auth Zustand store (two unreconciled identity stories).
- **CRITICAL GAP:** parse-error returns a flat Error; the kit ships NO field-error extractor (violations[]/CodedError → field map). Every persona hand-rolled it — the #1 completeness gap.

## FMW backlog (frontend iMprovement waves)

### ✅ FMW1 — enforcement-first (DONE, this commit)
1. **Fixed `ax/no-array-mutate-on-state`** — added .push/.pop/.shift/.unshift/.fill + an AssignmentExpression visitor for `arr[i]=v`; reconciled the JSDoc; +6 fixture tests. (THE highest danger×r3×cheapness item.)
2. **`lint_own_blocks_guard.sh` (+run-all-guards [56])** — the catalog now lints its own templates/L2/blocks + L0 with every ax rule at error (`--max-warnings 0`). Caught + FIXED 4 real O(n*m) violations: column-picker, MappingEditor, column-reorder, form-error-summary-extended. Pos/neg tested.
3. **Broadened `ax/no-array-includes-in-loop`** to `for-of` bodies (was iterator-callbacks only); +4 fixtures. (Plain for/while intentionally excluded — false-positive risk.)
4. **vitest RTL cleanup** — `afterEach(cleanup)` in vitest.setup.ts (all 3 personas hit the getMultipleElementsFound DOM-leak footgun). 265 tests still green.
5. **doc==impl alignment** — js-tosorted-immutable.md + js-set-map-lookups.md notes now match the shipped rule scope (the exact "doc lies about impl" failure mode this wave exists to kill).

### ✅ FMW2 — completeness: the 4 hand-rolled seams (DONE, commit 2039ebc)
1. **L0 field-error mapping primitive** (`parse-field-errors.ts`): RFC9457 errors[]/violations[]/CodedError → Record<field,string> + wire crud-create/edit-form `fieldErrors` prop. (#1 r3 gap; harvest persona2 `lib/parse-error` + senior `SERVER_CODE_TO_FIELD`.)
2. **`useUrlListState` L0 hook** (page/sort/search/filter ↔ query string, reset-page-on-filter) + fix advanced-filter-builder's JSDoc reference to the non-existent `useUrlState`. (harvest persona2 `useProductListParams` + persona3 `use-list-params`.)
3. **L2 `bulk-result-panel`** consuming `{id, ok, error}[]` — mirror backend `common.BulkResult` (#37); close the backend/frontend asymmetry.
4. **`useIdempotencyKey` L0 hook** (generate-once + regenerate-on-failure) + `toMinorUnits/toMajorUnits` currency helper (the float-bug the currency rule warns about but ships no helper for).

### ✅ FMW3 — spec/verification depth (DONE, commit b75176b)
1. **Pagination envelope contradiction resolved IN THE REFERENCE**: items/page.tsx now uses canonical PageEnvelope `{data, pagination:{page(0-based),pageSize,totalElements,totalPages,hasMore}}` (backend common/PageEnvelope) instead of the stale Spring `{content,totalElements,…}`. (A repo-wide spec reconcile across crud-ui-manifest/crud-openapi remains a separate, more delicate task.)
2. **Fixed the drifted props in items/page.tsx** (currentPage/totalPages→page/pageSize/total with 0↔1 index, actions→actionsSlot, actionLabel/actionHref→actionSlot) so the reference matches real block signatures + **wired new/edit pages to DEMO the parse-field-errors→fieldErrors seam**.

### Deferred follow-ups
- **tsc-on-L4 enforcement guard** — to mechanically PREVENT future prop drift (the FDW1 root finding). Needs a `templates/*` path-map tsconfig harness; pulls all templates into one compile (surfaces pre-existing template `any`/key noise) → larger infra task than the drift fix itself.
- **Wire `eslint-plugin-react` jsx-key** — needs a new frontend devDependency (dep decision).
- **Repo-wide envelope spec reconcile** (crud-ui-manifest + crud-openapi + crud-frontend-l0) — delicate (spec guards); the reference is now correct, the spec docs lag.

## Verdict
First frontend wave proves the dogfood loop works identically on the React side: the rich L2 kit
is real (85%), but the data-flow seams + enforcement are the convergence ramp — and the single
most dangerous finding (a rule lying about its own coverage) is exactly the kind of defect a
self-reinforcing catalog must catch. FMW1 closed the enforcement lie + made the catalog eat its
own dogfood; FMW2 closes the 4 rule-of-three seam primitives next.
