---
title: "Every L4 domain README that participates in a Business Pattern Recipe composition must declare applied_recipe: <pattern-name> in its frontmatter metadata block"
rule_id: business-domain-must-declare-applied-recipe
impact: HIGH
impactDescription: "Missing applied_recipe: declaration makes the recipe composition invisible to recipe_governance_guard.sh, breaks the audit trail linking business domains to their governing recipe, and allows ad-hoc composition to drift undetected from the recipe contract"
tags:
  - architecture
  - recipe-composition
  - metadata
  - audit-trail
  - l4-layer
provenance_class: internal_design
protects_template_id: templates/L4/<domain>/README.md
failing_fixture_path: practices/evals/fixtures/business-domain-must-declare-applied-recipe/fail_no_applied_recipe/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-ARCH-002"
verification:
  type: review
  notes: |
    recipe_governance_guard.sh scans every recipes/*/RECIPE.md enabled_l4_domains list.
    For each domain listed, it reads templates/L4/<domain>/README.md and asserts
    the applied_recipe: field is present and matches the recipe pattern name.
    Missing field or wrong value → VIOLATION.
evidence:
  - source_type: external
    anchors: generic_principle_only
    citation: "arc42 — Architecture Decision Records: every architectural decision must be traceable; undeclared composition cannot be verified or evolved without breaking hidden assumptions"
    url: "https://arc42.org/overview/"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "Spring Modulith reference — @ApplicationModule annotation makes module membership explicit and machine-verifiable; undeclared module boundaries are enforced to fail loudly"
    url: "https://docs.spring.io/spring-modulith/reference/fundamentals.html"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## business-domain-must-declare-applied-recipe

**Impact: HIGH — When an L4 domain participates in a Business Pattern Recipe, its README must carry `applied_recipe: <pattern-name>`. Without this field, `recipe_governance_guard.sh` cannot confirm the domain's wiring matches the recipe contract, and the composition drifts silently.**

The ax-template composition kit tracks which recipe governs each L4 domain via the `applied_recipe:` metadata field. This field is the single source of truth linking:
- the domain's README (human-readable entry point)
- the recipe's `enabled_l4_domains:` list (machine-readable contract)
- the guard script's validation loop

When the field is absent, the guard treats the domain as ungoverned — any composition drift goes undetected until a manual audit.

**Incorrect — L4 billing README without applied_recipe: in a saas-subscription context:**

```markdown
# L4 / billing — Full Trio Domain

Billing domain vertical: subscription lifecycle, plan management, invoice listing.

## Domain Mode

`full_trio` — backend Spec Trio + frontend Spec Trio both present.

<!-- VIOLATION: no applied_recipe: field -->
<!-- recipe_governance_guard.sh: FAIL — billing is listed in saas-subscription RECIPE.md
     enabled_l4_domains but README declares no applied_recipe -->
```

**Correct — L4 billing README with applied_recipe: declared:**

```markdown
# L4 / billing — Full Trio Domain

Billing domain vertical: subscription lifecycle, plan management, invoice listing.

## Domain Mode

`full_trio` — backend Spec Trio + frontend Spec Trio both present.

## Recipe Composition

applied_recipe: saas-subscription

<!-- recipe_governance_guard.sh: PASS — billing declares applied_recipe matching
     the recipe that lists it in enabled_l4_domains -->
```

### Where to declare

The `applied_recipe:` field belongs in the L4 domain README under a `## Recipe Composition` section. Format:

```
applied_recipe: <pattern-name>
```

Where `<pattern-name>` is the directory name under `recipes/` (e.g., `saas-subscription`, `e-commerce`, `crm`).

If a domain participates in multiple recipes, use the R6+ canonical plural form (with ≥1 list entry required — an empty `applied_recipes:` block is a violation) OR the R5 legacy multi-line form; both satisfy this rule:

```
# R6+ canonical (preferred for ≥2 recipes, alphabetically sorted):
applied_recipes:
  - e-commerce
  - saas-subscription

# R5 legacy (still valid, preserved for backward-compat):
applied_recipe: saas-subscription
applied_recipe_secondary: e-commerce
```

**Note (TD-2026-05-18-019):** Both `applied_recipe:` (R5 singular legacy) and `applied_recipes:` (R6+ plural canonical) satisfy this rule. `recipe_governance_guard.sh` accepts both forms via dual-form regex alternation. `applied_recipes:` MUST have ≥1 list item; an empty list is an explicit violation.

## Failing fixture

See: `practices/evals/fixtures/business-domain-must-declare-applied-recipe/fail_no_applied_recipe/README.md` — billing domain README without `applied_recipe:` field.

See: `practices/evals/fixtures/business-domain-must-declare-applied-recipe/pass/README.md` — billing domain README with `applied_recipe: saas-subscription`.

Reference: https://docs.spring.io/spring-modulith/reference/fundamentals.html
