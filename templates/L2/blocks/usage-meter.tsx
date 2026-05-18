/*
---
template_id: L2/blocks/usage-meter
layer: L2
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "Audit B.2.11 P1 — UsageMeter required for quota progress display in billing UI"
    url: "https://ax-template.internal/audit"
  - source_type: external
    citation: "WCAG 2.2 — 4.1.3 Status Messages; progress bars need role=progressbar with aria-valuenow/min/max"
    url: "https://www.w3.org/WAI/WCAG22/Techniques/"
    quoted_at: "2026-05-18"
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/payment/]
---
*/

'use client'

import * as React from 'react'

export interface UsageMeterProps {
  /** Human-readable label (e.g., "API Calls", "Storage"). */
  label: string
  /** Current usage value. */
  usage: number
  /** Maximum quota value. */
  limit: number
  /** Unit label (e.g., "calls", "GB"). */
  unit?: string
  /** Warning threshold 0–1 (default 0.8 = 80%). */
  warningThreshold?: number
  /** Critical threshold 0–1 (default 0.95 = 95%). */
  criticalThreshold?: number
  className?: string
}

/**
 * UsageMeter — L2 progress bar displaying quota usage.
 *
 * Accessible: role=progressbar with aria-valuenow/min/max/label.
 * Color: green → yellow (warning) → red (critical).
 *
 * ```tsx
 * <UsageMeter label="API Calls" usage={8500} limit={10000} unit="calls" />
 * ```
 */
export function UsageMeter({
  label,
  usage,
  limit,
  unit,
  warningThreshold = 0.8,
  criticalThreshold = 0.95,
  className,
}: UsageMeterProps) {
  const ratio = limit > 0 ? Math.min(usage / limit, 1) : 0
  const percentage = Math.round(ratio * 100)

  const colorClass =
    ratio >= criticalThreshold
      ? 'bg-destructive'
      : ratio >= warningThreshold
        ? 'bg-yellow-500'
        : 'bg-primary'

  const usageText = unit
    ? `${usage.toLocaleString()} / ${limit.toLocaleString()} ${unit}`
    : `${usage.toLocaleString()} / ${limit.toLocaleString()}`

  return (
    <div className={`space-y-1.5 ${className ?? ''}`}>
      <div className="flex justify-between text-sm">
        <span className="font-medium">{label}</span>
        <span className="tabular-nums text-muted-foreground">{usageText}</span>
      </div>

      <div
        role="progressbar"
        aria-label={`${label}: ${usageText}`}
        aria-valuenow={usage}
        aria-valuemin={0}
        aria-valuemax={limit}
        className="h-2 w-full overflow-hidden rounded-full bg-secondary"
      >
        <div
          className={`h-full rounded-full transition-all duration-300 ${colorClass}`}
          style={{ width: `${percentage}%` }}
        />
      </div>

      {ratio >= criticalThreshold && (
        <p className="text-xs text-destructive" role="alert">
          할당량의 {percentage}%를 사용했습니다. 플랜 업그레이드를 고려하세요.
        </p>
      )}
    </div>
  )
}
