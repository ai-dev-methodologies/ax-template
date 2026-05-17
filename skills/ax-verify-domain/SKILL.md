---
name: ax-verify-domain
description: >
  Tier-2 domain-axis verifier. Runs all checks scoped to a single named domain
  (templates/L4/<domain>/ + specs/<domain>-* + contracts/<domain>-* + blueprints/<domain>-*).
  Accepts a required <domain> argument. Invoked by ax-scaffold post-scaffold and by
  ax-verify for targeted domain verification. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: domain
  docs:
    - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
    - "https://nextjs.org/docs/app"
  pathPatterns:
    - 'templates/L4/<domain>/**'
  bashPatterns:
    - 'bash skills/ax-verify-domain/scripts/run.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-domain
    - verify domain
    - domain check
    - verify auth domain
    - verify crud domain
    - verify payment domain
  intents:
    - verify a specific domain end-to-end
    - post-scaffold domain validation
    - check domain Spec Trio completeness
    - run domain-specific Playwright tests
  entities:
    - domain
    - L4
    - Spec Trio
    - trio_integrity_guard
    - Playwright
---

# ax-verify-domain

Tier-2 domain-axis verifier. Unlike language or layer verifiers, this skill is
parameterized: it accepts a `<domain>` argument and scopes all checks to that
domain's artifacts. Used after `/ax-scaffold <domain>` and for targeted
re-verification when only one domain's files changed.

Invoke with: `/ax-verify-domain <domain-name>`

Supported domains: `auth`, `crud`, `payment`, `practices`, and any new domain
added via `/ax-scaffold`.

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Confirm domain is in `practices/evals/trio_integrity_allowlist.yaml`
- [ ] Step 2: Run evidence_guard scoped to `templates/L4/<domain>/`, `specs/<domain>-*`, `contracts/<domain>-*`, `blueprints/<domain>-*`
- [ ] Step 3: Run trio_integrity_guard for this domain only
- [ ] Step 4: Run cross_trio_guard for this domain only
- [ ] Step 5: Run backend Gradle tests for this domain — `./gradlew test{Domain}`
- [ ] Step 6: Run Playwright E2E scoped to `tests/L4/<domain>/`
- [ ] Step 7: Confirm all steps exit 0 — report domain-specific PASS or FAIL

## Steps detail

### Step 1: Allowlist check
Script: `skills/ax-verify-domain/scripts/check-allowlist.sh <domain>`.
Reads `practices/evals/trio_integrity_allowlist.yaml`.
Exit 1 with `DOMAIN_NOT_IN_ALLOWLIST: <domain>` if absent.

### Step 2: evidence_guard (scoped)
Script: `practices/evals/evidence_guard.sh templates/L4/<domain>/`.
Also scans `specs/<domain>-*.yaml`, `contracts/<domain>-*.yaml`, `blueprints/<domain>-*.yaml`.

### Step 3: trio_integrity (domain-scoped)
Script: `practices/evals/trio_integrity_guard.sh --domain <domain>`.
Only checks the named domain; skips all other domains.

### Step 4: cross_trio (domain-scoped)
Script: `practices/evals/cross_trio_guard.sh --domain <domain>`.
Only walks `templates/L4/<domain>/` for import resolution.

### Step 5: Backend tests
Script: `skills/ax-verify-domain/scripts/run-gradle.sh <Domain>`.
Runs `cd backend && ./gradlew test<Domain>` where `<Domain>` is the PascalCase
domain name (e.g., `Auth`, `Crud`, `Payment`).
For `practices` domain (`frontend_only`): step is skipped — no backend Gradle task.

### Step 6: Playwright (domain-scoped)
Script: `skills/ax-verify-domain/scripts/run-playwright.sh <domain>`.
Runs `cd frontend && npx playwright test tests/L4/<domain>/`.

## Bundled scripts
- `skills/ax-verify-domain/scripts/run.sh` — main orchestrator; accepts `<domain>` arg; exit 0 iff steps 1–6 all pass for that domain
- `skills/ax-verify-domain/scripts/check-allowlist.sh` — allowlist membership check; exit 0 if present
- `skills/ax-verify-domain/scripts/run-gradle.sh` — Gradle test runner; accepts PascalCase domain; skips for frontend_only
- `skills/ax-verify-domain/scripts/run-playwright.sh` — scoped Playwright runner; accepts domain name

## Feedback loop
When step 1 fails: run `/ax-scaffold <domain>` first, or manually add to allowlist.
When step 3 fails: fix the Spec Trio artifact identified in the error message.
When step 5 is unexpectedly skipped: confirm domain mode in allowlist is not `frontend_only` incorrectly.
Halt threshold: 3 consecutive failures after fixes → escalate.

## Invocation graph
- Calls (Tier-3): `ax-guard-evidence`, `ax-guard-trio-integrity`, `ax-guard-cross-trio`
- Called by (Tier-1): `ax-verify`, `ax-scaffold` (post-scaffold)

## Acceptance (binary)
```bash
bash skills/ax-verify-domain/scripts/run.sh auth
# Expected: exit 0 (on a green repo)
```
