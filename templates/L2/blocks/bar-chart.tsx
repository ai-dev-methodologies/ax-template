/*
---
template_id: L2/blocks/bar-chart
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "BarChart"
    quote: "BarChart is used to display and compare multiple sets of data or to track changes over time."
  - source_type: external
    citation: "WCAG 2.2 — 1.4.1 Use of Color: color must not be the only means of conveying information"
    url: "https://www.w3.org/TR/WCAG22/#use-of-color"
dependencies: [recharts]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'
import {
  ResponsiveContainer,
  BarChart as RechartsBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts'

// ─── types ────────────────────────────────────────────────────────────────────

export interface BarChartDataPoint {
  label: string
  [key: string]: string | number
}

export interface BarSeries {
  dataKey: string
  name: string
  color?: string
}

export interface BarChartProps {
  data: BarChartDataPoint[]
  series: BarSeries[]
  title: string
  height?: number
  /** When true, stacks bars instead of grouping. @default false */
  stacked?: boolean
  tooltipFormatter?: (value: number, name: string) => string
  className?: string
}

const DEFAULT_COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#14b8a6']

/**
 * BarChart — grouped or stacked bar chart for categorical comparisons.
 *
 * Fork instructions:
 *   1. Pass `stacked={true}` for 100% stacked bars.
 *   2. Pass `tooltipFormatter` for KRW or custom unit formatting.
 *   3. Single-series with `series={[{dataKey:'value', name:'Count'}]}` for simple bar charts.
 */
export default function BarChart({
  data,
  series,
  title,
  height = 300,
  stacked = false,
  tooltipFormatter,
  className,
}: BarChartProps) {
  return (
    <figure className={className} aria-label={title}>
      <ResponsiveContainer width="100%" height={height}>
        <RechartsBarChart data={data} margin={{ top: 8, right: 24, left: 8, bottom: 8 }}>
          <title>{title}</title>
          <CartesianGrid strokeDasharray="3 3" stroke="currentColor" strokeOpacity={0.1} vertical={false} />
          <XAxis dataKey="label" tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
          <YAxis tick={{ fontSize: 12 }} tickLine={false} axisLine={false} />
          <Tooltip
            formatter={tooltipFormatter ? (v, n) => [tooltipFormatter(v as number, n as string), n] : undefined}
            contentStyle={{ fontSize: 12, borderRadius: 6 }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          {series.map((s, i) => (
            <Bar
              key={s.dataKey}
              dataKey={s.dataKey}
              name={s.name}
              fill={s.color ?? DEFAULT_COLORS[i % DEFAULT_COLORS.length]}
              radius={[3, 3, 0, 0]}
              stackId={stacked ? 'stack' : undefined}
            />
          ))}
        </RechartsBarChart>
      </ResponsiveContainer>
    </figure>
  )
}
