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
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'

// ─── types ───────────────────────────────────────────────────────────────────

type CommentStatus = 'ACTIVE' | 'DELETED'

/**
 * CommentResponse — single comment row.
 *
 * Anchored to R38 rules:
 *   - soft-delete-audit-trail: when status=DELETED the backend returns
 *     `body='[deleted]'` (DELETED_BODY_MASK on the Java side); the
 *     deletedAt + deletedByUserId metadata survives.
 *   - admin-cannot-rewrite-user-content: the edit form on this page is
 *     only rendered for the author (not for admins). Admin role MAY
 *     delete but MUST NOT edit; the Spring service enforces this
 *     server-side, the UI mirrors it as defense-in-depth.
 *   - caller-authentication-only-no-userid-param: the caller id is
 *     derived from the session on the server; never passed as a query.
 */
interface CommentResponse {
  id: string
  authorUserId: string
  entityType: string
  entityId: string
  parentCommentId: string | null
  body: string
  status: CommentStatus
  createdAt: string
  updatedAt: string | null
  deletedAt: string | null
  deletedByUserId: string | null
}

interface CommentListResponse {
  items: CommentResponse[]
  totalElements: number
}

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

// ─── tree assembly ────────────────────────────────────────────────────────────

interface CommentNode {
  comment: CommentResponse
  replies: CommentNode[]
}

function buildTree(items: CommentResponse[]): CommentNode[] {
  const byId = new Map<string, CommentNode>()
  for (const c of items) byId.set(c.id, { comment: c, replies: [] })

  const roots: CommentNode[] = []
  for (const c of items) {
    const node = byId.get(c.id)
    if (!node) continue
    if (c.parentCommentId && byId.has(c.parentCommentId)) {
      byId.get(c.parentCommentId)!.replies.push(node)
    } else {
      roots.push(node)
    }
  }
  // Sort by createdAt ascending at every level
  const sortRec = (ns: CommentNode[]) => {
    ns.sort((a, b) => a.comment.createdAt.localeCompare(b.comment.createdAt))
    ns.forEach((n) => sortRec(n.replies))
  }
  sortRec(roots)
  return roots
}

// ─── view ────────────────────────────────────────────────────────────────────

interface CommentBranchProps {
  node: CommentNode
  callerId: string | null
  isAdmin: boolean
  depth: number
  onReply: (parentId: string, body: string) => void
  onEdit: (id: string, body: string) => void
  onDelete: (id: string) => void
}

/**
 * CommentBranch — recursive renderer for a comment and its replies.
 *
 * Visual contract:
 *   - DELETED rows show the masked body ('[deleted]') in muted gray.
 *     The deletedAt metadata is shown so the audit trail remains visible.
 *   - Edit action is rendered ONLY when callerId === authorUserId AND
 *     the comment is ACTIVE. Admins never see an Edit button (this is
 *     UI defense-in-depth for admin-cannot-rewrite-user-content).
 *   - Delete action is rendered for author OR admin.
 *   - Nesting depth caps the indentation at 6 levels visually but the
 *     data model is unbounded.
 */
function CommentBranch({
  node,
  callerId,
  isAdmin,
  depth,
  onReply,
  onEdit,
  onDelete,
}: CommentBranchProps) {
  const [replying, setReplying] = React.useState(false)
  const [editing, setEditing] = React.useState(false)
  const [draft, setDraft] = React.useState('')
  const [editDraft, setEditDraft] = React.useState(node.comment.body)

  const isDeleted = node.comment.status === 'DELETED'
  const isAuthor = callerId !== null && node.comment.authorUserId === callerId
  const canEdit = isAuthor && !isDeleted          // admin-cannot-rewrite enforced here
  const canDelete = (isAuthor || isAdmin) && !isDeleted
  const visibleIndent = Math.min(depth, 6) * 16

  return (
    <div style={{ marginLeft: visibleIndent }} className="mt-3">
      <div className={`rounded border p-3 ${isDeleted ? 'opacity-60' : ''}`}>
        <div className="flex items-baseline justify-between text-xs text-muted-foreground">
          <span>
            <span className="font-medium text-foreground">
              {node.comment.authorUserId}
            </span>{' '}
            · {new Date(node.comment.createdAt).toLocaleString()}
            {node.comment.updatedAt && (
              <span className="ml-1 italic">(edited)</span>
            )}
          </span>
          {isDeleted && node.comment.deletedAt && (
            <span>
              deleted {new Date(node.comment.deletedAt).toLocaleString()} by{' '}
              {node.comment.deletedByUserId ?? '—'}
            </span>
          )}
        </div>

        <div
          className={`mt-2 whitespace-pre-wrap text-sm ${
            isDeleted ? 'italic text-muted-foreground' : ''
          }`}
        >
          {node.comment.body}
        </div>

        {!isDeleted && (
          <div className="mt-2 flex gap-3 text-xs">
            <button
              type="button"
              className="text-muted-foreground hover:text-foreground"
              onClick={() => setReplying((v) => !v)}
            >
              Reply
            </button>
            {canEdit && (
              <button
                type="button"
                className="text-muted-foreground hover:text-foreground"
                onClick={() => setEditing((v) => !v)}
              >
                Edit
              </button>
            )}
            {canDelete && (
              <button
                type="button"
                className="text-red-600 hover:underline"
                onClick={() => onDelete(node.comment.id)}
              >
                Delete
              </button>
            )}
          </div>
        )}

        {editing && (
          <form
            className="mt-2 space-y-2"
            onSubmit={(e) => {
              e.preventDefault()
              onEdit(node.comment.id, editDraft)
              setEditing(false)
            }}
          >
            <textarea
              className="w-full rounded border px-2 py-1 text-sm"
              rows={3}
              value={editDraft}
              onChange={(e) => setEditDraft(e.target.value)}
            />
            <div className="flex gap-2">
              <button
                type="submit"
                className="rounded border px-2 py-1 text-xs hover:bg-muted"
              >
                Save edit
              </button>
              <button
                type="button"
                className="rounded border px-2 py-1 text-xs hover:bg-muted"
                onClick={() => {
                  setEditing(false)
                  setEditDraft(node.comment.body)
                }}
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {replying && (
          <form
            className="mt-2 space-y-2"
            onSubmit={(e) => {
              e.preventDefault()
              if (draft.trim().length === 0) return
              onReply(node.comment.id, draft)
              setDraft('')
              setReplying(false)
            }}
          >
            <textarea
              className="w-full rounded border px-2 py-1 text-sm"
              rows={2}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder={`Reply to ${node.comment.authorUserId}…`}
            />
            <div className="flex gap-2">
              <button
                type="submit"
                className="rounded border px-2 py-1 text-xs hover:bg-muted"
              >
                Post reply
              </button>
              <button
                type="button"
                className="rounded border px-2 py-1 text-xs hover:bg-muted"
                onClick={() => {
                  setReplying(false)
                  setDraft('')
                }}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>

      {node.replies.length > 0 && (
        <div>
          {node.replies.map((child) => (
            <CommentBranch
              key={child.comment.id}
              node={child}
              callerId={callerId}
              isAdmin={isAdmin}
              depth={depth + 1}
              onReply={onReply}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </div>
  )
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

  const tree = React.useMemo(
    () => (data ? buildTree(data.items) : []),
    [data],
  )

  const [newBody, setNewBody] = React.useState('')

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header className="space-y-1">
          <h1 className="text-lg font-semibold">
            {entityType} / {entityId}
          </h1>
          <p className="text-sm text-muted-foreground">
            Soft-deleted comments stay in the thread as <code>[deleted]</code> so
            replies still resolve and the audit trail (who deleted, when) is
            preserved.
          </p>
        </header>

        <form
          className="space-y-2 rounded border p-3"
          onSubmit={(e) => {
            e.preventDefault()
            if (newBody.trim().length === 0) return
            post.mutate({ parentCommentId: null, body: newBody })
            setNewBody('')
          }}
        >
          <textarea
            className="w-full rounded border px-2 py-1 text-sm"
            rows={3}
            value={newBody}
            onChange={(e) => setNewBody(e.target.value)}
            placeholder="Write a comment…"
          />
          <button
            type="submit"
            className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
            disabled={post.isPending}
          >
            Post comment
          </button>
        </form>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading comments…
          </div>
        ) : error ? (
          <EmptyState
            title="Failed to load comments"
            description={(error as Error).message}
          />
        ) : tree.length === 0 ? (
          <EmptyState
            title="No comments yet"
            description="Be the first to comment."
          />
        ) : (
          <div>
            {tree.map((node) => (
              <CommentBranch
                key={node.comment.id}
                node={node}
                callerId={callerId}
                isAdmin={isAdmin}
                depth={0}
                onReply={(parentId, body) => post.mutate({ parentCommentId: parentId, body })}
                onEdit={(id, body) => edit.mutate({ id, body })}
                onDelete={(id) => del.mutate(id)}
              />
            ))}
          </div>
        )}
      </div>
    </ErrorBoundary>
  )
}
