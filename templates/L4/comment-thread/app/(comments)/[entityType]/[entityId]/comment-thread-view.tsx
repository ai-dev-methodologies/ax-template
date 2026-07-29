/*
---
template_id: L4/comment-thread/app/(comments)/[entityType]/[entityId]/comment-thread-view
layer: L4
domain: comment-thread
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (comments)/[entityType]/[entityId]/page.tsx
      (BACKLOG P2-42 render-testability pass-1 closure — same class as
      (crud)/items/[id]/item-detail-view.tsx): the page's data-fetch/mutation orchestration
      (useQuery/useMutation/useQueryClient) is a hard dependency-resolution boundary for a vitest
      that imports this file directly from outside frontend/ — the @tanstack/react-query bare
      specifier does not resolve for a module living in templates/L4/... (see
      frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the same class of gap).
      The recursive CommentBranch renderer and the buildTree data-shaping helper have zero
      data-fetching dependencies (only local useState for the reply/edit forms) and move here
      unmodified. templates/L2/blocks/{empty-state,error-boundary} have zero external-npm deps."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'

// ─── types ───────────────────────────────────────────────────────────────────

export type CommentStatus = 'ACTIVE' | 'DELETED'

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
export interface CommentResponse {
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

export interface CommentListResponse {
  items: CommentResponse[]
  totalElements: number
}

// ─── tree assembly ────────────────────────────────────────────────────────────

export interface CommentNode {
  comment: CommentResponse
  replies: CommentNode[]
}

export function buildTree(items: CommentResponse[]): CommentNode[] {
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
  // Sort by createdAt ascending at every level (immutable: sort a copy, then
  // reassign each node's replies — never mutate an array in place; ax/no-array-mutate-on-state)
  const sortRec = (ns: CommentNode[]): CommentNode[] => {
    const sorted = [...ns].sort((a, b) => a.comment.createdAt.localeCompare(b.comment.createdAt))
    for (const n of sorted) n.replies = sortRec(n.replies)
    return sorted
  }
  return sortRec(roots)
}

// ─── comment branch ────────────────────────────────────────────────────────────

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

// ─── component ──────────────────────────────────────────────────────────────

export interface CommentThreadViewProps {
  entityType: string
  entityId: string
  data: CommentListResponse | undefined
  isLoading: boolean
  error: Error | null
  callerId: string | null
  isAdmin: boolean
  newBody: string
  onNewBodyChange: (value: string) => void
  onSubmitTopLevel: (body: string) => void
  submitPending: boolean
  onReply: (parentId: string, body: string) => void
  onEdit: (id: string, body: string) => void
  onDelete: (id: string) => void
}

/**
 * CommentThreadView — pure presentational render of a polymorphic comment thread.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(comments)/[entityType]/[entityId]/page.tsx`) owns all query/mutation state and
 * passes the resolved `data` + mutation-trigger callbacks in. This keeps the component a plain
 * props -> JSX function, which is what makes it renderable in a unit test without a
 * QueryClientProvider.
 */
export default function CommentThreadView({
  entityType,
  entityId,
  data,
  isLoading,
  error,
  callerId,
  isAdmin,
  newBody,
  onNewBodyChange,
  onSubmitTopLevel,
  submitPending,
  onReply,
  onEdit,
  onDelete,
}: CommentThreadViewProps) {
  const tree = React.useMemo(() => (data ? buildTree(data.items) : []), [data])

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
            onSubmitTopLevel(newBody)
          }}
        >
          <textarea
            className="w-full rounded border px-2 py-1 text-sm"
            rows={3}
            value={newBody}
            onChange={(e) => onNewBodyChange(e.target.value)}
            placeholder="Write a comment…"
          />
          <button
            type="submit"
            className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
            disabled={submitPending}
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
            description={error.message}
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
                onReply={onReply}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </div>
        )}
      </div>
    </ErrorBoundary>
  )
}
