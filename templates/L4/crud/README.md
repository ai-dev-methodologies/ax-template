# L4/crud — CRUD Reference Workload

`templates/L4/crud/` is the **crud domain** reference workload for
**ax-template**. It demonstrates how to compose L1 shadcn primitives, L2
feature blocks, and L3 page templates into a production-ready CRUD vertical
backed by the `contracts/crud-openapi.yaml` Spring Boot API.

---

## What's included

| File | Purpose |
|---|---|
| `app/layout.tsx` | Root HTML shell + Providers (QueryClient, MSW) |
| `app/page.tsx` | Redirect → `/items` |
| `app/providers.tsx` | TanStack Query + MSW dev provider |
| `app/(crud)/layout.tsx` | Authenticated layout: AppShell + Sidebar + AppHeader |
| `app/(crud)/page.tsx` | Redirect → `/items` |
| `app/(crud)/items/page.tsx` | **LIST** — DataTable + FilterBar + Pagination + EmptyState + BulkActionsBar |
| `app/(crud)/items/new/page.tsx` | **CREATE** — CrudCreateForm |
| `app/(crud)/items/[id]/page.tsx` | **DETAIL** — DetailPage with audit fields |
| `app/(crud)/items/[id]/edit/page.tsx` | **EDIT** — CrudEditForm + CrudDeleteConfirm danger zone |

---

## How to fork

### 1. Copy the directory into your project

```bash
cp -r templates/L4/crud/app <your-nextjs-project>/app
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

The backend API requires a Bearer JWT. Before copying, decide how to inject
the auth token:

- **Option A**: Add an Axios/fetch interceptor that reads the token from
  `localStorage` / a cookie and attaches `Authorization: Bearer <token>`.
- **Option B**: Use a Next.js route handler as a BFF proxy that adds the
  token server-side.

### 5. Configure MSW handlers (development)

Create `src/mocks/browser.ts` and `src/mocks/handlers.ts` with mock
responses for `/api/items`. See the
[MSW docs](https://mswjs.io/docs/getting-started) for setup.

### 6. Update domain entity

The template uses a generic `Item` type with `title` and `description`
fields. To rename:

1. Update the `ITEM_FIELDS` arrays in `new/page.tsx` and `[id]/edit/page.tsx`.
2. Update `COLUMNS` in `items/page.tsx`.
3. Update the `Item` TypeScript interface everywhere.
4. Rename the `/api/items` endpoints to match your domain resource.

---

## Spec Trio binding

| Artifact | File |
|---|---|
| Page Compliance Spec | `specs/crud-frontend-l0.yaml` |
| UI Contract | `contracts/crud-ui.yaml` |
| UI Policy Manifest | `blueprints/crud-ui-manifest.yaml` |
| Backend OpenAPI | `contracts/crud-openapi.yaml` |
| Backend Security Spec | `specs/crud-security.yaml` |

Run `bash practices/evals/trio_integrity_guard.sh --domain crud` to verify
the full Spec Trio is intact.

---

## Verification

```bash
# Verify L4 crud composition contract (static analysis)
cd frontend && npx playwright test tests/L4/crud/

# Verify full domain spec trio
bash skills/ax-verify-domain/scripts/run.sh crud

# Verify L4 layer
bash skills/ax-verify-L4/scripts/run.sh
```
