---
name: ax-guard-evidence
description: >
  Tier-3 evidence guard wrapper. Thin skill wrapper around practices/evals/evidence_guard.sh.
  Verifies every rule/.md and every template artifact carries an evidence: block with
  an external URL/quote/RFC/JEP citation. Invoked by Tier-2 skills only — NOT pathPattern-triggered.
metadata:
  priority: 4
  tier: 3
  axis: concern
  docs:
    - "https://owasp.org/www-project-application-security-verification-standard/"
  pathPatterns: []
  bashPatterns:
    - 'bash practices/evals/evidence_guard.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-guard-evidence
    - evidence guard
    - check evidence
    - verify evidence block
  intents:
    - verify evidence blocks in rules
    - check external citation presence
    - guard against un-anchored rules
  entities:
    - evidence_guard.sh
    - evidence block
    - upstream snapshot
    - external citation
---

# ax-guard-evidence

Tier-3 evidence guard wrapper. Wraps `practices/evals/evidence_guard.sh`.
This guard is the 4th hard gate — every rule and template artifact must anchor
to external facts (upstream snapshot, RFC, JEP, vendor docs, or peer-reviewed paper).
A rule with an empty `evidence:` block, a placeholder URL, or a missing block
is BLOCKED from the catalog.

NOT pathPattern-triggered. Invoked exclusively by Tier-2 skills.

## Workflow checklist (copyable per Anthropic best-practices)

- [ ] Step 1: Identify scope (full repo or specific directory path passed as arg)
- [ ] Step 2: Run `bash practices/evals/evidence_guard.sh [<scope-path>]`
- [ ] Step 3: Read output — each violation is `MISSING_EVIDENCE: <file>` or `PLACEHOLDER_URL: <file>`
- [ ] Step 4: Fix each violation: add or correct the `evidence:` block
- [ ] Step 5: Re-run guard — confirm exit 0

## Steps detail

### Step 2: evidence_guard.sh
Script: `practices/evals/evidence_guard.sh`.
Optional arg: `<scope-path>` (directory or glob). Defaults to full repo scan
(`practices/rules/**` + `practices-react/rules/**` + `templates/**`).
Exit 0 = all artifacts have valid `evidence:` blocks.
Exit non-zero = first failing file path + error code on stderr.

Two valid evidence forms:
- `upstream_id` referencing a `practices/upstream/_MANIFEST.yaml` snapshot entry
  with a `section` + `quoted_substring`
- `source_type: external` with a `citation`, `url`, and `quoted_excerpt` ≥ 20 chars

Placeholders (`TODO`, `http://example.com`, empty string) are rejected.

## Bundled scripts
- `skills/ax-guard-evidence/scripts/run.sh` — thin wrapper; passes args to `evidence_guard.sh`; exits with the guard's exit code

## Feedback loop
When guard fails: open the named file. Locate the `evidence:` block (or add one).
For `upstream_id` form: confirm the snapshot file exists in `practices/upstream/`
and the `_MANIFEST.yaml` entry is present with a non-empty `quoted_substring`.
For `external` form: add a real URL and a ≥ 20 char quoted excerpt.
Re-run guard on the single file: `bash practices/evals/evidence_guard.sh <file>`.

## Invocation graph
- Calls: `practices/evals/evidence_guard.sh` (the actual implementation; lives in SP3)
- Called by (Tier-2): `ax-verify-java`, `ax-verify-react`, `ax-verify-shared`, `ax-verify-L1`, `ax-verify-L2`, `ax-verify-L3`, `ax-verify-L4`, `ax-verify-domain`

## Acceptance (binary)
```bash
bash practices/evals/evidence_guard.sh
# Expected: exit 0
```
