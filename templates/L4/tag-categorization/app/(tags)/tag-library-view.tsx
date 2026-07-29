/*
---
template_id: L4/tag-categorization/app/(tags)/tag-library-view
layer: L4
domain: tag-categorization
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (tags)/page.tsx (BACKLOG P2-42
      render-testability pass-2 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient) is a hard
      dependency-resolution boundary for a vitest that imports this file directly from outside
      frontend/ — the @tanstack/react-query bare specifier does not resolve for a module living in
      templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the
      same class of gap). The recursive TagRow renderer, buildTree helper, and the entire Add/Edit
      form's local draft state (incl. the unsaved-draft window.confirm gate, same precedent as
      email-outbox-view's confirm()-stays-in-the-view) move here — create/update are threaded in
      as Promise-returning callbacks so this view can reset its own draft fields only on success,
      without lifting that state to the page. templates/L2/blocks/{empty-state,error-boundary}
      have zero external-npm deps. previewSlug (../slug-preview) is a pure string function with no
      data-fetching dependency."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { previewSlug } from '../slug-preview'

// ─── types ───────────────────────────────────────────────────────────────────

export interface TagResponse {
  id: string
  name: string
  slug: string
  parentTagId: string | null
  color: string | null
  createdAt: string
  createdByUserId: string
}

export interface TagListResponse {
  items: TagResponse[]
  totalElements: number
}

export interface CreateTagInput {
  name: string
  parentTagId: string | null
  color: string | null
}

export interface UpdateTagInput {
  name: string
  color: string | null
}

export interface TagLibraryViewProps {
  data: TagListResponse | undefined
  error: Error | null
  isLoading: boolean
  isAdmin: boolean

  createErrorMessage: string | null
  onDismissCreateError: () => void
  updateErrorMessage: string | null
  onDismissUpdateError: () => void
  deleteErrorMessage: string | null
  onDismissDeleteError: () => void

  createPending: boolean
  updatePending: boolean
  /** Tag id currently mid-delete — drives the per-row busy/disabled state. */
  pendingDeleteId: string | null

  /** Resolves on success, rejects on failure — the view resets its own draft fields only when
   *  this resolves (see evidence rationale above). */
  onCreateTag: (input: CreateTagInput) => Promise<void>
  onUpdateTag: (id: string, input: UpdateTagInput) => Promise<void>
  onDeleteTag: (id: string) => void
}

// ─── tree assembly ────────────────────────────────────────────────────────────

interface TagNode {
  tag: TagResponse
  children: TagNode[]
}

function buildTree(items: TagResponse[]): TagNode[] {
  const byId = new Map<string, TagNode>()
  for (const t of items) byId.set(t.id, { tag: t, children: [] })

  const roots: TagNode[] = []
  for (const t of items) {
    const node = byId.get(t.id)
    if (!node) continue
    if (t.parentTagId && byId.has(t.parentTagId)) {
      byId.get(t.parentTagId)!.children.push(node)
    } else {
      roots.push(node)
    }
  }
  const sortAlpha = (ns: TagNode[]): TagNode[] => {
    const sorted = [...ns].sort((a, b) => a.tag.name.localeCompare(b.tag.name))
    for (const n of sorted) n.children = sortAlpha(n.children)
    return sorted
  }
  return sortAlpha(roots)
}

// ─── row ─────────────────────────────────────────────────────────────────────

interface TagRowProps {
  node: TagNode
  depth: number
  isAdmin: boolean
  busyId: string | null
  onEdit: (tag: TagResponse) => void
  onDelete: (tag: TagResponse) => void
}

function TagRow({ node, depth, isAdmin, busyId, onEdit, onDelete }: TagRowProps) {
  const visibleIndent = Math.min(depth, 6) * 16
  const busy = busyId === node.tag.id
  return (
    <>
      <li
        style={{ paddingLeft: visibleIndent }}
        className="flex items-center justify-between gap-3 border-b px-3 py-2"
      >
        <div className="flex min-w-0 items-center gap-2">
          {node.tag.color && (
            <span
              className="inline-block h-3 w-3 shrink-0 rounded-full border"
              style={{ backgroundColor: node.tag.color }}
              role="img"
              aria-label={`Tag color ${node.tag.color}`}
            />
          )}
          <span className="truncate text-sm font-medium">{node.tag.name}</span>
          {isAdmin && (
            <span className="truncate font-mono text-xs text-muted-foreground">
              {node.tag.slug}
            </span>
          )}
        </div>
        {isAdmin && (
          <div className="flex shrink-0 gap-2">
            <button
              type="button"
              className="rounded border px-2 py-1 text-xs hover:bg-muted disabled:opacity-50"
              disabled={busy}
              aria-label={`Edit tag ${node.tag.name}`}
              onClick={() => onEdit(node.tag)}
            >
              Edit
            </button>
            <button
              type="button"
              className="rounded border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50 disabled:opacity-50"
              disabled={busy}
              aria-label={`Delete tag ${node.tag.name}`}
              onClick={() => onDelete(node.tag)}
            >
              {busy ? 'Deleting…' : 'Delete'}
            </button>
          </div>
        )}
      </li>
      {node.children.map((c) => (
        <TagRow
          key={c.tag.id}
          node={c}
          depth={depth + 1}
          isAdmin={isAdmin}
          busyId={busyId}
          onEdit={onEdit}
          onDelete={onDelete}
        />
      ))}
    </>
  )
}

// ─── component ──────────────────────────────────────────────────────────────

/**
 * TagLibraryView — pure presentational render of the tag taxonomy library.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(tags)/page.tsx`) owns all query/mutation state and passes the resolved `data` +
 * Promise-returning create/update callbacks in. This keeps the component a plain props -> JSX
 * function, which is what makes it renderable in a unit test without a QueryClientProvider.
 */
export default function TagLibraryView({
  data,
  error,
  isLoading,
  isAdmin,
  createErrorMessage,
  onDismissCreateError,
  updateErrorMessage,
  onDismissUpdateError,
  deleteErrorMessage,
  onDismissDeleteError,
  createPending,
  updatePending,
  pendingDeleteId,
  onCreateTag,
  onUpdateTag,
  onDeleteTag,
}: TagLibraryViewProps) {
  const tree = React.useMemo(() => (data ? buildTree(data.items) : []), [data])

  const [editing, setEditing] = React.useState<TagResponse | null>(null)
  const [draftName, setDraftName] = React.useState('')
  const [draftColor, setDraftColor] = React.useState('')
  const [draftParentId, setDraftParentId] = React.useState('')

  const draftSlug = React.useMemo(() => previewSlug(draftName), [draftName])
  const submitBlocked = draftName.trim().length === 0

  const resetForm = () => {
    setEditing(null)
    setDraftName('')
    setDraftColor('')
    setDraftParentId('')
  }

  const handleSubmitCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (submitBlocked) return
    try {
      await onCreateTag({
        name: draftName.trim(),
        parentTagId: draftParentId || null,
        color: draftColor.trim() || null,
      })
      resetForm()
    } catch {
      // error surfaces via createErrorMessage — keep the draft so the operator can retry.
    }
  }

  const handleSubmitEdit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editing || submitBlocked) return
    try {
      await onUpdateTag(editing.id, { name: draftName.trim(), color: draftColor.trim() || null })
      resetForm()
    } catch {
      // error surfaces via updateErrorMessage — keep the draft so the operator can retry.
    }
  }

  const handleStartEdit = (tag: TagResponse) => {
    const dirtyCreate =
      !editing &&
      (draftName.trim().length > 0 || draftColor.trim().length > 0 || draftParentId.length > 0)
    if (
      dirtyCreate &&
      !window.confirm(
        'You have unsaved changes in the Add tag form. Switch to editing this tag and discard the in-progress draft?',
      )
    ) {
      return
    }
    setEditing(tag)
    setDraftName(tag.name)
    setDraftColor(tag.color ?? '')
    setDraftParentId(tag.parentTagId ?? '')
  }

  const handleDelete = (tag: TagResponse) => {
    const ok = window.confirm(
      `Delete tag "${tag.name}"?\n\nThis cannot be undone. The backend will reject the delete if any child tags reference this one.`,
    )
    if (!ok) return
    onDeleteTag(tag.id)
  }

  return (
    <ErrorBoundary>
      <div className="space-y-6">
        <header className="flex items-baseline justify-between">
          <div>
            <h1 className="text-lg font-semibold">Tag library</h1>
            <p className="text-sm text-muted-foreground">
              Hierarchical tag taxonomy. {isAdmin
                ? 'You can create, edit, and delete tags.'
                : 'Tag definitions are managed by administrators. You can view but not edit.'}
            </p>
          </div>
        </header>

        {(createErrorMessage || updateErrorMessage || deleteErrorMessage) && (
          <div className="space-y-1.5">
            {createErrorMessage && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Create failed: {createErrorMessage}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={onDismissCreateError}
                >
                  Dismiss
                </button>
              </div>
            )}
            {updateErrorMessage && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Update failed: {updateErrorMessage}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={onDismissUpdateError}
                >
                  Dismiss
                </button>
              </div>
            )}
            {deleteErrorMessage && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Delete failed: {deleteErrorMessage}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={onDismissDeleteError}
                >
                  Dismiss
                </button>
              </div>
            )}
          </div>
        )}

        {isAdmin && (
          <section className="rounded border p-3">
            <h2 className="mb-2 text-sm font-semibold">
              {editing ? `Edit "${editing.name}"` : 'Add tag'}
            </h2>
            <form
              className="space-y-2"
              onSubmit={editing ? handleSubmitEdit : handleSubmitCreate}
            >
              <label className="block space-y-1">
                <span className="text-xs font-medium">Name</span>
                <input
                  type="text"
                  className="w-full rounded border px-2 py-1 text-sm"
                  value={draftName}
                  onChange={(e) => setDraftName(e.target.value)}
                  placeholder="e.g. high-priority, 긴급"
                  required
                  maxLength={64}
                />
                {draftName && (
                  <span className="block text-xs text-muted-foreground">
                    Slug preview (server is source of truth):{' '}
                    <span className="font-mono">
                      {draftSlug || '(server will generate a tag-XXXX slug)'}
                    </span>
                  </span>
                )}
              </label>
              <label className="block space-y-1">
                <span className="text-xs font-medium">Color (CSS color string, optional)</span>
                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    className="w-full rounded border px-2 py-1 text-sm"
                    value={draftColor}
                    onChange={(e) => setDraftColor(e.target.value)}
                    placeholder="#f87171 or red"
                    maxLength={16}
                  />
                  {draftColor.trim() && (
                    (typeof CSS !== 'undefined' && CSS.supports('color', draftColor.trim())) ? (
                      <span
                        className="inline-block h-6 w-6 shrink-0 rounded border"
                        style={{ backgroundColor: draftColor.trim() }}
                        role="img"
                        aria-label={`Color preview ${draftColor.trim()}`}
                      />
                    ) : (
                      <span
                        className="inline-block h-6 w-6 shrink-0 rounded border border-red-300 bg-[repeating-linear-gradient(45deg,_#fee_0_4px,_white_4px_8px)]"
                        role="img"
                        aria-label="Invalid color"
                        title="Not a valid CSS color — the swatch will not render"
                      />
                    )
                  )}
                </div>
              </label>
              {!editing ? (
                <label className="block space-y-1">
                  <span className="text-xs font-medium">Parent tag (optional)</span>
                  <select
                    className="w-full rounded border px-2 py-1 text-sm"
                    value={draftParentId}
                    onChange={(e) => setDraftParentId(e.target.value)}
                  >
                    <option value="">(none — root tag)</option>
                    {data?.items.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                      </option>
                    ))}
                  </select>
                </label>
              ) : (
                <p className="rounded border border-amber-200 bg-amber-50 px-2 py-1 text-xs text-amber-900">
                  Parent tag cannot be changed after creation. To move this tag
                  under a different parent, delete and recreate (only possible
                  if it has no children or attachments).
                </p>
              )}
              <div className="flex gap-2">
                <button
                  type="submit"
                  className="rounded bg-foreground px-3 py-1.5 text-sm text-background hover:opacity-90 disabled:opacity-50"
                  disabled={submitBlocked || createPending || updatePending}
                >
                  {editing ? 'Save changes' : 'Create tag'}
                </button>
                {editing && (
                  <button
                    type="button"
                    className="rounded border px-3 py-1.5 text-sm hover:bg-muted"
                    onClick={resetForm}
                  >
                    Cancel
                  </button>
                )}
              </div>
            </form>
          </section>
        )}

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading tags…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load tags" description={error.message} />
        ) : tree.length === 0 ? (
          <EmptyState
            title="No tags yet"
            description={
              isAdmin
                ? 'Add your first tag using the form above. Tags can be nested under a parent.'
                : 'No tags have been defined. Ask an administrator to add some.'
            }
          />
        ) : (
          <ul className="rounded border">
            {tree.map((root) => (
              <TagRow
                key={root.tag.id}
                node={root}
                depth={0}
                isAdmin={isAdmin}
                busyId={pendingDeleteId}
                onEdit={handleStartEdit}
                onDelete={handleDelete}
              />
            ))}
          </ul>
        )}
      </div>
    </ErrorBoundary>
  )
}
