/*
---
template_id: L4/activity-feed/app/(activities)/page
layer: L4
domain: activity-feed
domain_mode: full_trio
backend_operation_id: listMyActivityFeed
evidence:
  - source_type: internal
    rationale: "L4 activity-feed vertical — caller's activity inbox. Visibility = actor OR audience contains caller (enforced server-side per ActivityStreams 2.0). Mark-read on click; mark-all-read CTA; unread filter via URL."
  - source_type: external
    citation: "ActivityStreams 2.0 Core (W3C)"
    url: "https://www.w3.org/TR/activitystreams-core/"
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/practices, L4/payment]
---
*/
'use client'

import * as React from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallerId } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'
import ActivityFeedView, {
  type ActivityFeedResponse,
} from './activity-feed-view'

// ─── types ───────────────────────────────────────────────────────────────────

interface MarkAllReadResponse {
  markedCount: number
}

// ─── constants ───────────────────────────────────────────────────────────────

const PAGE_SIZE = 30

// ─── data ─────────────────────────────────────────────────────────────────────

async function fetchFeed(
  page: number,
  unread: boolean,
): Promise<ActivityFeedResponse> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(PAGE_SIZE))
  if (unread) params.set('unread', '1')
  const res = await fetch(`/api/activities?${params.toString()}`)
  if (!res.ok) throw await parseError(res, 'Failed to load activity feed')
  return res.json()
}

interface MarkReadResponse {
  // R44 iter2 (P2-F12 CRITICAL): the backend MUST return its actual
  // readAt timestamp so the client does not have to fabricate one.
  // Forensic timeline integrity (한국 개인정보보호법 §29, SOX §404)
  // depends on UI never displaying a time the server did not record.
  // If the backend cannot be changed in this release, the client falls
  // back to null and the UI shows "read" without a fabricated timestamp.
  readAt: string | null
}

async function markRead(id: string): Promise<MarkReadResponse> {
  const res = await fetch(`/api/activities/${id}/read`, { method: 'POST' })
  // RFC 9110 §9.2.2 idempotent: mark-read of an already-read event
  // returns the same success status; both states are success.
  // R44 iter2 (P1-F17): `!res.ok` already treats 204 as success (res.ok
  // is true for 200-299) — the prior `&& res.status !== 204` was dead.
  if (!res.ok) {
    throw await parseError(res, 'Failed to mark read')
  }
  // Some backends emit 204 No Content; tolerate empty body.
  try {
    const body = await res.json()
    return { readAt: typeof body?.readAt === 'string' ? body.readAt : null }
  } catch {
    return { readAt: null }
  }
}

async function markAllRead(): Promise<MarkAllReadResponse> {
  const res = await fetch('/api/activities/mark-all-read', { method: 'POST' })
  if (!res.ok) throw await parseError(res, 'Failed to mark all read')
  return res.json()
}

// ─── page ────────────────────────────────────────────────────────────────────

/**
 * ActivityFeedPage — caller's activity inbox.
 *
 * Visibility invariant (mirrors backend ActivityService): an event is
 * shown to the caller IFF the caller is the actor OR the caller is in
 * the audience. The server filters; this client never receives events
 * outside that set.
 *
 * R44 dogfood-iterated invariants:
 *   - All hooks above the early returns (Rules of Hooks).
 *   - Mark-read is idempotent (204 on already-read treated as success
 *     per RFC 9110 §9.2.2).
 *   - useCallerId throws in production if the fork-receiver forgets
 *     to replace the stub — no silent demo-user shipping.
 *   - Mark-all-read confirms unconditionally — the backend marks the
 *     caller's entire account, not just the visible page; iter1 caught
 *     the earlier "on this page" copy as a UI lie (P1-F3 critical).
 *   - Mark-read uses a typed pendingReadIds Set in component state, not
 *     a cache sentinel — the cache only carries backend truth (P2-F12
 *     forensic-timeline closure).
 */
export default function ActivityFeedPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const callerId = useCallerId()
  const qc = useQueryClient()

  // R44 iter2 (P2-F11): NaN-safe page parse. ?page=abc previously
  // produced NaN which propagated into the fetch URL.
  const rawPage = Number(searchParams.get('page') ?? 0)
  const page = Number.isFinite(rawPage) && rawPage >= 0 ? Math.floor(rawPage) : 0
  const unread = searchParams.get('unread') === '1'

  // ─── all hooks BEFORE any conditional early return ─────────────────────────

  // R82 — Providers wires a QueryClient default refetchInterval (see
  // app/providers.tsx), so this page MUST expose dataUpdatedAt as the
  // visible polling-cadence signal for operators on a second monitor.
  const { data, error, isLoading, dataUpdatedAt } = useQuery({
    queryKey: ['activity-feed', page, unread],
    queryFn: () => fetchFeed(page, unread),
  })

  // R44 iter3 (N3 P2-low): typed pending set in component-local state
  // instead of mutating cache with an empty-string sentinel. Removes
  // the wire-format collision class entirely — the cache only ever
  // carries the backend's truth or null.
  const [pendingReadIds, setPendingReadIds] = React.useState<Set<string>>(
    () => new Set(),
  )

  // R82 — mutation-in-flight-uses-aria-busy: pendingReadIds is threaded through as a prop
  // so ActivityFeedView (the co-located pure presentational view — P2-42) can render
  // aria-busy on each row's Mark-read button and the Mark-all-read button; the attributes
  // live there, not in this file, because that markup moved with the render layer.
  const read = useMutation({
    mutationFn: markRead,
    onMutate: (id: string) => {
      setPendingReadIds((prev) => {
        const next = new Set(prev)
        next.add(id)
        return next
      })
      return { id }
    },
    onSuccess: (resp, id) => {
      // R44 iter3 (N2 P1-medium): invalidate the whole query family
      // instead of writing to a closure-captured (page, unread) key.
      // Fast pagination after Mark-read no longer leaves the optimistic
      // write stranded on a stale page. The freshly-refetched data
      // carries the backend's authoritative readAt — never fabricated.
      qc.invalidateQueries({ queryKey: ['activity-feed'] })
      setPendingReadIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      // R44 iter2: if the server returned a readAt we could surface a
      // toast 'read just now' here — kept silent in the catalog
      // baseline; fork-receivers can wire a ToastQueue per their
      // design system.
      void resp
    },
    onError: (_err, id) => {
      setPendingReadIds((prev) => {
        const next = new Set(prev)
        next.delete(id)
        return next
      })
      // R44 iter3 (N6 P2-low): invalidate on error so the next refetch
      // authoritatively confirms the row is still unread server-side.
      // The optimistic state is held in pendingReadIds which we just
      // cleared, so the row already reverts visually — but invalidate
      // closes the corner case where the cache had a stale readAt set
      // by a prior interaction.
      qc.invalidateQueries({ queryKey: ['activity-feed'] })
    },
  })

  const markAll = useMutation({
    mutationFn: markAllRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['activity-feed'] })
      // R44 iter2 (P1-F11): mark-all-read can empty pages > 0. Reset to
      // page 0 so the user is not stranded on a now-empty page index.
      // R44 iter3 (N5 P1-low): no trailing '?' when params are empty.
      const sp = new URLSearchParams(searchParams.toString())
      sp.delete('page')
      const qs = sp.toString()
      router.replace(qs ? `/activities?${qs}` : '/activities')
    },
  })

  const updateUnread = React.useCallback(
    (next: boolean) => {
      const sp = new URLSearchParams(searchParams.toString())
      if (next) sp.set('unread', '1')
      else sp.delete('unread')
      sp.delete('page')
      router.push(`/activities?${sp.toString()}`)
    },
    [router, searchParams],
  )

  const updatePage = React.useCallback(
    (next: number) => {
      const sp = new URLSearchParams(searchParams.toString())
      sp.set('page', String(next))
      router.push(`/activities?${sp.toString()}`)
    },
    [router, searchParams],
  )

  // ─── render ─────────────────────────────────────────────────────────────────

  return (
    <ActivityFeedView
      data={data}
      error={error as Error | null}
      isLoading={isLoading}
      dataUpdatedAt={dataUpdatedAt}
      callerId={callerId}
      unread={unread}
      onToggleUnread={() => updateUnread(!unread)}
      onMarkAllRead={() => markAll.mutate()}
      markAllPending={markAll.isPending}
      readErrorMessage={read.error?.message ?? null}
      onDismissReadError={() => read.reset()}
      markAllErrorMessage={markAll.error?.message ?? null}
      onDismissMarkAllError={() => markAll.reset()}
      pendingReadIds={pendingReadIds}
      onMarkRead={(id) => read.mutate(id)}
      onPageChange={updatePage}
    />
  )
}
