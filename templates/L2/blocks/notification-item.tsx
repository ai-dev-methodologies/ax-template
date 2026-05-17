/*
---
template_id: L2/blocks/notification-item
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WCAG 2.2 — 1.3.1 Info and Relationships: information conveyed through presentation must also be available in text"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
  - source_type: external
    citation: "WCAG 2.2 — 2.4.6 Headings and Labels: headings and labels describe topic or purpose"
    url: "https://www.w3.org/TR/WCAG22/#headings-and-labels"
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
import * as React from 'react'
import { cn } from 'templates/L1/lib/utils'

// ─── types ────────────────────────────────────────────────────────────────────

export type NotificationType = 'SYSTEM' | 'ALERT' | 'REMINDER' | 'PROMOTION' | 'ACCOUNT'
export type NotificationStatus = 'UNREAD' | 'READ'

export interface NotificationItemData {
  id: string
  type: NotificationType
  title: string
  body: string
  status: NotificationStatus
  actionUrl?: string | null
  createdAt: string
}

export interface NotificationItemProps {
  notification: NotificationItemData
  /** Called when the item is clicked (e.g., navigate to detail page). */
  onClick?: (id: string) => void
  /** Called when the dismiss (delete) button is clicked. */
  onDismiss?: (id: string) => void
  /** Called when the mark-read button is clicked. */
  onMarkRead?: (id: string) => void
  className?: string
}

// ─── type badge ───────────────────────────────────────────────────────────────

const TYPE_STYLES: Record<NotificationType, { dot: string; label: string }> = {
  SYSTEM:    { dot: 'bg-blue-500',   label: 'System' },
  ALERT:     { dot: 'bg-red-500',    label: 'Alert' },
  REMINDER:  { dot: 'bg-yellow-500', label: 'Reminder' },
  PROMOTION: { dot: 'bg-purple-500', label: 'Promotion' },
  ACCOUNT:   { dot: 'bg-green-500',  label: 'Account' },
}

// ─── time formatter ────────────────────────────────────────────────────────────

function formatRelativeTime(isoDate: string): string {
  const now = Date.now()
  const date = new Date(isoDate).getTime()
  const diff = now - date

  if (diff < 60_000) return 'Just now'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`
  return new Date(isoDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * NotificationItem — single notification card for use in inbox lists.
 *
 * Displays:
 *   - Unread dot indicator (left accent)
 *   - Type badge (SYSTEM / ALERT / REMINDER / PROMOTION / ACCOUNT)
 *   - Title + body truncated with ellipsis
 *   - Relative timestamp
 *   - Action buttons: Mark Read, Dismiss
 *
 * Fork instructions:
 *   1. Compose inside NotificationList (L2 notification-list block).
 *   2. Pass `onClick` to navigate to /(notification)/[id] detail page.
 *   3. Pass `onDismiss` to call DELETE /api/notifications/{id}.
 *   4. Pass `onMarkRead` to call PATCH /api/notifications/{id}/read.
 *   5. Wrap in VirtualizedTable rows when rendering large notification lists.
 */
export default function NotificationItem({
  notification,
  onClick,
  onDismiss,
  onMarkRead,
  className,
}: NotificationItemProps) {
  const isUnread = notification.status === 'UNREAD'
  const { dot, label: typeLabel } = TYPE_STYLES[notification.type] ?? TYPE_STYLES.SYSTEM

  const handleCardClick = () => {
    onClick?.(notification.id)
  }

  const handleDismiss = (e: React.MouseEvent) => {
    e.stopPropagation()
    onDismiss?.(notification.id)
  }

  const handleMarkRead = (e: React.MouseEvent) => {
    e.stopPropagation()
    onMarkRead?.(notification.id)
  }

  return (
    <article
      className={cn(
        'group relative flex gap-3 rounded-lg border p-4 transition-colors',
        isUnread
          ? 'border-border bg-accent/40 hover:bg-accent/60'
          : 'border-transparent bg-muted/20 hover:bg-muted/40',
        onClick && 'cursor-pointer',
        className
      )}
      onClick={onClick ? handleCardClick : undefined}
      aria-label={`${isUnread ? 'Unread: ' : ''}${notification.title}`}
    >
      {/* Unread indicator */}
      {isUnread && (
        <span
          aria-hidden="true"
          className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-blue-500"
        />
      )}

      <div className={cn('flex-1 min-w-0', !isUnread && 'pl-5')}>
        {/* Header row: type badge + time */}
        <div className="flex items-center justify-between gap-2 mb-1">
          <span className="inline-flex items-center gap-1.5">
            <span aria-hidden="true" className={cn('h-1.5 w-1.5 rounded-full', dot)} />
            <span className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
              {typeLabel}
            </span>
          </span>
          <time
            dateTime={notification.createdAt}
            className="text-xs text-muted-foreground shrink-0"
          >
            {formatRelativeTime(notification.createdAt)}
          </time>
        </div>

        {/* Title */}
        <p
          className={cn(
            'text-sm truncate',
            isUnread ? 'font-semibold text-foreground' : 'font-medium text-muted-foreground'
          )}
        >
          {notification.title}
        </p>

        {/* Body */}
        <p className="mt-0.5 text-xs text-muted-foreground line-clamp-2">
          {notification.body}
        </p>

        {/* Action buttons */}
        <div className="mt-2 flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
          {isUnread && onMarkRead && (
            <button
              type="button"
              onClick={handleMarkRead}
              className="text-xs text-primary hover:underline focus-visible:underline"
              aria-label={`Mark "${notification.title}" as read`}
            >
              Mark read
            </button>
          )}
          {onDismiss && (
            <button
              type="button"
              onClick={handleDismiss}
              className="text-xs text-destructive hover:underline focus-visible:underline"
              aria-label={`Dismiss "${notification.title}"`}
            >
              Dismiss
            </button>
          )}
        </div>
      </div>
    </article>
  )
}
