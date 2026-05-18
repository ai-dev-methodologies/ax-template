/*
---
template_id: L2/blocks/event-stream
layer: L2
provenance_class: internal_design
transport: polling   # default; SSE opt-in via blueprints/realtime-policy-manifest.yaml
evidence:
  - source_type: external
    citation: "WHATWG — Server-Sent Events: EventSource interface"
    url: "https://html.spec.whatwg.org/multipage/server-sent-events.html#the-eventsource-interface"
  - source_type: external
    citation: "TanStack Query docs — useQuery refetchInterval (Polling / WebSocket)"
    url: "https://tanstack.com/query/v5/docs/framework/react/guides/important-defaults#polling"
  - source_type: internal
    rationale: "L2 event stream block — transport-adaptive: polls by default, connects EventSource when SSE opted in."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface StreamEvent {
  id?: string
  type: string
  data: string
  timestamp: number
}

export type EventStreamTransport = 'polling' | 'sse'

export interface EventStreamProps {
  /**
   * Polling endpoint (always used in polling mode).
   * SSE mode connects EventSource to `sseUrl` if provided, falls back to `pollUrl`.
   */
  pollUrl: string
  /** SSE endpoint — only used when transport='sse' AND ax.realtime.sse.enabled=true on backend */
  sseUrl?: string
  /** Transport mode. Default 'polling'. Set 'sse' only with backend opt-in. */
  transport?: EventStreamTransport
  /** Polling interval in ms (default: 5000). Ignored when transport='sse'. */
  pollingIntervalMs?: number
  /** Max events to retain in the display buffer */
  maxEvents?: number
  /** Called on each new event (polling batch or SSE single event) */
  onEvent?: (event: StreamEvent) => void
  /** Render an individual event row */
  renderEvent?: (event: StreamEvent) => React.ReactNode
  /** Empty state content */
  emptyContent?: React.ReactNode
  className?: string
}

const DEFAULT_POLLING_INTERVAL_MS = 5000
const DEFAULT_MAX_EVENTS = 50

/**
 * EventStream — real-time event feed with adaptive transport.
 *
 * **Default (polling):** fetches `pollUrl` on a `pollingIntervalMs` interval.
 * No `EventSource` is instantiated — safe for serverless environments.
 *
 * **SSE opt-in:** pass `transport="sse"` and `sseUrl`. Connects `EventSource`
 * to `sseUrl`. Requires `ax.realtime.sse.enabled=true` on the backend and a
 * persistent runtime (not serverless).
 *
 * The TDD fixture `realtime-default-polling.spec.ts` asserts that no `EventSource`
 * is instantiated when `transport` is not explicitly set to `'sse'`.
 *
 * @example
 * // Default polling (serverless-safe)
 * <EventStream pollUrl="/api/v1/audit/stream" />
 *
 * // SSE opt-in (persistent runtime only)
 * <EventStream
 *   pollUrl="/api/v1/audit/stream"
 *   sseUrl="/api/v1/events?topic=audit"
 *   transport="sse"
 * />
 */
export default function EventStream({
  pollUrl,
  sseUrl,
  transport = 'polling',
  pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS,
  maxEvents = DEFAULT_MAX_EVENTS,
  onEvent,
  renderEvent,
  emptyContent,
  className,
}: EventStreamProps) {
  const [events, setEvents] = React.useState<StreamEvent[]>([])
  const [status, setStatus] = React.useState<'idle' | 'connected' | 'error'>('idle')

  // ─── polling transport (default) ───────────────────────────────────────────
  React.useEffect(() => {
    if (transport !== 'polling') return

    let cancelled = false

    async function poll() {
      try {
        const res = await fetch(pollUrl, { credentials: 'include' })
        if (!res.ok) { setStatus('error'); return }
        const data: StreamEvent[] = await res.json()
        if (!cancelled) {
          setStatus('connected')
          appendEvents(data)
        }
      } catch {
        if (!cancelled) setStatus('error')
      }
    }

    poll()
    const timer = setInterval(poll, pollingIntervalMs)
    return () => { cancelled = true; clearInterval(timer) }
  }, [transport, pollUrl, pollingIntervalMs])

  // ─── SSE transport (opt-in) ────────────────────────────────────────────────
  React.useEffect(() => {
    if (transport !== 'sse') return
    const url = sseUrl ?? pollUrl
    const source = new EventSource(url, { withCredentials: true })

    source.onopen = () => setStatus('connected')
    source.onerror = () => setStatus('error')
    source.onmessage = (e) => {
      const event: StreamEvent = {
        type: e.type,
        data: e.data,
        timestamp: Date.now(),
        id: e.lastEventId || undefined,
      }
      appendEvents([event])
    }

    return () => source.close()
  }, [transport, sseUrl, pollUrl])

  // ─── shared helpers ────────────────────────────────────────────────────────

  function appendEvents(incoming: StreamEvent[]) {
    setEvents(prev => {
      const merged = [...incoming, ...prev].slice(0, maxEvents)
      incoming.forEach(e => onEvent?.(e))
      return merged
    })
  }

  // ─── render ───────────────────────────────────────────────────────────────

  return (
    <div className={className}>
      {/* Connection status badge */}
      <div className="mb-2 flex items-center gap-2">
        <span
          className={[
            'inline-block h-2 w-2 rounded-full',
            status === 'connected' ? 'bg-green-500' :
            status === 'error'     ? 'bg-red-500'   :
                                     'bg-muted',
          ].join(' ')}
          aria-hidden="true"
        />
        <span className="text-xs text-muted-foreground">
          {status === 'connected' ? `Live · ${transport}` :
           status === 'error'     ? 'Disconnected'         :
                                    'Connecting…'}
        </span>
      </div>

      {/* Event list */}
      {events.length === 0 ? (
        <div className="text-sm text-muted-foreground">
          {emptyContent ?? 'No events yet.'}
        </div>
      ) : (
        <ol className="space-y-1" aria-label="Event stream" aria-live="polite">
          {events.map((event, i) => (
            <li key={event.id ?? `${event.timestamp}-${i}`}>
              {renderEvent ? renderEvent(event) : <DefaultEventRow event={event} />}
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}

// ─── default event row ────────────────────────────────────────────────────────

function DefaultEventRow({ event }: { event: StreamEvent }) {
  return (
    <div className="flex items-start gap-2 rounded-md border border-border px-3 py-2 text-sm">
      <span className="mt-0.5 shrink-0 rounded bg-primary/10 px-1 py-0.5 text-xs font-mono text-primary">
        {event.type}
      </span>
      <span className="grow truncate text-foreground">{event.data}</span>
      <time className="shrink-0 text-xs text-muted-foreground">
        {new Date(event.timestamp).toLocaleTimeString()}
      </time>
    </div>
  )
}
