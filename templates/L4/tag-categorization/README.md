# L4 / tag-categorization — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

`Tag` definitions + polymorphic `TagAttachment` via `(entity_type, entity_id)` pair, exactly mirroring the comment-thread / activity-feed polymorphism. Slugging uses NFKD normalization → ASCII filter → hyphenate, with Korean fallback when the resulting slug is empty. `ROLE_ADMIN` is required for tag-definition mutations via `@PreAuthorize`. Attachments are caller-scoped at the application layer.

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/tagcategorization/`
- Spec: [`specs/tag-categorization-l0.yaml`](../../../specs/tag-categorization-l0.yaml) — 12 items / 4 families (CRUD × 4, ATTACHMENT × 3, HIERARCHY × 3, AUTHZ × 2)
- Tests: `./gradlew testTagCategorization` — GREEN (27/27 incl. iter1 violation proof — 8 deliberate-break checks)

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.tagcategorization` to your project's `<base>.tagcategorization`.
2. Copy `specs/tag-categorization-l0.yaml` for the contract surface.
3. The `TagSlugger` Korean fallback (returns a sanitized prefix when ASCII slugger empties the input) MUST be preserved — removing it breaks Korean tag input.
4. Tag definition mutations require `ROLE_ADMIN`; do NOT relax this without re-running `TagViolationProofTest`.
5. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
