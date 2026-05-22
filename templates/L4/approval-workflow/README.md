# L4 / approval-workflow — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `MULTI-TENANT-ISOLATION-002` (schema-per-tenant) / `MULTI-TENANT-ISOLATION-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `MULTI-TENANT-PROPAGATION-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This is a **backend-only L4 stub** (R39, 2026-05-23). The Next.js frontend tree is intentionally deferred — see "Why backend-only at this stage" below.

## Domain summary

Sequential ordered multi-step approval (Korean enterprise 결재선 pattern). `ApprovalRequest` owns an ordered `List<ApprovalStep>` (JPA `OneToMany cascade`) with two state machines: request status (`DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED`) and step status (`PENDING / APPROVED / REJECTED`). Both state transitions go through sole-mutator services; payload immutability is enforced via `JPA updatable=false` (R31 iter1 dogfood closure).

## Backend reference

- Java package: `backend/src/main/java/com/ax/template/authblueprint/approvalworkflow/`
- Spec: [`specs/approval-workflow-l0.yaml`](../../../specs/approval-workflow-l0.yaml) — 15 items / 5 families (LIFECYCLE × 4, AUTHZ × 3, STEP × 5, QUERY × 2, PAYLOAD × 1)
- Tests: `./gradlew testApprovalWorkflow` — GREEN (26/26 incl. iter1+2 dogfood violation proofs)
- Anchored generic rules (R38):
  - [`practices/rules/caller-authentication-only-no-userid-param.md`](../../../practices/rules/caller-authentication-only-no-userid-param.md) — duplicate approver + self-approve guards
  - [`practices/rules/admin-cannot-rewrite-user-content.md`](../../../practices/rules/admin-cannot-rewrite-user-content.md) — payload immutability across review

## Why backend-only at this stage

The R29–R36 catalog wave landed the backend impl first (each `./gradlew test<Domain>` GREEN) to validate the spec → TDD → GREEN loop without the additional surface area of a Next.js stub. R39 adds this README so `b2b-admin`'s `enabled_l4_domains` can reference the domain via `recipe_spec_referential_integrity_guard.sh` without manufacturing scaffolding that would later need to be discarded.

## How to fork into your project

1. Copy the Java package `com.ax.template.authblueprint.approvalworkflow` to your project's `<base>.approvalworkflow`.
2. Copy `specs/approval-workflow-l0.yaml` for the contract surface.
3. Adopt a `tenant_model: multi` isolation mode before production if your composition declares one.
4. The duplicate-approver guard and self-approve guard are required structural invariants — do NOT relax them; the iter1+2 dogfood adds them as VIOLATION proof.
