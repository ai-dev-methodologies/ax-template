/*
---
template_id: L3/pages/audit-log-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 16 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "L3 audit-log page skeleton using flat tab/header navigation via app-shell tabs (SP34 architecture note §5.7: no breadcrumb dependency). Three named tab slots: events list, filters sidebar, and export panel. L4 supplies typed event data and filter configuration. Reusable across admin and compliance surfaces."
imports_from: [L1, L2]
imports_forbidden: [L4]
---
*/
import * as React from 'react'

export type AuditLogTab = 'events' | 'exports' | 'settings'

export interface AuditLogPageProps {
  /** Page heading (default: "Audit Log"). */
  title?: string
  /** Optional subtitle. */
  description?: string
  /** Active tab (controlled by caller / URL search param). */
  activeTab?: AuditLogTab
  /** Called when the user clicks a tab. */
  onTabChange?: (tab: AuditLogTab) => void
  /** Events tab content — typically a table + filter bar. */
  eventsSlot?: React.ReactNode
  /** Exports tab content — export-job status, download list. */
  exportsSlot?: React.ReactNode
  /** Settings tab content — retention policies, archival rules. */
  settingsSlot?: React.ReactNode
  /** Optional header action (e.g. "Export" button). */
  headerAction?: React.ReactNode
}

const TABS: { id: AuditLogTab; label: string }[] = [
  { id: 'events', label: 'Events' },
  { id: 'exports', label: 'Exports' },
  { id: 'settings', label: 'Settings' },
]

/**
 * AuditLogPage — L3 page template for audit-log surfaces.
 *
 * Uses flat tab/header navigation (no breadcrumb). The active tab is
 * caller-controlled, typically driven by the URL search param `?tab=`.
 *
 * ## Architecture note (SP34 §5.7)
 *
 * L3 page uses `<nav>` tabs instead of breadcrumb hierarchy to keep the
 * admin navigation flat and avoid the deferred L1 `breadcrumb` dependency.
 *
 * ## Usage
 *
 * ```tsx
 * // app/admin/audit-log/page.tsx  (L4 route)
 * import AuditLogPage from 'templates/L3/pages/audit-log-page/page'
 *
 * export default function AuditLogRoute({ searchParams }) {
 *   const tab = (searchParams.tab ?? 'events') as AuditLogTab
 *   return (
 *     <AuditLogPage
 *       activeTab={tab}
 *       eventsSlot={<AuditEventTable />}
 *       exportsSlot={<ExportJobList />}
 *       settingsSlot={<RetentionSettings />}
 *       headerAction={<ExportButton />}
 *     />
 *   )
 * }
 * ```
 */
export default function AuditLogPage({
  title = 'Audit Log',
  description = 'Review all system and user activity.',
  activeTab = 'events',
  onTabChange,
  eventsSlot,
  exportsSlot,
  settingsSlot,
  headerAction,
}: AuditLogPageProps) {
  const slotMap: Record<AuditLogTab, React.ReactNode> = {
    events: eventsSlot,
    exports: exportsSlot,
    settings: settingsSlot,
  }

  return (
    <div className="flex flex-col h-full">
      {/* Page header */}
      <header className="px-6 pt-6 pb-0">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-0.5">
            <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
            {description && (
              <p className="text-sm text-muted-foreground">{description}</p>
            )}
          </div>
          {headerAction && <div className="shrink-0">{headerAction}</div>}
        </div>

        {/* Flat tab navigation */}
        <nav
          role="tablist"
          aria-label="Audit log sections"
          className="flex gap-0 mt-4 border-b"
        >
          {TABS.map(({ id, label }) => (
            <button
              key={id}
              role="tab"
              aria-selected={activeTab === id}
              aria-controls={`audit-log-panel-${id}`}
              id={`audit-log-tab-${id}`}
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
      <main className="flex-1 overflow-y-auto px-6 py-6">
        {TABS.map(({ id }) => (
          <div
            key={id}
            role="tabpanel"
            id={`audit-log-panel-${id}`}
            aria-labelledby={`audit-log-tab-${id}`}
            hidden={activeTab !== id}
            data-testid={`audit-log-panel-${id}`}
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
