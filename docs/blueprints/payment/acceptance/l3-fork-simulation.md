# L3 Fork Simulation — Payment Blueprint

**Date**: 2026-05-17
**Fork path**: `/tmp/ax-payment-test-l3`
**Source commit**: `2663cf830f0aa49d823aa477453252ff62d6c5be`
**Agent**: Wave 8 Agent A (Opus) — US-019
**Wall-clock**: ~191 s end-to-end (clone + 5 gates)

## Verdict

**PASS** — All 5 verification gates exit 0 in a fresh clone with zero source-repo modifications. The Payment blueprint is fully self-contained for fork-receiving teams.

## Verification commands + results

### 1. Gradle full test sweep

**Command**:
```bash
cd /tmp/ax-payment-test-l3/backend && ./gradlew test -q 2>&1 | tail -20
```

**Result**: **PASS** — exit 0 in **81 s**.

**Aggregate (parsed from `build/test-results/test/TEST-*.xml`, 95 test classes)**:
- Tests: **234**
- Failures: 0
- Errors: 0
- Skipped: 18 (expected — disabled probes / advisory tests)

Includes `testAsvs` + `testCrud` + `testRateLimit` + `testPayment` + `testPractices` (run.sh per `plan.md` line 290–297). Spring Boot context loads, H2 schema includes payment tables (`payments`, `payment_events`, `refunds`, `provider_links`), all Hibernate teardowns clean.

### 2. `practices/evals/run.sh` (Java catalog probe)

**Command**:
```bash
cd /tmp/ax-payment-test-l3 && bash practices/evals/run.sh 2>&1 | tail -10
```

**Result**: **PASS** — exit 0.

```
[run.sh] Report written: /tmp/ax-payment-test-l3/practices/evals/reports/2026-05-17.md
[run.sh] TOTAL: see per-axis scores (weighted advisory sum not yet implemented)
```

Report contents (per `practices/evals/reports/2026-05-17.md`):
- Rules audited: **68**
- `reference` axis: **68/68 rules have spec_ref**
- `outcome` axis: testPractices wall-clock 10.83 s; Semgrep/SpotBugs N/A (not installed — advisory, non-blocking)
- `portability` axis: 3 fixtures listed as **SKIP (no mvnw/gradlew)** — see "Issues" note below
- `binary_only: true` confirmed from rubric.yaml

### 3. Four Java hard gates

**Commands** (run sequentially, each must exit 0):
```bash
bash practices/evals/spec_ref_guard.sh    # exit 0 (silent)
bash practices/evals/substance_guard.sh   # exit 0 — "all rules pass"
bash practices/evals/time_decay_guard.sh  # exit 0 — "all snapshots within 90d threshold"
bash practices/evals/evidence_guard.sh    # exit 0 — "all rules have auditable evidence"
```

**Result**: **PASS** — all 4 gates exit 0.

These are the binary anchoring gates per `CLAUDE.md` and `plan.md` line 296 — they enforce that every rule has `spec_ref`, substance markers, fresh upstream snapshots (<90d), and traceable evidence (URL/quote or upstream_id).

### 4. `practices-react/evals/run.sh` (React catalog probe)

**Command**:
```bash
cd /tmp/ax-payment-test-l3 && bash practices-react/evals/run.sh 2>&1 | tail -10
```

**Result**: **PASS** — exit 0.

```
── spec_ref_guard ──
  PASS
── time_decay_guard ──
time_decay_guard: all snapshots within 90d threshold
  PASS
── evidence_guard ──
evidence_guard: all rules have auditable evidence
  PASS

practices-react/evals/run.sh: all 3 gates passed
```

3/3 React hard gates pass (the React surface has 3 gates vs Java's 4 — `substance_guard` is folded into the rule schema validation on the React side, per `practices-react/SKILL.md`).

### 5. ESLint plugin tests (`@ax/eslint-plugin-ax`)

**Commands**:
```bash
cd /tmp/ax-payment-test-l3/practices-react/eslint-plugin-ax
npm install   # 1 s — 86 packages, 0 vulnerabilities
npm test      # 0 s — 7/7 RuleTester suites pass
```

**npm install result**: exit 0 — added 86 packages in 1 s (uses npm cache), no vulnerabilities.

**npm test result**: **PASS** — exit 0, 7/7 RuleTester suites pass in 166 ms.

```
✔ ax/no-array-includes-in-loop — RuleTester suite (18.76ms)
✔ ax/no-array-mutate-on-state — RuleTester suite (18.73ms)
✔ ax/no-broad-barrel-imports — RuleTester suite (17.29ms)
✔ ax/no-falsy-numeric-render — RuleTester suite (23.08ms)
✔ ax/no-inline-component-definition — RuleTester suite (20.60ms)
✔ ax/prefer-functional-setstate — RuleTester suite (22.65ms)
✔ react-async-parallel — RuleTester suite (38.96ms)
ℹ tests 7, pass 7, fail 0
```

## Issues found

**None blocking.** Two observations on fork-bring-up behavior, neither a regression:

1. **Submodules not auto-cloned (expected behavior)**.
   `practices/evals/fixtures/{spring-petclinic, spring-realworld, spring-modulith-example}` are git submodules (per `.gitmodules`). A fresh `git clone` (without `--recurse-submodules`) leaves these directories empty, so `run.sh` portability axis lists all 3 as `SKIP (no mvnw/gradlew)`. This is documented graceful degradation — `testPortability` is **advisory-only** per `CLAUDE.md` ("외부 fixture에 룰 적용") and does not gate fork bring-up. Fork-receivers who want portability coverage run `git submodule update --init --recursive`; teams that don't care about it can ignore the SKIPs. No fix needed.

2. **`--depth 1` ignored for local clones (expected behavior)**.
   Git emits `warning: --depth is ignored in local clones; use file://` because the source path is on the same filesystem. Clone still completes in 1 s. No impact on verification. If this is run against a remote URL in CI it would honor `--depth 1` as intended.

**No real regressions detected.** No missing config files, no implicit dev-machine dependencies surfaced, no cache-warming required (the Gradle daemon cold-started fresh and finished in 81 s; npm install used the user's cached registry and completed in 1 s).

## Surprises / notes for fork-receivers

- **Gradle cold-start was fast (81 s for 234 tests across 95 classes)** — well below the `plan.md` "23-phase budget" expectations. The dependency graph is reasonably scoped (no superfluous Spring starters), and the JPA `ddl-auto: create-drop` keeps schema bootstrap deterministic per test class.
- **JDK 21 auto-detection works** — Gradle resolved JAVA_HOME → `/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home` automatically via toolchain config (per `backend/build.gradle.kts`). Fork-receivers on Linux/Windows running JDK 21 should see the same auto-detection (toolchain spec is platform-neutral).
- **Hard gates are fast (<1 s each)** — all 4 Java gates + 3 React gates complete in well under a second total. They are merge-loop-friendly.
- **No `gradle/wrapper/gradle-wrapper.jar` issue** — fresh clone has the wrapper jar; it didn't need re-downloading.
- **ESLint plugin's `package-lock.json` is committed** — `npm install` in fresh clone uses the lock and completes in 1 s with 0 vulnerabilities. Clean.

## Cleanup

```bash
rm -rf /tmp/ax-payment-test-l3
```

Completed at **2026-05-17 19:22 KST**.
