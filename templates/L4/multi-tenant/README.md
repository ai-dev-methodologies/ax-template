# L4 / multi-tenant — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 is **the tenancy-enforcement runtime itself** — every OTHER L4's tenant_model declaration cites the same spec. Single-tenant baseline is what the other 21 L4 verticals adopt when they ship as-is; this directory is the home of the runtime primitives a fork-receiver wires when their recipe declares `tenant_model: multi`.

**Status**: backend-only stub (promoted future_add → selectable, recipe_orphan). This README is the R39 backend-only stub convention — the spec + blueprint were already in the catalog; this commit reserves the on-disk slot so the classification guard can flip the entry to `selectable` and so fork-receivers see an explicit handle. Backend runtime primitives (TenantContext + TenantContextHolder + TenantContextFilter + Hibernate @Filter wiring) ship in a follow-up commit; this stub is the namespace reservation.

## Domain summary

Cross-cutting tenancy enforcement. Three isolation strategies (`MULTI-TENANT-ISOLATION-001` Hibernate row-level filter / `MULTI-TENANT-ISOLATION-002` schema-per-tenant / `MULTI-TENANT-ISOLATION-003` AOP guard) + two propagation primitives (`MULTI-TENANT-PROPAGATION-001` request-scoped TenantContext / `MULTI-TENANT-PROPAGATION-002` async-boundary propagation). The spec defines the policy contract; the runtime here will define the canonical Spring wiring.

## Backend reference

- Java package (future): `backend/src/main/java/com/ax/template/authblueprint/common/tenancy/` — cross-cutting, NOT per-domain
- Spec: [`specs/multi-tenant-l0.yaml`](../../../specs/multi-tenant-l0.yaml) — ISOLATION × 4 + PROPAGATION × 2 items
- Blueprint: [`blueprints/multi-tenant-manifest.yaml`](../../../blueprints/multi-tenant-manifest.yaml) — policy anchors per strategy
- Tests (future): `./gradlew testMultiTenant` — to be added when runtime primitives land
- Anchored generic rule (existing catalog reference):
  - [`practices/rules/dogfood-finding-must-cite-multi-tenant-deferral.md`](../../../practices/rules/dogfood-finding-must-cite-multi-tenant-deferral.md) — every dogfood finding that defers tenancy MUST cite this spec

## Frontend

Multi-tenant has **no first-class UI**. Tenancy is invisible to end-users (they see only their own tenant's data). Admin surfaces (tenant lifecycle, quota, billing scope) live in their respective domain L4s (`billing`, `audit-log`) operating within the tenant context this runtime establishes. Frontend deliberately skipped — registered as `backend_only` in `practices/evals/trio_integrity_allowlist.yaml`.

## Composition contract

When a fork-receiver flips a recipe's `tenant_model: single` → `multi`:

1. Pick one of `MULTI-TENANT-ISOLATION-001/002/003` and adopt the canonical wiring (see blueprint).
2. Add `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) — non-negotiable.
3. Add `MULTI-TENANT-PROPAGATION-002` (async-boundary propagation) if the recipe enables `@Async` / `CompletableFuture` / message-queue paths.
4. Re-run `bash practices/scripts/verify-completion.sh` — the per-domain integration tests must still GREEN under the tenancy overlay.

## Next steps

- Land the `common.tenancy` runtime primitives (`TenantContext`, `TenantContextHolder`, `TenantContextFilter`, Hibernate `@Filter` wiring) in a follow-up commit; promote `recipe_orphan: true` → wired into b2b-admin recipe.
- Backfill the `Tenant model: single` declaration line on any `templates/L4/*/README.md` that does not yet cite this spec.
- Add a dogfood ledger entry (`docs/dogfood-ledger/multi-tenant-iter1.yaml`) once the runtime primitives ship — the catalog's 2-persona protocol applies to cross-cutting primitives the same as it applies to domain verticals.
