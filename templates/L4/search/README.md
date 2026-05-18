# L4/search — Search Domain Reference Workload

**Layer**: L4 (full vertical)
**Domain**: search
**Mode**: `full_trio` — bound to `contracts/search-openapi.yaml` + `contracts/search-ui.yaml`

This directory is the **reference workload** for the search domain. It composes
L1 (shadcn primitives) + L2 (feature blocks) + L3 (page templates) into a
full-text and faceted search vertical backed by `SearchIndexService` and the
backend search API.

---

## What's included

| File | Purpose |
|---|---|
| `app/(search)/layout.tsx` | Authenticated shell: AppShell + Sidebar + AppHeader |
| `app/(search)/search/page.tsx` | Search results page: SearchBar + FacetPanel + ResultList + Pagination |
| `app/(search)/search/[id]/page.tsx` | Detail page for a search result item |

---

## How to fork

### 1. Copy the directory into your project

```bash
cp -r ax-template/templates/L4/search/app/(search) your-app/app/(search)
```

### 2. Copy the L1/L2/L3 dependencies

```bash
cp -r ax-template/templates/L1/components  your-app/src/components/ui
cp -r ax-template/templates/L2/blocks      your-app/src/components/blocks
cp -r ax-template/templates/L3/pages       your-app/src/templates
```

### 3. Wire the backend search service

The backend reference implementation lives at:

```
templates/backend/search/SearchIndexService.java
```

Two adapters are provided — choose one:

- `MeilisearchAdapter.java` — for Meilisearch (recommended for new projects)
- `PostgresFtsAdapter.java` — for PostgreSQL full-text search (zero extra infra)

Set `SEARCH_BACKEND` env var to `meilisearch` or `postgres-fts`.

### 4. Configure entity indexing

Annotate entities that must be indexed:

```java
@SearchIndexed(indexName = "products")
public class Product { ... }
```

`SearchIndexService` picks up `@SearchIndexed` beans automatically and
triggers re-indexing on every CREATE / UPDATE via the Spring event bus.

---

## Spec Trio binding

| Artifact | File |
|---|---|
| Page Compliance Spec | `specs/search-frontend-l0.yaml` |
| UI Contract | `contracts/search-ui.yaml` |
| UI Policy Manifest | `blueprints/search-ui-manifest.yaml` |
| Backend OpenAPI | `contracts/search-openapi.yaml` |

Run `bash practices/evals/trio_integrity_guard.sh --domain search` to verify
the full Spec Trio is intact.

---

## Backend operation bindings

| Route | HTTP | Operation ID | Backend endpoint |
|-------|------|--------------|------------------|
| /search | GET | `searchQuery` | `/api/search?q=&page=&size=` |
| /search/{id} | GET | `searchGetItem` | `/api/search/{id}` |

All operation IDs resolve against `contracts/search-openapi.yaml`.

---

## Fork customisation checklist

- [ ] Choose a backend adapter: `MeilisearchAdapter` or `PostgresFtsAdapter`
- [ ] Annotate domain entities with `@SearchIndexed(indexName = "...")`
- [ ] Update `SEARCH_BACKEND` and `MEILISEARCH_URL` / `MEILISEARCH_KEY` env vars
- [ ] Configure facet fields in `SearchQueryParser.java` for your entity schema
- [ ] Update `COLUMNS` and `FACETS` constants in `search/page.tsx`
- [ ] Set `NEXT_PUBLIC_API_BASE` for the search API proxy rewrite

---

## Verification

```bash
# From repo root
bash skills/ax-verify-domain/scripts/run.sh search
bash skills/ax-verify-L4/scripts/run.sh
```

Both must exit 0 before shipping.

## Recipe Composition

applied_recipe: e-commerce
applied_recipe_secondary: crm
applied_recipes:
  - b2b-admin
  - crm
  - e-commerce
  - marketplace
