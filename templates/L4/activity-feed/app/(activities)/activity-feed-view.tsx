/*
---
template_id: L4/activity-feed/app/(activities)/activity-feed-view
layer: L4
domain: activity-feed
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (activities)/page.tsx (BACKLOG P2-42
      render-testability pass-2 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useQueryClient/
      useRouter/useSearchParams) is a hard dependency-resolution boundary for a vitest that
      imports this file directly from outside frontend/ — the @tanstack/react-query bare
      specifier does not resolve for a module living in templates/L4/... (see
      frontend/tests/audit-log-redaction-render.vitest.tsx's own note on the same class of gap).
      Only the `sameUser` comparator is imported from templates/L0/fork-receiver-kit/use-caller-id
      (a pure string-equality helper, not the useCallerId hook itself, which stays on the page).
      templates/L2/blocks/{empty-state,error-boundary,pagination} have zero external-npm deps."
---
*/
import * as React from 'react'
import EmptyState from 'templates/L2/blocks/empty-state'
import ErrorBoundary from 'templates/L2/blocks/error-boundary'
import Pagination from 'templates/L2/blocks/pagination'
import { sameUser } from 'templates/L0/fork-receiver-kit/use-caller-id'

// ─── types ───────────────────────────────────────────────────────────────────

export interface ActivityEvent {
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

export interface ActivityFeedResponse {
  items: ActivityEvent[]
  page: number
  size: number
  totalElements: number
}

export interface ActivityFeedViewProps {
  data: ActivityFeedResponse | undefined
  error: Error | null
  isLoading: boolean
  dataUpdatedAt: number
  callerId: string

  unread: boolean
  onToggleUnread: () => void

  onMarkAllRead: () => void
  markAllPending: boolean

  readErrorMessage: string | null
  onDismissReadError: () => void
  markAllErrorMessage: string | null
  onDismissMarkAllError: () => void

  pendingReadIds: Set<string>
  onMarkRead: (id: string) => void

  onPageChange: (page: number) => void
}

// ─── helpers ──────────────────────────────────────────────────────────────────

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

// ─── component ──────────────────────────────────────────────────────────────

/**
 * ActivityFeedView — pure presentational render of the caller's activity inbox.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(activities)/page.tsx`) owns all query/mutation state and passes the resolved `data`,
 * pending-id set, and mutation-trigger callbacks in. This keeps the component a plain
 * props -> JSX function, which is what makes it renderable in a unit test without a
 * QueryClientProvider. Owns its own local `now` ticker (pure presentational timeAgo refresh, not
 * a data-fetching concern) — same precedent as favorites-list-view's local `now` state.
 */
export default function ActivityFeedView({
  data,
  error,
  isLoading,
  dataUpdatedAt,
  callerId,
  unread,
  onToggleUnread,
  onMarkAllRead,
  markAllPending,
  readErrorMessage,
  onDismissReadError,
  markAllErrorMessage,
  onDismissMarkAllError,
  pendingReadIds,
  onMarkRead,
  onPageChange,
}: ActivityFeedViewProps) {
  const [now, setNow] = React.useState(() => new Date())
  React.useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 60_000)
    return () => clearInterval(t)
  }, [])

  const handleMarkAllRead = () => {
    if (
      !window.confirm(
        'Mark every unread activity on your account as read?\n\nThis cannot be undone from this UI and includes items not visible on the current page.',
      )
    ) {
      return
    }
    onMarkAllRead()
  }

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
            <p className="text-xs text-muted-foreground" aria-live="polite">
              {dataUpdatedAt ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}` : ''}
            </p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50"
              onClick={onToggleUnread}
            >
              {unread ? 'Show all' : 'Show unread only'}
            </button>
            <button
              type="button"
              className="rounded border px-3 py-1.5 text-sm hover:bg-muted disabled:opacity-50 aria-busy:opacity-60"
              disabled={markAllPending}
              aria-busy={markAllPending || undefined}
              onClick={handleMarkAllRead}
              title="Marks all unread activity on your account — confirm required"
            >
              Mark all read
            </button>
          </div>
        </header>

        {readErrorMessage && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Mark read failed: {readErrorMessage}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={onDismissReadError}
            >
              Dismiss
            </button>
          </div>
        )}
        {markAllErrorMessage && (
          <div
            role="alert"
            className="flex items-start justify-between gap-3 rounded border border-red-300 bg-red-50 px-3 py-1.5 text-sm text-red-900"
          >
            <span>Mark all read failed: {markAllErrorMessage}</span>
            <button
              type="button"
              className="shrink-0 text-xs underline"
              onClick={onDismissMarkAllError}
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
          <EmptyState title="Failed to load activity" description={error.message} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            title={unread ? 'No unread activity' : 'No activity yet'}
            description={
              unread
                ? 'You are caught up. Switch to "Show all" to see read activity.'
                : 'When someone interacts with content that involves you, it will show up here.'
            }
            actionSlot={
              unread ? (
                <button
                  type="button"
                  className="rounded border bg-primary px-3 py-1.5 text-sm text-primary-foreground hover:opacity-90"
                  onClick={onToggleUnread}
                >
                  Show all
                </button>
              ) : undefined
            }
          />
        ) : (
          <>
            <ul className="divide-y rounded border">
              {data.items.map((e) => {
                const isUnread = e.readAt === null
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
                              {youAreSubject ? 'you' : e.subjectId}
                            </span>
                          </>
                        )}
                      </div>
                      <div className="mt-0.5 text-xs text-muted-foreground">
                        {timeAgo(e.createdAt, now)}
                        {isPendingRead
                          ? ' · marking read…'
                          : !isUnread && e.readAt && ` · read ${timeAgo(e.readAt, now)}`}
                      </div>
                    </div>
                    {isUnread && (
                      <button
                        type="button"
                        className="shrink-0 rounded border px-2 py-1 text-xs hover:bg-muted disabled:opacity-50 aria-busy:opacity-60"
                        disabled={isPendingRead}
                        aria-busy={isPendingRead || undefined}
                        aria-label={`Mark activity ${e.id} as read`}
                        onClick={() => onMarkRead(e.id)}
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
              onPageChange={onPageChange}
            />
          </>
        )}
      </div>
    </ErrorBoundary>
  )
}
