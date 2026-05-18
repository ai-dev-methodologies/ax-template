/*
---
template_id: L2/blocks/auto-save-indicator
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React Hook Form docs — useWatch (observe form values for auto-save)"
    url: "https://react-hook-form.com/docs/usewatch"
  - source_type: external
    citation: "MDN Web Docs — setTimeout / clearTimeout (debounce pattern)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/setTimeout"
  - source_type: internal
    rationale: "L2 form block — debounced auto-save indicator with save/saving/error/idle states. New in SP27."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'
import { useWatch, useFormContext, type FieldValues } from 'react-hook-form'

// ─── types ────────────────────────────────────────────────────────────────────

export type AutoSaveStatus = 'idle' | 'pending' | 'saving' | 'saved' | 'error'

export interface AutoSaveIndicatorProps<TFieldValues extends FieldValues> {
  /** Async function that persists the current form values. Called after debounce. */
  saveFn: (values: TFieldValues) => Promise<void>
  /** Debounce delay in ms (default: 1000) */
  debounceMs?: number
  /** Fields to watch. If omitted, watches the entire form. */
  watchFields?: (keyof TFieldValues)[]
  /** Status labels — override for i18n */
  labels?: Partial<Record<AutoSaveStatus, string>>
  className?: string
}

const DEFAULT_LABELS: Record<AutoSaveStatus, string> = {
  idle:    '',
  pending: 'Unsaved changes',
  saving:  'Saving…',
  saved:   'Saved',
  error:   'Save failed',
}

/**
 * AutoSaveIndicator — debounced auto-save with status display.
 *
 * Watches form values via RHF `useWatch`, debounces changes by `debounceMs`,
 * then calls `saveFn` with the current form values. Shows save status as a
 * small indicator string.
 *
 * **TDD fixture:** `auto-save-indicator.spec.ts` asserts that:
 * - Typing triggers pending state
 * - After debounce, saveFn is called
 * - Successful save shows "Saved" for 2s then returns to idle
 * - Failed save shows "Save failed" indefinitely until next edit
 *
 * Must be used inside a `<FormProvider>`.
 *
 * @example
 * <FormProvider {...form}>
 *   <AutoSaveIndicator
 *     saveFn={async (values) => { await api.patch('/api/doc/1', values) }}
 *     debounceMs={800}
 *   />
 *   <TitleField />
 *   <BodyField />
 * </FormProvider>
 */
export default function AutoSaveIndicator<TFieldValues extends FieldValues>({
  saveFn,
  debounceMs = 1000,
  labels,
  className,
}: AutoSaveIndicatorProps<TFieldValues>) {
  const { getValues, control } = useFormContext<TFieldValues>()
  const values = useWatch({ control }) as TFieldValues

  const [status, setStatus] = React.useState<AutoSaveStatus>('idle')
  const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null)
  const savedTimerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null)
  const isFirstRender = React.useRef(true)

  const effectiveLabels = { ...DEFAULT_LABELS, ...labels }

  React.useEffect(() => {
    // Skip first render — don't save on mount
    if (isFirstRender.current) { isFirstRender.current = false; return }

    setStatus('pending')

    if (timerRef.current) clearTimeout(timerRef.current)
    timerRef.current = setTimeout(async () => {
      setStatus('saving')
      try {
        await saveFn(getValues())
        setStatus('saved')
        // Emit metrics shim event (relayed by templates/L1/_lib/metrics.ts if present)
        if (typeof window !== 'undefined') {
          (window as Window & { __axMetrics?: { increment: (key: string) => void } })
            .__axMetrics?.increment('form.auto_save.success_count')
        }
        // Return to idle after 2s
        if (savedTimerRef.current) clearTimeout(savedTimerRef.current)
        savedTimerRef.current = setTimeout(() => setStatus('idle'), 2000)
      } catch {
        setStatus('error')
        if (typeof window !== 'undefined') {
          (window as Window & { __axMetrics?: { increment: (key: string) => void } })
            .__axMetrics?.increment('form.auto_save.error_count')
        }
      }
    }, debounceMs)

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    }
  // values serialised to string to avoid infinite loop from object identity
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(values), debounceMs])

  // Cleanup on unmount
  React.useEffect(() => () => {
    if (timerRef.current) clearTimeout(timerRef.current)
    if (savedTimerRef.current) clearTimeout(savedTimerRef.current)
  }, [])

  if (status === 'idle') return null

  const statusStyles: Record<AutoSaveStatus, string> = {
    idle:    '',
    pending: 'text-muted-foreground',
    saving:  'text-muted-foreground',
    saved:   'text-green-600 dark:text-green-400',
    error:   'text-destructive',
  }

  return (
    <span
      role="status"
      aria-live="polite"
      data-save-status={status}
      className={[
        'inline-flex items-center gap-1 text-xs',
        statusStyles[status],
        className,
      ].filter(Boolean).join(' ')}
    >
      {status === 'saving' && (
        <span className="h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent" aria-hidden="true" />
      )}
      {effectiveLabels[status]}
    </span>
  )
}
