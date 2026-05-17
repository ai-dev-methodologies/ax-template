/*
---
template_id: L3/pages/dashboard-page
layer: L3
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Next.js 15 App Router file conventions — page.tsx"
    url: "https://nextjs.org/docs/app/building-your-application/routing/pages"
  - source_type: internal
    rationale: "Generic dashboard skeleton for ax-template L4 composition. Accepts an array of widget slot nodes — no domain logic or data fetching."
dependencies: []
---
*/
import * as React from 'react'

/**
 * DashboardPage — generic dashboard skeleton template.
 *
 * Slot props:
 *   - title       (optional) page heading (default: "Dashboard")
 *   - description (optional) subtitle text
 *   - widgetSlots (required) array of widget nodes (stat cards, charts, tables…)
 *   - headerSlot  (optional) additional content in the header row (date range picker, etc.)
 *
 * Widgets are laid out in a responsive grid. Pass an empty array to render
 * the empty-state placeholder.
 *
 * L4 usage:
 *   import DashboardPage from 'templates/L3/pages/dashboard-page/page'
 *   export default function AdminDashboard() {
 *     return (
 *       <DashboardPage
 *         title="Admin Dashboard"
 *         widgetSlots={[
 *           <RevenueCard key="revenue" />,
 *           <OrdersCard key="orders" />,
 *           <UsersChart key="users" />,
 *         ]}
 *       />
 *     )
 *   }
 */
export interface DashboardPageProps {
  /** Page heading (default: "Dashboard") */
  title?: string
  /** Optional subtitle */
  description?: string
  /** Widget nodes arranged in a responsive grid */
  widgetSlots: React.ReactNode[]
  /** Optional header area slot (date pickers, global filters, etc.) */
  headerSlot?: React.ReactNode
}

export default function DashboardPage({
  title = 'Dashboard',
  description,
  widgetSlots,
  headerSlot,
}: DashboardPageProps) {
  return (
    <main className="container mx-auto px-4 py-8 space-y-6">
      {/* Header row */}
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {headerSlot && (
          <div className="flex items-center gap-2">{headerSlot}</div>
        )}
      </div>

      {/* Widget grid */}
      {widgetSlots.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {widgetSlots.map((widget, index) => (
            <div key={index}>{widget}</div>
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center py-24 text-center space-y-3">
          <p className="text-muted-foreground text-sm">
            No widgets configured yet.
          </p>
        </div>
      )}
    </main>
  )
}
