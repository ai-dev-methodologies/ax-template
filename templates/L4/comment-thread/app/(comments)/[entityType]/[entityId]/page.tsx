/*
---
template_id: L4/comment-thread/app/(comments)/[entityType]/[entityId]/page
layer: L4
domain: comment-thread
domain_mode: full_trio
backend_operation_id: listCommentsByEntity
evidence:
  - source_type: internal
    rationale: "L4 comment-thread vertical — threaded list for a polymorphic (entityType, entityId) pair. Soft-delete shows [deleted] mask; reply hierarchy via parentCommentId; author-only edit (admin cannot rewrite — R38)."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation for server-state"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "GDPR Article 5 — Lawfulness, fairness and transparency"
    url: "https://gdpr-info.eu/art-5-gdpr/"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import CommentThreadView, {
  type CommentResponse,
  type CommentListResponse,
} from './comment-thread-view'

// ─── data fetching ───────────────────────────────────────────────────────────

async function fetchByEntity(
  entityType: string,
  entityId: string,
): Promise<CommentListResponse> {
  const res = await fetch(
    `/api/comments/by-entity/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
  )
  if (!res.ok) throw new Error(`Failed to load comments (HTTP ${res.status})`)
  return res.json()
}

async function createComment(
  entityType: string,
  entityId: string,
  body: string,
  parentCommentId: string | null,
): Promise<CommentResponse> {
  const res = await fetch('/api/comments', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ entityType, entityId, parentCommentId, body }),
  })
  if (!res.ok) throw new Error(`Failed to post comment (HTTP ${res.status})`)
  return res.json()
}

async function editComment(id: string, body: string): Promise<CommentResponse> {
  const res = await fetch(`/api/comments/${id}`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ body }),
  })
  if (!res.ok) throw new Error(`Failed to edit comment (HTTP ${res.status})`)
  return res.json()
}

async function deleteComment(id: string): Promise<void> {
  const res = await fetch(`/api/comments/${id}`, { method: 'DELETE' })
  if (!res.ok && res.status !== 204) {
    throw new Error(`Failed to delete comment (HTTP ${res.status})`)
  }
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * CommentThreadPage — recursive thread for (entityType, entityId).
 *
 * Fork instructions:
 *   1. Replace the `useSessionContext` stub below with your real session
 *      hook. The page assumes the server already authenticated the
 *      caller; the callerId is used ONLY to gate the "Edit" button
 *      client-side (the server still enforces author-only edit).
 *   2. The isAdmin flag controls Delete visibility, not Edit. Admin can
 *      delete user content but MUST NOT rewrite it (R38 rule).
 *   3. If your domain needs pagination (very long threads), add a
 *      cursor / page selector here.
 */
export default function CommentThreadPage() {
  const params = useParams<{ entityType: string; entityId: string }>()
  const entityType = params.entityType
  const entityId = params.entityId

  // Stub — replace with real session hook in fork.
  const callerId: string | null = 'demo-user'
  const isAdmin = false

  const qc = useQueryClient()
  const { data, error, isLoading } = useQuery({
    queryKey: ['comments', entityType, entityId],
    queryFn: () => fetchByEntity(entityType, entityId),
  })

  const post = useMutation({
    mutationFn: ({ parentCommentId, body }: { parentCommentId: string | null; body: string }) =>
      createComment(entityType, entityId, body, parentCommentId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', entityType, entityId] }),
  })

  const edit = useMutation({
    mutationFn: ({ id, body }: { id: string; body: string }) => editComment(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', entityType, entityId] }),
  })

  const del = useMutation({
    mutationFn: (id: string) => deleteComment(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', entityType, entityId] }),
  })

  const [newBody, setNewBody] = React.useState('')

  return (
    <CommentThreadView
      entityType={entityType}
      entityId={entityId}
      data={data}
      isLoading={isLoading}
      error={error as Error | null}
      callerId={callerId}
      isAdmin={isAdmin}
      newBody={newBody}
      onNewBodyChange={setNewBody}
      onSubmitTopLevel={(body) => {
        post.mutate({ parentCommentId: null, body })
        setNewBody('')
      }}
      submitPending={post.isPending}
      onReply={(parentId, body) => post.mutate({ parentCommentId: parentId, body })}
      onEdit={(id, body) => edit.mutate({ id, body })}
      onDelete={(id) => del.mutate(id)}
    />
  )
}
