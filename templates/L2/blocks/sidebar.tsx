/*
---
template_id: L2/blocks/sidebar
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA navigation landmark — nav element best practices"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/landmark/examples/navigation.html"
  - source_type: internal
    rationale: "L2 layout block — sidebar nav with active state via props; no router imports."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
import * as React from 'react'

export interface NavItem {
  href: string
  label: string
  /** Optional icon node */
  icon?: React.ReactNode
  /** Sub-items for grouped nav */
  children?: NavItem[]
}

export interface SidebarProps {
  navItems: NavItem[]
  /** Current active href for highlight */
  activeHref?: string
  /** Slot for bottom content (user menu, settings) */
  footerSlot?: React.ReactNode
  /** Called when a nav link is clicked (caller handles routing) */
  onNavigate?: (href: string) => void
}

export default function Sidebar({
  navItems,
  activeHref,
  footerSlot,
  onNavigate,
}: SidebarProps) {
  return (
    <div className="flex h-full flex-col gap-2 p-3">
      <nav aria-label="Sidebar navigation" className="flex-1 space-y-1">
        {navItems.map(item => (
          <a
            key={item.href}
            href={item.href}
            aria-current={item.href === activeHref ? 'page' : undefined}
            onClick={e => {
              if (onNavigate) {
                e.preventDefault()
                onNavigate(item.href)
              }
            }}
            className={[
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              item.href === activeHref
                ? 'bg-accent text-accent-foreground'
                : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
            ].join(' ')}
          >
            {item.icon && <span aria-hidden="true">{item.icon}</span>}
            {item.label}
          </a>
        ))}
      </nav>

      {footerSlot && (
        <div className="border-t border-border pt-3">{footerSlot}</div>
      )}
    </div>
  )
}
