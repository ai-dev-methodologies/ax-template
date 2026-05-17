/*
---
template_id: L4/crud/app/(crud)/items/[id]/page
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: getItem
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — item DETAIL page composing L3 detail-page; displays item fields and audit trail."
  - source_type: external
    citation: "Next.js 15 App Router dynamic routes — params prop for [id] segment"
    url: "https://nextjs.org/docs/app/building-your-application/routing/dynamic-routes"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useQuery } from '@tanstack/react-query'
import DetailPage from 'templates/L3/pages/detail-page/[id]/page'

// ─── types ──────────────────────────────────────────────────────────────────

interface Item {
  id: string
  title: string
  description?: string
  createdBy: string
  createdAt: string
  updatedBy?: string
  updatedAt?: string
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchItem(id: string): Promise<Item> {
  const res = await fetch(`/api/items/${id}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (res.status === 404) throw new Error('Item not found')
  if (!res.ok) throw new Error(`Failed to fetch item: ${res.status}`)
  return res.json() as Promise<Item>
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
 * ItemDetailPage — L4 crud item detail page.
 *
 * Composes:
 *   L3 detail-page → page chrome (title, back link, actions slot, sections slot)
 *
 * Fork instructions:
 *   1. Replace fetch with your API client or tRPC query.
 *   2. Add domain-specific sections to sectionsSlot (e.g. related resources, timeline).
 *   3. Extend actionsSlot with domain-specific actions (e.g. publish, archive).
 */
export default function ItemDetailPage({ params }: { params: { id: string } }) {
  const { id } = params

  const { data: item, isLoading, isError } = useQuery<Item>({
    queryKey: ['items', id],
    queryFn: () => fetchItem(id),
  })

  if (isLoading) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading" />
      </div>
    )
  }

  if (isError || !item) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-2xl">
        <div role="alert" className="rounded-lg border border-destructive/40 bg-destructive/5 px-6 py-4 text-sm text-destructive">
          Item not found or you do not have permission to view it.
          <a href="/items" className="ml-2 underline hover:no-underline">
            Back to items
          </a>
        </div>
      </div>
    )
  }

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
      href={`/items/${id}/edit`}
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
