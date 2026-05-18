# L4 Composition — b2b-admin

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
auth
 └── multi-tenant JWT; role claim: ADMIN | MANAGER | MEMBER
      ↓ impersonation flow
auth (impersonation)
 └── POST /api/auth/impersonate/{userId} (ADMIN only)
      → issues impersonation token with { impersonatorId, impersonatedId }
      ↓
audit-log
 └── ImpersonationStartedEvent: { impersonator_id, impersonated_id, started_at }
      ↓
crud (tenant + user management)
 └── Tenant CRUD, User CRUD, org settings (ADMIN/MANAGER scope)
      ↓
feature-flags
 └── tenant-scoped flag CRUD: POST/PATCH /api/feature-flags/{tenantId}/{flagKey}
      ↓ every flag mutation
audit-log
 └── FeatureFlagChangedEvent: { tenant_id, flag_key, old_value, new_value, changed_by }
      ↓ (immutable — no DELETE allowed per FF-CRUD-003)
search
 └── cross-tenant search: GET /api/admin/search (ADMIN role only)
     per-tenant entity search: GET /api/tenants/{id}/search
      ↓ all queries
auth (authz filter)
 └── tenant boundary enforced at query time (ASVS-V4.2.1 + V4.2.2)
      ↓
audit-log (session end)
 └── ImpersonationEndedEvent: { impersonator_id, impersonated_id, ended_at }
```

## Domain Configuration Notes

### `auth`
- JWT claim: `tenantId` + `role` (ADMIN | MANAGER | MEMBER) + optional `impersonating: <userId>`
- ADMIN can access `/api/admin/**` cross-tenant
- Impersonation: ADMIN-only; issues short-lived (30m) impersonation token
- `@PreAuthorize("hasAuthority('ROLE_ADMIN') and !#isImpersonating")`
- Reference: `templates/L4/auth/`

### `crud`
- Entities: `Tenant`, `User`, `OrgSettings`
- Tenant: multi-tenant root entity; all other queries scoped by `tenantId`
- User: CRUD by ADMIN/MANAGER; scope: own tenant only (non-ADMIN)
- OrgSettings: immutable history via event-sourced pattern recommended
- Reference: `templates/L4/crud/`

### `audit-log`
- `@Audited(action = "impersonation.started")` on ImpersonationService.start()
- `@Audited(action = "impersonation.ended")` on ImpersonationService.end()
- `@Audited(action = "feature_flag.changed")` on FeatureFlagService.update()
- Retention: ≥180 days for impersonation events (compliance)
- Reference: `templates/L4/audit-log/`

### `feature-flags`
- Scope: per-tenant flags (`tenantId` mandatory on all flag mutations)
- Immutable history: PATCH allowed (create new version); DELETE is blocked (FF-CRUD-003)
- Tenant admin (MANAGER role) can toggle own-tenant flags; ADMIN can toggle any tenant
- Reference: `templates/L4/feature-flags/`

### `search`
- ADMIN: cross-tenant search index (all tenants)
- MANAGER/MEMBER: per-tenant search (own tenantId filter applied automatically)
- Authz filter injected at query time — no post-processing bypass possible
- Reference: `templates/L4/search/`

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - b2b-admin
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6 dual-form guard)
