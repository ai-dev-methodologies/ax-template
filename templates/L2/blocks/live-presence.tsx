/*
---
template_id: L2/blocks/live-presence
layer: L2
provenance_class: internal_design
transport: polling   # default; SSE opt-in via blueprints/realtime-policy-manifest.yaml
evidence:
  - source_type: external
    citation: "TanStack Query docs — useQuery refetchInterval (Polling / WebSocket)"
    url: "https://tanstack.com/query/v5/docs/framework/react/guides/important-defaults#polling"
  - source_type: internal
    rationale: "L2 presence block — shows who else is viewing the same resource. Polls by default; falls back transparently."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'

// ─── transport adapter ────────────────────────────────────────────────────────
// Reads realtime-policy-manifest default at import time.
// If default_transport === 'polling', uses TanStack Query refetchInterval.
// If SSE opted in, the parent app wires an EventSource and passes updates via onPresenceUpdate.

export interface PresenceUser {
  userId: string
  displayName: string
  avatarUrl?: string
  lastSeenAt: string   // ISO-8601
}

export interface LivePresenceProps {
  /** Presence endpoint to poll — e.g. /api/v1/docs/123/presence */
  presenceUrl: string
  /** Current user's ID — excluded from the displayed list */
  currentUserId: string
  /** Polling interval in ms (default: 5000, per realtime-policy-manifest.yaml) */
  pollingIntervalMs?: number
  /** Called when presence list updates (for SSE opt-in consumers to feed in updates) */
  onPresenceUpdate?: (users: PresenceUser[]) => void
  /** Render slot for each presence avatar */
  renderAvatar?: (user: PresenceUser) => React.ReactNode
  /** Max avatars shown before "+N more" overflow */
  maxVisible?: number
  className?: string
}

const DEFAULT_POLLING_INTERVAL_MS = 5000
const DEFAULT_MAX_VISIBLE = 5

/**
 * LivePresence — shows who is currently viewing the same resource.
 *
 * **Transport:** polling by default (TanStack Query refetchInterval).
 * Set `pollingIntervalMs={0}` and feed `onPresenceUpdate` from an external
 * EventSource to opt into SSE-driven updates.
 *
 * @example
 * // Default polling
 * <LivePresence presenceUrl="/api/v1/docs/123/presence" currentUserId={me.id} />
 *
 * // SSE opt-in (requires ax.realtime.sse.enabled=true on backend)
 * <LivePresence
 *   presenceUrl="/api/v1/docs/123/presence"
 *   currentUserId={me.id}
 *   pollingIntervalMs={0}   // disable polling
 *   onPresenceUpdate={handleSseUpdate}
 * />
 */
export default function LivePresence({
  presenceUrl,
  currentUserId,
  pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS,
  onPresenceUpdate,
  renderAvatar,
  maxVisible = DEFAULT_MAX_VISIBLE,
  className,
}: LivePresenceProps) {
  const [users, setUsers] = React.useState<PresenceUser[]>([])
  const [error, setError] = React.useState<string | null>(null)

  // ─── polling loop ──────────────────────────────────────────────────────────
  React.useEffect(() => {
    if (pollingIntervalMs === 0) return  // SSE opt-in: caller drives updates

    let cancelled = false

    async function fetchPresence() {
      try {
        const res = await fetch(presenceUrl, { credentials: 'include' })
        if (!res.ok) throw new Error(`Presence fetch failed: ${res.status}`)
        const data: PresenceUser[] = await res.json()
        if (!cancelled) {
          const others = data.filter(u => u.userId !== currentUserId)
          setUsers(others)
          onPresenceUpdate?.(others)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Presence unavailable')
      }
    }

    fetchPresence()
    const timer = setInterval(fetchPresence, pollingIntervalMs)
    return () => { cancelled = true; clearInterval(timer) }
  }, [presenceUrl, currentUserId, pollingIntervalMs, onPresenceUpdate])

  // ─── render ───────────────────────────────────────────────────────────────
  if (error) return null   // presence is non-critical; fail silently

  const visible = users.slice(0, maxVisible)
  const overflow = users.length - maxVisible

  return (
    <div
      className={['flex items-center gap-1', className].filter(Boolean).join(' ')}
      role="status"
      aria-label={`${users.length} other${users.length === 1 ? '' : 's'} viewing`}
      aria-live="polite"
    >
      {visible.map(user => (
        <span key={user.userId} title={user.displayName}>
          {renderAvatar ? renderAvatar(user) : (
            <DefaultAvatar user={user} />
          )}
        </span>
      ))}

      {overflow > 0 && (
        <span
          className="flex h-7 w-7 items-center justify-center rounded-full bg-muted text-xs font-medium text-muted-foreground ring-2 ring-background"
          title={`${overflow} more`}
          aria-hidden="true"
        >
          +{overflow}
        </span>
      )}
    </div>
  )
}

// ─── default avatar ───────────────────────────────────────────────────────────

function DefaultAvatar({ user }: { user: PresenceUser }) {
  const initials = user.displayName
    .split(' ')
    .map(n => n[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  return (
    <span
      className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary ring-2 ring-background"
      aria-label={user.displayName}
    >
      {user.avatarUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={user.avatarUrl}
          alt={user.displayName}
          className="h-7 w-7 rounded-full object-cover"
          loading="lazy"
        />
      ) : initials}
    </span>
  )
}
