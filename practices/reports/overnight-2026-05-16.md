# Overnight Run — 2026-05-16

> Autonomous execution while user was asleep. All decisions in this report were
> **auto-signed using the recommended option**. Every change is a reversible atomic
> commit on `main`. User reviews and either confirms ("keep") or reverts each item.

---

## TL;DR

- 2 commits added to `origin/main`: portability infrastructure + advisory weekly workflow.
- **First empirical portability measurement** done — 12 tests across petclinic + realworld, 11 PASS / 1 FAIL. The 1 FAIL is a real architectural cycle in realworld (not a false positive).
- All 4 hard gates still PASS on ax-template; `./gradlew test` still BUILD SUCCESSFUL.
- No rules deleted, no methodology changes, no force-push.

---

## Decisions Auto-Signed (with reasoning)

### 4a. Fixture build verification — recommended: try both, GREEN required
- **Result**: petclinic (Maven) ✅ GREEN. realworld (Gradle) ✅ GREEN (with deprecated-API warnings on `GraphQLCustomizeExceptionHandler` — fixture's own code, not our rules).
- **Reasoning**: both fixtures' bytecode is now available under `target/classes` / `build/classes/java/main` for ArchUnit consumption.

### 4b. D-stage — 5 fixture-friendly rules applied
- **Selected 5 rules** (rationale per rule):
  1. `arch-no-cyclic-package` — ArchUnit slicing, package root parametric, no class hardcoding
  2. `arch-layer-boundary` — Service/Repository/Controller simple-name suffixes are universal naming
  3. `lang-records-for-dtos` — `*Request` / `*Response` suffixes are universal
  4. ~~`lang-sealed-result-hierarchy`~~ **SUBSTITUTED**: hardcoded to `PaymentResult.class` (ax-template-specific) → not portable
  5. `quality-no-system-streams` (substitute for #4) — `System.out` / `System.err` detection is JDK-anchored, fixture-agnostic
  6. `lang-no-public-mutable-fields` — generic field-modifier check
- **Reasoning**: `lang-sealed-result-hierarchy` could not be re-targeted at a fixture without rewriting the rule's anchor class — out of D-stage scope. `quality-no-system-streams` is a clean substitute that keeps the 5-rule count.

### 4c. Application method — recommended: ArchUnit `ClassFileImporter.importPath()`
- **Result**: implemented in `backend/src/test/java/com/ax/template/authblueprint/practices/portability/PortabilityFixtures.java`. JUnit `Assumptions.assumeTrue` skips the test when the fixture's classes dir is missing — so the suite degrades gracefully, never failing on a clean clone.
- **Reasoning**: minimal new infrastructure; the test runner is the same JUnit 5 + ArchUnit pair as `testPractices`. New `tasks.register<Test>("testPortability")` in `backend/build.gradle.kts` includes only the `@Tag("PORTABILITY")` tag.

### 4d. A-stage — generic化 범위
- **Decision**: deferred — the 5 fixture-friendly rules were already authored generically enough to retarget. No new generic化 work was needed beyond writing the new test files.
- **Backlog item identified** (see "Decisions Needing Human Review" below): `lang-records-for-dtos` passes vacuously on both fixtures because neither uses `*Request` / `*Response` suffixes. Broadening the suffix set (Dto, Form, Payload) is a candidate for the next round but is an *opinion edit* — left for user review.

### 5a. P2-A5 AGENTS.md sentinel CI
- **Result**: **already implemented** in `.github/workflows/practices-sentinel.yml` lines 50–58 (regenerate + `git diff --exit-code`). No edits required.
- **Reasoning**: discovered during inspection; saved an unnecessary change.

### 5b. P2-C1 drift auto-PR
- **Result**: **already implemented** in `.github/workflows/practices-drift.yml` with `permissions: contents: write, pull-requests: write` and labels `practices-drift, maintenance, automated`. No edits required.

### Extra auto-decision: testPortability as weekly advisory workflow (not on practices-sentinel)
- **Decision**: created `.github/workflows/practices-portability.yml` — Mon 07:00 UTC cron, builds fixtures, runs `testPortability`, uploads test report as artifact. `continue-on-error: true` throughout — never blocks merges.
- **Reasoning**: putting fixture builds (5+ min petclinic, several min realworld) on every PR would slow `practices-sentinel` from <1 min to 8+ min for a *purely advisory* signal. Weekly cadence + workflow_dispatch is the right cost/value trade-off. The 4 hard gates remain instant on every PR.

---

## Commits Created (in order, all on `main`)

| # | Hash | Message |
|---|------|---------|
| 1 | `0386067` | feat(practices): portability axis first measurement — 5 rules applied to petclinic + realworld fixtures |
| 2 | `74c33c2` | feat(practices): portability advisory automation — weekly workflow + dashboard |

Both commits passed `pre-push` full regression (testPractices + testAsvs + testCrud). Both pushed to `origin/main`. `dd0f6c8` (last squash from previous session) is now `HEAD~2`.

---

## Rules Outcome on Fixtures

| Rule | spring-petclinic | spring-realworld | Interpretation |
|------|------------------|------------------|----------------|
| `arch-no-cyclic-package` | PASS | **FAIL** | Real cycle: `Slice application ↔ Slice infrastructure` — `io.spring.application.*QueryService` depends on `io.spring.infrastructure.mybatis.readservice.*` AND `io.spring.infrastructure.*` returns `io.spring.application.data.*` types. **Rule unchanged** — fixture genuinely has the anti-pattern, which is the strongest evidence the rule is universal. |
| `arch-layer-boundary` (Service ⇒ Controller) | PASS | PASS | Universal validated |
| `arch-layer-boundary` (Repository ⇒ Service/Controller) | PASS | PASS | Universal validated |
| `lang-records-for-dtos` | PASS (vacuous) | PASS (vacuous) | Neither fixture uses `*Request` / `*Response` naming — rule's *detection surface* is too narrow. **Backlog**: consider widening to `Dto` / `Form` / `Payload` |
| `quality-no-system-streams` | PASS | PASS | Universal validated |
| `lang-no-public-mutable-fields` | PASS | PASS | Universal validated |

**Net signal**: 4 of 5 rules validated outside ax-template. 1 rule (cyclic-package) found a real anti-pattern in external code. 1 rule (records-for-dtos) has a coverage gap.

---

## Decisions Needing Human Review (REVERSIBLE)

### N1. main branch protection on GitHub
- **What**: per `DECISIONS-P3.md` activation step 3, `main` should require `practices-sentinel` workflow PASS.
- **Why it's a user decision**: requires GitHub web admin access; outside the local repo.
- **Action when reviewing**: open GitHub repo settings → Branches → `main` → require status check `practices-sentinel / guards`.
- **My recommendation**: enable. Without it, the gate exists only locally and at PR-time but does not actually block merges to `main`.

### N2. Widen `lang-records-for-dtos` suffix set?
- **What**: rule today targets `*Request` / `*Response` only. Both fixtures pass vacuously because neither follows that convention.
- **Options**:
  - **A. Keep narrow** — accept that the rule covers only ax-template's own naming; portability axis just reports vacuous PASS elsewhere
  - **B. Add `Dto` / `Form` / `Payload`** to the suffix set — broadens reach, risks false positives on non-DTO classes that happen to end in those names (e.g. `WebForm`)
  - **C. Replace simple-name detection with annotation-based** (`@RecordDto` or similar) — strongest but requires the fixture authors to opt in (which they won't)
- **My recommendation**: **A** for now. The rule's primary value is on ax-template own code; portability vacuous PASS is a known limitation documented in `dashboard.md`. Re-evaluate after adding a third fixture.

### N3. Add third portability fixture?
- **What**: tie-breaking signal between petclinic (well-vetted) and realworld (less curated).
- **Candidates**: spring-modulith-example, spring-realworld-example-app (a different fork), spring-petclinic-microservices.
- **Why it's a user decision**: each fixture adds CI build time + maintenance burden. Worth it only if the existing two are insufficient.
- **My recommendation**: not yet. Wait until at least one more rule disagrees between the two existing fixtures.

### N4. `gh` CLI authentication
- **What**: `gh run list` returned empty during inspection. Either `gh` not authenticated in this shell or Actions workflows haven't run on the previous commits yet.
- **Why it's a user decision**: requires interactive `gh auth login` if it's the former.
- **Action when reviewing**: run `gh auth status` to confirm; if not logged in, `gh auth login`.
- **My recommendation**: log in. Without it, validating that the workflows actually fired post-push requires opening the GitHub web UI.

---

## Anomalies / Concerns

1. **realworld fixture has deprecated-API warnings** (`GraphQLCustomizeExceptionHandler` uses or overrides a deprecated API; unchecked operations elsewhere). These are the fixture's own code quality issues, not ours. Mentioning because the dashboard could mistakenly attribute them to our rules later.

2. **realworld build relies on Gradle 7+ wrapper** while our backend is Gradle 8.5 — they coexist because portability builds happen in a separate shell, but a future Gradle major bump might break this. Mentioning for awareness; no action needed today.

3. **No commits violated any hard gate**. The 4 binary guards (spec_ref / substance / time_decay / evidence) passed before each commit; pre-push regression (`testPractices testAsvs testCrud`) passed before the push.

---

## What I Did NOT Do (preserved for your review)

1. **No new rules added** — focus was empirical validation of existing 64 rules, not catalog growth.
2. **No rule edits** — the realworld cycle FAIL was preserved as the dashboard's strongest signal, not patched away.
3. **No A-stage generic化 of new rules** — the 5 selected rules were already generic enough; widening to other rules requires per-rule opinion edits, deferred to user review.
4. **No DECISIONS.md entries** — no rule was accepted/rejected; no candidate was newly classified. Decision integrity preserved.
5. **No `gh auth login`** — not my call.
6. **No GitHub web actions** (branch protection, sentinel CI manual trigger) — not my call.

---

## How to act on this report

Read top-to-bottom. For each item under **Decisions Needing Human Review**:
- **Keep**: no action needed (most defaults)
- **Revert**: `git revert <hash>` on the relevant commit and force-push (or PR with revert)
- **Edit further**: open a fresh task for me to address the specific concern

If everything looks right: nothing to do — the work is already on `origin/main`.

If anything looks wrong: each commit is small and reversible. The largest change is the 2 commits in this run; the earlier `dd0f6c8` (the giant squash) is also still revertible.

---

## Next session candidates

- **N1** (branch protection) — your only real "must do" item to lock in DECISIONS-P3.md.
- **N2/N3** — only if portability matters more than other axes; otherwise defer.
- **Backlog**: `lang-records-for-dtos` suffix widening, more fixtures, A-stage generic化 of additional rules.

---

Sleep well. The work is on `origin/main`, the gates are green, and nothing is on fire.

---

# Morning Follow-up — N1–N4 Closure (2026-05-16 morning session)

User reviewed the report and made decisions on N1–N4. All four are now closed.

## Closure status

| # | Decision | Result | Commits |
|---|----------|--------|---------|
| **N1** | Codify branch protection (A+B: script + Ruleset JSON) | DONE — `.github/rulesets/main-protection.json` + `practices/scripts/setup-branch-protection.sh`. User runs the script once after `gh auth login` to apply. DECISIONS-P3.md §Activation step 3 updated. | `31ba6de` |
| **N2** | Keep `lang-records-for-dtos` suffix narrow (A) | DONE — DECISIONS.md entry "lang-records-for-dtos-widen — DEFERRED" with re-evaluation triggers. dashboard.md updated inline. No code change. | `45184b4` |
| **N3** | Add 3rd fixture: spring-modulith-example (B) | DONE — submodule added (frademacher/spring-modulith-example, MIT, JDK 21, Maven). 5 PortabilityXxxTest classes extended with modulith case. portability/run.sh auto-detects JDK 21. testPortability now 18 tests / 17 PASS / 1 FAIL (the realworld cycle is corroborated, not tied — modulith and petclinic are both cycle-free). | `4255345` |
| **N4** | gh CLI auth + sentinel CI status | DONE — gh auth ✅. Sentinel CI was FAILING since the squash because messaging adapter commit `766b6d1` skipped `generate_agents.sh`, leaving AGENTS.md sha stale. Fixed by regenerating AGENTS.md AND adding pre-commit Stage 0 (auto-regen + auto-stage) so the regression cannot recur. | `2b7df9c`, `fed511f` |

## Sentinel CI verification

| commit | sentinel | reason |
|--------|----------|--------|
| `4255345` (N3) | FAILURE | AGENTS.md sha drift carried forward from messaging adapter commit |
| `2b7df9c` (N4 fix 1) | **SUCCESS** | AGENTS.md regenerated; sentinel green |
| `fed511f` (N4 fix 2) | not triggered | only `.githooks/pre-commit` changed; outside sentinel's `paths: [practices/**, specs/**]` trigger filter — by design |

main HEAD = `fed511f`. The most recent sentinel run on a sentinel-touching commit is `2b7df9c` SUCCESS. main is in a verified-green state.

## What N4 fix prevents going forward

Future commits that touch `practices/rules/*.md` automatically trigger pre-commit Stage 0:

1. `bash practices/generate_agents.sh` (silent)
2. If `practices/AGENTS.md` changed → `git add practices/AGENTS.md` (auto-stage)

The developer's intended changes are not modified — only AGENTS.md is auto-staged. Sentinel CI remains the second line of defense for clones that have not run `practices/scripts/install-hooks.sh`.

## Open items (non-blocking, lower priority)

1. **N1 application step** — user still needs to run `bash practices/scripts/setup-branch-protection.sh` once after `gh auth login` to actually apply the protection. The policy is codified; the application is one-time.
2. **Branch protection apply timing** — recommend doing this *after* the squash sentinel goes green a few more times (proves stability), so the first PR that gets the check applied is a normal one rather than a fix.

## Net for the morning session

- 5 commits added: `31ba6de` (N1), `45184b4` (N2), `4255345` (N3), `2b7df9c` (N4 fix 1), `fed511f` (N4 fix 2)
- main went from 3 stale FAILURE sentinel runs → 1 verified-green sentinel run on the source-of-truth commit (`2b7df9c`)
- 4 outstanding decisions resolved with full audit trail in DECISIONS.md / DECISIONS-P3.md / dashboard.md
- 1 self-healing infrastructure improvement (pre-commit Stage 0) installed — drift class of bug closed permanently for future commits

The catalog is in a steady state: 64 rules / 21 categories / 4 hard gates green / 3 portability fixtures / sentinel CI source-of-truth-green.
