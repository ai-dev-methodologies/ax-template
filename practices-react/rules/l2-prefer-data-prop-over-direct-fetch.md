---
title: "L2 data blocks — receive data as prop; never call fetch() or useQuery() inline"
impact: HIGH
impactDescription: "Calling fetch() or useQuery() inside an L2 data block binds it to a specific endpoint URL or query key, breaking layer-decoupling and making the block untestable without a running backend."
tags:
  - l2-layer
  - data-fetching
  - decoupling
  - data-blocks
  - tanstack-query
applicable_to:
  - nextjs
  - react
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-L2-002"
verification:
  type: review
  status: manual
  notes: "For each L2 data block, verify: (a) no fetch() or useQuery() calls inside the component, (b) data is accepted as a typed prop (data: Row[]), (c) loading/error state is accepted as props not derived from a query hook."
provenance:
  pilot: true
  pipeline_version: "2026-05-18"
  pipeline_steps: [implementation_observed, phaseB_audit_4check, phaseC_codex_consensus]
audit:
  accuracy:
    status: verified
    last_verified: "2026-05-18"
  freshness:
    status: current
    last_verified: "2026-05-18"
    next_review_by: "2026-08-16"
  completeness:
    status: complete
    amendments:
      - "Observed during SP7 implementation: DataTable, FilterBar, Pagination, SearchInput all required this discipline"
  gap_check:
    status: complete
evidence:
  - upstream_id: tanstack-query-v5
    section: "Overview — separation of fetching and UI"
    quote: "React Query makes fetching, caching, synchronizing and updating server state in your React applications a breeze."
sibling_rules:
  - l2-prefer-onsubmit-prop
  - async-api-routes
  - client-swr-dedup
---

## L2 data blocks — receive data as prop; never call `fetch()` or `useQuery()` inline

**Impact: HIGH — Inlining a fetch or query hook inside an L2 block binds it to a URL and query key, destroying reusability and testability.**

### The violation (do NOT do this in L2)

```typescript
// ❌ WRONG — L2 block fetching its own data
import { useQuery } from '@tanstack/react-query'

export default function DataTable() {
  // Hardcoded endpoint = domain coupling
  const { data, isLoading } = useQuery({
    queryKey: ['products'],
    queryFn: () => fetch('/api/products').then(r => r.json()),
  })
  return <table>...</table>
}
```

### Correct — data received as props

```typescript
// ✅ CORRECT — L2 block renders whatever data the caller provides
export interface DataTableProps<Row> {
  data: Row[]                            // caller fetches, block renders
  isLoading?: boolean                    // caller's loading state
  onSort?: (state: SortState) => void    // caller handles server-sort
  getRowKey: (row: Row) => string
  columns: ColumnDef<Row>[]
}

export default function DataTable<Row>({ data, isLoading, ...}: DataTableProps<Row>) {
  return <table aria-busy={isLoading}>...</table>
}
```

### L4 owns the fetch + query

```typescript
// app/(app)/products/page.tsx — L4 fetches and passes data down
import { useQuery } from '@tanstack/react-query'
import DataTable from 'templates/L2/blocks/data-table'

export default function ProductsPage() {
  const [sort, setSort] = useState<SortState>()
  const { data, isLoading } = useQuery({
    queryKey: ['products', sort],
    queryFn: () => fetchProducts(sort),
  })
  return (
    <DataTable
      data={data ?? []}
      isLoading={isLoading}
      sort={sort}
      onSort={setSort}
      columns={PRODUCT_COLUMNS}
      getRowKey={p => p.id}
    />
  )
}
```

### Why this rule exists

During SP7 block implementation, DataTable, FilterBar, Pagination, and SearchInput were natural candidates for inline TanStack Query usage. Keeping data as a prop:

1. **Tests without a backend** — pass `data={mockRows}` directly in unit tests.
2. **Works with any server-state library** — TanStack Query, SWR, RSC, manual fetch — caller decides.
3. **Supports any endpoint** — same DataTable renders products, orders, or users.
4. **Decouples pagination strategy** — cursor vs. offset pagination stays in L4.

### Layer enforcement

L2 data blocks must not contain:
- `import { useQuery } from '@tanstack/react-query'`
- `import useSWR from 'swr'`
- `fetch(...)` calls
- `import ... from 'app/...''` (any backend URL binding)
