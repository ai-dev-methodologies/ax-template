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

<!-- FAILING FIXTURE: recipe metadata declaration is absent -->
<!-- rule: business-domain-must-declare-applied-recipe -->
<!-- guard expects: exit 1 — no recipe metadata on this domain -->
