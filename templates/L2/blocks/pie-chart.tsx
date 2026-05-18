/*
---
template_id: L2/blocks/pie-chart
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "PieChart"
    quote: "PieChart renders pie slices. Each <Pie> entry needs dataKey and data."
  - source_type: external
    citation: "WCAG 2.2 — 1.4.1 Use of Color: color must not be the only means of conveying information; use labels or patterns alongside color"
    url: "https://www.w3.org/TR/WCAG22/#use-of-color"
dependencies: [recharts]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'
import { ResponsiveContainer, PieChart as RechartsPieChart, Pie, Cell, Tooltip, Legend } from 'recharts'

export interface PieSlice {
  name: string
  value: number
  color?: string
}

export interface PieChartProps {
  data: PieSlice[]
  title: string
  height?: number
  /** Show percentage labels on slices. @default true */
  showLabels?: boolean
  tooltipFormatter?: (value: number, name: string) => string
  className?: string
}

const DEFAULT_COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#14b8a6', '#8b5cf6']

const RADIAN = Math.PI / 180
function renderLabel({ cx, cy, midAngle, innerRadius, outerRadius, percent }: {
  cx: number; cy: number; midAngle: number; innerRadius: number; outerRadius: number; percent: number
}) {
  if (percent < 0.05) return null
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5
  const x = cx + radius * Math.cos(-midAngle * RADIAN)
  const y = cy + radius * Math.sin(-midAngle * RADIAN)
  return (
    <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central" fontSize={12} fontWeight={600}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  )
}

/**
 * PieChart — donut/pie chart for part-to-whole comparisons.
 *
 * Fork instructions:
 *   1. Pass `innerRadius={60}` for a donut chart.
 *   2. Pass `showLabels={false}` for clean minimal style; rely on legend + tooltip.
 */
export default function PieChart({ data, title, height = 280, showLabels = true, tooltipFormatter, className }: PieChartProps) {
  return (
    <figure className={className} aria-label={title}>
      <ResponsiveContainer width="100%" height={height}>
        <RechartsPieChart>
          <title>{title}</title>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={100}
            labelLine={false}
            label={showLabels ? renderLabel : undefined}
          >
            {data.map((slice, i) => (
              <Cell key={slice.name} fill={slice.color ?? DEFAULT_COLORS[i % DEFAULT_COLORS.length]} />
            ))}
          </Pie>
          <Tooltip
            formatter={tooltipFormatter ? (v, n) => [tooltipFormatter(v as number, n as string), n] : undefined}
            contentStyle={{ fontSize: 12, borderRadius: 6 }}
          />
          <Legend wrapperStyle={{ fontSize: 12 }} />
        </RechartsPieChart>
      </ResponsiveContainer>
    </figure>
  )
}
