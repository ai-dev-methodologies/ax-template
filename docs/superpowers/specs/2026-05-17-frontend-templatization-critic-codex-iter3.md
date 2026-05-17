# Codex Critic Review - 2026-05-17 frontend templatization PRD (Iteration 3)

## Verdict: ITERATE

Iter3 closes the route-level `frontend_only` gap I raised in iter2 and leaves the iter1 blocker fixes intact, but I cannot approve yet because the new guard algorithm is asymmetric: it validates `frontend_only` routes strictly, while page-compliance items are checked only when their `backend_operation_id` is already null. That leaves a narrow fake-backend escape in `specs/practices-frontend-l0.yaml`, exactly the class of provenance ambiguity iter3 was meant to eliminate.

## Patch verification (5 trigger items)

1. `frontend_only` mode in section 4.8.2/section 4.8.4/allowlist: PARTIAL - section 4.8.1 makes `backend_operation_id` nullable and adds `static_source_ref` at `prd.iter3.md:188-198`; section 4.8.2 mirrors the route-level flexibility at `prd.iter3.md:217-253`; the allowlist schema has the 3-class enum and marks `practices: frontend_only` at `prd.iter3.md:333-359`. The blocker is item-level mutual exclusion: section 4.8.4 applies item checks only to items whose `backend_operation_id` is null at `prd.iter3.md:323-325`, so non-null page items in a `frontend_only` domain are not rejected.
2. `practices` marked frontend_only OR SP11 revised: CLOSED - `practices: frontend_only` is explicit at `prd.iter3.md:355`; SP11 now runs `/ax-guard-trio-integrity --mode frontend_only` at `prd.iter3.md:945`.
3. 3 new fixtures present: CLOSED - `pass_frontend_only_practices/`, `fail_frontend_only_missing_source_ref/`, and `fail_frontend_only_unreachable_route/` are specified with expected exit codes/messages at `prd.iter3.md:383-399`.
4. SP11 row assertion: CLOSED - SP11 now asserts both Playwright reachability and the static-source contract, including `static_source_ref.unresolved_count == 0`, at `prd.iter3.md:945`.
5. iter-2 closed material intact: CLOSED - the diff only touches title/status, section 4.8, SP11, section 9, and the end delta note. Spot checks: placeholder guard ban remains at `prd.iter3.md:118-121`; other SP matrix rows remain intact around `prd.iter3.md:933-944` and `prd.iter3.md:946`; Autonomous Execution Safety remains at `prd.iter3.md:964-1055`; the pre-mortem remains at `prd.iter3.md:1070-1159`.

## Algorithm correctness

- Mutual exclusion (`backend_operation_id` non-null AND `static_source_ref` non-empty -> FAIL?): PARTIAL. For `frontend_only` routes, yes: non-null `backend_operation_id` fails at `prd.iter3.md:314-318`. For `frontend_only` page-compliance items, no: the algorithm checks only items "whose `backend_operation_id` is null" at `prd.iter3.md:323-325`, so a non-null item can evade both backend resolution and static-source validation. The schema prose also weakens the contract by saying `static_source_ref` is "Forbidden / ignored" when `backend_operation_id` is non-null at `prd.iter3.md:197-198` and `prd.iter3.md:231-253`; this should be a FAIL, not ignored.
- Zero-scan preserved: YES - the all-modes zero-scan guard remains explicit at `prd.iter3.md:326-329`, with the pre-mortem hard-threshold check intact at `prd.iter3.md:1079-1090`.
- Glob expansion edge cases: MOSTLY OK - glob expansion is rooted at repo top and fails on zero-file expansion at `prd.iter3.md:319-322`, with section 9 rejecting regex and documenting glob semantics at `prd.iter3.md:1305-1312`. I would not block on perf/cap limits for this PRD, but implementation should avoid shell-eval glob expansion and should cap emitted match diagnostics.

## New attack on iter 3

- Specific criticism: A `frontend_only` Page Compliance Spec item can still carry a fake `backend_operation_id`. Section 4.8.4 skips backend resolution for `frontend_only` domains at `prd.iter3.md:312-313`, then validates every UI route strictly at `prd.iter3.md:314-322`, but applies the "same checks" only to page items whose `backend_operation_id` is already null at `prd.iter3.md:323-325`. Therefore an item like `backend_operation_id: fakeReadPracticeRule` in `specs/practices-frontend-l0.yaml` is not resolved against OpenAPI and is not forced to carry `static_source_ref`. This reintroduces fake-backend pressure at the page-spec layer even though routes are fixed.
- Grade: BLOCKING.
- Required mitigation (if BLOCKING): Change section 4.8.4 frontend_only item logic to mirror route logic for every item, not only null-operation items: require `backend_operation_id: null`, fail non-null with a distinct message such as `frontend_only item has non-null backend_operation_id`, require non-empty `static_source_ref`, and verify each entry resolves to at least one file. Also replace "Forbidden / ignored" in section 4.8.1/4.8.2 with "Forbidden; guard MUST fail" and add or extend a fixture to cover the item-level non-null operation escape.

## Final verdict reasoning

This is a narrow iterate, not a rejection. The iter2 requested patch is mostly present: the allowlist has `frontend_only`, `practices` is assigned to it, SP11 now asserts a static-source signal, the three named fixtures are specified, and the six closed iter1 blockers were not reopened. The remaining defect is confined to section 4.8.4 and related schema wording. Because the PRD is meant to be binary-implementable by `/team`, the item-level bypass should be fixed before final approval.

## ADR-ready content (for Step 6 commit, if APPROVE)

- Decision: Not ready for Step 6 commit yet; approve the `frontend_only` direction but require one final guard-contract patch for strict route and page-item mutual exclusion.
- Drivers: Static frontend domains must be first-class without fake backend OpenAPI operations; guard behavior must be binary and symmetric across UI routes and page-compliance items.
- Alternatives considered: Approve iter3 as-is; require a real `practices` backend API; keep `frontend_only` but only validate routes; require strict `frontend_only` validation for both routes and page items.
- Why chosen: Approving as-is would allow fake operation IDs in page specs. A real backend API contradicts SP11's static viewer design. Strict validation keeps the iter3 architecture and closes the narrow bypass.
- Consequences: One small PRD edit is required in section 4.8.1/4.8.2/4.8.4 plus one fixture extension or new fail fixture.
- Follow-ups: Re-run Critic only on the item-level mutual exclusion patch and any revised fixture text; the route-level `frontend_only` mode, SP11 row, and iter1 closed blockers do not need broad re-review unless touched.
