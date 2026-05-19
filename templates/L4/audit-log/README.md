# L4 / audit-log — Fork & Copy Guide

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `-002` (schema-per-tenant) / `-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

This directory is a **full-stack vertical template** for an audit log domain.
It is the 5th L4 domain in `ax-template` (after auth / crud / payment / practices).

## What's included

| File | Purpose |
|------|---------|
| `app/layout.tsx` | Root Next.js layout with `Providers` |
| `app/page.tsx` | Root redirect to `/audit-log` |
| `app/providers.tsx` | `QueryClientProvider` (TanStack Query v5) |
| `app/(audit-log)/layout.tsx` | Route-group layout: AppShell + Sidebar |
| `app/(audit-log)/page.tsx` | **List page** — VirtualizedTable + FilterBar + Pagination |
| `app/(audit-log)/[id]/page.tsx` | **Detail page** — single entry with metadata JSON |
| `app/(audit-log)/export/page.tsx` | **Export page** — CSV/JSON async export with job polling |
| `next.config.ts` | Minimal Next.js config with API proxy + security headers |

## How to fork and copy into your project

1. **Copy the directory tree** into your Next.js project under `app/`:
   ```bash
   cp -r templates/L4/audit-log/app/(audit-log) <your-project>/src/app/(audit-log)
   cp    templates/L4/audit-log/app/providers.tsx <your-project>/src/app/providers.tsx
   ```

2. **Install dependencies** if not already present:
   ```bash
   npm install @tanstack/react-query @tanstack/react-virtual
   ```

3. **Wire the backend URL** in `next.config.ts` → update the `rewrites()` host/port
   to point at your Spring Boot instance.

4. **Replace the session role check** on `export/page.tsx`:
   ```tsx
   // Example with next-auth
   import { useSession } from 'next-auth/react'
   const { data: session } = useSession()
   const hasExportRole = session?.user?.roles?.includes('ADMIN')
     || session?.user?.roles?.includes('AUDITOR')
   ```

5. **Connect your auth guard** in `(audit-log)/layout.tsx` — redirect to `/login`
   if the user is not authenticated.

6. **Customize columns** — edit the `COLUMNS` array in `(audit-log)/page.tsx` to
   match your backend's field names and display preferences.

## Spec Trio bindings

| File | Spec | Contract |
|------|------|---------|
| `(audit-log)/page.tsx` | `specs/audit-log-frontend-l0.yaml` AUDIT-FE-001..003,007,008 | `listAuditLogs` |
| `(audit-log)/[id]/page.tsx` | AUDIT-FE-004 | `getAuditLog` |
| `(audit-log)/export/page.tsx` | AUDIT-FE-005, AUDIT-FE-006 | `exportAuditLogs` |

Backend: `specs/audit-log-l0.yaml` + `contracts/audit-log-openapi.yaml` + `blueprints/audit-log-manifest.yaml`.

## Performance notes

- The list page uses `VirtualizedTable` from SP15 (`@tanstack/react-virtual`) to handle
  datasets of 10,000+ rows without DOM explosion.
- Server-side pagination (default page size: 50) is enforced — no client-side re-sort.
- The filter bar syncs all state to URL query params so filters survive refresh/sharing.

## RBAC

- **List / Detail**: any authenticated user.
- **Export**: requires `ROLE_ADMIN` or `ROLE_AUDITOR` (server-enforced `@PreAuthorize`,
  plus client-side access-denied notice).

## L4 import boundary

This vertical must **not** import from other L4 domains:
```
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
```
L1 (shadcn primitives) + L2 (feature blocks) + L3 (page skeletons) are all permitted.

## Recipe Composition

applied_recipe: saas-subscription
applied_recipe_secondary: e-commerce
applied_recipe_tertiary: crm
applied_recipes:
  - api-gateway-relay
  - b2b-admin
  - booking
  - cms
  - community
  - crm
  - e-commerce
  - internal-it
  - lms
  - marketplace
  - saas-subscription
