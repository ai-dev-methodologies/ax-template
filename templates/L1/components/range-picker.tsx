/*
---
template_id: L1/components/range-picker
layer: L1
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "MDN — input type=range; aria-valuemin/aria-valuemax for accessible range inputs"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/range"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "WCAG 2.2 — 1.3.1 Info and Relationships; range controls need accessible labels"
    url: "https://www.w3.org/WAI/WCAG22/Techniques/"
    quoted_at: "2026-05-18"
a11y_criteria:
  - "role=slider with aria-valuenow, aria-valuemin, aria-valuemax, aria-valuetext"
  - "Label slot accepts any ReactNode"
dependencies: []
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

'use client'

import * as React from 'react'

export interface RangePickerProps {
  /** Controlled value. */
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  step?: number
  /** Render function for the track label at each tick (optional). */
  formatLabel?: (value: number) => string
  disabled?: boolean
  className?: string
  id?: string
  'aria-label'?: string
}

/**
 * RangePicker — L1 slider for selecting a value within a range.
 *
 * Used in billing context for selecting plan tiers by numeric threshold
 * (e.g., storage size, seat count, API call quota).
 *
 * ```tsx
 * <RangePicker value={tier} onChange={setTier} min={1} max={5} step={1} />
 * ```
 */
export function RangePicker({
  value,
  onChange,
  min = 0,
  max = 100,
  step = 1,
  formatLabel,
  disabled,
  className,
  id,
  'aria-label': ariaLabel,
}: RangePickerProps) {
  const percentage = max > min ? ((value - min) / (max - min)) * 100 : 0
  const valueText = formatLabel ? formatLabel(value) : String(value)

  return (
    <div className={`flex flex-col gap-1 ${className ?? ''}`}>
      <input
        id={id}
        type="range"
        value={value}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
        onChange={(e) => onChange(Number(e.target.value))}
        aria-label={ariaLabel}
        aria-valuenow={value}
        aria-valuemin={min}
        aria-valuemax={max}
        aria-valuetext={valueText}
        className="w-full h-2 rounded-lg appearance-none cursor-pointer bg-secondary
                   accent-primary disabled:cursor-not-allowed disabled:opacity-50"
        style={{
          background: `linear-gradient(to right, hsl(var(--primary)) ${percentage}%, hsl(var(--secondary)) ${percentage}%)`,
        }}
      />
      <div className="flex justify-between text-xs text-muted-foreground tabular-nums">
        <span>{formatLabel ? formatLabel(min) : min}</span>
        <span aria-live="polite" aria-atomic>{valueText}</span>
        <span>{formatLabel ? formatLabel(max) : max}</span>
      </div>
    </div>
  )
}
