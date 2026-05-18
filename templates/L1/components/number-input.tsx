/*
---
template_id: L1/components/number-input
layer: L1
provenance_class: internal_design
evidence:
  - source_type: external
    citation: "MDN — inputmode=numeric for numeric-only keyboards on mobile"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "WCAG 2.2 — 1.3.5 Identify Input Purpose; spinner controls use role=spinbutton"
    url: "https://www.w3.org/WAI/WCAG22/Techniques/"
    quoted_at: "2026-05-18"
a11y_criteria:
  - "role=spinbutton with aria-valuenow, aria-valuemin, aria-valuemax"
  - "Increment/decrement buttons have aria-label"
dependencies: []
imports_from: []
imports_forbidden: [L2, L3, L4, app/, lib/auth/]
---
*/

'use client'

import * as React from 'react'

export interface NumberInputProps {
  /** Controlled integer value. */
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  step?: number
  disabled?: boolean
  className?: string
  id?: string
  'aria-label'?: string
}

/**
 * NumberInput — L1 spinner for integer quantities (plan tier, seat count, etc.).
 *
 * Usage in billing context: quantity selector for plan seats.
 * Value is always an integer.
 *
 * ```tsx
 * <NumberInput value={seats} onChange={setSeats} min={1} max={100} />
 * ```
 */
export function NumberInput({
  value,
  onChange,
  min = 0,
  max,
  step = 1,
  disabled,
  className,
  id,
  'aria-label': ariaLabel,
}: NumberInputProps) {
  const decrement = () => {
    const next = value - step
    if (min === undefined || next >= min) onChange(next)
  }

  const increment = () => {
    const next = value + step
    if (max === undefined || next <= max) onChange(next)
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const parsed = parseInt(e.target.value, 10)
    if (!isNaN(parsed)) {
      const clamped = Math.max(min ?? -Infinity, Math.min(max ?? Infinity, parsed))
      onChange(clamped)
    }
  }

  return (
    <div
      role="spinbutton"
      aria-valuenow={value}
      aria-valuemin={min}
      aria-valuemax={max}
      aria-label={ariaLabel}
      className={`flex items-center rounded-md border border-input bg-background ${className ?? ''}`}
    >
      <button
        type="button"
        onClick={decrement}
        disabled={disabled || (min !== undefined && value <= min)}
        aria-label="Decrease value"
        className="px-3 py-2 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50 rounded-l-md"
      >
        −
      </button>
      <input
        id={id}
        type="number"
        value={value}
        onChange={handleChange}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
        className="w-16 text-center border-none bg-transparent text-sm tabular-nums
                   focus-visible:outline-none [&::-webkit-inner-spin-button]:appearance-none
                   [&::-webkit-outer-spin-button]:appearance-none"
        style={{ MozAppearance: 'textfield' } as React.CSSProperties}
      />
      <button
        type="button"
        onClick={increment}
        disabled={disabled || (max !== undefined && value >= max)}
        aria-label="Increase value"
        className="px-3 py-2 text-sm font-medium hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50 rounded-r-md"
      >
        +
      </button>
    </div>
  )
}
