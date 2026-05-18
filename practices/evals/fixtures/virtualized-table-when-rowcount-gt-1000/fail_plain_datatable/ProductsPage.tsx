/**
 * FIXTURE: virtualized-table-when-rowcount-gt-1000/fail_plain_datatable
 * Demonstrates WRONG pattern: DataTable used with 5000 rows without virtualization.
 * Renders 5000 DOM nodes at once — causes jank, layout thrashing, and INP > 500ms.
 * Guard must catch: DataTable used with row count > 1000 without VirtualizedTable.
 */
"use client";

// VIOLATION: using plain DataTable for large datasets causes performance issues
import { DataTable } from "templates/L2/blocks/data-table";
import { useMemo } from "react";

// Simulating 5000 products — a realistic catalog size
function generateMockProducts(count: number) {
  return Array.from({ length: count }, (_, i) => ({
    id: `PROD-${i}`,
    name: `Product ${i}`,
    price: Math.random() * 100,
    category: "Electronics",
  }));
}

const MOCK_PRODUCTS = generateMockProducts(5000);

export default function ProductsPage() {
  // VIOLATION: 5000 rows loaded into a non-virtualized DataTable.
  // All 5000 <tr> elements are created and inserted into the DOM.
  // INP > 500ms, Time to Interactive > 3s on mid-range devices.
  return (
    <div>
      <h1>Products ({MOCK_PRODUCTS.length})</h1>
      <DataTable
        data={MOCK_PRODUCTS}
        columns={[
          { key: "id", header: "ID" },
          { key: "name", header: "Name" },
          { key: "price", header: "Price" },
          { key: "category", header: "Category" },
        ]}
        getRowKey={(row) => row.id}
      />
    </div>
  );
}
