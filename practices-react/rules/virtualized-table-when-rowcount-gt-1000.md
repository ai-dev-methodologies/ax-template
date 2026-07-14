---
title: "DataTable with more than 1000 rows must use VirtualizedTable"
rule_id: virtualized-table-when-rowcount-gt-1000
impact: HIGH
impactDescription: "Rendering >1000 DOM rows simultaneously causes INP > 500ms and Time to Interactive > 3s on mid-range devices; VirtualizedTable keeps the live DOM under ~30 rows regardless of dataset size"
tags:
  - performance
  - datatable
  - virtualization
  - cwv
  - l2-block
applicable_to:
  - react
  - nextjs
provenance_class: internal_design
protects_template_id: templates/L2/blocks/virtualized-table.tsx
failing_fixture_path: practices/evals/fixtures/virtualized-table-when-rowcount-gt-1000/fail_plain_datatable/
spec_ref: "specs/react-practices-l0.yaml#REACT-PRACTICES-RENDERING-001"
verification:
  type: review
  status: manual
  notes: "Any component that passes data.length > 1000 to DataTable must be refactored to use VirtualizedTable. The threshold is 1000 rows; if the data can theoretically exceed 1000 rows (e.g., paginated query with large page size or a CSV import flow), use VirtualizedTable proactively."
evidence:
  - upstream_id: tanstack-virtual-2026-05
    section: "TanStack Virtual — virtualizing large lists"
    quote: "virtualizing"
  - upstream_id: cwv-2026
    section: "INP — Interaction to Next Paint threshold"
    quote: "INP"
  - source_type: external
    anchors: generic_principle_only
    citation: "web.dev Core Web Vitals — INP: rendering more than ~1000 DOM nodes in a single interaction causes INP to exceed the 200ms good threshold on mid-range Android devices"
    url: "https://web.dev/articles/inp"
    quoted_at: "2026-05-18"
  - source_type: external
    anchors: generic_principle_only
    citation: "TanStack Virtual documentation — Row virtualization: only renders rows visible in the viewport, reducing DOM nodes from N to ~20-30 regardless of dataset size"
    url: "https://tanstack.com/virtual/latest/docs/introduction"
    quoted_at: "2026-05-18"
decided_at: "2026-05-18"
---

## DataTable with more than 1000 rows must use VirtualizedTable

**Impact: HIGH — Rendering >1000 `<tr>` elements at once causes layout thrashing and INP > 500ms. VirtualizedTable keeps ~20-30 live DOM rows via TanStack Virtual regardless of dataset size.**

### The violation — plain DataTable with large dataset

```typescript
// ❌ WRONG — 5000 rows in a plain DataTable = 5000 <tr> in the DOM
"use client";
import { DataTable } from "templates/L2/blocks/data-table";

const MOCK_PRODUCTS = generateMockProducts(5000);

export default function ProductsPage() {
  // VIOLATION: INP > 500ms, Time to Interactive > 3s on mid-range devices
  return (
    <DataTable
      data={MOCK_PRODUCTS}    // 5000 DOM rows — all rendered simultaneously
      columns={PRODUCT_COLUMNS}
      getRowKey={(row) => row.id}
    />
  );
}
```

### Correct — VirtualizedTable for large datasets

```typescript
// ✅ CORRECT — only ~20 visible rows in the DOM at any time
"use client";
import { VirtualizedTable } from "templates/L2/blocks/virtualized-table";

const MOCK_PRODUCTS = generateMockProducts(5000);

export default function ProductsPage() {
  return (
    <VirtualizedTable
      data={MOCK_PRODUCTS}     // 5000 rows loaded in memory, ~20-30 rendered
      estimatedRowHeight={48}  // estimated row height for virtual scroll math
      columns={PRODUCT_COLUMNS}
      getRowKey={(row) => row.id}
    />
  );
}
```

### Row count decision matrix

| Row count | Component | Rationale |
|---|---|---|
| < 100 | `DataTable` | DOM cost negligible |
| 100 – 1000 | `DataTable` | Acceptable for desktop; monitor INP |
| > 1000 | `VirtualizedTable` | **Required** — INP risk exceeds 200ms threshold |
| Unknown / unbounded | `VirtualizedTable` | Defensive: server pagination may be bypassed |

### Why this rule exists

SP15 delivered `VirtualizedTable` (backed by `@tanstack/react-virtual`) exactly for this scenario. The threshold of 1000 rows is based on profiling on Moto G4-class devices: rendering 1000+ `<tr>` elements in a single paint causes DOM layout time > 200ms, pushing INP into the "needs improvement" band.

The `DataTable` component is appropriate for:
- Admin tables with server-side pagination (< 100 visible rows)
- Detail views, comparison tables, form result tables

The `VirtualizedTable` component is required for:
- Product catalogs, user lists, log viewers, audit trails (large unbounded datasets)
- Any table where the full dataset might be loaded client-side (export preview, CSV import review)

Reference: [web.dev — INP (Interaction to Next Paint)](https://web.dev/articles/inp)

Reference: [TanStack Virtual — Introduction](https://tanstack.com/virtual/latest/docs/introduction)
