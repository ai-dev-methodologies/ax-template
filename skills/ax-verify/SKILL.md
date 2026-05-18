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
    - 'bash skills/ax-verify/scripts/_legacy-call-compat.sh'
    - 'bash skills/ax-verify/scripts/policy-check.sh'
    - 'bash skills/ax-verify/scripts/evidence-fetch.sh'
    - 'bash skills/ax-verify/scripts/explain.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-verify
    - verify all
    - run all checks
    - full suite
    - binary pass fail
    - policy-check
    - evidence-fetch
    - explain rule
    - pre-execution gate
    - evidence freshness
    - rule lookup
  intents:
    - verify the entire repo
    - check all guards pass
    - run end-to-end verification
    - confirm SP is done
    - check which rules apply before editing
    - look up a rule explanation
    - check evidence freshness
  entities:
    - run-all.sh
    - policy-check.sh
    - evidence-fetch.sh
    - explain.sh
    - _legacy-call-compat.sh
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

---

## Subcommands (SP29 — F13/F14/F15)

`/ax-verify` now supports subcommands via `_legacy-call-compat.sh`. The Tier-1 cap
stays at 4 (ax-transform · ax-verify · ax-scaffold · ax-fork-receiver) — these are
extensions of the existing ax-verify skill, not new Tier-1 skills.

### Subcommand routing table

| Invocation | Script | Purpose |
|---|---|---|
| `/ax-verify all` | `run-all.sh` | Full verification suite (legacy, preserved) |
| `/ax-verify guards` | `run-all-guards.sh` | Guards only (legacy, preserved) |
| `/ax-verify backend` | `run-backend.sh` | Gradle backend tests only (legacy) |
| `/ax-verify frontend-unit` | `run-frontend-unit.sh` | Vitest unit tests only (legacy) |
| `/ax-verify e2e` | `run-e2e.sh` | Playwright E2E only (legacy) |
| `/ax-verify policy-check` | `policy-check.sh` | F13: pre-execution rule gate |
| `/ax-verify evidence-fetch` | `evidence-fetch.sh` | F14: evidence freshness check |
| `/ax-verify explain` | `explain.sh` | F15: rule explanation lookup |

### F13 — policy-check (pre-execution gate)

AI agents invoke this **before editing files** to discover applicable catalog rules.

```bash
# Discover rules for a domain tag
bash skills/ax-verify/scripts/policy-check.sh --domain persistence

# Infer domain from a file path
bash skills/ax-verify/scripts/policy-check.sh --file backend/src/main/java/com/example/UserRepository.java

# Look up a specific rule summary
bash skills/ax-verify/scripts/policy-check.sh --rule PRACTICES-PERS-005

# JSON output for programmatic consumption
bash skills/ax-verify/scripts/policy-check.sh --domain error --format json

# List all known tags
bash skills/ax-verify/scripts/policy-check.sh --list-tags
```

Exit 0 = lookup complete (0 or more rules printed). The agent must read the output
and comply with all RULE entries before writing code.

### F14 — evidence-fetch (evidence freshness check)

Detects catalog rules with missing, empty, or broken evidence blocks.

```bash
# Check all rules in practices/ catalog
bash skills/ax-verify/scripts/evidence-fetch.sh --all

# Check a specific rule
bash skills/ax-verify/scripts/evidence-fetch.sh --rule PRACTICES-PERS-005

# Also probe upstream URLs (requires network; slow)
bash skills/ax-verify/scripts/evidence-fetch.sh --all --http-check

# Check practices-react catalog
bash skills/ax-verify/scripts/evidence-fetch.sh --all --catalog practices-react
```

Exit 0 = all checked rules have valid evidence. Exit 1 = issues found.
Issues detected: `no_evidence_block`, `empty_evidence_block`, `unknown_upstream_id`,
`external_missing_url`, `external_missing_citation`.

### F15 — explain (rule explanation lookup)

Returns a structured explanation of any catalog rule.

```bash
# Look up by spec_id
bash skills/ax-verify/scripts/explain.sh PRACTICES-PERS-005

# Look up by keyword (searches title, tags, filename)
bash skills/ax-verify/scripts/explain.sh soft-delete

# JSON output
bash skills/ax-verify/scripts/explain.sh --format json PRACTICES-ERR-001

# List all rule IDs with titles
bash skills/ax-verify/scripts/explain.sh --list
```

Exit 0 = rule found. Exit 1 = not found.

### Backward compatibility

All legacy `/ax-verify <step>` calls continue to work unchanged:

```bash
bash skills/ax-verify/scripts/_legacy-call-compat.sh all
bash skills/ax-verify/scripts/_legacy-call-compat.sh guards
bash skills/ax-verify/scripts/_legacy-call-compat.sh backend
bash skills/ax-verify/scripts/_legacy-call-compat.sh frontend-unit
bash skills/ax-verify/scripts/_legacy-call-compat.sh e2e
bash skills/ax-verify/scripts/_legacy-call-compat.sh policy-check --domain persistence
bash skills/ax-verify/scripts/_legacy-call-compat.sh explain PRACTICES-PERS-005
```

### Eval set

50-fixture false-positive rate eval lives at `practices/evals/fixtures/policy-check/`.
Run via: `bash skills/_tests/policy-check-fp-rate.test.sh`
Acceptance: FP rate < 5% (at most 1 phantom rule per 25 pass-fixture assertions).
