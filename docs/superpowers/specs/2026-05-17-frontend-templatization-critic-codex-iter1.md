# Codex Critic Review — 2026-05-17 frontend templatization PRD (Iteration 1)

## Verdict: REJECT

The plan's strategic spine is sound: React 19 + Next.js 16, L1–L4 template layers, Frontend Spec Trio, Decision Provenance Trail, and skill-orchestrated verification all match the locked constraints. It is not execution-ready. The PRD has not been revised after the Architect review, so the known `trio_integrity_guard.sh` structural failure remains in §4.8/§4.10 (`prd.draft.md:342-386`). Five SP TDD anchors remain circular or under-specified, the verification plan lacks per-SP production observability, and autonomous `/team` execution lacks rollback and halt contracts. Under the requested rubric, multiple FAILs mean REJECT.

## Disposition of Architect findings (a–g)

- (a) Boundary integrity WEAK — agree. `ProtectedRoute`, `PaymentCheckoutForm`, and `AppHeader` remain ambiguous across L2/L4 or L1/L2 (`prd.draft.md:230-238`, `629-681`). Required revision: add a Layer Membership Decision Table with ownership, allowed imports, forbidden imports, and examples for those three components.
- (b) Verification closure WEAK — agree. `templates/backend/**` is introduced (`281-301`) but guard coverage names only `templates/L{1,2,3,4}` (`374-385`); `templates/DECISIONS.md` lacks an evidence schema (`354-372`). Required revision: assign both to a named verify skill/guard and define schemas.
- (c) Evidence chain density PASS — agree with caveat. Snapshot coverage is dense enough (`387-400`), but internal ADRs like skill topology and directory shape need `provenance_class: internal_design` rather than fake external evidence.
- (d) Anti-pattern resistance PASS — agree on intent. §10 maps well to CLAUDE.md (`936-948`; `CLAUDE.md:27-41`, `113-139`). Required revision: remove or bound the quarterly review mitigation (`805-806`) because it drifts toward governance ceremony.
- (e) TDD anchoring WEAK — agree, and I mark it FAIL for autonomous execution. SP2/SP4/SP6/SP11/SP12 remain circular or vague (`528-530`, `571`, `604`, `679`, `698`). Required revision: concrete path, assertion, and expected RED reason for each.
- (f) Cross-Trio integrity FAIL — agree. §4.8 lacks `backend_operation_id`, `backend_spec_ref`, allowlist/no-frontend markers, and coverage thresholds, while §4.10 expects operation-ID cross-checks (`342-386`). Required revision: rewrite schema and guard contract.
- (g) Parallelizability SP9–SP11 WEAK — agree. The plan claims parallelism (`702-721`) while ESLint plugin/rules, AGENTS sentinel, UI meta-schema, and L2 retro-edits remain shared mutable artifacts (`514-516`, `616-623`, `658-660`). Required revision: serialize or partition ownership.

## Criterion-by-criterion findings (A–L)

### A. Principle-Option consistency
- Finding: The principles (`16-35`) map reasonably to drivers (`37-50`). Option C satisfies the principles. Option D violates locked constraints but is labeled invalid.
- Severity: PASS
- Required revision: None blocking; clarify D is unavailable by constraint.

### B. Fair alternatives
- Finding: A–C list real pros/cons (`58-88`). D has a real pro, zero migration downtime (`90-93`), but the invalidation overstates indefinite coexistence. The real reason is the locked no-coexistence constraint (`174-187`).
- Severity: WEAK
- Required revision: Steelman time-boxed coexistence, then reject it by constraint.

### C. Risk mitigation clarity
- Finding: Some mitigations are executable, like preserving OAuth paths (`503-508`) and adding a missing-evidence fixture (`553-555`). Others are aspirational: quarterly review (`805-806`), "rolled back or amended" without rollback mechanics (`823-827`), and "escalate" without halt surface (`846-847`).
- Severity: WEAK
- Required revision: Convert each mitigation into owner, command/artifact, and rollback/continue threshold.

### D. Testable acceptance criteria
- Finding: Most SPs include binary skill exits, but SP2 includes prose acceptance ("METHODOLOGY.md updated diff applied", `522-524`) and SP12 mixes rubric/simulation thresholds without saying how `/ax-verify` returns those as exit 0/1 (`695-696`).
- Severity: WEAK
- Required revision: One named skill invocation per SP, with sub-checks bundled into its exit code.

### E. Concrete verification steps
- Finding: Every SP names a verify command, but scripts are not always deliverables before first use. SP1 creates placeholder guards that `exit 0` until SP3 (`484-490`) while SP2 already depends on `/ax-verify-shared` (`525-526`). Current repo only has `skills/ax-transform/SKILL.md`; no new guard scripts exist.
- Severity: FAIL
- Required revision: Move script contracts before use, ban placeholder false-green guards, and add failure fixtures for missing scripts.

### F. TDD anchor concreteness
- Finding: SP2, SP4, SP6, SP11, and SP12 remain circular or vague (`528-530`, `571`, `604`, `679`, `698`). SP9 and SP10 also need exact file paths (`657`, `668`).
- Severity: FAIL
- Required revision: Add table for all 12 SPs: `test_file`, `assertion`, `expected_RED_reason`, `first_green_command`.

### G. Pre-mortem adequacy
- Finding: §7 has three scenarios (`777-847`), but Scenario 1 lacks a hard threshold, Scenario 2 lacks rollback-vs-continue criteria, and Spec Trio drift is missing despite being the hardest guard risk.
- Severity: FAIL
- Required revision: Add thresholds to Scenarios 1–2 and add a Spec Trio drift scenario.

### H. Expanded test plan adequacy
- Finding: §6.3 covers unit/integration/e2e/static (`752-768`), but observability is mostly lint, budgets, a11y, or CWV. The only real production-style signal is global `traceId` text in §7 (`848-854`), not per SP.
- Severity: FAIL
- Required revision: Add per-SP metric/event names and assertions, e.g. `frontend.route.rendered`, `server_action.completed`, `guard.execution.duration`, `traceId_propagated`.

### I. Architect findings disposition
- Finding: All seven Architect calls are justified. Cross-Trio FAIL alone blocks approval.
- Severity: FAIL
- Required revision: Apply the seven dispositions above before re-review.

### J. CLAUDE.md anti-pattern resistance
- Finding: Mostly honored. No MockMvc (`181`, `941`; `METHODOLOGY.md:229-234`), no single package framing (`946`), no frozen React/Spring (`31-33`, `945`; `CLAUDE.md:27-41`). Weak spot: 17 skills plus quarterly review can become governance drift.
- Severity: WEAK
- Required revision: Remove quarterly review and bound skill decisions to binary SP4/SP12 checks.

### K. Autonomous execution safety
- Finding: Dependency graph exists (`702-721`), but rollback points are mostly absent, shared artifact locks are undocumented, and ESCAPE valves are informal ("freeze", "escalate") without exact halt conditions (`840-847`).
- Severity: FAIL
- Required revision: Add rollback boundary per SP, shared-artifact ownership matrix, stale-state invalidation, and explicit halt thresholds.

### L. Steelman challenge
- Finding: See below.
- Severity: WEAK
- Required revision: Add early fork-receiver smoke fixture before SP12.

## My independent steelman (criterion L)

The plan conflates two products: the migrated reference app in `frontend/` and reusable templates under `templates/L1..L4/`. A Next.js migration can be green while the templates remain non-portable because the app can rely on local env, package aliases, auth helpers, backend URLs, or cookies that fork receivers do not have. The PRD only tests portability at SP12 (`689-695`), too late to prevent 11 SPs of in-repo coupling. This threatens Principle 1 (`18-20`) and CLAUDE.md's composition-kit framing (`CLAUDE.md:3-41`).

Does it change my verdict? No. The verdict is already REJECT. This steelman does not require abandoning the plan; it requires moving a fork-receiver smoke test earlier, ideally SP5/SP6.

## Hard blockers (must fix before APPROVE)

1. Make `trio_integrity_guard.sh` binary-implementable with required schema fields, domain allowlist/no-frontend markers, and coverage thresholds.
2. Remove placeholder `exit 0` guard stubs; add real script contracts and failure fixtures before dependent SPs.
3. Rewrite all SP TDD anchors with file, assertion, expected RED reason, and first green command.
4. Add per-SP observability metrics/events.
5. Add autonomous `/team` safety: rollback points, shared-artifact ownership, stale-state invalidation, halt thresholds.
6. Resolve SP9–SP11 parallelism by serialization or explicit artifact partitioning.

## Soft suggestions (improve but not blocking)

1. Reword Option D as unavailable under locked constraints while preserving its real benefit.
2. Add shadcn drift probe/snapshot diff in SP3.
3. Add ADR `provenance_class`.
4. Move fork-receiver template smoke testing earlier than SP12.
5. Cap React rule additions to implementation-proven needs.

## Re-review trigger (7 surgical revisions for next Planner iteration)

1. Rewrite §4.8/§4.10 so `trio_integrity_guard.sh` and `cross_trio_guard.sh` are binary-implementable with fields, thresholds, fixtures, and allowed domains.
2. Add a verification matrix for SP1–SP12 with `verify_skill`, script path, test file, assertion, expected RED reason, first green command, and observability signal.
3. Add a layer membership and verification ownership table covering `ProtectedRoute`, `PaymentCheckoutForm`, `AppHeader`, `templates/backend/**`, and `templates/DECISIONS.md`.
4. Add autonomous execution safety: rollback boundary per SP, shared-artifact ownership matrix, stale-state invalidation, and ESCAPE/halt thresholds.
5. Revise Phase 3 to serialize SP9 before SP10/SP11 or partition ESLint plugin, AGENTS sentinel, meta-schema, and L2 retro-edits.
6. Patch §7 pre-mortem with executable mitigations and thresholds, plus Spec Trio drift.
7. Remove quarterly review/governance mitigation and add ADR `provenance_class`.

## ADR-ready content (for final Step 6 commit if APPROVE)

- Decision: Not approved in iteration 1. Preserve the strategic spine, but require schema, verification, TDD, observability, and autonomous-execution revisions before `/team`.
- Drivers: Binary verification per SP; self-discoverability; auth migration safety; composition-kit portability; avoiding governance loops.
- Alternatives considered: Approve as-is; iterate with Architect remediations only; reject and re-brainstorm; reject execution while preserving the spine.
- Why chosen: Execution must be rejected because Cross-Trio cannot be implemented from declared schemas, several TDD anchors are not RED-first, and no-checkpoint `/team` lacks rollback/halt contracts.
- Consequences: Planner must revise before implementation. SP1 should not start while placeholder guards can false-green or migration rollback is underspecified.
- Follow-ups: Re-run Critic review after the seven trigger revisions; if approved, commit final PRD/test spec with Lore trailers.
