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
import NotificationDetailView from './notification-detail-view'

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

  return (
    <NotificationDetailView
      isLoading={isLoading}
      notification={notification}
      onBack={() => router.push('/inbox')}
      onDismiss={(dismissId) => dismissMutation.mutate(dismissId)}
      dismissPending={dismissMutation.isPending}
      dismissIsError={dismissMutation.isError}
    />
  )
}
