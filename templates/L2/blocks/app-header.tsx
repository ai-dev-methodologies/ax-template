/*
---
template_id: L2/blocks/app-header
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WCAG 2.1 SC 2.4.1 Bypass Blocks — landmark regions for navigation"
    url: "https://www.w3.org/WAI/WCAG21/Understanding/bypass-blocks.html"
  - source_type: internal
    rationale: "L2 layout block per PRD §4.11 — AppHeader requires navigation slot contract, so it is L2 not L1."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export interface AppHeaderProps {
  /** Brand logo / wordmark slot */
  logoSlot?: React.ReactNode
  /** Primary navigation slot */
  navSlot?: React.ReactNode
  /** Right-side actions slot (user menu, notifications, etc.) */
  actionsSlot?: React.ReactNode
  /** Accessible label for the header landmark */
  ariaLabel?: string
}

/**
 * AppHeader — L2 layout block.
 *
 * Per PRD §4.11 this block is L2 (not L1) because it requires a navigation
 * slot contract. All domain content is injected via slots.
 *
 * ## Slot contract
 * | Slot         | Required | Description                                |
 * |-------------|----------|--------------------------------------------|
 * | logoSlot     | no       | Brand mark; links to home in L4 context    |
 * | navSlot      | no       | Primary navigation links                   |
 * | actionsSlot  | no       | User menu, notifications, theme toggle     |
 */
export default function AppHeader({
  logoSlot,
  navSlot,
  actionsSlot,
  ariaLabel = 'Main header',
}: AppHeaderProps) {
  return (
    <header
      aria-label={ariaLabel}
      className="sticky top-0 z-40 w-full border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60"
    >
      <div className="container flex h-14 items-center gap-4">
        {logoSlot && <div className="flex-shrink-0">{logoSlot}</div>}

        {navSlot && (
          <nav aria-label="Primary navigation" className="flex flex-1 items-center gap-1">
            {navSlot}
          </nav>
        )}

        {!navSlot && <div className="flex-1" />}

        {actionsSlot && (
          <div className="flex items-center gap-2">{actionsSlot}</div>
        )}
      </div>
    </header>
  )
}
