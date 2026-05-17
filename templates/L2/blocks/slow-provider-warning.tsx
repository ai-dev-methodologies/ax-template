/*
---
template_id: L2/blocks/slow-provider-warning
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "L2 payment block — shows warning when payment provider response exceeds thresholdMs (default 3000ms per PAYMENT-PROVIDER-007)."
dependencies: []
imports_from: [L1]
imports_forbidden: [L4, app/, lib/payment/]
---
*/
import * as React from 'react'

/** PAYMENT-PROVIDER-007: warn when provider response takes > 3 000 ms */
const DEFAULT_THRESHOLD_MS = 3_000

export interface SlowProviderWarningProps {
  /**
   * Whether a payment request is currently in-flight.
   * The timer starts when `isLoading` becomes true and resets when false.
   */
  isLoading: boolean
  /**
   * Milliseconds before the warning appears.
   * @default 3000  (per PAYMENT-PROVIDER-007)
   */
  thresholdMs?: number
  /**
   * Custom warning message.
   * @default "Payment is taking longer than expected. Please wait…"
   */
  message?: string
}

export default function SlowProviderWarning({
  isLoading,
  thresholdMs = DEFAULT_THRESHOLD_MS,
  message = 'Payment is taking longer than expected. Please wait…',
}: SlowProviderWarningProps) {
  const [showWarning, setShowWarning] = React.useState(false)

  React.useEffect(() => {
    if (!isLoading) {
      setShowWarning(false)
      return
    }
    const timer = window.setTimeout(() => setShowWarning(true), thresholdMs)
    return () => window.clearTimeout(timer)
  }, [isLoading, thresholdMs])

  if (!showWarning) return null

  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center gap-2 rounded-md border border-yellow-200 bg-yellow-50 px-3 py-2 text-sm text-yellow-800 dark:border-yellow-800/40 dark:bg-yellow-900/20 dark:text-yellow-300"
    >
      <span aria-hidden="true">⏳</span>
      {message}
    </div>
  )
}
