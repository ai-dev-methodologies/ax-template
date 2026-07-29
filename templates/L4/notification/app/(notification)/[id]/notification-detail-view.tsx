/*
---
template_id: L4/notification/app/(notification)/[id]/notification-detail-view
layer: L4
domain: notification
domain_mode: full_trio
evidence:
  - source_type: internal
    rationale: "Pure presentational extraction from (notification)/[id]/page.tsx (BACKLOG P2-42
      render-testability pass-1 closure — same class as (crud)/items/[id]/item-detail-view.tsx):
      the page's data-fetch/mutation orchestration (useQuery/useMutation/useRouter/useParams) is a
      hard dependency-resolution boundary for a vitest that imports this file directly from
      outside frontend/ — the @tanstack/react-query bare specifier does not resolve for a module
      living in templates/L4/... (see frontend/tests/audit-log-redaction-render.vitest.tsx's own
      note on the same class of gap). Only the `NotificationItemData` TYPE is imported from
      templates/L2/blocks/notification-item (type-only import, erased at compile time)."
---
*/
import * as React from 'react'
import type { NotificationItemData } from 'templates/L2/blocks/notification-item'

// ─── constants ──────────────────────────────────────────────────────────────

const TYPE_LABELS: Record<string, string> = {
  SYSTEM: 'System',
  ALERT: 'Alert',
  REMINDER: 'Reminder',
  PROMOTION: 'Promotion',
  ACCOUNT: 'Account',
}

// ─── component ──────────────────────────────────────────────────────────────

export interface NotificationDetailViewProps {
  isLoading: boolean
  notification: NotificationItemData | undefined
  onBack: () => void
  onDismiss: (id: string) => void
  dismissPending: boolean
  dismissIsError: boolean
}

/**
 * NotificationDetailView — pure presentational render of a single notification.
 *
 * Deliberately has ZERO data-fetching/mutation dependencies (no useQuery/useMutation) — the
 * caller (`(notification)/[id]/page.tsx`) owns all query/mutation state (including the
 * auto-mark-read-on-mount effect and the 404->redirect effect) and passes the resolved
 * `notification` + mutation-trigger callbacks in. This keeps the component a plain props -> JSX
 * function, which is what makes it renderable in a unit test without a QueryClientProvider.
 */
export default function NotificationDetailView({
  isLoading,
  notification,
  onBack,
  onDismiss,
  dismissPending,
  dismissIsError,
}: NotificationDetailViewProps) {
  if (isLoading) {
    return (
      <div className="mx-auto max-w-2xl py-6 px-4" aria-busy="true">
        <div className="h-6 w-32 animate-pulse rounded bg-muted mb-4" />
        <div className="space-y-3">
          <div className="h-8 w-3/4 animate-pulse rounded bg-muted" />
          <div className="h-32 animate-pulse rounded bg-muted" />
        </div>
      </div>
    )
  }

  if (!notification) return null

  return (
    <div className="mx-auto max-w-2xl py-6 px-4">
      {/* Back navigation */}
      <button
        type="button"
        onClick={onBack}
        className="mb-6 flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
        aria-label="Back to inbox"
      >
        ← Back to Inbox
      </button>

      <article aria-label={`Notification: ${notification.title}`}>
        {/* Type + time header */}
        <div className="flex items-center justify-between gap-4 mb-4">
          <span className="inline-flex items-center rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">
            {TYPE_LABELS[notification.type] ?? notification.type}
          </span>
          <time
            dateTime={notification.createdAt}
            className="text-xs text-muted-foreground"
          >
            {new Date(notification.createdAt).toLocaleString()}
          </time>
        </div>

        {/* Title */}
        <h1 className="text-xl font-semibold mb-3">{notification.title}</h1>

        {/* Body */}
        <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-wrap">
          {notification.body}
        </p>

        {/* Action URL CTA */}
        {notification.actionUrl && (
          <a
            href={notification.actionUrl}
            className="mt-6 inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
          >
            View details
          </a>
        )}

        {/* Read status */}
        <div className="mt-6 flex items-center justify-between border-t pt-4">
          <span className={`text-xs ${notification.status === 'READ' ? 'text-muted-foreground' : 'text-blue-600 font-medium'}`}>
            {notification.status === 'READ' ? 'Read' : 'Unread'}
          </span>

          {/* Dismiss action */}
          <button
            type="button"
            onClick={() => onDismiss(notification.id)}
            disabled={dismissPending}
            className="text-xs text-destructive hover:underline disabled:opacity-50"
            aria-label="Dismiss this notification"
          >
            {dismissPending ? 'Dismissing…' : 'Dismiss'}
          </button>
        </div>

        {dismissIsError && (
          <p role="alert" className="mt-2 text-xs text-destructive">
            Failed to dismiss. Please try again.
          </p>
        )}
      </article>
    </div>
  )
}
