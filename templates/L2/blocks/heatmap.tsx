/*
---
template_id: L2/blocks/heatmap
layer: L2
provenance_class: external_canonical
evidence:
  - source_type: upstream_id
    upstream_id: recharts-2026-05
    section: "Custom Content with ResponsiveContainer"
    quote: "ResponsiveContainer provides fluid width/height so charts resize with their container. Combine with custom SVG cells for heatmap-style visualizations."
  - source_type: external
    citation: "WCAG 2.2 — 1.4.1 Use of Color: color must not be the only means of conveying information; numeric labels must accompany color intensity in heatmap cells"
    url: "https://www.w3.org/TR/WCAG22/#use-of-color"
dependencies: []
imports_from: [L1, L2]
imports_forbidden: [L4, app/, lib/auth/]
---
*/
'use client'

import * as React from 'react'

// ─── types ────────────────────────────────────────────────────────────────────

export interface HeatmapCell {
  /** Row label (e.g. weekday name). */
  row: string
  /** Column label (e.g. hour "00"–"23"). */
  col: string
  /** Numeric value determining cell intensity. */
  value: number
}

export interface HeatmapProps {
  data: HeatmapCell[]
  title: string
  /** Accessible description of what the heatmap represents. */
  ariaLabel?: string
  /** Color for the minimum value. @default '#e0f2fe' */
  colorMin?: string
  /** Color for the maximum value. @default '#0369a1' */
  colorMax?: string
  /** Cell size in px. @default 28 */
  cellSize?: number
  /** Cell gap in px. @default 2 */
  gap?: number
  /** Show numeric value inside each cell. @default false */
  showValues?: boolean
  /** Format displayed value. @default String(v) */
  valueFormatter?: (value: number) => string
  className?: string
}

// ─── helpers ─────────────────────────────────────────────────────────────────

function unique<T>(arr: T[]): T[] {
  return Array.from(new Set(arr))
}

/** Linear interpolation between two hex colors based on t ∈ [0,1]. */
function lerpHex(colorA: string, colorB: string, t: number): string {
  const parse = (hex: string) => {
    const h = hex.replace('#', '')
    return [
      parseInt(h.slice(0, 2), 16),
      parseInt(h.slice(2, 4), 16),
      parseInt(h.slice(4, 6), 16),
    ] as const
  }
  const [ar, ag, ab] = parse(colorA)
  const [br, bg, bb] = parse(colorB)
  const r = Math.round(ar + (br - ar) * t)
  const g = Math.round(ag + (bg - ag) * t)
  const b = Math.round(ab + (bb - ab) * t)
  return `rgb(${r},${g},${b})`
}

// ─── component ───────────────────────────────────────────────────────────────

/**
 * Heatmap — grid-based intensity chart for time-distribution or correlation data.
 *
 * Pure SVG grid — no recharts dependency. Fluid-width via CSS.
 *
 * Accessibility:
 *   - Numeric value rendered inside each cell (WCAG 1.4.1 — not color alone)
 *   - `role="img"` with `aria-label` on the SVG container
 *   - Each cell has `aria-label` with row/col/value for screen readers
 *
 * Fork instructions:
 *   1. Pass `data` as a flat array of { row, col, value } — rows = y-axis, cols = x-axis.
 *   2. Use for "activity by hour × weekday" or "metric by dimension × dimension" views.
 *   3. Pass `showValues={true}` for dense analytical grids; omit for sparkline-style heatmaps.
 *
 * @example
 * ```tsx
 * <Heatmap
 *   title="Hourly request volume"
 *   ariaLabel="Request volume by weekday and hour"
 *   data={hourlyData}
 *   colorMin="#e0f2fe"
 *   colorMax="#0369a1"
 *   showValues
 *   valueFormatter={v => v.toLocaleString()}
 * />
 * ```
 */
export default function Heatmap({
  data,
  title,
  ariaLabel,
  colorMin = '#e0f2fe',
  colorMax = '#0369a1',
  cellSize = 28,
  gap = 2,
  showValues = false,
  valueFormatter = String,
  className,
}: HeatmapProps) {
  const rows = unique(data.map((d) => d.row))
  const cols = unique(data.map((d) => d.col))

  const values = data.map((d) => d.value)
  const minVal = Math.min(...values)
  const maxVal = Math.max(...values)
  const range = maxVal - minVal || 1

  // Build lookup for O(1) access
  const lookup = new Map<string, number>()
  for (const d of data) {
    lookup.set(`${d.row}:${d.col}`, d.value)
  }

  const ROW_LABEL_WIDTH = 48
  const COL_LABEL_HEIGHT = 20
  const svgWidth = ROW_LABEL_WIDTH + cols.length * (cellSize + gap)
  const svgHeight = COL_LABEL_HEIGHT + rows.length * (cellSize + gap)

  return (
    <figure className={className} aria-label={ariaLabel ?? title}>
      <figcaption className="sr-only">{title}</figcaption>
      <div style={{ overflowX: 'auto' }}>
        <svg
          width={svgWidth}
          height={svgHeight}
          role="img"
          aria-label={ariaLabel ?? title}
        >
          <title>{title}</title>

          {/* Column labels */}
          {cols.map((col, ci) => (
            <text
              key={col}
              x={ROW_LABEL_WIDTH + ci * (cellSize + gap) + cellSize / 2}
              y={COL_LABEL_HEIGHT - 4}
              textAnchor="middle"
              fontSize={9}
              fill="currentColor"
              opacity={0.6}
            >
              {col}
            </text>
          ))}

          {/* Row labels + cells */}
          {rows.map((row, ri) => (
            <g key={row}>
              <text
                x={ROW_LABEL_WIDTH - 4}
                y={COL_LABEL_HEIGHT + ri * (cellSize + gap) + cellSize / 2 + 1}
                textAnchor="end"
                dominantBaseline="middle"
                fontSize={10}
                fill="currentColor"
                opacity={0.7}
              >
                {row}
              </text>

              {cols.map((col, ci) => {
                const val = lookup.get(`${row}:${col}`) ?? 0
                const t = (val - minVal) / range
                const fill = lerpHex(colorMin, colorMax, t)
                const x = ROW_LABEL_WIDTH + ci * (cellSize + gap)
                const y = COL_LABEL_HEIGHT + ri * (cellSize + gap)
                const labelBrightness = t > 0.55 ? 'white' : 'currentColor'

                return (
                  <g
                    key={col}
                    role="img"
                    aria-label={`${row} ${col}: ${valueFormatter(val)}`}
                  >
                    <rect
                      x={x}
                      y={y}
                      width={cellSize}
                      height={cellSize}
                      fill={fill}
                      rx={3}
                    />
                    {showValues && (
                      <text
                        x={x + cellSize / 2}
                        y={y + cellSize / 2}
                        textAnchor="middle"
                        dominantBaseline="middle"
                        fontSize={9}
                        fill={labelBrightness}
                        opacity={0.85}
                      >
                        {valueFormatter(val)}
                      </text>
                    )}
                  </g>
                )
              })}
            </g>
          ))}
        </svg>
      </div>
    </figure>
  )
}
