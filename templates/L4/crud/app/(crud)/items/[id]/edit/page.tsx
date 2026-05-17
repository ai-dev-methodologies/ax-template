/*
---
template_id: L4/crud/app/(crud)/items/[id]/edit/page
layer: L4
domain: crud
domain_mode: full_trio
backend_operation_id: updateItem
evidence:
  - source_type: internal
    rationale: "L4 crud vertical — item EDIT page composing L3 edit-page + L2 CrudEditForm + L2 CrudDeleteConfirm in danger zone."
  - source_type: external
    citation: "TanStack Query v5 — useMutation for PUT/DELETE requests"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useMutation"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [other L4 domains]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import EditPage from 'templates/L3/pages/edit-page/[id]/page'
import CrudEditForm, { type FieldDef } from 'templates/L2/blocks/crud-edit-form'
import CrudDeleteConfirm from 'templates/L2/blocks/crud-delete-confirm'

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

interface Item {
  id: string
  title: string
  description?: string
  createdBy: string
  createdAt: string
}

interface UpdateItemRequest {
  title?: string
  description?: string
}

// ─── fetchers ───────────────────────────────────────────────────────────────

async function fetchItem(id: string): Promise<Item> {
  const res = await fetch(`/api/items/${id}`, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (res.status === 404) throw new Error('Item not found')
  if (!res.ok) throw new Error(`Failed to fetch item: ${res.status}`)
  return res.json() as Promise<Item>
}

async function updateItem(id: string, data: UpdateItemRequest): Promise<Item> {
  const res = await fetch(`/api/items/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`Failed to update item: ${res.status}`)
  return res.json() as Promise<Item>
}

async function deleteItem(id: string): Promise<void> {
  const res = await fetch(`/api/items/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`Failed to delete item: ${res.status}`)
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * EditItemPage — L4 crud item edit page.
 *
 * Composes:
 *   L3 edit-page         → page chrome (title, cancel link, form card, danger zone)
 *   L2 CrudEditForm      → pre-populated schema-driven form with current item values
 *   L2 CrudDeleteConfirm → confirmation dialog in the danger zone; calls deleteItem
 *
 * Fork instructions:
 *   1. Extend ITEM_FIELDS with your entity's editable field definitions.
 *   2. Replace fetch calls with your API client / tRPC mutations.
 *   3. Update redirect targets after update and delete.
 *   4. Add optimistic updates in the updateItem mutation's onMutate if needed.
 */
export default function EditItemPage({ params }: { params: { id: string } }) {
  const { id } = params
  const queryClient = useQueryClient()
  const [updateError, setUpdateError] = React.useState<string | null>(null)

  const { data: item, isLoading } = useQuery<Item>({
    queryKey: ['items', id],
    queryFn: () => fetchItem(id),
  })

  const updateMutation = useMutation({
    mutationFn: (data: UpdateItemRequest) => updateItem(id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['items'] })
      window.location.href = `/items/${updated.id}`
    },
    onError: (err: Error) => {
      setUpdateError(err.message ?? 'Failed to save changes.')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteItem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['items'] })
      window.location.href = '/items'
    },
  })

  function handleUpdate(data: Record<string, unknown>) {
    setUpdateError(null)
    updateMutation.mutate({
      title: data.title ? String(data.title) : undefined,
      description: data.description ? String(data.description) : undefined,
    })
  }

  if (isLoading || !item) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" aria-label="Loading" />
      </div>
    )
  }

  const initialValues: Record<string, string> = {
    title: item.title,
    description: item.description ?? '',
  }

  return (
    <EditPage
      title={`Edit: ${item.title}`}
      cancelHref={`/items/${id}`}
      formSlot={
        <>
          {updateError && (
            <div role="alert" className="mb-4 rounded-md bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {updateError}
            </div>
          )}
          <CrudEditForm
            fields={ITEM_FIELDS}
            initialValues={initialValues}
            onSubmit={handleUpdate}
            isLoading={updateMutation.isPending}
            submitLabel="Save changes"
          />
        </>
      }
      deleteSlot={
        <CrudDeleteConfirm
          resourceName={item.title}
          onConfirm={() => deleteMutation.mutate()}
          isLoading={deleteMutation.isPending}
          confirmLabel="Delete item"
          dialogDescription="This action cannot be undone. The item will be permanently removed."
        />
      }
    />
  )
}
