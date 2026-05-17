/*
---
template_id: L2/blocks/toast-queue
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: shadcn-registry-2026-05
    section: sonner
    quote: "An opinionated toast component for React."
  - source_type: external
    citation: "WCAG 2.2 — 4.1.3 Status Messages (Level AA): Status messages must be programmatically determinable via role or property such that they can be presented to the user by assistive technologies without receiving focus."
    url: "https://www.w3.org/TR/WCAG22/#status-messages"
dependencies: [sonner, zustand]
imports_from: [L1]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'
import { toast as sonnerToast, Toaster } from 'sonner'

// ─── types ────────────────────────────────────────────────────────────────────

export type ToastVariant = 'default' | 'success' | 'error' | 'info' | 'warning'

export interface ToastItem {
  id: string
  message: string
  variant?: ToastVariant
  description?: string
  /** Duration in ms. undefined = library default (4000ms). 0 = persistent. */
  duration?: number
}

// ─── singleton queue (module-level; survives re-renders) ──────────────────────

type Listener = () => void
const _listeners = new Set<Listener>()
const _queue: ToastItem[] = []

function notifyListeners() {
  _listeners.forEach(fn => fn())
}

/**
 * enqueueToast — push a toast into the global queue.
 *
 * L4 usage:
 *   import { enqueueToast } from 'templates/L2/blocks/toast-queue'
 *   enqueueToast({ message: 'Saved!', variant: 'success' })
 */
export function enqueueToast(item: Omit<ToastItem, 'id'>): void {
  const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
  const toastItem: ToastItem = { id, ...item }

  _queue.push(toastItem)
  notifyListeners()

  // Flush through sonner immediately
  const opts = {
    id,
    description: toastItem.description,
    duration: toastItem.duration,
  }
  switch (toastItem.variant) {
    case 'success':
      sonnerToast.success(toastItem.message, opts)
      break
    case 'error':
      sonnerToast.error(toastItem.message, opts)
      break
    case 'info':
      sonnerToast.info(toastItem.message, opts)
      break
    case 'warning':
      sonnerToast.warning(toastItem.message, opts)
      break
    default:
      sonnerToast(toastItem.message, opts)
  }

  // Remove from queue after dismiss (best-effort)
  const dur = toastItem.duration ?? 4_000
  if (dur > 0) {
    setTimeout(() => {
      const idx = _queue.findIndex(t => t.id === id)
      if (idx !== -1) {
        _queue.splice(idx, 1)
        notifyListeners()
      }
    }, dur + 500)
  }
}

/** useToastQueue — subscribe to the live queue (for display/testing). */
export function useToastQueue(): readonly ToastItem[] {
  const [, rerender] = React.useReducer(n => n + 1, 0)

  React.useEffect(() => {
    _listeners.add(rerender)
    return () => { _listeners.delete(rerender) }
  }, [])

  return _queue as readonly ToastItem[]
}

// ─── provider ─────────────────────────────────────────────────────────────────

export interface ToastQueueProviderProps {
  position?: 'top-left' | 'top-center' | 'top-right' | 'bottom-left' | 'bottom-center' | 'bottom-right'
  richColors?: boolean
  closeButton?: boolean
  children: React.ReactNode
}

/**
 * ToastQueueProvider — place once in root layout.
 *
 * Renders Sonner's <Toaster> and an aria-live region so assistive
 * technologies receive toast announcements (WCAG 4.1.3).
 *
 * L4 usage:
 *   // app/layout.tsx
 *   import ToastQueueProvider from 'templates/L2/blocks/toast-queue'
 *   export default function RootLayout({ children }) {
 *     return (
 *       <html><body>
 *         <ToastQueueProvider>{children}</ToastQueueProvider>
 *       </body></html>
 *     )
 *   }
 */
export default function ToastQueueProvider({
  position = 'bottom-right',
  richColors = true,
  closeButton = false,
  children,
}: ToastQueueProviderProps) {
  const queue = useToastQueue()

  return (
    <>
      {children}
      {/* WCAG 4.1.3 — aria-live region for AT announcements */}
      <div
        role="status"
        aria-live="polite"
        aria-atomic="false"
        aria-relevant="additions"
        className="sr-only"
      >
        {queue.map(t => (
          <span key={t.id}>{t.message}</span>
        ))}
      </div>
      <Toaster
        position={position}
        richColors={richColors}
        closeButton={closeButton}
        toastOptions={{ duration: 4_000 }}
      />
    </>
  )
}
