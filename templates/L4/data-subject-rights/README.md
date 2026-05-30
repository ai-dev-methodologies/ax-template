# L4/data-subject-rights — GDPR DSR Self-Service Console

**Tenant model**: `single` — per [`specs/multi-tenant-l0.yaml#MULTI-TENANT-ISOLATION-DEFAULT-001`](../../../specs/multi-tenant-l0.yaml). This L4 reference workload ships as **single-tenant**. Recipes composing this domain into a multi-tenant SaaS (e.g. `b2b-admin` with `tenant_model: multi`) MUST adopt one of `MULTI-TENANT-ISOLATION-001` (Hibernate filter row-level) / `-002` (schema-per-tenant) / `-003` (AOP guard) plus `MULTI-TENANT-PROPAGATION-001` (request-scoped TenantContext) + `-002` (async propagation) before production. fork-receivers MUST NOT assume cross-tenant data isolation in this L4 as-shipped.

`templates/L4/data-subject-rights/` is the **data-subject-rights domain**
reference workload for **ax-template**. It demonstrates how to compose L1
shadcn primitives, L2 feature blocks, and L3 page templates into a GDPR
self-service privacy console backed by the
`contracts/data-subject-rights-openapi.yaml` Spring Boot API. The subject
exercises their rights — access (Art 15), rectification (Art 16), erasure
(Art 17), restriction (Art 18), portability (Art 20) — and tracks each request
against the 30-day response SLA (Art 12(3)).

**Status (per [`docs/IMPLEMENTATION-STATUS.md`](../../../docs/IMPLEMENTATION-STATUS.md))**:
**impl** — backend Java reference workload ready at `backend/src/main/java/com/ax/template/authblueprint/dsr/`; frontend layer here is a reference stub. Single-tenant by default (no multi-tenant isolation enforcement).

---

## What's included

| File | Purpose |
|---|---|
| `app/layout.tsx` | Root HTML shell + Providers (QueryClient, MSW) |
| `app/page.tsx` | Redirect → `/privacy` |
| `app/providers.tsx` | TanStack Query + MSW dev provider |
| `app/(dsr)/layout.tsx` | Authenticated layout: AppShell + Sidebar + AppHeader |
| `app/(dsr)/page.tsx` | Redirect → `/privacy` |
| `app/(dsr)/privacy/page.tsx` | **DASHBOARD** — DataTable of the subject's DSR requests + EmptyState |
| `app/(dsr)/privacy/access/page.tsx` | **ACCESS** — open a subject-access request (dsrOpenAccess) |
| `app/(dsr)/privacy/rectify/page.tsx` | **RECTIFY** — field-level correction form (dsrRectify) + parse-field-errors |
| `app/(dsr)/privacy/erasure/page.tsx` | **ERASURE** — confirm-dialog → dsrErasure → manifest |
| `app/(dsr)/privacy/portability/page.tsx` | **PORTABILITY** — json/csv export (dsrPortability) |
| `app/(dsr)/privacy/restrict/page.tsx` | **RESTRICT** — confirm-dialog → dsrRestrict + lift (dsrLiftRestriction) |
| `app/(dsr)/privacy/requests/[id]/page.tsx` | **DETAIL** — single request tracking + extend window (dsrExtendRequest) |

---

## How to fork

### 1. Copy the directory into your project

```bash
cp -r templates/L4/data-subject-rights/app <your-nextjs-project>/app
```

### 2. Install dependencies

```bash
npm install @tanstack/react-query msw
# or
pnpm add @tanstack/react-query msw
```

### 3. Configure path aliases

Add to `tsconfig.json`:

```json
{
  "compilerOptions": {
    "paths": {
      "templates/*": ["../../ax-template/templates/*"]
    }
  }
}
```

Or copy the L1/L2/L3 blocks you use into `src/components/` and update the
import paths inside the copied files.

### 4. Wire authentication

The backend API requires a Bearer JWT. The data subject is ALWAYS the
authenticated principal — no route carries a subject id. Before copying,
decide how to inject the auth token:

- **Option A**: Add an Axios/fetch interceptor that reads the token from
  `localStorage` / a cookie and attaches `Authorization: Bearer <token>`.
- **Option B**: Use a Next.js route handler as a BFF proxy that adds the
  token server-side.

### 5. Configure MSW handlers (development)

Create `src/mocks/browser.ts` and `src/mocks/handlers.ts` with mock
responses for `/api/me/dsr/*`. See the
[MSW docs](https://mswjs.io/docs/getting-started) for setup.

### 6. Adapt the per-module access bundle

The `AccessBundle.modules` map is keyed by owning-module name; render it
generically (the dashboard does not hardcode field names). Wire the
`rectifiable_fields` allowlist from your recipe into the rectify form so
non-editable fields are not offered.

---

## Spec Trio binding

| Artifact | File |
|---|---|
| Page Compliance Spec | `specs/data-subject-rights-frontend-l0.yaml` |
| UI Contract | `contracts/data-subject-rights-ui.yaml` |
| UI Policy Manifest | `blueprints/data-subject-rights-ui-manifest.yaml` |
| Backend OpenAPI | `contracts/data-subject-rights-openapi.yaml` |
| Backend Compliance Spec | `specs/data-subject-rights-l0.yaml` |

Run `bash practices/evals/trio_integrity_guard.sh --domain data-subject-rights`
to verify the full Spec Trio is intact.

---

## Verification

```bash
# Verify L4 data-subject-rights composition contract (static analysis)
cd frontend && npx playwright test tests/L4/data-subject-rights/

# Verify full domain spec trio
bash skills/ax-verify-domain/scripts/run.sh data-subject-rights

# Verify L4 layer
bash skills/ax-verify-L4/scripts/run.sh
```

## Recipe Composition

applied_recipe: e-commerce
applied_recipes:
  - community
  - crm
  - e-commerce
  - lms
  - marketplace
