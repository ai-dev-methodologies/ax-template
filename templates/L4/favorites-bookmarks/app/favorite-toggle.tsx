/*
---
template_id: L4/favorites-bookmarks/app/favorite-toggle
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: toggleFavorite
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — embeddable star button. Fork-receivers drop this into any host-entity UI (a market card, an article header, an org detail) and the toggle wires through the catalog endpoints. Anchored to R38 caller-authentication-only and R38 http-delete-idempotency-rfc9110 — the favorites domain is the canonical example for both rules."
  - source_type: external
    citation: "RFC 9110 §9.3.5 — HTTP DELETE idempotency"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html#name-delete"
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
import { parseError, CodedError } from 'templates/L0/fork-receiver-kit/parse-error'
import { assertSafeEntityRef } from 'templates/L0/fork-receiver-kit/entity-key'

interface CheckResponse {
  favorited: boolean
}

async function fetchCheck(entityType: string, entityId: string): Promise<CheckResponse> {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/check/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
  )
  if (!res.ok) throw await parseError(res, 'Failed to check favorite')
  return res.json()
}

async function addFavorite(entityType: string, entityId: string): Promise<void> {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch('/api/favorites', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ entityType, entityId }),
  })
  if (!res.ok) throw await parseError(res, 'Failed to add favorite')
}

async function removeFavorite(entityType: string, entityId: string): Promise<void> {
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  // R38 http-delete-idempotency-rfc9110: 204 on absent target is success;
  // fetch's res.ok already covers 200-299 so no dead branch needed.
  if (!res.ok) throw await parseError(res, 'Failed to remove favorite')
}

interface FavoriteToggleProps {
  entityType: string
  entityId: string
  /**
   * Optional label rendered to the right of the star. Hidden on small
   * screens by callers as needed. Defaults to '' (icon-only).
   */
  label?: string
}

/**
 * FavoriteToggle — drop-in star button for any (entityType, entityId).
 *
 * Anchored invariants (R38):
 *   - The check endpoint is caller-scoped server-side — this component
 *     NEVER sends a `?userId=` parameter.
 *   - DELETE is RFC 9110 §9.3.5 idempotent — 204 on already-removed is
 *     success and shown as such.
 *
 * Fork-receivers drop this into host-entity UI:
 *
 *   <FavoriteToggle entityType="market" entityId={market.id} label="Save" />
 *
 * The component owns its own check/toggle queries; no caller context
 * required beyond the entity pair.
 */
export function FavoriteToggle({ entityType, entityId, label }: FavoriteToggleProps) {
  const qc = useQueryClient()
  const queryKey = React.useMemo(
    () => ['favorite-check', entityType, entityId],
    [entityType, entityId],
  )

  // R44 lesson: all hooks before any conditional return.
  const { data, error, isLoading } = useQuery({
    queryKey,
    queryFn: () => fetchCheck(entityType, entityId),
  })

  // R46 iter2 (F1 high + F2 medium): optimistic-update with snapshot
  // and rollback. Click → cache flips immediately → server confirms →
  // invalidate reconciles. Removes the stale-window race where a fast
  // double-click could re-enter the same direction (add+add, etc.).
  //
  // The decision to add or remove is snapshotted from the *current*
  // cache at click time and captured in the mutation variables, so the
  // mutationFn never reads stale state mid-flight.
  type ToggleDirection = 'add' | 'remove'

  const toggle = useMutation({
    mutationFn: async (direction: ToggleDirection) => {
      if (direction === 'remove') {
        await removeFavorite(entityType, entityId)
      } else {
        await addFavorite(entityType, entityId)
      }
    },
    onMutate: async (direction) => {
      await qc.cancelQueries({ queryKey })
      const previous = qc.getQueryData<CheckResponse | undefined>(queryKey)
      qc.setQueryData<CheckResponse | undefined>(queryKey, {
        favorited: direction === 'add',
      })
      return { previous }
    },
    onError: (_err, _direction, ctx) => {
      if (ctx?.previous) qc.setQueryData(queryKey, ctx.previous)
      // Also invalidate the list view in case it had been optimistically
      // touched by a separate `FavoritesListPage` instance.
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey })
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
    },
  })

  const favorited = data?.favorited ?? false
  const ariaLabel = favorited
    ? `Remove ${entityType} from favorites`
    : `Add ${entityType} to favorites`

  // R46 iter2 (F8 medium): aria-busy + aria-disabled instead of native
  // `disabled` so the screen reader announces the busy state and the
  // focus order remains intact while a mutation is in flight.
  const busy = isLoading || toggle.isPending

  return (
    <>
      <button
        type="button"
        className="inline-flex items-center gap-1 rounded border px-2 py-1 text-sm hover:bg-muted aria-busy:opacity-60 aria-disabled:opacity-50"
        aria-pressed={favorited}
        aria-label={ariaLabel}
        aria-busy={toggle.isPending || undefined}
        aria-disabled={busy || undefined}
        // R46 iter2 (F7 medium): error stays out of the native title
        // attribute — title shows only the aria-label so an
        // over-the-shoulder observer or screenshare attendee cannot
        // see server prose that may carry incidental PII.
        title={ariaLabel}
        onClick={() => {
          if (busy) return
          toggle.mutate(favorited ? 'remove' : 'add')
        }}
      >
        <span aria-hidden className={favorited ? 'text-amber-500' : 'text-muted-foreground'}>
          {favorited ? '★' : '☆'}
        </span>
        {label && <span>{label}</span>}
      </button>
      {/* Error surface: aria-live region next to the button so
           assistive tech announces failures instead of hiding them in
           a tooltip. */}
      {(toggle.error || error) && (() => {
        const e = (toggle.error ?? error) as Error
        // R55 — quota error gets actionable copy. CodedError preserves the
        // backend ProblemDetail.code; non-CodedError shows the message as-is.
        const isQuota = e instanceof CodedError && e.code === 'FAVORITES_QUOTA_EXCEEDED'
        return (
          <span
            role="alert"
            className={
              isQuota
                ? 'ml-1 text-xs text-amber-900'
                : 'ml-1 text-xs text-red-700'
            }
          >
            {isQuota
              ? 'Favorite cap reached — remove some favorites first.'
              : e.message}
          </span>
        )
      })()}
    </>
  )
}
