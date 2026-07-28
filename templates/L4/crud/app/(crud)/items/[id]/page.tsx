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
import ItemDetailView, { type Item } from './item-detail-view'

// ─── fetcher ────────────────────────────────────────────────────────────────

async function fetchItem(id: string): Promise<Item> {
  const res = await fetch(`/api/items/${id}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (res.status === 404) throw new Error('Item not found')
  if (!res.ok) throw new Error(`Failed to fetch item: ${res.status}`)
  return res.json() as Promise<Item>
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

  return <ItemDetailView item={item} />
}
