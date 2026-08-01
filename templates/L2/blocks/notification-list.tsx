/*
---
template_id: L2/blocks/notification-list
layer: L2
provenance_class: internal_design
evidence:
  # Re-anchored 2026-08-01 (BACKLOG P2-73): "Virtualize only what you see..." is a digest
  # sentence someone wrote; it appears nowhere in the TanStack Virtual introduction page,
  # historically or currently. PROTECTED LEDGER IDENTITY — re-anchored, not deleted. Quote
  # below is copied verbatim from the 2026-08-01 extractor output appended to the snapshot.
  - source_type: upstream_id
    upstream_id: tanstack-virtual-2026-05
    section: "Core API: useVirtualizer"
    quote: "TanStack Virtual is a headless UI utility for virtualizing long lists of elements in JS/TS, React, Vue, Svelte, Solid, Lit, and Angular."
  - source_type: external
    citation: "WCAG 2.2 — 1.3.1 Info and Relationships: lists must use appropriate list markup for assistive technology"
    url: "https://www.w3.org/TR/WCAG22/#info-and-relationships"
dependencies: ["@tanstack/react-query", "@tanstack/react-virtual"]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import VirtualizedTable from './virtualized-table'
import NotificationItem, { type NotificationItemData } from './notification-item'
import EmptyState from './empty-state'
import FilterBar, { type FilterDef } from './filter-bar'
import type { ColumnDef } from './data-table'

// ─── types ────────────────────────────────────────────────────────────────────

export type NotificationStatusFilter = 'UNREAD' | 'READ' | 'ALL'

export interface NotificationPage {
  content: NotificationItemData[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface NotificationListProps {
  /** Initial status filter applied on mount. @default 'UNREAD' */
  defaultFilter?: NotificationStatusFilter
  /** Height of the virtualized list container in px. @default 600 */
  containerHeight?: number
  /** Called when a notification card is clicked — typically navigate to detail page. */
  onNotificationClick?: (id: string) => void
  className?: string
}

// ─── API helpers ─────────────────────────────────────────────────────────────

async function fetchNotifications(
  status: NotificationStatusFilter,
  page: number,
  size: number
): Promise<{ page: NotificationPage; unreadCount: number }> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (status !== 'ALL') params.set('status', status)

  const res = await fetch(`/api/notifications?${params}`, {
    headers: { Accept: 'application/json' },
  })
  if (!res.ok) throw new Error(`Failed to fetch notifications: ${res.status}`)

  const unreadCount = parseInt(res.headers.get('X-Unread-Count') ?? '0', 10)
  const data = (await res.json()) as NotificationPage
  return { page: data, unreadCount }
}

async function markRead(id: string): Promise<void> {
  const res = await fetch(`/api/notifications/${id}/read`, { method: 'PATCH' })
  if (!res.ok) throw new Error(`markRead failed: ${res.status}`)
}

async function dismissNotification(id: string): Promise<void> {
  const res = await fetch(`/api/notifications/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`dismiss failed: ${res.status}`)
}

// ─── filter controls ──────────────────────────────────────────────────────────

const STATUS_FILTER_DEF: FilterDef[] = [
  {
    key: 'status',
    label: 'Status',
    options: [
      { value: 'UNREAD', label: 'Unread' },
      { value: 'READ',   label: 'Read' },
      { value: 'ALL',    label: 'All' },
    ],
  },
]

// ─── virtualized columns ─────────────────────────────────────────────────────

function buildColumns(
  onMarkRead: (id: string) => void,
  onDismiss: (id: string) => void,
  onNotificationClick?: (id: string) => void
): ColumnDef<NotificationItemData>[] {
  return [
    {
      key: 'title',
      header: 'Notification',
      cell: (row) => (
        <NotificationItem
          notification={row}
          onClick={onNotificationClick}
          onMarkRead={onMarkRead}
          onDismiss={onDismiss}
        />
      ),
    },
  ]
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * NotificationList — full-featured notification inbox block.
 *
 * Composes:
 *   L2 VirtualizedTable  → renders potentially 1000+ notification rows with DOM virtualization
 *   L2 NotificationItem  → individual notification card (title, body, type, timestamp, actions)
 *   L2 FilterBar         → UNREAD / READ / ALL toggle filter
 *   L2 EmptyState        → shown when no notifications match filter
 *
 * Data flow:
 *   - TanStack Query fetches GET /api/notifications with ?status filter and pagination
 *   - X-Unread-Count header is extracted for external unread badge usage
 *   - Mark-read mutation: PATCH /api/notifications/{id}/read → invalidates query
 *   - Dismiss mutation:   DELETE /api/notifications/{id}    → invalidates query
 *
 * Fork instructions:
 *   1. Use inside /(notification)/inbox/page.tsx (L4 vertical).
 *   2. Pass `onNotificationClick` to navigate to /(notification)/[id].
 *   3. Adjust `containerHeight` to fit your layout's available space.
 *   4. Replace fetch() calls with your API client / tRPC.
 *   5. Adjust `pollInterval` if you add SSE for real-time updates.
 */
export default function NotificationList({
  defaultFilter = 'UNREAD',
  containerHeight = 600,
  onNotificationClick,
  className,
}: NotificationListProps) {
  const queryClient = useQueryClient()
  const [filter, setFilter] = React.useState<NotificationStatusFilter>(defaultFilter)
  const PAGE_SIZE = 50  // Larger page for virtualized render

  // R82 — dataUpdatedAt destructured so the polled list exposes a
  // freshness anchor for the operator + screen reader. Without it the
  // section aria-busy below only signals "loading" not "freshness".
  const { data, isLoading, isError, dataUpdatedAt } = useQuery({
    queryKey: ['notifications', filter, PAGE_SIZE],
    queryFn: () => fetchNotifications(filter, 0, PAGE_SIZE),
    staleTime: 15_000,
    refetchInterval: 30_000,
  })

  const markReadMutation = useMutation({
    mutationFn: markRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  const dismissMutation = useMutation({
    mutationFn: dismissNotification,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  // R82 — combined in-flight signal: list-load OR any per-row mutation
  // is in flight. AT users hear the section as busy during any state
  // transition (list refetch + mark-read + dismiss).
  const anyInFlight = isLoading
    || markReadMutation.isPending
    || dismissMutation.isPending

  const columns = React.useMemo(
    () =>
      buildColumns(
        (id) => markReadMutation.mutate(id),
        (id) => dismissMutation.mutate(id),
        onNotificationClick
      ),
    [markReadMutation, dismissMutation, onNotificationClick]
  )

  const notifications = data?.page.content ?? []

  const emptySlot = isError ? (
    <EmptyState
      title="Failed to load notifications"
      description="An error occurred while fetching your notifications. Please try again."
      actionSlot={
        <button
          type="button"
          onClick={() => queryClient.invalidateQueries({ queryKey: ['notifications'] })}
          className="rounded-md bg-primary px-4 py-2 text-sm text-primary-foreground hover:bg-primary/90"
        >
          Retry
        </button>
      }
    />
  ) : (
    <EmptyState
      title={filter === 'UNREAD' ? 'All caught up!' : 'No notifications'}
      description={
        filter === 'UNREAD'
          ? 'You have no unread notifications.'
          : 'No notifications to display.'
      }
    />
  )

  return (
    <div className={className}>
      {/* Status filter using FilterBar component */}
      <FilterBar
        filters={STATUS_FILTER_DEF}
        activeValues={{ status: [filter] }}
        onFilter={(_key, values) => {
          if (values[0]) setFilter(values[0] as NotificationStatusFilter)
        }}
      />

      {/* R82 — visible polling-cadence timestamp + aria-live polite
          announcement of freshness. The section aria-busy below covers
          list refetch AND in-flight mutations so AT users hear the
          combined operation state. */}
      <p
        className="mt-2 text-xs text-muted-foreground"
        aria-live="polite"
      >
        {dataUpdatedAt
          ? `Updated ${new Date(dataUpdatedAt).toLocaleTimeString()}`
          : ''}
      </p>

      {/* Virtualized notification list */}
      <section aria-label="Notification list" aria-live="polite" aria-busy={anyInFlight}>
        <VirtualizedTable<NotificationItemData>
          columns={columns}
          data={notifications}
          getRowKey={(row) => row.id}
          isLoading={isLoading}
          emptySlot={emptySlot}
          containerHeight={containerHeight}
          estimatedRowHeight={96}  // approx height per notification card
        />
      </section>
    </div>
  )
}
