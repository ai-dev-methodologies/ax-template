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
import { useCallerId } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError, CodedError } from 'templates/L0/fork-receiver-kit/parse-error'
import { assertSafeEntityRef } from 'templates/L0/fork-receiver-kit/entity-key'
import FavoritesListView, {
  type FavoriteResponse,
  type FavoriteListResponse,
} from './favorites-list-view'

// ─── types ───────────────────────────────────────────────────────────────────

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

  const requestRemove = (f: FavoriteResponse) => {
    if (f.note && f.note.trim().length > 0) {
      // R50 destructive-action-confirm-with-side-effects — surface
      // the side effect (note destruction) via L2 ConfirmDialog.
      setConfirmingRemove({ entityType: f.entityType, entityId: f.entityId, note: f.note })
      return
    }
    remove.mutate({ entityType: f.entityType, entityId: f.entityId })
  }

  return (
    <FavoritesListView
      data={data}
      error={error as Error | null}
      isLoading={isLoading}
      quotaErrorMessage={
        quotaError
          ? 'You have hit the per-user favorite cap (1000). Remove at least one favorite from the list below before adding new ones.'
          : null
      }
      onDismissQuotaError={() => add.reset()}
      addOtherErrorMessage={addOtherError?.message ?? null}
      onDismissAddError={() => add.reset()}
      removeErrorMessage={remove.error?.message ?? null}
      onDismissRemoveError={() => remove.reset()}
      formType={formType}
      formId={formId}
      formNote={formNote}
      onFormTypeChange={setFormType}
      onFormIdChange={setFormId}
      onFormNoteChange={setFormNote}
      onSubmitAdd={submitAdd}
      addPending={add.isPending}
      onRequestRemove={requestRemove}
      removePending={remove.isPending}
      confirmingRemove={confirmingRemove}
      onCancelConfirmRemove={() => setConfirmingRemove(null)}
      onConfirmRemove={() => {
        if (!confirmingRemove) return
        remove.mutate({
          entityType: confirmingRemove.entityType,
          entityId: confirmingRemove.entityId,
        })
      }}
      renderCount={(entityType, entityId) => (
        <RowCount entityType={entityType} entityId={entityId} />
      )}
    />
  )
}
