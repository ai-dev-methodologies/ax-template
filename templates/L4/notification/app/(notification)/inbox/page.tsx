/*
---
template_id: L4/notification/app/(notification)/inbox/page
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: listNotifications
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — INBOX page composing L2 NotificationList + VirtualizedTable for paginated notifications. Supports UNREAD/READ/ALL filter tab; reads X-Unread-Count header."
  - source_type: external
    citation: "TanStack Query v5 — useQuery for server-state data fetching"
    url: "https://tanstack.com/query/latest/docs/framework/react/reference/useQuery"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
'use client'

import * as React from 'react'
import { useRouter } from 'next/navigation'
// NotificationList internally composes VirtualizedTable (SP15) for DOM-efficient rendering
import NotificationList from 'templates/L2/blocks/notification-list'

/**
 * NotificationInboxPage — paginated notification inbox.
 *
 * Composes:
 *   L2 NotificationList  → complete inbox block (filter + virtualized list + mutations)
 *   L2 VirtualizedTable  → used by NotificationList for DOM-efficient rendering
 *
 * Backend binding:
 *   GET /api/notifications?status=UNREAD|READ|ALL&page=0&size=50 → listNotifications
 *   X-Unread-Count header → displayed in sidebar badge + bell
 *
 * Mutation bindings (handled inside NotificationList):
 *   PATCH /api/notifications/{id}/read → markNotificationRead
 *   DELETE /api/notifications/{id}     → dismissNotification
 *
 * Fork instructions:
 *   1. Adjust containerHeight to fit your layout's available vertical space.
 *   2. Replace useRouter().push with your routing solution if not using Next.js App Router.
 *   3. Add keyboard shortcut (e.g., `j`/`k`) for navigating between items.
 *   4. Connect NotificationBell (in layout) to react-query cache for live unread update.
 */
export default function NotificationInboxPage() {
  const router = useRouter()

  const handleNotificationClick = React.useCallback(
    (id: string) => {
      router.push(`/${id}`)
    },
    [router]
  )

  return (
    <div className="mx-auto max-w-2xl py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Inbox</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Your recent notifications
        </p>
      </div>

      <NotificationList
        defaultFilter="UNREAD"
        containerHeight={560}
        onNotificationClick={handleNotificationClick}
      />
    </div>
  )
}
