---
title: "Every business_invariants entry in a recipe spec YAML must carry spec_ref: or rule_ref: pointing to an existing artifact; unresolvable references are prohibited"
rule_id: recipe-invariants-must-resolve
impact: CRITICAL
impactDescription: "A business invariant with an unresolvable spec_ref or rule_ref cannot be enforced — it is a claim with no evidence chain. Unresolvable references silently degrade the recipe from an enforceable contract to advisory prose, defeating the composition kit's binary-verification guarantee"
tags:
  - recipe-composition
  - invariants
  - referential-integrity
  - evidence-chain
  - spec-trio
provenance_class: internal_design
protects_template_id: specs/recipes/*.yaml
failing_fixture_path: practices/evals/fixtures/recipe-invariants-must-resolve/fail_unresolvable_spec_ref/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ARCH-003"
verification:
  guard: recipe_governance_guard.sh
  type: script
  notes: |
    recipe_governance_guard.sh (SP37) and recipe_spec_referential_integrity_guard.sh (SP35)
    both walk specs/recipes/*.yaml business_invariants list.
    For each entry:
      - spec_ref: → resolve specs/<file>.yaml existence + optional #anchor check
      - rule_ref: → resolve practices/rules/<file>.md existence
    Missing field OR non-existent artifact → VIOLATION, exit 1.
    Zero-invariants is a WARN not a FAIL (recipe may be L2-only).
evidence:
  - source_type: external
    citation: "OWASP ASVS — every security requirement must reference a testable control; untestable requirements provide false assurance and cannot be verified in a security audit"
    url: "https://owasp.org/www-project-application-security-verification-standard/"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## recipe-invariants-must-resolve

**Impact: CRITICAL — Every `business_invariants` entry in a recipe spec YAML must resolve to a real artifact. An invariant with a dangling `spec_ref:` or `rule_ref:` cannot be enforced by any guard, making the recipe's contract unverifiable.**

The composition kit's binary-verification guarantee requires that every business invariant in a recipe traces to either:
- **`spec_ref:`** — an item in an existing `specs/*.yaml` file (e.g., `specs/billing-l0.yaml#BILLING-IDEMP-001`)
- **`rule_ref:`** — an existing rule file in `practices/rules/*.md` (e.g., `practices/rules/billing-event-idempotent.md`)

If the referenced file does not exist, `recipe_governance_guard.sh` exits 1 and blocks merge.

**Incorrect — recipe YAML with unresolvable spec_ref:**

```yaml
# VIOLATION: specs/recipes/saas-subscription-recipe-l0.yaml
business_invariants:
  - id: SAAS-INV-001
    description: "subscription must have ≥1 active plan"
    # VIOLATION: specs/nonexistent-l0.yaml does not exist
    spec_ref: "specs/nonexistent-l0.yaml#NONEXISTENT-001"

  - id: SAAS-INV-002
    description: "usage metering resets on billing cycle boundary"
    # VIOLATION: rule_ref points to non-existent rule file
    rule_ref: "practices/rules/billing-cycle-reset-nonexistent.md"
```

### Failing — business_invariant with neither spec_ref nor rule_ref

```yaml
business_invariants:
  - id: SAAS-INV-003
    description: "feature-gate enforcement matches plan tier"
    # VIOLATION: no spec_ref and no rule_ref — unenforceable invariant
    rationale: "Manually verified during code review"
```

**Correct — all business_invariants resolve to existing artifacts:**

```yaml
# CORRECT: specs/recipes/saas-subscription-recipe-l0.yaml
business_invariants:
  - id: SAAS-INV-001
    description: "subscription must have ≥1 active plan"
    # EXISTS: specs/billing-l0.yaml is a real file on disk
    spec_ref: "specs/billing-l0.yaml#BILLING-AUTHZ-002"

  - id: SAAS-INV-002
    description: "usage metering resets on billing cycle boundary"
    # EXISTS: practices/rules/billing-event-idempotent.md is a real file on disk
    rule_ref: "practices/rules/billing-event-idempotent.md"

  - id: SAAS-INV-003
    description: "feature-gate enforcement matches plan tier"
    # EXISTS: specs/feature-flags-l0.yaml is a real file on disk
    spec_ref: "specs/feature-flags-l0.yaml"
```

### Resolution rules

| Field | Required format | Guard check |
|---|---|---|
| `spec_ref:` | `specs/<file>.yaml` or `specs/<file>.yaml#ANCHOR` | File must exist; anchor is informational |
| `rule_ref:` | `practices/rules/<file>.md` | File must exist |
| Neither | — | VIOLATION — at least one must be present |

## Failing fixture

See: `practices/evals/fixtures/recipe-invariants-must-resolve/fail_unresolvable_spec_ref/recipe.yaml` — `business_invariants` entries reference `specs/nonexistent-l0.yaml` which does not exist.

See: `practices/evals/fixtures/recipe-invariants-must-resolve/pass/recipe.yaml` — all `business_invariants` reference `specs/billing-l0.yaml` which exists.

Reference: https://owasp.org/www-project-application-security-verification-standard/
