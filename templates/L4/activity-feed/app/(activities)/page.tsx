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
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import Pagination from 'templates/L2/blocks/pagination'
import { useCallerId, sameUser } from 'templates/L0/fork-receiver-kit/use-caller-id'
import { parseError } from 'templates/L0/fork-receiver-kit/parse-error'

// ─── types ───────────────────────────────────────────────────────────────────

/**
 * ActivityEvent — single feed row.
 *
 * Anchored to R38 caller-authentication-only-no-userid-param: the
 * server scopes the feed to the caller via Authentication.getName().
 * The client NEVER sends ?userId= — visibility is server-derived from
 * (actor === caller) OR (audience contains caller).
 *
 * R52 (backend-contract wave 1) closed the R44 P2-F7 audience peer
 * leak: the response DTO no longer carries the full audience user-id
 * set. Instead, `youAreInAudience` is a server-computed boolean — the
 * client can still disambiguate "I sent this" (actor === caller) from
 * "Someone CC'd me" (youAreInAudience === true), without learning the
 * other audience members' identities.
 */
interface ActivityEvent {
  id: string
  actorUserId: string
  verb: string
  objectType: string
  objectId: string
  subjectType: string | null
  subjectId: string | null
  metadata: Record<string, unknown>
  youAreInAudience: boolean
  createdAt: string
  readAt: string | null
}

interface ActivityFeedResponse {
  items: ActivityEvent[]
  page: number
  size: number
  totalElements: number
}

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

// ─── helpers ──────────────────────────────────────────────────────────────────

/**
 * verbLabel — humanize a verb token.
 *
 * The backend stores raw ActivityStreams verbs (e.g. 'create', 'mention',
 * 'approve'). The UI maps the common ones to readable phrases and falls
 * back to the raw verb so an unknown verb is visible (rather than
 * silently dropped). Fork-receivers extend the map per their domain.
 */
function verbLabel(verb: string): string {
  switch (verb) {
    case 'create':
      return 'created'
    case 'update':
      return 'updated'
    case 'delete':
      return 'deleted'
    case 'mention':
      return 'mentioned you in'
    case 'approve':
      return 'approved'
    case 'reject':
      return 'rejected'
    case 'comment':
      return 'commented on'
    default:
      return verb
  }
}

function objectLabel(type: string, id: string): string {
  return `${type}/${id}`
}

function timeAgo(iso: string, now: Date): string {
  const ms = Math.max(0, now.getTime() - new Date(iso).getTime())
  const min = ms / 60_000
  if (min < 1) return 'just now'
  if (min < 60) return `${Math.floor(min)}m ago`
  const hr = min / 60
  if (hr < 24) return `${Math.floor(hr)}h ago`
  const d = hr / 24
  if (d < 7) return `${Math.floor(d)}d ago`
  return new Date(iso).toLocaleDateString()
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

  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(t)
  }, [])

  const { data, error, isLoading } = useQuery({
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

  const unreadCount = React.useMemo(
    () => (data ? data.items.filter((e) => e.readAt === null).length : 0),
    [data],
  )

  // R44 iter3 (iter2-N1 high): the iter2 client-side sort lifted unread
  // to the top of the CURRENT page only, teaching the user to trust an
  // incomplete signal — unread items on later pages stayed invisible
  // behind page 0's reordered list. iter3 drops the within-page sort.
  // Heavy users get unread visibility through (a) the blue dot per row
  // and (b) the global 'Show unread only' toggle, which is server-
  // scoped and therefore covers every page authoritatively. A true
  // 'unread-first' ordering belongs on the server (?sort=unread,
  // createdAt) and is tracked as a deferred backend-contract change.

  // ─── render ─────────────────────────────────────────────────────────────────

  return (
    <ErrorBoundary>
      <div className="space-y-4">
        <header className="flex items-baseline justify-between gap-2">
          <div>
            <h1 className="text-lg font-semibold">
              {unread ? 'Unread activity' : 'Activity'}
            </h1>
            <p className="text-sm text-muted-foreground">
              Events where you are the actor or are in the audience. Read state
              persists per user.
            </p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
              onClick={() => updateUnread(!unread)}
            >
              {unread ? 'Show all' : 'Show unread only'}
            </button>
            <button
              type="button"
              className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
              disabled={markAll.isPending}
              onClick={() => {
                // R44 iter2 (P1-F3 CRITICAL): the backend marks the
                // entire account's unread as read, not just the visible
                // page. Confirm ALWAYS (no threshold), with copy that
                // matches the server semantics. The "on this page"
                // language from iter1 was a UI lie that would silently
                // wipe unread evidence outside the visible window.
                if (
                  !window.confirm(
                    'Mark every unread activity on your account as read?\n\nThis cannot be undone from this UI and includes items not visible on the current page.',
                  )
                ) {
                  return
                }
                markAll.mutate()
              }}
              title="Marks all unread activity on your account — confirm required"
            >
              Mark all read
            </button>
          </div>
        </header>

        {/* R44 iter3 (iter2-N4 P1-low): split banners + Dismiss to drop
             sticky TanStack error state. The two mutations now surface
             independently so a simultaneous failure of both is visible. */}
        {read.error && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Mark read failed: {read.error.message}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={() => read.reset()}
            >
              Dismiss
            </button>
          </div>
        )}
        {markAll.error && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Mark all read failed: {markAll.error.message}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={() => markAll.reset()}
            >
              Dismiss
            </button>
          </div>
        )}

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            Loading activity…
          </div>
        ) : error ? (
          <EmptyState title="Failed to load activity" description={(error as Error).message} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title={unread ? 'No unread activity' : 'No activity yet'}
            description={
              unread
                ? 'You are caught up. Switch to "Show all" to see read activity.'
                : 'When someone interacts with content that involves you, it will show up here.'
            }
            actionLabel={unread ? 'Show all' : undefined}
            onAction={unread ? () => updateUnread(false) : undefined}
          />
        ) : (
          <>
            <ul className="divide-y rounded border">
              {data.items.map((e) => {
                const isUnread = e.readAt === null
                // R44 iter3 (iter2-N3): pending state lives in the
                // component, not in the cache. Removes the empty-
                // string sentinel collision class.
                const isPendingRead = isUnread && pendingReadIds.has(e.id)
                const youAreActor = sameUser(e.actorUserId, callerId)
                const youAreSubject = e.subjectId !== null && sameUser(e.subjectId, callerId)
                return (
                  <li
                    key={e.id}
                    className={`flex items-start gap-3 px-4 py-3 ${
                      isUnread && !isPendingRead ? 'bg-blue-50/40' : ''
                    } ${isPendingRead ? 'opacity-60' : ''}`}
                  >
                    <span
                      className={`mt-1.5 inline-block h-2 w-2 shrink-0 rounded-full ${
                        isUnread && !isPendingRead ? 'bg-blue-600' : 'bg-transparent'
                      }`}
                      role="img"
                      aria-label={isUnread && !isPendingRead ? 'Unread' : 'Read'}
                    />
                    <div className="min-w-0 flex-1">
                      <div className="text-sm">
                        <span className="font-mono">
                          {youAreActor ? 'You' : e.actorUserId}
                        </span>{' '}
                        <span className="text-muted-foreground">{verbLabel(e.verb)}</span>{' '}
                        <span className="font-mono">{objectLabel(e.objectType, e.objectId)}</span>
                        {e.subjectId && (
                          <>
                            {' '}
                            <span className="text-muted-foreground">on behalf of</span>{' '}
                            <span className="font-mono">
                              {/* R44 iter2 (P1-F7): caller-as-subject
                                   gets 'you' to match the actor handling.
                                   Verbatim ID exposure of delegation
                                   partners (P2-F14) is still surfaced
                                   to peers in this scaffold; backend DTO
                                   scoping is the proper closure (deferred). */}
                              {youAreSubject ? 'you' : e.subjectId}
                            </span>
                          </>
                        )}
                      </div>
                      <div className="mt-0.5 text-xs text-muted-foreground">
                        {timeAgo(e.createdAt, now)}
                        {isPendingRead
                          ? // R44 iter2 (P2-F12 CRITICAL) + iter3 (N3):
                            // no fabricated time. Pending state shows
                            // "marking read…" with no timestamp until
                            // the cache invalidation completes and the
                            // backend's authoritative readAt arrives.
                            ' · marking read…'
                          : !isUnread &&
                            e.readAt &&
                            ` · read ${timeAgo(e.readAt, now)}`}
                      </div>
                    </div>
                    {/* R44 iter2 (P1-F2 + P1-F13): explicit Mark-read
                         action — row click no longer mutates state. The
                         button is keyboard-focusable + has an aria-label.
                         iter3 (N3): per-row pending state from the typed
                         Set so the button disables only on its own row,
                         not across the whole list. */}
                    {isUnread && (
                      <button
                        type="button"
                        className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-muted disabled:opacity-50"
                        disabled={isPendingRead}
                        aria-label={`Mark activity ${e.id} as read`}
                        onClick={() => read.mutate(e.id)}
                      >
                        {isPendingRead ? 'Marking…' : 'Mark read'}
                      </button>
                    )}
                  </li>
                )
              })}
            </ul>
            <Pagination
              page={data.page}
              totalPages={Math.max(1, Math.ceil(data.totalElements / data.size))}
              onPageChange={updatePage}
            />
          </>
        )}
      </div>
    </ErrorBoundary>
  )
}
