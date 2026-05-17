---
name: ax-verify-java
description: >
  Tier-2 Java/Spring axis verifier. Runs backend Gradle test suite + spec_ref_guard
  + evidence_guard scoped to backend/ and templates/backend/. Auto-triggers when
  editing backend source or backend templates. Called by ax-verify Tier-1.
metadata:
  priority: 2
  tier: 2
  axis: language
  docs:
    - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
    - "https://docs.gradle.org/current/userguide/userguide.html"
  pathPatterns:
    - 'backend/**'
    - 'templates/backend/**'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-java
    - verify java
    - verify spring
    - verify backend
    - java axis
  intents:
    - verify backend code changes
    - run Gradle tests
    - check Java rules compliance
    - verify backend templates
  entities:
    - Gradle
    - Spring Boot
    - practices catalog
    - testDomain
---

# ax-verify-java

Tier-2 Java/Spring axis verifier. Auto-triggers when any file under `backend/**`
or `templates/backend/**` is modified. Scoped to the Java language axis only
— it does not run frontend tests or Playwright.

Part of the 3-tier topology: called by `/ax-verify` (Tier-1); does not call
Tier-1 or peer Tier-2 skills. May delegate to Tier-3 guards.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run spec_ref_guard scoped to backend rules
- [ ] Step 2: Run evidence_guard scoped to `backend/**` and `practices/rules/**`
- [ ] Step 3: Run `./gradlew testPractices` — verify 64 practice rules pass
- [ ] Step 4: Run `./gradlew testAsvs` — verify auth ASVS items pass
- [ ] Step 5: Run `./gradlew testCrud` — verify CRUD spec items pass
- [ ] Step 6: Run `./gradlew testPortability` (advisory) — note but do not block on failures

## Steps detail

### Step 1: spec_ref_guard (scoped)
Script: `practices/evals/spec_ref_guard.sh`
Scope: rules under `practices/rules/**/*.md`.
Exit 0 = all rules have `spec_ref`; non-zero = rule name + `MISSING_SPEC_REF` on stderr.

### Step 3: testPractices
Runs `cd backend && ./gradlew testPractices`.
Tag: `@Tag("PRACTICES")`. Covers 64 Java/Spring rules.

### Step 4: testAsvs
Runs `cd backend && ./gradlew testAsvs`.
Tag: `@Tag("ASVS")`. Covers 26 OWASP ASVS L1 items.

### Step 5: testCrud
Runs `cd backend && ./gradlew testCrud`.
Tag: `@Tag("CRUD")`. Covers CRUD-001..005 spec items.

### Step 6: testPortability
Advisory only. Failures are logged but do not cause this skill to exit non-zero.

## Bundled scripts
- `skills/ax-verify-java/scripts/run.sh` — orchestrates steps 1–5; step 6 is advisory; exit 0 iff steps 1–5 all pass
- `skills/ax-verify-java/scripts/run-gradle.sh` — wraps `./gradlew test` with task argument; accepts `<task>` positional arg

## Feedback loop
When Gradle fails: read the `BUILD FAILED` block in stderr. Identify failing test
class and tag. Fix the implementation or spec that the test anchors.
Re-run `./gradlew <failingTask> --tests *FailingClass*` to confirm fix before
running the full suite.
Halt threshold: 3 consecutive failures on the same test class → escalate.

## Invocation graph
- Calls (Tier-3): `ax-guard-spec-ref`, `ax-guard-evidence`
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-java/scripts/run.sh
# Expected: exit 0
```
