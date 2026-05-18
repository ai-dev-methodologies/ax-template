/*
---
template_id: L3/pages/admin-overview-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "L3 admin overview page skeleton. Flat tab navigation (Users / Permissions / System / Activity) — no breadcrumb dependency per SP34 §5.7 architecture note. Named slots for each tab panel. L4 supplies domain data (user counts, system health, audit feed). Designed for fork-receiver admin dashboards."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
import * as React from 'react'

export type AdminTab = 'overview' | 'users' | 'system' | 'activity'

export interface AdminKpiCard {
  /** Card label. */
  label: string
  /** Primary metric value (string for formatted numbers). */
  value: string
  /** Optional delta (e.g. "+12%"). */
  delta?: string
  /** Whether delta is positive (green) or negative (red). */
  deltaPositive?: boolean
  /** Optional icon emoji or URL. */
  icon?: string
}

export interface AdminOverviewPageProps {
  /** Page heading (default: "Administration"). */
  title?: string
  /** Active tab (controlled). */
  activeTab?: AdminTab
  /** Tab change callback. */
  onTabChange?: (tab: AdminTab) => void
  /** KPI summary cards shown at the top of the overview tab. */
  kpiCards?: AdminKpiCard[]
  /** Overview tab content (below KPI cards). */
  overviewSlot?: React.ReactNode
  /** Users tab content. */
  usersSlot?: React.ReactNode
  /** System tab content (health, background jobs, feature flags). */
  systemSlot?: React.ReactNode
  /** Activity tab content (audit feed, impersonation log). */
  activitySlot?: React.ReactNode
  /** Optional banner slot (e.g. ImpersonationBanner, MaintenanceNotice). */
  bannerSlot?: React.ReactNode
  /** Optional header action (e.g. "Invite user" button). */
  headerAction?: React.ReactNode
}

const TABS: { id: AdminTab; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'users', label: 'Users' },
  { id: 'system', label: 'System' },
  { id: 'activity', label: 'Activity' },
]

function KpiCard({ card }: { card: AdminKpiCard }) {
  return (
    <div
      className="rounded-lg border bg-card p-4 shadow-sm space-y-1"
      data-testid="admin-kpi-card"
    >
      <div className="flex items-center justify-between">
        <span className="text-sm text-muted-foreground">{card.label}</span>
        {card.icon && <span aria-hidden="true">{card.icon}</span>}
      </div>
      <p className="text-2xl font-bold">{card.value}</p>
      {card.delta && (
        <p
          className={[
            'text-xs font-medium',
            card.deltaPositive !== false ? 'text-green-600' : 'text-red-600',
          ].join(' ')}
        >
          {card.delta}
        </p>
      )}
    </div>
  )
}

/**
 * AdminOverviewPage — L3 page template for admin dashboards.
 *
 * Flat tab navigation (no breadcrumbs). KPI summary row at the top of the
 * overview tab. Named slots for each tab content area.
 *
 * ## Usage
 *
 * ```tsx
 * // app/admin/page.tsx  (L4 route)
 * import AdminOverviewPage, { AdminTab } from 'templates/L3/pages/admin-overview-page/page'
 * import ImpersonationBanner from 'templates/L2/blocks/impersonation-banner'
 *
 * export default async function AdminRoute({ searchParams }) {
 *   const session = await getAdminSession()
 *   const tab = (searchParams.tab ?? 'overview') as AdminTab
 *   return (
 *     <AdminOverviewPage
 *       activeTab={tab}
 *       bannerSlot={<ImpersonationBanner session={session} onEndImpersonation={endImpersonation} />}
 *       kpiCards={[
 *         { label: 'Total users', value: '12,345', delta: '+3%', deltaPositive: true, icon: '👥' },
 *         { label: 'Active sessions', value: '892', icon: '🔐' },
 *       ]}
 *       overviewSlot={<DashboardSummary />}
 *       usersSlot={<UserManagementTable />}
 *       systemSlot={<SystemHealthPanel />}
 *       activitySlot={<ActivityFeed events={auditEvents} />}
 *     />
 *   )
 * }
 * ```
 */
export default function AdminOverviewPage({
  title = 'Administration',
  activeTab = 'overview',
  onTabChange,
  kpiCards = [],
  overviewSlot,
  usersSlot,
  systemSlot,
  activitySlot,
  bannerSlot,
  headerAction,
}: AdminOverviewPageProps) {
  const slotMap: Record<AdminTab, React.ReactNode> = {
    overview: overviewSlot,
    users: usersSlot,
    system: systemSlot,
    activity: activitySlot,
  }

  return (
    <div className="flex flex-col h-full">
      {/* Banner slot (ImpersonationBanner, MaintenanceNotice, etc.) */}
      {bannerSlot}

      {/* Page header */}
      <header className="px-6 pt-6 pb-0">
        <div className="flex items-center justify-between gap-4 mb-4">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {headerAction && <div className="shrink-0">{headerAction}</div>}
        </div>

        {/* Flat tab navigation */}
        <nav
          role="tablist"
          aria-label="Admin sections"
          className="flex gap-0 border-b"
        >
          {TABS.map(({ id, label }) => (
            <button
              key={id}
              role="tab"
              aria-selected={activeTab === id}
              aria-controls={`admin-panel-${id}`}
              id={`admin-tab-${id}`}
              type="button"
              onClick={() => onTabChange?.(id)}
              className={[
                'px-4 py-2 text-sm font-medium -mb-px border-b-2 transition-colors',
                'focus:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset',
                activeTab === id
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border',
              ].join(' ')}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>

      {/* Tab panels */}
      <main id="main" className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
        {/* KPI cards — shown only on overview tab */}
        {activeTab === 'overview' && kpiCards.length > 0 && (
          <div
            className="grid gap-4 grid-cols-2 sm:grid-cols-4"
            aria-label="Key metrics"
          >
            {kpiCards.map((card) => (
              <KpiCard key={card.label} card={card} />
            ))}
          </div>
        )}

        {TABS.map(({ id }) => (
          <div
            key={id}
            role="tabpanel"
            id={`admin-panel-${id}`}
            aria-labelledby={`admin-tab-${id}`}
            hidden={activeTab !== id}
            data-testid={`admin-panel-${id}`}
          >
            {slotMap[id] ?? (
              <p className="text-sm text-muted-foreground">
                No content configured for this tab.
              </p>
            )}
          </div>
        ))}
      </main>
    </div>
  )
}
