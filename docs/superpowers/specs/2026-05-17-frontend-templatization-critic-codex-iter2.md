# Codex Critic Review — 2026-05-17 frontend templatization PRD (Iteration 2)

## Verdict: ITERATE

Iter2 closes the six hard blockers from my iter1 review at the requested binary-implementable resolution: guard schemas and fixtures are concrete, placeholder guards are banned, the SP verification matrix is filled, observability signals are named, `/team` safety now has rollback/ownership/stale-state/halt/ESCAPE contracts, and SP9-SP11 false parallelism is resolved. I do not approve yet because iter2 introduced one new blocking defect: the stricter Spec Trio schema cannot represent the in-scope static/non-API practices viewer without either fake backend operations or an unguarded escape. That is a narrow iter3 fix, not a reason to throw away the plan.

## Phase 1 — Closure verification (iter 1 hard blockers)

1. trio guards binary-implementable: CLOSED — §4.8 names concrete schema fields: `backend_operation_id`, `backend_spec_ref`, `coverage_threshold`, and `backend_only_marker` at `prd.iter2.md:181-195`; the UI contract binds routes to `backend_operation_id` at `prd.iter2.md:201-220`; `trio_integrity_guard.sh` has a deterministic algorithm at `prd.iter2.md:241-271`; the allowlist is concrete at `prd.iter2.md:273-283`; fixtures cover `pass/`, `fail_missing_frontend_yaml/`, `fail_unresolved_operation_id/`, `fail_coverage_shortfall/`, and `fail_zero_scan/` at `prd.iter2.md:285-297`. `cross_trio_guard.sh` is similarly specified with zero-scan and fixtures at `prd.iter2.md:299-325`. The coverage threshold is numeric: required ratio 100% at `prd.iter2.md:263-268`, not "appropriate level."

2. placeholder `exit 0` guard stubs banned: CLOSED — the principle explicitly bans placeholder `exit 0` guard stubs at `prd.iter2.md:31-33`; §3.2 repeats the ban and forbids iter1's SP1 placeholder plan at `prd.iter2.md:118-121`; the dependency graph now starts with SP3 at `prd.iter2.md:461-486`; SP1 explicitly lands no guard or skill stubs at `prd.iter2.md:545-546`.

3. all SP TDD anchors concrete: CLOSED — the Verification Matrix is present at `prd.iter2.md:824-844` with `verify_skill`, `script_path`, `test_file`, `assertion`, `expected_RED_reason`, `first_green_command`, and `observability_signal`. The previously circular SPs are fixed: SP2 at `prd.iter2.md:833`, SP4a/SP4b at `prd.iter2.md:834-835`, SP6 at `prd.iter2.md:838`, SP11 at `prd.iter2.md:843`, and SP12 at `prd.iter2.md:844`.

4. per-SP observability signals: CLOSED — the matrix has a concrete `observability_signal` column for every SP at `prd.iter2.md:829-844`. These are named signals such as `guard.execution.duration`, `frontend.route.rendered`, `spec_trio.coverage_ratio`, `traceId_propagated`, `server_action.completed`, `payment.idempotency.replay_match`, `practices.viewer.broken_link.count`, and `cold_start.duration`, not generic "logs are good" prose.

5. autonomous `/team` safety: CLOSED — rollback boundaries are concrete per SP at `prd.iter2.md:867-889`; shared-artifact ownership names one writer for the ESLint plugin, both AGENTS sentinels, UI meta-schema, and L2 retro-edit budget at `prd.iter2.md:893-901`; stale-state invalidation has executable rules at `prd.iter2.md:903-913`; halt thresholds are numeric (`3` failures, `30` minutes idle, `5` rebases) at `prd.iter2.md:915-935`; the ESCAPE valve path and YAML format are named at `prd.iter2.md:937-954`.

6. SP9-SP11 parallelism resolved: CLOSED — SP9 is serialized before SP10/SP11 in the dependency graph at `prd.iter2.md:469-478`; §5 explains why SP9 must precede SP10/SP11 at `prd.iter2.md:732-743`; shared artifacts are partitioned at `prd.iter2.md:747-753` and again in the autonomous ownership table at `prd.iter2.md:893-901`.

## Phase 2 — Soft suggestions disposition

- Reword Option D: applied — `CONSTRAINT-BLOCKED`, with the rollback benefit preserved by `pre-nextjs-migration`, at `prd.iter2.md:51-58` and `prd.iter2.md:1132-1138`.
- shadcn drift probe SP3: applied — snapshot and `_check-shadcn-drift.sh` at `prd.iter2.md:134-139`, upstream snapshot list at `prd.iter2.md:411-418`, and SP5 acceptance at `prd.iter2.md:650-654`.
- provenance_class ADR: applied — enum, schema, semantics, and guard enforcement at `prd.iter2.md:370-409`; ADR registry classes at `prd.iter2.md:1161-1172`.
- Fork-receiver smoke earlier: applied — SP5.5 inserted at `prd.iter2.md:489-491` and specified at `prd.iter2.md:665-690`; SP12 reruns full-tree smoke at `prd.iter2.md:797-803`.
- Cap React rules: applied — cap stated at `prd.iter2.md:168-170`, SP7 defers non-implementation-proven rules at `prd.iter2.md:713-717`, and honored constraints restate it at `prd.iter2.md:1214`.

## Phase 3 — Architect's new steelman (over-rigidity)

- Real risk? Yes, and stronger than the Architect framed it. The PRD says four L4 domains are eligible, including `practices`, at `prd.iter2.md:151-156`. It also says SP11's practices viewer reads `practices/AGENTS.md` and `practices-react/AGENTS.md` statically at `prd.iter2.md:740-741`, and its test navigates static viewer routes at `prd.iter2.md:783-789`. The current repository has no `contracts/practices-openapi.yaml`; existing contracts are auth, CRUD, payment, and ratelimit only. Yet §4.8.2 requires every UI route to carry `backend_operation_id: <string>` that MUST match `backend_contract_ref` OpenAPI at `prd.iter2.md:201-220`, and §4.8.4 step 5 enforces that match at `prd.iter2.md:257-259`. That means the in-scope practices viewer cannot be represented cleanly.
- Severity: blocking for execution, narrow in scope. This is not a rejection of the schema-rigidity fix; it is a missing domain class in the schema.
- Mitigation adequate? Not as written. The PRD has `full_trio` and `backend_only` allowlist classes at `prd.iter2.md:273-283`, but no `frontend_only` or `static_content` class. The Architect's suggested future `frontend_only` extension is the right shape, but it needs to be in iter3 now because SP11 is already a static/non-API UI domain.

## Phase 4 — My new attack on iter 2

- Specific criticism: Iter2's stricter UI Contract creates a fake-backend pressure for static frontend domains. The Page Compliance Spec allows `backend_operation_id: <string|null>` when a page is not API-bound at `prd.iter2.md:186-194`, but the UI Contract does not mirror that flexibility: route-level `backend_operation_id` is a required string and must resolve in `backend_contract_ref` at `prd.iter2.md:201-220`. The guard then enforces OpenAPI resolution for every UI route at `prd.iter2.md:257-259`. SP11 is explicitly a static practices viewer (`prd.iter2.md:740-741`, `prd.iter2.md:783-789`), so the plan either has to invent backend OpenAPI operations for file reads, omit routes from the UI contract and weaken verification, or fail the guard. All three violate the composition-kit vision in `CLAUDE.md:3-43`, especially the "React + Spring equal partner" and "new domain/rule addition is normal" framing.
- Grade: BLOCKING.
- Required mitigation: Add a third allowlist/domain mode, named either `frontend_only` or `static_content`, before `/team` execution. The minimum iter3 patch should:
  - Extend `practices/evals/trio_integrity_allowlist.yaml` schema to `full_trio`, `backend_only`, and `frontend_only` or `static_content`.
  - Extend `contracts/<domain>-ui.yaml` so non-API routes may set `backend_operation_id: null` only when they declare a concrete `static_source_ref` or equivalent source contract.
  - Update `trio_integrity_guard.sh` algorithm so `frontend_only` routes are verified against source files and route tests, not backend OpenAPI operation IDs.
  - Add PASS and FAIL fixtures for the practices/static-content case, including a fail case where a static route lacks a source ref.
  - Assign `practices` to that mode, or explicitly add a real `contracts/practices-openapi.yaml` if the intended architecture is not static. The current SP11 text points to static, so `frontend_only` is the cleaner fix.

I considered the ESCAPE valve conflict with "do not stop" autonomy. It is not blocking: hard halt thresholds and a manual takeover file at `prd.iter2.md:917-957` are safety boundaries for repeated failed verification, not a soft checkpoint. I also considered verification runtime cost; the 300s fork smoke at `prd.iter2.md:678-680` and `prd.iter2.md:802-811` is heavy but acceptable for SP termination, not inner-loop red-green work.

## Phase 5 — Final verdict reasoning

The original six hard blockers are closed. The Planner did the important work: SP3 now lands real guards first, no placeholder guard can false-green, verification has concrete RED reasons and first green commands, observability is at least named and asserted, `/team` execution has explicit rollback and halt contracts, and SP9 no longer races SP10/SP11 over shared L2/schema artifacts.

The reason for ITERATE is a single new structural mismatch introduced by making the guard schema rigorous: `practices` is both in-scope and non-API/static, while the UI Contract and guard still assume backend OpenAPI route binding for every UI route. This can be fixed surgically by adding a `frontend_only` or `static_content` mode with its own binary fixtures. After that patch, I would expect APPROVE without another broad rewrite.

## ADR-ready content (Step 6 commit)

- Decision: Iter2 is not approved for `/team` yet; approve the iter2 spine but require one schema-mode patch for static/non-API frontend domains before execution.
- Drivers: Binary verification must remain real; the composition kit must support backend-bound and static frontend domains; SP11 must not depend on fake backend OpenAPI operations.
- Alternatives considered: Approve iter2 as-is; reject the rigorous Spec Trio schema; add dummy practices OpenAPI operations; add a bounded `frontend_only` / `static_content` schema mode.
- Why chosen: Approving as-is would send `/team` into a predictable SP11 schema conflict. Rejecting the rigorous schema would reopen iter1's main failure. Dummy backend operations would encode false provenance. A bounded frontend-only mode preserves binary verification while supporting the current practices viewer and future static domains.
- Consequences: One iter3 spec edit is required: allowlist schema, UI Contract route schema, guard algorithm, and static-content fixtures. No implementation phase needs to be redesigned.
- Follow-ups: Re-run Critic only on the static/non-API domain patch and the revised guard fixtures; no need to reopen the six closed iter1 blockers unless the patch touches them.

## Re-review trigger (only if ITERATE/REJECT)

- Add `frontend_only` or `static_content` domain mode to §4.8.2, §4.8.4, and `trio_integrity_allowlist`.
- Mark `practices` as that mode, or revise SP11 to use a real backend API contract. Do not leave it implicit.
- Add `pass_frontend_only_practices/`, `fail_frontend_only_missing_source_ref/`, and `fail_frontend_only_unreachable_route/` fixtures, with expected exit codes/messages.
- Update §5.5 SP11 row so its assertion references the new static-source contract signal, not only Playwright 200s.
- Keep all iter2 closed-blocker material intact.
