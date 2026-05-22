# L4 / activity-feed — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

Per-user activity inbox using the ActivityStreams 2.0 vocabulary (`actor / verb / object / audience`). Polymorphic addressing via `(object_type, object_id)`. Publication is idempotent on `UNIQUE(actor, idempotencyKey)`. Read state is a separate `ActivityRead` row per `(event, user)` (fan-out-on-read). Visibility rule: an event is visible to caller IFF `actor == caller OR audience.contains(caller)` — enforced at the service layer.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/activityfeed/`
- Spec: [`specs/activity-feed-l0.yaml`](../../../specs/activity-feed-l0.yaml) — 12 items / 4 families (PUBLISH × 3, READ × 3, MARK × 3, AUTHZ × 3)
- Tests: `./gradlew testActivityFeed` — GREEN (18/18 incl. iter1 violation proof)
- Anchored generic rules (R38):
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — `Authentication.getName()` only; no `?userId=` parameter

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.activityfeed` to your project's `<base>.activityfeed`.
2. Copy `specs/activity-feed-l0.yaml` for the contract surface.
3. The visibility rule (actor OR audience contains caller) is a structural invariant — do NOT widen it to "anyone can read" without re-validating the IDOR test surface.
4. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
