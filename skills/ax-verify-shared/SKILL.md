---
name: ax-verify-shared
description: >
  Tier-2 shared-artifacts axis verifier. Validates Spec Trio files (specs/, contracts/,
  blueprints/), AGENTS.md sentinel integrity, and templates/DECISIONS.md ADR
  completeness. Auto-triggers when editing cross-stack artifacts. Called by ax-verify.
metadata:
  priority: 2
  tier: 2
  axis: language
  docs:
    - "https://spec.openapis.org/oas/v3.1.0"
    - "https://owasp.org/www-project-application-security-verification-standard/"
  pathPatterns:
    - 'specs/**'
    - 'contracts/**'
    - 'blueprints/**'
    - 'templates/DECISIONS.md'
    - 'templates/AGENTS.md'
  bashPatterns: []
  importPatterns: []
retrieval:
  aliases:
    - ax-verify-shared
    - verify shared
    - verify spec trio
    - verify contracts
    - verify blueprints
  intents:
    - verify Spec Trio files
    - check AGENTS.md sentinel
    - validate ADR completeness
    - check OpenAPI contracts
  entities:
    - Spec Trio
    - AGENTS.md
    - DECISIONS.md
    - OpenAPI
    - trio_integrity_guard
---

# ax-verify-shared

Tier-2 shared-artifacts axis verifier. Covers cross-stack artifacts that belong to
neither Java nor React exclusively: the Spec Trio files (`specs/`, `contracts/`,
`blueprints/`), AGENTS.md sentinel hash, and `templates/DECISIONS.md` ADR registry.

Auto-triggers when any of these files change. Does not run language-specific tests.
Part of the 3-tier topology: called by `/ax-verify` (Tier-1).

## Workflow checklist (copyable per Anthropic best-practices)

Copy this checklist and check off as you progress:
- [ ] Step 1: Run spec_ref_guard — confirm all rules reference a spec item
- [ ] Step 2: Run trio_integrity_guard — confirm Spec Trio completeness per domain
- [ ] Step 3: Regenerate AGENTS.md — `bash templates/generate_agents.sh`; confirm sha256 unchanged
- [ ] Step 4: Validate DECISIONS.md — confirm each ADR has `provenance_class` field
- [ ] Step 5: Validate OpenAPI contracts — `npx @redocly/cli lint contracts/**/*.yaml`

## Steps detail

### Step 1: spec_ref_guard
Script: `practices/evals/spec_ref_guard.sh`.
Covers both `practices/rules/**` and `practices-react/rules/**`.
Exit 0 = all rules have `spec_ref`.

### Step 2: trio_integrity_guard
Script: `practices/evals/trio_integrity_guard.sh`.
Reads `practices/evals/trio_integrity_allowlist.yaml` for domain modes.
Exit 0 = all `full_trio` domains have matching backend + frontend Spec Trio;
`backend_only` domains have `frontend_required: false`; `frontend_only` domains
have non-null `static_source_ref` on every item.

### Step 3: AGENTS.md sentinel
Run `bash templates/generate_agents.sh`. Compare output sha256 to stored sentinel.
Any diff means either the catalog changed without regenerating AGENTS.md, or the
generator was modified. Correct the source of truth and regenerate.

### Step 4: ADR completeness
Script: `skills/ax-verify-shared/scripts/check-decisions.sh`.
Reads `templates/DECISIONS.md`. Asserts each ADR block contains `provenance_class:`.
Exit 1 with `MISSING_PROVENANCE_CLASS: TD-2026-05-17-NNN` on first failure.

### Step 5: OpenAPI lint
Advisory: failures are logged but do not cause exit non-zero unless `--strict` flag is set.

## Bundled scripts
- `skills/ax-verify-shared/scripts/run.sh` — orchestrates steps 1–4 (step 5 advisory); exit 0 iff 1–4 pass
- `skills/ax-verify-shared/scripts/check-decisions.sh` — ADR provenance_class validator; exit 0 on pass
- `skills/ax-verify-shared/scripts/regen-agents.sh` — thin wrapper around `templates/generate_agents.sh`; exits non-zero if sha changes

## Feedback loop
When trio_integrity_guard fails: read the error code (`MISSING_FRONTEND_SPEC`,
`COVERAGE_SHORTFALL`, `ZERO_SCAN`, `MISSING_OPERATION_ID`). The error code
names the spec file or field that is absent. Add or fix that artifact, then
re-run the guard in isolation.
Halt threshold: 3 consecutive failures on the same domain → escalate.

## Invocation graph
- Calls (Tier-3): `ax-guard-spec-ref`, `ax-guard-trio-integrity`
- Called by (Tier-1): `ax-verify`

## Acceptance (binary)
```bash
bash skills/ax-verify-shared/scripts/run.sh
# Expected: exit 0
```
