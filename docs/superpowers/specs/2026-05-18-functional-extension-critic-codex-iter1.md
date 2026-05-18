# Codex Critic Review — Functional Extension PRD (Iteration 1)

## Verdict: ITERATE

The draft is close enough to preserve the seven-SP framing, but it is not safe to approve for autonomous `/ralph` or `/team` execution. I ratify the Architect's three structural concerns: SP29 should flip from three new Tier-1 skills to `/ax-verify` subcommands, SP24/SP25/SP26 are not actually parallel, and `BaseEntityWithSoftDelete` creates a conflicting entity hierarchy. I also found one new hard contradiction: SP28's acceptance criteria require existing L4 auth/payment string migration, while the same PRD declares that migration out of scope.

## Architect findings disposition

1. Tier-1 cap (4->7) — **agree.** Collapse F13/F14/F15 into `/ax-verify policy-check|evidence-fetch|explain` subcommands. PRD lines 526-535 define three new Tier-1 skills, line 550 names subcommands as fallback, and line 822 explicitly admits the prior Tier-1 rule is violated. The fallback should become the default.
2. SP24/25/26 false parallelism — **agree.** PRD line 297 claims SP24/SP25/SP26 can run in parallel, but line 355 says SP24's `ExportJobService` depends on SP25 `JobDispatcher`, lines 389-394 place `BaseEntityWithSoftDelete`, `PageRequestNormalizer`, and `JobDispatcher` in SP25, and SP26 search needs paging normalization for its API surface.
3. BaseEntity vs BaseEntityWithSoftDelete — **agree, with one sharper repo fact.** At least five templates already import/extend `com.example.app.common.BaseEntity`: `Notification.java:22/54`, `NotificationPreferences.java:23/43`, `EmailOutbox.java:23/55`, `EmailTemplate.java:23/52`, `ScheduledTask.java:23/57`, and `JobHistory.java:54`. The repo currently has references to that base but no checked-in `templates/backend/**/BaseEntity.java`, so SP25 must first normalize the existing base-entity story. Preferred revision: extend the SP13/common `BaseEntity` in place with `@SQLDelete` + `@Where` or explicitly add/repair the missing common `BaseEntity` template, then have all existing extenders inherit the same behavior. Do not introduce a sibling `BaseEntityWithSoftDelete`.

Dimensional verdicts from Architect:

| Dimension | Architect verdict | Codex disposition |
|---|---:|---|
| (a) Boundary integrity | PASS-WEAK | **Agree.** F5/F6 placement is sound; F11 `-extended` siblings need an explicit deprecation/in-place-upgrade policy. |
| (b) Verification closure | PASS | **Agree with caveat.** Matrix names verify skills, but SP29's future path must be `/ax-verify` subcommands, not three self-tested Tier-1 skills. |
| (c) Evidence chain density | PASS | **Agree.** Snapshot/ADR coverage is adequate for PRD approval once quote anchors are required in SP cards. |
| (d) Anti-pattern resistance | PASS | **Agree.** No MockMvc and no release/deployment artifacts are explicit; policy-check false-positive handling is strong but belongs under `/ax-verify`. |
| (e) TDD anchoring | PASS | **Mostly agree.** Search and realtime have named tests, but realtime's RED reason is more structural than behavioral. Not blocking. |
| (f) Spec Trio atomic rule | PASS | **Agree.** Search and feature-flags ship as full-trio atomic clusters; tighten SP28 rollback wording. |
| (g) Parallelizability | WEAK | **Agree, and blocking.** The dependency graph must be corrected before execution. |
| DELIBERATE pre-mortem | PASS | **Agree overall.** Five scenarios exist; Scenario 5 is too narrow because it names only registry race, not the real BaseEntity/JobDispatcher/PageRequest shared surfaces. |
| Observability matrix | PASS/PARTIAL | **Agree.** Every row has an `observability_signal`, but some emission mechanisms are implied rather than executable. |

## Criterion-by-criterion findings (A-L)

### A. Principle-Option consistency — WEAK

The principles are good, but Option B does not fully satisfy them. "Few exposed surfaces" on line 17 conflicts with SP29's 4->7 Tier-1 jump. "Binary verification per axis" on line 16 conflicts with SP29 being described as three separate self-tested skills instead of a unified `/ax-verify` axis. Option B is still salvageable if SP29 becomes a single `/ax-verify` extension and the dependency graph is reserialized.

### B. Fair alternatives — WEAK

Options A and C are treated honestly. Option D is underdeveloped: it frames deferring F13-F15 as losing empirical feedback, but does not evaluate the more relevant alternative: ship F13-F15 now as `/ax-verify` subcommands. The draft hides the best alternative in a risk fallback at line 550 rather than presenting it in §1 Options.

### C. Risk mitigation clarity — FAIL

The §8 pre-mortem mostly has owner + command + threshold. The SP-level risk sections do not. Examples: SP24 "pin in `blueprints/pinned-versions.yaml`" has no owner, command, or compatibility threshold; SP24 POI memory mitigation has an ArchUnit assertion but no command/threshold; SP25 migration collision mitigation is README-only; SP27 serverless mitigation is a warning, not a safer default. Before approval, every SP-card risk needs the same executable shape as §8: owner, command, threshold, recovery.

### D. Testable acceptance criteria — WEAK

Most SPs terminate with binary commands in lines 322-328, 362-368, 402-409, 435-443, 470-476, 502-510, and 536-543. The weakness is SP29: acceptance is currently three independent Tier-1 self-tests, not a single `/ax-verify-*` termination path. Revise to `bash skills/ax-verify/scripts/run.sh policy-check|evidence-fetch|explain ...` or equivalent subcommand self-tests.

### E. Concrete verification steps — PASS-WEAK

Every SP names a `verify_skill` in the matrix lines 559-567. Existing skills include `/ax-verify-java`, `/ax-verify-L1`, `/ax-verify-L2`, `/ax-verify-L4`, and `/ax-verify-domain`; SP29 creates new skills but should instead create subcommands in existing `/ax-verify`. Also fix O1 line 99, which claims `ax-verify-domain observability` even though SP23's actual acceptance uses `/ax-verify-java`.

### F. TDD anchor concreteness — PASS-WEAK

The matrix is concrete enough for execution. SP26 names `fail_search_missing_frontend_spec/`, `SearchFlowIT.java`, and `typeahead-search-ime.spec.ts` with a binary first green command. SP27 names `SseSubscribeIT.java`, `dirty-guard.spec.ts`, and `auto-save-indicator.spec.ts`; however, the SP-level TDD anchor at line 476 only exercises `dirty-guard`, while SSE's RED reason is "SseEmitterConfig ENOENT / 404." Add a realtime-specific failing fixture or RestAssured test file with the first green command.

### G. Pre-mortem adequacy (DELIBERATE) — PASS-WEAK

There are five scenarios with detection and thresholds. Scenario 4 is particularly good: a 50-fixture eval set and FP rate <5%. Weakness: Scenario 5 is mislabeled as "SP24/25/26 parallel race" but only covers `domain-registry.yaml`; it misses the actual race surfaces: `JobDispatcher`, `PageRequestNormalizer`, and `BaseEntity`.

### H. Expanded test plan adequacy — PASS-WEAK

§6.5 covers unit/static fixtures, RestAssured integration, Playwright/e2e, and observability signals per SP. The observability column is populated for all rows. Remaining gap: several signals are named without proving how they are emitted or scraped, e.g. `explain.responses.cache_hit_ratio` and `form.dirty_block.fired_count`.

### I. Architect findings disposition — PASS

I agree with all three structural concerns and with the Architect's dimensional findings. Required Planner revisions are listed in "Re-review trigger" below.

### J. CLAUDE.md anti-pattern resistance — WEAK

No deployment/release scope creep: PASS, lines 119-120 and 835-837 explicitly exclude it. No MockMvc-only tests: PASS, line 823 says RestAssured/Playwright/Vitest. Governance-loop risk: WEAK. Line 820 says the new skills advise rather than enforce, and Scenario 4 has FP controls, but a top-level `/ax-policy-check` with STOP semantics still risks policy theater if over-triggered. Collapsing it into `/ax-verify policy-check` and preserving FP <5% keeps the useful gate while reducing surface-area theater.

### K. Autonomous execution safety — WEAK

Rollback boundaries exist per SP at lines 577-585, halt thresholds exist at lines 606-612, and ESCAPE exists at lines 614-616. Shared-artifact ownership is incomplete: §7.2 lines 589-597 does not name the i18n L1 ownership, cache layer, BaseEntity extension, `JobDispatcher`, or `PageRequestNormalizer`. Those omissions are material because they are exactly where autonomous parallel execution will conflict.

### L. My independent steelman — FAIL

SP28 contains a binary acceptance/scope contradiction. Lines 506-508 require switching locale ko-KR -> en-US and making all `templates/L4/auth/` strings update, plus making hardcoded-string probes pass against `templates/L4/auth/` and `templates/L4/payment/` after migration. But lines 512-513 and 839 declare existing L4 string migration out of scope. That is not a small wording issue: `/ralph` cannot both migrate the existing strings and not migrate them. Planner must choose one.

## My independent steelman (criterion L)

The i18n rule will create hidden migration debt if it is scoped as "new files only" while acceptance still demonstrates existing L4 auth/payment compliance. The current repo already has hardcoded Korean strings in `templates/L4/payment/app/(payment)/checkout/page.tsx:61-62`, `templates/L4/payment/app/(payment)/methods/new/page.tsx:30-31`, and `templates/L4/file-storage/app/(file-storage)/upload/page.tsx:142,170,177,204-205`. Even the PRD's own F8 justification only names auth/payment, missing file-storage. Either SP28 must run a bounded migration across all existing L4 domains covered by the rule, or the acceptance criteria must only run the rule against new SP28-created L4 paths and fixtures.

Does it change my verdict? **Yes, reinforcing ITERATE.** I was already ITERATE from the Architect's three concerns; this adds a fourth hard blocker because acceptance criteria are internally impossible.

## Hard blockers (must fix before APPROVE)

1. Flip SP29 default: replace `/ax-policy-check`, `/ax-evidence-fetch`, and `/ax-explain` Tier-1 skills with `/ax-verify policy-check`, `/ax-verify evidence-fetch`, and `/ax-verify explain` subcommands. Update §1 Options, SP29 deliverables, acceptance criteria, verification matrix, ADR text, and honored-constraints table.
2. Reserialize dependency graph: `SP23 -> SP25 -> SP24 || SP26`, or move `JobDispatcher` interface and `PageRequestNormalizer` into a stable earlier SP. Update lines 277-300 and ADR "Why chosen" lines 777-780.
3. Resolve BaseEntity explicitly. Remove `BaseEntityWithSoftDelete` as a sibling. Add/repair the common `BaseEntity` template if missing, then extend it in place with soft-delete annotations/policy and update existing extenders/queries as needed.
4. Fix SP28 i18n scope contradiction. Either migrate all existing L4 domains in scope and include them in rollback/ownership/tests, or declare existing L4 migration out of scope and remove lines 506-508's existing-auth/payment pass requirement.
5. Expand §7.2 shared-artifact ownership with at least: i18n L1 components, cache layer, common `BaseEntity`, `JobDispatcher`, `PageRequestNormalizer`, `domain-registry.yaml`, and feature-flags/search allowlist entries.
6. Convert SP-card risks to executable mitigations with owner + command + threshold, not README/ADR-only mitigations.

## Soft suggestions (improve but not blocking)

1. SP27 should default L4 notification/audit/payment realtime wiring to polling and expose SSE/WebSocket as opt-in transport in a blueprint manifest.
2. SP26 should make Korean tokenizer selection an install-time or first-run decision, not only a passive README warning.
3. SP27 form blocks should either upgrade SP15 files in place or mark the old shell files as deprecated with a documented `-extended` convention.
4. SP28 feature-flags rollback wording should say atomic revert of the full feature-flags trio/backend/frontend cluster.
5. Add exact evidence quote anchors for new snapshots in SP deliverables, especially Recharts, PostgreSQL FTS, Meilisearch, next-intl, and RHF.

## Re-review trigger (if ITERATE/REJECT)

Exact Planner revisions that would flip this to APPROVE on next iteration:

1. A revised PRD where SP29 is `/ax-verify` subcommands, Tier-1 remains at 4, and no new top-level skill names are introduced.
2. A revised dependency graph and ADR where SP25 foundational data/jobs lands before SP24/SP26, or shared interfaces are moved earlier with sole ownership.
3. A revised SP25 deliverable set replacing `BaseEntityWithSoftDelete.java` with an in-place common `BaseEntity` soft-delete extension, including the impact list for `Notification`, `NotificationPreferences`, `EmailOutbox`, `EmailTemplate`, `ScheduledTask`, and `JobHistory`.
4. A revised SP28 i18n scope where acceptance, risk, out-of-scope, and rule `applies_to` all say the same thing.
5. A completed shared-artifact ownership table and executable risk mitigations for every SP-card risk.
6. Updated verification matrix rows reflecting the above, especially SP27 realtime RED/first-green and SP29 subcommand self-tests.

## ADR-ready content (for Step 6 commit if APPROVE)

- Decision: Extend ax-template with F1-F15 functional capability coverage across SP23-SP29, while preserving a four-skill Tier-1 surface by implementing F13-F15 as `/ax-verify` subcommands.
- Drivers: Close self-referential rules that currently protect no template; ship search and feature-flags as atomic Spec Trio domains; add pre-execution policy/evidence/explanation feedback without expanding user-facing skill topology.
- Alternatives considered: Mega-SP rejected for blast radius; 15 one-gap SPs rejected for coordination overhead; deferring F13-F15 rejected because pre-execution feedback is needed before the next cycle; three new Tier-1 skills rejected because `/ax-verify` subcommands satisfy the same capability with lower surface area.
- Why chosen: The revised Option B keeps SP grouping reviewable, makes SP25 the shared data/jobs foundation before SP24/SP26, and keeps verification binary through existing skill surfaces.
- Consequences: SP count remains seven; Tier-1 count remains four; SP25 touches common BaseEntity and must own its rollback; SP28 i18n scope must be explicit; SP29 extends `/ax-verify` and keeps the 50-fixture false-positive gate.
- Follow-ups: Optional SSE/WebSocket transport hardening, Korean search tokenizer install prompt, existing L4 i18n migration if not included in SP28, and 180-day snapshot refresh for new evidence files.
