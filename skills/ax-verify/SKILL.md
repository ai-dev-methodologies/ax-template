---
name: ax-verify
description: >
  Tier-1 recursive verification skill. Chains all 6 guards + backend Gradle tests
  + frontend Vitest + Playwright E2E. Use as the single pass/fail signal for any SP
  or PR. Invoke after any non-trivial change to confirm the full repo is green.
metadata:
  priority: 1
  tier: 1
  axis: root
  docs:
    - "https://docs.gradle.org/current/userguide/userguide.html"
    - "https://vitest.dev/guide/"
    - "https://playwright.dev/docs/intro"
  pathPatterns:
    - 'skills/ax-verify/SKILL.md'
  bashPatterns:
    - 'bash skills/ax-verify/scripts/run-all.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-verify
    - verify all
    - run all checks
    - full suite
    - binary pass fail
  intents:
    - verify the entire repo
    - check all guards pass
    - run end-to-end verification
    - confirm SP is done
  entities:
    - run-all.sh
    - guards
    - testDomain
    - Playwright
    - Vitest
---

# ax-verify

Tier-1 recursive verification skill. The single source of truth for repo health.
Every SP in the PRD terminates only when this skill exits 0.

`/ax-verify` chains, in order:
1. All 6 guard scripts via `practices/evals/run-all-guards.sh`
2. `./gradlew test` (all backend domain tags)
3. `npm run test` (frontend Vitest unit tests)
4. `npx playwright test` (Playwright E2E)

Agents MUST NOT declare an SP complete on prose alone. The binary exit code
of this skill is the SP termination condition.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Confirm working directory is repo root
- [ ] Step 2: Run guards — `bash practices/evals/run-all-guards.sh --include-fixtures`
- [ ] Step 3: Run backend — `cd backend && ./gradlew test`
- [ ] Step 4: Run frontend unit — `cd frontend && npm run test`
- [ ] Step 5: Run Playwright E2E — `cd frontend && npx playwright test`
- [ ] Step 6: Confirm all steps exit 0 — report PASS; if any fails, report FAIL + step number + stderr

## Steps detail

### Step 2: Guards
Script: `practices/evals/run-all-guards.sh`
Args: `--include-fixtures` to also run pass/fail fixture assertions.
Exit semantics: 0 = all guards pass; non-zero = first failing guard name + error code on stderr.

### Step 3: Backend
Script: `skills/ax-verify/scripts/run-backend.sh` (delegates to Gradle).
The script runs `cd backend && ./gradlew test` which executes all registered
`test{Domain}` tasks. Exit 0 = all domain tests pass.

### Step 4: Frontend unit
Script: `skills/ax-verify/scripts/run-frontend-unit.sh`.
Runs `cd frontend && npm run test -- --run` (Vitest non-watch mode).
Exit 0 = all unit tests pass.

### Step 5: Playwright
Script: `skills/ax-verify/scripts/run-e2e.sh`.
Runs `cd frontend && npx playwright test`.
Exit 0 = all E2E specs pass.

## Bundled scripts
- `skills/ax-verify/scripts/run-all.sh` — orchestrator; chains steps 2–5; exit 0 iff all pass
- `skills/ax-verify/scripts/run-backend.sh` — Gradle wrapper; exit semantics mirror Gradle
- `skills/ax-verify/scripts/run-frontend-unit.sh` — Vitest non-watch; exit 0 on pass
- `skills/ax-verify/scripts/run-e2e.sh` — Playwright runner; exit 0 on pass

## Feedback loop
When any step fails: stderr contains the tool's native error output.
Read the first ERROR or FAILED line to identify the failing test/guard.
Fix only the artifact that caused the failure. Re-run only the failing step
to confirm the fix before re-running the full chain.
Halt threshold: if the same step fails 3 consecutive times after fixes,
write an ESCAPE file at `docs/superpowers/escape/<timestamp>.md` and halt.

## Invocation graph
- Calls (Tier-2): `/ax-verify-java`, `/ax-verify-react`, `/ax-verify-shared` (for targeted re-runs)
- Calls (Tier-3): all 6 guards (via `run-all-guards.sh`)
- Called by (Tier-1): user directly; also called by CI gate

## Acceptance (binary)
```bash
bash skills/ax-verify/scripts/run-all.sh
# Expected: exit 0
```
