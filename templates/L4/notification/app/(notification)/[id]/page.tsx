/*
---
template_id: L4/notification/app/(notification)/[id]/page
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: markNotificationRead
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — DETAIL page. Loads single notification via getNotification; auto marks-read on mount; offers dismiss (DELETE) action navigating back to inbox."
  - source_type: external
    citation: "TanStack Query v5 — useQuery + useMutation for data and mutations"
    url: "https://tanstack.com/query/latest/docs/framework/react/guides/mutations"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter, useParams } from 'next/navigation'
import type { NotificationItemData } from 'templates/L2/blocks/notification-item'

// ─── API helpers ─────────────────────────────────────────────────────────────

async function fetchNotification(id: string): Promise<NotificationItemData> {
  const res = await fetch(`/api/notifications/${id}`, {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) throw new Error(`Notification not found: ${res.status}`)
  return res.json() as Promise<NotificationItemData>
}

async function markRead(id: string): Promise<void> {
  const res = await fetch(`/api/notifications/${id}/read`, { method: 'PATCH' })
  if (!res.ok) throw new Error(`markRead failed: ${res.status}`)
}

async function dismissNotification(id: string): Promise<void> {
  const res = await fetch(`/api/notifications/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`dismiss failed: ${res.status}`)
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * NotificationDetailPage — single notification view.
 *
 * Auto mark-read on mount (UX policy: reading = marking read).
 * Dismiss action: DELETE → soft-delete → navigate back to /inbox.
 *
 * Backend bindings:
 *   GET   /api/notifications/{id}      → getNotification
 *   PATCH /api/notifications/{id}/read → markNotificationRead (auto on mount)
 *   DELETE /api/notifications/{id}     → dismissNotification
 *
 * UX policy (notification-ui-manifest.yaml#detail):
 *   - auto_mark_read: true — PATCH on mount; idempotent so safe even if already READ.
 *   - dismiss returns to /inbox.
 *   - 404 on getNotification → redirect /inbox (notification dismissed elsewhere).
 *
 * Fork instructions:
 *   1. Replace useParams with your routing library's equivalent.
 *   2. Add ConfirmDialog (L2 confirm-dialog) before dismissing if preferred.
 *   3. Wire actionUrl to render a CTA button linking to the notification's target.
 *   4. Add toast feedback for mark-read errors (ToastQueue, L2 toast-queue).
 */
export default function NotificationDetailPage() {
  const router = useRouter()
  const params = useParams<{ id: string }>()
  const id = params.id
  const queryClient = useQueryClient()

  const { data: notification, isLoading, isError } = useQuery<NotificationItemData>({
    queryKey: ['notification', id],
    queryFn: () => fetchNotification(id),
    retry: false,  // Don't retry 404 — notification may be dismissed
  })

  // Auto mark-read on mount (idempotent — safe even if already READ)
  const markReadMutation = useMutation({ mutationFn: markRead })
  const dismissMutation = useMutation({
    mutationFn: dismissNotification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      router.push('/inbox')
    },
  })

  React.useEffect(() => {
    if (id && notification?.status === 'UNREAD') {
      markReadMutation.mutate(id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, notification?.status])

  // 404: notification may have been dismissed elsewhere → back to inbox
  React.useEffect(() => {
    if (isError) {
      router.replace('/inbox')
    }
  }, [isError, router])

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

  const TYPE_LABELS: Record<string, string> = {
    SYSTEM:    'System',
    ALERT:     'Alert',
    REMINDER:  'Reminder',
    PROMOTION: 'Promotion',
    ACCOUNT:   'Account',
  }

  return (
    <div className="mx-auto max-w-2xl py-6 px-4">
      {/* Back navigation */}
      <button
        type="button"
        onClick={() => router.push('/inbox')}
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
            onClick={() => dismissMutation.mutate(id)}
            disabled={dismissMutation.isPending}
            className="text-xs text-destructive hover:underline disabled:opacity-50"
            aria-label="Dismiss this notification"
          >
            {dismissMutation.isPending ? 'Dismissing…' : 'Dismiss'}
          </button>
        </div>

        {dismissMutation.isError && (
          <p role="alert" className="mt-2 text-xs text-destructive">
            Failed to dismiss. Please try again.
          </p>
        )}
      </article>
    </div>
  )
}
