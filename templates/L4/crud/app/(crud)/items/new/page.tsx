/*
---
template_id: L4/crud/app/(crud)/items/new/page
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: createItem
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — item CREATE page composing L3 create-page + L2 CrudCreateForm."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for POST requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import CreatePage from 'templates/L3/pages/create-page/page'
import CrudCreateForm, { type FieldDef } from 'templates/L2/blocks/crud-create-form'

// ─── fields ─────────────────────────────────────────────────────────────────

const ITEM_FIELDS: FieldDef[] = [
  {
    key: 'title',
    label: 'Title',
    type: 'text',
    placeholder: 'Enter a title…',
    required: true,
  },
  {
    key: 'description',
    label: 'Description',
    type: 'textarea',
    placeholder: 'Optional description…',
  },
]

// ─── types ──────────────────────────────────────────────────────────────────

interface CreateItemRequest {
  title: string
  description?: string
}

interface Item {
  id: string
  title: string
}

// ─── fetcher ────────────────────────────────────────────────────────────────

async function createItem(data: CreateItemRequest): Promise<Item> {
  const res = await fetch('/api/items', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Failed to create item: ${res.status}`)
  return res.json() as Promise<Item>
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * NewItemPage — L4 crud item create page.
 *
 * Composes:
 *   L3 create-page   → page chrome (title, cancel link, form card)
 *   L2 CrudCreateForm → schema-driven form with title + description fields
 *
 * Fork instructions:
 *   1. Extend ITEM_FIELDS with your entity's field definitions.
 *   2. Replace the fetch call with your API client / tRPC mutation.
 *   3. Update the redirect target after successful creation.
 *   4. Add client-side validation or Zod schema as needed.
 */
export default function NewItemPage() {
  const queryClient = useQueryClient()
  const [error, setError] = React.useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: createItem,
    onSuccess: (created) => {
      // Invalidate list cache so the new item appears on return
      queryClient.invalidateQueries({ queryKey: ['items'] })
      // Navigate to the new item's detail page
      window.location.href = `/items/${created.id}`
    },
    onError: (err: Error) => {
      setError(err.message ?? 'Failed to create item. Please try again.')
    },
  })

  function handleSubmit(data: Record<string, unknown>) {
    setError(null)
    mutation.mutate({
      title: String(data.title ?? ''),
      description: data.description ? String(data.description) : undefined,
    })
  }

  return (
    <CreatePage
      title="New Item"
      description="Fill in the details below to create a new item."
      cancelHref="/items"
      formSlot={
        <>
          {error && (
            <div role="alert" className="mb-4 rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {error}
            </div>
          )}
          <CrudCreateForm
            fields={ITEM_FIELDS}
            onSubmit={handleSubmit}
            isLoading={mutation.isPending}
            submitLabel="Create item"
          />
        </>
      }
    />
  )
}
