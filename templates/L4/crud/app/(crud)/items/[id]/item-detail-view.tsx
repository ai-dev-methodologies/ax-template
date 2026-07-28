/*
---
template_id: L4/crud/app/(crud)/items/[id]/item-detail-view
layer: L4
domain: crud
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (crud)/items/[id]/page.tsx (BACKLOG P2-28
      render-testability closure — same class as (audit-log)/[id]/audit-log-detail-view.tsx): the
      page's data-fetch orchestration (useQuery) is a hard dependency-resolution boundary for a
      vitest that imports this file directly from outside frontend/ — the @tanstack/react-query
      bare specifier does not resolve for a module living in templates/L4/... (see
      frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the same class of gap).
      Splitting the resolved-item->JSX render surface into its own file makes the detail render
      path unit-testable without touching shared vitest config. templates/L3/pages/detail-page is
      safe to import here (React-only, zero external-npm deps — unlike @tanstack/react-query)."
---
*/
import * as React from 'react'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'

// ─── types ──────────────────────────────────────────────────────────────────

export interface Item {
  id: string
  title: string
  description?: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

// ─── helpers ─────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * ItemDetailView — pure presentational render of a crud item's detail page.
 *
 * Deliberately has ZERO data-fetching dependencies (no useQuery) — the caller
 * (`(crud)/items/[id]/page.tsx`) owns loading/error orchestration and passes the resolved `item`
 * in. This keeps the component a plain props -> JSX function, which is what makes it renderable
 * in a unit test without a QueryClientProvider.
 */
export default function ItemDetailView({ item }: { item: Item }) {
  const sectionsSlot = (
    <dl className="divide-y rounded-lg border bg-card">
      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Title</dt>
        <dd className="col-span-2 text-sm">{item.title}</dd>
      </div>
      {item.description && (
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Description</dt>
          <dd className="col-span-2 text-sm whitespace-pre-line">{item.description}</dd>
        </div>
      )}
      <div className="grid grid-cols-3 gap-4 px-6 py-4">
        <dt className="text-sm font-medium text-muted-foreground">Created</dt>
        <dd className="col-span-2 text-sm">
          {formatDate(item.createdAt)} by <span className="font-medium">{item.createdBy}</span>
        </dd>
      </div>
      {item.updatedAt && (
        <div className="grid grid-cols-3 gap-4 px-6 py-4">
          <dt className="text-sm font-medium text-muted-foreground">Last updated</dt>
          <dd className="col-span-2 text-sm">
            {formatDate(item.updatedAt)}
            {item.updatedBy && (
              <> by <span className="font-medium">{item.updatedBy}</span></>
            )}
          </dd>
        </div>
      )}
    </dl>
  )

  const actionsSlot = (
    <a
      href={`/items/${item.id}/edit`}
      className="inline-flex items-center rounded-md border bg-background px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
    >
      Edit
    </a>
  )

  return (
    <DetailPage
      title={item.title}
      backHref="/items"
      backLabel="Back to items"
      actionsSlot={actionsSlot}
      sectionsSlot={sectionsSlot}
    />
  )
}
