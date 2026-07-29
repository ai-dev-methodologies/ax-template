/*
---
template_id: L4/tag-categorization/app/(tags)/page
layer: L4
domain: tag-categorization
domain_mode: full_trio
backend_operation_id: listTags
evidence:
  - source_type: internal
    rationale: "L4 tag-categorization vertical — tag library: hierarchical list + admin CRUD (gated). Slug preview mirrors backend TagSlugger; server is source of truth. Delete refuses when tag has children (R32 spec)."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API5:2023 Broken Function Level Authorization"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa5-broken-function-level-authorization/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import TagLibraryView, {
  type TagResponse,
  type TagListResponse,
  type CreateTagInput,
  type UpdateTagInput,
} from './tag-library-view'

// ─── types ───────────────────────────────────────────────────────────────────

type CreateTagRequest = CreateTagInput

interface UpdateTagRequest {
  name?: string
  color?: string | null
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchTags(): Promise<TagListResponse> {
  const res = await fetch('/api/tags')
  if (!res.ok) throw await parseError(res, 'Failed to load tags')
  return res.json()
}

async function createTag(body: CreateTagRequest): Promise<TagResponse> {
  const res = await fetch('/api/tags', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw await parseError(res, 'Failed to create tag')
  return res.json()
}

async function updateTag(id: string, body: UpdateTagRequest): Promise<TagResponse> {
  const res = await fetch(`/api/tags/${id}`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw await parseError(res, 'Failed to update tag')
  return res.json()
}

async function deleteTag(id: string): Promise<void> {
  const res = await fetch(`/api/tags/${id}`, { method: 'DELETE' })
  // R44 lesson: 204 falls under res.ok in fetch semantics (status 200-299).
  // Do NOT add `&& res.status !== 204` — it would be a dead branch.
  // The backend returns 409 TagHasChildrenException when the tag has
  // child tags; that becomes a `parseError` with the server's reason.
  if (!res.ok) throw await parseError(res, 'Failed to delete tag')
}

// ─── page ────────────────────────────────────────────────────────────────────

export default function TagLibraryPage() {
  const callerRole = useCallerRole()
  const isAdmin = callerRole === 'admin'
  const qc = useQueryClient()

  // ─── all hooks ABOVE any conditional early return (Rules of Hooks) ─────────

  const { data, error, isLoading } = useQuery({
    queryKey: ['tags'],
    queryFn: fetchTags,
  })

  const [pendingDeleteId, setPendingDeleteId] = React.useState<string | null>(null)

  const create = useMutation({
    mutationFn: createTag,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
  const update = useMutation({
    mutationFn: ({ id, body }: { id: string; body: UpdateTagRequest }) =>
      updateTag(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
  const del = useMutation({
    mutationFn: deleteTag,
    onMutate: (id: string) => setPendingDeleteId(id),
    onSettled: () => setPendingDeleteId(null),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })

  return (
    <TagLibraryView
      data={data}
      error={error as Error | null}
      isLoading={isLoading}
      isAdmin={isAdmin}
      createErrorMessage={create.error?.message ?? null}
      onDismissCreateError={() => create.reset()}
      updateErrorMessage={update.error?.message ?? null}
      onDismissUpdateError={() => update.reset()}
      deleteErrorMessage={del.error?.message ?? null}
      onDismissDeleteError={() => del.reset()}
      createPending={create.isPending}
      updatePending={update.isPending}
      pendingDeleteId={pendingDeleteId}
      onCreateTag={(input) => create.mutateAsync(input).then(() => undefined)}
      onUpdateTag={(id, input) => update.mutateAsync({ id, body: input }).then(() => undefined)}
      onDeleteTag={(id) => del.mutate(id)}
    />
  )
}
