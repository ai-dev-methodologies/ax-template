/*
---
template_id: L2/blocks/notification-bell
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching with refetchInterval"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
  - source_type: external
    citation: "WCAG 2.2 — 4.1.3 Status Messages: status messages must be programmatically determinable via role or property"
    url: "https://www.w3.org/TR/WCAG22/#status-messages"
dependencies: [@tanstack/react-query]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'
import { useQuery } from '@tanstack/react-query'

// ─── types ────────────────────────────────────────────────────────────────────

export interface NotificationBellProps {
  /**
   * Polling interval in milliseconds for unread count refresh.
   * Set to 0 to disable polling (use SSE / push instead).
   * @default 30000
   */
  pollIntervalMs?: number
  /** Called when the bell is clicked — typically opens the inbox or a dropdown. */
  onOpen?: () => void
  /** href to navigate to when bell is clicked (alternative to onOpen). */
  inboxHref?: string
  className?: string
}

// ─── fetcher ─────────────────────────────────────────────────────────────────

async function fetchUnreadCount(): Promise<number> {
  const res = await fetch('/api/notifications?status=UNREAD&size=1', {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) return 0
  const count = res.headers.get('X-Unread-Count')
  return count ? parseInt(count, 10) : 0
}

// ─── badge ────────────────────────────────────────────────────────────────────

interface BadgeProps {
  count: number
}

function UnreadBadge({ count }: BadgeProps) {
  if (count <= 0) return null
  const label = count > 99 ? '99+' : String(count)
  return (
    <span
      aria-hidden="true"
      className={[
        'absolute -top-1 -right-1 flex items-center justify-center',
        'min-w-[1.125rem] h-[1.125rem] rounded-full px-1',
        'bg-red-500 text-white text-[0.625rem] font-semibold leading-none',
        'ring-2 ring-background',
        'transition-transform duration-150 scale-100',
      ].join(' ')}
    >
      {label}
    </span>
  )
}

// ─── bell icon ────────────────────────────────────────────────────────────────

function BellIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </svg>
  )
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * NotificationBell — header bell button with unread count badge.
 *
 * Polls GET /api/notifications?status=UNREAD&size=1 every {@link pollIntervalMs}
 * milliseconds, reading the {@code X-Unread-Count} response header.
 *
 * Fork instructions:
 *   1. Replace polling with SSE: listen to /api/notifications/stream when available.
 *   2. Update `inboxHref` or `onOpen` to match your routing.
 *   3. Compose inside AppHeader (L2 app-header block).
 *   4. Style the button with your design system's icon-button variant.
 */
export default function NotificationBell({
  pollIntervalMs = 30_000,
  onOpen,
  inboxHref = '/inbox',
  className,
}: NotificationBellProps) {
  const { data: unreadCount = 0 } = useQuery<number>({
    queryKey: ['notification-bell-unread'],
    queryFn: fetchUnreadCount,
    refetchInterval: pollIntervalMs > 0 ? pollIntervalMs : false,
    staleTime: Math.max(0, pollIntervalMs - 5_000),
  })

  const ariaLabel =
    unreadCount > 0
      ? `Notifications (${unreadCount > 99 ? '99+' : unreadCount} unread)`
      : 'Notifications'

  const handleClick = (e: React.MouseEvent) => {
    if (onOpen) {
      e.preventDefault()
      onOpen()
    }
  }

  return (
    <a
      href={inboxHref}
      onClick={handleClick}
      aria-label={ariaLabel}
      className={[
        'relative inline-flex items-center justify-center',
        'h-9 w-9 rounded-md',
        'text-muted-foreground hover:text-foreground',
        'hover:bg-accent focus-visible:outline-none focus-visible:ring-2',
        'focus-visible:ring-ring transition-colors',
        className,
      ]
        .filter(Boolean)
        .join(' ')}
    >
      <BellIcon className="h-5 w-5" />
      <UnreadBadge count={unreadCount} />
      {/* Screen reader live region for async count changes */}
      <span
        role="status"
        aria-live="polite"
        aria-atomic="true"
        className="sr-only"
      >
        {unreadCount > 0 ? `${unreadCount} unread notifications` : 'No unread notifications'}
      </span>
    </a>
  )
}
