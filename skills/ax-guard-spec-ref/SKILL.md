---
name: ax-guard-spec-ref
description: >
  Tier-3 spec_ref guard wrapper. Thin skill wrapper around practices/evals/spec_ref_guard.sh.
  Verifies every rule file has a spec_ref field pointing to a real item in a specs/ YAML.
  First hard gate — runs before any other catalog check. Invoked by Tier-2 only — NOT pathPattern-triggered.
metadata:
  priority: 4
  tier: 3
  axis: concern
  docs: []
  pathPatterns: []
  bashPatterns:
    - 'bash practices/evals/spec_ref_guard.sh'
  importPatterns: []
retrieval:
  aliases:
    - ax-guard-spec-ref
    - spec ref guard
    - check spec_ref
    - verify spec reference
    - first hard gate
  intents:
    - verify spec_ref field in rules
    - block rules without spec anchoring
    - run the first hard gate
  entities:
    - spec_ref_guard.sh
    - spec_ref
    - specs/ YAML
    - hard gate
---

# ax-guard-spec-ref

Tier-3 spec_ref guard wrapper. Wraps `practices/evals/spec_ref_guard.sh`.
The first hard gate in the catalog quality system. Every rule `.md` file must
carry a `spec_ref:` field in its frontmatter pointing to a real item ID in a
`specs/` YAML file. A rule without `spec_ref` is rejected before it can
accumulate evidence, substance, or test coverage — preventing orphan rules.

NOT pathPattern-triggered. Invoked exclusively by Tier-2 skills.

## Workflow checklist (copyable per Anthropic best-practices)

- [ ] Step 1: Run `bash practices/evals/spec_ref_guard.sh [<scope-path>]`
- [ ] Step 2: Read output — violations are `MISSING_SPEC_REF: <file>` or `INVALID_SPEC_REF: <file> → <ref>`
- [ ] Step 3: For `MISSING_SPEC_REF`: add a `spec_ref:` field to the rule frontmatter
- [ ] Step 4: For `INVALID_SPEC_REF`: verify the referenced item ID exists in the named `specs/` YAML
- [ ] Step 5: Re-run guard — confirm exit 0

## Steps detail

### Step 1: spec_ref_guard.sh
Script: `practices/evals/spec_ref_guard.sh`.
Optional arg: `<scope-path>`. Defaults to `practices/rules/**` + `practices-react/rules/**`.
Reads frontmatter from each `*.md`. Checks:
1. `spec_ref:` key is present and non-empty
2. The value is of the form `specs/<filename>.yaml#<item-id>`
3. The referenced `specs/<filename>.yaml` exists
4. The `<item-id>` is a defined `id:` in that YAML file
Exit 0 = all rules pass; non-zero = first failing file + reason on stderr.

### Step 3: Adding spec_ref
The `spec_ref` format is: `specs/<domain>-<level>.yaml#<ITEM-ID>`.
Example: `spec_ref: specs/spring-practices-l0.yaml#PRACTICES-PERS-001`.
If the spec item does not yet exist, add it to the YAML first (following the
existing item schema: `id`, `chapter`, `requirement`, `test_method`,
`verification_type`, `applicable`, `notes`).

## Bundled scripts
- `skills/ax-guard-spec-ref/scripts/run.sh` — thin wrapper; passes args to `spec_ref_guard.sh`; exits with guard's exit code

## Feedback loop
When `MISSING_SPEC_REF` fires: the rule frontmatter is incomplete. Add the field.
When `INVALID_SPEC_REF` fires: either the spec YAML filename is wrong, or the
item ID does not exist in that YAML. Open the YAML and verify the item ID exactly.
Never create a fake spec item just to satisfy the guard — the item must represent
a real requirement.

## Invocation graph
- Calls: `practices/evals/spec_ref_guard.sh`
- Called by (Tier-2): `ax-verify-java`, `ax-verify-react`, `ax-verify-shared`, `ax-verify-domain`

## Acceptance (binary)
```bash
bash practices/evals/spec_ref_guard.sh
# Expected: exit 0
```
