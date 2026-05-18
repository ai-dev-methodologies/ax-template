# Business Pattern Recipes

> **What this is:** Composition manifests that declare WHICH existing L4 domains to assemble for a given business pattern. Recipes do NOT ship code or define new L4 domains.

## Shipped Recipes (v1.3.0-business-patterns)

| Pattern | Description | Enabled L4 domains |
|---|---|---|
| [`saas-subscription`](saas-subscription/RECIPE.md) | Multi-tenant SaaS with plan tiers, recurring billing, feature gating | billing, auth, feature-flags, notification, audit-log |
| [`e-commerce`](e-commerce/RECIPE.md) | Product catalog + cart + checkout + order management | crud, payment, notification, audit-log, search |
| [`crm`](crm/RECIPE.md) | Sales pipeline: lead → contact → deal → activity | crud, audit-log, notification, search |

All 3 recipes carry sealed sub-agent verdicts (≥10/12 MUST + ≥5/8 SHOULD). See `skills/_tests/sealed-verdict/`.

## Deferred Recipes (pending fork-receiver demand)

| Pattern | Why deferred | Re-introduction trigger |
|---|---|---|
| `booking` | Korean URL evidence for 야놀자/캐치테이블 thin; needs Connectivity API access for citation | Fork-receiver demand OR public Booking.com Connectivity case study URL |
| `community` | Discourse + Reddit citations OK; Korean refs (디시인사이드, 클리앙) lack structured API URLs | Fork-receiver demand with Korean community platform requirement |
| `marketplace` | Etsy URL OK; 당근마켓 has no public API docs URL → would need `internal_design` fallback | Fork-receiver demand |
| `lms` | Moodle URL OK; 인프런 has no public API docs URL → would need `internal_design` fallback | Fork-receiver demand |
| `b2b-admin` | 토스 비즈니스 lacks structured URL for external citation | Fork-receiver demand |
| `cms` | Sanity + Contentful URLs OK; rich-text already shipped in SP32 | Fork-receiver demand or batch with `community` |
| `internal-it` | Jira + ServiceNow URLs OK; Korean refs to 잔디/카카오워크 lack structured URLs | Fork-receiver demand |

All deferred patterns carry `status: deferred-pending-fork-receiver-demand` in `_MANIFEST.yaml`.

## Using a Recipe

```bash
# Scaffold a new project from a recipe (SP36)
/ax-scaffold business saas-subscription my-saas-app
```

The subcommand (`skills/ax-scaffold/scripts/new-business-recipe.sh`) will:
1. Copy each enabled L4 skeleton into the new project
2. Run `/ax-verify-domain <each-L4>` in a loop
3. Emit the file tree for review

## Referential Integrity Guard

```bash
bash practices/evals/recipe_spec_referential_integrity_guard.sh
```

Validates:
- Every `enabled_l4_domains:` entry resolves to `templates/L4/<domain>/`
- Every `l2_blocks_used:` entry resolves to `templates/L2/blocks/<name>.tsx`
- Every `business_invariants[*].spec_ref:` resolves to an existing spec file + ID
- Every `business_invariants[*].rule_ref:` resolves to an existing rule file

## Adding a Future Recipe

When a new recipe is approved in a future PRD:
1. Follow the 5-step blueprint in `METHODOLOGY.md` §5 (new domain → spec → L4-composition → L2-block-recipe → evidence)
2. Create `recipes/<pattern>/` with all 4 artifacts
3. Add `specs/recipes/<pattern>-recipe-l0.yaml`
4. Register in `recipes/_MANIFEST.yaml` (use `yq`-sorted insertion to stay deterministic):
   ```bash
   yq e '.recipes += [{"pattern": "<name>", "status": "active", ...}]' -i recipes/_MANIFEST.yaml
   ```
5. Run the referential-integrity guard to verify
6. Add sealed verdict in `skills/_tests/sealed-verdict/<pattern>-verdict.md`

## Schema Version

Recipe-level specs use `schema_version: 1`. If the recipe spec schema changes in a future PRD, bump to `schema_version: 2` and document the migration in `METHODOLOGY.md` Appendix C.
