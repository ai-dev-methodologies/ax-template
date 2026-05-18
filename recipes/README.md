# Business Pattern Recipes

> **What this is:** Composition manifests that declare WHICH existing L4 domains to assemble for a given business pattern. Recipes do NOT ship code or define new L4 domains.

## Shipped Recipes (v1.6.0-lms-cms — 9 active)

| Pattern | Description | Enabled L4 domains | Verdict |
|---|---|---|---|
| [`saas-subscription`](saas-subscription/RECIPE.md) | Multi-tenant SaaS with plan tiers, recurring billing, feature gating | billing, auth, feature-flags, notification, audit-log | [12/12 MUST, 8/8 SHOULD](../skills/_tests/sealed-verdict/saas-subscription-verdict.md) |
| [`e-commerce`](e-commerce/RECIPE.md) | Product catalog + cart + checkout + order management | crud, payment, notification, audit-log, search | [12/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/e-commerce-verdict.md) |
| [`crm`](crm/RECIPE.md) | Sales pipeline: lead → contact → deal → activity | crud, audit-log, notification, search | [PASS](../skills/_tests/sealed-verdict/crm-verdict.md) |
| [`booking`](booking/RECIPE.md) | Calendar + availability + reservation + cancellation | audit-log, crud, feature-flags, notification, payment | [11/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/booking-verdict.md) |
| [`marketplace`](marketplace/RECIPE.md) | Listings + bids + escrow + ratings | audit-log, crud, notification, payment, search | [12/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/marketplace-verdict.md) |
| [`b2b-admin`](b2b-admin/RECIPE.md) | Multi-tenant ops + analytics + audit + impersonation | audit-log, auth, crud, feature-flags, search | [11/12 MUST, 6/8 SHOULD](../skills/_tests/sealed-verdict/b2b-admin-verdict.md) |
| [`community`](community/RECIPE.md) | Posts + comments + moderation + reply notifications + soft-delete-aware search | audit-log, auth, crud, notification, search | [11/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/community-verdict.md) |
| [`lms`](lms/RECIPE.md) | Course catalog + enrollment + lesson + due-date reminders + role-gated visibility | audit-log, auth, crud, notification, scheduled-task | [11/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/lms-verdict.md) |
| [`cms`](cms/RECIPE.md) | Content authoring + scheduled publish + editorial workflow + scheduled expiry | audit-log, crud, notification, scheduled-task | [11/12 MUST, 7/8 SHOULD](../skills/_tests/sealed-verdict/cms-verdict.md) |

All 9 recipes carry sealed sub-agent verdicts (≥10/12 MUST + ≥5/8 SHOULD). See `skills/_tests/sealed-verdict/`.

## Deferred Recipes (1 remaining — pending fork-receiver demand)

| Pattern | Why deferred | Re-introduction trigger |
|---|---|---|
| `internal-it` | Independent of R8 scheduler-consuming recipes (lms + cms landed R8 v1.6.0). Jira API not verbatim-fetched (3 fetch failures); webhook patterns vendor-specific. | Independent of R8 scheduler-consuming recipes. Remaining gap = verbatim Jira/ServiceNow REST API quote + clarified webhook-emit primitive in notification L4. R9+ if fetch succeeds OR notification L4 gains explicit webhook-emit spec items. |

`internal-it` carries `status: deferred-pending-fork-receiver-demand` in `_MANIFEST.yaml`. R8 consumed scheduler for lms + cms — internal-it remains gated on webhook-emit primitive (notification L4 extension).

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
