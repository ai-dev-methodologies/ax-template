# Codex Critic R7 iter 1

## Verdict

ITERATE.

The Architect's 2 HIGH + 4 MEDIUM findings stand. I found no on-disk proof that justifies downgrading either HIGH. Synthesis-B also stands: split scheduler into SP41, community into SP41b, and make SP42 partial-tag-aware unless Planner explicitly rejects that option with a bounded rationale.

One additional BLOCKING issue: PRD SP41 requires `/ax-verify-domain scheduled-task` to exit 0, but the current verifier maps `scheduled-task` to the invalid Gradle task string `testScheduled Task` because `skills/ax-verify-domain/scripts/run-gradle.sh:44` sets `OFS=""` after field rewriting. `practices/evals/trio_integrity_allowlist.yaml:24` already marks `scheduled-task` as backend_only, so this path is reachable. PRD iter 2 must either add an explicit `scheduled-task) GRADLE_TASK="testScheduledTask"` mapping plus task/test scope, fix the generic hyphen mapper, or replace the SP41 verification command with the new `skills/_tests/L4/scheduler-domain.test.sh` gate only.

## Architect findings disposition (6)

1. **HIGH: L1 count 48 claimed vs 49 on disk - STANDS.** `ls templates/L1/components | wc -l` returned 49. PRD still claims 48 in section 2 (`docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:82`) and section 9 (`:375`).

2. **HIGH: TD-2026-05-20-022 empty-list guard specialization unnecessary - STANDS.** `practices/evals/recipe_governance_guard.sh:40-80` explicitly rejects empty `applied_recipes:` lists. `templates/L4/file-storage/README.md` and `templates/L4/practices/README.md` have no `applied_recipe` key, and `bash practices/evals/recipe_governance_guard.sh` exits 0 because unused L4 are not checked through active recipes. Correct scheduler intro shape is absence of key, not `applied_recipes: []`.

3. **MEDIUM: Korean verbatim regression - STANDS.** R7 has only DCinside and Clien downgrades. The project vision in `CLAUDE.md:39` explicitly frames the kit around the Korean enterprise stack, and R6 precedent used Korean verbatim. Iter 2 should attempt at least 2-3 additional Korean engineering/product sources or document a deliberate zero-Korean-verbatim exception.

4. **MEDIUM: Reddit downgrade incomplete - STANDS.** Current attempts are only `developers.reddit.com` and `www.reddit.com/dev/api`. Alternative host fingerprints such as `github.com/reddit-archive`, `praw.readthedocs.io`, and `devvit.dev` remain untried.

5. **MEDIUM: 3 of 5 community invariants cite unresolved refs - STANDS.** Disk evidence: `specs/notification-l0.yaml:131` has `NOTIF-PREF-001`, not `NOTIFICATION-PREF-001`; `grep "ASVS-V11"` in `specs/auth-asvs-l1.yaml` returns no match; `practices/rules/sanitize-user-html-server-side.md` does not exist.

6. **MEDIUM: literal `applied_recipes: []` syntax - STANDS.** PRD mentions it in section 4.1/section 4.5/section 5 (`:162`, `:239`, `:276`, `:280`, `:286`). This syntax conflicts with `practices/rules/business-domain-must-declare-applied-recipe.md:108` and the guard implementation.

## Criterion findings (A-L)

**A. Principle-Option consistency - FAIL.** Principle 2 says every `spec_ref` is disk-verified, but section 4.2 marks three community bindings "TO BE DISK-VERIFIED". The PRD also repeatedly says scheduler unblocks 3 deferred recipes (`:10`, `:18`, `:38`), while `recipes/_MANIFEST.yaml:89-102` shows only `lms` and `cms` name scheduler; `internal-it` is gated by Jira/ServiceNow verbatim + webhook notification.

**B. Fair alternatives - PARTIAL.** Three alternatives exist, with pros/cons, but the strongest fair alternative is missing: Synthesis-B split-SP. Since mutation surfaces are disjoint and scheduler uses a new harness shape, the split option must be included or explicitly rejected.

**C. Risk mitigation clarity - PARTIAL.** The pre-flight, mid-flight, stop, and rollback gates are clear. However, the mitigations assume false facts in two places: all five community invariants resolving, and `applied_recipes: []` being guard-safe for unused L4.

**D. Testable acceptance binary - PARTIAL.** SP41/SP42 have binary gates, but SP42 says failed verdicts both hold the tag and still ship SP41 mutations as `active-verdict-pending` (`:240`, `:295`). That conflicts with the "no partial deliverable ship within SP41" posture unless Synthesis-B/partial-tag policy is made explicit.

**E. Concrete verification - PARTIAL.** Existing named verification surfaces exist: `/ax-verify`, `/ax-verify-domain`, `recipe_governance_guard.sh`, and `recipe_spec_referential_integrity_guard.sh`. Planned new files are acceptable as deliverables. But `/ax-verify-domain scheduled-task` is not currently a valid gate because the Gradle task mapper produces `testScheduled Task` for `scheduled-task`.

**F. TDD anchor concreteness per deliverable - PARTIAL.** Scheduler has a concrete RED reason and command. Community's `first_GREEN_command` is a prose list of edits, not an executable command, and the five participating L4 README appends lack their own per-L4 TDD anchor beyond aggregate guard success.

**G. Pre-mortem adequacy - PASS with caveats.** DELIBERATE mode requires 3; PRD has 5 real scenarios with likelihood/impact/mitigation. Caveat: scenario 1 claims the recipe binds to five disk-verified internal anchors, but three are currently unresolved.

**H. Expanded test plan - PARTIAL.** The plan covers guards, sealed verdicts, scheduler-domain, community compose, and `/ax-verify`. Missing: a concrete fix/test for the scheduler `/ax-verify-domain` path, and explicit re-verification for corrected community invariant refs.

**I. Architect findings disposition - ITERATE.** All six findings stand; no downgrade.

**J. CLAUDE.md anti-patterns - MOSTLY PASS.** PRD avoids `RECIPE_DEVIATION.md` (`:140`, `:404`), does not introduce MockMvc, and says no git workflow / CI / release policy changes (`:143`). Watch item: tag/PR language should remain catalog-release scoped and not become a fork-team policy mandate, per `CLAUDE.md:63-71`.

**K. Autonomous safety - PASS with one contradiction.** No destructive ops are allowed (`:297`), rollback is single-SP revert, and stop conditions are named. Contradiction: SP42's `active-verdict-pending` path permits partial semantic ship after a verdict failure; Synthesis-B resolves that cleanly.

**L. Independent steelman attack - BLOCKING.** See next section.

## My steelman attack (one new)

**BLOCKING: the scheduled-task domain verification gate is currently invalid.**

PRD section 4.5 requires `/ax-verify-domain` over `scheduled-task, crud, audit-log, notification, search, auth` to exit 0 (`docs/superpowers/specs/2026-05-20-r7-scheduler-community-prd.draft.md:239`). On disk, `scheduled-task` is already allowlisted as backend_only (`practices/evals/trio_integrity_allowlist.yaml:24`). The verifier therefore reaches the backend Gradle path. `skills/ax-verify-domain/scripts/run-gradle.sh:33-45` has explicit cases for several hyphenated domains but not `scheduled-task`; its generic awk mapper emits `Scheduled Task`, producing `testScheduled Task`, not `testScheduledTask`.

I validated the mapping directly:

```text
scheduled-task -> Scheduled Task -> testScheduled Task
```

There is also no current backend `testScheduledTask` evidence from `rg "testScheduled|ScheduledTaskCompliance|@Tag\\(\"SCHED" backend`. This means the PRD's SP41 verification matrix can fail for a tooling reason unrelated to the new L4 README. Iter 2 must include one of:

- add an explicit `scheduled-task` mapping and the matching Gradle task/test scope,
- fix the generic hyphen mapper before relying on `/ax-verify-domain scheduled-task`,
- or remove `/ax-verify-domain scheduled-task` from SP41 and make `bash skills/_tests/L4/scheduler-domain.test.sh` the scheduler-specific binary gate.

## Hard blockers

- Correct L1 baseline: 48 -> 49 in section 2/section 9 and any summary table.
- Remove TD-2026-05-20-022 and all `applied_recipes: []` scheduler README language. Use no `applied_recipes` key until a recipe consumes scheduler.
- Resolve community invariant refs before approval: `NOTIF-PREF-001`; replace/remove nonexistent `ASVS-V11.1.4`; replace/remove or co-ship the nonexistent sanitize-user-html rule.
- Fix the scheduled-task verification path or change the SP41 verification matrix so its binary gate is executable.
- Add Synthesis-B as a viable option or explicitly reject it.

## Soft suggestions

- Add 2-3 Korean source attempts beyond DCinside/Clien; Toss, Naver D2, Kakao Tech, Woowahan, and LINE Engineering are reasonable candidates.
- Add Reddit alternative-host attempts through GitHub archive, PRAW docs, or Devvit before preserving `internal_design`.
- Fix the "3 of 4 deferred recipes name scheduler" language to "2 of 4" everywhere; `internal-it` is independent of scheduler.
- Make community's first GREEN command executable instead of prose.
- Re-check whether `practices/generate_agents.sh` actually depends on L4 topology; section 11 currently hedges this correctly, but section 1/section 3 still assume a sentinel sha change.

## Re-review trigger

Re-review after Planner publishes iter 2 that:

- corrects all numeric baselines,
- removes the empty-list guard specialization path,
- resolves the three community invariant references,
- adds Synthesis-B or a defensible rejection,
- repairs the scheduled-task verification gate,
- and updates the evidence ledger/retry plan for Korean + Reddit sources.

## ADR-ready (if APPROVE)

Not ADR-ready. TD-020 and TD-021 are directionally valid, but TD-022 should be deleted. TD-023 should remain only after broader Reddit alternative-host attempts are recorded or explicitly rejected.
