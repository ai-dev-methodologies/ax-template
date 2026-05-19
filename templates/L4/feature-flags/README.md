# Feature Flags — L4 Domain Template

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `-002` (schema-per-tenant) / `-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

**domain**: `feature-flags`  
**spec_ref**: `specs/feature-flags-l0.yaml` + `specs/feature-flags-frontend-l0.yaml`  
**blueprint_ref**: `blueprints/feature-flags-manifest.yaml` + `blueprints/feature-flags-ui-manifest.yaml`

Runtime-controlled feature toggles for React (Next.js) + Spring Boot.
No redeployment required — flip a flag in the admin UI to gate features.

---

## Architecture

```
Backend (Spring Boot)                 Frontend (Next.js)
────────────────────                  ─────────────────
FeatureFlag (entity)                  middleware.ts — server-side eval
FeatureFlagRepository                 FeatureGate (L2) — client-side gate
FeatureFlagCache (Caffeine 30s TTL)   FeatureFlagToggle (L2) — admin toggle
FeatureFlagService                    /admin/feature-flags — list page
FeatureFlagController (public)        /admin/feature-flags/[name] — detail page
FeatureFlagAdminController (ADMIN)
```

## Fail-Closed Policy

- Unknown flags → `{ active: false }` (never accidentally rolls out)
- Network error during eval → `false`
- Loading state in FeatureGate → renders fallback (not children)

## Quick Start

### 1. Backend

Copy `templates/backend/feature-flags/` into your Spring Boot module:

```
src/main/java/com/example/app/featureflags/
├── FeatureFlag.java
├── FeatureFlagRepository.java
├── FeatureFlagCache.java
├── FeatureFlagService.java
├── FeatureFlagDto.java
├── FeatureFlagController.java       ← public eval endpoint
└── FeatureFlagAdminController.java  ← ROLE_ADMIN CRUD
```

Add to `application.yaml`:
```yaml
spring:
  cache:
    caffeine:
      spec: "featureFlags=maximumSize=500,expireAfterWrite=30s"
```

Add to `SecurityConfig`:
```java
.requestMatchers("/api/v1/feature-flags/*/active").permitAll()
.requestMatchers("/api/v1/admin/feature-flags/**").hasRole("ADMIN")
```

### 2. Frontend

Add env var:
```
BACKEND_API_BASE=http://localhost:8080
```

Copy `next.config.ts` (rewrites `/api/v1/**` to backend).

Copy `middleware.ts` and add your flagged routes to `FLAGGED_ROUTES`.

Use `FeatureGate` to gate client-rendered features:
```tsx
<FeatureGate name="new-checkout" fallback={<LegacyCheckout />}>
  <NewCheckout />
</FeatureGate>
```

Use `FeatureFlagToggle` in admin UI:
```tsx
<FeatureFlagToggle
  name="new-checkout"
  initialEnabled={flag.enabled}
/>
```

## Spec Compliance

| Spec ID | Requirement | Status |
|---------|-------------|--------|
| FF-AUTHZ-001 | Eval endpoint public; admin requires ROLE_ADMIN | template |
| FF-AUTHZ-002 | Non-admin gets 403 | template |
| FF-EVAL-001 | Active flag returns `{active: true}` | template |
| FF-EVAL-002 | Unknown flag → `{active: false}` (fail-closed) | template |
| FF-EVAL-003 | Caffeine 30s TTL cache, evicted on write | template |
| FF-CRUD-001 | POST creates flag; duplicate → 409 | template |
| FF-CRUD-002 | GET list paginated | template |
| FF-CRUD-003 | PATCH updates enabled/description | template |
| FF-CRUD-004 | DELETE hard-deletes; subsequent eval → false | template |
| FF-VALID-001 | Name ^[a-z][a-z0-9-]{1,62}$ | template |
| FF-VALID-002 | Description max 500 chars | template |
| FF-FE-001 | Admin list table with toggle | template |
| FF-FE-002 | FeatureFlagToggle optimistic update + rollback | template |
| FF-FE-003 | Admin detail with description editor | template |
| FF-FE-004 | FeatureGate client-side with loading/error handling | template |
| FF-FE-005 | middleware server-side redirect when inactive | template |

## Recipe Composition

applied_recipe: saas-subscription
applied_recipes:
  - api-gateway-relay
  - b2b-admin
  - booking
  - lms
  - marketplace
  - saas-subscription
