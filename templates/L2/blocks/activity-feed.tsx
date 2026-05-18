/*
---
template_id: L2/blocks/activity-feed
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA 1.2 — feed role: The feed role is a specialized form of the list role for feeds that allow users to browse, read, and interact with content from a scrollable list."
    url: "https://www.w3.org/TR/wai-aria-1.2/#feed"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Generic chronological activity feed used in admin dashboards. Receives a typed event array and renders a timeline with avatar, actor, action, and timestamp. No domain-specific logic — L4 supplies the events."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
import * as React from 'react'

export interface ActivityEvent {
  /** Unique identifier for the event. */
  id: string
  /** Actor display name (user, system, etc.). */
  actor: string
  /** Short description of the action performed. */
  action: string
  /** Optional detail text or secondary line. */
  detail?: string
  /** ISO 8601 timestamp string. */
  timestamp: string
  /** Optional avatar URL. Falls back to initials. */
  avatarUrl?: string
  /** Optional icon/badge shown alongside the actor (e.g. ✅, 🛠). */
  icon?: string
}

export interface ActivityFeedProps {
  /** Ordered list of events (most recent first). */
  events: ActivityEvent[]
  /** Maximum number of events to show (default: all). */
  limit?: number
  /** Label for the feed region (default: "Recent activity"). */
  label?: string
  /** Empty state message when events is empty (default: "No recent activity."). */
  emptyMessage?: string
  /** Custom class name for the container. */
  className?: string
}

function formatRelative(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  return `${days}d ago`
}

function Initials({ name }: { name: string }) {
  const parts = name.trim().split(/\s+/)
  const letters = parts.length >= 2
    ? `${parts[0][0]}${parts[parts.length - 1][0]}`
    : name.slice(0, 2)
  return (
    <span
      aria-hidden="true"
      className="flex h-8 w-8 items-center justify-center rounded-full bg-muted text-xs font-semibold text-muted-foreground uppercase"
    >
      {letters.toUpperCase()}
    </span>
  )
}

/**
 * ActivityFeed — chronological event timeline for admin dashboards.
 *
 * L4 supplies the typed `events` array; this component renders the timeline.
 *
 * ```tsx
 * import ActivityFeed from 'templates/L2/blocks/activity-feed'
 *
 * <ActivityFeed
 *   events={auditEvents.map(e => ({
 *     id: e.id,
 *     actor: e.userEmail,
 *     action: e.eventType,
 *     detail: e.resourceId,
 *     timestamp: e.createdAt,
 *   }))}
 *   limit={20}
 * />
 * ```
 */
export default function ActivityFeed({
  events,
  limit,
  label = 'Recent activity',
  emptyMessage = 'No recent activity.',
  className,
}: ActivityFeedProps) {
  const visible = limit != null ? events.slice(0, limit) : events

  return (
    <section
      aria-label={label}
      role="feed"
      data-testid="activity-feed"
      className={['space-y-4', className ?? ''].filter(Boolean).join(' ')}
    >
      {visible.length === 0 ? (
        <p className="text-sm text-muted-foreground">{emptyMessage}</p>
      ) : (
        <ol className="space-y-3">
          {visible.map((event) => (
            <li
              key={event.id}
              aria-label={`${event.actor} ${event.action} — ${formatRelative(event.timestamp)}`}
              className="flex items-start gap-3"
            >
              {/* Avatar */}
              <div className="shrink-0 mt-0.5">
                {event.avatarUrl ? (
                  <img
                    src={event.avatarUrl}
                    alt={event.actor}
                    className="h-8 w-8 rounded-full object-cover"
                    loading="lazy"
                  />
                ) : (
                  <Initials name={event.actor} />
                )}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <p className="text-sm leading-snug">
                  {event.icon && (
                    <span aria-hidden="true" className="mr-1">
                      {event.icon}
                    </span>
                  )}
                  <span className="font-medium">{event.actor}</span>
                  {' '}
                  <span>{event.action}</span>
                </p>
                {event.detail && (
                  <p className="text-xs text-muted-foreground mt-0.5 truncate">
                    {event.detail}
                  </p>
                )}
              </div>

              {/* Timestamp */}
              <time
                dateTime={event.timestamp}
                className="shrink-0 text-xs text-muted-foreground"
                title={new Date(event.timestamp).toLocaleString('ko-KR')}
              >
                {formatRelative(event.timestamp)}
              </time>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
