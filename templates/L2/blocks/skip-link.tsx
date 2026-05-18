/*
---
template_id: L2/blocks/skip-link
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: external
    citation: "WCAG 2.2 SC 2.4.1 Bypass Blocks (Level A): A mechanism is available to bypass blocks of content that are repeated on multiple Web pages. The skip-link pattern satisfies this criterion."
    url: "https://www.w3.org/WAI/WCAG22/Understanding/bypass-blocks.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "WebAIM — Skip Navigation Links: The skip link MUST be the first focusable element on the page. It should be visible on focus so keyboard users can see it."
    url: "https://webaim.org/techniques/skipnav/"
    quoted_at: "2026-05-18"
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
import * as React from 'react'

export interface SkipLinkProps {
  /** The `id` of the main content element to jump to (default: "main"). */
  targetId?: string
  /** Visible label for the link (default: "Skip to main content"). */
  label?: string
  /** Custom class name for the link element. */
  className?: string
}

/**
 * SkipLink — WCAG 2.4.1 skip-navigation link.
 *
 * Must be the **first focusable element** on the page. Visually hidden
 * until focused so keyboard users can bypass the navigation block.
 *
 * Wire `app-shell.tsx` to render this as the first child:
 *
 * ```tsx
 * import SkipLink from 'templates/L2/blocks/skip-link'
 *
 * // app/layout.tsx — FIRST element inside <body>
 * <SkipLink targetId="main-content" />
 * <AppShell ...>
 *   <main id="main-content">
 *     {children}
 *   </main>
 * </AppShell>
 * ```
 *
 * ## WCAG compliance
 * - SC 2.4.1 (Level A) — skip navigation link satisfies Bypass Blocks.
 * - Visible on focus: `focus:translate-y-0` reveals the link when tabbed to.
 * - `id="skip-link"` allows E2E tests to assert first-focusable position.
 */
export default function SkipLink({
  targetId = 'main',
  label = 'Skip to main content',
  className,
}: SkipLinkProps) {
  return (
    <a
      id="skip-link"
      href={`#${targetId}`}
      data-testid="skip-link"
      className={[
        // Visually hidden until focused
        'absolute -translate-y-full transform opacity-0',
        'focus:translate-y-0 focus:opacity-100',
        // Appearance when visible
        'left-0 top-0 z-[100]',
        'inline-block bg-primary px-4 py-2',
        'text-sm font-semibold text-primary-foreground',
        'rounded-br-md shadow-lg',
        'transition-transform duration-150',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {label}
    </a>
  )
}
