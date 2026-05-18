/*
---
template_id: L2/blocks/kpi-card
layer: L2
provenance_class: internal_design
evidence:
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "Sparkline (compact LineChart)"
    quote: "A Sparkline is a miniature LineChart with no axes, no legend, and no tooltip by default — used inline in tables or KPI cards."
  - source_type: external
    citation: "WCAG 2.2 — 1.3.3 Sensory Characteristics: instructions must not rely solely on shape, color, or visual location; KPI trend direction must use icon+text, not color alone"
    url: "https://www.w3.org/TR/WCAG22/#sensory-characteristics"
dependencies: [recharts]
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'
import { ResponsiveContainer, LineChart, Line } from 'recharts'

// ─── types ────────────────────────────────────────────────────────────────────

export interface KPICardProps {
  /** KPI title label. */
  title: string
  /** Current value displayed prominently. */
  value: string | number
  /** Formatted value string (e.g. "₩1,234,000"). If provided, overrides `value` display. */
  formattedValue?: string
  /** Trend direction relative to previous period. */
  trend?: 'up' | 'down' | 'neutral'
  /** Percentage change string (e.g. "+12.5%"). */
  changePercent?: string
  /** Sparkline data points — array of objects with a `v` key. */
  sparklineData?: Array<{ v: number }>
  /** Sparkline line color. @default derives from trend */
  sparklineColor?: string
  className?: string
}

// ─── trend helpers ────────────────────────────────────────────────────────────

const TREND_COLOR: Record<string, string> = {
  up: 'text-emerald-600',
  down: 'text-rose-600',
  neutral: 'text-muted-foreground',
}

const TREND_SPARKLINE: Record<string, string> = {
  up: '#22c55e',
  down: '#ef4444',
  neutral: '#94a3b8',
}

const TREND_ARROW: Record<string, string> = {
  up: '↑',
  down: '↓',
  neutral: '→',
}

const TREND_LABEL: Record<string, string> = {
  up: 'increased',
  down: 'decreased',
  neutral: 'unchanged',
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * KPICard — key-performance-indicator card with optional sparkline trend.
 *
 * Composes:
 *   recharts Sparkline (compact LineChart) — rendered inline in the card footer
 *
 * Accessibility:
 *   - Trend direction communicated via text symbol + aria-label (not color alone; WCAG 1.3.3)
 *   - `aria-label` on sparkline region describes the trend direction
 *
 * Fork instructions:
 *   1. Compose in dashboard grids (2-col, 4-col, etc.).
 *   2. Pass `formattedValue="₩1,234,000"` for currency display.
 *   3. Omit `sparklineData` for a static card with no trend chart.
 *
 * @example
 * ```tsx
 * <KPICard
 *   title="Total Revenue"
 *   value={1234000}
 *   formattedValue="₩1,234,000"
 *   trend="up"
 *   changePercent="+12.5%"
 *   sparklineData={revenueHistory.map(v => ({ v }))}
 * />
 * ```
 */
export default function KPICard({
  title,
  value,
  formattedValue,
  trend = 'neutral',
  changePercent,
  sparklineData,
  sparklineColor,
  className,
}: KPICardProps) {
  const trendColor = TREND_COLOR[trend]
  const lineColor = sparklineColor ?? TREND_SPARKLINE[trend]
  const arrow = TREND_ARROW[trend]
  const trendDesc = TREND_LABEL[trend]

  return (
    <div
      className={[
        'rounded-xl border bg-card p-5 flex flex-col gap-3',
        className ?? '',
      ].join(' ')}
    >
      {/* header */}
      <p className="text-sm text-muted-foreground font-medium tracking-wide uppercase">{title}</p>

      {/* value */}
      <p className="text-3xl font-bold tabular-nums tracking-tight">
        {formattedValue ?? value}
      </p>

      {/* trend + sparkline */}
      {(changePercent || sparklineData) && (
        <div className="flex items-center justify-between gap-4">
          {changePercent && (
            <span
              className={['text-sm font-semibold', trendColor].join(' ')}
              aria-label={`${trendDesc} by ${changePercent}`}
            >
              <span aria-hidden="true">{arrow}</span> {changePercent}
            </span>
          )}

          {sparklineData && sparklineData.length > 0 && (
            <div
              aria-label={`${title} trend chart — ${trendDesc}`}
              className="flex-1 max-w-[100px] h-10"
            >
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={sparklineData}>
                  <Line
                    type="monotone"
                    dataKey="v"
                    stroke={lineColor}
                    strokeWidth={2}
                    dot={false}
                    isAnimationActive={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
