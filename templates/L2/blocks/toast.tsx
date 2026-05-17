/*
---
template_id: L2/blocks/toast
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-registry-2026-05
    section: sonner
    quote: "An opinionated toast component for React."
  - source_type: internal
    rationale: "L2 common block — thin Sonner wrapper with ax-template type presets; no domain coupling."
dependencies: [sonner]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/]
---
*/
'use client'

import { toast as sonnerToast, Toaster } from 'sonner'

export type ToastType = 'default' | 'success' | 'error' | 'info' | 'warning'

export interface ShowToastOptions {
  message: string
  type?: ToastType
  description?: string
  /** Duration in ms (default: 4000) */
  duration?: number
  /** Unique id for deduplication */
  id?: string
}

/**
 * toast — programmatic toast helper.
 *
 * L4 usage:
 *   import { toast } from 'templates/L2/blocks/toast'
 *   toast({ message: 'Saved!', type: 'success' })
 */
export function toast({
  message,
  type = 'default',
  description,
  duration = 4_000,
  id,
}: ShowToastOptions): void {
  const opts = { description, duration, id }
  switch (type) {
    case 'success':
      sonnerToast.success(message, opts)
      break
    case 'error':
      sonnerToast.error(message, opts)
      break
    case 'info':
      sonnerToast.info(message, opts)
      break
    case 'warning':
      sonnerToast.warning(message, opts)
      break
    default:
      sonnerToast(message, opts)
  }
}

export interface ToastProviderProps {
  /** Position of the toast stack */
  position?: 'top-left' | 'top-center' | 'top-right' | 'bottom-left' | 'bottom-center' | 'bottom-right'
  richColors?: boolean
  closeButton?: boolean
}

/**
 * ToastProvider — wraps Sonner's <Toaster> with ax-template defaults.
 * Place once in root layout.
 *
 * L4 usage:
 *   // app/layout.tsx
 *   import { ToastProvider } from 'templates/L2/blocks/toast'
 *   export default function RootLayout({ children }) {
 *     return <html><body>{children}<ToastProvider /></body></html>
 *   }
 */
export default function ToastProvider({
  position = 'bottom-right',
  richColors = true,
  closeButton = false,
}: ToastProviderProps) {
  return (
    <Toaster
      position={position}
      richColors={richColors}
      closeButton={closeButton}
      toastOptions={{ duration: 4_000 }}
    />
  )
}
