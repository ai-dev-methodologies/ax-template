# Business Pattern Recipes

> **What this is:** Composition manifests that declare WHICH existing L4 domains to assemble for a given business pattern. Recipes do NOT ship code or define new L4 domains.

## Shipped Recipes (v1.5.0-scheduler-community — 7 active)

| Pattern | Description | Enabled L4 domains | Verdict |
|---|---|---|---|
| [`saas-subscription`](saas-subscription/RECIPE.md) | Multi-tenant SaaS with plan tiers, recurring billing, feature gating | billing, auth, feature-flags, notification, audit-log | [12/12 MUST, 8/8 SHOULD](../skills/_tests/sealed-verdict/saas-subscription-verdict.md) |
| [`e-commerce`](e-commerce/RECIPE.md) | Product catalog + cart + checkout + order management | crud, payment, notification, audit-log, search | [12/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/e-commerce-verdict.md) |
| [`crm`](crm/RECIPE.md) | Sales pipeline: lead → contact → deal → activity | crud, audit-log, notification, search | [PASS](../skills/_tests/sealed-verdict/crm-verdict.md) |
| [`booking`](booking/RECIPE.md) | Calendar + availability + reservation + cancellation | audit-log, crud, feature-flags, notification, payment | [11/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/booking-verdict.md) |
| [`marketplace`](marketplace/RECIPE.md) | Listings + bids + escrow + ratings | audit-log, crud, notification, payment, search | [12/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/marketplace-verdict.md) |
| [`b2b-admin`](b2b-admin/RECIPE.md) | Multi-tenant ops + analytics + audit + impersonation | audit-log, auth, crud, feature-flags, search | [11/12 MUST, 6/8 SHOULD](../skills/_tests/sealed-verdict/b2b-admin-verdict.md) |
| [`community`](community/RECIPE.md) | Posts + comments + moderation + reply notifications + soft-delete-aware search | audit-log, auth, crud, notification, search | [11/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/community-verdict.md) |

All 7 recipes carry sealed sub-agent verdicts (≥10/12 MUST + ≥5/8 SHOULD). See `skills/_tests/sealed-verdict/`.

## Deferred Recipes (3 remaining — pending fork-receiver demand)

| Pattern | Why deferred | Re-introduction trigger |
|---|---|---|
| `lms` | Scheduler L4 **landed in R7 v1.5.0** (`templates/L4/scheduled-task/`); LMS due-date-reminder primitive now disk-resolvable. Remaining gap: Coursera/Moodle/edX case-study verbatim. 인프런 closed API. | **Scheduler L4 landed in R7 v1.5.0**; remaining gap = Coursera/Moodle/edX case-study URL with verbatim integration text. R8 eligible. |
| `cms` | Scheduler L4 **landed in R7 v1.5.0**; scheduled-publish primitive now disk-resolvable. Remaining gap: Sanity/Contentful verbatim citation. 네이버 블로그 closed API. | **Scheduler L4 landed in R7 v1.5.0**; remaining gap = Sanity/Contentful verbatim citation. R8 eligible. |
| `internal-it` | Independent of R7 scheduler. Jira API not verbatim-fetched (3 fetch failures); webhook patterns vendor-specific. | Independent of R7 scheduler. Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R8+ if fetch succeeds. |

All deferred patterns carry `status: deferred-pending-fork-receiver-demand` in `_MANIFEST.yaml`. R7 scheduler L4 unblocks 2 of these 3 — lms + cms (internal-it is independent).

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
