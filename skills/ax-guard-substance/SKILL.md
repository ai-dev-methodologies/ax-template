---
name: ax-guard-substance
description: >
  Tier-3 substance guard wrapper. Thin skill wrapper around practices/evals/substance_guard.sh.
  Verifies each rule has a meaningful explanation (≥ 3 non-compliant + compliant code pair,
  rationale ≥ 50 chars). Blocks thin/placeholder rules from the catalog.
  Invoked by Tier-2 skills only — NOT pathPattern-triggered.
metadata:
  priority: 4
  tier: 3
  axis: concern
  docs: []
  pathPatterns: []
  bashPatterns:
    - 'bash practices/evals/substance_guard.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-guard-substance
    - substance guard
    - check rule substance
    - verify rule quality
  intents:
    - verify rule explanation quality
    - check compliant/non-compliant code pairs
    - guard against thin rules
  entities:
    - substance_guard.sh
    - rule quality
    - compliant code
    - non-compliant code
---

# ax-guard-substance

Tier-3 substance guard wrapper. Wraps `practices/evals/substance_guard.sh`.
The substance gate blocks rules that are too thin to be actionable: a rule
must explain WHY it matters, show a non-compliant example, and show a compliant
example. Rationale must be ≥ 50 chars (prevents one-liners posing as rules).

NOT pathPattern-triggered. Invoked exclusively by Tier-2 skills.

## Workflow checklist (copyable per Anthropic best-practices)

- [ ] Step 1: Identify scope (full repo or specific rule path)
- [ ] Step 2: Run `bash practices/evals/substance_guard.sh [<scope-path>]`
- [ ] Step 3: Read output — violations are `MISSING_CODE_PAIR: <file>` or `THIN_RATIONALE: <file>`
- [ ] Step 4: Fix violations: add non-compliant/compliant code blocks; expand rationale
- [ ] Step 5: Re-run guard — confirm exit 0

## Steps detail

### Step 2: substance_guard.sh
Script: `practices/evals/substance_guard.sh`.
Optional arg: `<scope-path>`. Defaults to `practices/rules/**` + `practices-react/rules/**`.
Checks:
1. `## Non-compliant` section exists with a fenced code block ≥ 3 lines
2. `## Compliant` section exists with a fenced code block ≥ 3 lines
3. A rationale paragraph exists with ≥ 50 chars
Exit 0 = all rules pass; non-zero = first failing rule + reason code on stderr.

## Bundled scripts
- `skills/ax-guard-substance/scripts/run.sh` — thin wrapper; passes args to `substance_guard.sh`; exits with the guard's exit code

## Feedback loop
When `MISSING_CODE_PAIR` fires: add `## Non-compliant` and `## Compliant` sections
with real Java/TypeScript code examples (≥ 3 lines each).
When `THIN_RATIONALE` fires: expand the rationale paragraph to explain the root cause
of the problem and the consequence of ignoring the rule.

## Invocation graph
- Calls: `practices/evals/substance_guard.sh`
- Called by (Tier-2): `ax-verify-java`, `ax-verify-react`, `ax-verify-domain`

## Acceptance (binary)
```bash
bash practices/evals/substance_guard.sh
# Expected: exit 0
```
