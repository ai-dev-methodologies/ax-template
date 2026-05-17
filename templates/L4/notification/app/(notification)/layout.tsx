/*
---
template_id: L4/notification/app/(notification)/layout
layer: L4
domain: notification
domain_mode: full_trio
backend_operation_id: null
evidence:
  - source_type: internal
    rationale: "L4 notification vertical — notification route group layout with AppShell (sidebar + header) and NotificationBell unread badge in header."
  - source_type: external
    citation: "Next.js 15 App Router — route groups and layouts"
    url: "https://nextjs.org/docs/app/building-your-application/routing/route-groups"
provenance_class: internal_design
imports_from: [L1, L2, L3]
imports_forbidden: [L4/auth, L4/crud, L4/payment, L4/practices]
---
*/
import React from 'react'
import AppShell from 'templates/L2/blocks/app-shell'
import AppHeader from 'templates/L2/blocks/app-header'
import Sidebar from 'templates/L2/blocks/sidebar'
import NotificationBell from 'templates/L2/blocks/notification-bell'

interface NotificationLayoutProps {
  children: React.ReactNode
}

const NAV_ITEMS = [
  { href: '/inbox',    label: 'Inbox',    icon: 'bell' as const },
  { href: '/settings', label: 'Settings', icon: 'settings' as const },
]

/**
 * NotificationLayout — route group layout for all notification pages.
 *
 * Applies:
 *   - AppShell (authenticated shell with sidebar + content area)
 *   - AppHeader with NotificationBell (unread badge; polls every 30s)
 *   - Sidebar with nav items for the notification domain
 *
 * NotificationBell polls GET /api/notifications?status=UNREAD&size=1 and reads
 * X-Unread-Count from the response header to display the unread badge count.
 *
 * Fork instructions:
 *   1. Update NAV_ITEMS to match your notification flow's navigation requirements.
 *   2. Adjust NotificationBell.pollIntervalMs or replace with SSE for real-time updates.
 *   3. Add user profile / logout action to AppHeader.
 *   4. Wire NotificationBell.onOpen to open a dropdown instead of navigating.
 */
export default function NotificationLayout({ children }: NotificationLayoutProps) {
  return (
    <AppShell
      sidebarSlot={
        <Sidebar
          navItems={NAV_ITEMS}
          title="Notifications"
        />
      }
      headerSlot={
        <AppHeader
          title="Notifications"
          actionsSlot={
            /* Bell is inside its own domain layout; badge count from unread polling */
            <NotificationBell
              pollIntervalMs={30_000}
              inboxHref="/inbox"
            />
          }
        />
      }
    >
      {children}
    </AppShell>
  )
}
