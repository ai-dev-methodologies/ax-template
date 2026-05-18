/*
---
template_id: L2/blocks/dirty-guard
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "React Hook Form docs — formState.isDirty"
    url: "https://react-hook-form.com/docs/useformstate"
  - source_type: external
    citation: "MDN Web Docs — beforeunload event (navigation interception)"
    url: "https://developer.mozilla.org/en-US/docs/Web/API/Window/beforeunload_event"
  - source_type: internal
    rationale: "L2 form block — intercepts navigation and browser close when form has unsaved changes. New in SP27."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/

'use client'

import * as React from 'react'
import { useFormContext } from 'react-hook-form'

// ─── types ────────────────────────────────────────────────────────────────────

export interface DirtyGuardProps {
  /**
   * Custom message shown in the browser's beforeunload dialog.
   * Note: modern browsers ignore this text and show their own message.
   */
  message?: string
  /**
   * Called when navigation is blocked (router-level guard).
   * Use this with your router's navigation guard API (e.g. Next.js router.events).
   */
  onNavigationBlocked?: () => void
  /**
   * Override isDirty check — useful for custom dirty detection outside RHF.
   * When provided, this overrides the RHF formState.isDirty value.
   */
  isDirty?: boolean
  children?: React.ReactNode
}

/**
 * DirtyGuard — blocks browser navigation and window close when a form has unsaved changes.
 *
 * **TDD fixture:** `dirty-guard.spec.ts` asserts that navigating away with unsaved changes
 * triggers the `beforeunload` handler. Pre-SP27: this file ENOENT; fixture exits 1.
 *
 * **Usage:**
 * 1. Place inside a `<FormProvider>` to auto-detect RHF `isDirty`.
 * 2. Optionally pass `isDirty` prop to override (for non-RHF forms).
 * 3. Connect `onNavigationBlocked` to your router's guard (e.g. Next.js `useRouter`).
 *
 * **Browser beforeunload:** the browser shows its own message (not the custom `message`
 * prop) on tab close / URL navigation. The prop is a fallback for older browsers.
 *
 * **Metrics:** fires `window.__axMetrics?.increment('form.dirty_block.fired_count')`
 * when the guard activates. Relayed by `templates/L1/_lib/metrics.ts` if present.
 *
 * Must be used inside a `<FormProvider>` (or pass `isDirty` directly).
 *
 * @example
 * <FormProvider {...form}>
 *   <DirtyGuard onNavigationBlocked={() => setShowLeaveDialog(true)} />
 *   <form onSubmit={form.handleSubmit(save)}>
 *     <TitleField />
 *   </form>
 * </FormProvider>
 */
export default function DirtyGuard({
  message = 'You have unsaved changes. Leave anyway?',
  onNavigationBlocked,
  isDirty: isDirtyOverride,
  children,
}: DirtyGuardProps) {
  // Gracefully handle usage outside FormProvider (e.g. custom dirty prop only)
  let rhfIsDirty = false
  try {
    // eslint-disable-next-line react-hooks/rules-of-hooks
    const ctx = useFormContext()
    rhfIsDirty = ctx?.formState?.isDirty ?? false
  } catch {
    // Not inside FormProvider — use prop override
  }

  const isDirty = isDirtyOverride ?? rhfIsDirty

  // ─── beforeunload (tab close / URL bar navigation) ─────────────────────────
  React.useEffect(() => {
    if (!isDirty) return

    function handleBeforeUnload(e: BeforeUnloadEvent) {
      e.preventDefault()
      e.returnValue = message   // legacy browsers

      // Emit metrics shim event
      if (typeof window !== 'undefined') {
        (window as Window & { __axMetrics?: { increment: (key: string) => void } })
          .__axMetrics?.increment('form.dirty_block.fired_count')
      }

      onNavigationBlocked?.()
      return message
    }

    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty, message, onNavigationBlocked])

  return <>{children}</>
}
