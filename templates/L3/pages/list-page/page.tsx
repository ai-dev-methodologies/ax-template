/*
---
template_id: L3/pages/list-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: internal
    rationale: "Generic list-page skeleton for ax-template L4 composition. Accepts slot props for filter, list, and pagination — no domain logic."
dependencies: []
---
*/
import * as React from 'react'

/**
 * ListPage — generic list view template.
 *
 * Slot props:
 *   - listSlot       (required) rendered list or table content
 *   - filterSlot     (optional) filter bar / search input area
 *   - paginationSlot (optional) pagination controls
 *   - createHref     (optional) if provided, renders a "Create" link button
 *   - createLabel    (optional) label for the create link (default: "Create")
 *   - title          (required) page heading
 *   - description    (optional) subtitle text
 *
 * L4 usage:
 *   import ListPage from 'templates/L3/pages/list-page/page'
 *   export default function ProductsPage() {
 *     return (
 *       <ListPage
 *         title="Products"
 *         filterSlot={<ProductFilter />}
 *         listSlot={<ProductTable />}
 *         paginationSlot={<Pagination />}
 *         createHref="/products/new"
 *       />
 *     )
 *   }
 */
export interface ListPageProps {
  /** Page heading */
  title: string
  /** Optional subtitle below the heading */
  description?: string
  /** Filter bar / search slot (rendered above list) */
  filterSlot?: React.ReactNode
  /** Main list or table content (required) */
  listSlot: React.ReactNode
  /** Pagination controls (rendered below list) */
  paginationSlot?: React.ReactNode
  /** If provided, renders a "Create" anchor above the list */
  createHref?: string
  /** Label for the create anchor (default: "Create") */
  createLabel?: string
}

export default function ListPage({
  title,
  description,
  filterSlot,
  listSlot,
  paginationSlot,
  createHref,
  createLabel = 'Create',
}: ListPageProps) {
  return (
    <main className="container mx-auto px-4 py-8 space-y-6">
      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {createHref && (
          <a
            href={createHref}
            className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 transition-opacity"
          >
            {createLabel}
          </a>
        )}
      </div>

      {/* Filter slot */}
      {filterSlot && <div className="w-full">{filterSlot}</div>}

      {/* List / table slot */}
      <div className="w-full">{listSlot}</div>

      {/* Pagination slot */}
      {paginationSlot && (
        <div className="flex justify-center">{paginationSlot}</div>
      )}
    </main>
  )
}
