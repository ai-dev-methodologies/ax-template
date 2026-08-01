/*
---
template_id: L2/blocks/sparkline
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "ResponsiveContainer"
    quote: "The ResponsiveContainer component is a container that adjusts its width and height based on the size of its parent element. It is used to create responsive charts that adapt to different screen sizes."
  - source_type: external
    citation: "Edward Tufte — Sparklines: intense, simple, word-sized graphics embedded in text or table cells to convey trend at a glance without requiring a dedicated chart area"
    url: "https://www.edwardtufte.com/bboard/q-and-a-fetch-msg?msg_id=0001OR"
dependencies: [recharts]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'
import { ResponsiveContainer, LineChart, Line, ReferenceLine } from 'recharts'

// ─── types ────────────────────────────────────────────────────────────────────

export interface SparklineProps {
  /** Array of numeric values (index = x-axis). */
  data: number[]
  /** Accessible description for screen readers. */
  ariaLabel: string
  /** Line color. @default '#6366f1' */
  color?: string
  /** Show a horizontal reference line at this y value (e.g. baseline = 0). */
  referenceLine?: number
  /** Width in px. @default 80 */
  width?: number
  /** Height in px. @default 32 */
  height?: number
  className?: string
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * Sparkline — minimal inline trend chart for embedding in table cells or text.
 *
 * No axes, no legend, no tooltip. Pure trend signal.
 *
 * Fork instructions:
 *   1. Use in data-table cell renderers for numeric trend columns.
 *   2. Pass `referenceLine={0}` to show a zero baseline for revenue delta.
 *   3. Pair with KPICard's sparklineData prop for consistent styling.
 *
 * @example
 * ```tsx
 * // In a table cell:
 * <Sparkline data={row.weeklyRevenue} ariaLabel="7-day revenue trend" color="#22c55e" />
 * ```
 */
export default function Sparkline({
  data,
  ariaLabel,
  color = '#6366f1',
  referenceLine,
  width = 80,
  height = 32,
  className,
}: SparklineProps) {
  const chartData = data.map((v, i) => ({ i, v }))

  return (
    <span
      role="img"
      aria-label={ariaLabel}
      className={['inline-block', className ?? ''].join(' ')}
      style={{ width, height }}
    >
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData} margin={{ top: 2, right: 2, bottom: 2, left: 2 }}>
          {referenceLine !== undefined && (
            <ReferenceLine y={referenceLine} stroke="currentColor" strokeOpacity={0.2} strokeWidth={1} />
          )}
          <Line
            type="monotone"
            dataKey="v"
            stroke={color}
            strokeWidth={1.5}
            dot={false}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </span>
  )
}
