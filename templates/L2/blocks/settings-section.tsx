/*
---
template_id: L2/blocks/settings-section
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "WAI-ARIA 1.2 — section landmark: Use aria-labelledby to associate a section heading with its region so screen readers identify the purpose of the content area."
    url: "https://www.w3.org/TR/wai-aria-1.2/#aria-labelledby"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Reusable section shell used inside settings pages and admin panels. Renders a titled card with optional description and action slot. L4 composes these as named slots passed to SettingsOverviewPage."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
import * as React from 'react'

export interface SettingsSectionProps {
  /** Section heading text (required). */
  title: string
  /** Optional subtitle shown below the title. */
  description?: string
  /** Section body content. */
  children: React.ReactNode
  /** Optional action(s) placed in the header row (e.g. Save button, toggle). */
  action?: React.ReactNode
  /** Custom class name for the outer wrapper. */
  className?: string
  /** Unique id for aria-labelledby (auto-derived from title if omitted). */
  id?: string
}

function slugify(s: string): string {
  return s.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')
}

/**
 * SettingsSection — reusable titled card for settings pages.
 *
 * Complements `SettingsOverviewPage` — use directly when you need a single
 * standalone section (e.g. inside an individual settings route).
 *
 * ```tsx
 * import SettingsSection from 'templates/L2/blocks/settings-section'
 *
 * <SettingsSection
 *   title="Notifications"
 *   description="Configure how you receive alerts."
 *   action={<SaveButton />}
 * >
 *   <NotificationsForm />
 * </SettingsSection>
 * ```
 */
export default function SettingsSection({
  title,
  description,
  children,
  action,
  className,
  id,
}: SettingsSectionProps) {
  const headingId = id ?? `settings-section-${slugify(title)}`

  return (
    <section
      aria-labelledby={headingId}
      data-testid="settings-section"
      className={[
        'rounded-lg border bg-card shadow-sm',
        className ?? '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-4 px-6 py-4 border-b">
        <div className="space-y-0.5">
          <h2 id={headingId} className="text-base font-semibold leading-snug">
            {title}
          </h2>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>

      {/* Body */}
      <div className="px-6 py-4">{children}</div>
    </section>
  )
}
