/*
---
template_id: L2/blocks/app-shell
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA Landmarks — main, navigation, banner regions"
    url: "https://www.w3.org/TR/wai-aria-1.2/#landmark_roles"
  - source_type: internal
    rationale: "L2 layout block — app chrome skeleton with header/sidebar/main slots. No domain logic."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export interface AppShellProps {
  /** AppHeader slot (sticky, full-width) */
  headerSlot?: React.ReactNode
  /** Sidebar slot (collapsible in mobile) */
  sidebarSlot?: React.ReactNode
  /** Main page content */
  children: React.ReactNode
  /** Whether sidebar is visible (controlled by caller) */
  sidebarOpen?: boolean
}

/**
 * AppShell — L2 layout block.
 *
 * Provides the outer chrome: header + optional sidebar + main content area.
 * All content injected via slots. Sidebar open/close state is caller-controlled.
 *
 * ## Slot contract
 * | Slot        | Required | Description                         |
 * |------------|----------|-------------------------------------|
 * | headerSlot  | no       | Full-width sticky header             |
 * | sidebarSlot | no       | Collapsible sidebar                  |
 * | children    | yes      | Main scrollable content area         |
 */
export default function AppShell({
  headerSlot,
  sidebarSlot,
  children,
  sidebarOpen = true,
}: AppShellProps) {
  return (
    <div className="flex min-h-screen flex-col">
      {headerSlot}

      <div className="flex flex-1 overflow-hidden">
        {sidebarSlot && (
          <aside
            aria-label="Sidebar navigation"
            data-open={sidebarOpen}
            className="hidden w-64 flex-shrink-0 border-r border-border bg-background data-[open=true]:flex flex-col md:flex"
          >
            {sidebarSlot}
          </aside>
        )}

        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  )
}
