# templates/L4/ — Reference Workload Roots

This directory contains the L4 reference workloads that ax-template ships
as **composition kit** building blocks. Each subdirectory is a complete
domain (spec trio + Spring backend slice + Next.js frontend slice) that
forks may compose into their product via a Business Pattern Recipe.

**Canonical classification**:
[`specs/l4-domain-classification.yaml`](../../specs/l4-domain-classification.yaml)
**Enforced by** the 35th hard guard
[`practices/evals/l4_domain_enum_sync_guard.sh`](../../practices/evals/l4_domain_enum_sync_guard.sh)
(see `bash practices/evals/run-all-guards.sh`).

---

## Three sources of truth

ax-template tracks L4 domains across three independent sources:

| Source | Path | Question it answers |
|---|---|---|
| **DISK** | `templates/L4/<domain>/` | Is there a reference workload to copy? |
| **SCHEMA** | `specs/recipes/_override-schema.yaml#$defs/l4_domain` | May a recipe override target this domain? |
| **RECIPES** | `specs/recipes/*-recipe-l0.yaml#enabled_l4_domains` | Which recipes currently compose this domain? |

These three sources **intentionally disagree**. The disagreement is
classified into three tiers below. The guard mechanically validates that
the disagreement matches the declared classification.

---

## Tier 1 — INFRA (1 entry)

| Domain | On disk | In schema | In recipes | Why |
|---|---|---|---|---|
| `practices` | YES | NO | NO | Cross-cutting catalog; not recipe-selectable |

INFRA entries ship on disk for fork visibility but are excluded from the
override schema enum because no recipe can add or skip them.

---

## Tier 2 — SELECTABLE (11 entries)

Business domains present on disk **and** authorised by the schema enum.
Forks compose them via `enabled_l4_domains:` or override them via
`override_allowed.enabled_l4_domains.add/skip`.

| Domain | Active recipes (dogfood-12 cut) |
|---|---|
| `audit-log` | all 11 recipes |
| `auth` | 6 recipes (api-gateway-relay, b2b-admin, community, internal-it, lms, saas-subscription) |
| `billing` | 1 recipe (saas-subscription) |
| `crud` | 10 recipes (all except saas-subscription) |
| `feature-flags` | 3 recipes (b2b-admin, booking, saas-subscription) |
| `file-storage` | **recipe-orphan** — provisioned for forks, no active recipe yet |
| `notification` | 9 recipes |
| `payment` | 3 recipes (booking, e-commerce, marketplace) |
| `scheduled-task` | 4 recipes (api-gateway-relay, cms, internal-it, lms) |
| `search` | 5 recipes (b2b-admin, community, crm, e-commerce, marketplace) |
| `webhook` | 2 recipes (api-gateway-relay, internal-it) |

Authoritative per-domain recipe list:
[`specs/l4-domain-classification.yaml#tiers.selectable.items[].active_recipes`](../../specs/l4-domain-classification.yaml).

---

## Tier 3 — FUTURE_ADD (6 entries)

Schema-reserved L4 namespaces **intentionally absent from disk** at the
current cut. The schema authorises the name so the first fork to add the
domain on disk does not need to also patch the schema enum or any guard.

| Domain | Intended use |
|---|---|
| `email-outbox` | Transactional email outbox pattern with retry + dead-letter |
| `i18n-policy` | Locale + currency + RTL policy enforcement |
| `identity-verification` | KYC / document verification pipeline |
| `multi-tenant` | Row-level / schema-per-tenant isolation runtime |
| `ratelimit` | API rate-limiting policy + counter store |
| `realtime-policy` | WebSocket / SSE channel auth + back-pressure |

**These dirs are not missing — they are intentionally absent.** The
`l4_domain_enum_sync_guard.sh` invariant I6 enforces this absence: a fork
that materialises one of these directories must simultaneously move the
entry from `future_add` to `selectable` in the classification file (and
typically ship a recipe that consumes it).

---

## Procedure for fork-receivers

### Adding a FUTURE_ADD domain to disk

1. Materialise `templates/L4/<domain>/` with the standard Spec Trio + Spring
   slice + Next.js slice (see `METHODOLOGY.md` 5-step playbook).
2. Move the entry from `tiers.future_add.items` to `tiers.selectable.items`
   in `specs/l4-domain-classification.yaml`.
3. (Optional) Add `applied_recipe:` declaration in the new README and add
   the domain to a recipe's `enabled_l4_domains:` list.
4. Run `bash practices/evals/run-all-guards.sh` — all 35 guards must PASS.

### Adding a new SELECTABLE domain not yet in schema

1. Append the name to the `$defs.l4_domain.enum` list in
   `specs/recipes/_override-schema.yaml`.
2. Materialise the disk dir (step 1 above).
3. Add the name to `tiers.selectable.items` in the classification file.
4. `bash practices/evals/run-all-guards.sh` — all 35 guards must PASS.

### Removing a SELECTABLE domain

Removal is rarely correct (forks downstream may depend on the disk dir).
If genuinely needed, the procedure is the reverse of "adding": remove
from disk, classification, schema, **and** every recipe's
`enabled_l4_domains:` list in the same commit. The guard catches partial
removals as I1 / I2 / I5 violations.

---

## Why this file exists

Before R12 closure the three sources drifted silently. R10 mis-framed the
gap as a 17-vs-12 enum count mismatch; the real gap was that the
disagreement had no canonical classification document and no mechanical
sync check. This README plus the classification file plus the 35th guard
close that gap as a coherent triple.
