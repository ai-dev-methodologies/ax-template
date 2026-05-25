/*
---
template_id: L4/favorites-bookmarks/app/(favorites)/page
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: listMyFavorites
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — caller's favorite list with per-row Remove, inline note, global-count lazy reveal, and quota-exceeded actionable banner. Anchored to R38 caller-authentication-only-no-userid-param + R38 http-delete-idempotency-rfc9110 + R46 hooks-before-conditional-return + R50 destructive-action-confirm-with-side-effects + L2 confirm-dialog primitive."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "OWASP API Security Top 10 (2023) — API1:2023 BOLA"
    url: "https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/"
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
import ConfirmDialog from 'templates/L2/blocks/confirm-dialog'
import { useCallerId } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError, CodedError } from 'templates/L0/fork-receiver-kit/parse-error'
import { assertSafeEntityRef } from 'templates/L0/fork-receiver-kit/entity-key'

// ─── types ───────────────────────────────────────────────────────────────────

interface FavoriteResponse {
  id: string
  entityType: string
  entityId: string
  note: string | null
  favoritedAt: string
}

interface FavoriteListResponse {
  items: FavoriteResponse[]
  totalElements: number
}

interface CountResponse {
  count: number
}

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchFavorites(): Promise<FavoriteListResponse> {
  const res = await fetch('/api/favorites')
  if (!res.ok) throw await parseError(res, 'Failed to load favorites')
  return res.json()
}

interface AddFavoriteInput {
  entityType: string
  entityId: string
  note?: string | null
}

async function addFavorite(input: AddFavoriteInput): Promise<void> {
  assertSafeEntityRef(input.entityType, input.entityId)
  const res = await fetch('/api/favorites', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      entityType: input.entityType,
      entityId: input.entityId,
      note: input.note && input.note.trim().length > 0 ? input.note.trim() : null,
    }),
  })
  if (!res.ok) throw await parseError(res, 'Failed to add favorite')
}

async function removeFavorite(entityType: string, entityId: string): Promise<void> {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  // R38 http-delete-idempotency-rfc9110: 204 on absent target is success.
  if (!res.ok) throw await parseError(res, 'Failed to remove favorite')
}

async function fetchCount(entityType: string, entityId: string): Promise<CountResponse> {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/count/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
  )
  if (!res.ok) throw await parseError(res, 'Failed to load count')
  return res.json()
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function timeAgo(iso: string, now: Date): string {
  const ms = Math.max(0, now.getTime() - new Date(iso).getTime())
  const min = ms / 60_000
  if (min < 1) return 'just now'
  if (min < 60) return `${Math.floor(min)}m ago`
  const hr = min / 60
  if (hr < 24) return `${Math.floor(hr)}h ago`
  const d = hr / 24
  if (d < 30) return `${Math.floor(d)}d ago`
  return new Date(iso).toLocaleDateString()
}

function isQuotaExceeded(err: unknown): boolean {
  return err instanceof CodedError && err.code === 'FAVORITES_QUOTA_EXCEEDED'
}

// ─── per-row count widget ────────────────────────────────────────────────────

interface RowCountProps {
  entityType: string
  entityId: string
}

/**
 * Lazy count reveal — global "X others starred this" is FAV-QUERY-002 and is
 * useful but adds a fetch per row when shown eagerly. We render a button that
 * fires the fetch on click; data caches in TanStack so a re-toggle is free.
 */
function RowCount({ entityType, entityId }: RowCountProps) {
  const [enabled, setEnabled] = React.useState(false)
  const { data, error, isFetching } = useQuery({
    queryKey: ['favorite-count', entityType, entityId],
    queryFn: () => fetchCount(entityType, entityId),
    enabled,
    staleTime: 30_000,
  })
  if (!enabled) {
    return (
      <button
        type="button"
        className="text-xs text-muted-foreground underline hover:text-foreground"
        onClick={() => setEnabled(true)}
      >
        Show global count
      </button>
    )
  }
  if (isFetching) {
    return <span className="text-xs text-muted-foreground">Loading count…</span>
  }
  if (error) {
    return (
      <span className="text-xs text-red-700">Count error: {(error as Error).message}</span>
    )
  }
  return (
    <span className="text-xs text-muted-foreground">
      {data?.count ?? 0} others starred this
    </span>
  )
}

// ─── page ────────────────────────────────────────────────────────────────────

export default function FavoritesListPage() {
  useCallerId()
  const qc = useQueryClient()

  // ─── all hooks ABOVE any conditional early return ─────────────────────────

  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const handle = () => setNow(new Date())
    window.addEventListener('focus', handle)
    return () => window.removeEventListener('focus', handle)
  }, [])

  // Confirm-dialog state — replaces window.confirm so the side-effect
  // explanation matches the R50 destructive-action-confirm pattern and the
  // L2 ConfirmDialog primitive (a11y: role=alertdialog, focus-trap-ready).
  const [confirmingRemove, setConfirmingRemove] =
    React.useState<{ entityType: string; entityId: string; note: string | null } | null>(
      null,
    )

  // Add-favorite form state — entityType + entityId + optional note (FAV-VALID-003
  // caps the note at 256 chars). The note is stored verbatim on the row.
  const [formType, setFormType] = React.useState('')
  const [formId, setFormId] = React.useState('')
  const [formNote, setFormNote] = React.useState('')

  const { data, error, isLoading } = useQuery({
    queryKey: ['favorites-list'],
    queryFn: fetchFavorites,
  })

  const add = useMutation({
    mutationFn: (input: AddFavoriteInput) => addFavorite(input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
      qc.invalidateQueries({ queryKey: ['favorite-check'] })
      setFormType('')
      setFormId('')
      setFormNote('')
    },
  })

  const remove = useMutation({
    mutationFn: ({ entityType, entityId }: { entityType: string; entityId: string }) =>
      removeFavorite(entityType, entityId),
    onMutate: async ({ entityType, entityId }) => {
      await qc.cancelQueries({ queryKey: ['favorites-list'] })
      const previous = qc.getQueryData<FavoriteListResponse | undefined>([
        'favorites-list',
      ])
      qc.setQueryData<FavoriteListResponse | undefined>(['favorites-list'], (old) =>
        old
          ? {
              ...old,
              items: old.items.filter(
                (it) =>
                  !(it.entityType === entityType && it.entityId === entityId),
              ),
              totalElements: Math.max(0, old.totalElements - 1),
            }
          : old,
      )
      return { previous }
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.previous) qc.setQueryData(['favorites-list'], ctx.previous)
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
      qc.invalidateQueries({ queryKey: ['favorite-check'] })
      setConfirmingRemove(null)
    },
  })

  // R55 — quota actionable banner. Sticky until the user dismisses or the
  // mutation is reset, because the underlying constraint (1000-cap) needs an
  // action the user must take outside the form (delete some favorites).
  const quotaError = isQuotaExceeded(add.error) ? (add.error as CodedError) : null
  const addOtherError = add.error && !quotaError ? (add.error as Error) : null

  const submitAdd = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!formType.trim() || !formId.trim()) return
    add.mutate({
      entityType: formType.trim(),
      entityId: formId.trim(),
      note: formNote.trim() || null,
    })
  }

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header>
          <h1 className="text-lg font-semibold">My favorites</h1>
          <p className="text-sm text-muted-foreground">
            Entities you have starred. Server scopes this list to your account —
            it never accepts a <code>?userId=</code> parameter.
          </p>
        </header>

        {/* R55 — quota actionable banner. Tells the user precisely what they
            must do to recover, not just "400 bad request". */}
        {quotaError && (
          <div
            role="alert"
            className="rounded border border-amber-400 bg-amber-50 px-3 py-2 text-sm text-amber-900"
          >
            <div className="font-semibold">Favorite cap reached</div>
            <div>
              You have hit the per-user favorite cap (1000). Remove at least one
              favorite from the list below before adding new ones.
            </div>
            <button
              type="button"
              className="mt-1 text-xs underline"
              onClick={() => add.reset()}
            >
              Dismiss
            </button>
          </div>
        )}

        {addOtherError && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Add failed: {addOtherError.message}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={() => add.reset()}
            >
              Dismiss
            </button>
          </div>
        )}

        {remove.error && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Remove failed: {remove.error.message}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={() => remove.reset()}
            >
              Dismiss
            </button>
          </div>
        )}

        {/* R55 — add-with-note form. Note is optional, FAV-VALID-003 caps it
            at 256 chars (server enforces too). Per-row inline edit is NOT in
            scope — the backend has no PATCH endpoint, so note edits would be
            delete+re-add and lose the original favoritedAt. */}
        <form
          onSubmit={submitAdd}
          className="rounded border bg-muted/30 px-3 py-2 space-y-2"
        >
          <div className="text-xs font-medium uppercase text-muted-foreground">
            Add favorite
          </div>
          <div className="flex flex-wrap items-start gap-2">
            <input
              aria-label="Entity type"
              placeholder="entity type (e.g. product)"
              className="rounded border px-2 py-1 text-sm"
              value={formType}
              onChange={(e) => setFormType(e.target.value)}
              required
              maxLength={64}
            />
            <input
              aria-label="Entity id"
              placeholder="entity id"
              className="rounded border px-2 py-1 text-sm"
              value={formId}
              onChange={(e) => setFormId(e.target.value)}
              required
              maxLength={128}
            />
            <input
              aria-label="Optional note (max 256 chars)"
              placeholder="optional note (max 256)"
              className="min-w-[14rem] flex-1 rounded border px-2 py-1 text-sm"
              value={formNote}
              onChange={(e) => setFormNote(e.target.value)}
              maxLength={256}
            />
            <button
              type="submit"
              className="rounded border bg-primary px-3 py-1 text-sm text-primary-foreground hover:opacity-90 aria-busy:opacity-60 aria-disabled:opacity-50"
              aria-busy={add.isPending || undefined}
              aria-disabled={add.isPending || !formType.trim() || !formId.trim() || undefined}
            >
              {add.isPending ? 'Adding…' : 'Add'}
            </button>
          </div>
          <div className="text-xs text-muted-foreground">
            Note appears in this list and on the entity detail page. Removing a
            favorite deletes the note (no undo).
          </div>
        </form>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading favorites…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load favorites" description={(error as Error).message} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title="No favorites yet"
            description="Star an entity from its detail page or use the form above to bookmark one here."
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.items.map((f) => {
              const handleRemove = () => {
                if (f.note && f.note.trim().length > 0) {
                  // R50 destructive-action-confirm-with-side-effects — surface
                  // the side effect (note destruction) via L2 ConfirmDialog.
                  setConfirmingRemove({
                    entityType: f.entityType,
                    entityId: f.entityId,
                    note: f.note,
                  })
                  return
                }
                remove.mutate({ entityType: f.entityType, entityId: f.entityId })
              }
              return (
                <li key={f.id} className="flex items-start gap-3 px-4 py-3">
                  <span aria-hidden className="mt-1 text-amber-500">★</span>
                  <div className="min-w-0 flex-1">
                    <div className="text-sm">
                      <span className="rounded bg-muted px-1.5 py-0.5 text-xs uppercase">
                        {f.entityType}
                      </span>{' '}
                      <span className="break-all font-mono">{f.entityId}</span>
                    </div>
                    {f.note && (
                      <div className="mt-0.5 whitespace-pre-wrap text-xs text-muted-foreground">
                        {f.note}
                      </div>
                    )}
                    <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                      <span>favorited {timeAgo(f.favoritedAt, now)}</span>
                      <span aria-hidden>·</span>
                      <RowCount entityType={f.entityType} entityId={f.entityId} />
                    </div>
                  </div>
                  <button
                    type="button"
                    className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-muted aria-busy:opacity-60 aria-disabled:opacity-50"
                    aria-label={`Remove ${f.entityType}/${f.entityId} from favorites`}
                    aria-busy={remove.isPending || undefined}
                    onClick={handleRemove}
                  >
                    Remove
                  </button>
                </li>
              )
            })}
          </ul>
        )}

        <ConfirmDialog
          open={confirmingRemove !== null}
          title="Remove favorite?"
          description={
            confirmingRemove
              ? `This deletes the note as well — there is no undo.\n\nnote:\n${confirmingRemove.note ?? ''}`
              : ''
          }
          destructive
          confirmLabel="Remove"
          isLoading={remove.isPending}
          onCancel={() => setConfirmingRemove(null)}
          onConfirm={() => {
            if (!confirmingRemove) return
            remove.mutate({
              entityType: confirmingRemove.entityType,
              entityId: confirmingRemove.entityId,
            })
          }}
        />
      </div>
    </ErrorBoundary>
  )
}
