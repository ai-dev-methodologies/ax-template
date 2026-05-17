# Codex Critic Review — 2026-05-17 frontend templatization PRD (Iteration 4)

## Verdict: APPROVE

Iter4 cleanly closes the sole iter3 defect. The patched `frontend_only` item algorithm now mirrors the route-level checks for every page-compliance item, the schema prose makes the mutual-exclusion behavior binary instead of "ignored", and the new negative fixture covers the item-level fake-backend escape. The iter3 to iter4 diff is limited to the requested §4.8.1, §4.8.2, and §4.8.4 surfaces, with no new ambiguity or fixture-message collision found.

## Patch verification (3 items)
1. Item algorithm mirrors route: CLOSED — §4.8.4 now applies to each item in `specs/<domain>-frontend-l0.yaml`, explicitly with no "applies only to null" exception at lines 328-329. Non-null `backend_operation_id` fails with `frontend_only item has non-null backend_operation_id: <id>` at lines 330-331. Missing or empty `static_source_ref` fails at lines 332-333. Zero-file `static_source_ref` expansion fails with `static_source_ref resolves to zero files: <entry>` at lines 334-337. This is consistent with the route-level sequence at lines 319-327 while using item-specific messages where needed.
2. "Forbidden; guard MUST fail" wording: CLOSED — the old `Forbidden / ignored` wording has zero occurrences in iter4. §4.8.1 now states `Forbidden; guard MUST fail` and `BINARY — there is no soft mode` for page item `static_source_ref` when `backend_operation_id` is non-null at lines 193-200. §4.8.2 does the same for route `static_source_ref` at lines 230-236, and the route validation prose replaces "ignores" with explicit non-zero guard failure at lines 255-258.
3. New fail fixture present: CLOSED — §4.8.4 adds `fail_frontend_only_item_non_null_operation/` at lines 413-420. The fixture requires a `frontend_only` page-compliance item with non-null `backend_operation_id`, expects exit 1, and asserts message `frontend_only item has non-null backend_operation_id: <id>`.

## Scope check
- Out-of-scope edits in iter4: no. The diff hunks are limited to §4.8.1 lines 193-200, §4.8.2 lines 230-258, and §4.8.4 lines 328-338 plus fixture text at lines 393-420.
- New ambiguity introduced: no. The item-level branch now states the check applies to every item and explicitly removes the null-only exception.
- Message string collisions: no. The new item non-null message is distinct from the route non-null message; the item missing-source message is distinct from the route missing-source message; the zero-file message remains shared by design and does not affect the new non-null-operation fixture assertion.

## Final verdict reasoning

APPROVE. The only open iter3 defect was item-level mutual exclusion bypass for `frontend_only` domains. Iter4 closes it directly by making every item require `backend_operation_id: null`, a non-empty `static_source_ref`, and at least one resolved file per entry. The schema prose now treats static-source/backend-operation mutual exclusion as a hard guard failure, and the added fixture captures the exact bypass. No previously closed blocker is reopened.

## ADR (FINAL — for Step 6 commit if APPROVE)
- Decision: approve PRD iter4 as canonical.
- Drivers: eliminate fake-backend pressure in `frontend_only` page-compliance specs; keep route and item validation symmetric; preserve binary guard semantics.
- Alternatives considered: iterate again for broader re-review; rejected because the requested patch is narrow, verified, and does not touch previously closed surfaces outside the approved section set.
- Why chosen: iter4 fully closes the remaining concrete bypass with explicit algorithm text and a targeted failing fixture.
- Consequences: implementation teams can proceed against iter4 as the canonical PRD and must enforce item-level `frontend_only` mutual exclusion exactly as specified.
- Follow-ups: commit iter4 as canonical with ADR, then proceed to `/team-builder` and `/team` autonomous execution.
