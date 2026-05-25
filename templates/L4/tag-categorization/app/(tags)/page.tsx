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
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import { useCallerRole } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import { previewSlug } from '../slug-preview'

// ─── types ───────────────────────────────────────────────────────────────────

interface TagResponse {
  id: string
  name: string
  slug: string
  parentTagId: string | null
  color: string | null
  createdAt: string
  createdByUserId: string
}

interface TagListResponse {
  items: TagResponse[]
  totalElements: number
}

interface CreateTagRequest {
  name: string
  parentTagId: string | null
  color: string | null
}

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
  const sortAlpha = (ns: TagNode[]) => {
    ns.sort((a, b) => a.tag.name.localeCompare(b.tag.name))
    ns.forEach((n) => sortAlpha(n.children))
  }
  sortAlpha(roots)
  return roots
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
  // R42 lesson (comment-thread): cap visual indent at 6 levels so a
  // deeply nested tag tree does not overflow horizontally. The data
  // model itself is unbounded.
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
          {/* R45 iter2 (F9 low): non-admin viewers do not need the
               implementation-detail slug. Hide for cleaner read-only UX. */}
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

// ─── page ────────────────────────────────────────────────────────────────────

export default function TagLibraryPage() {
  const role = useCallerRole()
  const isAdmin = role === 'admin'
  const qc = useQueryClient()

  // ─── all hooks ABOVE any conditional early return (Rules of Hooks) ─────────

  const { data, error, isLoading } = useQuery({
    queryKey: ['tags'],
    queryFn: fetchTags,
  })

  const [pendingDeleteId, setPendingDeleteId] = React.useState<string | null>(null)

  const tree = React.useMemo(() => (data ? buildTree(data.items) : []), [data])

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

  const [editing, setEditing] = React.useState<TagResponse | null>(null)
  const [draftName, setDraftName] = React.useState('')
  const [draftColor, setDraftColor] = React.useState('')
  const [draftParentId, setDraftParentId] = React.useState<string>('')

  const draftSlug = React.useMemo(() => previewSlug(draftName), [draftName])
  const submitBlocked = draftName.trim().length === 0

  const handleSubmitCreate = (e: React.FormEvent) => {
    e.preventDefault()
    if (submitBlocked) return
    create.mutate(
      {
        name: draftName.trim(),
        parentTagId: draftParentId || null,
        color: draftColor.trim() || null,
      },
      {
        onSuccess: () => {
          setDraftName('')
          setDraftColor('')
          setDraftParentId('')
        },
      },
    )
  }

  const handleStartEdit = (tag: TagResponse) => {
    // R45 iter2 (F6 medium): warn before overwriting an in-progress
    // Add-tag draft. Without this, partially-typed creates were silently
    // lost when the operator pivoted to editing an existing tag.
    const dirtyCreate =
      !editing &&
      (draftName.trim().length > 0 ||
        draftColor.trim().length > 0 ||
        draftParentId.length > 0)
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

  const handleSubmitEdit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!editing || submitBlocked) return
    update.mutate(
      {
        id: editing.id,
        body: { name: draftName.trim(), color: draftColor.trim() || null },
      },
      {
        onSuccess: () => {
          setEditing(null)
          setDraftName('')
          setDraftColor('')
          setDraftParentId('')
        },
      },
    )
  }

  const handleDelete = (tag: TagResponse) => {
    // R44 lesson: explicit confirm with verbatim consequence — the
    // backend rejects deletion when the tag has children (catalog
    // R32 invariant), so we tell the user that up front.
    const ok = window.confirm(
      `Delete tag "${tag.name}"?\n\nThis cannot be undone. The backend will reject the delete if any child tags reference this one.`,
    )
    if (!ok) return
    del.mutate(tag.id)
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

        {(create.error || update.error || del.error) && (
          <div className="space-y-1.5">
            {create.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Create failed: {create.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => create.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
            {update.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Update failed: {update.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => update.reset()}
                >
                  Dismiss
                </button>
              </div>
            )}
            {del.error && (
              <div
                role="alert"
                className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
              >
                <span>Delete failed: {del.error.message}</span>
                <button
                  type="button"
                  className="shrink-0 text-xs underline"
                  onClick={() => del.reset()}
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
                      {/* R45 iter2 (F10 low): honest copy when previewSlug
                           returns '' — the backend writes `tag-<random>`
                           for any input whose ASCII slug is empty, so the
                           message accurately sets that expectation rather
                           than implying the server picks something
                           deterministic from the input. */}
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
                  {/* R45 iter2 (F5 medium): live color preview + validity
                       feedback. Browser silently ignores invalid CSS
                       colors, so we use CSS.supports to detect and tell
                       the user explicitly. */}
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
                // R45 iter2 (F4 medium): explain why the parent field is
                // missing in edit mode. UpdateTagRequest deliberately
                // omits parentTagId — moving a tag between parents is
                // not supported by the current backend contract.
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
                  disabled={
                    submitBlocked || create.isPending || update.isPending
                  }
                >
                  {editing ? 'Save changes' : 'Create tag'}
                </button>
                {editing && (
                  <button
                    type="button"
                    className="rounded border px-3 py-1.5 text-sm hover:bg-muted"
                    onClick={() => {
                      setEditing(null)
                      setDraftName('')
                      setDraftColor('')
                      setDraftParentId('')
                    }}
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
          <EmptyState title="Failed to load tags" description={(error as Error).message} />
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
