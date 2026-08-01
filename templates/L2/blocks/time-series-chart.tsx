/*
---
template_id: L2/blocks/time-series-chart
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "Recharts project overview (README)"
    quote: "The LineChart is composed of x axis, tooltip, grid, and line items, and each of them is an independent React Component."
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "ResponsiveContainer"
    quote: "The ResponsiveContainer component is a container that adjusts its width and height based on the size of its parent element. It is used to create responsive charts that adapt to different screen sizes."
  - source_type: external
    citation: "WCAG 2.2 — 1.4.1 Use of Color: color must not be the only means of conveying information in a chart"
    url: "https://www.w3.org/TR/WCAG22/#use-of-color"
dependencies: [recharts]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/, lib/payment/]
---
*/
'use client'

import * as React from 'react'
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts'

// ─── types ────────────────────────────────────────────────────────────────────

export interface TimeSeriesDataPoint {
  /** X-axis label (date string, timestamp, or category). */
  label: string
  [seriesKey: string]: string | number
}

export interface TimeSeriesSeries {
  /** Key in each data point for this series' value. */
  dataKey: string
  /** Display label for the legend and tooltip. */
  name: string
  /** Hex/CSS color for this line. */
  color?: string
  /** Whether to show dots at each data point. @default false */
  dot?: boolean
}

export interface TimeSeriesChartProps {
  /** Chart data — each object has a `label` plus one key per series. */
  data: TimeSeriesDataPoint[]
  /** Series definitions. */
  series: TimeSeriesSeries[]
  /** Accessible title for screen readers. */
  title: string
  /** Chart height in px. @default 300 */
  height?: number
  /** Y-axis label. */
  yAxisLabel?: string
  /** Custom tooltip formatter (value, name) → string. */
  tooltipFormatter?: (value: number, name: string) => string
  className?: string
}

// ─── default colors ───────────────────────────────────────────────────────────

const DEFAULT_COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#14b8a6']

// ─── component ───────────────────────────────────────────────────────────────

/**
 * TimeSeriesChart — responsive line chart for time-series data.
 *
 * Composes:
 *   recharts ResponsiveContainer → fluid width
 *   recharts LineChart → SVG chart with lines, axes, tooltip, legend
 *
 * Accessibility:
 *   - SVG <title> element for screen readers (WCAG 1.1.1)
 *   - Color is supplemented by distinct strokeDasharray patterns (WCAG 1.4.1)
 *
 * Fork instructions:
 *   1. Use inside L4 dashboard pages (audit-log, payment-revenue).
 *   2. Pass `tooltipFormatter` to format currency (KRW) or custom units.
 *   3. Adjust `height` to match your layout.
 *
 * @example
 * ```tsx
 * <TimeSeriesChart
 *   title="Monthly Revenue"
 *   data={[{ label: 'Jan', revenue: 12000 }, { label: 'Feb', revenue: 15000 }]}
 *   series={[{ dataKey: 'revenue', name: 'Revenue (KRW)', color: '#6366f1' }]}
 *   tooltipFormatter={(v) => `₩${v.toLocaleString()}`}
 * />
 * ```
 */
export default function TimeSeriesChart({
  data,
  series,
  title,
  height = 300,
  yAxisLabel,
  tooltipFormatter,
  className,
}: TimeSeriesChartProps) {
  const dashArrays = ['', '5 5', '5 2 2 2', '10 5', '2 2']

  return (
    <figure className={className} aria-label={title}>
      <ResponsiveContainer width="100%" height={height}>
        <LineChart data={data} margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
          {/* accessible title */}
          <title>{title}</title>

          <CartesianGrid strokeDasharray="3 3" stroke="currentColor" strokeOpacity={0.1} />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 12 }}
            tickLine={false}
            axisLine={false}
          />
          <YAxis
            tick={{ fontSize: 12 }}
            tickLine={false}
            axisLine={false}
            label={yAxisLabel ? { value: yAxisLabel, angle: -90, position: 'insideLeft', fontSize: 12 } : undefined}
          />
          <Tooltip
            formatter={tooltipFormatter ? (v, n) => [tooltipFormatter(v as number, n as string), n] : undefined}
            contentStyle={{ fontSize: 12, borderRadius: 6 }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />

          {series.map((s, i) => (
            <Line
              key={s.dataKey}
              type="monotone"
              dataKey={s.dataKey}
              name={s.name}
              stroke={s.color ?? DEFAULT_COLORS[i % DEFAULT_COLORS.length]}
              strokeWidth={2}
              strokeDasharray={dashArrays[i % dashArrays.length]}
              dot={s.dot ?? false}
              activeDot={{ r: 4 }}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </figure>
  )
}
