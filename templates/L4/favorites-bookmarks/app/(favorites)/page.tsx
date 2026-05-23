/*
---
template_id: L4/favorites-bookmarks/app/(favorites)/page
layer: L4
domain: favorites-bookmarks
domain_mode: full_trio
backend_operation_id: listMyFavorites
evidence:
  - source_type: internal
    rationale: "L4 favorites-bookmarks vertical — caller's favorite list with per-row Remove and inline note. Anchored to R38 caller-authentication-only-no-userid-param (server scopes to Authentication.getName()) and R38 http-delete-idempotency-rfc9110 (DELETE on absent returns 204)."
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
import { useCallerId } from '../use-caller-id'
import { parseError } from '../parse-error'
import { assertSafeEntityRef } from '../entity-key'

// ─── types ───────────────────────────────────────────────────────────────────

/**
 * FavoriteResponse — caller's own favorite row.
 *
 * Anchored to R38 caller-authentication-only-no-userid-param: the list
 * endpoint is server-scoped to `Authentication.getName()`. The client
 * NEVER sends a `?userId=` parameter — the favorites domain is the
 * canonical example for that rule (FAV-AUTHZ-002).
 */
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

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchFavorites(): Promise<FavoriteListResponse> {
  const res = await fetch('/api/favorites')
  if (!res.ok) throw await parseError(res, 'Failed to load favorites')
  return res.json()
}

async function removeFavorite(entityType: string, entityId: string): Promise<void> {
  // R46 iter2 (F6): defense-in-depth path-segment guard.
  assertSafeEntityRef(entityType, entityId)
  const res = await fetch(
    `/api/favorites/${encodeURIComponent(entityType)}/${encodeURIComponent(entityId)}`,
    { method: 'DELETE' },
  )
  // R38 http-delete-idempotency-rfc9110: 204 on absent target is
  // success per RFC 9110 §9.3.5. fetch's res.ok covers 200-299 so we
  // do NOT add `&& res.status !== 204` — that would be a dead branch
  // (R44 P1-F17 lesson).
  if (!res.ok) throw await parseError(res, 'Failed to remove favorite')
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

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * FavoritesListPage — caller's own favorited entities.
 *
 * Audit posture:
 *   - Server scopes the list to the caller; client never sends
 *     ?userId= (R38 FAV-AUTHZ-002 canonical anchor).
 *   - Per-row Remove is RFC 9110 §9.3.5 idempotent — 204 on already-
 *     absent is treated as success without rolling the optimistic
 *     update back (the row was going away anyway).
 *
 * R44/R45 lessons preempted:
 *   - All hooks above conditional early returns (Rules of Hooks).
 *   - useCallerId production hard-stop with scrubbed message.
 *   - Mutation error banner has Dismiss + .reset() so stale errors do
 *     not stick after a subsequent success.
 *   - Per-row pendingRemoveKeys typed Set, not a cache sentinel — no
 *     fabricated timestamps in the cache.
 *   - parseError surfaces server detail (RFC 9457) + text/html fallback
 *     with PII deny-list.
 */
export default function FavoritesListPage() {
  // R46 iter2 (F10 low): explicit production-hard-stop assertion so a
  // future refactor that strips the unused `callerId` value cannot
  // accidentally remove the guard. The hook itself fires the throw
  // path on production with no wired session.
  useCallerId()

  const qc = useQueryClient()

  // ─── all hooks ABOVE any conditional early return ─────────────────────────

  // R46 iter2 (F15 low): refresh `now` on window focus instead of a
  // forever-running 60s setInterval. Idle tabs throttle setInterval
  // already, but a focus listener is the modern idiom and stops the
  // tab from owning a long-lived timer.
  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const handle = () => setNow(new Date())
    window.addEventListener('focus', handle)
    return () => window.removeEventListener('focus', handle)
  }, [])

  const { data, error, isLoading } = useQuery({
    queryKey: ['favorites-list'],
    queryFn: fetchFavorites,
  })

  // R46 iter2 (F3 medium): optimistic row removal. iter1 dimmed the
  // row (opacity-60) and waited for the invalidate refetch to remove
  // it — heavy users sweeping their favorites saw a 400-1200ms delay.
  // We now drop the row from the cache immediately and reconcile on
  // server confirmation. Errors restore the snapshot.
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
      if (ctx?.previous) {
        qc.setQueryData(['favorites-list'], ctx.previous)
      }
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
    },
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['favorites-list'] })
      // Per-entity check queries used by FavoriteToggle instances on
      // host-entity pages need to know about the removal too.
      qc.invalidateQueries({ queryKey: ['favorite-check'] })
    },
  })

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

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading favorites…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load favorites" description={(error as Error).message} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title="No favorites yet"
            description="Star an entity from its detail page to bookmark it here."
          />
        ) : (
          <ul className="divide-y rounded border">
            {data.items.map((f) => {
              const handleRemove = () => {
                // R46 iter2 (F4 medium): confirm when the favorite carries
                // a note. Notes are reversibility-loss surfaces — a Korean
                // enterprise user often stores 결재/follow-up context
                // there, and an accidental click during a cleanup sweep
                // destroys that context with no undo.
                if (
                  f.note &&
                  f.note.trim().length > 0 &&
                  !window.confirm(
                    `Remove this favorite? The note will also be deleted:\n\n${f.note}`,
                  )
                ) {
                  return
                }
                remove.mutate({ entityType: f.entityType, entityId: f.entityId })
              }
              return (
                <li
                  key={f.id}
                  className="flex items-start gap-3 px-4 py-3"
                >
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
                    <div className="mt-0.5 text-xs text-muted-foreground">
                      favorited {timeAgo(f.favoritedAt, now)}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-muted"
                    aria-label={`Remove ${f.entityType}/${f.entityId} from favorites`}
                    onClick={handleRemove}
                  >
                    Remove
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </div>
    </ErrorBoundary>
  )
}
