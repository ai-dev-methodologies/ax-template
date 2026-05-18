/*
---
template_id: L2/blocks/app-shell
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA Landmarks — main, navigation, banner regions"
    url: "https://www.w3.org/TR/wai-aria-1.2/#landmark_roles"
  - source_type: external
    citation: "WCAG 2.2 SC 2.4.1 Bypass Blocks (Level A): skipLinkSlot satisfies the bypass-blocks requirement when provided. The skip link must be the first focusable element on the page."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/bypass-blocks.html"
  - source_type: internal
    rationale: "L2 layout block — app chrome skeleton with header/sidebar/main slots. No domain logic. SP34 in-place edit: added skipLinkSlot (opt-in, WCAG 2.4.1) and announceLiveSlot (opt-in, WCAG 4.1.3) as composable a11y slots."
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
  /**
   * Skip-link slot (opt-in, WCAG 2.4.1 — Bypass Blocks, Level A).
   * Must be the FIRST focusable element. Pass a SkipLink component:
   *   skipLinkSlot={<SkipLink targetId="main-content" />}
   * The targetId must match the id on the <main> element below.
   */
  skipLinkSlot?: React.ReactNode
  /**
   * Announce-live slot (opt-in, WCAG 4.1.3 — Status Messages, Level AA).
   * Pass an AnnounceLiveProvider to enable programmatic status announcements:
   *   announceLiveSlot={<AnnounceLiveProvider>{children}</AnnounceLiveProvider>}
   * Typically wired at the root layout level, not per-page.
   */
  announceLiveSlot?: React.ReactNode
}

/**
 * AppShell — L2 layout block.
 *
 * Provides the outer chrome: header + optional sidebar + main content area.
 * All content injected via slots. Sidebar open/close state is caller-controlled.
 *
 * ## Slot contract
 * | Slot             | Required | Description                                         |
 * |-----------------|----------|-----------------------------------------------------|
 * | skipLinkSlot    | no       | WCAG 2.4.1 skip-navigation link (first focusable)   |
 * | headerSlot      | no       | Full-width sticky header                             |
 * | sidebarSlot     | no       | Collapsible sidebar                                  |
 * | children        | yes      | Main scrollable content area                         |
 * | announceLiveSlot| no       | WCAG 4.1.3 aria-live region provider                |
 *
 * ## A11y composables (SP34)
 *
 * Wire these by passing the corresponding L2 block as a slot:
 *
 * ```tsx
 * import AppShell from 'templates/L2/blocks/app-shell'
 * import SkipLink from 'templates/L2/blocks/skip-link'
 * import { AnnounceLiveProvider } from 'templates/L2/blocks/announce-live'
 *
 * export default function RootLayout({ children }) {
 *   return (
 *     <AnnounceLiveProvider>
 *       <AppShell
 *         skipLinkSlot={<SkipLink targetId="main-content" />}
 *         headerSlot={<AppHeader />}
 *       >
 *         <main id="main-content">{children}</main>
 *       </AppShell>
 *     </AnnounceLiveProvider>
 *   )
 * }
 * ```
 */
export default function AppShell({
  headerSlot,
  sidebarSlot,
  children,
  sidebarOpen = true,
  skipLinkSlot,
  announceLiveSlot,
}: AppShellProps) {
  return (
    <div className="flex min-h-screen flex-col">
      {/* Skip-link slot — MUST be first focusable element (WCAG 2.4.1) */}
      {skipLinkSlot}

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

      {/* Announce-live slot — aria-live regions for status messages (WCAG 4.1.3) */}
      {announceLiveSlot}
    </div>
  )
}
