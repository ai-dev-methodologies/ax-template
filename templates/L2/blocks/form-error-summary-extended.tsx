/*
---
template_id: L2/blocks/form-error-summary-extended
layer: L2
provenance_class: internal_design
supersedes: L2/blocks/form-error-summary (SP15 shell — deprecated, re-exports this file)
evidence:
  - source_type: external
    citation: "React Hook Form docs — formState.errors"
    url: "https://react-hook-form.com/docs/useformstate"
  - source_type: external
    citation: "WAI-ARIA Authoring Practices — Alert pattern (role=alert, aria-live=assertive)"
    url: "https://www.w3.org/WAI/ARIA/apg/patterns/alert/"
  - source_type: internal
    rationale: "Extended L2 form block — full RHF error tree traversal with ARIA live region and anchor links to fields."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'
import { useFormContext, type FieldValues, type FieldErrors } from 'react-hook-form'

// ─── types ────────────────────────────────────────────────────────────────────

export interface FormErrorSummaryExtendedProps {
  /** Section heading — defaults to "Please fix the following errors" */
  heading?: string
  /** If true, clicking an error message scrolls to and focuses the field */
  linkToFields?: boolean
  /** Fields to exclude from the summary (e.g. root-level async errors shown elsewhere) */
  excludeFields?: string[]
  className?: string
}

interface FlatError {
  path: string
  message: string
}

/**
 * FormErrorSummaryExtended — accessible error summary for multi-field forms.
 *
 * Supersedes the SP15 shell `form-error-summary.tsx` (which re-exports this file).
 *
 * Renders all RHF validation errors in a `role="alert"` region at the top of the form.
 * On submit-attempt with errors, screen readers announce the summary.
 * Clicking an error link scrolls to and focuses the offending field (requires
 * field `id` to match `react-hook-form` field path, e.g. id="email" for field "email").
 *
 * Must be used inside a `<FormProvider>`.
 *
 * @example
 * <form onSubmit={form.handleSubmit(onSubmit)}>
 *   <FormErrorSummaryExtended heading="Fix these errors before submitting" linkToFields />
 *   <EmailField />
 *   <PasswordField />
 * </form>
 */
export default function FormErrorSummaryExtended({
  heading = 'Please fix the following errors',
  linkToFields = true,
  excludeFields = [],
  className,
}: FormErrorSummaryExtendedProps) {
  const { formState: { errors, submitCount } } = useFormContext()

  const flatErrors = React.useMemo(
    () => flattenErrors(errors, '', excludeFields),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [errors, submitCount, excludeFields.join(',')]
  )

  if (flatErrors.length === 0) return null

  return (
    <div
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
      className={[
        'rounded-md border border-destructive/50 bg-destructive/5 px-4 py-3',
        className,
      ].filter(Boolean).join(' ')}
    >
      <p className="mb-2 text-sm font-semibold text-destructive">
        {heading} ({flatErrors.length})
      </p>
      <ul className="space-y-1">
        {flatErrors.map(({ path, message }) => (
          <li key={path} className="text-sm text-destructive">
            {linkToFields ? (
              <a
                href={`#${path}`}
                onClick={(e) => {
                  e.preventDefault()
                  const el = document.getElementById(path)
                  el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
                  el?.focus()
                }}
                className="underline underline-offset-2 hover:no-underline"
              >
                {message}
              </a>
            ) : message}
          </li>
        ))}
      </ul>
    </div>
  )
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function flattenErrors(
  errors: FieldErrors<FieldValues>,
  prefix: string,
  exclude: string[]
): FlatError[] {
  const result: FlatError[] = []

  for (const [key, value] of Object.entries(errors)) {
    if (!value) continue
    const path = prefix ? `${prefix}.${key}` : key
    if (exclude.includes(path)) continue

    if (typeof (value as { message?: string }).message === 'string') {
      result.push({ path, message: (value as { message: string }).message })
    } else if (typeof value === 'object' && !Array.isArray(value)) {
      result.push(...flattenErrors(value as FieldErrors<FieldValues>, path, exclude))
    } else if (Array.isArray(value)) {
      value.forEach((item, i) => {
        if (item && typeof item === 'object') {
          result.push(...flattenErrors(item as FieldErrors<FieldValues>, `${path}.${i}`, exclude))
        }
      })
    }
  }

  return result
}
