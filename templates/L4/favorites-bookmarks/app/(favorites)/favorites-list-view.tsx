/*
---
template_id: L4/favorites-bookmarks/app/(favorites)/favorites-list-view
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (favorites)/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient/
      useCallerId) is a hard dependency-resolution boundary for a vitest that imports this file
      directly from outside frontend/ — the @tanstack/react-query bare specifier does not resolve
      for a module living in templates/L4/... (see frontend/tests/audit-log-redaction-render.
      vitest.tsx's own note on the same class of gap). The per-row 'global count' widget
      (RowCount, page-local) has its OWN useQuery for its lazy-reveal fetch — a data-fetching hook
      nested inside the render tree, not just top-level orchestration — so it is passed in as a
      `renderCount` render-prop rather than defined in this file, keeping this view's own import
      list free of @tanstack/react-query entirely. templates/L2/blocks/{empty-state,error-boundary,
      confirm-dialog} have zero external-npm deps and are safe to import/render directly."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import ConfirmDialog from 'templates/L2/blocks/confirm-dialog'

// ─── types ───────────────────────────────────────────────────────────────────

export interface FavoriteResponse {
  id: string
  entityType: string
  entityId: string
  note: string | null
  favoritedAt: string
}

export interface FavoriteListResponse {
  items: FavoriteResponse[]
  totalElements: number
}

export interface FavoritesListViewProps {
  data: FavoriteListResponse | undefined
  error: Error | null
  isLoading: boolean

  quotaErrorMessage: string | null
  onDismissQuotaError: () => void
  addOtherErrorMessage: string | null
  onDismissAddError: () => void
  removeErrorMessage: string | null
  onDismissRemoveError: () => void

  formType: string
  formId: string
  formNote: string
  onFormTypeChange: (value: string) => void
  onFormIdChange: (value: string) => void
  onFormNoteChange: (value: string) => void
  onSubmitAdd: (e: React.FormEvent<HTMLFormElement>) => void
  addPending: boolean

  /** Called on the row's Remove click. The caller (page) owns the confirm-vs-direct branching
   *  (a note-bearing favorite must be confirmed first — R50 destructive-action-confirm) because
   *  that decision is entangled with the confirmingRemove state the mutation's onSettled clears. */
  onRequestRemove: (favorite: FavoriteResponse) => void
  removePending: boolean

  /** Page-owned confirm-dialog state (cleared by the remove mutation's onSettled, not by this
   *  view) — see onRequestRemove's own doc comment for why this cannot be view-local state. */
  confirmingRemove: { entityType: string; entityId: string; note: string | null } | null
  onCancelConfirmRemove: () => void
  onConfirmRemove: () => void

  /** RowCount has its own useQuery (lazy-reveal global count) — see evidence rationale above. */
  renderCount: (entityType: string, entityId: string) => React.ReactNode
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

// ─── component ──────────────────────────────────────────────────────────────

/**
 * FavoritesListView — pure presentational render of the caller's favorites list.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(favorites)/page.tsx`) owns all query/mutation state and passes the resolved `data`,
 * error messages, form state, and mutation-trigger callbacks in. This keeps the component a plain
 * props -> JSX function, which is what makes it renderable in a unit test without a
 * QueryClientProvider. Owns its OWN local UI-only state (the confirm-before-destructive-remove
 * toggle and the `now` timestamp used for timeAgo) — neither is a data-fetching concern.
 */
export default function FavoritesListView({
  data,
  error,
  isLoading,
  quotaErrorMessage,
  onDismissQuotaError,
  addOtherErrorMessage,
  onDismissAddError,
  removeErrorMessage,
  onDismissRemoveError,
  formType,
  formId,
  formNote,
  onFormTypeChange,
  onFormIdChange,
  onFormNoteChange,
  onSubmitAdd,
  addPending,
  onRequestRemove,
  removePending,
  confirmingRemove,
  onCancelConfirmRemove,
  onConfirmRemove,
  renderCount,
}: FavoritesListViewProps) {
  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const handle = () => setNow(new Date())
    window.addEventListener('focus', handle)
    return () => window.removeEventListener('focus', handle)
  }, [])

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

        {quotaErrorMessage && (
          <div
            role="alert"
            className="rounded border border-amber-400 bg-amber-50 px-3 py-2 text-sm text-amber-900"
          >
            <div className="font-semibold">Favorite cap reached</div>
            <div>{quotaErrorMessage}</div>
            <button
              type="button"
              className="mt-1 text-xs underline"
              onClick={onDismissQuotaError}
            >
              Dismiss
            </button>
          </div>
        )}

        {addOtherErrorMessage && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Add failed: {addOtherErrorMessage}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={onDismissAddError}
            >
              Dismiss
            </button>
          </div>
        )}

        {removeErrorMessage && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Remove failed: {removeErrorMessage}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={onDismissRemoveError}
            >
              Dismiss
            </button>
          </div>
        )}

        <form
          onSubmit={onSubmitAdd}
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
              onChange={(e) => onFormTypeChange(e.target.value)}
              required
              maxLength={64}
            />
            <input
              aria-label="Entity id"
              placeholder="entity id"
              className="rounded border px-2 py-1 text-sm"
              value={formId}
              onChange={(e) => onFormIdChange(e.target.value)}
              required
              maxLength={128}
            />
            <input
              aria-label="Optional note (max 256 chars)"
              placeholder="optional note (max 256)"
              className="min-w-[14rem] flex-1 rounded border px-2 py-1 text-sm"
              value={formNote}
              onChange={(e) => onFormNoteChange(e.target.value)}
              maxLength={256}
            />
            <button
              type="submit"
              className="rounded border bg-primary px-3 py-1 text-sm text-primary-foreground hover:opacity-90 aria-busy:opacity-60 aria-disabled:opacity-50"
              aria-busy={addPending || undefined}
              aria-disabled={addPending || !formType.trim() || !formId.trim() || undefined}
            >
              {addPending ? 'Adding…' : 'Add'}
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
          <EmptyState title="Failed to load favorites" description={error.message} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title="No favorites yet"
            description="Star an entity from its detail page or use the form above to bookmark one here."
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.items.map((f) => {
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
                      {renderCount(f.entityType, f.entityId)}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-muted aria-busy:opacity-60 aria-disabled:opacity-50"
                    aria-label={`Remove ${f.entityType}/${f.entityId} from favorites`}
                    aria-busy={removePending || undefined}
                    onClick={() => onRequestRemove(f)}
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
          isLoading={removePending}
          onCancel={onCancelConfirmRemove}
          onConfirm={onConfirmRemove}
        />
      </div>
    </ErrorBoundary>
  )
}
