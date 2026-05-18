/*
---
template_id: L2/blocks/form-section-extended
layer: L2
provenance_class: internal_design
supersedes: L2/blocks/form-section (SP15 shell — deprecated, re-exports this file)
evidence:
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — grouping form controls with fieldset/legend"
    url: "https://www.w3.org/WAI/tutorials/forms/grouping/"
  - source_type: internal
    rationale: "Extended L2 form block — accessible fieldset wrapper with collapsible support and step indicator. Supersedes SP15 shell."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface FormSectionExtendedProps {
  /** Section title — rendered as `<legend>` inside a `<fieldset>` */
  title: string
  /** Optional description shown below the title */
  description?: string
  /** Step number for multi-step forms (e.g. "Step 1 of 3") */
  step?: { current: number; total: number }
  /** If true, section is collapsible (accordion-style). Default false. */
  collapsible?: boolean
  /** Initial collapsed state (only meaningful when collapsible=true). Default false. */
  defaultCollapsed?: boolean
  /** Visual variant */
  variant?: 'default' | 'card' | 'flat'
  children: React.ReactNode
  className?: string
}

/**
 * FormSectionExtended — accessible fieldset wrapper for multi-section forms.
 *
 * Supersedes the SP15 shell `form-section.tsx` (which re-exports this file).
 *
 * Features:
 * - Proper `<fieldset>`/`<legend>` for screen reader grouping
 * - Optional step indicator for multi-step forms
 * - Optional collapsible toggle (accordion-style)
 * - Three visual variants: default, card, flat
 *
 * @example
 * // Static section
 * <FormSectionExtended title="Personal Information" description="Enter your details">
 *   <NameField />
 *   <EmailField />
 * </FormSectionExtended>
 *
 * // Multi-step indicator
 * <FormSectionExtended title="Address" step={{ current: 2, total: 3 }} variant="card">
 *   <AddressFields />
 * </FormSectionExtended>
 */
export default function FormSectionExtended({
  title,
  description,
  step,
  collapsible = false,
  defaultCollapsed = false,
  variant = 'default',
  children,
  className,
}: FormSectionExtendedProps) {
  const [isCollapsed, setIsCollapsed] = React.useState(defaultCollapsed)
  const contentId = React.useId()

  const variantClasses: Record<NonNullable<FormSectionExtendedProps['variant']>, string> = {
    default: 'space-y-4',
    card:    'rounded-lg border border-border bg-card p-6 space-y-4',
    flat:    'space-y-4 border-b border-border pb-6',
  }

  return (
    <fieldset
      className={[variantClasses[variant], className].filter(Boolean).join(' ')}
      aria-expanded={collapsible ? !isCollapsed : undefined}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-0.5">
          <legend className="text-base font-semibold leading-none">
            {step && (
              <span className="mr-2 text-sm font-normal text-muted-foreground">
                Step {step.current}/{step.total}
              </span>
            )}
            {title}
          </legend>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
        </div>

        {collapsible && (
          <button
            type="button"
            aria-expanded={!isCollapsed}
            aria-controls={contentId}
            onClick={() => setIsCollapsed(prev => !prev)}
            className="shrink-0 rounded-sm p-1 text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            aria-label={isCollapsed ? `Expand ${title}` : `Collapse ${title}`}
          >
            <svg
              className={['h-4 w-4 transition-transform', isCollapsed ? '' : 'rotate-180'].join(' ')}
              viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}
              aria-hidden="true"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        )}
      </div>

      {/* Content */}
      {(!collapsible || !isCollapsed) && (
        <div id={contentId} className="space-y-4">
          {children}
        </div>
      )}
    </fieldset>
  )
}
