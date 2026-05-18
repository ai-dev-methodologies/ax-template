/*
---
template_id: L2/blocks/preferences-form
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React Hook Form — Controlled inputs with register(): the preferred pattern for settings forms because it integrates with native form reset and does not require controlled state per field."
    url: "https://react-hook-form.com/docs/useform/register"
    quoted_at: "2026-05-18"
  - source_type: internal
    rationale: "Generic user-preferences form shell. Accepts typed default values and an onSubmit handler. L4 extends with domain preferences (notification toggles, timezone picker, language selector). No domain-specific fields — those are supplied via the children slot."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'

export interface PreferencesFormProps<T extends Record<string, unknown>> {
  /** Called with the form values when the user clicks Save. */
  onSubmit: (values: T) => void | Promise<void>
  /** Whether the form is currently submitting. */
  isSubmitting?: boolean
  /** Label for the submit button (default: "Save preferences"). */
  submitLabel?: string
  /** Optional label for a destructive reset action. */
  resetLabel?: string
  /** Called when user clicks the reset button. */
  onReset?: () => void
  /** Form field content (rendered between header and footer). */
  children: React.ReactNode
  /** Custom class name. */
  className?: string
}

/**
 * PreferencesForm — generic settings form shell.
 *
 * Renders a `<form>` element with a save button and optional reset action.
 * Field content is injected via `children`.
 *
 * Intentionally thin — no built-in field logic. Use React Hook Form's
 * `useForm()` in the parent and pass the bound `handleSubmit` as `onSubmit`.
 *
 * ```tsx
 * import { useForm } from 'react-hook-form'
 * import PreferencesForm from 'templates/L2/blocks/preferences-form'
 *
 * export function NotificationPreferencesForm() {
 *   const { register, handleSubmit } = useForm({ defaultValues })
 *   return (
 *     <PreferencesForm onSubmit={handleSubmit(savePreferences)}>
 *       <label>
 *         <input type="checkbox" {...register('emailDigest')} />
 *         Email digest
 *       </label>
 *     </PreferencesForm>
 *   )
 * }
 * ```
 */
export default function PreferencesForm<T extends Record<string, unknown>>({
  onSubmit,
  isSubmitting = false,
  submitLabel = 'Save preferences',
  resetLabel,
  onReset,
  children,
  className,
}: PreferencesFormProps<T>) {
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const data = Object.fromEntries(new FormData(e.currentTarget)) as unknown as T
    await onSubmit(data)
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      data-testid="preferences-form"
      className={['space-y-6', className ?? ''].filter(Boolean).join(' ')}
    >
      {/* Field slot */}
      <div className="space-y-4">{children}</div>

      {/* Footer actions */}
      <div className="flex items-center gap-3 pt-2 border-t">
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          data-testid="preferences-save-btn"
        >
          {isSubmitting ? 'Saving…' : submitLabel}
        </button>

        {onReset && resetLabel && (
          <button
            type="button"
            onClick={onReset}
            disabled={isSubmitting}
            className="rounded-md border px-4 py-2 text-sm font-medium text-foreground hover:bg-accent disabled:opacity-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            data-testid="preferences-reset-btn"
          >
            {resetLabel}
          </button>
        )}
      </div>
    </form>
  )
}
