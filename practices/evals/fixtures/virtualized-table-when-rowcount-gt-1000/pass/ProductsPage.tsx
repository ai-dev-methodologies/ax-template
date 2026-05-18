/**
 * FIXTURE: virtualized-table-when-rowcount-gt-1000/pass
 * Demonstrates CORRECT pattern: VirtualizedTable used for 5000 rows.
 * Only ~20 visible rows are rendered at any time — INP stays under 200ms.
 */
"use client";

// CORRECT: VirtualizedTable for large datasets (>1000 rows)
import { VirtualizedTable } from "templates/L2/blocks/virtualized-table";

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
  // CORRECT: VirtualizedTable only renders ~20 rows visible in the viewport.
  // The remaining 4980 rows are not in the DOM — INP and layout cost negligible.
  return (
    <div>
      <h1>Products ({MOCK_PRODUCTS.length})</h1>
      <VirtualizedTable
        data={MOCK_PRODUCTS}
        estimateSize={() => 48}
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
