# L4 / billing — Full Trio Domain

Billing domain vertical: subscription lifecycle, plan management, invoice listing,
billing event history.

## Domain Mode

`full_trio` — backend Spec Trio + frontend Spec Trio both present.

## Spec Trio

| File | Purpose |
|---|---|
| `specs/billing-l0.yaml` | Backend compliance spec |
| `contracts/billing-openapi.yaml` | OpenAPI 3.0 contract |
| `blueprints/billing-manifest.yaml` | Backend policy manifest |

## Recipe Composition

applied_recipe: saas-subscription

<!-- CORRECT: applied_recipe: saas-subscription declared -->
<!-- business-domain-must-declare-applied-recipe: PASS -->
<!-- recipe_governance_guard.sh reads this field and validates against
     recipes/saas-subscription/RECIPE.md enabled_l4_domains -->
